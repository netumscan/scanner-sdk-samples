package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.CapabilityEntry
import com.netumscan.scannersdk.model.CapabilityEntryAvailability
import com.netumscan.scannersdk.model.CapabilityEntryKind
import com.netumscan.scannersdk.model.CapabilityEntrySource
import com.netumscan.scannersdk.model.CapabilityLabel
import com.netumscan.scannersdk.model.CapabilityLabelKind
import com.netumscan.scannersdk.model.CapabilityOption
import com.netumscan.scannersdk.model.CapabilityRiskLevel
import com.netumscan.scannersdk.model.CapabilityValue
import com.netumscan.scannersdk.model.CapabilityValueKind
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProductSettingsSupportTest {
    @Test
    fun groupProductSettings_groupsBySourceDomainFamilyAndSection() {
        val definitions = listOf(
            settingDefinition("QrCodeEnable", "QR Code", "symbology", "qr_code", "basic"),
            settingDefinition("Code39Enable", "Code 39", "symbology", "code39", "basic"),
            settingDefinition("PrefixData", "Prefix Data", "data_edit", "prefix", "value"),
            settingDefinition(
                "MasterPrefix",
                "Master Prefix",
                domainKey = "data_edit",
                familyKey = "prefix",
                sectionKey = "value",
                source = CapabilityEntrySource.MASTER,
            ),
            settingDefinition(
                "BluetoothName",
                "Bluetooth Name",
                domainKey = "connectivity",
                familyKey = "bluetooth",
                sectionKey = "identity",
                source = CapabilityEntrySource.MASTER,
            ),
        )

        val labels = listOf(
            label(CapabilityLabelKind.DOMAIN, "connectivity", "Connectivity", rank = 0),
            label(CapabilityLabelKind.DOMAIN, "data_edit", "Data Edit", rank = 1),
            label(CapabilityLabelKind.DOMAIN, "symbology", "Symbology", rank = 2),
            label(CapabilityLabelKind.FAMILY, "bluetooth", "Bluetooth"),
            label(CapabilityLabelKind.FAMILY, "prefix", "Prefix"),
            label(CapabilityLabelKind.FAMILY, "qr_code", "QR Code"),
            label(CapabilityLabelKind.FAMILY, "code39", "Code39"),
            label(CapabilityLabelKind.SECTION, "basic", "Basic"),
            label(CapabilityLabelKind.SECTION, "identity", "Identity"),
            label(CapabilityLabelKind.SECTION, "value", "Value"),
            label(CapabilityLabelKind.SETTING, "BluetoothName", "Bluetooth Name"),
        )

        val groups = groupProductSettings(definitions, labels)

        assertEquals(listOf("Master Capabilities", "Module Capabilities"), groups.map { it.title })
        assertEquals(listOf("Connectivity", "Data Edit"), groups.first().domains.map { it.title })
        assertEquals(listOf("Bluetooth"), groups.first().domains.first().families.map { it.title })
        assertEquals(listOf("Prefix"), groups.first().domains.last().families.map { it.title })
        assertEquals(listOf("Data Edit", "Symbology"), groups.last().domains.map { it.title })
        assertEquals(listOf("Prefix"), groups.last().domains.first().families.map { it.title })
        assertEquals(listOf("Code39", "QR Code"), groups.last().domains.last().families.map { it.title })
        assertEquals("Bluetooth Name", groups.first().domains.first().families.first().sections.first().items.first().title)
    }

    @Test
    fun productSettingOptions_usesLocalizedLabels() {
        val definition = settingDefinition(
            "QrCodeEnable",
            "QR Code",
            valueKind = CapabilityValueKind.ENUM,
            options = listOf(CapabilityOption(value = "0x00"), CapabilityOption(value = "0x01")),
        )
        val labels = listOf(
            label(CapabilityLabelKind.ENUM_VALUE, "0x00", "Disable", ownerKey = "QrCodeEnable"),
            label(CapabilityLabelKind.ENUM_VALUE, "0x01", "Enable", ownerKey = "QrCodeEnable"),
        )
        val options = productSettingOptions(
            definition,
            labels,
        )

        assertEquals(2, options.size)
        assertEquals("0x00", options[0].rawValue)
        assertEquals("Disable", options[0].label)
        assertEquals("0x01", options[1].rawValue)
        assertEquals("Enable", options[1].label)
    }

    @Test
    fun productSettingOptions_fallsBackToValue() {
        val definition = settingDefinition(
            "WorkMode",
            "Work Mode",
            valueKind = CapabilityValueKind.ENUM,
            options = listOf(CapabilityOption(value = "Store"), CapabilityOption(value = "Normal")),
        )
        val options = productSettingOptions(definition)

        assertEquals(2, options.size)
        assertEquals("Store", options[0].rawValue)
        assertEquals("Store", options[0].label)
        assertEquals("Normal", options[1].rawValue)
        assertEquals("Normal", options[1].label)
    }

    @Test
    fun encodeSettingDraft_supportsBooleanAndNumbersAndAsciiAndHex() {
        val booleanDefinition = settingDefinition("QrCodeEnable", "QR", valueKind = CapabilityValueKind.BOOLEAN)
        val uint8Definition = settingDefinition("Code39Len", "Code39 Len", valueKind = CapabilityValueKind.UINT8)
        val uint16Definition = settingDefinition("Delay", "Delay", valueKind = CapabilityValueKind.UINT16)
        val asciiDefinition = settingDefinition("PrefixData", "Prefix", valueKind = CapabilityValueKind.BYTES_ASCII)
        val templateDefinition = settingDefinition("Template", "Template", valueKind = CapabilityValueKind.TEMPLATE)
        val asciiEnumDefinition = settingDefinition(
            "WorkMode",
            "Work Mode",
            valueKind = CapabilityValueKind.ENUM,
        )
        val asciiEnumOptions = listOf(ProductSettingOption("Store", "Store"))

        assertEquals(true, encodeSettingDraft(booleanDefinition, ProductSettingDraft("true")).booleanValue)
        assertContentEquals(byteArrayOf(0x0A), encodeSettingDraft(uint8Definition, ProductSettingDraft("10")).bytes)
        assertContentEquals(byteArrayOf(0x01, 0xF4.toByte()), encodeSettingDraft(uint16Definition, ProductSettingDraft("500")).bytes)
        assertEquals("ABC", encodeSettingDraft(asciiDefinition, ProductSettingDraft("ABC")).textValue)
        assertContentEquals(byteArrayOf(0x01, 0x0D, 0x0A), encodeSettingDraft(templateDefinition, ProductSettingDraft("01 0D 0A")).bytes)
        assertContentEquals(
            "Store".encodeToByteArray(),
            encodeSettingDraft(asciiEnumDefinition, ProductSettingDraft("Store"), asciiEnumOptions).bytes,
        )
    }

    @Test
    fun encodeSettingDraft_usesSelectedRawValueFromValueLabelOptions() {
        val enumDefinition = settingDefinition(
            "DeviceCharset",
            "Device Charset",
            valueKind = CapabilityValueKind.ENUM,
        )
        val options = listOf(ProductSettingOption("Utf8Txt", "UTF-8 Text"))

        assertContentEquals(
            "Utf8Txt".encodeToByteArray(),
            encodeSettingDraft(enumDefinition, ProductSettingDraft("Utf8Txt"), options).bytes,
        )
    }

    @Test
    fun defaultDraftForSetting_prefersReadValueAndPreservesPersist() {
        val definition = settingDefinition("QrCodeEnable", "QR", valueKind = CapabilityValueKind.BOOLEAN, defaultValue = "false")
        val draft = defaultDraftForSetting(
            definition = definition,
            currentValue = CapabilityValue.fromBoolean(true),
            previousPersist = true,
        )

        assertEquals("true", draft.valueText)
        assertTrue(draft.persist)
    }

    @Test
    fun formatCapabilityValueSummary_usesOptionLabelsAndWriteOnlyHint() {
        val enumDefinition = settingDefinition(
            "AckMode",
            "Ack",
            valueKind = CapabilityValueKind.ENUM,
        )
        val options = listOf(
            ProductSettingOption("0x00", "Disable"),
            ProductSettingOption("0x01", "Enable"),
        )
        val actionDefinition = settingDefinition("FactoryReset", "Factory Reset", valueKind = CapabilityValueKind.ACTION)

        assertEquals(
            "Enable",
            formatCapabilityValueSummary(enumDefinition, CapabilityValue.fromBytes(CapabilityValueKind.ENUM, byteArrayOf(0x01)), options),
        )
        assertEquals(
            DemoStrings.text(R.string.setting_action_write_only),
            formatCapabilityValueSummary(actionDefinition, CapabilityValue.fromBytes(CapabilityValueKind.ACTION, byteArrayOf())),
        )
    }

    @Test
    fun formatSettingDefaultSummary_usesLocalizedBooleanAndEnumLabels() {
        val booleanDefinition = settingDefinition(
            "QrCodeEnable",
            "QR",
            valueKind = CapabilityValueKind.BOOLEAN,
            defaultValue = "true",
        )
        val enumDefinition = settingDefinition(
            "AckMode",
            "Ack",
            valueKind = CapabilityValueKind.ENUM,
            defaultValue = "0x01",
        )
        val options = listOf(
            ProductSettingOption("0x00", "Disable"),
            ProductSettingOption("0x01", "Enable"),
        )

        DemoLocaleController.setLanguageForTest(DemoLanguage.ZH)
        assertEquals("已开启", formatSettingDefaultSummary(booleanDefinition))
        assertEquals("Enable", formatSettingDefaultSummary(enumDefinition, options))

        DemoLocaleController.setLanguageForTest(DemoLanguage.EN)
        assertEquals("Enabled", formatSettingDefaultSummary(booleanDefinition))
    }

    @Test
    fun formatSettingGroupTitle_handlesUnderscoreAndCamelCase() {
        assertEquals("Qr Code", formatSettingGroupTitle("qr_code"))
        assertEquals("Device Setting", formatSettingGroupTitle("deviceSetting"))
        assertEquals("General", formatSettingGroupTitle(""))
    }

    @Test
    fun helperFlagsReflectReadWriteSupport() {
        val readOnly = settingDefinition("ReadOnly", "ReadOnly", supportsRead = true, supportsWrite = false)
        val writeOnly = settingDefinition("WriteOnly", "WriteOnly", supportsRead = false, supportsWrite = true)

        assertTrue(readOnly.supportsRead)
        assertFalse(readOnly.supportsWrite)
        assertFalse(writeOnly.supportsRead)
        assertTrue(writeOnly.supportsWrite)
    }

    private fun settingDefinition(
        key: String,
        displayName: String,
        domainKey: String = "scan",
        familyKey: String = "general",
        sectionKey: String = "basic",
        valueKind: CapabilityValueKind = CapabilityValueKind.BOOLEAN,
        supportsRead: Boolean = true,
        supportsWrite: Boolean = true,
        defaultValue: String = "",
        source: CapabilityEntrySource = CapabilityEntrySource.MODULE,
        options: List<CapabilityOption> = emptyList(),
    ): CapabilityEntry {
        return CapabilityEntry(
            entryKey = "setting.$key",
            kind = CapabilityEntryKind.SETTING,
            domainKey = domainKey,
            groupKey = "group",
            familyKey = familyKey,
            sectionKey = sectionKey,
            semanticKey = key,
            defaultValue = defaultValue,
            notes = "",
            source = source,
            transportScopes = 0,
            availability = CapabilityEntryAvailability.AVAILABLE,
            routePriority = 0,
            visibleByDefault = true,
            valueKind = valueKind,
            supportsRead = supportsRead,
            supportsWrite = supportsWrite,
            supportsExecute = false,
            requiresValue = false,
            riskLevel = CapabilityRiskLevel.NORMAL,
            valueHint = "",
            options = options,
        )
    }

    private fun label(
        kind: CapabilityLabelKind,
        key: String,
        displayName: String,
        rank: Int = 0,
        ownerKey: String = "",
    ): CapabilityLabel =
        CapabilityLabel(
            kind = kind,
            key = key,
            ownerKey = ownerKey,
            displayName = displayName,
            localizationKey = "",
            rank = rank,
        )
}
