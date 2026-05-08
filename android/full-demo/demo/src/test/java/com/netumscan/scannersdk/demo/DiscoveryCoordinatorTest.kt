package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.SessionState
import com.netumscan.scannersdk.TransportType
import com.netumscan.scannersdk.model.DeviceModelId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DiscoveryCoordinatorTest {
    @Test
    fun fakeDiscoveryReturnsStableBleAndSppDevices() = runTest {
        val coordinator = DiscoveryCoordinator(FakeDemoDiscoveryBackend())

        val ble = coordinator.startDiscovery(DemoTransportMode.BLE, DeviceModelId.CS7501)
        val spp = coordinator.startDiscovery(DemoTransportMode.SPP, DeviceModelId.CS7501)

        assertTrue(coordinator.isFakeMode)
        assertEquals(TransportType.BLE_GATT, ble.devices.single().transportType)
        assertEquals(TransportType.SPP_CLASSIC, spp.devices.single().transportType)
        assertTrue(ble.devices.single().deviceId.startsWith("FAKE-BLE"))
    }

    @Test
    fun fakeConnectReturnsReadySessionHandle() = runTest {
        val coordinator = DiscoveryCoordinator(FakeDemoDiscoveryBackend())
        val device = coordinator.startDiscovery(DemoTransportMode.BLE, DeviceModelId.CS7501).devices.single()

        val result = coordinator.connectReady(
            device = device,
            channelKind = com.netumscan.scannersdk.ProtocolChannelKind.SCANNER_MASTER,
            selectedModelId = DeviceModelId.CS7501,
            applyDecoderModule = true,
        )

        val fakeSession = assertNotNull(result.fakeSession)
        assertEquals(SessionState.READY, fakeSession.state.value)
        assertEquals(null, result.session)
    }
}
