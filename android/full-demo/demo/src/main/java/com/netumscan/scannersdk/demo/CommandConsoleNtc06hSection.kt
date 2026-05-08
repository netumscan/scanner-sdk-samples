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
import com.netumscan.scannersdk.model.ntc06hBuildTemplateCode
import com.netumscan.scannersdk.model.ntc06hTemplateInputSpec

@Composable
internal fun Ntc06hModuleCatalog(
    vm: CommandConsoleViewModel,
    state: ModuleConsoleSectionState,
    actions: ModuleConsoleSectionActions,
) {
    if (state.ntc06hDomainGroups.isEmpty()) {
        ConsoleHint(
            demoStringResource(R.string.ntc06h_setting_catalog_unavailable),
        )
        return
    }
    val sendSettingCodeLabel = demoStringResource(R.string.send_setting_code)
    val saveNtc06hSettingsLabel = demoStringResource(R.string.save_ntc06h_settings)

    ScrollableTabRow(
        selectedTabIndex = state.moduleSelectedTab.coerceIn(0, (state.ntc06hDomainGroups.size - 1).coerceAtLeast(0)),
        containerColor = Color.Transparent,
        contentColor = DemoColors.TextPrimary,
        edgePadding = 0.dp,
    ) {
        state.ntc06hDomainGroups.forEachIndexed { index, group ->
            Tab(
                selected = state.moduleSelectedTab == index,
                onClick = { actions.onModuleSelectedTabChange(index) },
                text = { Text(ModuleSettingsCatalog.domainTitleForKey(group.key)) },
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        state.ntc06hDomainGroups[state.moduleSelectedTab.coerceIn(0, state.ntc06hDomainGroups.lastIndex)].families.forEach { family ->
            val isExpanded = state.expandedModuleFamilyKey == family.key
            CollapsibleGroupedCommandCard(
                title = ModuleSettingsCatalog.familyTitleForKey(family.key),
                summary = demoStringResource(
                    R.string.family_setting_summary,
                    family.sections.sumOf { it.settings.size },
                    family.sections.size,
                ),
                danger = family.key in setOf("rs485", "wiegand", "modbus"),
                expanded = isExpanded,
                onToggle = {
                    actions.onExpandedModuleFamilyKeyChange(if (isExpanded) null else family.key)
                    if (!isExpanded) {
                        actions.onInlineNtc06hSettingKeyChange(null)
                    }
                },
                headerTestTag = DemoTestTags.consoleModuleFamilyHeader(family.key),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    family.sections.forEach { section ->
                        Text(
                            text = ModuleSettingsCatalog.sectionTitleForKey(section.key),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DemoColors.TextSecondary,
                        )
                        section.settings.forEach { setting ->
                            val settingTitle = setting.localizedTitle()
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Ntc06hSettingCommandCard(
                                    setting = setting,
                                    enabled = !state.isExecuting && state.canExecuteModuleCommands,
                                    customEditButtonTag = DemoTestTags.consoleNtc06hCustomEdit(setting.key),
                                    onWrite = {
                                        vm.writeNtc06hSetting(
                                            settingKey = setting.key,
                                            label = "$sendSettingCodeLabel ${setting.displayCode} $settingTitle",
                                            saveAfterWrite = false,
                                        )
                                    },
                                    onWriteAndSave = {
                                        vm.writeNtc06hSetting(
                                            settingKey = setting.key,
                                            label = "$sendSettingCodeLabel ${setting.displayCode} $settingTitle",
                                            saveAfterWrite = true,
                                        )
                                    },
                                    onLoadCustom = {
                                        actions.onSelectedNtc06hSettingIndexChange(state.ntc06hSettingIndexByKey[setting.key] ?: 0)
                                        actions.onNtc06hCustomCodeChange(setting.templateExampleCode.ifBlank { setting.displayCode })
                                        actions.onNtc06hTemplateHexValueChange(com.netumscan.scannersdk.model.ntc06hTemplateValueOrEmpty(setting))
                                        if (setting.requiresSave != state.ntc06hSaveAfterWrite) {
                                            actions.onNtc06hSaveAfterWriteToggle()
                                        }
                                        actions.onInlineNtc06hSettingKeyChange(
                                            if (state.inlineNtc06hSettingKey == setting.key) null else setting.key,
                                        )
                                    },
                                )
                                if (state.inlineNtc06hSettingKey == setting.key) {
                                    val activeSetting = state.selectedNtc06hSetting ?: setting
                                    val templateInputSpec = ntc06hTemplateInputSpec(activeSetting)
                                    val resolvedSettingCode = if (templateInputSpec != null) {
                                        ntc06hBuildTemplateCode(templateInputSpec, state.ntc06hTemplateHexValue)
                                    } else {
                                        state.ntc06hCustomCode
                                    }
                                    Ntc06hCustomSettingCard(
                                        moduleSummary = state.moduleSummary,
                                        selectedSetting = activeSetting,
                                        selectedSettingIndex = state.selectedNtc06hSettingIndex,
                                        settingCount = state.ntc06hSettings.size,
                                        settingCode = resolvedSettingCode,
                                        templateHexValue = state.ntc06hTemplateHexValue,
                                        saveAfterWrite = state.ntc06hSaveAfterWrite,
                                        enabled = !state.isExecuting && state.canExecuteModuleCommands,
                                        onPrevSetting = {
                                            if (state.ntc06hSettings.isNotEmpty()) {
                                                actions.onSelectedNtc06hSettingIndexChange(
                                                    (state.selectedNtc06hSettingIndex - 1 + state.ntc06hSettings.size) % state.ntc06hSettings.size,
                                                )
                                            }
                                        },
                                        onNextSetting = {
                                            if (state.ntc06hSettings.isNotEmpty()) {
                                                actions.onSelectedNtc06hSettingIndexChange(
                                                    (state.selectedNtc06hSettingIndex + 1) % state.ntc06hSettings.size,
                                                )
                                            }
                                        },
                                        onSettingCodeChange = actions.onNtc06hCustomCodeChange,
                                        onTemplateHexValueChange = actions.onNtc06hTemplateHexValueChange,
                                        onToggleSaveAfterWrite = actions.onNtc06hSaveAfterWriteToggle,
                                        onWrite = {
                                            vm.writeNtc06hRawSetting(
                                                settingCode = resolvedSettingCode,
                                                label = "$sendSettingCodeLabel $resolvedSettingCode ${activeSetting.localizedTitle()}",
                                                saveAfterWrite = state.ntc06hSaveAfterWrite,
                                            )
                                        },
                                        onSaveOnly = {
                                            vm.saveNtc06hSettings(saveNtc06hSettingsLabel)
                                        },
                                        onClose = { actions.onInlineNtc06hSettingKeyChange(null) },
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
