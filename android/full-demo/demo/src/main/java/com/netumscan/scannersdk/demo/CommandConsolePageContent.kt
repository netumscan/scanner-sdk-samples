package com.netumscan.scannersdk.demo

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netumscan.scannersdk.demo.ui.theme.DemoColors
import com.netumscan.scannersdk.model.CapabilityEntry

@Composable
internal fun CommandConsolePageContent(
    vm: CommandConsoleViewModel,
    uiState: CommandConsoleUiState,
    context: Context,
    onBack: () -> Unit,
    initialState: CommandConsolePageInitialState = CommandConsolePageInitialState(),
) {
    data class PendingCapabilityActionRequest(
        val definition: CapabilityEntry,
        val inputValue: String,
    )

    var selectedConsoleTab by rememberSaveable {
        mutableStateOf(if (initialState.startInSettingsScope) ConsoleTab.CAPABILITIES else ConsoleTab.COMMON)
    }
    var selectedDomainKey by rememberSaveable { mutableStateOf<String?>(null) }
    var quickActionsExpanded by rememberSaveable { mutableStateOf(true) }
    var parseStateExpanded by rememberSaveable { mutableStateOf(false) }
    var dataRuleExpanded by rememberSaveable { mutableStateOf(true) }
    var pendingQuickAction by remember { mutableStateOf<QuickActionCommand?>(null) }
    var pendingCapabilityAction by remember { mutableStateOf<PendingCapabilityActionRequest?>(null) }
    var capabilityActionInputs by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var builderMode by rememberSaveable { mutableStateOf(DataRuleFormMode.PREFIX) }
    var builderValueA by rememberSaveable { mutableStateOf("") }
    var builderValueB by rememberSaveable { mutableStateOf("") }

    val selectedDomain = selectedDomainKey?.let { key ->
        uiState.capabilityCatalog.firstOrNull { it.definition.key == key }
    }
    val quickActionRows = CommandCatalog.quickActionRows
    val masterSectionState = MasterConsoleSectionState(
        isExecuting = uiState.isExecuting,
        canExecuteMasterCommands = uiState.hasReadySession && !uiState.isExecuting,
        quickActionsExpanded = quickActionsExpanded,
        capabilityActionsExpanded = false,
        quickActionCount = quickActionRows.sumOf { it.commands.size },
        quickActionRows = quickActionRows,
        actionCapabilityEntries = emptyList(),
    )
    val masterSectionActions = MasterConsoleSectionActions(
        onQuickActionsExpandedChange = { quickActionsExpanded = it },
        onPendingQuickActionChange = { pendingQuickAction = it },
        onCapabilityActionsExpandedChange = {},
        onPendingCapabilityActionChange = {},
    )
    val settingsSectionState = ProductSettingsSectionState(
        isExecuting = uiState.isExecuting,
        isLoadingSettings = uiState.isLoadingSettings,
        hasReadySession = uiState.hasReadySession,
        settingsReadSupported = uiState.settingsReadSupported,
        settingsWriteSupported = uiState.settingsWriteSupported,
        settingDefinitions = uiState.settingDefinitions,
        settingOptionsByKey = uiState.settingOptionsByKey,
        settingGroups = uiState.settingGroups,
        capabilityReadValues = uiState.capabilityReadValues,
        settingDrafts = uiState.settingDrafts,
        settingOperations = uiState.settingOperations,
        settingValidationErrors = uiState.settingValidationErrors,
        settingRecentlyUpdatedKeys = uiState.settingRecentlyUpdatedKeys,
    )
    val dataRulesSupported = uiState.publicCapabilitySummary?.supportsDataRules ?: true
    val dataRuleSectionState = DataRuleConsoleSectionState(
        isExecuting = uiState.isExecuting,
        canExecuteMasterCommands = uiState.hasReadySession && !uiState.isExecuting && dataRulesSupported,
        dataRulesSupported = dataRulesSupported,
        deviceCharsetSummary = uiState.deviceCharsetSummary,
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

    pendingQuickAction?.let { action ->
        DangerDialog(
            label = action.title,
            message = quickActionRiskText(action),
            onDismiss = {
                vm.logRiskActionCancelled(action.title)
                pendingQuickAction = null
            },
            onConfirm = {
                pendingQuickAction = null
                vm.performQuickAction(action)
            },
        )
    }

    pendingCapabilityAction?.let { action ->
        DangerDialog(
            label = deviceActionLabel(action.definition),
            message = capabilityRiskText(action.definition),
            onDismiss = {
                vm.logRiskActionCancelled(deviceActionLabel(action.definition))
                pendingCapabilityAction = null
            },
            onConfirm = {
                pendingCapabilityAction = null
                vm.executeCapabilityAction(action.definition, action.inputValue)
            },
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = DemoColors.Page) {
        if (selectedDomain != null) {
            CapabilityDomainDetailPage(
                vm = vm,
                domain = selectedDomain,
                settingsState = settingsSectionState,
                context = context,
                actionInputs = capabilityActionInputs,
                onActionInputChange = { key, value ->
                    capabilityActionInputs = capabilityActionInputs + (key to value)
                },
                onPendingAction = { definition ->
                    pendingCapabilityAction = PendingCapabilityActionRequest(
                        definition = definition,
                        inputValue = capabilityActionInputs[definition.entryKey].orEmpty(),
                    )
                },
                onBack = { selectedDomainKey = null },
            )
        } else {
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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        DemoSectionCard(title = demoStringResource(R.string.console_tools)) {
                            ConsoleTabRow(
                                selected = selectedConsoleTab,
                                onSelect = { selectedConsoleTab = it },
                            )
                        }
                    }
                    when (selectedConsoleTab) {
                        ConsoleTab.COMMON -> {
                            item {
                                ScanWorkspaceCard(
                                    vm = vm,
                                    uiState = uiState,
                                    dataRuleState = dataRuleSectionState,
                                    dataRuleActions = dataRuleSectionActions,
                                )
                            }
                            item {
                                DemoSectionCard(title = demoStringResource(R.string.common_functions)) {
                                    MasterQuickActionsSection(
                                        vm = vm,
                                        state = masterSectionState,
                                        actions = masterSectionActions,
                                    )
                                }
                            }
                        }
                        ConsoleTab.CAPABILITIES -> {
                            item {
                                DemoSectionCard(title = demoStringResource(R.string.device_capabilities)) {
                                    Text(
                                        text = demoStringResource(R.string.capability_catalog_description),
                                        fontSize = 12.sp,
                                        color = DemoColors.TextSecondary,
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    CapabilityDomainList(
                                        catalog = uiState.capabilityCatalog,
                                        isLoading = uiState.isLoadingCapabilityCatalog,
                                        errorText = uiState.capabilityCatalogError?.asString(),
                                        onRetry = vm::reloadCapabilityCatalog,
                                        onOpenDomain = { domain ->
                                            vm.onCapabilityDomainOpened(domain)
                                            selectedDomainKey = domain.definition.key
                                        },
                                    )
                                }
                            }
                        }
                        ConsoleTab.DATA_RULES -> {
                            item {
                                DemoSectionCard(title = demoStringResource(R.string.data_rules)) {
                                    DataRuleConsoleSection(
                                        vm = vm,
                                        state = dataRuleSectionState,
                                        actions = dataRuleSectionActions,
                                        showParseControls = false,
                                    )
                                }
                            }
                        }
                        ConsoleTab.DIAGNOSTICS -> {
                            item {
                                ConsoleStatusCard(
                                    statusSummary = uiState.statusSummary.asString(),
                                    deviceSummary = uiState.deviceSummary.asString(),
                                    selectedModelSummary = uiState.selectedModelSummary.asString(),
                                    sdkResolvedModelSummary = uiState.sdkResolvedModelSummary.asString(),
                                    capabilitySummary = uiState.capabilitySummary.asString(),
                                    initializationSummary = uiState.initializationSummary.asString(),
                                    diagnosticsSummary = DemoDiagnosticsStore.compactSummaryText().asString(),
                                    lastActionResult = uiState.lastActionResult?.asString(),
                                    errorMessage = uiState.errorMessage?.asString(),
                                    isExecuting = uiState.isExecuting,
                                    onDisconnect = vm::disconnect,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CapabilityDomainDetailPage(
    vm: CommandConsoleViewModel,
    domain: CapabilityCatalogDomain,
    settingsState: ProductSettingsSectionState,
    context: Context,
    actionInputs: Map<String, String>,
    onActionInputChange: (String, String) -> Unit,
    onPendingAction: (CapabilityEntry) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.Page),
    ) {
        DemoTopBar(
            title = domain.title,
            context = context,
            onBack = onBack,
            showLogsEntry = true,
            titleTestTag = DemoTestTags.CAPABILITY_DOMAIN_DETAIL_TITLE,
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .demoTestTag(DemoTestTags.CAPABILITY_DOMAIN_DETAIL_LIST)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                CapabilityDomainDetailContent(
                    vm = vm,
                    domain = domain,
                    settingsState = settingsState,
                    actionInputs = actionInputs,
                    onActionInputChange = onActionInputChange,
                    onPendingAction = onPendingAction,
                )
            }
        }
    }
}

internal data class CommandConsolePageInitialState(
    val startInSettingsScope: Boolean = false,
)

private enum class ConsoleTab {
    COMMON,
    CAPABILITIES,
    DATA_RULES,
    DIAGNOSTICS,
}

@Composable
private fun ConsoleTabRow(
    selected: ConsoleTab,
    onSelect: (ConsoleTab) -> Unit,
) {
    val tabs = ConsoleTab.entries
    ScrollableTabRow(
        selectedTabIndex = tabs.indexOf(selected).coerceAtLeast(0),
        modifier = Modifier.fillMaxWidth(),
        edgePadding = 0.dp,
        containerColor = DemoColors.Surface,
        contentColor = DemoColors.AccentStrong,
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                modifier = Modifier.demoTestTag(consoleTabTestTag(tab)),
                text = { Text(text = consoleTabLabel(tab), fontSize = 13.sp) },
                selectedContentColor = DemoColors.AccentStrong,
                unselectedContentColor = DemoColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun consoleTabLabel(tab: ConsoleTab): String = when (tab) {
    ConsoleTab.COMMON -> demoStringResource(R.string.common_functions)
    ConsoleTab.CAPABILITIES -> demoStringResource(R.string.device_capabilities)
    ConsoleTab.DATA_RULES -> demoStringResource(R.string.data_rules)
    ConsoleTab.DIAGNOSTICS -> demoStringResource(R.string.diagnostics)
}

private fun consoleTabTestTag(tab: ConsoleTab): String = when (tab) {
    ConsoleTab.COMMON -> DemoTestTags.CONSOLE_SCOPE_MASTER
    ConsoleTab.CAPABILITIES -> DemoTestTags.CONSOLE_SCOPE_SETTINGS
    ConsoleTab.DATA_RULES -> DemoTestTags.CONSOLE_SCOPE_DATA_RULES
    ConsoleTab.DIAGNOSTICS -> DemoTestTags.CONSOLE_SCOPE_DIAGNOSTICS
}
