import CoreBluetooth
import Foundation
import ScannerSDK
import UIKit

struct DemoPlatformDiagnostics: Equatable {
    var demoVersion = "-"
    var demoBuild = "-"
    var sdkVersion = "-"
    var sdkCommit = "-"
    var iosVersion = "-"
    var bluetoothAuthorization = "-"

    static func current(sdkVersion: String = "-") -> DemoPlatformDiagnostics {
        let info = Bundle.main.infoDictionary
        let version = info?["CFBundleShortVersionString"] as? String
        let build = info?["CFBundleVersion"] as? String
        return DemoPlatformDiagnostics(
            demoVersion: version?.ifBlank("-") ?? "-",
            demoBuild: build?.ifBlank("-") ?? "-",
            sdkVersion: sdkVersion.ifBlank("-"),
            sdkCommit: (info?["NSDKSdkCommit"] as? String)?.ifBlank("local-dev") ?? "local-dev",
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
    var selectedModelKey: String = ""
    var resolvedModelKey: String = ""
    var sessionState: SessionState?
    var recentSessionInitStage: String?
    var recentFailure: String?
    var fakeMode = false
}

@MainActor
final class DemoDiagnosticsStore {
    private var state = DemoDiagnosticsState()

    func refreshPlatform(sdkVersion: String = "-") {
        state.platform = .current(sdkVersion: sdkVersion)
    }

    func updateSdkInitialized(_ initialized: Bool) {
        state.sdkInitialized = initialized
    }

    func updateSelectedModel(_ modelKey: String) {
        state.selectedModelKey = modelKey
    }

    func updateResolvedModel(_ modelKey: String) {
        state.resolvedModelKey = modelKey
    }

    func updateSessionState(_ sessionState: SessionState?) {
        state.sessionState = sessionState
    }

    func updateRecentSessionInitStage(_ detail: String?) {
        state.recentSessionInitStage = detail
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
            "\(DemoStrings.tr("diagnostics_demo_platform")): \(state.platform.demoVersion) (\(state.platform.demoBuild)) / \(state.platform.iosVersion)",
            "SDK: \(state.platform.sdkVersion) / \(state.platform.sdkCommit)",
            "\(DemoStrings.tr("diagnostics_permissions")): bluetooth=\(state.platform.bluetoothAuthorization)",
            "\(DemoStrings.tr("diagnostics_sdk_state")): initialized=\(flag(state.sdkInitialized)) transport=\(DemoStrings.tr("ios_ble_only"))",
            "\(DemoStrings.tr("diagnostics_model_state")): selected=\(modelLabel(state.selectedModelKey)) resolved=\(modelLabel(state.resolvedModelKey))",
            "\(DemoStrings.tr("diagnostics_session_state")): \(state.sessionState.map { String(describing: $0) } ?? DemoStrings.tr("none"))",
            "\(DemoStrings.tr("diagnostics_session_init_stage")): \(state.recentSessionInitStage ?? DemoStrings.tr("none"))",
            "\(DemoStrings.tr("diagnostics_recent_failure")): \(state.recentFailure ?? DemoStrings.tr("none"))",
            "\(DemoStrings.tr("diagnostics_fake_mode")): \(flag(state.fakeMode))",
        ].joined(separator: "\n")
    }

    private func flag(_ value: Bool) -> String {
        DemoStrings.tr(value ? "diagnostics_yes" : "diagnostics_no")
    }

    private func modelLabel(_ modelKey: String) -> String {
        modelKey == "" ? DemoStrings.tr("none") : displayModelLabel(modelKey)
    }
}
