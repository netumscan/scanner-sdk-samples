package com.netumscan.scannersdk.demo

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netumscan.scannersdk.demo.ui.theme.DemoColors
import com.netumscan.scannersdk.model.MasterCommandCategory
import com.netumscan.scannersdk.model.ModuleFamily
import com.netumscan.scannersdk.model.Ntc06hSettingCatalog
import com.netumscan.scannersdk.model.formatModuleParameterId
import com.netumscan.scannersdk.model.ntc06hTemplateValueOrEmpty

@Composable
internal fun CommandConsolePageContent(
    vm: CommandConsoleViewModel,
    uiState: CommandConsoleUiState,
    context: Context,
    onBack: () -> Unit,
    initialState: CommandConsolePageInitialState = CommandConsolePageInitialState(),
) {
    var parseStateExpanded by rememberSaveable { mutableStateOf(false) }
    var quickActionsExpanded by rememberSaveable { mutableStateOf(false) }
    var moduleSettingsExpanded by rememberSaveable { mutableStateOf(initialState.moduleSettingsExpanded) }
    var commandsExpanded by rememberSaveable { mutableStateOf(false) }
    var dataRuleExpanded by rememberSaveable { mutableStateOf(false) }
    var operationScope by rememberSaveable {
        mutableStateOf(if (initialState.startInModuleScope) OperationScope.MODULE else OperationScope.MASTER)
    }
    var masterSelectedTab by rememberSaveable { mutableStateOf(0) }
    var moduleSelectedTab by rememberSaveable { mutableStateOf(initialState.moduleSelectedTab) }
    var pendingCommand by remember { mutableStateOf<ConsoleCommand?>(null) }
    var pendingQuickAction by remember { mutableStateOf<QuickActionCommand?>(null) }
    var builderMode by rememberSaveable { mutableStateOf(DataRuleFormMode.PREFIX) }
    var builderValueA by rememberSaveable { mutableStateOf("") }
    var builderValueB by rememberSaveable { mutableStateOf("") }
    var expandedModuleFamilyKey by rememberSaveable { mutableStateOf(initialState.expandedModuleFamilyKey) }
    var inlineCustomPresetId by rememberSaveable { mutableStateOf(initialState.inlineCustomPresetId) }
    var inlineNtc06hSettingKey by rememberSaveable { mutableStateOf(initialState.inlineNtc06hSettingKey) }
    var skipInitialModuleReset by rememberSaveable { mutableStateOf(initialState.preserveInlineEditorOnFirstComposition) }
    var ntc06hCustomCode by rememberSaveable { mutableStateOf("") }
    var ntc06hTemplateHexValue by rememberSaveable { mutableStateOf("") }
    var ntc06hSaveAfterWrite by rememberSaveable { mutableStateOf(true) }

    val activeModuleFamily = uiState.currentModuleFamily
    val moduleSummary = uiState.moduleSummary.asString()
    val isNtc06hFamily = activeModuleFamily == ModuleFamily.NTC06H
    val ntc06hSettings = remember(activeModuleFamily) {
        if (activeModuleFamily == ModuleFamily.NTC06H) Ntc06hSettingCatalog.all else emptyList()
    }
    val modulePresets = remember(activeModuleFamily) { ModuleSettingsCatalog.presetsFor(activeModuleFamily) }
    val moduleDomainGroups = remember(activeModuleFamily) { ModuleSettingsCatalog.domainGroupsFor(activeModuleFamily) }
    val ntc06hDomainGroups = remember(activeModuleFamily) {
        if (activeModuleFamily == ModuleFamily.NTC06H) {
            buildNtc06hDomainGroups()
        } else {
            emptyList()
        }
    }
    val modulePresetIndexById = remember(modulePresets) {
        modulePresets.mapIndexed { index, preset -> preset.parameterId to index }.toMap()
    }
    val ntc06hSettingIndexByKey = remember(ntc06hSettings) {
        ntc06hSettings.mapIndexed { index, setting -> setting.key to index }.toMap()
    }
    var selectedModulePresetIndex by rememberSaveable { mutableStateOf(initialState.selectedModulePresetIndex) }
    var selectedNtc06hSettingIndex by rememberSaveable { mutableStateOf(initialState.selectedNtc06hSettingIndex) }
    var moduleParameterIdText by rememberSaveable { mutableStateOf("") }
    var modulePayloadHexText by rememberSaveable { mutableStateOf("") }
    var modulePersistWrite by rememberSaveable { mutableStateOf(true) }

    val selectedModulePreset = modulePresets.getOrNull(selectedModulePresetIndex)
    val selectedNtc06hSetting = ntc06hSettings.getOrNull(selectedNtc06hSettingIndex)

    val masterQuickActionRows = CommandCatalog.quickActionRows
    val masterTabItems = CommandCatalog.masterConsoleBuckets
    val selectedMasterTab = masterSelectedTab.coerceIn(0, (masterTabItems.size - 1).coerceAtLeast(0))
    val selectedMasterTabItem = masterTabItems.getOrNull(selectedMasterTab)
    val selectedTabSections = selectedMasterTabItem?.let {
        CommandCatalog.masterBucketSectionsForKey(it.key)
    } ?: emptyList()
    val quickActionCount = masterQuickActionRows.sumOf { it.commands.size }
    val selectedMasterCommandCount = selectedTabSections.sumOf { it.commands.size }
    val isDataRulesTab = operationScope == OperationScope.MASTER &&
        selectedMasterTabItem?.key == MasterCommandCategory.DATA_PROCESSING.name
    val masterSectionState = MasterConsoleSectionState(
        isExecuting = uiState.isExecuting,
        canExecuteMasterCommands = uiState.hasReadySession && !uiState.isExecuting,
        quickActionsExpanded = quickActionsExpanded,
        commandsExpanded = commandsExpanded,
        quickActionCount = quickActionCount,
        quickActionRows = masterQuickActionRows,
        selectedMasterTab = selectedMasterTab,
        masterTabItems = masterTabItems,
        selectedMasterTabItem = selectedMasterTabItem,
        selectedTabSections = selectedTabSections,
        selectedMasterCommandCount = selectedMasterCommandCount,
    )
    val masterSectionActions = MasterConsoleSectionActions(
        onQuickActionsExpandedChange = { quickActionsExpanded = it },
        onPendingQuickActionChange = { pendingQuickAction = it },
        onCommandsExpandedChange = { commandsExpanded = it },
        onSelectedMasterTabChange = { masterSelectedTab = it },
        onPendingCommandChange = { pendingCommand = it },
    )
    val moduleSectionState = ModuleConsoleSectionState(
        activeModuleFamily = activeModuleFamily,
        moduleSettingsExpanded = moduleSettingsExpanded,
        isNtc06hFamily = isNtc06hFamily,
        isExecuting = uiState.isExecuting,
        supportsModuleCommands = uiState.supportsModuleCommands,
        canExecuteModuleCommands = uiState.canExecuteModuleCommands,
        moduleSummary = moduleSummary,
        ntc06hSettings = ntc06hSettings,
        ntc06hDomainGroups = ntc06hDomainGroups,
        modulePresets = modulePresets,
        moduleDomainGroups = moduleDomainGroups,
        modulePresetIndexById = modulePresetIndexById,
        ntc06hSettingIndexByKey = ntc06hSettingIndexByKey,
        selectedModulePreset = selectedModulePreset,
        selectedNtc06hSetting = selectedNtc06hSetting,
        selectedModulePresetIndex = selectedModulePresetIndex,
        selectedNtc06hSettingIndex = selectedNtc06hSettingIndex,
        moduleSelectedTab = moduleSelectedTab,
        expandedModuleFamilyKey = expandedModuleFamilyKey,
        inlineCustomPresetId = inlineCustomPresetId,
        inlineNtc06hSettingKey = inlineNtc06hSettingKey,
        moduleParameterIdText = moduleParameterIdText,
        modulePayloadHexText = modulePayloadHexText,
        modulePersistWrite = modulePersistWrite,
        ntc06hCustomCode = ntc06hCustomCode,
        ntc06hTemplateHexValue = ntc06hTemplateHexValue,
        ntc06hSaveAfterWrite = ntc06hSaveAfterWrite,
    )
    val moduleSectionActions = ModuleConsoleSectionActions(
        onModuleSettingsExpandedChange = { moduleSettingsExpanded = it },
        onModuleSelectedTabChange = { moduleSelectedTab = it },
        onExpandedModuleFamilyKeyChange = { expandedModuleFamilyKey = it },
        onInlineCustomPresetIdChange = { inlineCustomPresetId = it },
        onInlineNtc06hSettingKeyChange = { inlineNtc06hSettingKey = it },
        onSelectedModulePresetIndexChange = { selectedModulePresetIndex = it },
        onSelectedNtc06hSettingIndexChange = { selectedNtc06hSettingIndex = it },
        onModuleParameterIdTextChange = { moduleParameterIdText = it },
        onModulePayloadHexTextChange = { modulePayloadHexText = it },
        onModulePersistWriteToggle = { modulePersistWrite = !modulePersistWrite },
        onNtc06hCustomCodeChange = { ntc06hCustomCode = it },
        onNtc06hTemplateHexValueChange = { ntc06hTemplateHexValue = it },
        onNtc06hSaveAfterWriteToggle = { ntc06hSaveAfterWrite = !ntc06hSaveAfterWrite },
    )
    val dataRuleSectionState = DataRuleConsoleSectionState(
        isExecuting = uiState.isExecuting,
        canExecuteMasterCommands = uiState.hasReadySession && !uiState.isExecuting,
        scanCharsetSummary = uiState.scanCharsetSummary,
        scanTerminatorSummary = uiState.scanTerminatorSummary,
        parseStateExpanded = parseStateExpanded,
        dataRuleExpanded = dataRuleExpanded,
        builderMode = builderMode,
        builderValueA = builderValueA,
        builderValueB = builderValueB,
    )
    val dataRuleSectionActions = DataRuleConsoleSectionActions(
        onParseStateExpandedChange = { parseStateExpanded = it },
        onDataRuleExpandedChange = { dataRuleExpanded = it },
        onBuilderModeChange = { builderMode = it },
        onBuilderValueAChange = { builderValueA = it },
        onBuilderValueBChange = { builderValueB = it },
    )

    LaunchedEffect(activeModuleFamily, modulePresets.size) {
        selectedModulePresetIndex = 0
        if (modulePresets.isNotEmpty()) {
            moduleParameterIdText = formatModuleParameterId(modulePresets.first().parameterId)
            modulePayloadHexText = modulePresets.first().writeOnHex
        } else {
            moduleParameterIdText = ""
            modulePayloadHexText = ""
        }
    }

    LaunchedEffect(selectedModulePresetIndex, modulePresets) {
        val preset = modulePresets.getOrNull(selectedModulePresetIndex) ?: return@LaunchedEffect
        moduleParameterIdText = formatModuleParameterId(preset.parameterId)
        modulePayloadHexText = preset.writeOnHex
    }

    LaunchedEffect(moduleSelectedTab, activeModuleFamily) {
        if (skipInitialModuleReset) {
            skipInitialModuleReset = false
            return@LaunchedEffect
        }
        expandedModuleFamilyKey = null
        inlineCustomPresetId = null
        inlineNtc06hSettingKey = null
    }

    LaunchedEffect(activeModuleFamily) {
        if (activeModuleFamily == ModuleFamily.NTC06H) {
            val firstSetting = Ntc06hSettingCatalog.all.firstOrNull()
            selectedNtc06hSettingIndex = 0
            ntc06hCustomCode = firstSetting?.templateExampleCode?.ifBlank { firstSetting.displayCode }.orEmpty()
            ntc06hTemplateHexValue = firstSetting?.let(::ntc06hTemplateValueOrEmpty).orEmpty()
            ntc06hSaveAfterWrite = firstSetting?.requiresSave ?: true
        }
    }

    LaunchedEffect(selectedNtc06hSettingIndex, ntc06hSettings) {
        val setting = ntc06hSettings.getOrNull(selectedNtc06hSettingIndex) ?: return@LaunchedEffect
        ntc06hCustomCode = setting.templateExampleCode.ifBlank { setting.displayCode }
        ntc06hTemplateHexValue = ntc06hTemplateValueOrEmpty(setting)
        ntc06hSaveAfterWrite = setting.requiresSave
    }

    pendingCommand?.let { command ->
        DangerDialog(
            label = CommandCatalog.titleFor(command),
            message = commandRiskText(command),
            onDismiss = { pendingCommand = null },
            onConfirm = {
                pendingCommand = null
                when (command) {
                    is ConsoleCommand.Basic -> vm.executeBasicDeviceCommand(command.command)
                    is ConsoleCommand.Master -> vm.executeMasterCommand(command.command)
                }
            },
        )
    }

    pendingQuickAction?.let { action ->
        DangerDialog(
            label = action.title,
            message = quickActionRiskText(action),
            onDismiss = { pendingQuickAction = null },
            onConfirm = {
                pendingQuickAction = null
                vm.performQuickAction(action)
            },
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = DemoColors.Page) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DemoColors.Page),
        ) {
            DemoTopBar(
                title = demoStringResource(R.string.scanner_console),
                context = context,
                onBack = onBack,
                titleTestTag = DemoTestTags.CONSOLE_TOP_BAR_TITLE,
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .demoTestTag(DemoTestTags.CONSOLE_PAGE_LIST)
                    .navigationBarsPadding(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    ConsoleStatusCard(
                        statusSummary = uiState.statusSummary.asString(),
                        deviceSummary = uiState.deviceSummary.asString(),
                        selectedModelSummary = uiState.selectedModelSummary.asString(),
                        sdkResolvedModelSummary = uiState.sdkResolvedModelSummary.asString(),
                        capabilitySummary = uiState.capabilitySummary.asString(),
                        diagnosticsSummary = uiState.diagnosticsSummary.asString(),
                        lastActionResult = uiState.lastActionResult?.asString(),
                        errorMessage = uiState.errorMessage?.asString(),
                        isExecuting = uiState.isExecuting,
                        onDisconnect = vm::disconnect,
                    )
                }
                item {
                    DemoScenarioPresetCard()
                }
                item {
                    DemoSectionCard(title = demoStringResource(R.string.operations)) {
                        OperationScopeRow(
                            selected = operationScope,
                            onSelect = { operationScope = it },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = operationScopeSummary(
                                operationScope = operationScope,
                                currentModuleFamily = activeModuleFamily,
                                quickActionCount = quickActionCount,
                                selectedMasterTabTitle = selectedMasterTabItem?.title,
                                selectedMasterCommandCount = selectedMasterCommandCount,
                                moduleCommandCount = if (isNtc06hFamily) ntc06hSettings.size else modulePresets.size,
                            ),
                            fontSize = 12.sp,
                            color = DemoColors.TextSecondary,
                        )

                        if (operationScope == OperationScope.MASTER) {
                            Spacer(modifier = Modifier.height(12.dp))
                            MasterQuickActionsSection(
                                vm = vm,
                                state = masterSectionState,
                                actions = masterSectionActions,
                            )
                        }

                        if (operationScope == OperationScope.MODULE) {
                            Spacer(modifier = Modifier.height(12.dp))
                            ModuleCommandsSection(
                                vm = vm,
                                state = moduleSectionState,
                                actions = moduleSectionActions,
                            )
                        }

                        if (operationScope == OperationScope.MASTER) {
                            Spacer(modifier = Modifier.height(12.dp))
                            MasterCommandsSection(
                                vm = vm,
                                state = masterSectionState,
                                actions = masterSectionActions,
                            )
                        }

                        if (isDataRulesTab) {
                            Spacer(modifier = Modifier.height(12.dp))
                            DataRuleConsoleSection(
                                vm = vm,
                                state = dataRuleSectionState,
                                actions = dataRuleSectionActions,
                            )
                        }
                    }
                }
            }
        }
    }
}

internal data class CommandConsolePageInitialState(
    val startInModuleScope: Boolean = false,
    val moduleSettingsExpanded: Boolean = false,
    val moduleSelectedTab: Int = 0,
    val expandedModuleFamilyKey: String? = null,
    val inlineCustomPresetId: Int? = null,
    val inlineNtc06hSettingKey: String? = null,
    val selectedModulePresetIndex: Int = 0,
    val selectedNtc06hSettingIndex: Int = 0,
    val preserveInlineEditorOnFirstComposition: Boolean = false,
)

private enum class OperationScope {
    MASTER,
    MODULE,
}

@Composable
private fun operationScopeSummary(
    operationScope: OperationScope,
    currentModuleFamily: ModuleFamily,
    quickActionCount: Int,
    selectedMasterTabTitle: String?,
    selectedMasterCommandCount: Int,
    moduleCommandCount: Int,
): String {
    return when (operationScope) {
        OperationScope.MASTER -> demoStringResource(
            R.string.master_mode_summary,
            quickActionCount,
            selectedMasterTabTitle ?: "-",
            selectedMasterCommandCount,
        )
        OperationScope.MODULE -> demoStringResource(
            R.string.module_mode_summary,
            displayModuleFamilyLabel(currentModuleFamily),
            moduleCommandCount,
        )
    }
}

@Composable
private fun OperationScopeRow(
    selected: OperationScope,
    onSelect: (OperationScope) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CommandFilterChip(
            modifier = Modifier.weight(1f),
            label = demoStringResource(R.string.master),
            selected = selected == OperationScope.MASTER,
            onClick = { onSelect(OperationScope.MASTER) },
            testTag = DemoTestTags.CONSOLE_SCOPE_MASTER,
        )
        CommandFilterChip(
            modifier = Modifier.weight(1f),
            label = demoStringResource(R.string.module),
            selected = selected == OperationScope.MODULE,
            onClick = { onSelect(OperationScope.MODULE) },
            testTag = DemoTestTags.CONSOLE_SCOPE_MODULE,
        )
    }
}
