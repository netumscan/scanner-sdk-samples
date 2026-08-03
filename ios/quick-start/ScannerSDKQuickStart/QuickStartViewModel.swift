import Combine
import Foundation
import ScannerSDK

struct QuickScanRow: Equatable, Identifiable {
    let id: UInt64
    let barcodeType: Int32
    let text: String
}

@MainActor
final class QuickStartViewModel: ObservableObject {
    @Published private(set) var sdkVersion = ""
    @Published private(set) var status = "Initializing SDK"
    @Published private(set) var models: [QuickModel] = []
    @Published var selectedModelKey = ""
    @Published private(set) var isDiscovering = false
    @Published private(set) var devices: [QuickDevice] = []
    @Published private(set) var isConnecting = false
    @Published private(set) var connectedDeviceName: String?
    @Published private(set) var isReady = false
    @Published private(set) var canRetry = false
    @Published private(set) var isRunningCommand = false
    @Published private(set) var operationSupport = QuickOperationSupport.none
    @Published private(set) var deviceInfo = "Not loaded"
    @Published private(set) var battery = "Not loaded"
    @Published private(set) var lastAction = ""
    @Published private(set) var scans: [QuickScanRow] = []

    private let backend: QuickStartBackend
    private var session: QuickSession?
    private var scanSequence: UInt64 = 0
    private var discoveryTask: Task<Void, Never>?
    private var discoveryFailureTask: Task<Void, Never>?
    private var connectionTask: Task<Void, Never>?
    private var sessionStateTask: Task<Void, Never>?
    private var scanTask: Task<Void, Never>?
    private var sessionFailureTask: Task<Void, Never>?

    init(backend: QuickStartBackend) {
        self.backend = backend
        sdkVersion = backend.sdkVersion
        bindDiscoveryStreams()
        initialize()
    }

    deinit {
        discoveryTask?.cancel()
        discoveryFailureTask?.cancel()
        connectionTask?.cancel()
        sessionStateTask?.cancel()
        scanTask?.cancel()
        sessionFailureTask?.cancel()
    }

    func selectModel(_ modelKey: String) {
        guard !isDiscovering, session == nil, models.contains(where: { $0.modelKey == modelKey }) else {
            return
        }
        selectedModelKey = modelKey
    }

    func startDiscovery() {
        guard !selectedModelKey.isEmpty, session == nil else { return }
        do {
            try backend.startDiscovery(modelKey: selectedModelKey)
            devices = []
            status = "Discovering BLE scanners"
            isDiscovering = true
            canRetry = false
            lastAction = ""
        } catch {
            showFailure(error)
        }
    }

    func stopDiscovery() {
        do {
            try backend.stopDiscovery()
            status = "Discovery stopped"
            isDiscovering = false
        } catch {
            showFailure(error)
        }
    }

    func connect(_ device: QuickDevice) {
        guard !isConnecting, session == nil else { return }
        connectionTask?.cancel()
        status = "Connecting to \(device.displayName)"
        isConnecting = true
        isDiscovering = false
        canRetry = false

        connectionTask = Task { [weak self] in
            guard let self else { return }
            do {
                try? backend.stopDiscovery()
                let connected = try await backend.connect(device: device)
                guard !Task.isCancelled else {
                    try? await connected.disconnect()
                    return
                }
                session = connected
                bindSession(connected)
                let support = try await connected.operationSupport()
                let modelKey = try await connected.resolvedModelKey()
                guard session === connected else { return }
                operationSupport = support
                connectedDeviceName = device.displayName
                isConnecting = false
                isReady = true
                status = "Connected and ready"
                lastAction = "Resolved model: \(modelKey.isEmpty ? device.modelKey : modelKey)"
            } catch {
                isConnecting = false
                canRetry = true
                if let active = session {
                    try? await active.disconnect()
                }
                clearSession(status: "Connection setup failed", retry: true)
                showFailure(error)
            }
        }
    }

    func disconnect() {
        guard let active = session else { return }
        Task { [weak self] in
            try? await active.disconnect()
            self?.clearSession(status: "Disconnected", retry: true)
        }
    }

    func triggerScan() {
        runCommand(
            supported: operationSupport.triggerScan,
            unsupportedMessage: "Trigger Scan is not supported by this session"
        ) { session in
            try await session.triggerScan()
            return "Trigger Scan command sent"
        }
    }

    func refreshInfo() {
        runCommand(
            supported: operationSupport.refreshInfo,
            unsupportedMessage: "Refresh Device Info is not supported by this session"
        ) { [weak self] session in
            let summary = try await session.refreshInfo().displaySummary
            self?.deviceInfo = summary
            return "Device information refreshed"
        }
    }

    func readBattery() {
        runCommand(
            supported: operationSupport.battery,
            unsupportedMessage: "Read Battery is not supported by this session"
        ) { [weak self] session in
            let summary = try await session.readBattery().displaySummary
            self?.battery = summary
            return "Battery information refreshed"
        }
    }

    func shutdown() {
        let active = session
        clearSession(status: "Disconnected", retry: false)
        Task {
            try? await active?.disconnect()
            try? backend.shutdown()
        }
    }

    private func initialize() {
        Task { [weak self] in
            guard let self else { return }
            do {
                let loaded = try backend.initialize()
                let unique = Dictionary(
                    loaded
                        .filter { !$0.modelKey.isEmpty }
                        .map { ($0.modelKey.lowercased(), $0) },
                    uniquingKeysWith: { first, _ in first }
                )
                models = unique.values.sorted {
                    $0.modelName.localizedCaseInsensitiveCompare($1.modelName) == .orderedAscending
                }
                selectedModelKey = models.first {
                    $0.modelKey.caseInsensitiveCompare("CS7501") == .orderedSame
                }?.modelKey ?? models.first?.modelKey ?? ""
                sdkVersion = backend.sdkVersion
                status = models.isEmpty ? "No BLE models are available" : "Ready to discover"
                canRetry = !models.isEmpty
            } catch {
                showFailure(error)
            }
        }
    }

    private func bindDiscoveryStreams() {
        discoveryTask = Task { [weak self] in
            guard let self else { return }
            for await device in backend.discoveries {
                guard !Task.isCancelled else { break }
                devices.removeAll { $0.deviceId == device.deviceId }
                devices.append(device)
                devices.sort { ($0.rssi ?? Int.min) > ($1.rssi ?? Int.min) }
            }
        }
        discoveryFailureTask = Task { [weak self] in
            guard let self else { return }
            for await message in backend.discoveryFailures {
                guard !Task.isCancelled else { break }
                status = message
                isDiscovering = false
                canRetry = true
            }
        }
    }

    private func bindSession(_ active: QuickSession) {
        cancelSessionTasks()
        sessionStateTask = Task { [weak self, weak active] in
            guard let self, let active else { return }
            for await state in active.states {
                guard !Task.isCancelled, session === active else { break }
                switch state {
                case .ready:
                    status = "Connected and ready"
                    isReady = true
                case .disconnected:
                    clearSession(status: "Connection lost", retry: true)
                case .error:
                    clearSession(status: "Connection error", retry: true)
                default:
                    status = state.localizedLabel.fallbackDisplayName
                }
            }
        }
        scanTask = Task { [weak self, weak active] in
            guard let self, let active else { return }
            for await event in active.scans {
                guard !Task.isCancelled, session === active else { break }
                scanSequence += 1
                scans.insert(
                    QuickScanRow(id: scanSequence, barcodeType: event.barcodeType, text: event.text),
                    at: 0
                )
                if scans.count > 20 {
                    scans.removeLast(scans.count - 20)
                }
            }
        }
        sessionFailureTask = Task { [weak self, weak active] in
            guard let self, let active else { return }
            for await message in active.failures {
                guard !Task.isCancelled, session === active else { break }
                status = message
                lastAction = message
            }
        }
    }

    private func runCommand(
        supported: Bool,
        unsupportedMessage: String,
        operation: @escaping (QuickSession) async throws -> String
    ) {
        guard let active = session, isReady else {
            lastAction = "Connect a ready scanner first"
            return
        }
        guard supported else {
            lastAction = unsupportedMessage
            return
        }
        guard !isRunningCommand else { return }
        isRunningCommand = true
        Task { [weak self] in
            guard let self else { return }
            do {
                lastAction = try await operation(active)
            } catch {
                showFailure(error)
            }
            isRunningCommand = false
        }
    }

    private func clearSession(status: String, retry: Bool) {
        session = nil
        cancelSessionTasks()
        self.status = status
        isConnecting = false
        connectedDeviceName = nil
        isReady = false
        canRetry = retry
        isRunningCommand = false
        operationSupport = .none
    }

    private func cancelSessionTasks() {
        sessionStateTask?.cancel()
        scanTask?.cancel()
        sessionFailureTask?.cancel()
        sessionStateTask = nil
        scanTask = nil
        sessionFailureTask = nil
    }

    private func showFailure(_ error: Error) {
        let message = error.localizedDescription.isEmpty
            ? String(describing: type(of: error))
            : error.localizedDescription
        status = message
        lastAction = message
    }
}
