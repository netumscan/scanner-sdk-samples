import Foundation
import ScannerSDK

@MainActor
extension AppViewModel {
    func observe(_ session: ScannerSession) {
        stateTask?.cancel()
        stateTask = nil
        scanTask?.cancel()
        scanTask = nil
        initializationStageTask?.cancel()
        initializationStageTask = nil
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
                self.appendEvent(
                    .scan,
                    .info,
                    "type=\(scan.barcodeType) charset=\(session.getScanTextCharset().displayName) " +
                        "textBytesLength=\(scan.textBytes.count) rawBytesLength=\(scan.rawBytes.count)"
                )
            }
        }

        initializationStageTask = Task { [weak self] in
            guard let self else { return }
            for await event in session.initializationStages {
                if Task.isCancelled { return }
                if self.session?.handle != observedHandle {
                    return
                }
                let summary = self.sessionInitializationStageSummary(event)
                self.diagnosticsStore.updateRecentSessionInitStage(summary)
                let level: ConsoleEventLevel = (!event.success || event.stage == .failed) ? .error : .info
                self.appendEvent(.sdk, level, "initializeSession stage: \(summary)")
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
        initializationStageTask?.cancel()
        initializationStageTask = nil
        session = nil
        sessionHandle = nil
        connectedModelKey = nil
        diagnosticsStore.updateSessionState(nil)
        diagnosticsStore.updateResolvedModel("")
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
            try session.setScanTextTerminator(localTerminator)
        } catch {
            appendEvent(.ui, .warn, "\(DemoStrings.tr("apply_scan_terminator_on_ready_failed")): \(DemoErrorFormatter.detail(error))")
        }
        autoInfoRequested = true
        refreshInfo()
    }

    private func sessionInitializationStageSummary(_ event: SessionInitializationStageEvent) -> String {
        let stageLabel: String
        switch event.stage {
        case .started:
            stageLabel = "started"
        case .readingDeviceInfo:
            stageLabel = "readingDeviceInfo"
        case .readingBattery:
            stageLabel = "readingBattery"
        case .readingCapabilitySummary:
            stageLabel = "readingCapabilitySummary"
        case .readingOperationSupport:
            stageLabel = "readingOperationSupport"
        case .completed:
            stageLabel = "completed"
        case .failed:
            stageLabel = "failed"
        case "":
            stageLabel = "unknown"
        }

        var parts = [
            stageLabel,
            "trace=\(event.traceId)",
            "model=\(displayModelLabel(event.selectedModelKey))",
        ]
        if !event.success || event.errorCode != 0 {
            parts.append("error=\(event.errorCode)")
        }
        if let message = event.message, !message.isEmpty {
            parts.append("msg=\(message)")
        }
        return parts.joined(separator: " ")
    }
}
