import XCTest
@testable import ScannerSDK
@testable import ScannerSDKiOSDemo

final class CapabilityCatalogSupportTests: XCTestCase {
    func testCatalogMergesSourcesAndUsesSdkTaxonomyOrder() {
        let snapshot = CapabilityCatalogSnapshot(
            domains: [
                domain("system", "System", sortOrder: 20),
                domain("feedback", "Feedback", sortOrder: 10),
            ],
            entries: [
                entry(
                    "setting.AckBeep",
                    domain: "feedback",
                    family: "beep",
                    section: "prompt",
                    source: .master,
                    supportsRead: true,
                    supportsWrite: true
                ),
                entry(
                    "action.Vibrate",
                    domain: "feedback",
                    family: "vibration",
                    section: "prompt",
                    kind: .action,
                    source: .module,
                    supportsExecute: true
                ),
                entry(
                    "setting.WorkMode",
                    domain: "system",
                    family: "mode",
                    section: "general",
                    source: .session,
                    supportsRead: true
                ),
            ],
            labels: [
                label(.family, key: "vibration", title: "Vibration", rank: 10),
                label(.family, key: "beep", title: "Beep", rank: 20),
            ]
        )

        let catalog = buildCapabilityCatalog(snapshot: snapshot)

        XCTAssertEqual(catalog.map(\.definition.key), ["feedback", "system"])
        XCTAssertEqual(catalog[0].sections.map(\.familyKey), ["vibration", "beep"])
        XCTAssertEqual(catalog[0].items.count, 2)
        XCTAssertEqual(catalog[0].readableCount, 1)
        XCTAssertEqual(catalog[0].writableCount, 1)
        XCTAssertEqual(catalog[0].actionCount, 1)
        XCTAssertEqual(Set(catalog[0].items.map(\.definition.source)), Set([.master, .module]))
    }

    func testCatalogFiltersHiddenUnavailableEntriesAndEmptyDomains() {
        let snapshot = CapabilityCatalogSnapshot(
            domains: [
                domain("visible", "Visible", sortOrder: 0),
                domain("empty", "Empty", sortOrder: 1),
                domain("hidden-domain", "Hidden", sortOrder: 2, visible: false),
            ],
            entries: [
                entry("setting.Visible", domain: "visible", family: "general", section: "general"),
                entry(
                    "setting.Hidden",
                    domain: "visible",
                    family: "general",
                    section: "general",
                    visible: false
                ),
                entry(
                    "setting.Unavailable",
                    domain: "visible",
                    family: "general",
                    section: "general",
                    availability: .unavailableInCurrentTransport
                ),
                entry(
                    "setting.HiddenDomain",
                    domain: "hidden-domain",
                    family: "general",
                    section: "general"
                ),
            ],
            labels: []
        )

        let catalog = buildCapabilityCatalog(snapshot: snapshot)

        XCTAssertEqual(catalog.map(\.definition.key), ["visible"])
        XCTAssertEqual(catalog[0].items.map(\.definition.entryKey), ["setting.Visible"])
    }

    private func domain(
        _ key: String,
        _ title: String,
        sortOrder: UInt32,
        visible: Bool = true
    ) -> CapabilityDomain {
        CapabilityDomain(
            key: key,
            displayName: title,
            localizationKey: "test.domain.\(key)",
            sortOrder: sortOrder,
            visibleByDefault: visible
        )
    }

    private func entry(
        _ key: String,
        domain: String,
        family: String,
        section: String,
        kind: CapabilityEntryKind = .setting,
        source: CapabilityEntrySource = .master,
        visible: Bool = true,
        availability: CapabilityEntryAvailability = .available,
        supportsRead: Bool = false,
        supportsWrite: Bool = false,
        supportsExecute: Bool = false
    ) -> CapabilityEntry {
        CapabilityEntry(
            entryKey: key,
            kind: kind,
            domainKey: domain,
            groupKey: family,
            familyKey: family,
            sectionKey: section,
            semanticKey: String(key.split(separator: ".").last ?? Substring(key)),
            defaultValue: "",
            notes: "",
            source: source,
            transportScopes: 1,
            availability: availability,
            routePriority: 0,
            visibleByDefault: visible,
            supportsRead: supportsRead,
            supportsWrite: supportsWrite,
            supportsExecute: supportsExecute,
            requiresValue: false,
            valueKind: kind == .action ? .action : .boolean,
            riskLevel: .normal,
            valueHint: "",
            options: []
        )
    }

    private func label(
        _ kind: CapabilityLabelKind,
        key: String,
        title: String,
        rank: UInt32
    ) -> CapabilityLabel {
        CapabilityLabel(
            kind: kind,
            key: key,
            ownerKey: "",
            displayName: title,
            localizationKey: "test.label.\(key)",
            rank: rank
        )
    }
}
