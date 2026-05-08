package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.ScannerInfo

internal object CachedDeviceStateSummaryLoader {
    suspend fun load(
        infoProvider: suspend () -> ScannerInfo,
    ): DeviceStateSummary {
        val info = infoProvider()
        return DeviceStateSummaryFormatter.format(info)
    }
}
