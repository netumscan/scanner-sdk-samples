package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.DeviceCapabilitySummary
import com.netumscan.scannersdk.model.ScanTextCharset
import com.netumscan.scannersdk.model.CapabilityDomain
import com.netumscan.scannersdk.model.CapabilityEntry
import com.netumscan.scannersdk.model.CapabilityValue

internal data class CommandConsoleUiState(
    val statusSummary: UiText = uiText(R.string.not_connected),
    val deviceSummary: UiText = uiText(R.string.no_selected_device),
    val infoSummary: UiText = uiText(R.string.device_info_not_loaded),
    val selectedModelSummary: UiText = uiText(R.string.selected_model_not_provided),
    val sdkResolvedModelSummary: UiText = uiText(R.string.sdk_resolved_model_not_loaded),
    val capabilitySummary: UiText = uiText(R.string.capability_summary_not_loaded),
    val initializationSummary: UiText = uiText(R.string.session_initialization_idle),
    val diagnosticsSummary: UiText = DemoDiagnosticsStore.summaryText(),
    val publicCapabilitySummary: DeviceCapabilitySummary? = null,
    val capabilityDomains: List<CapabilityDomain> = emptyList(),
    val capabilityEntries: List<CapabilityEntry> = emptyList(),
    val capabilityCatalog: List<CapabilityCatalogDomain> = emptyList(),
    val isLoadingCapabilityCatalog: Boolean = false,
    val capabilityCatalogError: UiText? = null,
    val actionCapabilityEntries: List<CapabilityEntry> = emptyList(),
    val settingDefinitions: List<CapabilityEntry> = emptyList(),
    val settingOptionsByKey: Map<String, List<ProductSettingOption>> = emptyMap(),
    val settingGroups: List<ProductSettingGroup> = emptyList(),
    val settingsReadSupported: Boolean = false,
    val settingsWriteSupported: Boolean = false,
    val capabilityReadValues: Map<String, CapabilityValue> = emptyMap(),
    val settingDrafts: Map<String, ProductSettingDraft> = emptyMap(),
    val settingOperations: Map<String, ProductSettingOperation> = emptyMap(),
    val settingValidationErrors: Map<String, String> = emptyMap(),
    val settingRecentlyUpdatedKeys: Set<String> = emptySet(),
    val expandedSettingGroupKey: String? = null,
    val isLoadingSettings: Boolean = false,
    val deviceCharsetSummary: UiText = uiText(R.string.device_config_not_auto_queried),
    val deviceTerminalSummary: UiText = uiText(R.string.device_terminator_not_auto_queried),
    val bluetoothFirmwareVersionSummary: UiText = uiText(R.string.setting_value_not_loaded),
    val scanCharsetSummary: String = ScanTextCharset.UTF_8.displayName,
    val scanTerminatorSummary: String = "0D",
    val scanCount: Int = 0,
    val lastScanText: String? = null,
    val lastScanMeta: UiText = uiText(R.string.last_scan_waiting),
    val lastScanRawHex: String = "",
    val hasReadySession: Boolean = false,
    val isExecuting: Boolean = false,
    val events: List<DebugEvent> = emptyList(),
    val lastActionResult: UiText? = null,
    val errorMessage: UiText? = null,
)
