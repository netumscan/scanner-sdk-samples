import XCTest
@testable import ScannerSDK
@testable import ScannerSDKiOSDemo

@MainActor
final class DemoP2P3Tests: XCTestCase {
    func testFakeDiscoveryReturnsReadySessionHandle() async throws {
        let coordinator = DiscoveryCoordinator(backend: FakeDemoDiscoveryBackend())

        let discovery = try coordinator.startDiscovery(selectedModelId: .cs7501)
        let device = try XCTUnwrap(discovery.devices.first)
        let connection = try await coordinator.connectReady(
            device,
            channelKind: .scannerMaster,
            selectedModelId: .cs7501,
            applyDecoderModule: true
        )

        XCTAssertTrue(coordinator.isFakeMode)
        XCTAssertEqual(device.transportType, .bleGatt)
        XCTAssertNil(connection.session)
        XCTAssertEqual(connection.fakeSession?.latestState, .ready)
    }

    func testModuleCommandRunnerReportsNotReadyAndClassifiesTimeout() {
        var warning: String?
        let runner = ModuleCommandRunner(
            canExecuteModuleCommands: { false },
            notReadyReason: { "module not ready" },
            onNotReady: { title, reason in warning = "\(title): \(reason)" }
        )

        XCTAssertFalse(runner.ensureReady(title: "Read module", titleProvider: { "Read module" }))
        XCTAssertTrue(warning?.contains("module not ready") == true)
        XCTAssertTrue(runner.isNtc06hSilentAckTimeout(ScannerError(code: 5, operation: "timeout")))
    }

    func testCompatibilityRecordIncludesDiagnosticsAndFakeMode() {
        let store = DemoDiagnosticsStore()
        store.updateSdkInitialized(true)
        store.updateSelectedModel(.cs7501)
        store.updateResolvedModel(.cs7501)
        store.updateSessionState(.ready)
        store.updateRecentFailure("none")
        store.updateFakeMode(true)

        let record = DemoCompatibilityRecord.format(state: store.snapshot())

        XCTAssertTrue(record.contains("platform=iOS"))
        XCTAssertTrue(record.contains("transport=BLE_GATT"))
        XCTAssertTrue(record.contains("selectedModel=cs7501"))
        XCTAssertTrue(record.contains("sessionState=ready"))
        XCTAssertTrue(record.contains("fakeMode=true"))
    }

    func testScenarioPresetsKeepExpectedOrder() {
        XCTAssertEqual(
            DemoScenarioPresets.all.map(\.kind),
            [.quickScan, .readInfo, .readBattery, .masterCommands, .moduleParameters, .dataRuleBuilder]
        )
    }
}
