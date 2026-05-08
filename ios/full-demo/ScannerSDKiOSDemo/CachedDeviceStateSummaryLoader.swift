import Foundation
import ScannerSDK

enum CachedDeviceStateSummaryLoader {
    static func load(
        infoProvider: () throws -> ScannerInfo
    ) throws -> DeviceStateSummary {
        let info = try infoProvider()
        return DeviceStateSummaryFormatter.format(info: info)
    }
}
