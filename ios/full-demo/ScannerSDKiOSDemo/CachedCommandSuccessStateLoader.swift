import Foundation

struct CachedCommandSuccessState {
    let infoSummarySource: DemoInfoSummarySource
    let deviceCharsetSummarySource: DemoDeviceConfigSummarySource
    let deviceTerminalSummarySource: DemoDeviceConfigSummarySource
    let lastActionResult: String

    var infoSummary: String { infoSummarySource.text }
    var deviceCharsetSummary: String { deviceCharsetSummarySource.text }
    var deviceTerminalSummary: String { deviceTerminalSummarySource.text }
}

enum CachedCommandSuccessStateLoader {
    static func load(
        actionResult: String,
        summaryProvider: () throws -> DeviceStateSummary
    ) throws -> CachedCommandSuccessState {
        let summary = try summaryProvider()
        return CachedCommandSuccessState(
            infoSummarySource: summary.infoSummarySource,
            deviceCharsetSummarySource: summary.deviceCharsetSummarySource,
            deviceTerminalSummarySource: summary.deviceTerminalSummarySource,
            lastActionResult: actionResult
        )
    }
}
