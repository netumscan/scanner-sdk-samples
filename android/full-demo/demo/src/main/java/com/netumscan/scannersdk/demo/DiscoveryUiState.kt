package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.DiscoveredDevice

enum class DiscoveryBlockerAction {
    OPEN_APP_SETTINGS,
    OPEN_LOCATION_SETTINGS,
    OPEN_BLUETOOTH_SETTINGS,
}

data class DiscoveryUiState(
    val statusSummary: UiText = uiText(R.string.sdk_not_initialized),
    val selectedDeviceSummary: UiText = uiText(R.string.no_connected_device),
    val selectedModelKey: String = "CS7501",
    val selectedModelSummary: UiText = rawDisplayText("CS7501"),
    val supportedModels: List<DemoSupportedModel> = emptyList(),
    val isLoadingSupportedModels: Boolean = false,
    val supportedModelsError: UiText? = null,
    val selectedTransportMode: DemoTransportMode? = null,
    val selectedTransportSummary: UiText = uiText(R.string.transport_not_selected),
    val diagnosticsSummary: UiText = DemoDiagnosticsStore.summaryText(),
    val devices: List<DiscoveredDevice> = emptyList(),
    val events: List<DebugEvent> = emptyList(),
    val lastActionResult: UiText? = null,
    val errorMessage: UiText? = null,
    val blockerAction: DiscoveryBlockerAction? = null,
    val isInitialized: Boolean = false,
    val isDiscovering: Boolean = false,
    val isConnecting: Boolean = false,
    val connectingDeviceId: String? = null,
    val hasActiveSession: Boolean = false,
) {
    val canInitializeSdk: Boolean
        get() = !isInitialized

    val canChangeDiscoveryTarget: Boolean
        get() = !isConnecting && !hasActiveSession

    val canStartDiscovery: Boolean
        get() = isInitialized &&
            selectedTransportMode != null &&
            supportedModels.any { it.modelKey == selectedModelKey } &&
            !isLoadingSupportedModels &&
            supportedModelsError == null &&
            !isDiscovering &&
            canChangeDiscoveryTarget

    val canStopDiscovery: Boolean
        get() = isInitialized && isDiscovering

    val canOpenConsole: Boolean
        get() = hasActiveSession && !isConnecting

    val canDisconnect: Boolean
        get() = hasActiveSession && !isConnecting

    val canConnectDiscoveredDevice: Boolean
        get() = isInitialized && !isConnecting && !hasActiveSession
}
