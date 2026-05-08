import Foundation
import ScannerSDK

@MainActor
extension AppViewModel {
    func observe(_ session: ScannerSession) {
        stateTask?.cancel()
        stateTask = nil
        scanTask?.cancel()
        scanTask = nil
        let observedHandle = session.handle

        stateTask = Task { [weak self] in
            guard let self else { return }
            for await state in session.state {
                if Task.isCancelled { return }
                self.sessionStateText = String(describing: state)
                self.diagnosticsStore.updateSessionState(state)
                self.applyStatusSummary(.session(state))
                self.appendEvent(.session, .info, "\(DemoStrings.tr("session_state")): \(state)")
                if state == .ready || state == .disconnected {
                    self.isConnecting = false
                }
                if state == .ready, !self.autoInfoRequested {
                    self.handleReadySession(session)
                }
                if state == .error || state == .disconnected {
                    if self.session?.handle == observedHandle {
                        self.handleSessionClosed(
                            status: .session(state),
                            lastActionProvider: {
                                state == .error
                                    ? DemoStrings.tr("device_connection_interrupted")
                                    : DemoStrings.tr("device_connection_closed")
                            },
                            cancelStateTask: false,
                            clearError: false
                        )
                    }
                    return
                }
            }
        }

        scanTask = Task { [weak self] in
            guard let self else { return }
            for await scan in session.scanEvents {
                if Task.isCancelled { return }
                if self.session?.handle != observedHandle {
                    return
                }
                let text = escapeControlText(scan.text)
                let raw = hexSummary(scan.rawBytes)
                self.appendEvent(.scan, .info, "\(DemoStrings.tr("log_source_scan")): \(text) raw=\(raw)")
            }
        }
    }

    func handleSessionClosed(
        status: DemoStatusSummarySource,
        lastActionProvider: @escaping () -> String,
        cancelStateTask: Bool = true,
        clearError: Bool = true
    ) {
        if cancelStateTask {
            stateTask?.cancel()
            stateTask = nil
        }
        scanTask?.cancel()
        scanTask = nil
        session = nil
        sessionHandle = nil
        connectedModelId = nil
        diagnosticsStore.updateSessionState(nil)
        diagnosticsStore.updateResolvedModel(.unknown)
        isConnecting = false
        autoInfoRequested = false
        sessionStateText = "disconnected"
        applyStatusSummary(status)
        if clearError {
            setLocalizedLastActionResult(lastActionProvider)
            errorText = nil
        } else {
            lastActionResultProvider = lastActionProvider
            applyLocalizedFeedback {
                lastActionResult = lastActionProvider()
            }
        }
        resetDisconnectedDeviceState()
    }

    func handleReadySession(_ session: ScannerSession) {
        guard !autoInfoRequested else { return }
        do {
            try session.setScanTerminator(localTerminator)
        } catch {
            appendEvent(.ui, .warn, "\(DemoStrings.tr("apply_scan_terminator_on_ready_failed")): \(DemoErrorFormatter.detail(error))")
        }
        autoInfoRequested = true
        refreshInfo()
    }
}
