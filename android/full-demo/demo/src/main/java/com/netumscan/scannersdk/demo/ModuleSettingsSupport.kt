package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.ModuleEnumOption
import com.netumscan.scannersdk.model.ModuleFamily
import com.netumscan.scannersdk.model.ModuleParameterCatalog
import com.netumscan.scannersdk.model.ModuleParameterDefinition
import com.netumscan.scannersdk.model.ModuleParameterUiKind
import com.netumscan.scannersdk.model.ModuleQuickValueOption
import com.netumscan.scannersdk.model.ModuleNumericInputKind
import com.netumscan.scannersdk.model.ModuleNumericInputSpec
import com.netumscan.scannersdk.model.ModuleTaxonomyCatalog
import com.netumscan.scannersdk.model.ModuleTaxonomyKind
import com.netumscan.scannersdk.model.Ntc06hSettingDefinition
import com.netumscan.scannersdk.model.booleanPayloadPair
import com.netumscan.scannersdk.model.defaultPayloadBytes
import com.netumscan.scannersdk.model.enumOptions
import com.netumscan.scannersdk.model.localizedLabel
import com.netumscan.scannersdk.model.localizedTitleLabel
import com.netumscan.scannersdk.model.numericInputSpec
import com.netumscan.scannersdk.model.quickValueOptions
import com.netumscan.scannersdk.model.toHexEditorText
import java.nio.charset.StandardCharsets

data class ModulePresetTaxonomy(
    val domainKey: String,
    val familyKey: String,
    val sectionKey: String,
) {
    fun domainTitle(): String = ModuleSettingsCatalog.domainTitleForKey(domainKey)
    fun familyTitle(): String = ModuleSettingsCatalog.familyTitleForKey(familyKey)
    fun sectionTitle(): String = ModuleSettingsCatalog.sectionTitleForKey(sectionKey)
}

fun ModuleQuickValueOption.label(): String = SdkLabelResolver.resolve(localizationKey, fallbackDisplayName)

fun ModuleEnumOption.label(): String = SdkLabelResolver.resolve(localizationKey, fallbackDisplayName)

fun ModuleNumericInputSpec.label(): String = SdkLabelResolver.resolve(localizationKey, fallbackDisplayName)

fun ModuleNumericInputSpec.rangeHint(): String = DemoStrings.format(R.string.module_range_hint, minValue, maxValue, SdkLabelResolver.resolve(stepHintKey, stepHintFallback))

fun Ntc06hSettingDefinition.localizedTitle(): String {
    val label = localizedTitleLabel()
    return SdkLabelResolver.resolve(label.localizationKey, label.fallbackDisplayName)
}

data class ModuleSettingPreset(
    val definition: ModuleParameterDefinition,
) {
    val parameterId: Int
        get() = definition.parameterId

    val kind: ModuleParameterUiKind
        get() = definition.kind

    val writeOnHex: String
        get() = booleanPayloadPair()?.second ?: "01"

    val writeOffHex: String
        get() = booleanPayloadPair()?.first ?: "00"

    val groupKey: String
        get() = definition.group

    val defaultPayloadHex: String?
        get() = definition.defaultPayloadBytes()?.toHexEditorText()

    val supportsBooleanToggle: Boolean
        get() = kind == ModuleParameterUiKind.BOOL

    val enumOptions: List<ModuleEnumOption>
        get() = if (kind == ModuleParameterUiKind.ENUM) {
            definition.enumOptions()
        } else {
            emptyList()
        }

    val quickValueOptions: List<ModuleQuickValueOption>
        get() = definition.quickValueOptions()

    val numericInputSpec: ModuleNumericInputSpec?
        get() = definition.numericInputSpec()

    val taxonomy: ModulePresetTaxonomy
        get() = ModuleSettingsCatalog.taxonomyFor(definition)

    fun title(): String = moduleTitle(definition)

    fun hint(): String {
        val kindLabel = moduleKindLabel(kind)
        val defaultValue = definition.defaultValue.ifBlank {
            DemoStrings.text(R.string.module_default_not_provided)
        }
        val quickHint = quickValueOptions.takeIf { it.isNotEmpty() }?.joinToString(separator = " / ") { it.label() }
        val base = DemoStrings.format(R.string.module_parameter_hint_format, taxonomy.domainTitle(), taxonomy.familyTitle(), taxonomy.sectionTitle(), kindLabel, defaultValue)
        return if (quickHint.isNullOrBlank()) {
            base
        } else {
            DemoStrings.format(R.string.module_parameter_hint_quick_values_format, base, quickHint)
        }
    }

    private fun booleanPayloadPair(): Pair<String, String>? {
        return definition.booleanPayloadPair()
    }
}

fun moduleNumericInputError(spec: ModuleNumericInputSpec, text: String): String? {
    if (text.isBlank()) {
        return null
    }
    val value = text.toIntOrNull()
        ?: return DemoStrings.text(R.string.enter_decimal_integer)
    if (value !in spec.minValue..spec.maxValue) {
        return DemoStrings.format(R.string.value_out_of_range, spec.minValue, spec.maxValue)
    }
    return null
}

data class ModuleSectionGroup(
    val key: String,
    val presets: List<ModuleSettingPreset>,
) {
    fun title(): String = ModuleSettingsCatalog.sectionTitleForKey(key)
}

data class ModuleFamilyGroup(
    val key: String,
    val sections: List<ModuleSectionGroup>,
) {
    fun title(): String = ModuleSettingsCatalog.familyTitleForKey(key)
}

data class ModuleDomainGroup(
    val key: String,
    val families: List<ModuleFamilyGroup>,
) {
    fun title(): String = ModuleSettingsCatalog.domainTitleForKey(key)
}

object ModuleSettingsCatalog {
    private val nt212xAllPresets: List<ModuleSettingPreset> = ModuleParameterCatalog.allFor(ModuleFamily.NT212X)
        .sortedWith(compareBy({ taxonomyRank(ModuleTaxonomyKind.GROUP, it.group) }, { it.group }, { it.parameterId }))
        .map(::ModuleSettingPreset)

    private val nt280hAllPresets: List<ModuleSettingPreset> = ModuleParameterCatalog.allFor(ModuleFamily.NT280H)
        .filterNot { it.isPlaceholder }
        .sortedWith(compareBy({ taxonomyRank(ModuleTaxonomyKind.GROUP, it.group) }, { it.group }, { it.parameterId }))
        .map(::ModuleSettingPreset)

    private val se4750AllPresets: List<ModuleSettingPreset> = ModuleParameterCatalog.allFor(ModuleFamily.SE4750)
        .sortedWith(compareBy({ taxonomyRank(ModuleTaxonomyKind.GROUP, it.group) }, { it.group }, { it.parameterId }))
        .map(::ModuleSettingPreset)

    fun presetsFor(moduleFamily: ModuleFamily): List<ModuleSettingPreset> = when (moduleFamily) {
        ModuleFamily.NT212X -> nt212xAllPresets
        ModuleFamily.NT280H -> nt280hAllPresets
        ModuleFamily.SE4750 -> se4750AllPresets
        else -> emptyList()
    }

    fun domainGroupsFor(moduleFamily: ModuleFamily): List<ModuleDomainGroup> {
            val presetsByDomain = presetsFor(moduleFamily).groupBy { it.taxonomy.domainKey }
            val sortedDomainKeys = ModuleTaxonomyCatalog.sortKeys(ModuleTaxonomyKind.DOMAIN, presetsByDomain.keys)
            return sortedDomainKeys.map { domainKey ->
                val domainPresets = presetsByDomain.getValue(domainKey)
                val presetsByFamily = domainPresets.groupBy { it.taxonomy.familyKey }
                val sortedFamilyKeys = ModuleTaxonomyCatalog.sortKeys(ModuleTaxonomyKind.FAMILY, presetsByFamily.keys)
                ModuleDomainGroup(
                    key = domainKey,
                    families = sortedFamilyKeys.map { familyKey ->
                        val familyPresets = presetsByFamily.getValue(familyKey)
                        val presetsBySection = familyPresets.groupBy { it.taxonomy.sectionKey }
                        val sortedSectionKeys = ModuleTaxonomyCatalog.sortKeys(ModuleTaxonomyKind.SECTION, presetsBySection.keys)
                        ModuleFamilyGroup(
                            key = familyKey,
                            sections = sortedSectionKeys.map { sectionKey ->
                                ModuleSectionGroup(
                                    key = sectionKey,
                                    presets = presetsBySection.getValue(sectionKey),
                                )
                            }
                        )
                    }
                )
            }
        }

    fun groupTitle(groupKey: String): String {
        return localizedGroupTitle(groupKey)
    }

    fun domainTitleForKey(key: String): String {
        return localizedDomainTitle(key)
    }

    fun familyTitleForKey(key: String): String {
        return localizedFamilyTitle(key)
    }

    fun sectionTitleForKey(key: String): String {
        return localizedSectionTitle(key)
    }

    fun taxonomyFor(definition: ModuleParameterDefinition): ModulePresetTaxonomy {
        return ModulePresetTaxonomy(
            domainKey = definition.domainKey,
            familyKey = definition.familyKey,
            sectionKey = definition.sectionKey,
        )
    }

    private fun taxonomyRank(kind: ModuleTaxonomyKind, key: String): Int {
        return ModuleTaxonomyCatalog.rankOf(kind, key) ?: Int.MAX_VALUE
    }

    private fun localizedGroupTitle(groupKey: String): String = localizedTaxonomyTitle(
        ModuleTaxonomyKind.GROUP,
        groupKey,
        DemoStrings.text(R.string.module_taxonomy_other)
    )

    private fun localizedDomainTitle(key: String): String = localizedTaxonomyTitle(
        ModuleTaxonomyKind.DOMAIN,
        key,
        DemoStrings.text(R.string.module_taxonomy_other)
    )

    private fun localizedFamilyTitle(key: String): String = localizedTaxonomyTitle(
        ModuleTaxonomyKind.FAMILY,
        key,
        DemoStrings.text(R.string.module_taxonomy_other)
    )

    private fun localizedSectionTitle(key: String): String = localizedTaxonomyTitle(
        ModuleTaxonomyKind.SECTION,
        key,
        DemoStrings.text(R.string.module_taxonomy_general)
    )

    private fun localizedTaxonomyTitle(
        kind: ModuleTaxonomyKind,
        key: String,
        fallback: String,
    ): String {
        return ModuleTaxonomyCatalog.entryOf(kind, key)?.let { SdkLabelResolver.resolve(it.localizationKey, it.fallbackDisplayName) } ?: fallback
    }
}

private fun moduleKindLabel(kind: ModuleParameterUiKind): String =
    kind.localizedLabel().let { SdkLabelResolver.resolve(it.localizationKey, it.fallbackDisplayName) }

private fun moduleTitle(definition: ModuleParameterDefinition): String {
    val label = definition.localizedTitleLabel()
    return SdkLabelResolver.resolve(label.localizationKey, label.fallbackDisplayName)
}

fun formatModuleValuePreview(
    kind: ModuleParameterUiKind,
    valueBytes: ByteArray,
): String {
    val hex = valueBytes.toHexEditorText().ifBlank {
        DemoStrings.text(R.string.module_value_empty)
    }
    return when (kind) {
        ModuleParameterUiKind.BOOL -> {
            val boolValue = valueBytes.firstOrNull()?.toInt()?.let { it != 0 }
            val boolText = boolValue?.toString()
                ?: DemoStrings.text(R.string.module_value_invalid)
            DemoStrings.format(R.string.module_value_bool, hex, boolText)
        }

        ModuleParameterUiKind.BYTESASCII -> {
            val text = escapeControlText(String(valueBytes, StandardCharsets.US_ASCII))
            DemoStrings.format(R.string.module_value_ascii, hex, text)
        }

        else -> DemoStrings.format(R.string.module_value, hex)
    }
}
