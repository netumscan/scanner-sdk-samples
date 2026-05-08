import Foundation
import ScannerSDK

@MainActor
final class SessionCommandRunner {
    private let setExecuting: (Bool) -> Void
    private let onBusy: () -> Void

    init(
        setExecuting: @escaping (Bool) -> Void,
        onBusy: @escaping () -> Void
    ) {
        self.setExecuting = setExecuting
        self.onBusy = onBusy
    }

    func requireReadySession(
        _ session: ScannerSession?,
        onMissing: () -> Void,
        onNotReady: (SessionState) -> Void
    ) -> ScannerSession? {
        guard let session else {
            onMissing()
            return nil
        }
        guard session.latestState == .ready else {
            onNotReady(session.latestState)
            return nil
        }
        return session
    }

    func execute(
        isExecuting: Bool,
        operation: @escaping () throws -> Void,
        onFailure: @escaping (Error) -> Void
    ) {
        guard !isExecuting else {
            onBusy()
            return
        }
        setExecuting(true)
        Task { [weak self] in
            guard let self else { return }
            defer { self.setExecuting(false) }
            do {
                try operation()
            } catch {
                onFailure(error)
            }
        }
    }

    func ensureOperationSupport(
        _ session: ScannerSession,
        operation: DemoSessionOperation,
        unsupportedReason: (DemoSessionOperation) -> String,
        onUnsupported: (String) -> Void
    ) throws -> Bool {
        let support = try session.getOperationSupportSummary()
        guard supportsSessionOperation(operation, support: support) else {
            onUnsupported(unsupportedReason(operation))
            return false
        }
        return true
    }

    func ensureMasterCommandSupport(
        _ session: ScannerSession,
        command: MasterCommand,
        unsupportedReason: () -> String,
        onUnsupported: (String) -> Void
    ) throws -> Bool {
        guard try session.canExecuteMasterCommand(command) else {
            onUnsupported(unsupportedReason())
            return false
        }
        return true
    }
}
