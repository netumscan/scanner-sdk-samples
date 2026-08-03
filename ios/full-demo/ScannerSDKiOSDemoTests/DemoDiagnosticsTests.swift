import XCTest
@testable import ScannerSDK
@testable import ScannerSDKiOSDemo

@MainActor
final class DemoDiagnosticsTests: XCTestCase {
    func testCompatibilityRecordIncludesBuildProvenance() {
        let state = DemoDiagnosticsState(
            platform: DemoPlatformDiagnostics(
                demoVersion: "1.0.0",
                demoBuild: "12",
                sdkVersion: "1.0.0",
                sdkCommit: "4989fda4",
                iosVersion: "iOS 16.7.16",
                bluetoothAuthorization: "authorized"
            )
        )

        let record = DemoCompatibilityRecord.format(state: state)

        XCTAssertTrue(record.contains("demoVersion=1.0.0"))
        XCTAssertTrue(record.contains("demoBuild=12"))
        XCTAssertTrue(record.contains("sdkVersion=1.0.0"))
        XCTAssertTrue(record.contains("sdkCommit=4989fda4"))
    }

    func testDiagnosticsSummaryFollowsSelectedLanguage() {
        let localization = DemoLocalization.shared
        let originalLanguage = localization.language
        defer { localization.language = originalLanguage }

        let store = DemoDiagnosticsStore()
        store.updateSdkInitialized(true)
        store.updateSelectedModel("CS7501")
        store.updateResolvedModel("CS7501")
        store.updateSessionState(.ready)
        store.updateRecentFailure("last failure")

        localization.language = .en
        XCTAssertTrue(store.summaryText().contains("SDK State: initialized=Yes"))
        XCTAssertTrue(store.summaryText().contains("Session State: ready"))

        localization.language = .zh
        XCTAssertTrue(store.summaryText().contains("SDK 状态: initialized=是"))
        XCTAssertTrue(store.summaryText().contains("最近失败: last failure"))
    }

    func testMissingStringFallbackDoesNotExposeRawKey() {
        let localization = DemoLocalization.shared
        let originalLanguage = localization.language
        defer { localization.language = originalLanguage }

        localization.language = .en

        XCTAssertEqual(DemoStrings.tr("sdk_log_missing_translation"), "SDK Log Missing Translation")
    }

}
