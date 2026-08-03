import Foundation
import ScannerSDK

@MainActor
final class DiscoveryCoordinator {
    private let backend: DemoDiscoveryBackend

    init() {
        self.backend = RealDemoDiscoveryBackend()
    }

    init(backend: DemoDiscoveryBackend) {
        self.backend = backend
    }

    var isFakeMode: Bool { backend.isFake }
    var discoveries: AsyncStream<DiscoveredDevice> { backend.discoveries }
    var discoveryFailures: AsyncStream<DiscoveryFailure> { backend.discoveryFailures }
    var debugEvents: AsyncStream<String> { backend.debugEvents }

    func initialize() throws {
        try backend.initialize()
    }

    func startDiscovery(selectedModelKey: String) throws -> DemoDiscoveryStartResult {
        try backend.startDiscovery(selectedModelKey: selectedModelKey)
    }

    func stopDiscovery() throws {
        try backend.stopDiscovery()
    }

    func connectReady(
        _ device: DiscoveredDevice,
        selectedModelKey: String,
        applyModelConfig: Bool
    ) async throws -> DemoConnectResult {
        try await backend.connectReady(
            device,
            selectedModelKey: selectedModelKey,
            applyModelConfig: applyModelConfig
        )
    }
}
