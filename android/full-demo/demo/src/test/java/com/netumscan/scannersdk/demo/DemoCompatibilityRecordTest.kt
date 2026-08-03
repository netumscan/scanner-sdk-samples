package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.SessionState
import kotlin.test.Test
import kotlin.test.assertTrue

class DemoCompatibilityRecordTest {
    @Test
    fun recordIncludesPlatformTransportModelSessionAndFakeMode() {
        DemoDiagnosticsStore.updatePlatform(
            DemoPlatformDiagnostics(
                demoVersion = "1.0.0",
                demoBuild = "12",
                sdkVersion = "1.0.0",
                sdkCommit = "4989fda4",
                androidVersion = "Android 15",
                bluetoothEnabled = true,
                locationEnabled = true,
                bluetoothScanPermissionGranted = true,
                bluetoothConnectPermissionGranted = false,
                fineLocationPermissionGranted = true,
            )
        )
        DemoDiagnosticsStore.updateTransport(DemoTransportMode.SPP)
        DemoDiagnosticsStore.updateSelectedModel("CS7501")
        DemoDiagnosticsStore.updateResolvedModel("CS7501")
        DemoDiagnosticsStore.updateSessionState(SessionState.READY)
        DemoDiagnosticsStore.updateRecentFailure("none")
        DemoDiagnosticsStore.updateFakeMode(true)

        val record = DemoCompatibilityRecord.format()

        assertTrue(record.contains("platform=Android"))
        assertTrue(record.contains("demoBuild=12"))
        assertTrue(record.contains("sdkVersion=1.0.0"))
        assertTrue(record.contains("sdkCommit=4989fda4"))
        assertTrue(record.contains("transport=SPP"))
        assertTrue(record.contains("selectedModel=CS7501"))
        assertTrue(record.contains("sessionState=READY"))
        assertTrue(record.contains("fakeMode=true"))
    }
}
