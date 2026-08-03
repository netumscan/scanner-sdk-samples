package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.CapabilityDomain
import com.netumscan.scannersdk.model.CapabilityEntry
import com.netumscan.scannersdk.model.CapabilityEntryAvailability
import com.netumscan.scannersdk.model.CapabilityEntryKind
import com.netumscan.scannersdk.model.CapabilityEntrySource
import com.netumscan.scannersdk.model.CapabilityLabel
import com.netumscan.scannersdk.model.CapabilityLabelKind
import com.netumscan.scannersdk.model.CapabilityRiskLevel
import com.netumscan.scannersdk.model.CapabilityValueKind
import kotlin.test.Test
import kotlin.test.assertEquals

class CapabilityCatalogSupportTest {
    @Test
    fun buildCatalog_mergesSourcesAndSortsBySdkTaxonomy() {
        val domains = listOf(
            domain("system", "System", sortOrder = 20),
            domain("feedback", "Feedback", sortOrder = 10),
        )
        val entries = listOf(
            entry(
                key = "setting.AckBeep",
                domain = "feedback",
                family = "beep",
                section = "prompt",
                source = CapabilityEntrySource.MASTER,
                supportsRead = true,
                supportsWrite = true,
            ),
            entry(
                key = "action.Vibrate",
                domain = "feedback",
                family = "vibration",
                section = "prompt",
                kind = CapabilityEntryKind.ACTION,
                source = CapabilityEntrySource.MODULE,
                supportsExecute = true,
            ),
            entry(
                key = "setting.WorkMode",
                domain = "system",
                family = "mode",
                section = "general",
                source = CapabilityEntrySource.SESSION,
                supportsRead = true,
            ),
        )
        val labels = listOf(
            label(CapabilityLabelKind.FAMILY, "vibration", "Vibration", rank = 10),
            label(CapabilityLabelKind.FAMILY, "beep", "Beep", rank = 20),
        )

        val catalog = buildCapabilityCatalog(domains, entries, labels)

        assertEquals(listOf("feedback", "system"), catalog.map { it.definition.key })
        assertEquals(listOf("vibration", "beep"), catalog.first().sections.map { it.familyKey })
        assertEquals(2, catalog.first().items.size)
        assertEquals(1, catalog.first().readableCount)
        assertEquals(1, catalog.first().writableCount)
        assertEquals(1, catalog.first().actionCount)
        assertEquals(
            setOf(CapabilityEntrySource.MASTER, CapabilityEntrySource.MODULE),
            catalog.first().items.map { it.definition.source }.toSet(),
        )
    }

    @Test
    fun buildCatalog_filtersHiddenAndUnavailableEntriesAndEmptyDomains() {
        val domains = listOf(
            domain("visible", "Visible", sortOrder = 0),
            domain("empty", "Empty", sortOrder = 1),
            domain("hidden-domain", "Hidden", sortOrder = 2, visible = false),
        )
        val entries = listOf(
            entry("setting.Visible", "visible", "general", "general", visible = true),
            entry("setting.Hidden", "visible", "general", "general", visible = false),
            entry(
                "setting.Unavailable",
                "visible",
                "general",
                "general",
                availability = CapabilityEntryAvailability.UNAVAILABLE_IN_CURRENT_TRANSPORT,
            ),
            entry("setting.HiddenDomain", "hidden-domain", "general", "general"),
        )

        val catalog = buildCapabilityCatalog(domains, entries)

        assertEquals(listOf("visible"), catalog.map { it.definition.key })
        assertEquals(listOf("setting.Visible"), catalog.single().items.map { it.definition.entryKey })
    }

    private fun domain(
        key: String,
        title: String,
        sortOrder: Int,
        visible: Boolean = true,
    ) = CapabilityDomain(
        key = key,
        displayName = title,
        localizationKey = "test.domain.$key",
        sortOrder = sortOrder,
        visibleByDefault = visible,
    )

    private fun entry(
        key: String,
        domain: String,
        family: String,
        section: String,
        kind: CapabilityEntryKind = CapabilityEntryKind.SETTING,
        source: CapabilityEntrySource = CapabilityEntrySource.MASTER,
        visible: Boolean = true,
        availability: CapabilityEntryAvailability = CapabilityEntryAvailability.AVAILABLE,
        supportsRead: Boolean = false,
        supportsWrite: Boolean = false,
        supportsExecute: Boolean = false,
    ) = CapabilityEntry(
        entryKey = key,
        kind = kind,
        domainKey = domain,
        groupKey = family,
        familyKey = family,
        sectionKey = section,
        semanticKey = key.substringAfter('.'),
        defaultValue = "",
        notes = "",
        source = source,
        transportScopes = 1,
        availability = availability,
        routePriority = 0,
        visibleByDefault = visible,
        supportsRead = supportsRead,
        supportsWrite = supportsWrite,
        supportsExecute = supportsExecute,
        requiresValue = false,
        valueKind = if (kind == CapabilityEntryKind.ACTION) CapabilityValueKind.ACTION else CapabilityValueKind.BOOLEAN,
        riskLevel = CapabilityRiskLevel.NORMAL,
        valueHint = "",
    )

    private fun label(
        kind: CapabilityLabelKind,
        key: String,
        title: String,
        rank: Int,
    ) = CapabilityLabel(
        kind = kind,
        key = key,
        ownerKey = "",
        displayName = title,
        localizationKey = "test.label.$key",
        rank = rank,
    )
}
