import Foundation
import ScannerSDK

@MainActor
extension AppViewModel {
    func performQuickAction(_ action: DemoQuickAction) {
        switch action {
        case .refreshInfo:
            refreshInfo()
        case .getBattery:
            getBattery()
        case .ackBeepOn:
            beep()
        case .ackBeepOff:
            disableAckBeep()
        case .vibrateOn:
            vibrateOn()
        case .vibrateOff:
            vibrateOff()
        }
    }

    func refreshInfo() {
        guard let session = requireActiveSession(title: commandCodeTitle(.getInfo)) else { return }
        appendCommandSend("$SW#VER", prefix: DemoStrings.tr("send_command"))
        executeAction(
            title: commandCodeTitle(.getInfo),
            sendText: "$SW#VER",
            requiredOperation: .initializeSession,
            supportSession: session
        ) { [weak self] in
            guard let self else { return }
            let initialization = try session.initializeSession(applyModelConfig: true)
            let snapshot = DeviceSnapshot(
                info: initialization.info,
                batteryInfo: initialization.batteryInfo,
                modelConfigApplied: initialization.modelConfigApplied,
                resolvedModel: try session.getResolvedModelId(),
                capability: try session.getDeviceCapabilitySummary()
            )
            let effectiveCapability = self.effectiveCapabilityFor(snapshot.capability)
            let canExecuteModuleCommands = try self.queryModuleCommandAvailability(session)
            self.applyDeviceState(snapshot: snapshot, effectiveCapability: effectiveCapability, canExecuteModuleCommands: canExecuteModuleCommands)
            self.setLocalizedLastActionResult {
                DemoStrings.tr("device_info_loaded")
            }
            if snapshot.modelConfigApplied {
                self.appendEvent(
                    .command,
                    .info,
                    DemoStrings.format("model_config_applied_after_refresh", displayModelLabel(self.activeModelId))
                )
            }
            self.appendEvent(.command, .debug, formatSdkResolvedModelSummary(selectedModelId: self.activeModelId, resolvedModel: snapshot.resolvedModel))
            self.appendEvent(.command, .info, "\(DemoStrings.tr("info")) firmware=\(snapshot.info.firmwareVersion) hardware=\(snapshot.info.hardwareVersion) serial=\(snapshot.info.serialNumber)")
            if let batteryInfo = snapshot.batteryInfo {
                self.appendEvent(.command, .info, "\(DemoStrings.tr("battery")) raw=\(batteryInfo.rawText)")
                self.appendEvent(.command, .info, "\(DemoStrings.tr("battery_parsed")) voltage=\(batteryInfo.voltageText) percent=\(batteryInfo.percent)")
            }
            self.appendEvent(.command, .info, "\(DemoStrings.tr("capability")) \(effectiveCapability.displaySummary)")
        }
    }

    func getBattery() {
        guard let session = requireActiveSession(title: commandCodeTitle(.getBatteryInfo)) else { return }
        appendCommandSend("%BAT_VOL#", prefix: DemoStrings.tr("send_command"))
        executeAction(
            title: commandCodeTitle(.getBatteryInfo),
            sendText: "%BAT_VOL#",
            requiredOperation: .getBatteryInfo,
            supportSession: session
        ) { [weak self] in
            guard let self else { return }
            let battery = try session.getBatteryInfo()
            self.applyBatterySummary(.value(voltageText: battery.voltageText, percent: battery.percent))
            self.setLocalizedLastActionResult {
                DemoStrings.tr("battery_loaded")
            }
            self.appendEvent(.command, .info, "\(DemoStrings.tr("battery")) raw=\(battery.rawText)")
        }
    }

    func readCharset() {
        executeTextCommand(label: "READ_CURRENT_CHARSET", commandText: "%CHARSET#")
    }

    func beep() {
        guard let session = requireActiveSession(title: commandCodeTitle(.beep)) else { return }
        appendCommandSend("%ACKBEEP#1", prefix: DemoStrings.tr("send_command"))
        executeAction(
            title: commandCodeTitle(.beep),
            sendText: "%ACKBEEP#1",
            requiredOperation: .beep,
            supportSession: session
        ) { [weak self] in
            guard let self else { return }
            try session.beep()
            self.applyCachedSuccessState(
                session,
                actionResult: DemoStrings.tr("beep_command_sent"),
                actionResultProvider: {
                    DemoStrings.tr("beep_command_sent")
                }
            )
            self.appendEvent(.command, .info, DemoStrings.tr("beep_command_sent"))
        }
    }

    func disableAckBeep() {
        guard let session = requireActiveSession(title: commandCodeTitle(.disableAckBeep)) else { return }
        appendCommandSend("%ACKBEEP#0", prefix: DemoStrings.tr("send_command"))
        executeAction(
            title: commandCodeTitle(.disableAckBeep),
            sendText: "%ACKBEEP#0",
            requiredOperation: .disableAckBeep,
            supportSession: session
        ) { [weak self] in
            guard let self else { return }
            try session.disableAckBeep()
            self.applyCachedSuccessState(
                session,
                actionResult: DemoStrings.tr("ack_beep_off_command_sent"),
                actionResultProvider: {
                    DemoStrings.tr("ack_beep_off_command_sent")
                }
            )
            self.appendEvent(.command, .info, DemoStrings.tr("ack_beep_off_command_sent"))
        }
    }

    func vibrateOn() {
        guard let session = requireActiveSession(title: commandCodeTitle(.vibrateOn)) else { return }
        appendCommandSend("$MOTO#0", prefix: DemoStrings.tr("send_command"))
        executeAction(
            title: commandCodeTitle(.vibrateOn),
            sendText: "$MOTO#0",
            requiredOperation: .vibrateOn,
            supportSession: session
        ) { [weak self] in
            guard let self else { return }
            try session.vibrateOn()
            self.applyCachedSuccessState(
                session,
                actionResult: DemoStrings.tr("vibrate_on_command_sent"),
                actionResultProvider: {
                    DemoStrings.tr("vibrate_on_command_sent")
                }
            )
            self.appendEvent(.command, .info, DemoStrings.tr("vibrate_on_command_sent"))
        }
    }

    func vibrateOff() {
        guard let session = requireActiveSession(title: commandCodeTitle(.vibrateOff)) else { return }
        appendCommandSend("$MOTO#1", prefix: DemoStrings.tr("send_command"))
        executeAction(
            title: commandCodeTitle(.vibrateOff),
            sendText: "$MOTO#1",
            requiredOperation: .vibrateOff,
            supportSession: session
        ) { [weak self] in
            guard let self else { return }
            try session.vibrateOff()
            self.applyCachedSuccessState(
                session,
                actionResult: DemoStrings.tr("vibrate_off_command_sent"),
                actionResultProvider: {
                    DemoStrings.tr("vibrate_off_command_sent")
                }
            )
            self.appendEvent(.command, .info, DemoStrings.tr("vibrate_off_command_sent"))
        }
    }

    func executeCommand(_ entry: DemoCommandEntry) {
        switch entry.command {
        case .basic(let basic):
            executeBasicCommand(label: entry.title, command: basic)
        case .master(let master):
            executeMasterCommand(label: entry.title, command: master)
        case .text(let text):
            executeTextCommand(label: entry.title, commandText: text)
        }
    }

    func executeDataRule(mode: DataRuleFormMode, valueA: String, valueB: String) {
        guard let session = requireActiveSession(title: mode.title) else { return }
        executeAction(
            title: mode.title,
            sendText: nil,
            requiredOperation: .dataRuleCommands,
            supportSession: session
        ) { [weak self] in
            guard let self else { return }
            let rule = try DemoCommandCatalog.buildDataRule(mode: mode, valueA: valueA, valueB: valueB)
            let response = try session.executeDataRuleCommand(kind: rule.kind, primary: rule.primary, secondary: rule.secondary)
            let line = self.formatCommandResponse(prefix: "\(DemoStrings.tr("data_rule")) \(mode.title)", response: response)
            self.setLocalizedLastActionResult {
                "\(DemoStrings.tr("data_rule_command_completed")): \(mode.title)"
            }
            self.appendEvent(.command, .info, line)
        }
    }

    func executeBasicCommand(label: String, command: BasicDeviceCommand) {
        guard let session = requireActiveSession(title: label) else { return }
        let sendText = commandText(for: command)
        appendCommandSend(sendText, prefix: DemoStrings.tr("send_basic_command"))
        executeAction(
            title: label,
            sendText: nil,
            requiredOperation: .basicDeviceCommands,
            supportSession: session
        ) { [weak self] in
            guard let self else { return }
            let response = try session.executeBasicDeviceCommand(command)
            let line = self.formatCommandResponse(prefix: "\(DemoStrings.tr("basic_command")) \(label)", response: response)
            self.setLocalizedLastActionResult {
                "\(DemoStrings.tr("basic_command_completed")): \(self.basicCommandLabel(command))"
            }
            self.appendEvent(.command, .info, line)
        }
    }

    func executeMasterCommand(label: String, command: MasterCommand) {
        guard let session = requireActiveSession(title: label) else { return }
        let sendText = commandText(for: command)
        appendCommandSend(sendText, prefix: DemoStrings.tr("send_master_command"))
        executeAction(title: label, sendText: nil) { [weak self] in
            guard let self else { return }
            guard try self.ensureMasterCommandSupport(session, command: command, title: label) else {
                return
            }
            let response = try session.executeMasterCommand(command)
            self.applyCachedSuccessState(
                session,
                actionResult: "\(DemoStrings.tr("master_command_completed")): \(label)",
                actionResultProvider: {
                    "\(DemoStrings.tr("master_command_completed")): \(self.masterCommandLabel(command))"
                }
            )
            let line = self.formatCommandResponse(prefix: "\(DemoStrings.tr("master_command")) \(label)", response: response)
            self.appendEvent(.command, .info, line)
        }
    }

    func executeTextCommand(label: String, commandText: String) {
        guard let session = requireActiveSession(title: label) else { return }
        appendCommandSend(commandText, prefix: DemoStrings.tr("send_master_command"))
        executeAction(
            title: label,
            sendText: nil,
            requiredOperation: .textCommands,
            supportSession: session
        ) { [weak self] in
            guard let self else { return }
            let response = try session.executeTextCommand(commandText)
            self.applyCachedSuccessState(
                session,
                actionResult: "\(DemoStrings.tr("master_command_completed")): \(label)",
                actionResultProvider: {
                    let currentLabel = DemoCommandCatalog.textCommandTitle(
                        commandText: commandText,
                        fallback: label
                    )
                    return "\(DemoStrings.tr("master_command_completed")): \(currentLabel)"
                }
            )
            let line = self.formatCommandResponse(prefix: "\(DemoStrings.tr("master_command")) \(label)", response: response)
            self.appendEvent(.command, .info, line)
        }
    }
}
