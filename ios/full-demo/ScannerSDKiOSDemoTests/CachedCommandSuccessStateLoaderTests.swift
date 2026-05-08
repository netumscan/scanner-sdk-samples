import XCTest
@testable import ScannerSDKiOSDemo

final class CachedCommandSuccessStateLoaderTests: XCTestCase {
    func testLoadReadsSummaryOnceAndIncludesSuccessMessage() throws {
        var calls = 0

        let state = try CachedCommandSuccessStateLoader.load(
            actionResult: "主控指令 SET_DECODER_MODULE_3 执行完成",
            summaryProvider: {
                calls += 1
                return DeviceStateSummary(
                    infoSummarySource: .value("固件=FW1.0  硬件=HW2.0  系列码=customer"),
                    deviceCharsetSummarySource: .value("UTF8 (Txt) / 接收设备=Windows / 布局=EN / 接口=RF HID+USB COM"),
                    deviceTerminalSummarySource: .value("CRLF")
                )
            }
        )

        XCTAssertEqual(calls, 1)
        XCTAssertEqual(state.infoSummary, "固件=FW1.0  硬件=HW2.0  系列码=customer")
        XCTAssertEqual(state.deviceCharsetSummary, "UTF8 (Txt) / 接收设备=Windows / 布局=EN / 接口=RF HID+USB COM")
        XCTAssertEqual(state.deviceTerminalSummary, "CRLF")
        XCTAssertEqual(state.lastActionResult, "主控指令 SET_DECODER_MODULE_3 执行完成")
    }
}
