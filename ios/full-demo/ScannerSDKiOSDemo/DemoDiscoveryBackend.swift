import Foundation
import ScannerSDK

struct DemoDiscoveryStartResult {
    var devices: [DiscoveredDevice] = []
}

struct DemoConnectResult {
    var session: ScannerSession?
    var fakeSession: DemoSessionHandle?
}

@MainActor
protocol DemoDiscoveryBackend {
    var isFake: Bool { get }
    var discoveries: AsyncStream<DiscoveredDevice> { get }
    var discoveryFailures: AsyncStream<DiscoveryFailure> { get }
    var debugEvents: AsyncStream<String> { get }

    func initialize() throws
    func startDiscovery(selectedModelKey: String) throws -> DemoDiscoveryStartResult
    func stopDiscovery() throws
    func connectReady(
        _ device: DiscoveredDevice,
        selectedModelKey: String,
        applyModelConfig: Bool
    ) async throws -> DemoConnectResult
}

struct RealDemoDiscoveryBackend: DemoDiscoveryBackend {
    private let sdk = ScannerSDK.shared

    var isFake: Bool { false }
    var discoveries: AsyncStream<DiscoveredDevice> { sdk.discoveries }
    var discoveryFailures: AsyncStream<DiscoveryFailure> { sdk.discoveryFailures }
    var debugEvents: AsyncStream<String> { sdk.debugEvents }

    func initialize() throws {
        try sdk.initialize()
    }

    func startDiscovery(selectedModelKey: String) throws -> DemoDiscoveryStartResult {
        try sdk.startDiscovery(transports: [.bleGatt], selectedModelKey: selectedModelKey)
        return DemoDiscoveryStartResult()
    }

    func stopDiscovery() throws {
        try sdk.stopDiscovery()
    }

    func connectReady(
        _ device: DiscoveredDevice,
        selectedModelKey: String,
        applyModelConfig: Bool
    ) async throws -> DemoConnectResult {
        let session = try await sdk.connectReady(
            deviceId: device.deviceId,
            transport: device.transportType,
            selectedModelKey: selectedModelKey
        )
        return DemoConnectResult(session: session)
    }
}

struct FakeDemoDiscoveryBackend: DemoDiscoveryBackend {
    var isFake: Bool { true }
    var discoveries: AsyncStream<DiscoveredDevice> { AsyncStream { continuation in continuation.finish() } }
    var discoveryFailures: AsyncStream<DiscoveryFailure> { AsyncStream { continuation in continuation.finish() } }
    var debugEvents: AsyncStream<String> {
        AsyncStream { continuation in
            continuation.yield("fake debug stream ready")
            continuation.finish()
        }
    }

    func initialize() throws {}

    func startDiscovery(selectedModelKey: String) throws -> DemoDiscoveryStartResult {
        let model = selectedModelKey == "" ? "CS7501" : selectedModelKey
        return DemoDiscoveryStartResult(
            devices: [
                DiscoveredDevice(
                    deviceId: "FAKE-BLE-\(model)-001",
                    name: "Fake BLE Scanner",
                    transportType: .bleGatt,
                    modelKey: model,
                    matchReason: "fake-demo",
                    rssi: -48
                )
            ]
        )
    }

    func stopDiscovery() throws {}

    func connectReady(
        _ device: DiscoveredDevice,
        selectedModelKey: String,
        applyModelConfig: Bool
    ) async throws -> DemoConnectResult {
        DemoConnectResult(
            session: nil,
            fakeSession: FakeDemoSessionHandle(
                deviceID: device.deviceId,
                transportType: device.transportType,
                selectedModelKey: selectedModelKey
            )
        )
    }
}
