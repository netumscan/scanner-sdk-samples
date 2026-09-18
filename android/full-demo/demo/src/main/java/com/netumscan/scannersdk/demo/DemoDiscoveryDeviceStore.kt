package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.DiscoveredDevice

internal class DemoDiscoveryDeviceStore {
    private val devices = LinkedHashMap<String, DiscoveredDevice>()

    fun clear() {
        devices.clear()
    }

    fun upsert(device: DiscoveredDevice): List<DiscoveredDevice> {
        devices[device.deviceId] = mergeDevice(devices[device.deviceId], device)
        return visibleDevices()
    }

    fun replace(newDevices: List<DiscoveredDevice>): List<DiscoveredDevice> {
        devices.clear()
        newDevices.forEach { device ->
            devices[device.deviceId] = device
        }
        return visibleDevices()
    }

    fun visibleDevices(): List<DiscoveredDevice> = devices.values.sortedWith(
        compareByDescending<DiscoveredDevice> { isCandidate(it) }
            .thenByDescending { it.rssi ?: Int.MIN_VALUE }
            .thenBy { it.name.lowercase(java.util.Locale.ROOT) }
            .thenBy { it.deviceId }
    )

    private fun mergeDevice(old: DiscoveredDevice?, new: DiscoveredDevice): DiscoveredDevice {
        if (old == null) return new
        return new.copy(
            name = new.name.ifEmpty { old.name },
            modelKey = new.modelKey.ifEmpty { old.modelKey },
            matchReason = new.matchReason ?: old.matchReason,
            rssi = new.rssi ?: old.rssi,
            advertisementName = new.advertisementName.ifEmpty { old.advertisementName },
            serviceUuids = new.serviceUuids.ifEmpty { old.serviceUuids },
            manufacturerData = old.manufacturerData + new.manufacturerData,
            serviceData = old.serviceData + new.serviceData,
            connectable = new.connectable ?: old.connectable,
        )
    }

    private fun isCandidate(device: DiscoveredDevice): Boolean =
        listOf(device.name, device.advertisementName).any { name ->
            name.contains("scanner", ignoreCase = true) || name.contains("barcode", ignoreCase = true)
        }
}
