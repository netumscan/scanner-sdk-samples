import Foundation
import ScannerSDK

@MainActor
final class AppViewModel: ObservableObject {
    private static var cachedEvents: [ConsoleEvent] = []

    var isApplyingLocalizedFeedback = false
    var lastActionResultProvider: (() -> String)?
    var errorTextProvider: (() -> String)?

    @Published var isInitialized = false
    @Published var isDiscovering = false
    @Published var isExecuting = false
    @Published var isConnecting = false
    @Published var devices: [DiscoveredDevice] = []
    @Published var selectedDeviceID: String?
    @Published var selectedModelId: DeviceModelId = .cs7501
    @Published var sessionStateText = "idle"
    @Published private var statusSummarySource: DemoStatusSummarySource = .notConnected
    @Published private var deviceSummarySource: DemoDeviceSummarySource = .noneSelected
    @Published private var infoSummarySource: DemoInfoSummarySource = .notLoaded
    @Published private var sdkResolvedModelSummarySource: DemoSdkResolvedModelSummarySource = .notLoaded
    @Published private var capabilitySummarySource: DemoCapabilitySummarySource = .notLoaded
    @Published private var moduleSummarySource: DemoModuleSummarySource = .notLoaded
    @Published var currentModuleFamily: ModuleFamily = .unknown
    @Published var supportsModuleCommands = false
    @Published var canExecuteModuleCommands = false
    @Published var consoleModuleCatalogExpanded = true
    @Published var consoleNtc06hDomainIndex = 0
    @Published var consoleNtc06hExpandedFamilyKeys: Set<String> = []
    @Published var consoleInlineNtc06hSettingKey: String?
    @Published var consoleNtc06hCustomCode = ""
    @Published var consoleNtc06hTemplateValue = ""
    @Published var consoleNtc06hSaveAfterWrite = true
    @Published private var batterySummarySource: DemoBatterySummarySource = .notLoaded
    @Published private var deviceCharsetSummarySource: DemoDeviceConfigSummarySource = .notReadCharsetQuery
    @Published private var deviceTerminalSummarySource: DemoDeviceConfigSummarySource = .notSetSessionCacheEmpty
    @Published var localCharset: ScanTextCharset = .utf8
    @Published var localTerminator = Data([0x0D])
    @Published var selectedChannelKind: ProtocolChannelKind = .scannerMaster
    @Published var lastActionResult: String? {
        didSet {
            if !isApplyingLocalizedFeedback {
                lastActionResultProvider = nil
            }
        }
    }
    @Published var errorText: String? {
        didSet {
            if !isApplyingLocalizedFeedback {
                errorTextProvider = nil
            }
        }
    }
    @Published var recoveryAction: DemoRecoveryAction?
    @Published var alertErrorText: String?
    @Published var events: [ConsoleEvent]

    var discoveryCoordinator = DiscoveryCoordinator()
    let diagnosticsStore = DemoDiagnosticsStore()
    lazy var commandRunner = SessionCommandRunner(
        setExecuting: { [weak self] executing in
            self?.isExecuting = executing
        },
        onBusy: { [weak self] in
            self?.reportBusyCommand()
        }
    )
    lazy var moduleCommandRunner = ModuleCommandRunner(
        canExecuteModuleCommands: { [weak self] in
            self?.canExecuteModuleCommands ?? false
        },
        notReadyReason: {
            DemoStrings.tr("module_command_not_ready_reason")
        },
        onNotReady: { [weak self] title, reason in
            guard let self else { return }
            self.setLocalizedErrorText {
                "\(title): \(DemoStrings.tr("module_command_not_ready_reason"))"
            }
            self.appendEvent(.command, .warn, "\(title): \(reason)")
        }
    )
    var session: ScannerSession?
    var sessionHandle: DemoSessionHandle?
    var connectedModelId: DeviceModelId?
    private var discoveryTask: Task<Void, Never>?
    var stateTask: Task<Void, Never>?
    var scanTask: Task<Void, Never>?
    var sdkDebugTask: Task<Void, Never>?
    var discoveryFailureTask: Task<Void, Never>?
    var sessionFailureTask: Task<Void, Never>?
    var autoInfoRequested = false
    var globalStreamsBound = false

    init() {
        events = Self.cachedEvents
        if events.isEmpty {
            let bootstrap = ConsoleEvent(source: .ui, level: .debug, message: "AppViewModel initialized")
            events = [bootstrap]
            Self.cachedEvents = [bootstrap]
        }
        applyPreferredCapabilitySummary()
        diagnosticsStore.updateSelectedModel(selectedModelId)
    }

    deinit {
        discoveryTask?.cancel()
        stateTask?.cancel()
        scanTask?.cancel()
        sdkDebugTask?.cancel()
        discoveryFailureTask?.cancel()
        sessionFailureTask?.cancel()
    }

    var selectedModelSummary: String { formatSelectedModelSummary(activeModelId) }
    var statusSummary: String { statusSummarySource.text }
    var deviceSummary: String { deviceSummarySource.text }
    var infoSummary: String { infoSummarySource.text }
    var sdkResolvedModelSummary: String { sdkResolvedModelSummarySource.text }
    var capabilitySummary: String { capabilitySummarySource.text }
    var moduleSummary: String { moduleSummarySource.text }
    var batterySummary: String { batterySummarySource.text }
    var deviceCharsetSummary: String { deviceCharsetSummarySource.text }
    var deviceTerminalSummary: String { deviceTerminalSummarySource.text }
    var diagnosticsSummary: String { diagnosticsStore.summaryText() }
    var canInitiateConnection: Bool { !isConnecting && (sessionHandle == nil || latestSessionState == .disconnected) }
    var hasActiveSession: Bool { sessionHandle != nil && (latestSessionState == .connected || latestSessionState == .ready) }
    var hasReadySession: Bool { sessionHandle != nil && latestSessionState == .ready }
    var canExecuteMasterCommands: Bool { hasReadySession && !isExecuting }
    var canChangeDiscoveryTarget: Bool { !isConnecting && !hasActiveSession }

    func onAppear() {
        diagnosticsStore.refreshPlatform()
        initializeSdk()
    }

#if DEBUG
    func enableFakeMode() {
        guard !isInitialized, !hasActiveSession else { return }
        discoveryCoordinator = DiscoveryCoordinator(backend: FakeDemoDiscoveryBackend())
        diagnosticsStore.updateFakeMode(true)
        appendEvent(.sdk, .warn, "Fake demo mode enabled")
    }

    func configureConsoleUiTestState() {
        isInitialized = true
        selectedModelId = .nt91
        connectedModelId = .nt91
        selectedDeviceID = "ui-test-device"
        deviceSummarySource = .device(name: "UI Test Device", deviceId: "ui-test-device")
        sessionStateText = "ready"
        statusSummarySource = .session(.ready)
        capabilitySummarySource = .loaded
        moduleSummarySource = .value("family=\(displayModuleFamilyLabel(.ntc06h)) / moduleCommands=YES / passthrough=YES")
        currentModuleFamily = .ntc06h
        supportsModuleCommands = true
        canExecuteModuleCommands = true
        consoleModuleCatalogExpanded = true
        consoleNtc06hDomainIndex = 0
        if let firstFamily = DemoModuleSettingsCatalog.ntc06hDomainGroups().first?.families.first {
            consoleNtc06hExpandedFamilyKeys = [firstFamily.key]
        }
    }

    func configureDiscoveryErrorUiTestState() {
        isInitialized = true
        isDiscovering = false
        statusSummarySource = .sdkInitialized
        setLocalizedErrorText {
            DemoStrings.sdk(
                "nsdk.discovery_failure_status.bluetooth_disabled",
                fallback: "Bluetooth disabled"
            )
        }
        recoveryAction = .enableBluetooth
        alertErrorText = nil
    }
#endif

    func initializeSdk() {
        if isInitialized {
            bindGlobalStreamsIfNeeded()
            setLocalizedLastActionResult {
                DemoStrings.tr("sdk_already_initialized")
            }
            return
        }
        do {
            try discoveryCoordinator.initialize()
            isInitialized = true
            diagnosticsStore.updateSdkInitialized(true)
            diagnosticsStore.updateFakeMode(discoveryCoordinator.isFakeMode)
            diagnosticsStore.refreshPlatform()
            bindGlobalStreamsIfNeeded()
            statusSummarySource = .sdkInitialized
            setLocalizedLastActionResult {
                DemoStrings.tr("sdk_initialization_completed")
            }
            appendEvent(.session, .info, DemoStrings.tr("sdk_initialized"))
        } catch {
            diagnosticsStore.updateSdkInitialized(false)
            let detail = DemoErrorFormatter.detail(error)
            setLocalizedBlockingErrorText {
                "\(DemoStrings.tr("initialize_failed")): \(detail)"
            }
            appendEvent(.session, .error, errorText ?? "")
        }
    }

    func startDiscovery() {
        guard isInitialized else {
            setLocalizedLastActionResult {
                DemoStrings.tr("initialize_sdk_before_discovery")
            }
            appendEvent(.ui, .warn, DemoStrings.tr("initialize_sdk_before_discovery"))
            return
        }
        errorText = nil
        devices.removeAll()
        selectedDeviceID = nil
        diagnosticsStore.updateSelectedModel(selectedModelId)
        diagnosticsStore.updateResolvedModel(.unknown)
        diagnosticsStore.updateSessionState(nil)
        appendEvent(
            .command,
            .debug,
            "\(DemoStrings.tr("start_discovery")): \(selectedModelSummary)"
        )
        discoveryTask?.cancel()
        discoveryTask = Task { [weak self] in
            guard let self else { return }
            for await device in discoveryCoordinator.discoveries {
                if Task.isCancelled { return }
                if let index = self.devices.firstIndex(where: { $0.deviceId == device.deviceId }) {
                    self.devices[index] = device
                } else {
                    self.devices.append(device)
                }
                if self.isPreferredScannerDevice(device) {
                    let discoveredLabel = DemoStrings.tr("discovered_device")
                    let unnamedDevice = DemoStrings.tr("unnamed_device")
                    self.appendEvent(.session, .info, "\(discoveredLabel) \(device.name.ifBlank(unnamedDevice)) / \(device.deviceId)")
                }
            }
        }

        do {
            let result = try discoveryCoordinator.startDiscovery(selectedModelId: selectedModelId)
            devices.append(contentsOf: result.devices)
            isDiscovering = true
            let modelId = selectedModelId
            setLocalizedLastActionResult {
                "\(DemoStrings.tr("started_discovery_for_target_model")): \(displayModelLabel(modelId))"
            }
        } catch {
            discoveryTask?.cancel()
            discoveryTask = nil
            let detail = DemoErrorFormatter.detail(error)
            setLocalizedErrorText {
                "\(DemoStrings.tr("start_discovery_failed")): \(detail)"
            }
            appendEvent(.session, .error, "\(DemoStrings.tr("start_discovery_failed")): \(detail)")
        }
    }

    func stopDiscovery() {
        guard isDiscovering || discoveryTask != nil else { return }
        discoveryTask?.cancel()
        discoveryTask = nil
        isDiscovering = false
        do {
            try discoveryCoordinator.stopDiscovery()
            appendEvent(.command, .debug, DemoStrings.tr("stop_discovery"))
        } catch {
            let detail = DemoErrorFormatter.detail(error)
            setLocalizedErrorText {
                "\(DemoStrings.tr("stop_discovery_failed")): \(detail)"
            }
            appendEvent(.session, .error, "\(DemoStrings.tr("stop_discovery_failed")): \(detail)")
        }
    }

    func connect(_ device: DiscoveredDevice) {
        guard !isConnecting else {
            setLocalizedLastActionResult {
                DemoStrings.tr("connection_already_in_progress")
            }
            return
        }
        guard sessionHandle == nil || latestSessionState == .disconnected else {
            setLocalizedLastActionResult {
                DemoStrings.tr("device_ready_open_console")
            }
            return
        }
        errorText = nil
        isConnecting = true
        selectedDeviceID = device.deviceId
        deviceSummarySource = .device(name: device.name, deviceId: device.deviceId)
        stopDiscovery()
        appendEvent(.command, .debug, "\(DemoStrings.tr("connect_device")) \(device.deviceId)")
        let connectionModelId = resolveConnectionModelId(
            selectedModelId: selectedModelId,
            discoveredModelId: device.modelId
        )
        Task { [weak self] in
            guard let self else { return }
            do {
                let result = try await discoveryCoordinator.connectReady(
                    device,
                    channelKind: selectedChannelKind,
                    selectedModelId: connectionModelId,
                    applyDecoderModule: true
                )
                self.session = result.session
                self.sessionHandle = result.session.map(RealDemoSessionHandle.init) ?? result.fakeSession
                self.connectedModelId = connectionModelId
                self.diagnosticsStore.updateSelectedModel(connectionModelId)
                self.diagnosticsStore.updateSessionState(self.latestSessionState)
                self.diagnosticsStore.updateFakeMode(discoveryCoordinator.isFakeMode)
                result.session?.setScanTextCharset(localCharset)
                autoInfoRequested = false
                appendEvent(.session, .info, "\(DemoStrings.tr("protocol_mode")) \(protocolChannelKindSummary)")
                if let session = result.session {
                    observe(session)
                }
                sessionStateText = String(describing: latestSessionState)
                statusSummarySource = .session(latestSessionState)
                isConnecting = false
                diagnosticsStore.updateSessionState(latestSessionState)
                if let session = result.session, session.latestState == .ready {
                    handleReadySession(session)
                } else if result.fakeSession?.latestState == .ready {
                    setLocalizedLastActionResult { DemoStrings.tr("fake_session_ready") }
                    appendEvent(.session, .warn, DemoStrings.tr("fake_session_ready"))
                }
            } catch {
                isConnecting = false
                let detail = DemoErrorFormatter.detail(error)
                setLocalizedErrorText {
                    "\(DemoStrings.tr("connect_failed")): \(detail)"
                }
                appendEvent(.session, .error, "\(DemoStrings.tr("connect_failed")): \(detail)")
            }
        }
    }

    func disconnect() {
        do {
            try sessionHandle?.disconnect()
            appendEvent(.session, .info, DemoStrings.tr("session_disconnected"))
            handleSessionClosed(
                status: .session(.disconnected),
                lastActionProvider: {
                    DemoStrings.tr("device_disconnected")
                }
            )
        } catch {
            let detail = DemoErrorFormatter.detail(error)
            setLocalizedErrorText {
                "\(DemoStrings.tr("disconnect_failed")): \(detail)"
            }
            appendEvent(.session, .error, "\(DemoStrings.tr("disconnect_failed")): \(detail)")
        }
    }

    func applyLocalCharset(_ charset: ScanTextCharset) {
        localCharset = charset
        if hasReadySession {
            session?.setScanTextCharset(charset)
        }
        setLocalizedLastActionResult {
            "\(DemoStrings.tr("local_scan_text_charset_switched")) \(charset.displayName)"
        }
        appendEvent(.ui, .info, "\(DemoStrings.tr("scan_text_charset_set_to")) \(charset.displayName)")
    }

    func applyLocalTerminator(_ preset: ScanTerminatorPreset) {
        guard let session, hasReadySession else {
            localTerminator = preset.bytes
            setLocalizedLastActionResult {
                "\(DemoStrings.tr("local_scan_terminator_staged_as")) \(preset.summary)"
            }
            appendEvent(.ui, .info, "\(DemoStrings.tr("scan_terminator_staged_as")) \(preset.summary)")
            return
        }
        Task { [weak self] in
            guard let self else { return }
            do {
                try session.setScanTerminator(preset.bytes)
                self.localTerminator = preset.bytes
                self.setLocalizedLastActionResult {
                    "\(DemoStrings.tr("local_scan_terminator_switched")) \(preset.summary)"
                }
                self.appendEvent(.ui, .info, "\(DemoStrings.tr("scan_terminator_set_to")) \(preset.summary)")
            } catch {
                let detail = DemoErrorFormatter.detail(error)
                self.setLocalizedErrorText {
                    "\(DemoStrings.tr("set_scan_terminator_failed")): \(detail)"
                }
                self.appendEvent(.ui, .error, "\(DemoStrings.tr("set_scan_terminator_failed")): \(detail)")
            }
        }
    }

    func clearLogs() {
        events.removeAll()
        Self.cachedEvents.removeAll()
    }

    func refreshLocalizedUi() {
        diagnosticsStore.refreshPlatform()
        statusSummarySource = session.map { .session($0.latestState) } ?? (isInitialized ? .sdkInitialized : .notConnected)
        if let selectedDeviceID,
           let device = devices.first(where: { $0.deviceId == selectedDeviceID }) {
            deviceSummarySource = .device(name: device.name, deviceId: device.deviceId)
        } else {
            deviceSummarySource = .noneSelected
        }
        if let session {
            diagnosticsStore.updateSessionState(session.latestState)
            do {
                try refreshLocalizedCachedState(session)
            } catch {
                resetDisconnectedDeviceState()
            }
        } else if let sessionHandle, sessionHandle.isFake {
            diagnosticsStore.updateSessionState(sessionHandle.latestState)
            statusSummarySource = .session(sessionHandle.latestState)
        } else {
            diagnosticsStore.updateSessionState(nil)
            resetDisconnectedDeviceState()
        }
        refreshLocalizedFeedback()
    }

    func applySelectedModel(_ modelId: DeviceModelId) {
        guard canChangeDiscoveryTarget else {
            setLocalizedLastActionResult {
                DemoStrings.tr("disconnect_before_changing_model")
            }
            appendEvent(.ui, .warn, DemoStrings.tr("disconnect_before_changing_model"))
            return
        }
        selectedModelId = modelId
        selectedChannelKind = resolveProtocolChannelKind(for: preferredCapabilityForSelectedModel())
        diagnosticsStore.updateSelectedModel(modelId)
        diagnosticsStore.updateResolvedModel(.unknown)
        if session == nil || latestSessionState == .disconnected || latestSessionState == .idle {
            applyPreferredCapabilitySummary()
        }
        setLocalizedLastActionResult {
            DemoStrings.tr("test_target_model_updated_protocol")
        }
        appendEvent(
            .ui,
            .info,
            "\(DemoStrings.tr("test_target_model")): \(displayModelLabel(modelId))"
        )
        appendEvent(.ui, .info, "\(DemoStrings.tr("protocol_mode")) \(protocolChannelKindSummary)")
    }

    func appendEvent(_ source: ConsoleEventSource, _ level: ConsoleEventLevel, _ message: String) {
        var nextEvents = events
        nextEvents.append(ConsoleEvent(source: source, level: level, message: message))
        if nextEvents.count > 200 {
            nextEvents.removeFirst(nextEvents.count - 200)
        }
        events = nextEvents
        Self.cachedEvents = nextEvents
    }

    func applyStatusSummary(_ source: DemoStatusSummarySource) {
        statusSummarySource = source
    }

    func applyDeviceSummary(_ source: DemoDeviceSummarySource) {
        deviceSummarySource = source
    }

    func applyInfoSummary(_ source: DemoInfoSummarySource) {
        infoSummarySource = source
    }

    func applySdkResolvedModelSummary(_ source: DemoSdkResolvedModelSummarySource) {
        sdkResolvedModelSummarySource = source
    }

    func applyCapabilitySummary(_ source: DemoCapabilitySummarySource) {
        capabilitySummarySource = source
    }

    func applyModuleSummary(_ source: DemoModuleSummarySource) {
        moduleSummarySource = source
    }

    func applyBatterySummary(_ source: DemoBatterySummarySource) {
        batterySummarySource = source
    }

    func applyDeviceCharsetSummary(_ source: DemoDeviceConfigSummarySource) {
        deviceCharsetSummarySource = source
    }

    func applyDeviceTerminalSummary(_ source: DemoDeviceConfigSummarySource) {
        deviceTerminalSummarySource = source
    }

    private var latestSessionState: SessionState {
        sessionHandle?.latestState ?? .idle
    }

    var activeModelId: DeviceModelId {
        connectedModelId ?? selectedModelId
    }

    func applySessionFailure(_ failure: SessionFailure) {
        guard session != nil, failure.deviceId == selectedDeviceID else { return }
        let label = failure.code.localizedStatusLabel
        statusSummarySource = .sdkLabel(localizationKey: label.localizationKey, fallback: label.fallbackDisplayName)
        setLocalizedLastActionResult {
            DemoStrings.tr("device_connection_interrupted")
        }
        setLocalizedErrorText {
            self.sessionFailureDetail(failure)
        }
        diagnosticsStore.updateRecentFailure("\(DemoStrings.tr("global_session_failure")): \(sessionFailureDetail(failure))")
        recoveryAction = demoRecoveryAction(for: failure)
    }

}
