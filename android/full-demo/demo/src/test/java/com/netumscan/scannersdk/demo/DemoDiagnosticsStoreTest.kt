package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.SessionState
import com.netumscan.scannersdk.model.DeviceModelId
import kotlin.test.Test
import kotlin.test.assertTrue

class DemoDiagnosticsStoreTest {
    @Test
    fun summaryIncludesPlatformPermissionsModelsAndRecentFailure() {
        val originalLanguage = DemoLocaleController.currentLanguage
        try {
            DemoLocaleController.setLanguageForTest(DemoLanguage.EN)
            DemoDiagnosticsStore.updatePlatform(
                DemoPlatformDiagnostics(
                    demoVersion = "1.2.3",
                    androidVersion = "Android 15 (API 35)",
                    bluetoothEnabled = true,
                    locationEnabled = false,
                    bluetoothScanPermissionGranted = true,
                    bluetoothConnectPermissionGranted = true,
                    fineLocationPermissionGranted = false,
                )
            )
            DemoDiagnosticsStore.updateSdkInitialized(true)
            DemoDiagnosticsStore.updateTransport(DemoTransportMode.BLE)
            DemoDiagnosticsStore.updateSelectedModel(DeviceModelId.CS7501)
            DemoDiagnosticsStore.updateResolvedModel(DeviceModelId.CS7501)
            DemoDiagnosticsStore.updateSessionState(SessionState.READY)
            DemoDiagnosticsStore.updateRecentFailure("last failure")

            val summary = DemoDiagnosticsStore.summaryText().asStringForCurrentLanguage()

            assertTrue(summary.contains("1.2.3 / Android 15"))
            assertTrue(summary.contains("scan=Yes"))
            assertTrue(summary.contains("location=No"))
            assertTrue(summary.contains("initialized=Yes"))
            assertTrue(summary.contains("selected=CS7501"))
            assertTrue(summary.contains("READY"))
            assertTrue(summary.contains("last failure"))
        } finally {
            DemoLocaleController.setLanguageForTest(originalLanguage)
        }
    }
}
