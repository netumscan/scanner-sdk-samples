package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.TransportType
import com.netumscan.scannersdk.localizedLabel

enum class DemoTransportMode(
    val sdkTransport: TransportType,
) {
    BLE(TransportType.BLE_GATT),
    SPP(TransportType.SPP_CLASSIC);

    fun summary(): String {
        val label = sdkTransport.localizedLabel()
        return SdkLabelResolver.resolve(label.localizationKey, label.fallbackDisplayName)
    }

    companion object {
        fun fromTransportType(transportType: TransportType): DemoTransportMode? {
            return entries.firstOrNull { it.sdkTransport == transportType }
        }
    }
}
