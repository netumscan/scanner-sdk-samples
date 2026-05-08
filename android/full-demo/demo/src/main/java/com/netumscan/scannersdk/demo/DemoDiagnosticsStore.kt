package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.SessionState
import com.netumscan.scannersdk.model.DeviceModelId

internal data class DemoPlatformDiagnostics(
    val demoVersion: String = "-",
    val androidVersion: String = "-",
    val bluetoothEnabled: Boolean? = null,
    val locationEnabled: Boolean? = null,
    val bluetoothScanPermissionGranted: Boolean? = null,
    val bluetoothConnectPermissionGranted: Boolean? = null,
    val fineLocationPermissionGranted: Boolean? = null,
)

internal data class DemoDiagnosticsState(
    val platform: DemoPlatformDiagnostics = DemoPlatformDiagnostics(),
    val sdkInitialized: Boolean = false,
    val transportMode: DemoTransportMode? = null,
    val selectedModelId: DeviceModelId = DeviceModelId.UNKNOWN,
    val resolvedModelId: DeviceModelId = DeviceModelId.UNKNOWN,
    val sessionState: SessionState? = null,
    val recentFailure: String? = null,
    val fakeMode: Boolean = false,
)

internal object DemoDiagnosticsStore {
    private var state = DemoDiagnosticsState()

    fun updatePlatform(platform: DemoPlatformDiagnostics) {
        state = state.copy(platform = platform)
    }

    fun updateSdkInitialized(initialized: Boolean) {
        state = state.copy(sdkInitialized = initialized)
    }

    fun updateTransport(mode: DemoTransportMode?) {
        state = state.copy(transportMode = mode)
    }

    fun updateSelectedModel(modelId: DeviceModelId) {
        state = state.copy(selectedModelId = modelId)
    }

    fun updateResolvedModel(modelId: DeviceModelId) {
        state = state.copy(resolvedModelId = modelId)
    }

    fun updateSessionState(sessionState: SessionState?) {
        state = state.copy(sessionState = sessionState)
    }

    fun updateRecentFailure(detail: String?) {
        state = state.copy(recentFailure = detail)
    }

    fun updateFakeMode(enabled: Boolean) {
        state = state.copy(fakeMode = enabled)
    }

    fun snapshot(): DemoDiagnosticsState = state

    fun summaryText(): UiText = dynamicText { formatSummary(state) }

    private fun formatSummary(state: DemoDiagnosticsState): String {
        val platform = state.platform
        return listOf(
            "${DemoStrings.text(R.string.diagnostics_demo_platform)}: ${platform.demoVersion} / ${platform.androidVersion}",
            "${DemoStrings.text(R.string.diagnostics_permissions)}: scan=${flag(platform.bluetoothScanPermissionGranted)} connect=${flag(platform.bluetoothConnectPermissionGranted)} location=${flag(platform.fineLocationPermissionGranted)}",
            "${DemoStrings.text(R.string.diagnostics_platform_state)}: bluetooth=${flag(platform.bluetoothEnabled)} location=${flag(platform.locationEnabled)}",
            "${DemoStrings.text(R.string.diagnostics_sdk_state)}: initialized=${flag(state.sdkInitialized)} transport=${state.transportMode?.summary() ?: DemoStrings.text(R.string.none)}",
            "${DemoStrings.text(R.string.diagnostics_model_state)}: selected=${modelLabel(state.selectedModelId)} resolved=${modelLabel(state.resolvedModelId)}",
            "${DemoStrings.text(R.string.diagnostics_session_state)}: ${state.sessionState?.name ?: DemoStrings.text(R.string.none)}",
            "${DemoStrings.text(R.string.diagnostics_recent_failure)}: ${state.recentFailure ?: DemoStrings.text(R.string.none)}",
            "${DemoStrings.text(R.string.diagnostics_fake_mode)}: ${flag(state.fakeMode)}",
        ).joinToString(separator = "\n")
    }

    private fun flag(value: Boolean?): String {
        return when (value) {
            true -> DemoStrings.text(R.string.diagnostics_yes)
            false -> DemoStrings.text(R.string.diagnostics_no)
            null -> DemoStrings.text(R.string.diagnostics_unknown)
        }
    }

    private fun modelLabel(modelId: DeviceModelId): String {
        return if (modelId == DeviceModelId.UNKNOWN) {
            DemoStrings.text(R.string.none)
        } else {
            displayModelLabel(modelId)
        }
    }
}
