import Foundation
import ScannerSDK

@MainActor
final class AppViewModel: ObservableObject {
    private static var cachedEvents: [ConsoleEvent] = []

    @Published var isInitialized = false
    @Published var isDiscovering = false
    @Published var isExecuting = false
    @Published var isConnecting = false
    @Published var devices: [DiscoveredDevice] = []
    @Published var selectedDeviceID: String?
    @Published var selectedModelKey: String = "CS7501"
    @Published var supportedModels: [DemoSupportedModel] = []
    @Published var isLoadingSupportedModels = false
    @Published var supportedModelsError: String?
    @Published var sessionState: SessionState = .idle
    @Published var lastActionResult: String?
    @Published var errorText: String?
    @Published var recoveryAction: DemoRecoveryAction?
    @Published var alertErrorText: String?
    @Published var events: [ConsoleEvent]
    @Published var scanCount = 0
    @Published var lastScanText = DemoStrings.tr("last_scan_empty", fallback: "No scan received yet")
    @Published var lastScanMeta = DemoStrings.tr("last_scan_waiting", fallback: "Waiting for scan data")
    @Published var lastScanRawHex = DemoStrings.tr("last_scan_raw_empty", fallback: "No raw bytes")
    @Published var localCharset: ScanTextCharset = .utf8
    @Published var localTerminator = Data([0x0D])
    @Published var latestCapabilityValues: [String: String] = [:]
    @Published var latestCapabilityDraftValues: [String: String] = [:]
    @Published var capabilityEntries: [CapabilityEntry] = []
    @Published var deviceInfoText = DemoStrings.tr("device_info_not_loaded")
    @Published var batteryInfoText = DemoStrings.tr("battery_not_loaded")
    @Published var sessionOperationSupport: SessionOperationSupport?

    let diagnosticsStore = DemoDiagnosticsStore()

    private let sdk = ScannerSDK.shared
    private let supportedModelsLoader: @Sendable () throws -> [SupportedDeviceModel]
    private var session: ScannerSession?
    private var discoveryTask: Task<Void, Never>?
    private var discoveryFailureTask: Task<Void, Never>?
    private var debugTask: Task<Void, Never>?
    private var stateTask: Task<Void, Never>?
    private var scanTask: Task<Void, Never>?
    private var connectTask: Task<Void, Never>?
    private var supportedModelsTask: Task<Void, Never>?

    private struct SessionCommandResult {
        let summary: String
        var deviceInfoText: String?
        var batteryInfoText: String?
        var settingValue: (key: String, summary: String, draftValue: String)?
    }

    init(
        supportedModelsLoader: @escaping @Sendable () throws -> [SupportedDeviceModel] = {
            try ScannerSDK.shared.getSupportedDeviceModels(transport: .bleGatt)
        }
    ) {
        self.supportedModelsLoader = supportedModelsLoader
        events = Self.cachedEvents
        if events.isEmpty {
            appendBootstrapEvent()
        }
        diagnosticsStore.updateSelectedModel(selectedModelKey)
    }

    deinit {
        discoveryTask?.cancel()
        discoveryFailureTask?.cancel()
        debugTask?.cancel()
        stateTask?.cancel()
        scanTask?.cancel()
        connectTask?.cancel()
        supportedModelsTask?.cancel()
    }

    var displayDevices: [DiscoveredDevice] { devices }
    var selectedModelSummary: String { formatSelectedModelSummary(selectedModelKey) }
    var statusSummary: String {
        if isConnecting { return DemoStrings.tr("connecting") }
        if let session { return statusText(session.latestState) }
        return isInitialized ? DemoStrings.tr("sdk_initialized") : DemoStrings.tr("not_connected")
    }
    var deviceSummary: String {
        guard let selectedDeviceID else { return DemoStrings.tr("no_selected_device") }
        let name = devices.first { $0.deviceId == selectedDeviceID }?.name ?? DemoStrings.unknownDeviceName
        return "\(name.ifBlank(DemoStrings.unknownDeviceName)) / \(selectedDeviceID)"
    }
    var infoSummary: String { deviceInfoText }
    var sdkResolvedModelSummary: String { formatSdkResolvedModelSummary(selectedModelKey: selectedModelKey, resolvedModel: sessionResolvedModelKey) }
    var capabilitySummary: String {
        sdk.getDeviceModelProfile(activeModelKey)?.capability.displaySummary ?? DemoStrings.tr("capability_summary_not_loaded")
    }
    var moduleSummary: String { DemoStrings.tr("module_capability_not_loaded") }
    var moduleCommandAvailabilitySummary: String { DemoStrings.tr("not_detected") }
    var batterySummary: String { batteryInfoText }
    var deviceCharsetSummary: String {
        latestCapabilityValues["setting.DeviceCharset"] ?? DemoStrings.tr("not_read_charset_query")
    }
    var deviceTerminalSummary: String { hexSummary(localTerminator) }
    var localCharsetSummary: String { localCharset.displayName }
    var localTerminatorSummary: String { hexSummary(localTerminator) }
    var diagnosticsSummary: String { diagnosticsStore.summaryText() }
    var sessionStateText: String { String(describing: sessionState) }
    var canInitiateConnection: Bool { !isConnecting && session == nil }
    var hasActiveSession: Bool { session != nil }
    var hasReadySession: Bool { session?.latestState == .ready }
    var canChangeDiscoveryTarget: Bool { !isConnecting && !isDiscovering && !hasActiveSession }
    var hasValidSelectedModel: Bool {
        supportedModels.contains {
            $0.modelKey.caseInsensitiveCompare(selectedModelKey) == .orderedSame
        }
    }
    var canRunSessionCommands: Bool { hasReadySession && !isExecuting }
    var canOpenLogs: Bool { !isConnecting }
    var activeModelKey: String { selectedModelKey == "" ? "CS7501" : selectedModelKey }
    var dataRulesSupported: Bool {
        sessionOperationSupport?.supportsApplyDataRule
            ?? sdk.getDeviceModelProfile(activeModelKey)?.capability.supportsDataRules
            ?? true
    }
    var refreshInfoSupported: Bool { sessionOperationSupport?.supportsRefreshInfo ?? hasReadySession }
    var batteryInfoSupported: Bool { sessionOperationSupport?.supportsGetBatteryInfo ?? hasReadySession }
    var triggerScanSupported: Bool { sessionOperationSupport?.supportsTriggerScan ?? hasReadySession }
    var ackBeepSupported: Bool { sessionOperationSupport?.supportsSetAckBeepEnabled ?? hasReadySession }
    var vibrationSupported: Bool { sessionOperationSupport?.supportsSetVibrationEnabled ?? hasReadySession }
    var operationSupportSummary: String {
        sessionOperationSupport?.displaySummary ?? DemoStrings.tr("capability_summary_not_loaded")
    }
    private var sessionResolvedModelKey: String {
        (try? session?.getResolvedModelKey()) ?? selectedModelKey
    }

    func onAppear() {
        diagnosticsStore.refreshPlatform()
        initializeSdk()
    }

    func refreshLocalizedUi() {
        if scanCount == 0 {
            lastScanText = DemoStrings.tr("last_scan_empty", fallback: "No scan received yet")
            lastScanMeta = DemoStrings.tr("last_scan_waiting", fallback: "Waiting for scan data")
            lastScanRawHex = DemoStrings.tr("last_scan_raw_empty", fallback: "No raw bytes")
        }
        if deviceInfoText == "device_info_not_loaded" || deviceInfoText == "Device info not loaded" {
            deviceInfoText = DemoStrings.tr("device_info_not_loaded")
        }
        if batteryInfoText == "battery_not_loaded" || batteryInfoText == "Battery not loaded" {
            batteryInfoText = DemoStrings.tr("battery_not_loaded")
        }
        objectWillChange.send()
    }

    func applySelectedModel(_ modelKey: String) {
        if !supportedModels.isEmpty,
           !supportedModels.contains(where: {
               $0.modelKey.caseInsensitiveCompare(modelKey) == .orderedSame
           }) {
            return
        }
        selectedModelKey = modelKey
        diagnosticsStore.updateSelectedModel(modelKey)
        loadCapabilityEntries()
        lastActionResult = "\(DemoStrings.tr("selected_model")): \(displayModelLabel(modelKey))"
    }

    func initializeSdk() {
        if isInitialized {
            lastActionResult = DemoStrings.tr("sdk_already_initialized")
            bindGlobalStreamsIfNeeded()
            loadCapabilityEntries()
            if supportedModels.isEmpty && !isLoadingSupportedModels {
                reloadSupportedModels()
            }
            return
        }
        do {
            try sdk.initialize()
            isInitialized = true
            diagnosticsStore.updateSdkInitialized(true)
            diagnosticsStore.refreshPlatform(sdkVersion: sdk.version)
            bindGlobalStreamsIfNeeded()
            loadCapabilityEntries()
            reloadSupportedModels()
            lastActionResult = DemoStrings.tr("sdk_initialization_completed")
            appendEvent(.sdk, .info, DemoStrings.tr("sdk_initialized"))
        } catch {
            diagnosticsStore.updateSdkInitialized(false)
            errorText = "\(DemoStrings.tr("initialize_failed")): \(DemoErrorFormatter.detail(error))"
            alertErrorText = errorText
            appendEvent(.sdk, .error, errorText ?? "")
        }
    }

    func reloadSupportedModels() {
        supportedModelsTask?.cancel()
        supportedModels = []
        supportedModelsError = nil
        isLoadingSupportedModels = true
        appendEvent(.sdk, .info, DemoStrings.tr(
            "supported_models_loading_logged",
            fallback: "Loading supported models: BLE GATT"
        ))

        let loader = supportedModelsLoader
        supportedModelsTask = Task { [weak self] in
            await Task.yield()
            guard !Task.isCancelled else { return }
            do {
                let loadedModels = normalizeSupportedModels(try loader().map(DemoSupportedModel.init(sdkModel:)))
                guard let self, !Task.isCancelled else { return }
                let resolvedModelKey = selectSupportedModel(
                    loadedModels,
                    currentModelKey: self.selectedModelKey
                )
                self.supportedModels = loadedModels
                self.selectedModelKey = resolvedModelKey
                self.isLoadingSupportedModels = false
                self.supportedModelsError = nil
                self.diagnosticsStore.updateSelectedModel(resolvedModelKey)
                self.loadCapabilityEntries()
                self.appendEvent(
                    .sdk,
                    .info,
                    DemoStrings.format(
                        "supported_models_loaded_logged",
                        fallback: "Supported models loaded: %d",
                        loadedModels.count
                    )
                )
            } catch {
                guard let self, !Task.isCancelled else { return }
                self.supportedModels = []
                self.isLoadingSupportedModels = false
                self.supportedModelsError = DemoStrings.tr(
                    "supported_models_load_failed",
                    fallback: "Failed to load supported models."
                )
                self.appendEvent(
                    .sdk,
                    .error,
                    "\(DemoStrings.tr("supported_models_load_failed", fallback: "Failed to load supported models.")): \(DemoErrorFormatter.detail(error))"
                )
            }
        }
    }

    func makeCapabilityCatalogSnapshot() throws -> CapabilityCatalogSnapshot {
        let domains: [CapabilityDomain]
        let entries: [CapabilityEntry]
        if let session, session.latestState == .ready {
            domains = try session.getCapabilityDomains()
            entries = try session.getCapabilityEntries()
        } else {
            domains = try sdk.getCapabilityDomains(
                modelKey: activeModelKey,
                transport: .bleGatt
            )
            entries = try sdk.getCapabilityEntries(
                modelKey: activeModelKey,
                transport: .bleGatt
            )
        }
        let labels = try CapabilityLabelKind.allCases.flatMap {
            try sdk.getCapabilityLabels(kind: $0)
        }
        return CapabilityCatalogSnapshot(
            domains: domains,
            entries: entries,
            labels: labels
        )
    }

    func startDiscovery() {
        guard isInitialized else {
            lastActionResult = DemoStrings.tr("initialize_sdk_before_discovery")
            appendEvent(.ui, .warn, lastActionResult ?? "")
            return
        }
        guard canChangeDiscoveryTarget else {
            lastActionResult = DemoStrings.tr(
                "finish_current_operation_before_discovery",
                fallback: "Finish the current operation before starting discovery."
            )
            appendEvent(.ui, .warn, lastActionResult ?? "")
            return
        }
        guard hasValidSelectedModel else {
            lastActionResult = DemoStrings.tr(
                "select_supported_model_before_discovery",
                fallback: "Select a supported model before starting discovery."
            )
            appendEvent(.ui, .warn, lastActionResult ?? "")
            return
        }
        errorText = nil
        recoveryAction = nil
        devices.removeAll()
        selectedDeviceID = nil
        diagnosticsStore.updateSelectedModel(selectedModelKey)
        diagnosticsStore.updateResolvedModel("")
        diagnosticsStore.updateSessionState(nil)
        bindDiscoveryStreams()
        do {
            try sdk.startDiscovery(transports: [.bleGatt], selectedModelKey: selectedModelKey)
            isDiscovering = true
            sessionState = .discovering
            lastActionResult = "\(DemoStrings.tr("started_discovery_for_target_model")): \(displayModelLabel(selectedModelKey))"
            appendEvent(.session, .info, lastActionResult ?? "")
        } catch {
            isDiscovering = false
            sessionState = .error
            let detail = DemoErrorFormatter.detail(error)
            errorText = "\(DemoStrings.tr("start_discovery_failed")): \(detail)"
            appendEvent(.session, .error, errorText ?? "")
        }
    }

    func stopDiscovery() {
        guard isDiscovering else { return }
        do {
            try sdk.stopDiscovery()
            isDiscovering = false
            if session == nil {
                sessionState = .idle
            }
            lastActionResult = DemoStrings.tr("discovery_stopped")
            appendEvent(.session, .info, lastActionResult ?? "")
        } catch {
            errorText = "\(DemoStrings.tr("stop_discovery_failed")): \(DemoErrorFormatter.detail(error))"
            appendEvent(.session, .error, errorText ?? "")
        }
    }

    func connect(_ device: DiscoveredDevice) {
        guard canInitiateConnection else { return }
        isConnecting = true
        selectedDeviceID = device.deviceId
        stopDiscovery()
        sessionState = .connecting
        appendEvent(.session, .info, "\(DemoStrings.tr("connect_open_console")): \(device.name.ifBlank(DemoStrings.unnamedDevice))")
        connectTask?.cancel()
        connectTask = Task {
            do {
                let session = try await sdk.connectReady(
                    deviceId: device.deviceId,
                    transport: device.transportType,
                    selectedModelKey: resolveConnectionModelKey(
                        selectedModelKey: selectedModelKey,
                        discoveredModelKey: device.modelKey
                    )
                )
                self.session = session
                isConnecting = false
                sessionState = session.latestState
                diagnosticsStore.updateSessionState(session.latestState)
                diagnosticsStore.updateResolvedModel((try? session.getResolvedModelKey()) ?? selectedModelKey)
                refreshSessionOperationSupport(using: session)
                loadCapabilityEntries()
                lastActionResult = DemoStrings.tr("connected_open_console")
                observe(session)
            } catch {
                isConnecting = false
                selectedDeviceID = nil
                sessionState = .error
                errorText = "\(DemoStrings.tr("connect_failed")): \(DemoErrorFormatter.detail(error))"
                appendEvent(.session, .error, errorText ?? "")
            }
            connectTask = nil
        }
    }

    func disconnect() {
        do {
            try session?.disconnect()
        } catch {
            appendEvent(.session, .error, "\(DemoStrings.tr("disconnect_failed")): \(DemoErrorFormatter.detail(error))")
        }
        handleSessionClosed()
    }

    func triggerScan() {
        guard triggerScanSupported else {
            lastActionResult = DemoStrings.tr("unsupported_trigger_scan", fallback: "The current session does not support trigger scan.")
            appendEvent(.command, .warn, lastActionResult ?? "")
            return
        }
        runSessionCommand(label: DemoStrings.tr("trigger_scan_action", fallback: "Trigger Scan")) { session in
            try session.triggerScan()
            return SessionCommandResult(summary: DemoStrings.tr("trigger_scan_action", fallback: "Trigger Scan"))
        }
    }

    func requestInfo() {
        guard refreshInfoSupported else {
            lastActionResult = DemoStrings.tr("unsupported_refresh_info", fallback: "The current session does not support loading device info.")
            appendEvent(.command, .warn, lastActionResult ?? "")
            return
        }
        runSessionCommand(label: DemoStrings.tr("load_device_info_action", fallback: "Load Device Info")) { session in
            let info = try session.refreshInfo()
            let summary = DeviceStateSummaryFormatter.format(info: info).infoSummary
            return SessionCommandResult(summary: summary, deviceInfoText: summary)
        }
    }

    func requestBatteryLevel() {
        guard batteryInfoSupported else {
            lastActionResult = DemoStrings.tr("unsupported_get_battery_info")
            appendEvent(.command, .warn, lastActionResult ?? "")
            return
        }
        runSessionCommand(label: DemoStrings.tr("load_battery_info_action", fallback: "Load Battery Info")) { session in
            let battery = try session.getBatteryInfo()
            let summary = "\(battery.rawText) / \(battery.percent)%"
            return SessionCommandResult(summary: summary, batteryInfoText: summary)
        }
    }

    func requestStorageUsage() {
        runSessionCommand(label: DemoStrings.tr("load_memory_usage_action", fallback: "Memory Usage")) { session in
            let usage = try session.getStorageUsage()
            let summary = "\(DemoStrings.tr("memory_usage", fallback: "Memory usage")) count=\(usage.barcodeCount) used=\(usage.used)/\(usage.capacity) remaining=\(usage.remaining) raw=\(usage.rawText)"
            return SessionCommandResult(summary: summary)
        }
    }

    func setAckBeepEnabled(_ enabled: Bool) {
        guard ackBeepSupported else {
            lastActionResult = DemoStrings.tr("unsupported_beep")
            appendEvent(.command, .warn, lastActionResult ?? "")
            return
        }
        let label = enabled
            ? DemoStrings.tr("ack_beep_on_action", fallback: "Ack Beep On")
            : DemoStrings.tr("ack_beep_off_action", fallback: "Ack Beep Off")
        runSessionCommand(label: label) { session in
            let response = try session.setAckBeepEnabled(enabled)
            let summary = enabled
                ? DemoStrings.tr("ack_beep_on_command_sent", fallback: "Ack beep enable command sent")
                : DemoStrings.tr("ack_beep_off_command_sent")
            return SessionCommandResult(summary: "\(summary) / \(Self.commandResponseSummary(response))")
        }
    }

    func setVibrationEnabled(_ enabled: Bool) {
        guard vibrationSupported else {
            lastActionResult = DemoStrings.tr("unsupported_vibration")
            appendEvent(.command, .warn, lastActionResult ?? "")
            return
        }
        let label = enabled
            ? DemoStrings.tr("vibrate_on_action", fallback: "Vibrate On")
            : DemoStrings.tr("vibrate_off_action", fallback: "Vibrate Off")
        runSessionCommand(label: label) { session in
            let response = try session.setVibrationEnabled(enabled)
            let summary = enabled
                ? DemoStrings.tr("vibrate_on_command_sent")
                : DemoStrings.tr("vibrate_off_command_sent")
            return SessionCommandResult(summary: "\(summary) / \(Self.commandResponseSummary(response))")
        }
    }

    func executeDataRule(mode: DataRuleFormMode, valueA: String, valueB: String) {
        guard dataRulesSupported else {
            lastActionResult = DemoStrings.tr("unsupported_data_rule_commands")
            appendEvent(.command, .warn, lastActionResult ?? "")
            return
        }
        let label = "\(DemoStrings.tr("data_rule")): \(mode.title)"
        do {
            let rule = try DemoCommandCatalog.buildDataRule(mode: mode, valueA: valueA, valueB: valueB)
            runSessionCommand(label: label) { session in
                let response = try session.applyDataRule(rule)
                return SessionCommandResult(
                    summary: "\(DemoStrings.tr("data_rule_command_completed")) / \(Self.commandResponseSummary(response))"
                )
            }
        } catch {
            errorText = "\(label): \(DemoErrorFormatter.detail(error))"
            lastActionResult = DemoStrings.tr("data_rule_failed", fallback: "Data rule failed")
            appendEvent(.command, .error, errorText ?? "")
        }
    }

    func readCapability(_ definition: CapabilityEntry) {
        let displayName = localizedCapabilityActionTitle(definition)
        runSessionCommand(label: "\(DemoStrings.tr("read")) \(displayName)") { session in
            let value = try session.readCapabilityValue(definition.entryKey)
            let summary = Self.settingValueSummary(value, definition: definition)
            return SessionCommandResult(
                summary: "\(displayName): \(summary)",
                settingValue: (
                    definition.entryKey,
                    summary,
                    Self.settingDraftText(value, definition: definition)
                )
            )
        }
    }

    func readCapabilities(_ definitions: [CapabilityEntry]) {
        let readableDefinitions = definitions.filter { $0.kind == .setting && $0.supportsRead }
        guard let session, session.latestState == .ready else {
            lastActionResult = DemoStrings.tr("connect_ready_session_before_command", fallback: "Connect a ready scanner session before running this command.")
            appendEvent(.command, .warn, lastActionResult ?? "")
            return
        }
        guard !readableDefinitions.isEmpty else {
            lastActionResult = DemoStrings.tr("no_public_settings_available")
            appendEvent(.command, .warn, lastActionResult ?? "")
            return
        }
        guard !isExecuting else { return }

        isExecuting = true
        errorText = nil
        lastActionResult = DemoStrings.format(
            "command_executing",
            fallback: "Executing"
        ) + ": " + DemoStrings.format(
            "read_visible_settings_count",
            fallback: "Read Visible Settings (%d)",
            readableDefinitions.count
        )
        let observedHandle = session.handle
        Task(priority: .userInitiated) { [weak self, session] in
            var successCount = 0
            var failureMessages: [String] = []
            var latestValues: [(key: String, summary: String, draftValue: String)] = []

            for definition in readableDefinitions {
                do {
                    let value = try session.readCapabilityValue(definition.entryKey)
                    let summary = Self.settingValueSummary(value, definition: definition)
                    latestValues.append((
                        definition.entryKey,
                        summary,
                        Self.settingDraftText(value, definition: definition)
                    ))
                    successCount += 1
                } catch {
                    failureMessages.append("\(localizedCapabilityActionTitle(definition)): \(DemoErrorFormatter.detail(error))")
                }
            }

            await MainActor.run {
                guard let self else { return }
                self.isExecuting = false
                guard self.session?.handle == observedHandle else { return }

                for value in latestValues {
                    self.latestCapabilityValues[value.key] = value.summary
                    self.latestCapabilityDraftValues[value.key] = value.draftValue
                }

                if failureMessages.isEmpty {
                    self.lastActionResult = DemoStrings.format(
                        "read_visible_settings_success",
                        fallback: "Read %d settings.",
                        successCount
                    )
                    self.appendEvent(.command, .info, self.lastActionResult ?? "")
                } else {
                    self.lastActionResult = DemoStrings.format(
                        "read_visible_settings_partial",
                        fallback: "Read %d settings, %d failed.",
                        successCount,
                        failureMessages.count
                    )
                    self.errorText = failureMessages.joined(separator: "\n")
                    self.appendEvent(.command, .warn, self.lastActionResult ?? "")
                    self.appendEvent(.command, .error, self.errorText ?? "")
                }
            }
        }
    }

    func writeDefaultCapability(_ definition: CapabilityEntry) {
        let displayName = localizedCapabilityActionTitle(definition)
        runSessionCommand(label: "\(DemoStrings.tr("write_default")) \(displayName)") { session in
            let value = try Self.defaultCapabilityValue(for: definition)
            let response = try session.writeCapabilityValue(definition.entryKey, value: value)
            let valueSummary = Self.settingValueSummary(value, definition: definition)
            let summary = "\(DemoStrings.tr("write_default")): \(valueSummary) / \(Self.commandResponseSummary(response))"
            return SessionCommandResult(
                summary: "\(displayName): \(summary)",
                settingValue: (
                    definition.entryKey,
                    valueSummary,
                    Self.settingDraftText(value, definition: definition)
                )
            )
        }
    }

    func writeCapability(
        _ definition: CapabilityEntry,
        value: CapabilityValue,
        persist: Bool,
        valueSummary: String
    ) {
        let actionTitle = definition.valueKind == .action
            ? DemoStrings.tr("execute", fallback: "Execute")
            : DemoStrings.tr("write", fallback: "Write")
        let displayName = localizedCapabilityActionTitle(definition)
        runSessionCommand(label: "\(actionTitle) \(displayName)") { session in
            let response = try session.writeCapabilityValue(definition.entryKey, value: value, persist: persist)
            let persistSummary = definition.valueKind == .action ? "" : " / persist=\(supportFlag(persist))"
            let summary = "\(actionTitle): \(valueSummary)\(persistSummary) / \(Self.commandResponseSummary(response))"
            return SessionCommandResult(
                summary: "\(displayName): \(summary)",
                settingValue: (
                    definition.entryKey,
                    valueSummary,
                    Self.settingDraftText(value, definition: definition)
                )
            )
        }
    }

    func executeCapabilityAction(_ definition: CapabilityEntry) {
        executeCapabilityAction(definition, inputValue: nil)
    }

    func executeCapabilityAction(_ definition: CapabilityEntry, inputValue: String?) {
        let actionTitle = localizedCapabilityActionTitle(definition)
        guard !definition.requiresValue else {
            let payload = inputValue?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            guard !payload.isEmpty else {
                lastActionResult = DemoStrings.tr(
                    "device_action_value_required",
                    fallback: "This device action requires an input value before execution."
                )
                appendEvent(.command, .warn, lastActionResult ?? "")
                return
            }
            runSessionCommand(label: actionTitle) { session in
                let response = try session.executeCapabilityAction(definition.entryKey, value: Data(payload.utf8))
                return SessionCommandResult(
                    summary: "\(actionTitle) value=\(payload) / \(Self.commandResponseSummary(response))"
                )
            }
            return
        }
        runSessionCommand(label: actionTitle) { session in
            let response = try session.executeCapabilityAction(definition.entryKey)
            return SessionCommandResult(
                summary: "\(actionTitle) / \(Self.commandResponseSummary(response))"
            )
        }
    }

    func canWriteDefaultCapability(_ definition: CapabilityEntry) -> Bool {
        definition.supportsWrite && Self.makeDefaultCapabilityValue(for: definition) != nil
    }

    func setScanTextCharset(_ charset: ScanTextCharset) {
        localCharset = charset
        session?.setScanTextCharset(charset)
        appendEvent(.ui, .info, "\(DemoStrings.tr("local_scan_charset")): \(charset.displayName)")
    }

    func setScanTextTerminator(_ bytes: Data) {
        localTerminator = bytes
        do {
            try session?.setScanTextTerminator(bytes)
        } catch {
            appendEvent(.ui, .warn, "\(DemoStrings.tr("local_scan_terminator")): \(DemoErrorFormatter.detail(error))")
        }
    }

    func readDeviceCharsetSetting() {
        guard let definition = capabilityEntries.first(where: { $0.entryKey == "setting.DeviceCharset" }) else {
            lastActionResult = DemoStrings.tr("setting_read_not_supported")
            appendEvent(.command, .warn, lastActionResult ?? "")
            return
        }
        readCapability(definition)
    }

    func clearLogs() {
        events.removeAll()
        Self.cachedEvents = []
    }

    func appendEvent(_ source: ConsoleEventSource, _ level: ConsoleEventLevel, _ message: String) {
        let event = ConsoleEvent(source: source, level: level, message: redactDemoLogMessage(message))
        events.append(event)
        Self.cachedEvents = events
    }

    func makeLogExport(title: String, scopeSummaryLines: [String], events: [ConsoleEvent]) -> String {
        let body = events.map(formatLogLine).joined(separator: "\n")
        return ([
            title,
            "\(DemoStrings.tr("exported_at")): \(logDateFormatter.string(from: Date()))",
            "\(DemoStrings.tr("status")): \(statusSummary)",
            "\(DemoStrings.tr("device")): \(deviceSummary)",
            selectedModelSummary,
            "\(DemoStrings.tr("capability_summary")): \(capabilitySummary)",
            "\(DemoStrings.tr("battery")): \(batterySummary)",
            "\(DemoStrings.tr("local_scan_charset")): \(localCharsetSummary)",
            "\(DemoStrings.tr("local_scan_terminator")): \(localTerminatorSummary)",
        ] + scopeSummaryLines + ["", body])
            .map(redactDemoLogMessage)
            .joined(separator: "\n")
    }

    func makeCompatibilityRecordExport() -> String {
        [
            DemoStrings.tr("compatibility_record"),
            "\(DemoStrings.tr("exported_at")): \(logDateFormatter.string(from: Date()))",
            "",
            DemoCompatibilityRecord.format(state: diagnosticsStore.snapshot()),
        ].joined(separator: "\n")
    }

#if DEBUG
    func enableFakeMode() {
        isInitialized = true
        devices = [
            DiscoveredDevice(
                deviceId: "FAKE-BLE-\(selectedModelKey)-001",
                name: "Fake BLE Scanner",
                transportType: .bleGatt,
                modelKey: selectedModelKey,
                matchReason: "fake-demo",
                rssi: -48
            )
        ]
        diagnosticsStore.updateFakeMode(true)
        appendEvent(.sdk, .warn, "Fake demo mode enabled")
    }

    func configureConsoleUiTestState() {
        isInitialized = true
        selectedDeviceID = "ui-test-device"
        sessionState = .ready
        lastActionResult = DemoStrings.tr("connected_open_console")
    }

    func configureDiscoveryErrorUiTestState() {
        isInitialized = true
        errorText = DemoStrings.sdk(
            "nsdk.discovery_failure_status.bluetooth_disabled",
            fallback: "Bluetooth disabled"
        )
        recoveryAction = .enableBluetooth
        alertErrorText = nil
    }
#endif

    private func appendBootstrapEvent() {
        let bootstrap = ConsoleEvent(source: .ui, level: .debug, message: "AppViewModel initialized")
        events = [bootstrap]
        Self.cachedEvents = [bootstrap]
    }

    private func bindDiscoveryStreams() {
        discoveryTask?.cancel()
        discoveryTask = Task { [weak self] in
            guard let self else { return }
            for await device in sdk.discoveries {
                if let index = devices.firstIndex(where: { $0.deviceId == device.deviceId }) {
                    devices[index] = device
                } else {
                    devices.append(device)
                    appendEvent(.session, .info, "\(DemoStrings.tr("discovered_device")) \(device.name.ifBlank(DemoStrings.unnamedDevice)) / \(device.deviceId)")
                }
            }
        }
        discoveryFailureTask?.cancel()
        discoveryFailureTask = Task { [weak self] in
            guard let self else { return }
            for await failure in sdk.discoveryFailures {
                isDiscovering = false
                sessionState = .error
                recoveryAction = demoRecoveryAction(for: failure)
                errorText = failure.message
                diagnosticsStore.updateRecentFailure(failure.message)
                appendEvent(.session, .error, failure.message)
            }
        }
    }

    private func bindGlobalStreamsIfNeeded() {
        guard debugTask == nil else { return }
        debugTask = Task { [weak self] in
            guard let self else { return }
            for await message in sdk.debugEvents {
                let event = makeSdkConsoleEvent(message)
                appendEvent(event.source, event.level, event.message)
            }
        }
    }

    private func observe(_ session: ScannerSession) {
        stateTask?.cancel()
        scanTask?.cancel()
        stateTask = Task { [weak self] in
            guard let self else { return }
            for await state in session.state {
                guard self.session?.handle == session.handle else { return }
                sessionState = state
                diagnosticsStore.updateSessionState(state)
                if state == .ready {
                    refreshSessionOperationSupport(using: session)
                    loadCapabilityEntries()
                }
                appendEvent(.session, .info, "\(DemoStrings.tr("session_state")): \(state)")
                if state == .disconnected || state == .error {
                    handleSessionClosed(clearResult: false)
                    return
                }
            }
        }
        scanTask = Task { [weak self] in
            guard let self else { return }
            for await scan in session.scanEvents {
                guard self.session?.handle == session.handle else { return }
                scanCount += 1
                lastScanText = scan.text.ifBlank("(empty scan)")
                lastScanMeta = "type=\(scan.barcodeType) / textBytes=\(scan.textBytes.count) / rawBytes=\(scan.rawBytes.count)"
                lastScanRawHex = hexSummary(scan.rawBytes)
                appendEvent(
                    .scan,
                    .info,
                    "type=\(scan.barcodeType) charset=\(session.getScanTextCharset().displayName) " +
                        "textBytesLength=\(scan.textBytes.count) rawBytesLength=\(scan.rawBytes.count)"
                )
            }
        }
    }

    private func handleSessionClosed(clearResult: Bool = true) {
        stateTask?.cancel()
        scanTask?.cancel()
        stateTask = nil
        scanTask = nil
        session = nil
        sessionOperationSupport = nil
        selectedDeviceID = nil
        sessionState = .disconnected
        diagnosticsStore.updateSessionState(nil)
        diagnosticsStore.updateResolvedModel("")
        if clearResult {
            lastActionResult = DemoStrings.tr("device_connection_closed")
        }
    }

    private func refreshSessionOperationSupport(using session: ScannerSession? = nil) {
        let activeSession = session ?? self.session
        guard let activeSession, activeSession.latestState == .ready else {
            sessionOperationSupport = nil
            return
        }
        do {
            sessionOperationSupport = try activeSession.getOperationSupport()
        } catch {
            sessionOperationSupport = nil
            appendEvent(
                .sdk,
                .warn,
                "\(DemoStrings.tr("operation_support", fallback: "Operation Support")): \(DemoErrorFormatter.detail(error))"
            )
        }
    }

    private func loadCapabilityEntries() {
        do {
            if let session, session.latestState == .ready {
                capabilityEntries = try session.getCapabilityEntries()
            } else {
                capabilityEntries = try sdk.getCapabilityEntries(modelKey: activeModelKey, transport: .bleGatt)
            }
        } catch {
            capabilityEntries = []
            appendEvent(.sdk, .warn, "loadCapabilityEntries: \(DemoErrorFormatter.detail(error))")
        }
    }

    private func runSessionCommand(
        label: String,
        operation: @escaping @Sendable (ScannerSession) throws -> SessionCommandResult
    ) {
        guard let session, session.latestState == .ready else {
            lastActionResult = DemoStrings.tr("connect_ready_session_before_command", fallback: "Connect a ready scanner session before running this command.")
            appendEvent(.command, .warn, lastActionResult ?? "")
            return
        }
        guard !isExecuting else { return }
        isExecuting = true
        errorText = nil
        lastActionResult = "\(DemoStrings.tr("command_executing", fallback: "Executing")): \(label)"
        let observedHandle = session.handle
        Task(priority: .userInitiated) { [weak self, session] in
            let result = Result { try operation(session) }
            guard let self else { return }
            self.isExecuting = false
            guard self.session?.handle == observedHandle else { return }
            switch result {
            case .success(let commandResult):
                if let deviceInfoText = commandResult.deviceInfoText {
                    self.deviceInfoText = deviceInfoText
                }
                if let batteryInfoText = commandResult.batteryInfoText {
                    self.batteryInfoText = batteryInfoText
                }
                if let settingValue = commandResult.settingValue {
                    self.latestCapabilityValues[settingValue.key] = settingValue.summary
                    self.latestCapabilityDraftValues[settingValue.key] = settingValue.draftValue
                }
                self.lastActionResult = commandResult.summary
                self.appendEvent(.command, .info, "\(label): \(commandResult.summary)")
            case .failure(let error):
                self.errorText = "\(label): \(DemoErrorFormatter.detail(error))"
                self.lastActionResult = DemoStrings.tr("command_failed", fallback: "Command failed")
                self.appendEvent(.command, .error, self.errorText ?? "")
            }
        }
    }

    nonisolated private static func commandResponseSummary(_ response: CommandResponse) -> String {
        let text = response.text.ifBlank(DemoStrings.tr("command_response_empty"))
        let ack = response.acknowledged ? DemoStrings.tr("command_response_ack") : DemoStrings.tr("command_response_no_ack")
        let raw = response.rawHex.ifBlank(DemoStrings.tr("none"))
        let records = response.recordCount > 0 ? " / \(DemoStrings.tr("command_response_records"))=\(response.recordCount)" : ""
        return "\(ack) / \(text) / raw=\(raw)\(records)"
    }

    nonisolated private static func settingValueSummary(_ value: CapabilityValue, definition: CapabilityEntry? = nil) -> String {
        let kind = definition?.valueKind ?? value.kind
        switch kind {
        case .boolean:
            let fallback = (value.bytes.first ?? 0x00) != 0x00
            return supportFlag(value.booleanValue ?? fallback)
        case .enumeration:
            if let definition, let option = enumOptionLabel(for: definition, bytes: value.bytes) {
                return option
            }
            return hexSummary(value.bytes).ifBlank(DemoStrings.tr("command_response_empty"))
        case .uint8, .uint16:
            let number = decodeUnsignedValue(value.bytes)
            return "\(number) (\(hexSummary(value.bytes)))"
        case .bytesAscii:
            return value.textValue ?? String(decoding: value.bytes, as: UTF8.self)
        case .action:
            return DemoStrings.tr("setting_action_write_only", fallback: "Action")
        default:
            return hexSummary(value.bytes).ifBlank(DemoStrings.tr("command_response_empty"))
        }
    }

    nonisolated private static func settingDraftText(
        _ value: CapabilityValue,
        definition: CapabilityEntry
    ) -> String {
        switch definition.valueKind {
        case .boolean:
            let fallback = (value.bytes.first ?? 0x00) != 0x00
            return (value.booleanValue ?? fallback) ? "true" : "false"
        case .enumeration:
            return definition.options.first {
                parseCapabilityOptionBytes($0.value) == value.bytes
            }?.value ?? hexSummary(value.bytes)
        case .uint8, .uint16:
            return String(decodeUnsignedValue(value.bytes))
        case .bytesAscii:
            return value.textValue ?? String(decoding: value.bytes, as: UTF8.self)
        case .action:
            return ""
        default:
            return hexSummary(value.bytes)
        }
    }

    nonisolated private static func defaultCapabilityValue(for definition: CapabilityEntry) throws -> CapabilityValue {
        guard let value = makeDefaultCapabilityValue(for: definition) else {
            throw ScannerError(code: -1, operation: "writeDefaultCapability unsupported default entry=\(definition.entryKey)")
        }
        return value
    }

    nonisolated private static func makeDefaultCapabilityValue(for definition: CapabilityEntry) -> CapabilityValue? {
        let raw = definition.defaultValue.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !raw.isEmpty else { return nil }
        switch definition.valueKind {
        case .boolean:
            guard let byte = parseByte(raw) else { return nil }
            return .boolean(byte != 0x00)
        case .enumeration, .uint8:
            guard let byte = parseByte(raw) else { return nil }
            return .bytes(definition.valueKind, Data([byte]))
        case .uint16:
            guard let value = parseUInt16(raw) else { return nil }
            return .bytes(definition.valueKind, Data([UInt8(value >> 8), UInt8(value & 0x00FF)]))
        case .bytesAscii:
            return .asciiText(raw)
        default:
            return nil
        }
    }

    nonisolated private static func parseByte(_ raw: String) -> UInt8? {
        guard let value = parseInteger(raw), value >= 0, value <= 0xFF else { return nil }
        return UInt8(value)
    }

    nonisolated private static func parseUInt16(_ raw: String) -> UInt16? {
        guard let value = parseInteger(raw), value >= 0, value <= 0xFFFF else { return nil }
        return UInt16(value)
    }

    nonisolated private static func parseInteger(_ raw: String) -> Int? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.lowercased().hasPrefix("0x") {
            return Int(trimmed.dropFirst(2), radix: 16)
        }
        if trimmed.range(of: #"^[0-9A-Fa-f]{2,4}$"#, options: .regularExpression) != nil,
           trimmed.rangeOfCharacter(from: CharacterSet.letters) != nil {
            return Int(trimmed, radix: 16)
        }
        return Int(trimmed)
    }

    nonisolated private static func decodeUnsignedValue(_ bytes: Data) -> Int {
        bytes.reduce(0) { partialResult, byte in
            (partialResult << 8) | Int(byte)
        }
    }

    nonisolated private static func enumOptionLabel(for definition: CapabilityEntry, bytes: Data) -> String? {
        let hex = hexSummary(bytes)
        return definition.options.first { option in
            parseCapabilityOptionBytes(option.value) == bytes || option.value == hex
        }?.value
    }
}
