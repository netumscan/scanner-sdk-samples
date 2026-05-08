package com.netumscan.scannersdk.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netumscan.scannersdk.demo.ui.theme.DemoColors
import com.netumscan.scannersdk.model.formatModuleParameterId
import com.netumscan.scannersdk.model.parseModuleParameterId
import com.netumscan.scannersdk.model.parseModulePayloadHex

@Composable
internal fun GenericModuleCatalog(
    vm: CommandConsoleViewModel,
    state: ModuleConsoleSectionState,
    actions: ModuleConsoleSectionActions,
) {
    if (state.moduleDomainGroups.isEmpty() || state.modulePresets.isEmpty()) {
        ConsoleHint(
            demoStringResource(R.string.module_parameter_catalog_unavailable),
        )
        return
    }
    val readModuleParameterLabel = demoStringResource(R.string.read_module_parameter)
    val writeModuleParameterLabel = demoStringResource(R.string.write_module_parameter)

    ScrollableTabRow(
        selectedTabIndex = state.moduleSelectedTab.coerceIn(0, (state.moduleDomainGroups.size - 1).coerceAtLeast(0)),
        containerColor = Color.Transparent,
        contentColor = DemoColors.TextPrimary,
        edgePadding = 0.dp,
    ) {
        state.moduleDomainGroups.forEachIndexed { index, group ->
            Tab(
                modifier = Modifier.demoTestTag(DemoTestTags.consoleModuleDomainTab(index)),
                selected = state.moduleSelectedTab == index,
                onClick = { actions.onModuleSelectedTabChange(index) },
                text = { Text(group.title()) },
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        state.moduleDomainGroups[state.moduleSelectedTab.coerceIn(0, state.moduleDomainGroups.lastIndex)].families.forEach { family ->
            val isExpanded = state.expandedModuleFamilyKey == family.key
            CollapsibleGroupedCommandCard(
                title = family.title(),
                summary = demoStringResource(
                    R.string.family_parameter_summary,
                    family.sections.sumOf { it.presets.size },
                    family.sections.size,
                ),
                danger = family.key in setOf("rs485", "wiegand", "modbus"),
                expanded = isExpanded,
                onToggle = {
                    actions.onExpandedModuleFamilyKeyChange(if (isExpanded) null else family.key)
                    if (!isExpanded) {
                        actions.onInlineCustomPresetIdChange(null)
                    }
                },
                headerTestTag = DemoTestTags.consoleModuleFamilyHeader(family.key),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    family.sections.forEach { section ->
                        Text(
                            text = section.title(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DemoColors.TextSecondary,
                        )
                        section.presets.forEach { preset ->
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                ModulePresetCommandCard(
                                    preset = preset,
                                    enabled = !state.isExecuting && state.canExecuteModuleCommands,
                                    customEditButtonTag = DemoTestTags.consoleModuleCustomEdit(preset.parameterId),
                                    onRead = {
                                        vm.readModuleParameter(
                                            family = preset.definition.moduleFamily,
                                            parameterId = preset.parameterId,
                                            label = "$readModuleParameterLabel 0x${formatModuleParameterId(preset.parameterId)} ${preset.title()}",
                                            parameterKind = preset.kind,
                                        )
                                    },
                                    onWriteOn = {
                                        vm.writeModuleParameter(
                                            family = preset.definition.moduleFamily,
                                            parameterId = preset.parameterId,
                                            label = "$writeModuleParameterLabel 0x${formatModuleParameterId(preset.parameterId)} ${preset.title()}",
                                            payloadBytes = parseModulePayloadHex(preset.writeOnHex),
                                            persist = true,
                                            parameterKind = preset.kind,
                                        )
                                    },
                                    onWriteOff = {
                                        vm.writeModuleParameter(
                                            family = preset.definition.moduleFamily,
                                            parameterId = preset.parameterId,
                                            label = "$writeModuleParameterLabel 0x${formatModuleParameterId(preset.parameterId)} ${preset.title()}",
                                            payloadBytes = parseModulePayloadHex(preset.writeOffHex),
                                            persist = true,
                                            parameterKind = preset.kind,
                                        )
                                    },
                                    onWriteDefault = preset.defaultPayloadHex?.let { payloadHex ->
                                        {
                                            vm.writeModuleParameter(
                                                family = preset.definition.moduleFamily,
                                                parameterId = preset.parameterId,
                                                label = "$writeModuleParameterLabel 0x${formatModuleParameterId(preset.parameterId)} ${preset.title()}",
                                                payloadBytes = parseModulePayloadHex(payloadHex),
                                                persist = true,
                                                parameterKind = preset.kind,
                                            )
                                        }
                                    },
                                    onWriteEnumOption = { option ->
                                        vm.writeModuleParameter(
                                            family = preset.definition.moduleFamily,
                                            parameterId = preset.parameterId,
                                            label = "$writeModuleParameterLabel 0x${formatModuleParameterId(preset.parameterId)} ${preset.title()} ${option.label()}",
                                            payloadBytes = parseModulePayloadHex(option.payloadHex),
                                            persist = true,
                                            parameterKind = preset.kind,
                                        )
                                    },
                                    onWriteQuickValue = { option ->
                                        vm.writeModuleParameter(
                                            family = preset.definition.moduleFamily,
                                            parameterId = preset.parameterId,
                                            label = "$writeModuleParameterLabel 0x${formatModuleParameterId(preset.parameterId)} ${preset.title()} ${option.label()}",
                                            payloadBytes = parseModulePayloadHex(option.payloadHex),
                                            persist = true,
                                            parameterKind = preset.kind,
                                        )
                                    },
                                    onLoadCustom = {
                                        actions.onSelectedModulePresetIndexChange(state.modulePresetIndexById[preset.parameterId] ?: 0)
                                        actions.onModuleParameterIdTextChange(formatModuleParameterId(preset.parameterId))
                                        actions.onModulePayloadHexTextChange(preset.defaultPayloadHex ?: "")
                                        actions.onInlineCustomPresetIdChange(
                                            if (state.inlineCustomPresetId == preset.parameterId) null else preset.parameterId,
                                        )
                                    },
                                )
                                if (state.inlineCustomPresetId == preset.parameterId) {
                                    val inlinePreset = state.selectedModulePreset ?: preset
                                    ModuleSettingsCard(
                                        moduleSummary = state.moduleSummary,
                                        canExecuteModuleCommands = state.canExecuteModuleCommands,
                                        selectedPreset = inlinePreset,
                                        selectedPresetIndex = state.selectedModulePresetIndex,
                                        presetCount = state.modulePresets.size,
                                        parameterIdText = state.moduleParameterIdText,
                                        payloadHexText = state.modulePayloadHexText,
                                        persistWrite = state.modulePersistWrite,
                                        enabled = !state.isExecuting && state.canExecuteModuleCommands,
                                        onPrevPreset = {
                                            actions.onSelectedModulePresetIndexChange(
                                                (state.selectedModulePresetIndex - 1 + state.modulePresets.size) % state.modulePresets.size,
                                            )
                                        },
                                        onNextPreset = {
                                            actions.onSelectedModulePresetIndexChange(
                                                (state.selectedModulePresetIndex + 1) % state.modulePresets.size,
                                            )
                                        },
                                        onParameterIdChange = actions.onModuleParameterIdTextChange,
                                        onPayloadHexChange = actions.onModulePayloadHexTextChange,
                                        onPersistToggle = actions.onModulePersistWriteToggle,
                                        onFillOn = { actions.onModulePayloadHexTextChange(inlinePreset.writeOnHex) },
                                        onFillOff = { actions.onModulePayloadHexTextChange(inlinePreset.writeOffHex) },
                                        onFillQuickValue = { option -> actions.onModulePayloadHexTextChange(option.payloadHex) },
                                        onRead = {
                                            val parameterId = parseModuleParameterId(state.moduleParameterIdText)
                                            vm.readModuleParameter(
                                                family = inlinePreset.definition.moduleFamily,
                                                parameterId = parameterId,
                                                label = "$readModuleParameterLabel 0x${formatModuleParameterId(parameterId)} ${inlinePreset.title()}",
                                                parameterKind = inlinePreset.kind,
                                            )
                                        },
                                        onWrite = {
                                            val parameterId = parseModuleParameterId(state.moduleParameterIdText)
                                            val payloadBytes = parseModulePayloadHex(state.modulePayloadHexText)
                                            vm.writeModuleParameter(
                                                family = inlinePreset.definition.moduleFamily,
                                                parameterId = parameterId,
                                                label = "$writeModuleParameterLabel 0x${formatModuleParameterId(parameterId)} ${inlinePreset.title()}",
                                                payloadBytes = payloadBytes,
                                                persist = state.modulePersistWrite,
                                                parameterKind = inlinePreset.kind,
                                            )
                                        },
                                        onClose = { actions.onInlineCustomPresetIdChange(null) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
