package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.CapabilityDomain
import com.netumscan.scannersdk.model.CapabilityEntry
import com.netumscan.scannersdk.model.CapabilityEntryAvailability
import com.netumscan.scannersdk.model.CapabilityEntryKind
import com.netumscan.scannersdk.model.CapabilityLabel
import com.netumscan.scannersdk.model.CapabilityLabelKind

internal data class CapabilityCatalogItem(
    val definition: CapabilityEntry,
    val title: String,
    val familyTitle: String,
    val sectionTitle: String,
)

internal data class CapabilityCatalogSection(
    val familyKey: String,
    val familyTitle: String,
    val sectionKey: String,
    val sectionTitle: String,
    val items: List<CapabilityCatalogItem>,
)

internal data class CapabilityCatalogDomain(
    val definition: CapabilityDomain,
    val title: String,
    val sections: List<CapabilityCatalogSection>,
) {
    val items: List<CapabilityCatalogItem> = sections.flatMap { it.items }
    val readableCount: Int = items.count { it.definition.supportsRead }
    val writableCount: Int = items.count { it.definition.supportsWrite }
    val actionCount: Int = items.count { it.definition.kind == CapabilityEntryKind.ACTION }
}

internal fun buildCapabilityCatalog(
    domains: List<CapabilityDomain>,
    entries: List<CapabilityEntry>,
    labels: List<CapabilityLabel> = emptyList(),
): List<CapabilityCatalogDomain> {
    val resolver = CapabilityLabelResolver(labels)
    val visibleEntries = entries.filter { entry ->
        entry.visibleByDefault && entry.availability == CapabilityEntryAvailability.AVAILABLE
    }

    return domains
        .asSequence()
        .filter { it.visibleByDefault }
        .mapNotNull { domain ->
            val domainEntries = visibleEntries.filter { it.domainKey == domain.key }
            if (domainEntries.isEmpty()) {
                return@mapNotNull null
            }
            val sections = domainEntries
                .groupBy { it.familyKey to it.sectionKey }
                .map { (keys, sectionEntries) ->
                    val (familyKey, sectionKey) = keys
                    CapabilityCatalogSection(
                        familyKey = familyKey,
                        familyTitle = resolver.taxonomyTitle(CapabilityLabelKind.FAMILY, familyKey),
                        sectionKey = sectionKey,
                        sectionTitle = resolver.taxonomyTitle(CapabilityLabelKind.SECTION, sectionKey),
                        items = sectionEntries
                            .sortedWith(
                                compareBy<CapabilityEntry> { resolver.entryRank(it) }
                                    .thenBy { resolver.entryTitle(it) }
                                    .thenBy { it.entryKey }
                            )
                            .map { entry ->
                                CapabilityCatalogItem(
                                    definition = entry,
                                    title = resolver.entryTitle(entry),
                                    familyTitle = resolver.taxonomyTitle(CapabilityLabelKind.FAMILY, familyKey),
                                    sectionTitle = resolver.taxonomyTitle(CapabilityLabelKind.SECTION, sectionKey),
                                )
                            },
                    )
                }
                .sortedWith(
                    compareBy<CapabilityCatalogSection> {
                        resolver.rank(CapabilityLabelKind.FAMILY, it.familyKey)
                    }.thenBy {
                        resolver.rank(CapabilityLabelKind.SECTION, it.sectionKey)
                    }.thenBy { it.familyTitle }
                        .thenBy { it.sectionTitle }
                )

            CapabilityCatalogDomain(
                definition = domain,
                title = SdkLabelResolver.resolve(domain.localizationKey, domain.displayName)
                    .ifBlank { formatSettingGroupTitle(domain.key) },
                sections = sections,
            )
        }
        .sortedWith(compareBy<CapabilityCatalogDomain> { it.definition.sortOrder }.thenBy { it.title })
        .toList()
}

internal fun CapabilityCatalogItem.toProductSettingListItem(
    domain: CapabilityCatalogDomain,
): ProductSettingListItem = ProductSettingListItem(
    definition = definition,
    title = title,
    domainKey = domain.definition.key,
    domainTitle = domain.title,
    familyKey = definition.familyKey,
    familyTitle = familyTitle,
    sectionKey = definition.sectionKey,
    sectionTitle = sectionTitle,
)
