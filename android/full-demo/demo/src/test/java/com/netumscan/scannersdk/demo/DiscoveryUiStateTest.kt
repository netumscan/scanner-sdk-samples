package com.netumscan.scannersdk.demo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiscoveryUiStateTest {
    private val supportedModels = listOf(
        DemoSupportedModel("CS7501", "CS7501", "c_pro", "C Pro series"),
    )

    @Test
    fun defaultState_disablesDiscoveryActionsUntilInitializedAndTransportSelected() {
        val state = DiscoveryUiState()

        assertTrue(state.canInitializeSdk)
        assertFalse(state.canStartDiscovery)
        assertFalse(state.canStopDiscovery)
        assertFalse(state.canConnectDiscoveredDevice)
    }

    @Test
    fun initializedBleState_allowsStartButNotStopUntilDiscoveryRuns() {
        val state = DiscoveryUiState(
            isInitialized = true,
            selectedTransportMode = DemoTransportMode.BLE,
            supportedModels = supportedModels,
        )

        assertFalse(state.canInitializeSdk)
        assertTrue(state.canStartDiscovery)
        assertFalse(state.canStopDiscovery)
        assertTrue(state.canConnectDiscoveredDevice)
    }

    @Test
    fun discoveringState_disablesDuplicateStartAndAllowsStop() {
        val state = DiscoveryUiState(
            isInitialized = true,
            isDiscovering = true,
            selectedTransportMode = DemoTransportMode.BLE,
            supportedModels = supportedModels,
        )

        assertFalse(state.canStartDiscovery)
        assertTrue(state.canStopDiscovery)
    }

    @Test
    fun activeSessionState_disablesTargetChangesAndEnablesSessionActions() {
        val state = DiscoveryUiState(
            isInitialized = true,
            selectedTransportMode = DemoTransportMode.BLE,
            supportedModels = supportedModels,
            hasActiveSession = true,
        )

        assertFalse(state.canChangeDiscoveryTarget)
        assertFalse(state.canStartDiscovery)
        assertFalse(state.canConnectDiscoveredDevice)
        assertTrue(state.canOpenConsole)
        assertTrue(state.canDisconnect)
    }

    @Test
    fun connectingState_tracksOnlyThePendingDevice() {
        val state = DiscoveryUiState(
            isInitialized = true,
            selectedTransportMode = DemoTransportMode.BLE,
            isConnecting = true,
            connectingDeviceId = "device-1",
        )

        assertFalse(state.canConnectDiscoveredDevice)
        assertEquals("device-1", state.connectingDeviceId)
    }

    @Test
    fun modelCatalogLoadingOrFailure_disablesDiscoveryStart() {
        val loading = DiscoveryUiState(
            isInitialized = true,
            selectedTransportMode = DemoTransportMode.BLE,
            supportedModels = supportedModels,
            isLoadingSupportedModels = true,
        )
        val failed = loading.copy(
            isLoadingSupportedModels = false,
            supportedModelsError = rawDisplayText("failed"),
        )

        assertFalse(loading.canStartDiscovery)
        assertFalse(failed.canStartDiscovery)
    }
}
