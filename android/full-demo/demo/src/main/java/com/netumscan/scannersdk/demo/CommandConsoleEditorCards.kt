package com.netumscan.scannersdk.demo

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netumscan.scannersdk.model.DataRuleCommand
import com.netumscan.scannersdk.model.DataRuleKind
import com.netumscan.scannersdk.model.ModuleFamily
import com.netumscan.scannersdk.model.ModuleParameterUiKind
import com.netumscan.scannersdk.model.ModuleQuickValueOption
import com.netumscan.scannersdk.model.ModuleTestRecommendation
import com.netumscan.scannersdk.model.Ntc06hSettingCatalog
import com.netumscan.scannersdk.model.Ntc06hSettingDefinition
import com.netumscan.scannersdk.model.ScanTerminatorPreset
import com.netumscan.scannersdk.model.ScanTextCharset
import com.netumscan.scannersdk.model.formatModuleParameterId
import com.netumscan.scannersdk.model.localizedLabel
import com.netumscan.scannersdk.model.localizedModuleTestRecommendations
import com.netumscan.scannersdk.model.moduleNumericInputTextFromPayload
import com.netumscan.scannersdk.model.moduleNumericInputToPayloadHex
import com.netumscan.scannersdk.model.ntc06hTemplateInputSpec
import com.netumscan.scannersdk.model.ntc06hTemplateValueIsValid
import com.netumscan.scannersdk.model.ntc06hTemplateValueOrEmpty
import com.netumscan.scannersdk.model.parseModuleParameterId
import com.netumscan.scannersdk.model.parseModulePayloadHex
import com.netumscan.scannersdk.demo.ui.theme.DemoColors

@Composable
internal fun ModuleSettingsCard(
    moduleSummary: String,
    canExecuteModuleCommands: Boolean,
    selectedPreset: ModuleSettingPreset,
    selectedPresetIndex: Int,
    presetCount: Int,
    parameterIdText: String,
    payloadHexText: String,
    persistWrite: Boolean,
    enabled: Boolean,
    onPrevPreset: () -> Unit,
    onNextPreset: () -> Unit,
    onParameterIdChange: (String) -> Unit,
    onPayloadHexChange: (String) -> Unit,
    onPersistToggle: () -> Unit,
    onFillOn: () -> Unit,
    onFillOff: () -> Unit,
    onFillQuickValue: (ModuleQuickValueOption) -> Unit,
    onRead: () -> Unit,
    onWrite: () -> Unit,
    onClose: (() -> Unit)? = null,
) {
    val payloadError = runCatching { parseModulePayloadHex(payloadHexText) }.exceptionOrNull()?.message
    val parameterError = runCatching { parseModuleParameterId(parameterIdText) }.exceptionOrNull()?.message
    val numericInputSpec = selectedPreset.numericInputSpec
    var numericInputText by remember(selectedPreset.parameterId) {
        mutableStateOf(numericInputSpec?.let { moduleNumericInputTextFromPayload(it, payloadHexText) }.orEmpty())
    }
    var lastSyncedNumericPayloadHex by remember(selectedPreset.parameterId) { mutableStateOf(payloadHexText) }
    LaunchedEffect(selectedPreset.parameterId, payloadHexText, numericInputSpec) {
        if (numericInputSpec != null && payloadHexText != lastSyncedNumericPayloadHex) {
            moduleNumericInputTextFromPayload(numericInputSpec, payloadHexText)?.let { numericInputText = it }
            lastSyncedNumericPayloadHex = payloadHexText
        }
    }
    val numericInputError = numericInputSpec?.let { moduleNumericInputError(it, numericInputText) }
    val canRead = enabled && parameterError == null
    val canWrite = enabled && parameterError == null && payloadError == null && numericInputError == null

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "${demoStringResource(R.string.module_capability)}: $moduleSummary",
            fontSize = 13.sp,
            color = DemoColors.TextSecondary,
        )
        onClose?.let {
            OutlinedButton(
                onClick = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .demoTestTag(DemoTestTags.CONSOLE_GENERIC_CUSTOM_CLOSE),
                border = BorderStroke(1.dp, DemoColors.Outline),
            ) {
                Text(demoStringResource(R.string.close_custom_editor))
            }
        }
        ConsoleHint(
            if (canExecuteModuleCommands) {
                selectedPreset.hint()
            } else {
                demoStringResource(R.string.module_commands_not_ready_hint)
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onPrevPreset, enabled = enabled, modifier = Modifier.weight(0.8f)) {
                Text(demoStringResource(R.string.previous))
            }
            ParseStateCard(modifier = Modifier.weight(2f), title = demoStringResource(R.string.preset)) {
                Text(
                    text = "${selectedPresetIndex + 1}/$presetCount ${selectedPreset.title()}",
                    fontSize = 13.sp,
                    color = DemoColors.TextPrimary,
                )
                val definition = selectedPreset.definition
                Text(
                    text = "0x${formatModuleParameterId(selectedPreset.parameterId)} / ${definition.displayName}",
                    fontSize = 12.sp,
                    color = DemoColors.TextSecondary,
                )
            }
            Button(onClick = onNextPreset, enabled = enabled, modifier = Modifier.weight(0.8f)) {
                Text(demoStringResource(R.string.next))
            }
        }
        OutlinedTextField(
            value = parameterIdText,
            onValueChange = onParameterIdChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(demoStringResource(R.string.parameter_id_hex)) },
            enabled = enabled,
            singleLine = true,
            isError = parameterError != null,
        )
        OutlinedTextField(
            value = payloadHexText,
            onValueChange = onPayloadHexChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(demoStringResource(R.string.write_value_hex)) },
            enabled = enabled,
            singleLine = true,
            isError = payloadError != null,
        )
        if (numericInputSpec != null) {
            OutlinedTextField(
                value = numericInputText,
                onValueChange = { next ->
                    numericInputText = next
                    val payloadHex = moduleNumericInputToPayloadHex(numericInputSpec, next)
                    if (payloadHex != null) {
                        onPayloadHexChange(payloadHex)
                        lastSyncedNumericPayloadHex = payloadHex
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(numericInputSpec.label()) },
                supportingText = { Text(numericInputSpec.rangeHint()) },
                enabled = enabled,
                singleLine = true,
                isError = numericInputError != null,
            )
        }
        if (parameterError != null) {
            ConsoleFeedbackBanner(text = parameterError, tone = ConsoleBannerTone.Error)
        }
        if (payloadError != null) {
            ConsoleFeedbackBanner(text = payloadError, tone = ConsoleBannerTone.Error)
        }
        if (numericInputError != null) {
            ConsoleFeedbackBanner(text = numericInputError, tone = ConsoleBannerTone.Error)
        }
        if (selectedPreset.quickValueOptions.isNotEmpty()) {
            selectedPreset.quickValueOptions.chunked(2).forEach { chunk ->
                CommandButtonRow(
                    actions = chunk.map { option ->
                        commandButtonAction(demoStringResource(R.string.fill_value, option.label()), enabled) {
                            onFillQuickValue(option)
                        }
                    },
                )
            }
        } else {
            CommandButtonRow(
                actions = listOf(
                    commandButtonAction(demoStringResource(R.string.fill_01), enabled, onFillOn),
                    commandButtonAction(demoStringResource(R.string.fill_00), enabled, onFillOff),
                ),
            )
        }
        CommandButtonRow(
            actions = listOf(
                commandButtonAction(
                    if (persistWrite) demoStringResource(R.string.persist_write_on) else demoStringResource(R.string.persist_write_off),
                    enabled,
                    onPersistToggle,
                ),
                commandButtonAction(demoStringResource(R.string.read_parameter), canRead, onRead),
            ),
        )
        Button(
            onClick = onWrite,
            enabled = canWrite,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(demoStringResource(R.string.write_parameter))
        }
        val preview = runCatching {
            formatModuleValuePreview(
                kind = selectedPreset.kind,
                valueBytes = parseModulePayloadHex(payloadHexText),
            )
        }.getOrNull()
        if (!preview.isNullOrBlank()) {
            ConsoleHint(preview)
        }
    }
}

@Composable
internal fun ModulePresetCommandCard(
    preset: ModuleSettingPreset,
    enabled: Boolean,
    customEditButtonTag: String? = null,
    onRead: () -> Unit,
    onWriteOn: () -> Unit,
    onWriteOff: () -> Unit,
    onWriteDefault: (() -> Unit)?,
    onWriteEnumOption: (com.netumscan.scannersdk.model.ModuleEnumOption) -> Unit,
    onWriteQuickValue: (ModuleQuickValueOption) -> Unit,
    onLoadCustom: () -> Unit,
) {
    GroupedCommandCard(
        title = preset.title(),
        summary = "0x${formatModuleParameterId(preset.parameterId)} / ${preset.taxonomy.familyTitle()} / ${preset.taxonomy.sectionTitle()}",
    ) {
        Text(
            text = preset.hint(),
            fontSize = 12.sp,
            color = DemoColors.TextSecondary,
        )
        val customEditLabel = demoStringResource(R.string.custom_edit)
        CommandButtonRow(
            actions = listOf(
                commandButtonAction(demoStringResource(R.string.read), enabled, onRead),
                commandButtonAction(customEditLabel, enabled, onLoadCustom),
            ),
            testTagForLabel = { label ->
                if (label == customEditLabel) customEditButtonTag else null
            },
        )
        when {
            preset.supportsBooleanToggle -> {
                CommandButtonRow(
                    actions = listOf(
                        commandButtonAction(demoStringResource(R.string.on), enabled, onWriteOn),
                        commandButtonAction(demoStringResource(R.string.off), enabled, onWriteOff),
                    ),
                )
            }

            preset.enumOptions.isNotEmpty() -> {
                preset.enumOptions.chunked(2).forEach { chunk ->
                    CommandButtonRow(
                        actions = chunk.map { option ->
                            commandButtonAction(option.label(), enabled) { onWriteEnumOption(option) }
                        },
                    )
                }
            }

            preset.quickValueOptions.isNotEmpty() -> {
                preset.quickValueOptions.chunked(2).forEach { chunk ->
                    CommandButtonRow(
                        actions = chunk.map { option ->
                            commandButtonAction(option.label(), enabled) { onWriteQuickValue(option) }
                        },
                    )
                }
            }

            onWriteDefault != null -> {
                CommandButtonRow(
                    actions = listOf(
                        commandButtonAction(demoStringResource(R.string.write_default), enabled, onWriteDefault),
                    ),
                )
            }
        }
    }
}

internal data class Ntc06hUiSectionGroup(
    val key: String,
    val settings: List<Ntc06hSettingDefinition>,
)

internal data class Ntc06hUiFamilyGroup(
    val key: String,
    val sections: List<Ntc06hUiSectionGroup>,
)

internal data class Ntc06hUiDomainGroup(
    val key: String,
    val families: List<Ntc06hUiFamilyGroup>,
)

internal fun buildNtc06hDomainGroups(): List<Ntc06hUiDomainGroup> {
    val byDomain = Ntc06hSettingCatalog.all.groupBy { it.domainKey }
    val sortedDomainKeys = com.netumscan.scannersdk.model.ModuleTaxonomyCatalog.sortKeys(com.netumscan.scannersdk.model.ModuleTaxonomyKind.DOMAIN, byDomain.keys)
    return sortedDomainKeys.map { domainKey ->
        val byFamily = byDomain.getValue(domainKey).groupBy { it.familyKey }
        val sortedFamilyKeys = com.netumscan.scannersdk.model.ModuleTaxonomyCatalog.sortKeys(com.netumscan.scannersdk.model.ModuleTaxonomyKind.FAMILY, byFamily.keys)
        Ntc06hUiDomainGroup(
            key = domainKey,
            families = sortedFamilyKeys.map { familyKey ->
                val bySection = byFamily.getValue(familyKey).groupBy { it.sectionKey }
                val sortedSectionKeys = com.netumscan.scannersdk.model.ModuleTaxonomyCatalog.sortKeys(com.netumscan.scannersdk.model.ModuleTaxonomyKind.SECTION, bySection.keys)
                Ntc06hUiFamilyGroup(
                    key = familyKey,
                    sections = sortedSectionKeys.map { sectionKey ->
                        Ntc06hUiSectionGroup(
                            key = sectionKey,
                            settings = bySection.getValue(sectionKey),
                        )
                    },
                )
            },
        )
    }
}

@Composable
internal fun Ntc06hSettingCommandCard(
    setting: Ntc06hSettingDefinition,
    enabled: Boolean,
    customEditButtonTag: String? = null,
    onWrite: () -> Unit,
    onWriteAndSave: () -> Unit,
    onLoadCustom: () -> Unit,
) {
    val settingTitle = setting.localizedTitle()
    GroupedCommandCard(
        title = settingTitle,
        summary = "${setting.displayCode} / ${ModuleSettingsCatalog.familyTitleForKey(setting.familyKey)} / ${ModuleSettingsCatalog.sectionTitleForKey(setting.sectionKey)}",
    ) {
        Text(
            text = ntc06hSettingHint(setting),
            fontSize = 12.sp,
            color = DemoColors.TextSecondary,
        )
        val customEditLabel = demoStringResource(R.string.custom_edit)
        CommandButtonRow(
            actions = listOf(
                commandButtonAction(
                    if (setting.isTemplate) demoStringResource(R.string.edit_template) else demoStringResource(R.string.send_setting),
                    enabled,
                    if (setting.isTemplate) onLoadCustom else onWrite,
                ),
                commandButtonAction(customEditLabel, enabled, onLoadCustom),
            ),
            testTagForLabel = { label ->
                if (label == customEditLabel) customEditButtonTag else null
            },
        )
        if (setting.requiresSave && !setting.isTemplate) {
            CommandButtonRow(
                actions = listOf(
                    commandButtonAction(demoStringResource(R.string.send_save), enabled, onWriteAndSave),
                ),
            )
        }
    }
}

@Composable
internal fun Ntc06hCustomSettingCard(
    moduleSummary: String,
    selectedSetting: Ntc06hSettingDefinition,
    selectedSettingIndex: Int,
    settingCount: Int,
    settingCode: String,
    templateHexValue: String,
    saveAfterWrite: Boolean,
    enabled: Boolean,
    onPrevSetting: () -> Unit,
    onNextSetting: () -> Unit,
    onSettingCodeChange: (String) -> Unit,
    onTemplateHexValueChange: (String) -> Unit,
    onToggleSaveAfterWrite: () -> Unit,
    onWrite: () -> Unit,
    onSaveOnly: () -> Unit,
    onClose: () -> Unit,
) {
    val templateInputSpec = ntc06hTemplateInputSpec(selectedSetting)
    val selectedSettingTitle = selectedSetting.localizedTitle()
    val canWrite = enabled && settingCode.isNotBlank() &&
        (templateInputSpec == null || ntc06hTemplateValueIsValid(templateInputSpec, templateHexValue))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "${demoStringResource(R.string.module_capability)}: $moduleSummary",
            fontSize = 13.sp,
            color = DemoColors.TextSecondary,
        )
        OutlinedButton(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .demoTestTag(DemoTestTags.CONSOLE_NTC06H_CUSTOM_CLOSE),
            border = BorderStroke(1.dp, DemoColors.Outline),
        ) {
            Text(demoStringResource(R.string.close_custom_editor))
        }
        ConsoleHint(
            if (selectedSetting.isTemplate) {
                DemoStrings.format(R.string.ntc06h_current_template_hint, selectedSettingTitle, selectedSetting.displayCode, selectedSetting.templateHint)
            } else {
                DemoStrings.format(R.string.ntc06h_current_suggested_hint, selectedSettingTitle, selectedSetting.displayCode)
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onPrevSetting, enabled = enabled, modifier = Modifier.weight(0.8f)) {
                Text(demoStringResource(R.string.previous))
            }
            ParseStateCard(modifier = Modifier.weight(2f), title = demoStringResource(R.string.preset)) {
                Text(
                    text = "${selectedSettingIndex + 1}/$settingCount $selectedSettingTitle",
                    fontSize = 13.sp,
                    color = DemoColors.TextPrimary,
                )
                Text(
                    text = "${selectedSetting.displayCode} / ${ModuleSettingsCatalog.familyTitleForKey(selectedSetting.familyKey)} / ${ModuleSettingsCatalog.sectionTitleForKey(selectedSetting.sectionKey)}",
                    fontSize = 12.sp,
                    color = DemoColors.TextSecondary,
                )
            }
            Button(onClick = onNextSetting, enabled = enabled, modifier = Modifier.weight(0.8f)) {
                Text(demoStringResource(R.string.next))
            }
        }
        Ntc06hTemplateEditorFields(
            setting = selectedSetting,
            settingCode = settingCode,
            templateValue = templateHexValue,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            onSettingCodeChange = onSettingCodeChange,
            onTemplateValueChange = onTemplateHexValueChange,
        )
        CommandButtonRow(
            actions = listOf(
                commandButtonAction(demoStringResource(R.string.fill_suggested), enabled) {
                    if (templateInputSpec != null) {
                        onTemplateHexValueChange(ntc06hTemplateValueOrEmpty(selectedSetting))
                    } else {
                        onSettingCodeChange(selectedSetting.templateExampleCode.ifBlank { selectedSetting.displayCode })
                    }
                },
                commandButtonAction(
                    if (saveAfterWrite) demoStringResource(R.string.save_after_write_on) else demoStringResource(R.string.save_after_write_off),
                    enabled,
                    onToggleSaveAfterWrite,
                ),
            ),
        )
        CommandButtonRow(
            actions = listOf(
                commandButtonAction(demoStringResource(R.string.save_only), enabled, onSaveOnly),
            ),
        )
        Button(
            onClick = onWrite,
            enabled = canWrite,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(demoStringResource(R.string.send_setting_code))
        }
    }
}

private fun ntc06hSettingHint(setting: Ntc06hSettingDefinition): String {
    val behavior = if (setting.requiresSave) {
        DemoStrings.text(R.string.ntc06h_behavior_save_recommended)
    } else {
        DemoStrings.text(R.string.ntc06h_behavior_applies_immediately)
    }
    val templateHint = if (setting.isTemplate) {
        DemoStrings.text(R.string.ntc06h_template_code_replace)
    } else {
        ""
    }
    return DemoStrings.format(R.string.ntc06h_setting_hint_format, ModuleSettingsCatalog.domainTitleForKey(setting.domainKey), ModuleSettingsCatalog.familyTitleForKey(setting.familyKey), ModuleSettingsCatalog.sectionTitleForKey(setting.sectionKey), behavior, templateHint)
}

internal fun moduleTestRecommendations(family: ModuleFamily): List<ModuleTestRecommendation> =
    localizedModuleTestRecommendations(family)

private fun ModuleTestRecommendation.title(): String = SdkLabelResolver.resolve(titleKey, titleFallback)

private fun ModuleTestRecommendation.detail(): String = SdkLabelResolver.resolve(detailKey, detailFallback)

@Composable
internal fun ModuleTestGuideCard(
    family: ModuleFamily,
    recommendations: List<ModuleTestRecommendation>,
) {
    GroupedCommandCard(
        title = demoStringResource(R.string.recommended_first_tests),
        summary = demoStringResource(R.string.module_test_order_summary, family.name),
    ) {
        recommendations.forEachIndexed { index, item ->
            Text(
                text = "${index + 1}. ${item.title()}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DemoColors.TextPrimary,
            )
            Text(
                text = item.detail(),
                fontSize = 12.sp,
                color = DemoColors.TextSecondary,
            )
            if (index != recommendations.lastIndex) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
internal fun CharsetSelectorRow(
    selected: String,
    enabled: Boolean,
    onSelect: (ScanTextCharset) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CommandButtonRow(
            actions = listOf(
                commandButtonAction(charsetButtonLabel(ScanTextCharset.UTF_8, selected), enabled) { onSelect(ScanTextCharset.UTF_8) },
                commandButtonAction(charsetButtonLabel(ScanTextCharset.GBK, selected), enabled) { onSelect(ScanTextCharset.GBK) },
            ),
        )
        CommandButtonRow(
            actions = listOf(
                commandButtonAction(charsetButtonLabel(ScanTextCharset.US_ASCII, selected), enabled) { onSelect(ScanTextCharset.US_ASCII) },
                commandButtonAction(charsetButtonLabel(ScanTextCharset.ISO_8859_1, selected), enabled) { onSelect(ScanTextCharset.ISO_8859_1) },
            ),
        )
    }
}

private fun charsetButtonLabel(charset: ScanTextCharset, selected: String): String {
    return if (charset.displayName == selected) {
        "[${charset.displayName}]"
    } else {
        charset.displayName
    }
}

@Composable
internal fun TerminatorSelectorRow(
    selected: String,
    enabled: Boolean,
    onSelect: (ScanTerminatorPreset) -> Unit,
) {
    val rows = ScanTerminatorPreset.DEFAULTS.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            CommandButtonRow(
                actions = row.map { preset ->
                    commandButtonAction(terminatorButtonLabel(preset, selected), enabled) { onSelect(preset) }
                },
            )
        }
    }
}

private fun terminatorButtonLabel(preset: ScanTerminatorPreset, selected: String): String {
    return if (preset.summary == selected) {
        "[${preset.label}]"
    } else {
        preset.label
    }
}

internal enum class DataRuleFormMode(val kind: DataRuleKind) {
    PREFIX(DataRuleKind.PREFIX),
    SUFFIX(DataRuleKind.SUFFIX),
    HIDE_START(DataRuleKind.HIDE_START),
    HIDE_MIDDLE(DataRuleKind.HIDE_MIDDLE),
    HIDE_END(DataRuleKind.HIDE_END),
    REPLACE(DataRuleKind.REPLACE),
}

@Composable
internal fun DataRuleBuilderCard(
    mode: DataRuleFormMode,
    valueA: String,
    valueB: String,
    enabled: Boolean,
    onModeChange: (DataRuleFormMode) -> Unit,
    onValueAChange: (String) -> Unit,
    onValueBChange: (String) -> Unit,
    onExecute: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ScrollableTabRow(
            selectedTabIndex = dataRuleFormModeIndex(mode),
            containerColor = Color.Transparent,
            contentColor = DemoColors.TextPrimary,
            edgePadding = 0.dp,
        ) {
            DataRuleFormMode.entries.forEach { entry ->
                Tab(
                    selected = mode == entry,
                    onClick = { onModeChange(entry) },
                    text = { Text(dataRuleFormModeLabel(entry)) },
                )
            }
        }

        val buildResult = runCatching { buildDataRuleCommand(mode, valueA, valueB) }
        val canBuild = buildResult.isSuccess
        val error = buildResult.exceptionOrNull()?.message

        when (mode) {
            DataRuleFormMode.PREFIX,
            DataRuleFormMode.SUFFIX -> {
                OutlinedTextField(
                    value = valueA,
                    onValueChange = onValueAChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(demoStringResource(R.string.data_rule_text_ascii_hint)) },
                    enabled = enabled,
                    singleLine = true,
                )
            }

            DataRuleFormMode.HIDE_START,
            DataRuleFormMode.HIDE_END -> {
                OutlinedTextField(
                    value = valueA,
                    onValueChange = onValueAChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(demoStringResource(R.string.data_rule_length_hint)) },
                    enabled = enabled,
                    singleLine = true,
                )
            }

            DataRuleFormMode.HIDE_MIDDLE -> {
                OutlinedTextField(
                    value = valueA,
                    onValueChange = onValueAChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(demoStringResource(R.string.data_rule_start_hint)) },
                    enabled = enabled,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = valueB,
                    onValueChange = onValueBChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(demoStringResource(R.string.data_rule_length_hint)) },
                    enabled = enabled,
                    singleLine = true,
                )
            }

            DataRuleFormMode.REPLACE -> {
                OutlinedTextField(
                    value = valueA,
                    onValueChange = onValueAChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(demoStringResource(R.string.data_rule_source_ascii_hint)) },
                    enabled = enabled,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = valueB,
                    onValueChange = onValueBChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(demoStringResource(R.string.data_rule_target_ascii_hint)) },
                    enabled = enabled,
                    singleLine = true,
                )
            }
        }

        if (canBuild) {
            ConsoleHint(demoStringResource(R.string.data_rule_valid_hint))
        } else if (!error.isNullOrBlank()) {
            ConsoleFeedbackBanner(text = error, tone = ConsoleBannerTone.Error)
        }

        Button(
            onClick = onExecute,
            enabled = enabled && canBuild,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(demoStringResource(R.string.build_and_send))
        }
    }
}

internal fun dataRuleFormModeLabel(mode: DataRuleFormMode): String {
    val label = mode.kind.localizedLabel()
    return SdkLabelResolver.resolve(label.localizationKey, label.fallbackDisplayName)
}

private fun dataRuleFormModeIndex(mode: DataRuleFormMode): Int = when (mode) {
    DataRuleFormMode.PREFIX -> 0
    DataRuleFormMode.SUFFIX -> 1
    DataRuleFormMode.HIDE_START -> 2
    DataRuleFormMode.HIDE_MIDDLE -> 3
    DataRuleFormMode.HIDE_END -> 4
    DataRuleFormMode.REPLACE -> 5
}

internal fun buildDataRuleCommand(
    mode: DataRuleFormMode,
    valueA: String,
    valueB: String,
): DataRuleCommand {
    return when (mode) {
        DataRuleFormMode.PREFIX -> DataRuleCommand.prefix(parseAsciiEscapes(valueA))
        DataRuleFormMode.SUFFIX -> DataRuleCommand.suffix(parseAsciiEscapes(valueA))
        DataRuleFormMode.HIDE_START -> DataRuleCommand.hideStart(valueA.trim().toInt())
        DataRuleFormMode.HIDE_MIDDLE -> DataRuleCommand.hideMiddle(valueA.trim().toInt(), valueB.trim().toInt())
        DataRuleFormMode.HIDE_END -> DataRuleCommand.hideEnd(valueA.trim().toInt())
        DataRuleFormMode.REPLACE -> DataRuleCommand.replace(parseAsciiEscapes(valueA), parseAsciiEscapes(valueB))
    }
}

private fun parseAsciiEscapes(text: String): ByteArray {
    val bytes = ArrayList<Byte>(text.length)
    var index = 0
    while (index < text.length) {
        val ch = text[index]
        if (ch == '\\' && index + 3 < text.length && text[index + 1] == 'x') {
            val hex = text.substring(index + 2, index + 4)
            val value = hex.toIntOrNull(16)
                ?: throw IllegalArgumentException("invalid hex escape: \\x$hex")
            bytes += value.toByte()
            index += 4
            continue
        }
        require(ch.code in 0x00..0x7F) { "only ASCII text is supported in builder" }
        bytes += ch.code.toByte()
        index += 1
    }
    return bytes.toByteArray()
}
