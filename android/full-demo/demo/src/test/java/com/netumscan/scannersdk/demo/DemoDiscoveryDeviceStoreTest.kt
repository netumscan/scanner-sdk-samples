package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.TransportType
import com.netumscan.scannersdk.model.DeviceModelId
import com.netumscan.scannersdk.model.DiscoveredDevice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DemoDiscoveryDeviceStoreTest {

    @Test
    fun clear_removes_devices_from_previous_discovery_round() {
        val store = DemoDiscoveryDeviceStore()
        store.upsert(
            DiscoveredDevice(
                deviceId = "AA:BB",
                name = "Scanner One",
                transportType = TransportType.BLE_GATT,
                modelId = DeviceModelId.CS7501,
                rssi = -42,
            )
        )

        assertEquals(listOf("AA:BB"), store.visibleDevices().map { it.deviceId })

        store.clear()

        assertTrue(store.visibleDevices().isEmpty())
    }

    @Test
    fun separateStores_keepBleAndSppDiscoveryIndependent() {
        val bleStore = DemoDiscoveryDeviceStore()
        val sppStore = DemoDiscoveryDeviceStore()
        bleStore.upsert(
            DiscoveredDevice(
                deviceId = "AA:BB",
                name = "BLE Scanner",
                transportType = TransportType.BLE_GATT,
                modelId = DeviceModelId.CS7501,
            )
        )
        sppStore.upsert(
            DiscoveredDevice(
                deviceId = "11:22",
                name = "SPP Scanner",
                transportType = TransportType.SPP_CLASSIC,
                modelId = DeviceModelId.CS7501,
            )
        )

        assertEquals(listOf("AA:BB"), bleStore.visibleDevices().map { it.deviceId })
        assertEquals(listOf("11:22"), sppStore.visibleDevices().map { it.deviceId })
    }

    @Test
    fun replace_setsDevicesForModeStore() {
        val store = DemoDiscoveryDeviceStore()

        val visible = store.replace(
            listOf(
                DiscoveredDevice(
                    deviceId = "11:22",
                    name = "SPP Scanner",
                    transportType = TransportType.SPP_CLASSIC,
                    modelId = DeviceModelId.CS7501,
                )
            )
        )

        assertEquals(listOf("11:22"), visible.map { it.deviceId })
    }

    @Test
    fun resolveConnectionModelId_prefers_customer_selected_model() {
        val resolved = resolveConnectionModelId(
            selectedModelId = DeviceModelId.CS7501,
            discoveredModelId = DeviceModelId.C740,
        )

        assertEquals(DeviceModelId.CS7501, resolved)
    }

    @Test
    fun resolveConnectionModelId_falls_back_to_discovered_model_when_selected_model_is_unknown() {
        val resolved = resolveConnectionModelId(
            selectedModelId = DeviceModelId.UNKNOWN,
            discoveredModelId = DeviceModelId.C740,
        )

        assertEquals(DeviceModelId.C740, resolved)
    }
}
