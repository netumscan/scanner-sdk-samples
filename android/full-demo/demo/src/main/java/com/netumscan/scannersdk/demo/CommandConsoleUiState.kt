package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.ModuleFamily
import com.netumscan.scannersdk.model.ScanTextCharset

data class CommandConsoleUiState(
    val statusSummary: UiText = uiText(R.string.not_connected),
    val deviceSummary: UiText = uiText(R.string.no_selected_device),
    val infoSummary: UiText = uiText(R.string.device_info_not_loaded),
    val selectedModelSummary: UiText = uiText(R.string.selected_model_not_provided),
    val sdkResolvedModelSummary: UiText = uiText(R.string.sdk_resolved_model_not_loaded),
    val capabilitySummary: UiText = uiText(R.string.capability_summary_not_loaded),
    val diagnosticsSummary: UiText = DemoDiagnosticsStore.summaryText(),
    val moduleSummary: UiText = uiText(R.string.module_capability_not_loaded),
    val currentModuleFamily: ModuleFamily = ModuleFamily.UNKNOWN,
    val supportsModuleCommands: Boolean = false,
    val canExecuteModuleCommands: Boolean = false,
    val deviceCharsetSummary: UiText = uiText(R.string.device_config_not_auto_queried),
    val deviceTerminalSummary: UiText = uiText(R.string.device_terminator_not_auto_queried),
    val scanCharsetSummary: String = ScanTextCharset.UTF_8.displayName,
    val scanTerminatorSummary: String = "0D",
    val hasReadySession: Boolean = false,
    val isExecuting: Boolean = false,
    val events: List<DebugEvent> = emptyList(),
    val lastActionResult: UiText? = null,
    val errorMessage: UiText? = null,
)
