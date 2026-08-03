import XCTest
@testable import ScannerSDKiOSDemo

final class AppLogSupportTests: XCTestCase {
    func testRedactsDeviceIdentifiers() {
        let message = redactDemoLogMessage(
            "deviceId=AA:BB:CC:DD:EE:FF serial=SN123456 " +
                "123E4567-E89B-12D3-A456-426614174000 " +
                "AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE"
        )

        XCTAssertFalse(message.contains("AA:BB:CC:DD:EE:FF"))
        XCTAssertFalse(message.contains("SN123456"))
        XCTAssertFalse(message.contains("123E4567-E89B-12D3-A456-426614174000"))
        XCTAssertFalse(message.contains("AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE"))
        XCTAssertTrue(message.contains("<redacted>"))
    }

    private var originalLanguage: DemoLanguage!

    override func setUp() {
        super.setUp()
        originalLanguage = DemoLocalization.shared.language
        DemoLocalization.shared.language = .zh
    }

    override func tearDown() {
        DemoLocalization.shared.language = originalLanguage
        originalLanguage = nil
        super.tearDown()
    }

    func testMatchesSourceTreatsEmptySelectionAsAllSources() {
        let event = ConsoleEvent(source: .scan, level: .info, message: "scan")

        XCTAssertTrue(matchesSource(event, filters: []))
        XCTAssertTrue(matchesSource(event, filters: [.scan, .session]))
        XCTAssertFalse(matchesSource(event, filters: [.ui, .command]))
    }

    func testMatchesLevelFollowsSelectedSeverity() {
        let debugEvent = ConsoleEvent(source: .ui, level: .debug, message: "debug")
        let errorEvent = ConsoleEvent(source: .session, level: .error, message: "error")

        XCTAssertTrue(matchesLevel(debugEvent, filter: .all))
        XCTAssertTrue(matchesLevel(debugEvent, filter: .debug))
        XCTAssertFalse(matchesLevel(debugEvent, filter: .info))
        XCTAssertTrue(matchesLevel(errorEvent, filter: .error))
        XCTAssertFalse(matchesLevel(errorEvent, filter: .warn))
    }

    func testSourceFilterSummaryUsesStableOrdering() {
        let summary = sourceFilterSummary([.command, .ui, .scan])

        XCTAssertEqual(summary, "UI, 扫码, 指令")
    }

    func testMakeSdkConsoleEventMapsCapabilityWarningToWarn() {
        let event = makeSdkConsoleEvent("session[42] capability-warning moduleFamily=se4750 but moduleCommands=false")

        XCTAssertEqual(event.source, .sdk)
        XCTAssertEqual(event.level, .warn)
        XCTAssertEqual(event.message, "session[42] 设置能力未就绪: family=se4750, moduleCommands=false")
    }

    func testMakeSdkConsoleEventSeparatesCoreLogs() {
        let event = makeSdkConsoleEvent("core: ScannerSession: binary ack received")

        XCTAssertEqual(event.source, .core)
        XCTAssertEqual(event.level, .debug)
        XCTAssertEqual(event.message, "Core 日志: ScannerSession: binary ack received")
    }

    func testMakeSdkConsoleEventSeparatesBleLogs() {
        let event = makeSdkConsoleEvent("BLE didDiscoverServices failed device=ABC error=timeout")

        XCTAssertEqual(event.source, .ble)
        XCTAssertEqual(event.level, .error)
        XCTAssertEqual(event.message, "BLE didDiscoverServices failed device=ABC error=timeout")
    }

    func testMakeSdkConsoleEventMapsProbeFailureToWarn() {
        let event = makeSdkConsoleEvent("session[42] defaultModuleProbe family=se4750 supported=false")

        XCTAssertEqual(event.source, .sdk)
        XCTAssertEqual(event.level, .warn)
        XCTAssertEqual(event.message, "session[42] 默认模组探测失败: family=se4750")
    }

    func testSdkDiagnosticEventsOnlyKeepsSdkWarningsAndErrors() {
        let diagnostics = sdkDiagnosticEvents(
            in: [
                ConsoleEvent(source: .sdk, level: .warn, message: "warn"),
                ConsoleEvent(source: .sdk, level: .error, message: "error"),
                ConsoleEvent(source: .sdk, level: .info, message: "info"),
                ConsoleEvent(source: .command, level: .error, message: "command error"),
            ]
        )

        XCTAssertEqual(diagnostics.map(\.message), ["warn", "error"])
    }

    func testSdkDiagnosticsSummaryLinesExposeCountsAndLatestMessage() {
        let summary = sdkDiagnosticsSummaryLines(
            for: [
                ConsoleEvent(source: .sdk, level: .warn, message: "first warn"),
                ConsoleEvent(source: .sdk, level: .info, message: "info"),
                ConsoleEvent(source: .sdk, level: .error, message: "last error"),
            ]
        )

        XCTAssertEqual(
            summary,
            [
                "SDK 告警数: 1",
                "SDK 错误数: 1",
                "最近一条 SDK 诊断: last error",
            ]
        )
    }
}
