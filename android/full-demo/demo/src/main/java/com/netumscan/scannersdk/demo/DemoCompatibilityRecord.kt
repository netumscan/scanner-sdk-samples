package com.netumscan.scannersdk.demo

internal object DemoCompatibilityRecord {
    fun format(state: DemoDiagnosticsState = DemoDiagnosticsStore.snapshot()): String {
        val platform = state.platform
        return listOf(
            "platform=Android",
            "demoVersion=${platform.demoVersion}",
            "androidVersion=${platform.androidVersion}",
            "bluetoothEnabled=${formatFlag(platform.bluetoothEnabled)}",
            "locationEnabled=${formatFlag(platform.locationEnabled)}",
            "bluetoothScanPermission=${formatFlag(platform.bluetoothScanPermissionGranted)}",
            "bluetoothConnectPermission=${formatFlag(platform.bluetoothConnectPermissionGranted)}",
            "fineLocationPermission=${formatFlag(platform.fineLocationPermissionGranted)}",
            "transport=${state.transportMode?.name ?: "none"}",
            "selectedModel=${state.selectedModelId}",
            "resolvedModel=${state.resolvedModelId}",
            "sessionState=${state.sessionState?.name ?: "none"}",
            "recentFailure=${state.recentFailure ?: "none"}",
            "fakeMode=${state.fakeMode}",
        ).joinToString(separator = "\n")
    }

    private fun formatFlag(value: Boolean?): String {
        return when (value) {
            true -> "yes"
            false -> "no"
            null -> "unknown"
        }
    }
}
