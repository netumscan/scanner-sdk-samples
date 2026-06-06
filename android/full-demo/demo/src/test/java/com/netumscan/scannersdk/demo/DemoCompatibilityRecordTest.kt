package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.SessionState
import com.netumscan.scannersdk.model.DeviceModelId
import kotlin.test.Test
import kotlin.test.assertTrue

class DemoCompatibilityRecordTest {
    @Test
    fun recordIncludesPlatformTransportModelSessionAndFakeMode() {
        DemoDiagnosticsStore.updatePlatform(
            DemoPlatformDiagnostics(
                demoVersion = "0.1.3",
                androidVersion = "Android 15",
                bluetoothEnabled = true,
                locationEnabled = true,
                bluetoothScanPermissionGranted = true,
                bluetoothConnectPermissionGranted = false,
                fineLocationPermissionGranted = true,
            )
        )
        DemoDiagnosticsStore.updateTransport(DemoTransportMode.SPP)
        DemoDiagnosticsStore.updateSelectedModel(DeviceModelId.CS7501)
        DemoDiagnosticsStore.updateResolvedModel(DeviceModelId.CS7501)
        DemoDiagnosticsStore.updateSessionState(SessionState.READY)
        DemoDiagnosticsStore.updateRecentFailure("none")
        DemoDiagnosticsStore.updateFakeMode(true)

        val record = DemoCompatibilityRecord.format()

        assertTrue(record.contains("platform=Android"))
        assertTrue(record.contains("transport=SPP"))
        assertTrue(record.contains("selectedModel=CS7501"))
        assertTrue(record.contains("sessionState=READY"))
        assertTrue(record.contains("fakeMode=true"))
    }
}
