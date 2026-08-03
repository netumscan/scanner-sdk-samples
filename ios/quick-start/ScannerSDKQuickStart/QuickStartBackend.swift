import Foundation
import ScannerSDK

struct QuickModel: Equatable, Sendable {
    let modelKey: String
    let modelName: String
}

struct QuickDevice: Equatable, Sendable, Identifiable {
    let deviceId: String
    let name: String
    let modelKey: String
    let rssi: Int?

    var id: String { deviceId }
    var displayName: String { name.isEmpty ? "Unnamed scanner" : name }
}

struct QuickScanEvent: Equatable, Sendable {
    let barcodeType: Int32
    let text: String
}

struct QuickOperationSupport: Equatable, Sendable {
    let refreshInfo: Bool
    let battery: Bool
    let triggerScan: Bool

    static let none = QuickOperationSupport(
        refreshInfo: false,
        battery: false,
        triggerScan: false
    )
}

struct QuickDeviceInfo: Equatable, Sendable {
    let name: String
    let resolvedModel: String
    let firmware: String
    let hardware: String

    var displaySummary: String {
        [
            name.isEmpty ? nil : name,
            resolvedModel.isEmpty ? nil : resolvedModel,
            firmware.isEmpty ? nil : "FW \(firmware)",
            hardware.isEmpty ? nil : "HW \(hardware)",
        ]
        .compactMap { $0 }
        .joined(separator: " · ")
        .ifEmpty("No device information returned")
    }
}

struct QuickBattery: Equatable, Sendable {
    let percent: Int
    let voltage: String

    var displaySummary: String {
        voltage.isEmpty ? "\(percent)%" : "\(percent)% · \(voltage)"
    }
}

protocol QuickSession: AnyObject {
    var states: AsyncStream<SessionState> { get }
    var scans: AsyncStream<QuickScanEvent> { get }
    var failures: AsyncStream<String> { get }

    func operationSupport() async throws -> QuickOperationSupport
    func resolvedModelKey() async throws -> String
    func triggerScan() async throws
    func refreshInfo() async throws -> QuickDeviceInfo
    func readBattery() async throws -> QuickBattery
    func disconnect() async throws
}

protocol QuickStartBackend: AnyObject {
    var sdkVersion: String { get }
    var discoveries: AsyncStream<QuickDevice> { get }
    var discoveryFailures: AsyncStream<String> { get }

    func initialize() throws -> [QuickModel]
    func startDiscovery(modelKey: String) throws
    func stopDiscovery() throws
    func connect(device: QuickDevice) async throws -> QuickSession
    func shutdown() throws
}

final class ScannerQuickStartBackend: QuickStartBackend, @unchecked Sendable {
    private let sdk = ScannerSDK.shared
    private let lock = NSLock()
    private var discoveredDevices: [String: DiscoveredDevice] = [:]

    var sdkVersion: String { sdk.version }

    var discoveries: AsyncStream<QuickDevice> {
        AsyncStream { continuation in
            let task = Task { [weak self] in
                guard let self else { return }
                for await device in sdk.discoveries {
                    guard !Task.isCancelled else { break }
                    lock.withLock {
                        self.discoveredDevices[device.deviceId] = device
                    }
                    continuation.yield(
                        QuickDevice(
                            deviceId: device.deviceId,
                            name: device.name,
                            modelKey: device.modelKey,
                            rssi: device.rssi
                        )
                    )
                }
                continuation.finish()
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }

    var discoveryFailures: AsyncStream<String> {
        AsyncStream { continuation in
            let task = Task { [weak self] in
                guard let self else { return }
                for await failure in sdk.discoveryFailures {
                    guard !Task.isCancelled else { break }
                    continuation.yield("Discovery failed: \(failure.code)")
                }
                continuation.finish()
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }

    func initialize() throws -> [QuickModel] {
        try sdk.initialize()
        return try sdk.getSupportedDeviceModels(transport: .bleGatt).map {
            QuickModel(modelKey: $0.modelKey, modelName: $0.modelName)
        }
    }

    func startDiscovery(modelKey: String) throws {
        lock.withLock {
            discoveredDevices.removeAll()
        }
        try sdk.startDiscovery(transports: [.bleGatt], selectedModelKey: modelKey)
    }

    func stopDiscovery() throws {
        try sdk.stopDiscovery()
    }

    func connect(device: QuickDevice) async throws -> QuickSession {
        let source = lock.withLock {
            discoveredDevices[device.deviceId]
        }
        guard let source else {
            throw QuickStartError.message("The selected scanner is no longer available")
        }
        return ScannerQuickSession(session: try await sdk.connectReady(source))
    }

    func shutdown() throws {
        try sdk.shutdown()
    }
}

private final class ScannerQuickSession: QuickSession, @unchecked Sendable {
    private let session: ScannerSession

    init(session: ScannerSession) {
        self.session = session
    }

    var states: AsyncStream<SessionState> { session.state }

    var scans: AsyncStream<QuickScanEvent> {
        AsyncStream { continuation in
            let task = Task {
                for await scan in session.scanEvents {
                    guard !Task.isCancelled else { break }
                    continuation.yield(
                        QuickScanEvent(barcodeType: scan.barcodeType, text: scan.text)
                    )
                }
                continuation.finish()
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }

    var failures: AsyncStream<String> {
        AsyncStream { continuation in
            let task = Task {
                for await failure in session.failureEvents {
                    guard !Task.isCancelled else { break }
                    continuation.yield("Session error: \(failure.issue)")
                }
                continuation.finish()
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }

    func operationSupport() async throws -> QuickOperationSupport {
        let support = try session.getOperationSupport()
        return QuickOperationSupport(
            refreshInfo: support.supportsRefreshInfo,
            battery: support.supportsGetBatteryInfo,
            triggerScan: support.supportsTriggerScan
        )
    }

    func resolvedModelKey() async throws -> String {
        try session.getResolvedModelKey()
    }

    func triggerScan() async throws {
        try await session.triggerScan()
    }

    func refreshInfo() async throws -> QuickDeviceInfo {
        let info = try await session.refreshInfo()
        return QuickDeviceInfo(
            name: info.name,
            resolvedModel: try session.getResolvedModelKey(),
            firmware: info.firmwareVersion,
            hardware: info.hardwareVersion
        )
    }

    func readBattery() async throws -> QuickBattery {
        let battery = try await session.getBatteryInfo()
        return QuickBattery(percent: battery.percent, voltage: battery.voltageText)
    }

    func disconnect() async throws {
        try await session.disconnect()
    }
}

enum QuickStartError: LocalizedError {
    case message(String)

    var errorDescription: String? {
        switch self {
        case .message(let message): message
        }
    }
}

private extension String {
    func ifEmpty(_ fallback: String) -> String {
        isEmpty ? fallback : self
    }
}
