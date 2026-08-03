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

    fun visibleDevices(): List<DiscoveredDevice> {
        val values = devices.values.asSequence()
            .sortedWith(
                compareByDescending<DiscoveredDevice> { isLikelyScanner(it) }
                    .thenByDescending { it.rssi ?: Int.MIN_VALUE }
                    .thenBy { it.name.lowercase() }
            )
            .toList()

        val likely = values.filter(::isLikelyScanner)
        val fallback = values.filterNot(::isLikelyScanner)
        return (likely + fallback).take(12)
    }

    private fun mergeDevice(old: DiscoveredDevice?, new: DiscoveredDevice): DiscoveredDevice {
        if (old == null) return new
        return DiscoveredDevice(
            deviceId = new.deviceId,
            name = if (new.name.isNotBlank()) new.name else old.name,
            transportType = new.transportType,
            modelKey = new.modelKey.takeIf { it != "" } ?: old.modelKey,
            matchReason = new.matchReason ?: old.matchReason,
            rssi = maxOf(old.rssi ?: Int.MIN_VALUE, new.rssi ?: Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE },
        )
    }

    private fun isLikelyScanner(device: DiscoveredDevice): Boolean {
        val name = device.name.lowercase()
        return scannerKeywords.any { keyword -> keyword in name }
    }

    private companion object {
        private val scannerKeywords = listOf(
            "scanner",
            "scan",
            "barcode",
            "2d",
            "ble",
            "rf",
            "nt_",
        )
    }
}
