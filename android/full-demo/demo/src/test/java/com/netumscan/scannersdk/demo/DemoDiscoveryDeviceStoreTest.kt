package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.TransportType
import com.netumscan.scannersdk.model.DiscoveredDevice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DemoDiscoveryDeviceStoreTest {

    @Test
    fun sharedAdvertisementSamplesHaveIdenticalCandidateOrder() {
        val fixture = generateSequence(java.io.File(System.getProperty("user.dir"))) { it.parentFile }
            .map { java.io.File(it, "tests/fixtures/ble-discovery.tsv") }.first { it.isFile }
        val rows = fixture.readLines().filter { it.isNotEmpty() && !it.startsWith("#") }.map { it.split('\t') }
        val devices = rows.map { row ->
            DiscoveredDevice(row[0], row[1].takeUnless { it == "-" }.orEmpty(), TransportType.BLE_GATT,
                rssi = row[2].toIntOrNull(), advertisementName = row[3].takeUnless { it == "-" }.orEmpty(),
                connectable = when (row[9]) { "0" -> false; "1" -> true; else -> null })
        }
        val store = DemoDiscoveryDeviceStore()
        assertEquals(rows.sortedBy { it[10].toInt() }.map { it[0] }, store.replace(devices).map { it.deviceId })
        assertEquals(18, store.visibleDevices().size)
        val unnamed = store.visibleDevices().first { it.deviceId == "anonymous" }
        assertEquals("", unnamed.name)
        assertEquals(false, store.visibleDevices().first { it.deviceId == "not-connectable" }.connectable)
        store.upsert(unnamed.copy(name = "Name completed", rssi = -99))
        val updated = store.visibleDevices().first { it.deviceId == "anonymous" }
        assertEquals(-99, updated.rssi)
        assertEquals("Name completed", updated.name)
        assertEquals(18, store.visibleDevices().size)
    }

    @Test
    fun scannerNames_takePriorityWithoutDisplayLimit() {
        val store = DemoDiscoveryDeviceStore()
        store.replace((1..12).map { index ->
            DiscoveredDevice("ble-$index", "BLE $index", TransportType.BLE_GATT, rssi = -20)
        })
        store.upsert(DiscoveredDevice("rw185", "RW185 SCANNERLE", TransportType.BLE_GATT, rssi = -90))
        store.upsert(DiscoveredDevice("mixed", "Pocket ScAnNeR", TransportType.BLE_GATT, rssi = -60))
        val visible = store.upsert(
            DiscoveredDevice("lower", "scanner", TransportType.BLE_GATT, rssi = null)
        )

        assertEquals(listOf("mixed", "rw185", "lower"), visible.take(3).map { it.deviceId })
        assertEquals(15, visible.size)
    }

    @Test
    fun clear_removes_devices_from_previous_discovery_round() {
        val store = DemoDiscoveryDeviceStore()
        store.upsert(
            DiscoveredDevice(
                deviceId = "AA:BB",
                name = "Scanner One",
                transportType = TransportType.BLE_GATT,
                modelKey = "CS7501",
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
                modelKey = "CS7501",
            )
        )
        sppStore.upsert(
            DiscoveredDevice(
                deviceId = "11:22",
                name = "SPP Scanner",
                transportType = TransportType.SPP_CLASSIC,
                modelKey = "CS7501",
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
                    modelKey = "CS7501",
                )
            )
        )

        assertEquals(listOf("11:22"), visible.map { it.deviceId })
    }

    @Test
    fun resolveConnectionModelKey_prefers_customer_selected_model() {
        val resolved = resolveConnectionModelKey(
            selectedModelKey = "CS7501",
            discoveredModelKey = "C740",
        )

        assertEquals("CS7501", resolved)
    }

    @Test
    fun resolveConnectionModelKey_falls_back_to_discovered_model_when_selected_model_is_unknown() {
        val resolved = resolveConnectionModelKey(
            selectedModelKey = "",
            discoveredModelKey = "C740",
        )

        assertEquals("C740", resolved)
    }
}
