import Foundation
import ScannerSDK

struct ModuleCommandRequest {
    let family: ModuleFamily
    let kind: ModuleCommandKind
    let parameterID: UInt32
    let payload: Data
    let persist: Bool
}

@MainActor
final class ModuleCommandRunner {
    private let canExecuteModuleCommands: () -> Bool
    private let notReadyReason: () -> String
    private let onNotReady: (String, String) -> Void

    init(
        canExecuteModuleCommands: @escaping () -> Bool,
        notReadyReason: @escaping () -> String,
        onNotReady: @escaping (String, String) -> Void
    ) {
        self.canExecuteModuleCommands = canExecuteModuleCommands
        self.notReadyReason = notReadyReason
        self.onNotReady = onNotReady
    }

    func ensureReady(title: String, titleProvider: () -> String) -> Bool {
        guard canExecuteModuleCommands() else {
            onNotReady(title, notReadyReason())
            return false
        }
        return true
    }

    func execute(_ session: ScannerSession, request: ModuleCommandRequest) throws -> CommandResponse {
        try session.executeModuleCommand(
            family: request.family,
            kind: request.kind,
            parameterID: request.parameterID,
            payload: request.payload,
            persist: request.persist
        )
    }

    func isNtc06hSilentAckTimeout(_ error: Error) -> Bool {
        if let scannerError = error as? ScannerError {
            return scannerError.code == 5
        }
        let detail = String(describing: error)
        return detail.contains("code: 5") ||
            detail.contains("code=5") ||
            detail.localizedCaseInsensitiveContains("timeout")
    }
}
