import Foundation
import ScannerSDK

enum DemoCompatibilityRecord {
    @MainActor
    static func format(state: DemoDiagnosticsState) -> String {
        [
            "platform=iOS",
            "demoVersion=\(state.platform.demoVersion)",
            "demoBuild=\(state.platform.demoBuild)",
            "sdkVersion=\(state.platform.sdkVersion)",
            "sdkCommit=\(state.platform.sdkCommit)",
            "iosVersion=\(state.platform.iosVersion)",
            "bluetoothAuthorization=\(state.platform.bluetoothAuthorization)",
            "transport=BLE_GATT",
            "selectedModel=\(state.selectedModelKey)",
            "resolvedModel=\(state.resolvedModelKey)",
            "sessionState=\(state.sessionState.map { String(describing: $0) } ?? "none")",
            "recentFailure=\(state.recentFailure ?? "none")",
            "fakeMode=\(state.fakeMode)",
        ].joined(separator: "\n")
    }
}
