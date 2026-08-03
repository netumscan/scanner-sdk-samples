package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.CapabilityEntry
import com.netumscan.scannersdk.model.CapabilityEntrySource
import com.netumscan.scannersdk.model.CapabilityLabel
import com.netumscan.scannersdk.model.CapabilityLabelKind
import com.netumscan.scannersdk.model.CapabilityValue
import com.netumscan.scannersdk.model.CapabilityValueKind
import kotlin.math.min

internal data class ProductSettingDraft(
    val valueText: String = "",
    val persist: Boolean = false,
)

internal data class ProductSettingOption(
    val rawValue: String,
    val label: String,
)

internal data class ProductSettingItem(
    val definition: CapabilityEntry,
    val title: String,
)

internal data class ProductSettingSection(
    val key: String,
    val title: String,
    val items: List<ProductSettingItem>,
)

internal data class ProductSettingFamily(
    val key: String,
    val title: String,
    val sections: List<ProductSettingSection>,
)

internal data class ProductSettingDomain(
    val key: String,
    val title: String,
    val families: List<ProductSettingFamily>,
)

internal data class ProductSettingGroup(
    val key: String,
    val title: String,
    val domains: List<ProductSettingDomain>,
)

internal fun groupProductSettings(
    definitions: List<CapabilityEntry>,
    labels: List<CapabilityLabel> = emptyList(),
): List<ProductSettingGroup> {
    val resolver = CapabilityLabelResolver(labels)
    return definitions
        .sortedWith(
            compareBy<CapabilityEntry>(
                { settingSourceRank(it.source) },
                { it.domainKey },
                { it.familyKey },
                { it.sectionKey },
                { resolver.entryTitle(it) },
                { it.entryKey },
            )
        )
        .groupBy { it.source }
        .map { (source, sourceDefinitions) ->
            ProductSettingGroup(
                key = source.name.lowercase(),
                title = formatSettingSourceTitle(source),
                domains = sourceDefinitions
                    .groupBy { it.domainKey }
                    .map { (domainKey, domainDefinitions) ->
                        ProductSettingDomain(
                            key = domainKey,
                            title = resolver.taxonomyTitle(CapabilityLabelKind.DOMAIN, domainKey),
                            families = domainDefinitions
                                .groupBy { it.familyKey }
                                .map { (familyKey, familyDefinitions) ->
                                    ProductSettingFamily(
                                        key = familyKey,
                                        title = resolver.taxonomyTitle(CapabilityLabelKind.FAMILY, familyKey),
                                        sections = familyDefinitions
                                            .groupBy { it.sectionKey }
                                            .map { (sectionKey, sectionDefinitions) ->
                                                ProductSettingSection(
                                                    key = sectionKey,
                                                    title = resolver.taxonomyTitle(CapabilityLabelKind.SECTION, sectionKey),
                                                    items = sectionDefinitions.map { definition ->
                                                        ProductSettingItem(definition, resolver.entryTitle(definition))
                                                    },
                                                )
                                            }
                                            .sortedWith(compareBy<ProductSettingSection> { resolver.rank(CapabilityLabelKind.SECTION, it.key) }.thenBy { it.title }),
                                    )
                                }
                                .sortedWith(compareBy<ProductSettingFamily> { resolver.rank(CapabilityLabelKind.FAMILY, it.key) }.thenBy { it.title }),
                        )
                    }
                    .sortedWith(compareBy<ProductSettingDomain> { resolver.rank(CapabilityLabelKind.DOMAIN, it.key) }.thenBy { it.title }),
            )
        }
        .sortedBy { if (it.key == CapabilityEntrySource.MASTER.name.lowercase()) 0 else 1 }
}

internal class CapabilityLabelResolver(labels: List<CapabilityLabel>) {
    private val byKindAndKey = labels.associateBy { it.kind to it.key }
    private val byKindOwnerAndKey = labels.associateBy { Triple(it.kind, it.ownerKey, it.key) }

    fun taxonomyTitle(kind: CapabilityLabelKind, key: String): String {
        val label = byKindAndKey[kind to key]
        return label?.let { SdkLabelResolver.resolve(it.localizationKey, it.displayName) }?.ifBlank { null }
            ?: formatSettingGroupTitle(key)
    }

    fun rank(kind: CapabilityLabelKind, key: String): Int =
        byKindAndKey[kind to key]?.rank ?: Int.MAX_VALUE

    fun entryTitle(entry: CapabilityEntry): String {
        val kind = when (entry.kind) {
            com.netumscan.scannersdk.model.CapabilityEntryKind.ACTION -> CapabilityLabelKind.ACTION
            com.netumscan.scannersdk.model.CapabilityEntryKind.SETTING -> CapabilityLabelKind.SETTING
        }
        val label = byKindOwnerAndKey[Triple(kind, "", entry.semanticKey)]
            ?: byKindAndKey[kind to entry.semanticKey]
        return label?.let { SdkLabelResolver.resolve(it.localizationKey, it.displayName) }?.ifBlank { null }
            ?: entry.semanticKey.ifBlank { entry.entryKey }
    }

    fun entryRank(entry: CapabilityEntry): Int {
        val kind = when (entry.kind) {
            com.netumscan.scannersdk.model.CapabilityEntryKind.ACTION -> CapabilityLabelKind.ACTION
            com.netumscan.scannersdk.model.CapabilityEntryKind.SETTING -> CapabilityLabelKind.SETTING
        }
        return (
            byKindOwnerAndKey[Triple(kind, "", entry.semanticKey)]
                ?: byKindAndKey[kind to entry.semanticKey]
            )?.rank ?: Int.MAX_VALUE
    }

    fun enumValueTitle(ownerKey: String, value: String): String {
        val label = byKindOwnerAndKey[Triple(CapabilityLabelKind.ENUM_VALUE, ownerKey, value)]
        return label?.let { SdkLabelResolver.resolve(it.localizationKey, it.displayName) }?.ifBlank { null }
            ?: value
    }
}

private fun settingSourceRank(source: CapabilityEntrySource): Int = when (source) {
    CapabilityEntrySource.MASTER -> 0
    CapabilityEntrySource.MODULE -> 1
    CapabilityEntrySource.SESSION -> 2
}

private fun formatSettingSourceTitle(source: CapabilityEntrySource): String = when (source) {
    CapabilityEntrySource.MASTER -> "Master Capabilities"
    CapabilityEntrySource.MODULE -> "Module Capabilities"
    CapabilityEntrySource.SESSION -> "Session Capabilities"
}

internal fun formatSettingGroupTitle(key: String): String {
    if (key.isBlank()) {
        return "General"
    }
    if (key == "output") {
        return "Output & Format"
    }
    val normalized = key
        .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
        .replace('_', ' ')
        .replace('-', ' ')
        .trim()
    if (normalized.isBlank()) {
        return key
    }
    return normalized
        .split(Regex("\\s+"))
        .joinToString(" ") { part ->
            part.lowercase().replaceFirstChar { first -> first.uppercase() }
        }
}

internal fun productSettingOptions(
    definition: CapabilityEntry,
    labels: List<CapabilityLabel> = emptyList(),
): List<ProductSettingOption> {
    val resolver = CapabilityLabelResolver(labels)
    return definition.options.map { option ->
        ProductSettingOption(
            rawValue = option.value,
            label = resolver.enumValueTitle(definition.semanticKey, option.value),
        )
    }
}

internal fun defaultDraftForSetting(
    definition: CapabilityEntry,
    options: List<ProductSettingOption> = emptyList(),
    currentValue: CapabilityValue? = null,
    previousPersist: Boolean = false,
): ProductSettingDraft {
    val valueText = currentValue?.let { draftTextFromValue(definition, it, options) }
        ?: definition.defaultValue
    return ProductSettingDraft(valueText = valueText, persist = previousPersist)
}

internal fun draftTextFromValue(
    definition: CapabilityEntry,
    value: CapabilityValue,
    options: List<ProductSettingOption> = emptyList(),
): String {
    return when (definition.valueKind) {
        CapabilityValueKind.BOOLEAN -> if (value.booleanValue == true) "true" else "false"
        CapabilityValueKind.BYTES_ASCII -> value.textValue ?: value.bytes.decodeToString()
        CapabilityValueKind.UINT8,
        CapabilityValueKind.UINT16 -> decodeUnsignedValue(value.bytes).toString()
        CapabilityValueKind.ENUM -> enumOptionForValue(value, options)?.rawValue ?: value.bytes.toSpacedHex()
        CapabilityValueKind.ACTION -> ""
        else -> value.bytes.toSpacedHex()
    }
}

internal fun enumOptionForValue(
    value: CapabilityValue,
    options: List<ProductSettingOption>,
): ProductSettingOption? {
    return options.firstOrNull { option ->
        parseOptionBytes(option.rawValue)?.contentEquals(value.bytes) == true
    }
}

internal fun encodeSettingDraft(
    definition: CapabilityEntry,
    draft: ProductSettingDraft,
    options: List<ProductSettingOption> = emptyList(),
): CapabilityValue {
    val text = draft.valueText.trim()
    return when (definition.valueKind) {
        CapabilityValueKind.BOOLEAN -> {
            val booleanValue = when (text.lowercase()) {
                "true", "1", "on", "enable", "enabled", "yes" -> true
                "false", "0", "off", "disable", "disabled", "no" -> false
                else -> throw IllegalArgumentException(DemoStrings.text(R.string.setting_validation_boolean))
            }
            CapabilityValue.fromBoolean(booleanValue)
        }

        CapabilityValueKind.ENUM -> {
            val optionBytes = if (options.isEmpty()) {
                parseOptionBytes(text)
                    ?: throw IllegalArgumentException(DemoStrings.text(R.string.setting_validation_enum_format))
            } else {
                parseOptionBytes(text)
                    ?: throw IllegalArgumentException(DemoStrings.text(R.string.setting_validation_enum_select))
            }
            CapabilityValue.fromBytes(definition.valueKind, optionBytes)
        }

        CapabilityValueKind.UINT8 -> {
            val value = parseUnsignedInt(text, 0, 0xFF)
            CapabilityValue.fromBytes(definition.valueKind, byteArrayOf((value and 0xFF).toByte()))
        }

        CapabilityValueKind.UINT16 -> {
            val value = parseUnsignedInt(text, 0, 0xFFFF)
            CapabilityValue.fromBytes(
                definition.valueKind,
                byteArrayOf(((value ushr 8) and 0xFF).toByte(), (value and 0xFF).toByte()),
            )
        }

        CapabilityValueKind.BYTES_ASCII -> CapabilityValue.fromAsciiText(draft.valueText)
        CapabilityValueKind.ACTION -> CapabilityValue.fromBytes(definition.valueKind, byteArrayOf())
        else -> CapabilityValue.fromBytes(definition.valueKind, parseHexBytes(text))
    }
}

internal fun validateSettingDraft(
    definition: CapabilityEntry,
    draft: ProductSettingDraft,
    options: List<ProductSettingOption> = emptyList(),
): String? {
    return runCatching {
        encodeSettingDraft(definition, draft, options)
    }.exceptionOrNull()?.message
}

internal fun formatCapabilityValueSummary(
    definition: CapabilityEntry,
    value: CapabilityValue,
    options: List<ProductSettingOption> = emptyList(),
): String {
    return when (definition.valueKind) {
        CapabilityValueKind.BOOLEAN -> if (value.booleanValue == true) {
            DemoStrings.text(R.string.setting_enabled)
        } else {
            DemoStrings.text(R.string.setting_disabled)
        }
        CapabilityValueKind.ENUM -> {
            val option = enumOptionForValue(value, options)
            if (option != null) {
                option.label
            } else {
                value.bytes.toSpacedHex()
            }
        }

        CapabilityValueKind.UINT8,
        CapabilityValueKind.UINT16 -> {
            val number = decodeUnsignedValue(value.bytes)
            "$number (${value.bytes.toSpacedHex()})"
        }

        CapabilityValueKind.BYTES_ASCII -> value.textValue ?: value.bytes.decodeToString()
        CapabilityValueKind.ACTION -> DemoStrings.text(R.string.setting_action_write_only)
        else -> value.bytes.toSpacedHex().ifBlank { DemoStrings.text(R.string.setting_empty_value) }
    }
}

internal fun formatSettingDefaultSummary(
    definition: CapabilityEntry,
    options: List<ProductSettingOption> = emptyList(),
): String {
    val rawValue = definition.defaultValue
    if (rawValue.isBlank()) {
        return DemoStrings.text(R.string.setting_default_not_provided)
    }
    return when (definition.valueKind) {
        CapabilityValueKind.BOOLEAN -> when (rawValue.trim().lowercase()) {
            "true", "1", "on", "enable", "enabled", "yes" -> DemoStrings.text(R.string.setting_enabled)
            "false", "0", "off", "disable", "disabled", "no" -> DemoStrings.text(R.string.setting_disabled)
            else -> rawValue
        }

        CapabilityValueKind.ENUM -> options.firstOrNull { it.rawValue == rawValue }?.label ?: rawValue
        else -> rawValue
    }
}

internal fun parseHexBytes(text: String): ByteArray {
    val normalized = text.filterNot(Char::isWhitespace)
    require(normalized.isNotBlank()) { DemoStrings.text(R.string.setting_validation_hex_empty) }
    require(normalized.length % 2 == 0) { DemoStrings.text(R.string.setting_validation_hex_even) }
    return ByteArray(normalized.length / 2) { index ->
        val start = index * 2
        val byteText = normalized.substring(start, start + 2)
        byteText.toIntOrNull(16)?.toByte()
            ?: throw IllegalArgumentException(DemoStrings.text(R.string.setting_validation_hex_invalid, byteText))
    }
}

internal fun ByteArray.toSpacedHex(limit: Int = size): String {
    return take(min(size, limit)).joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
}

private fun parseUnsignedInt(text: String, min: Int, max: Int): Int {
    if (text.isBlank()) {
        throw IllegalArgumentException(
            when (max) {
                0xFF -> DemoStrings.text(R.string.setting_validation_uint8)
                0xFFFF -> DemoStrings.text(R.string.setting_validation_uint16)
                else -> DemoStrings.text(R.string.setting_validation_number_generic, min, max)
            }
        )
    }
    val value = text.toIntOrNull()
        ?: throw IllegalArgumentException(
            when (max) {
                0xFF -> DemoStrings.text(R.string.setting_validation_uint8)
                0xFFFF -> DemoStrings.text(R.string.setting_validation_uint16)
                else -> DemoStrings.text(R.string.setting_validation_number_generic, min, max)
            }
        )
    require(value in min..max) {
        when (max) {
            0xFF -> DemoStrings.text(R.string.setting_validation_uint8)
            0xFFFF -> DemoStrings.text(R.string.setting_validation_uint16)
            else -> DemoStrings.text(R.string.setting_validation_number_generic, min, max)
        }
    }
    return value
}

private fun decodeUnsignedValue(bytes: ByteArray): Int {
    require(bytes.isNotEmpty()) { "numeric value bytes must not be empty" }
    return bytes.fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) }
}

private fun parseOptionBytes(rawValue: String): ByteArray? {
    val trimmed = rawValue.trim()
    if (trimmed.isBlank()) {
        return null
    }
    if (trimmed.startsWith("0x", ignoreCase = true)) {
        val hex = trimmed.removePrefix("0x").removePrefix("0X")
        val normalized = if (hex.length % 2 == 0) hex else "0$hex"
        return parseHexBytes(normalized)
    }
    return trimmed.toIntOrNull()?.let { value ->
        when {
            value in 0..0xFF -> byteArrayOf((value and 0xFF).toByte())
            value in 0x100..0xFFFF -> byteArrayOf(((value ushr 8) and 0xFF).toByte(), (value and 0xFF).toByte())
            else -> null
        }
    } ?: trimmed.encodeToByteArray()
}
