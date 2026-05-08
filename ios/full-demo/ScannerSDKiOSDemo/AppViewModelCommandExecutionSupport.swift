import Foundation
import ScannerSDK

@MainActor
extension AppViewModel {
    func requireActiveSession(
        title: String,
        titleProvider: (() -> String)? = nil
    ) -> ScannerSession? {
        return commandRunner.requireReadySession(
            session,
            onMissing: {
                let reason = DemoStrings.tr("no_active_session_action_hint")
                setLocalizedErrorText {
                    "\((titleProvider ?? { title })()): \(DemoStrings.tr("no_active_session_action_hint"))"
                }
                appendEvent(.session, .warn, "\((titleProvider ?? { title })()): \(reason)")
            },
            onNotReady: { state in
                let reason = DemoStrings.tr("session_not_ready_action_hint")
                diagnosticsStore.updateSessionState(state)
                setLocalizedErrorText {
                    "\((titleProvider ?? { title })()): \(DemoStrings.tr("session_not_ready_action_hint"))"
                }
                appendEvent(.session, .warn, "\((titleProvider ?? { title })()): \(reason)")
            }
        )
    }

    func handleNtc06hWriteTimeout(
        title: String,
        titleProvider: (() -> String)? = nil,
        settingCode: String,
        saveAfterWrite: Bool,
        didReceiveWriteResponse: Bool,
        error: Error
    ) -> Bool {
        guard moduleCommandRunner.isNtc06hSilentAckTimeout(error) else {
            return false
        }
        let currentTitleProvider = titleProvider ?? { title }
        let messageProvider: () -> String
        if didReceiveWriteResponse {
            messageProvider = {
                DemoStrings.format("ntc06h_write_ack_save_timeout", currentTitleProvider())
            }
        } else if saveAfterWrite {
            messageProvider = {
                DemoStrings.format("ntc06h_write_setting_save_timeout", currentTitleProvider(), settingCode)
            }
        } else {
            messageProvider = {
                DemoStrings.format("ntc06h_write_setting_timeout", currentTitleProvider(), settingCode)
            }
        }
        let message = messageProvider()
        setLocalizedLastActionResult(messageProvider)
        appendEvent(.command, .warn, message)
        appendEvent(.sdk, .warn, "\(title): \(DemoErrorFormatter.detail(error))")
        return true
    }

    func executeAction(
        title: String,
        sendText: String?,
        titleProvider: (() -> String)? = nil,
        requiredOperation: DemoSessionOperation? = nil,
        supportSession: ScannerSession? = nil,
        operation: @escaping () throws -> Void
    ) {
        commandRunner.execute(
            isExecuting: isExecuting,
            operation: { [weak self] in
                guard let self else { return }
                self.errorText = nil
                if let requiredOperation, let supportSession {
                    guard try self.ensureOperationSupport(
                        supportSession,
                        operation: requiredOperation,
                        title: title,
                        titleProvider: titleProvider
                    ) else {
                        return
                    }
                }
                try operation()
            },
            onFailure: { [weak self] error in
                guard let self else { return }
                let detail = DemoErrorFormatter.detail(error)
                self.setLocalizedErrorText {
                    "\(DemoStrings.tr("operation_failed")) [\((titleProvider ?? { title })())]: \(detail)"
                }
                self.appendEvent(.command, .error, "\(title) \(DemoStrings.tr("command_response_failed")): \(detail)")
            }
        )
    }

    func ensureOperationSupport(
        _ session: ScannerSession,
        operation: DemoSessionOperation,
        title: String,
        titleProvider: (() -> String)? = nil
    ) throws -> Bool {
        return try commandRunner.ensureOperationSupport(
            session,
            operation: operation,
            unsupportedReason: unsupportedOperationReason,
            onUnsupported: { reason in
                setLocalizedErrorText {
                    "\((titleProvider ?? { title })()): \(self.unsupportedOperationReason(operation))"
                }
                appendEvent(.command, .warn, "\(title): \(reason)")
            }
        )
    }

    func ensureMasterCommandSupport(
        _ session: ScannerSession,
        command: MasterCommand,
        title: String,
        titleProvider: (() -> String)? = nil
    ) throws -> Bool {
        return try commandRunner.ensureMasterCommandSupport(
            session,
            command: command,
            unsupportedReason: { DemoStrings.tr("unsupported_master_command") },
            onUnsupported: { reason in
                setLocalizedErrorText {
                    "\((titleProvider ?? { title })()): \(DemoStrings.tr("unsupported_master_command"))"
                }
                appendEvent(.command, .warn, "\(title): \(reason)")
            }
        )
    }

    func reportBusyCommand() {
        let message = DemoStrings.tr("command_already_running_wait")
        setLocalizedErrorText {
            DemoStrings.tr("command_already_running_wait")
        }
        appendEvent(.command, .warn, message)
    }

    func unsupportedOperationReason(_ operation: DemoSessionOperation) -> String {
        switch operation {
        case .initializeSession:
            return DemoStrings.tr("unsupported_initialize_session")
        case .getBatteryInfo:
            return DemoStrings.tr("unsupported_get_battery_info")
        case .basicDeviceCommands:
            return DemoStrings.tr("unsupported_basic_device_commands")
        case .textCommands:
            return DemoStrings.tr("unsupported_text_commands")
        case .dataRuleCommands:
            return DemoStrings.tr("unsupported_data_rule_commands")
        case .beep:
            return DemoStrings.tr("unsupported_beep")
        case .disableAckBeep:
            return DemoStrings.tr("unsupported_disable_ack_beep")
        case .vibrateOn, .vibrateOff:
            return DemoStrings.tr("unsupported_vibration")
        }
    }

    func appendCommandSend(_ commandText: String, prefix: String) {
        appendEvent(.command, .debug, "\(prefix): \(commandText) frameHex=\(hexSummary(encodeSsiCommand(commandText)))")
    }
}
