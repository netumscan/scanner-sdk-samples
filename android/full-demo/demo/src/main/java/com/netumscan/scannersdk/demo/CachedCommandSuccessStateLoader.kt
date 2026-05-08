package com.netumscan.scannersdk.demo

internal data class CachedCommandSuccessState(
    val infoSummary: UiText,
    val deviceCharsetSummary: UiText,
    val deviceTerminalSummary: UiText,
    val lastActionResult: String,
)

internal object CachedCommandSuccessStateLoader {
    suspend fun load(
        actionResult: String,
        summaryProvider: suspend () -> DeviceStateSummary,
    ): CachedCommandSuccessState {
        val summary = summaryProvider()
        return CachedCommandSuccessState(
            infoSummary = summary.infoSummary,
            deviceCharsetSummary = summary.deviceCharsetSummary,
            deviceTerminalSummary = summary.deviceTerminalSummary,
            lastActionResult = actionResult,
        )
    }
}
