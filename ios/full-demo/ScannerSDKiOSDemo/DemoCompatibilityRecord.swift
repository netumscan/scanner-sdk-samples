import Foundation
import ScannerSDK

enum DemoCompatibilityRecord {
    @MainActor
    static func format(state: DemoDiagnosticsState) -> String {
        [
            "platform=iOS",
            "appVersion=\(state.platform.appVersion)",
            "iosVersion=\(state.platform.iosVersion)",
            "bluetoothAuthorization=\(state.platform.bluetoothAuthorization)",
            "transport=BLE_GATT",
            "selectedModel=\(state.selectedModelId)",
            "resolvedModel=\(state.resolvedModelId)",
            "sessionState=\(state.sessionState.map { String(describing: $0) } ?? "none")",
            "recentFailure=\(state.recentFailure ?? "none")",
            "fakeMode=\(state.fakeMode)",
        ].joined(separator: "\n")
    }
}
