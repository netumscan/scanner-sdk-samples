package com.netumscan.scannersdk.demo

import android.Manifest
import android.os.Build
import com.netumscan.scannersdk.TransportType
import com.netumscan.scannersdk.model.BleScanIssue
import com.netumscan.scannersdk.model.DiscoveryFailure
import com.netumscan.scannersdk.model.DiscoveryFailureCode
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MainActivityPermissionTest {

    @Test
    fun discoveryPermissionsForSAndAbove_matchBleRuntimePermissions() {
        assertContentEquals(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            discoveryPermissionsForSdk(Build.VERSION_CODES.S, DemoTransportMode.BLE),
        )
    }

    @Test
    fun discoveryPermissionsForPreS_keepFineLocationRequirement() {
        assertContentEquals(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            discoveryPermissionsForSdk(Build.VERSION_CODES.R, DemoTransportMode.BLE),
        )
    }

    @Test
    fun sppPermissionsForSAndAbove_requireScanConnectAndFineLocation() {
        assertContentEquals(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            discoveryPermissionsForSdk(Build.VERSION_CODES.S, DemoTransportMode.SPP),
        )
    }

    @Test
    fun sppPermissionsForPreS_keepFineLocationRequirement() {
        assertContentEquals(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            discoveryPermissionsForSdk(Build.VERSION_CODES.R, DemoTransportMode.SPP),
        )
    }

    @Test
    fun discoveryOnSAndAbove_doesNotRequireLocationServiceToggle() {
        kotlin.test.assertFalse(requiresLocationServiceForDiscovery(Build.VERSION_CODES.S, DemoTransportMode.BLE))
    }

    @Test
    fun discoveryOnPreS_requiresLocationServiceToggle() {
        kotlin.test.assertTrue(requiresLocationServiceForDiscovery(Build.VERSION_CODES.R, DemoTransportMode.BLE))
    }

    @Test
    fun sppDiscoveryOnPreS_requiresLocationServiceToggle() {
        kotlin.test.assertTrue(requiresLocationServiceForDiscovery(Build.VERSION_CODES.R, DemoTransportMode.SPP))
    }

    @Test
    fun permissionDeniedReport_exposesMissingPermissionsAndSettingsAction() {
        val originalLanguage = DemoLocaleController.currentLanguage
        try {
            DemoLocaleController.setLanguageForTest(DemoLanguage.ZH)
            val vm = DemoViewModel()

            vm.reportDiscoveryPermissionsDenied(
                deniedPermissions = listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
                includeAndroid12LocationNote = true,
            )

            assertEquals(DiscoveryBlockerAction.OPEN_APP_SETTINGS, vm.uiState.value.blockerAction)
            assertTrue(vm.uiState.value.errorMessage.orEmpty().contains("蓝牙扫描"))
            assertTrue(vm.uiState.value.errorMessage.orEmpty().contains("某些 Android 12+ ROM"))
        } finally {
            DemoLocaleController.setLanguageForTest(originalLanguage)
        }
    }

    @Test
    fun locationDisabledReport_exposesLocationSettingsAction() {
        val originalLanguage = DemoLocaleController.currentLanguage
        try {
            DemoLocaleController.setLanguageForTest(DemoLanguage.ZH)
            val vm = DemoViewModel()

            vm.reportDiscoveryLocationDisabled()

            assertEquals(DiscoveryBlockerAction.OPEN_LOCATION_SETTINGS, vm.uiState.value.blockerAction)
            assertTrue(vm.uiState.value.errorMessage.orEmpty().contains("定位服务未开启"))
        } finally {
            DemoLocaleController.setLanguageForTest(originalLanguage)
        }
    }

    @Test
    fun android12ScanFailure_canSuggestLocationSettingsWithoutPreBlockingScan() {
        val failure = DiscoveryFailure(
            transportType = TransportType.BLE_GATT,
            code = DiscoveryFailureCode.BLE_UNFILTERED_SCAN_FAILED,
            message = "scan failed",
            bleScanIssue = BleScanIssue.INTERNAL_ERROR,
            recoverable = false,
        )

        assertEquals(
            DiscoveryBlockerAction.OPEN_LOCATION_SETTINGS,
            discoveryBlockerActionForFailure(failure, sdkInt = Build.VERSION_CODES.S),
        )
    }

    @Test
    fun recoverableScanFailure_doesNotExposeBlockerAction() {
        val failure = DiscoveryFailure(
            transportType = TransportType.BLE_GATT,
            code = DiscoveryFailureCode.BLE_FILTERED_SCAN_FAILED,
            message = "scan failed",
            bleScanIssue = BleScanIssue.INTERNAL_ERROR,
            recoverable = true,
        )

        assertEquals(null, discoveryBlockerActionForFailure(failure, sdkInt = Build.VERSION_CODES.S))
    }
}
