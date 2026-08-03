import ScannerSDK
import XCTest
@testable import ScannerSDKQuickStart

@MainActor
final class QuickStartViewModelTests: XCTestCase {
    func testInitializationSelectsCS7501AndSortsModels() async {
        let backend = FakeQuickStartBackend(
            models: [
                QuickModel(modelKey: "Z200", modelName: "Zulu"),
                QuickModel(modelKey: "CS7501", modelName: "CS7501"),
                QuickModel(modelKey: "A100", modelName: "Alpha"),
                QuickModel(modelKey: "cs7501", modelName: "Duplicate"),
            ]
        )

        let viewModel = QuickStartViewModel(backend: backend)
        await settle()

        XCTAssertEqual(viewModel.selectedModelKey, "CS7501")
        XCTAssertEqual(viewModel.models.map(\.modelKey), ["A100", "CS7501", "Z200"])
        XCTAssertEqual(viewModel.status, "Ready to discover")
    }

    func testDiscoveryDeduplicatesDevices() async {
        let backend = FakeQuickStartBackend()
        let viewModel = QuickStartViewModel(backend: backend)
        await settle()

        viewModel.startDiscovery()
        backend.emitDevice(QuickDevice(deviceId: "private-id", name: "Scanner", modelKey: "CS7501", rssi: -70))
        backend.emitDevice(QuickDevice(deviceId: "private-id", name: "Scanner", modelKey: "CS7501", rssi: -30))
        await settle()

        XCTAssertEqual(viewModel.devices.count, 1)
        XCTAssertEqual(viewModel.devices.first?.rssi, -30)
    }

    func testConnectionKeepsLatestTwentyScansAndClearsAfterDisconnect() async {
        let session = FakeQuickSession()
        let backend = FakeQuickStartBackend(session: session)
        let viewModel = QuickStartViewModel(backend: backend)
        await settle()

        viewModel.connect(QuickDevice(deviceId: "private-id", name: "Scanner", modelKey: "CS7501", rssi: -20))
        await settle()
        for index in 0..<21 {
            session.emitScan(QuickScanEvent(barcodeType: Int32(index), text: "scan-\(index)"))
        }
        await settle()

        XCTAssertTrue(viewModel.isReady)
        XCTAssertEqual(viewModel.scans.count, 20)
        XCTAssertEqual(viewModel.scans.first?.text, "scan-20")
        XCTAssertEqual(viewModel.scans.last?.text, "scan-1")

        session.emitState(.disconnected)
        await settle()
        XCTAssertFalse(viewModel.isReady)
        XCTAssertTrue(viewModel.canRetry)
        XCTAssertEqual(viewModel.status, "Connection lost")
    }

    func testQuickActionsPublishSafeSummaries() async {
        let session = FakeQuickSession()
        let viewModel = QuickStartViewModel(
            backend: FakeQuickStartBackend(session: session)
        )
        await settle()
        viewModel.connect(QuickDevice(deviceId: "private-id", name: "Scanner", modelKey: "CS7501", rssi: -20))
        await settle()

        viewModel.triggerScan()
        await waitUntil {
            session.triggerCalls == 1 && !viewModel.isRunningCommand
        }
        viewModel.refreshInfo()
        await settle()
        viewModel.readBattery()
        await settle()

        XCTAssertEqual(session.triggerCalls, 1)
        XCTAssertEqual(session.infoCalls, 1)
        XCTAssertEqual(session.batteryCalls, 1)
        XCTAssertEqual(viewModel.deviceInfo, "Scanner · CS7501 · FW 1.2.3 · HW A")
        XCTAssertEqual(viewModel.battery, "75% · 3.8V")
    }

    func testUnsupportedActionsDoNotCallSession() async {
        let session = FakeQuickSession(support: .none)
        let viewModel = QuickStartViewModel(
            backend: FakeQuickStartBackend(session: session)
        )
        await settle()
        viewModel.connect(QuickDevice(deviceId: "private-id", name: "Scanner", modelKey: "CS7501", rssi: -20))
        await settle()

        viewModel.triggerScan()
        viewModel.refreshInfo()
        viewModel.readBattery()
        await settle()

        XCTAssertEqual(session.triggerCalls, 0)
        XCTAssertEqual(session.infoCalls, 0)
        XCTAssertEqual(session.batteryCalls, 0)
        XCTAssertEqual(viewModel.lastAction, "Read Battery is not supported by this session")
    }

    func testDiscoveryFailureEnablesManualRetry() async {
        let backend = FakeQuickStartBackend()
        let viewModel = QuickStartViewModel(backend: backend)
        await settle()

        viewModel.startDiscovery()
        backend.emitDiscoveryFailure("Bluetooth permission is required")
        await settle()

        XCTAssertEqual(viewModel.status, "Bluetooth permission is required")
        XCTAssertTrue(viewModel.canRetry)
        XCTAssertFalse(viewModel.isDiscovering)
    }

    func testCommandFailureLeavesReadySessionUsable() async {
        let session = FakeQuickSession(failCommands: true)
        let viewModel = QuickStartViewModel(
            backend: FakeQuickStartBackend(session: session)
        )
        await settle()
        viewModel.connect(QuickDevice(deviceId: "private-id", name: "Scanner", modelKey: "CS7501", rssi: -20))
        await settle()

        viewModel.triggerScan()
        await waitUntil {
            session.triggerCalls == 1 && !viewModel.isRunningCommand
        }

        XCTAssertEqual(viewModel.lastAction, "Command failed")
        XCTAssertFalse(viewModel.isRunningCommand)
        XCTAssertTrue(viewModel.isReady)
    }

    private func settle() async {
        for _ in 0..<30 {
            await Task.yield()
        }
    }

    private func waitUntil(_ condition: @MainActor () -> Bool) async {
        for _ in 0..<1_000 {
            if condition() {
                return
            }
            await Task.yield()
        }
    }
}

private final class FakeQuickStartBackend: QuickStartBackend {
    let sdkVersion = "1.0.0"
    let discoveries: AsyncStream<QuickDevice>
    let discoveryFailures: AsyncStream<String>

    private let models: [QuickModel]
    private let session: QuickSession
    private let deviceContinuation: AsyncStream<QuickDevice>.Continuation
    private let failureContinuation: AsyncStream<String>.Continuation

    init(
        models: [QuickModel] = [
            QuickModel(modelKey: "CS7501", modelName: "CS7501"),
            QuickModel(modelKey: "C750", modelName: "C750"),
        ],
        session: QuickSession = FakeQuickSession()
    ) {
        self.models = models
        self.session = session
        let deviceStream = AsyncStream<QuickDevice>.makeStream()
        discoveries = deviceStream.stream
        deviceContinuation = deviceStream.continuation
        let failureStream = AsyncStream<String>.makeStream()
        discoveryFailures = failureStream.stream
        failureContinuation = failureStream.continuation
    }

    func initialize() throws -> [QuickModel] { models }
    func startDiscovery(modelKey: String) throws {}
    func stopDiscovery() throws {}
    func connect(device: QuickDevice) async throws -> QuickSession { session }
    func shutdown() throws {}

    func emitDevice(_ device: QuickDevice) {
        deviceContinuation.yield(device)
    }

    func emitDiscoveryFailure(_ message: String) {
        failureContinuation.yield(message)
    }
}

private final class FakeQuickSession: QuickSession {
    let states: AsyncStream<SessionState>
    let scans: AsyncStream<QuickScanEvent>
    let failures: AsyncStream<String>

    private let support: QuickOperationSupport
    private let stateContinuation: AsyncStream<SessionState>.Continuation
    private let scanContinuation: AsyncStream<QuickScanEvent>.Continuation
    private let failureContinuation: AsyncStream<String>.Continuation
    private let failCommands: Bool

    var triggerCalls = 0
    var infoCalls = 0
    var batteryCalls = 0

    init(
        support: QuickOperationSupport = QuickOperationSupport(
            refreshInfo: true,
            battery: true,
            triggerScan: true
        ),
        failCommands: Bool = false
    ) {
        self.support = support
        self.failCommands = failCommands
        let stateStream = AsyncStream<SessionState>.makeStream()
        states = stateStream.stream
        stateContinuation = stateStream.continuation
        let scanStream = AsyncStream<QuickScanEvent>.makeStream()
        scans = scanStream.stream
        scanContinuation = scanStream.continuation
        let failureStream = AsyncStream<String>.makeStream()
        failures = failureStream.stream
        failureContinuation = failureStream.continuation
    }

    func operationSupport() async throws -> QuickOperationSupport { support }
    func resolvedModelKey() async throws -> String { "CS7501" }

    func triggerScan() async throws {
        triggerCalls += 1
        if failCommands {
            throw FakeQuickStartError.commandFailed
        }
    }

    func refreshInfo() async throws -> QuickDeviceInfo {
        infoCalls += 1
        return QuickDeviceInfo(
            name: "Scanner",
            resolvedModel: "CS7501",
            firmware: "1.2.3",
            hardware: "A"
        )
    }

    func readBattery() async throws -> QuickBattery {
        batteryCalls += 1
        return QuickBattery(percent: 75, voltage: "3.8V")
    }

    func disconnect() async throws {
        stateContinuation.yield(.disconnected)
    }

    func emitState(_ state: SessionState) {
        stateContinuation.yield(state)
    }

    func emitScan(_ scan: QuickScanEvent) {
        scanContinuation.yield(scan)
    }
}

private enum FakeQuickStartError: LocalizedError {
    case commandFailed

    var errorDescription: String? {
        "Command failed"
    }
}
