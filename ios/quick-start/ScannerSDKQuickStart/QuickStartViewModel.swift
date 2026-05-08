import Foundation
import ScannerSDK

@MainActor
final class QuickStartViewModel: ObservableObject {
    @Published var devices: [DiscoveredDevice] = []
    @Published var scans: [String] = []
    @Published var status = "Idle"
    @Published var connectedDeviceName: String?

    private let sdk = ScannerSDK.shared
    private var discoveryTask: Task<Void, Never>?
    private var scanTask: Task<Void, Never>?
    private var session: ScannerSession?

    func startDiscovery() {
        discoveryTask?.cancel()
        devices.removeAll()
        status = "Discovering"

        discoveryTask = Task { [weak self] in
            guard let self else { return }
            for await device in sdk.discoveries {
                await MainActor.run {
                    if let index = devices.firstIndex(where: { $0.deviceId == device.deviceId }) {
                        devices[index] = device
                    } else {
                        devices.append(device)
                    }
                }
            }
        }

        do {
            try sdk.initialize()
            try sdk.startDiscovery(transports: [.bleGatt], selectedModelId: .unknown)
        } catch {
            status = error.localizedDescription
        }
    }

    func connect(_ device: DiscoveredDevice) {
        Task {
            do {
                try? sdk.stopDiscovery()
                status = "Connecting"
                let connected = try await sdk.connectReady(device)
                session = connected
                connectedDeviceName = device.name.isEmpty ? device.deviceId : device.name
                status = "Connected"
                observeScans(from: connected)
            } catch {
                status = error.localizedDescription
            }
        }
    }

    func disconnect() {
        Task {
            scanTask?.cancel()
            try? session?.disconnect()
            session = nil
            connectedDeviceName = nil
            status = "Disconnected"
        }
    }

    private func observeScans(from session: ScannerSession) {
        scanTask?.cancel()
        scanTask = Task { [weak self] in
            guard let self else { return }
            for await event in session.scanEvents {
                await MainActor.run {
                    scans.insert(event.text, at: 0)
                }
            }
        }
    }
}

