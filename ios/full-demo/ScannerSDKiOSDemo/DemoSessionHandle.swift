import Foundation
import ScannerSDK

@MainActor
protocol DemoSessionHandle: AnyObject {
    var isFake: Bool { get }
    var deviceID: String { get }
    var transportType: TransportType { get }
    var latestState: SessionState { get }
    var realSession: ScannerSession? { get }
    func disconnect() throws
}

@MainActor
final class RealDemoSessionHandle: DemoSessionHandle {
    private let session: ScannerSession

    init(_ session: ScannerSession) {
        self.session = session
    }

    var isFake: Bool { false }
    var deviceID: String { session.deviceId }
    var transportType: TransportType { session.transportType }
    var latestState: SessionState { session.latestState }
    var realSession: ScannerSession? { session }

    func disconnect() throws {
        try session.disconnect()
    }
}

@MainActor
final class FakeDemoSessionHandle: DemoSessionHandle {
    let deviceID: String
    let transportType: TransportType
    let selectedModelId: DeviceModelId
    private(set) var latestState: SessionState = .ready

    init(deviceID: String, transportType: TransportType, selectedModelId: DeviceModelId) {
        self.deviceID = deviceID
        self.transportType = transportType
        self.selectedModelId = selectedModelId
    }

    var isFake: Bool { true }
    var realSession: ScannerSession? { nil }

    func disconnect() throws {
        latestState = .disconnected
    }
}
