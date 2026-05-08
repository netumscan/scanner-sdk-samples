import CoreBluetooth
import Foundation
import ScannerSDK
import UIKit

struct DemoPlatformDiagnostics: Equatable {
    var appVersion = "-"
    var iosVersion = "-"
    var bluetoothAuthorization = "-"

    static func current() -> DemoPlatformDiagnostics {
        let info = Bundle.main.infoDictionary
        let version = info?["CFBundleShortVersionString"] as? String
        let build = info?["CFBundleVersion"] as? String
        let displayVersion = [version, build.map { "(\($0))" }]
            .compactMap { $0 }
            .joined(separator: " ")
            .ifBlank("-")
        return DemoPlatformDiagnostics(
            appVersion: displayVersion,
            iosVersion: "\(UIDevice.current.systemName) \(UIDevice.current.systemVersion)",
            bluetoothAuthorization: bluetoothAuthorizationSummary()
        )
    }

    private static func bluetoothAuthorizationSummary() -> String {
        switch CBManager.authorization {
        case .allowedAlways:
            return DemoStrings.tr("diagnostics_authorized")
        case .denied:
            return DemoStrings.tr("diagnostics_denied")
        case .restricted:
            return DemoStrings.tr("diagnostics_restricted")
        case .notDetermined:
            return DemoStrings.tr("diagnostics_not_determined")
        @unknown default:
            return DemoStrings.tr("unknown_device_name")
        }
    }
}

struct DemoDiagnosticsState: Equatable {
    var platform = DemoPlatformDiagnostics.current()
    var sdkInitialized = false
    var selectedModelId: DeviceModelId = .unknown
    var resolvedModelId: DeviceModelId = .unknown
    var sessionState: SessionState?
    var recentFailure: String?
    var fakeMode = false
}

@MainActor
final class DemoDiagnosticsStore {
    private var state = DemoDiagnosticsState()

    func refreshPlatform() {
        state.platform = .current()
    }

    func updateSdkInitialized(_ initialized: Bool) {
        state.sdkInitialized = initialized
    }

    func updateSelectedModel(_ modelId: DeviceModelId) {
        state.selectedModelId = modelId
    }

    func updateResolvedModel(_ modelId: DeviceModelId) {
        state.resolvedModelId = modelId
    }

    func updateSessionState(_ sessionState: SessionState?) {
        state.sessionState = sessionState
    }

    func updateRecentFailure(_ detail: String?) {
        state.recentFailure = detail
    }

    func updateFakeMode(_ enabled: Bool) {
        state.fakeMode = enabled
    }

    func snapshot() -> DemoDiagnosticsState {
        state
    }

    func summaryText() -> String {
        [
            "\(DemoStrings.tr("diagnostics_demo_platform")): \(state.platform.appVersion) / \(state.platform.iosVersion)",
            "\(DemoStrings.tr("diagnostics_permissions")): bluetooth=\(state.platform.bluetoothAuthorization)",
            "\(DemoStrings.tr("diagnostics_sdk_state")): initialized=\(flag(state.sdkInitialized)) transport=\(DemoStrings.tr("ios_ble_only"))",
            "\(DemoStrings.tr("diagnostics_model_state")): selected=\(modelLabel(state.selectedModelId)) resolved=\(modelLabel(state.resolvedModelId))",
            "\(DemoStrings.tr("diagnostics_session_state")): \(state.sessionState.map { String(describing: $0) } ?? DemoStrings.tr("none"))",
            "\(DemoStrings.tr("diagnostics_recent_failure")): \(state.recentFailure ?? DemoStrings.tr("none"))",
            "\(DemoStrings.tr("diagnostics_fake_mode")): \(flag(state.fakeMode))",
        ].joined(separator: "\n")
    }

    private func flag(_ value: Bool) -> String {
        DemoStrings.tr(value ? "diagnostics_yes" : "diagnostics_no")
    }

    private func modelLabel(_ modelId: DeviceModelId) -> String {
        modelId == .unknown ? DemoStrings.tr("none") : displayModelLabel(modelId)
    }
}
