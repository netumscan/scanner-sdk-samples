import Foundation
import ScannerSDK

struct CapabilityCatalogSnapshot: Sendable {
    let domains: [CapabilityDomain]
    let entries: [CapabilityEntry]
    let labels: [CapabilityLabel]
}

struct CapabilityCatalogItem: Identifiable, Sendable {
    let definition: CapabilityEntry
    let title: String
    let familyTitle: String
    let sectionTitle: String

    var id: String { definition.entryKey }
}

struct CapabilityCatalogSection: Identifiable, Sendable {
    let familyKey: String
    let familyTitle: String
    let sectionKey: String
    let sectionTitle: String
    let items: [CapabilityCatalogItem]

    var id: String { "\(familyKey)/\(sectionKey)" }
}

struct CapabilityCatalogDomain: Identifiable, Sendable {
    let definition: CapabilityDomain
    let title: String
    let sections: [CapabilityCatalogSection]

    var id: String { definition.key }
    var items: [CapabilityCatalogItem] { sections.flatMap(\.items) }
    var readableCount: Int { items.filter(\.definition.supportsRead).count }
    var writableCount: Int { items.filter(\.definition.supportsWrite).count }
    var actionCount: Int { items.filter { $0.definition.kind == .action }.count }
}

struct SettingDraft: Equatable, Sendable {
    var valueText = ""
    var persist = false
}

enum CapabilityCatalogLoadState: Equatable {
    case idle
    case loading
    case loaded
    case failed(String)
}

@MainActor
final class CapabilityCatalogStore: ObservableObject {
    @Published private(set) var catalog: [CapabilityCatalogDomain] = []
    @Published private(set) var optionsByKey: [String: [DemoCapabilityOption]] = [:]
    @Published var drafts: [String: SettingDraft] = [:]
    @Published private(set) var loadState: CapabilityCatalogLoadState = .idle

    private var loadGeneration = 0

    func reload(using viewModel: AppViewModel) {
        loadGeneration += 1
        let generation = loadGeneration
        loadState = .loading
        viewModel.appendEvent(
            .sdk,
            .info,
            DemoStrings.format(
                "capability_catalog_loading_logged",
                fallback: "Loading capability catalog: model=%@ transport=BLE GATT",
                viewModel.activeModelKey
            )
        )

        Task { [weak self, weak viewModel] in
            await Task.yield()
            guard let self, let viewModel, generation == self.loadGeneration else { return }
            do {
                let snapshot = try viewModel.makeCapabilityCatalogSnapshot()
                guard generation == self.loadGeneration else { return }
                let nextCatalog = buildCapabilityCatalog(snapshot: snapshot)
                self.catalog = nextCatalog
                self.optionsByKey = buildCapabilityOptions(
                    entries: snapshot.entries,
                    labels: snapshot.labels
                )
                self.syncDrafts(entries: snapshot.entries)
                self.applyReadDraftValues(viewModel.latestCapabilityDraftValues)
                self.loadState = .loaded
                viewModel.appendEvent(
                    .sdk,
                    .info,
                    DemoStrings.format(
                        "capability_catalog_loaded_logged",
                        fallback: "Capability catalog loaded: domains=%d visibleEntries=%d",
                        nextCatalog.count,
                        nextCatalog.flatMap(\.items).count
                    )
                )
            } catch {
                guard generation == self.loadGeneration else { return }
                let message = "\(DemoStrings.tr("capability_catalog_load_failed", fallback: "Failed to load the capability catalog.")): \(DemoErrorFormatter.detail(error))"
                self.catalog = []
                self.optionsByKey = [:]
                self.loadState = .failed(message)
                viewModel.appendEvent(.sdk, .error, message)
            }
        }
    }

    func draft(for definition: CapabilityEntry) -> SettingDraft {
        drafts[definition.entryKey] ?? SettingDraft(valueText: definition.defaultValue)
    }

    func updateDraft(_ draft: SettingDraft, for definition: CapabilityEntry) {
        drafts[definition.entryKey] = draft
    }

    func resetDraft(for definition: CapabilityEntry) {
        drafts[definition.entryKey] = SettingDraft(valueText: definition.defaultValue)
    }

    func applyReadDraftValues(_ values: [String: String]) {
        for (key, value) in values where drafts[key] != nil {
            let persist = drafts[key]?.persist ?? false
            drafts[key] = SettingDraft(valueText: value, persist: persist)
        }
    }

    private func syncDrafts(entries: [CapabilityEntry]) {
        let settings = entries.filter { $0.kind == .setting }
        let validKeys = Set(settings.map(\.entryKey))
        drafts = drafts.filter { validKeys.contains($0.key) }
        for definition in settings where drafts[definition.entryKey] == nil {
            drafts[definition.entryKey] = SettingDraft(valueText: definition.defaultValue)
        }
    }
}

func buildCapabilityCatalog(snapshot: CapabilityCatalogSnapshot) -> [CapabilityCatalogDomain] {
    let resolver = CapabilityCatalogLabelResolver(labels: snapshot.labels)
    let visibleEntries = snapshot.entries.filter {
        $0.visibleByDefault && $0.availability == .available
    }

    return snapshot.domains
        .filter(\.visibleByDefault)
        .compactMap { domain -> CapabilityCatalogDomain? in
            let domainEntries = visibleEntries.filter { $0.domainKey == domain.key }
            guard !domainEntries.isEmpty else { return nil }

            let grouped = Dictionary(grouping: domainEntries) {
                CatalogSectionIdentity(familyKey: $0.familyKey, sectionKey: $0.sectionKey)
            }
            let sections = grouped.map { identity, entries in
                CapabilityCatalogSection(
                    familyKey: identity.familyKey,
                    familyTitle: resolver.taxonomyTitle(kind: .family, key: identity.familyKey),
                    sectionKey: identity.sectionKey,
                    sectionTitle: resolver.taxonomyTitle(kind: .section, key: identity.sectionKey),
                    items: entries
                        .sorted {
                            let leftRank = resolver.entryRank($0)
                            let rightRank = resolver.entryRank($1)
                            if leftRank != rightRank { return leftRank < rightRank }
                            let leftTitle = resolver.entryTitle($0)
                            let rightTitle = resolver.entryTitle($1)
                            if leftTitle != rightTitle {
                                return leftTitle.localizedCaseInsensitiveCompare(rightTitle) == .orderedAscending
                            }
                            return $0.entryKey.localizedCaseInsensitiveCompare($1.entryKey) == .orderedAscending
                        }
                        .map {
                            CapabilityCatalogItem(
                                definition: $0,
                                title: resolver.entryTitle($0),
                                familyTitle: resolver.taxonomyTitle(kind: .family, key: identity.familyKey),
                                sectionTitle: resolver.taxonomyTitle(kind: .section, key: identity.sectionKey)
                            )
                        }
                )
            }
            .sorted {
                let leftFamily = resolver.rank(kind: .family, key: $0.familyKey)
                let rightFamily = resolver.rank(kind: .family, key: $1.familyKey)
                if leftFamily != rightFamily { return leftFamily < rightFamily }
                let leftSection = resolver.rank(kind: .section, key: $0.sectionKey)
                let rightSection = resolver.rank(kind: .section, key: $1.sectionKey)
                if leftSection != rightSection { return leftSection < rightSection }
                return $0.id.localizedCaseInsensitiveCompare($1.id) == .orderedAscending
            }

            return CapabilityCatalogDomain(
                definition: domain,
                title: DemoStrings.sdk(domain.localizationKey, fallback: domain.displayName)
                    .ifBlank(readableCapabilityCatalogKey(domain.key)),
                sections: sections
            )
        }
        .sorted {
            if $0.definition.sortOrder != $1.definition.sortOrder {
                return $0.definition.sortOrder < $1.definition.sortOrder
            }
            return $0.title.localizedCaseInsensitiveCompare($1.title) == .orderedAscending
        }
}

func buildCapabilityOptions(
    entries: [CapabilityEntry],
    labels: [CapabilityLabel]
) -> [String: [DemoCapabilityOption]] {
    let resolver = CapabilityCatalogLabelResolver(labels: labels)
    return Dictionary(uniqueKeysWithValues: entries.map { definition in
        let options = definition.options.map { option in
            DemoCapabilityOption(
                rawValue: option.value,
                label: resolver.enumValueTitle(ownerKey: definition.semanticKey, value: option.value)
            )
        }
        return (definition.entryKey, options)
    })
}

private struct CatalogSectionIdentity: Hashable {
    let familyKey: String
    let sectionKey: String
}

private struct CapabilityCatalogLabelResolver {
    private let labels: [CapabilityLabel]

    init(labels: [CapabilityLabel]) {
        self.labels = labels
    }

    func taxonomyTitle(kind: CapabilityLabelKind, key: String) -> String {
        label(kind: kind, key: key)?.localizedTitle
            .ifBlank(readableCapabilityCatalogKey(key))
            ?? readableCapabilityCatalogKey(key)
    }

    func rank(kind: CapabilityLabelKind, key: String) -> UInt32 {
        label(kind: kind, key: key)?.rank ?? UInt32.max
    }

    func entryTitle(_ entry: CapabilityEntry) -> String {
        let kind: CapabilityLabelKind = entry.kind == .action ? .action : .setting
        return label(kind: kind, key: entry.semanticKey)?.localizedTitle
            ?? label(kind: kind, key: entry.entryKey)?.localizedTitle
            ?? readableCapabilityCatalogKey(entry.semanticKey.ifBlank(entry.entryKey))
    }

    func entryRank(_ entry: CapabilityEntry) -> UInt32 {
        let kind: CapabilityLabelKind = entry.kind == .action ? .action : .setting
        return label(kind: kind, key: entry.semanticKey)?.rank
            ?? label(kind: kind, key: entry.entryKey)?.rank
            ?? UInt32.max
    }

    func enumValueTitle(ownerKey: String, value: String) -> String {
        labels.first {
            $0.kind == .enumValue && $0.ownerKey == ownerKey && $0.key == value
        }?.localizedTitle ?? value
    }

    private func label(kind: CapabilityLabelKind, key: String) -> CapabilityLabel? {
        labels.first { $0.kind == kind && $0.key == key && $0.ownerKey.isEmpty }
            ?? labels.first { $0.kind == kind && $0.key == key }
    }
}

private extension CapabilityLabel {
    var localizedTitle: String {
        DemoStrings.sdk(localizationKey, fallback: displayName)
    }
}

private func readableCapabilityCatalogKey(_ key: String) -> String {
    let normalized = key
        .replacingOccurrences(
            of: "([a-z0-9])([A-Z])",
            with: "$1 $2",
            options: .regularExpression
        )
        .replacingOccurrences(of: "_", with: " ")
        .replacingOccurrences(of: "-", with: " ")
        .trimmingCharacters(in: .whitespacesAndNewlines)
    guard !normalized.isEmpty else { return DemoStrings.tr("general", fallback: "General") }
    return normalized
        .split(whereSeparator: \.isWhitespace)
        .map { word in
            let text = String(word)
            return text.prefix(1).uppercased() + text.dropFirst()
        }
        .joined(separator: " ")
}
