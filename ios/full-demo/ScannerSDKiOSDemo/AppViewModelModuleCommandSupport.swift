import Foundation
import ScannerSDK

@MainActor
extension AppViewModel {
    func performModuleAction(_ action: DemoModuleAction) {
        let titleProvider = { self.currentModuleActionTitle(id: action.id, fallback: action.title) }
        guard let session = requireActiveSession(title: action.title, titleProvider: titleProvider) else { return }
        guard moduleCommandRunner.ensureReady(title: action.title, titleProvider: titleProvider) else { return }

        executeAction(title: action.title, sendText: nil, titleProvider: titleProvider) { [weak self] in
            guard let self else { return }
            self.appendEvent(.command, .debug, action.executionLabel)
            let response = try self.moduleCommandRunner.execute(
                session,
                request: ModuleCommandRequest(
                    family: action.family,
                    kind: action.kind,
                    parameterID: action.parameterID,
                    payload: action.payload,
                    persist: action.persist
                )
            )
            self.setLocalizedLastActionResult {
                "\(titleProvider()) \(DemoStrings.tr("command_completed"))"
            }
            self.appendEvent(
                .command,
                .info,
                self.formatCommandResponse(
                    prefix: "\(DemoStrings.tr("module_command")) \(action.title)",
                    response: response
                )
            )
        }
    }

    func readModuleParameter(_ preset: DemoModuleSettingPreset) {
        executeModuleParameter(
            title: "\(DemoStrings.tr("read_module_parameter")) 0x\(preset.formattedParameterID) \(preset.title)",
            titleProvider: {
                "\(DemoStrings.tr("read_module_parameter")) 0x\(preset.formattedParameterID) \(preset.title)"
            },
            family: preset.definition.moduleFamily,
            kind: .readParameter,
            parameterID: preset.parameterID,
            payload: Data(),
            persist: false
        )
    }

    func writeModuleParameter(
        _ preset: DemoModuleSettingPreset,
        payloadHex: String,
        persist: Bool
    ) {
        do {
            let payload = try parseModulePayloadHex(payloadHex)
            executeModuleParameter(
                title: "\(DemoStrings.tr("write_module_parameter")) 0x\(preset.formattedParameterID) \(preset.title)",
                titleProvider: {
                    "\(DemoStrings.tr("write_module_parameter")) 0x\(preset.formattedParameterID) \(preset.title)"
                },
                family: preset.definition.moduleFamily,
                kind: .writeParameter,
                parameterID: preset.parameterID,
                payload: payload,
                persist: persist
            )
        } catch {
            let detail = DemoErrorFormatter.detail(error)
            setLocalizedErrorText {
                "\(DemoStrings.tr("invalid_module_write_payload")): \(detail)"
            }
            appendEvent(.command, .error, errorText ?? "")
        }
    }

    func readModuleParameter(
        family: ModuleFamily,
        parameterIDText: String,
        label: String
    ) {
        do {
            let parameterID = try parseModuleParameterID(parameterIDText)
            let titleProvider = {
                self.moduleParameterCommandTitle(kind: .readParameter, family: family, parameterID: parameterID, fallbackLabel: label)
            }
            executeModuleParameter(
                title: titleProvider(),
                titleProvider: titleProvider,
                family: family,
                kind: .readParameter,
                parameterID: parameterID,
                payload: Data(),
                persist: false
            )
        } catch {
            let detail = DemoErrorFormatter.detail(error)
            setLocalizedErrorText {
                "\(DemoStrings.tr("invalid_module_parameter_id")): \(detail)"
            }
            appendEvent(.command, .error, errorText ?? "")
        }
    }

    func writeModuleParameter(
        family: ModuleFamily,
        parameterIDText: String,
        payloadHex: String,
        persist: Bool,
        label: String
    ) {
        do {
            let parameterID = try parseModuleParameterID(parameterIDText)
            let payload = try parseModulePayloadHex(payloadHex)
            let titleProvider = {
                self.moduleParameterCommandTitle(kind: .writeParameter, family: family, parameterID: parameterID, fallbackLabel: label)
            }
            executeModuleParameter(
                title: titleProvider(),
                titleProvider: titleProvider,
                family: family,
                kind: .writeParameter,
                parameterID: parameterID,
                payload: payload,
                persist: persist
            )
        } catch {
            let detail = DemoErrorFormatter.detail(error)
            setLocalizedErrorText {
                "\(DemoStrings.tr("invalid_custom_module_parameter")): \(detail)"
            }
            appendEvent(.command, .error, errorText ?? "")
        }
    }

    func writeNtc06hSetting(_ setting: Ntc06hSettingDefinition, saveAfterWrite: Bool) {
        let code = setting.settingCode.ifBlank(setting.templateExampleCode.ifBlank(setting.displayCode))
        let settingTitleProvider = { ntc06hSettingTitle(setting) }
        writeNtc06hRawSetting(
            code: code,
            label: settingTitleProvider(),
            saveAfterWrite: saveAfterWrite,
            titleProvider: {
                "\(DemoStrings.tr("send_ntc06h_setting")) \(setting.displayCode) \(settingTitleProvider())"
            }
        )
    }

    func writeNtc06hRawSetting(
        code: String,
        label: String,
        saveAfterWrite: Bool,
        titleProvider: (() -> String)? = nil
    ) {
        let trimmed = code.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            setLocalizedErrorText {
                DemoStrings.tr("ntc06h_setting_code_empty")
            }
            appendEvent(.command, .error, errorText ?? "")
            return
        }
        let currentTitleProvider = titleProvider ?? {
            "\(DemoStrings.tr("send_ntc06h_setting")) \(label)"
        }
        executeNtc06hSetting(
            title: currentTitleProvider(),
            titleProvider: currentTitleProvider,
            settingCode: code,
            saveAfterWrite: saveAfterWrite
        )
    }

    func saveNtc06hSettings() {
        executeModuleParameter(
            title: DemoStrings.tr("save_ntc06h_settings"),
            titleProvider: {
                DemoStrings.tr("save_ntc06h_settings")
            },
            family: .ntc06h,
            kind: .saveSettings,
            parameterID: 0,
            payload: Data(),
            persist: false
        )
    }

    func executeModuleParameter(
        title: String,
        titleProvider: (() -> String)? = nil,
        family: ModuleFamily,
        kind: ModuleCommandKind,
        parameterID: UInt32,
        payload: Data,
        persist: Bool
    ) {
        guard let session = requireActiveSession(title: title, titleProvider: titleProvider) else { return }
        guard moduleCommandRunner.ensureReady(title: title, titleProvider: titleProvider ?? { title }) else { return }
        executeAction(title: title, sendText: nil, titleProvider: titleProvider) { [weak self] in
            guard let self else { return }
            self.appendEvent(
                .command,
                .debug,
                "\(title): family=\(family) kind=\(kind) parameter=0x\(formatModuleParameterID(parameterID)) persist=\(persist) payload=\(hexSummary(payload).ifBlank("<empty>"))"
            )
            let response = try self.moduleCommandRunner.execute(
                session,
                request: ModuleCommandRequest(
                    family: family,
                    kind: kind,
                    parameterID: parameterID,
                    payload: payload,
                    persist: persist
                )
            )
            self.setLocalizedLastActionResult {
                "\((titleProvider ?? { title })()) \(DemoStrings.tr("command_completed"))"
            }
            self.appendEvent(
                .command,
                .info,
                self.formatCommandResponse(prefix: title, response: response)
            )
        }
    }

    func executeNtc06hSetting(
        title: String,
        titleProvider: (() -> String)? = nil,
        settingCode: String,
        saveAfterWrite: Bool
    ) {
        guard let session = requireActiveSession(title: title, titleProvider: titleProvider) else { return }
        guard moduleCommandRunner.ensureReady(title: title, titleProvider: titleProvider ?? { title }) else { return }
        executeAction(title: title, sendText: nil, titleProvider: titleProvider) { [weak self] in
            guard let self else { return }
            var didReceiveWriteResponse = false
            do {
                self.appendEvent(.command, .debug, "\(title): settingCode=\(escapeControlText(settingCode)) saveAfterWrite=\(saveAfterWrite)")
                let response = try self.moduleCommandRunner.execute(
                    session,
                    request: ModuleCommandRequest(
                        family: .ntc06h,
                        kind: .writeParameter,
                        parameterID: 0,
                        payload: Data(settingCode.utf8),
                        persist: false
                    )
                )
                didReceiveWriteResponse = true
                self.appendEvent(
                    .command,
                    .info,
                    self.formatCommandResponse(prefix: title, response: response)
                )
                if saveAfterWrite {
                    let saveResponse = try self.moduleCommandRunner.execute(
                        session,
                        request: ModuleCommandRequest(
                            family: .ntc06h,
                            kind: .saveSettings,
                            parameterID: 0,
                            payload: Data(),
                            persist: false
                        )
                    )
                    self.appendEvent(
                        .command,
                        .info,
                        self.formatCommandResponse(prefix: DemoStrings.tr("save_ntc06h_settings"), response: saveResponse)
                    )
                }
                self.setLocalizedLastActionResult {
                    "\((titleProvider ?? { title })()) \(DemoStrings.tr("command_completed"))"
                }
            } catch {
                if self.handleNtc06hWriteTimeout(
                    title: title,
                    titleProvider: titleProvider,
                    settingCode: settingCode,
                    saveAfterWrite: saveAfterWrite,
                    didReceiveWriteResponse: didReceiveWriteResponse,
                    error: error
                ) {
                    return
                }
                throw error
            }
        }
    }
}
