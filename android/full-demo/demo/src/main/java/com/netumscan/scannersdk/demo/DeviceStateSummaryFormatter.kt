package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.ScannerInfo

internal data class DeviceStateSummary(
    val infoSummary: UiText,
    val deviceCharsetSummary: UiText,
    val deviceTerminalSummary: UiText,
)

internal object DeviceStateSummaryFormatter {
    fun format(info: ScannerInfo): DeviceStateSummary {
        return DeviceStateSummary(
            infoSummary = joinedText(
                uiText(R.string.firmware),
                rawDisplayText("=${info.firmwareVersion.ifBlank { "-" }}  "),
                uiText(R.string.hardware),
                rawDisplayText("=${info.hardwareVersion.ifBlank { "-" }}  "),
                uiText(R.string.series_code),
                rawDisplayText("=${info.versionSeriesCode.ifBlank { "-" }}"),
            ),
            deviceCharsetSummary = uiText(R.string.device_config_not_auto_queried),
            deviceTerminalSummary = uiText(R.string.device_terminator_not_auto_queried),
        )
    }
}
