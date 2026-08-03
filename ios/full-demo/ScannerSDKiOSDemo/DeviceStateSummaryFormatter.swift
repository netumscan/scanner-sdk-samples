import Foundation
import ScannerSDK

struct DeviceStateSummary {
    let infoSummarySource: DemoInfoSummarySource
    let deviceCharsetSummarySource: DemoDeviceConfigSummarySource
    let deviceTerminalSummarySource: DemoDeviceConfigSummarySource

    var infoSummary: String { infoSummarySource.text }
    var deviceCharsetSummary: String { deviceCharsetSummarySource.text }
    var deviceTerminalSummary: String { deviceTerminalSummarySource.text }
}

enum DeviceStateSummaryFormatter {
    static func format(info: ScannerInfo) -> DeviceStateSummary {
        return DeviceStateSummary(
            infoSummarySource: .info(
                firmwareVersion: info.firmwareVersion,
                hardwareVersion: info.hardwareVersion,
                versionSeriesCode: info.versionSeriesCode,
                bluetoothName: info.bluetoothName,
                bluetoothFirmwareVersion: info.bluetoothFirmwareVersion
            ),
            deviceCharsetSummarySource: .notRead,
            deviceTerminalSummarySource: .notRead
        )
    }
}
