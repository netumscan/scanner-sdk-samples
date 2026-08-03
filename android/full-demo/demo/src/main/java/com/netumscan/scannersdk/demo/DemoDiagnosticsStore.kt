package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.SessionState

internal data class DemoPlatformDiagnostics(
    val demoVersion: String = "-",
    val demoBuild: String = "-",
    val sdkVersion: String = "-",
    val sdkCommit: String = "-",
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
    val selectedModelKey: String = "",
    val resolvedModelKey: String = "",
    val sessionState: SessionState? = null,
    val recentSessionInitStage: String? = null,
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

    fun updateSelectedModel(modelKey: String) {
        state = state.copy(selectedModelKey = modelKey)
    }

    fun updateResolvedModel(modelKey: String) {
        state = state.copy(resolvedModelKey = modelKey)
    }

    fun updateSessionState(sessionState: SessionState?) {
        state = state.copy(sessionState = sessionState)
    }

    fun updateRecentSessionInitStage(detail: String?) {
        state = state.copy(recentSessionInitStage = detail)
    }

    fun updateRecentFailure(detail: String?) {
        state = state.copy(recentFailure = detail)
    }

    fun updateFakeMode(enabled: Boolean) {
        state = state.copy(fakeMode = enabled)
    }

    fun snapshot(): DemoDiagnosticsState = state

    fun summaryText(): UiText = dynamicText { formatSummary(state) }
    fun compactSummaryText(): UiText = dynamicText { formatCompactSummary(state) }

    private fun formatSummary(state: DemoDiagnosticsState): String {
        val platform = state.platform
        return listOf(
            "${DemoStrings.text(R.string.diagnostics_demo_platform)}: ${platform.demoVersion} (${platform.demoBuild}) / ${platform.androidVersion}",
            "SDK: ${platform.sdkVersion} / ${platform.sdkCommit}",
            "${DemoStrings.text(R.string.diagnostics_permissions)}: scan=${flag(platform.bluetoothScanPermissionGranted)} connect=${flag(platform.bluetoothConnectPermissionGranted)} location=${flag(platform.fineLocationPermissionGranted)}",
            "${DemoStrings.text(R.string.diagnostics_platform_state)}: bluetooth=${flag(platform.bluetoothEnabled)} location=${flag(platform.locationEnabled)}",
            "${DemoStrings.text(R.string.diagnostics_sdk_state)}: initialized=${flag(state.sdkInitialized)} transport=${state.transportMode?.summary() ?: DemoStrings.text(R.string.none)}",
            "${DemoStrings.text(R.string.diagnostics_model_state)}: selected=${modelLabel(state.selectedModelKey)} resolved=${modelLabel(state.resolvedModelKey)}",
            "${DemoStrings.text(R.string.diagnostics_session_state)}: ${state.sessionState?.name ?: DemoStrings.text(R.string.none)}",
            "${DemoStrings.text(R.string.diagnostics_session_init_stage)}: ${state.recentSessionInitStage ?: DemoStrings.text(R.string.none)}",
            "${DemoStrings.text(R.string.diagnostics_recent_failure)}: ${state.recentFailure ?: DemoStrings.text(R.string.none)}",
            "${DemoStrings.text(R.string.diagnostics_fake_mode)}: ${flag(state.fakeMode)}",
        ).joinToString(separator = "\n")
    }

    private fun formatCompactSummary(state: DemoDiagnosticsState): String {
        return buildString {
            append("transport=")
            append(state.transportMode?.summary() ?: DemoStrings.text(R.string.none))
            append("  session=")
            append(state.sessionState?.name ?: DemoStrings.text(R.string.none))
            append("  init=")
            append(state.recentSessionInitStage ?: DemoStrings.text(R.string.none))
            state.recentFailure?.takeIf { it.isNotBlank() }?.let {
                append("  fail=")
                append(it)
            }
        }
    }

    private fun flag(value: Boolean?): String {
        return when (value) {
            true -> DemoStrings.text(R.string.diagnostics_yes)
            false -> DemoStrings.text(R.string.diagnostics_no)
            null -> DemoStrings.text(R.string.diagnostics_unknown)
        }
    }

    private fun modelLabel(modelKey: String): String {
        return if (modelKey == "") {
            DemoStrings.text(R.string.none)
        } else {
            displayModelLabel(modelKey)
        }
    }
}
