import XCTest
@testable import ScannerSDK
@testable import ScannerSDKiOSDemo

@MainActor
final class DemoDiagnosticsTests: XCTestCase {
    func testDiagnosticsSummaryFollowsSelectedLanguage() {
        let localization = DemoLocalization.shared
        let originalLanguage = localization.language
        defer { localization.language = originalLanguage }

        let store = DemoDiagnosticsStore()
        store.updateSdkInitialized(true)
        store.updateSelectedModel(.cs7501)
        store.updateResolvedModel(.cs7501)
        store.updateSessionState(.ready)
        store.updateRecentFailure("last failure")

        localization.language = .en
        XCTAssertTrue(store.summaryText().contains("SDK State: initialized=Yes"))
        XCTAssertTrue(store.summaryText().contains("Session State: ready"))

        localization.language = .zh
        XCTAssertTrue(store.summaryText().contains("SDK 状态: initialized=是"))
        XCTAssertTrue(store.summaryText().contains("最近失败: last failure"))
    }

    func testSessionCommandRunnerReportsBusyWithoutRunningOperation() {
        var executingValues: [Bool] = []
        var busyCount = 0
        var ranOperation = false
        let runner = SessionCommandRunner(
            setExecuting: { executingValues.append($0) },
            onBusy: { busyCount += 1 }
        )

        runner.execute(
            isExecuting: true,
            operation: {
                ranOperation = true
            },
            onFailure: { _ in }
        )

        XCTAssertEqual(busyCount, 1)
        XCTAssertFalse(ranOperation)
        XCTAssertTrue(executingValues.isEmpty)
    }
}
