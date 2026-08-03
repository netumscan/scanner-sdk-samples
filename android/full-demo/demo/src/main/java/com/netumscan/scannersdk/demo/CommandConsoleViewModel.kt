package com.netumscan.scannersdk.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netumscan.scannersdk.ScannerSdk
import com.netumscan.scannersdk.ScannerSession
import com.netumscan.scannersdk.SessionState
import com.netumscan.scannersdk.TransportType
import com.netumscan.scannersdk.localizedLabel
import com.netumscan.scannersdk.model.TransportIssue
import com.netumscan.scannersdk.model.CapabilityDomain
import com.netumscan.scannersdk.model.CapabilityEntry
import com.netumscan.scannersdk.model.CapabilityEntryKind
import com.netumscan.scannersdk.model.CapabilityLabel
import com.netumscan.scannersdk.model.CapabilityLabelKind
import com.netumscan.scannersdk.model.CommandResponse
import com.netumscan.scannersdk.model.DataRule
import com.netumscan.scannersdk.model.DataRuleKind
import com.netumscan.scannersdk.model.DiscoveredDevice
import com.netumscan.scannersdk.model.DeviceCapabilitySummary
import com.netumscan.scannersdk.model.ScanTextCharset
import com.netumscan.scannersdk.model.ScannerInfo
import com.netumscan.scannersdk.model.ScanEvent
import com.netumscan.scannersdk.model.SessionFailure
import com.netumscan.scannersdk.model.SessionInitializationStage
import com.netumscan.scannersdk.model.SessionInitializationStageEvent
import com.netumscan.scannersdk.model.CapabilityValue
import com.netumscan.scannersdk.model.DeviceSupportStatus
import com.netumscan.scannersdk.model.localizedLabel as localizedSdkLabel
import com.netumscan.scannersdk.model.localizedStatusLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

internal fun formatCapabilitySummary(capability: DeviceCapabilitySummary): UiText =
    capability.toCapabilitySummaryUi().asText()

internal class CommandConsoleViewModel : ViewModel() {
    private companion object {
        const val AUTO_INFO_REQUEST_DELAY_MS = 250L
        const val BT_FIRMWARE_VERSION_SETTING_KEY = "BluetoothFirmwareVersion"
    }

    private val _uiState = MutableStateFlow(CommandConsoleUiState())
    val uiState: StateFlow<CommandConsoleUiState> = _uiState.asStateFlow()
    private val _closePageEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val closePageEvents: SharedFlow<Unit> = _closePageEvents.asSharedFlow()

    private var observing = false
    private var closeHandled = false
    private var boundDevice: DiscoveredDevice? = null
    private var boundSession: ScannerSession? = null
    private var boundSessionHandle: DemoSessionHandle? = null
    private var sessionObservationJobs: List<Job> = emptyList()
    private var sdkDebugJob: Job? = null
    private var autoInfoRequested = false
    private var pendingSessionFailure: SessionFailure? = null
    private var lastObservedSessionState: SessionState? = null
    private val settingHighlightJobs = mutableMapOf<String, Job>()
    private var lastActionResultProvider: (() -> String)? = null
    private var errorMessageProvider: (() -> String)? = null
    private var previewLoadJob: Job? = null
    private var capabilityCatalogCacheKey: String? = null
    private var capabilityCatalogRequestedKey: String? = null
    private val commandExecutionMutex = Mutex()
    private val commandRunner = SessionCommandRunner(
        scope = viewModelScope,
        mutex = commandExecutionMutex,
        setExecuting = { executing -> updateUiState { copy(isExecuting = executing) } },
        onBusy = ::reportBusyCommand,
    )

    fun refreshLocalizedUi() {
        val device = boundDevice
        val session = boundSession
        val latestState = session?.state?.value
        val scanCharset = session?.getScanTextCharset() ?: ScanTextCharset.UTF_8
        val scanTerminator = session?.getScanTextTerminator()?.toHexSummary() ?: "0D"
        val selectedModelKey = preferredModelKey()
        DemoDiagnosticsStore.updateSelectedModel(selectedModelKey)
        DemoDiagnosticsStore.updateSessionState(latestState)
        updateUiState {
            copy(
                statusSummary = latestState?.let(::statusText) ?: uiText(R.string.not_connected),
                deviceSummary = if (device == null) {
                    uiText(R.string.no_selected_device)
                } else {
                    rawDisplayText("${device.name.ifBlank { DemoStrings.unknownDeviceName }} / ${device.deviceId}")
                },
                selectedModelSummary = formatSelectedModelSummaryText(selectedModelKey),
                capabilitySummary = publicCapabilitySummary?.let(::formatCapabilitySummary) ?: capabilitySummary,
                scanCharsetSummary = scanCharset.displayName,
                scanTerminatorSummary = scanTerminator,
                hasReadySession = latestState == SessionState.READY,
                diagnosticsSummary = DemoDiagnosticsStore.summaryText(),
                lastActionResult = lastActionResultProvider?.let(::dynamicText) ?: lastActionResult,
                errorMessage = errorMessageProvider?.let(::dynamicText) ?: errorMessage,
            )
        }
        schedulePreviewLoad(selectedModelKey)
        if (session == null) {
            updateUiState {
                copy(
                    infoSummary = uiText(R.string.device_info_not_loaded),
                    sdkResolvedModelSummary = uiText(R.string.sdk_resolved_model_not_loaded),
                    initializationSummary = uiText(R.string.session_initialization_idle),
                    deviceCharsetSummary = uiText(R.string.device_config_not_auto_queried),
                    deviceTerminalSummary = uiText(R.string.device_terminator_not_auto_queried),
                    bluetoothFirmwareVersionSummary = uiText(R.string.setting_value_not_loaded),
                )
            }
            return
        }
        viewModelScope.launch {
            refreshLocalizedCachedState(session)
        }
    }

    fun initialize() {
        closeHandled = false
        autoInfoRequested = false
        pendingSessionFailure = null
        lastObservedSessionState = null
        boundDevice = DemoSessionCoordinator.selectedDevice
        boundSession = DemoSessionCoordinator.activeSession
        boundSessionHandle = DemoSessionCoordinator.activeSessionHandle
        val preferredModelKey = DemoSessionCoordinator.preferredModelKey
        val device = boundDevice
        val session = boundSession
        val scanCharset = session?.getScanTextCharset() ?: ScanTextCharset.UTF_8
        val scanTerminator = session?.getScanTextTerminator()?.toHexSummary() ?: "0D"
        DemoDiagnosticsStore.updateSelectedModel(preferredModelKey)
        DemoDiagnosticsStore.updateSessionState(session?.state?.value)
        updateUiState {
            copy(
                deviceSummary = if (device == null) {
                    uiText(R.string.no_selected_device)
                } else {
                    rawDisplayText("${device.name.ifBlank { DemoStrings.unknownDeviceName }} / ${device.deviceId}")
                },
                selectedModelSummary = formatSelectedModelSummaryText(preferredModelKey),
                sdkResolvedModelSummary = uiText(R.string.sdk_resolved_model_not_loaded),
                capabilitySummary = uiText(R.string.capability_summary_not_loaded),
                actionCapabilityEntries = emptyList(),
                initializationSummary = uiText(R.string.session_initialization_waiting_to_start),
                publicCapabilitySummary = null,
                capabilityDomains = emptyList(),
                capabilityEntries = emptyList(),
                capabilityCatalog = emptyList(),
                isLoadingCapabilityCatalog = preferredModelKey.isNotBlank(),
                capabilityCatalogError = null,
                settingDefinitions = emptyList(),
                settingOptionsByKey = emptyMap(),
                settingGroups = emptyList(),
                settingsReadSupported = false,
                settingsWriteSupported = false,
                deviceCharsetSummary = uiText(R.string.device_config_not_auto_queried),
                deviceTerminalSummary = uiText(R.string.device_terminator_not_auto_queried),
                bluetoothFirmwareVersionSummary = uiText(R.string.setting_value_not_loaded),
                scanCharsetSummary = scanCharset.displayName,
                scanTerminatorSummary = scanTerminator,
                hasReadySession = session?.state?.value == SessionState.READY,
                diagnosticsSummary = DemoDiagnosticsStore.summaryText(),
                lastActionResult = if (device == null) {
                    null
                } else {
                    localizedAction {
                        "${DemoStrings.text(R.string.console_loaded_for)} ${device.name.ifBlank { device.deviceId }}"
                    }
                }
            )
        }
        if (session == null && boundSessionHandle?.isFake == true) {
            applyFakeSessionState(preferredModelKey)
            return
        }
        startObserving()
        schedulePreviewLoad(preferredModelKey)
    }

    fun requestInfo() {
        launchSerializedCommand {
            val session = requireBoundSession() ?: run {
                appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, DemoStrings.text(R.string.no_active_session))
                return@launchSerializedCommand
            }
            if (!ensureOperationSupport(
                    session = session,
                    operation = DemoSessionOperation.INITIALIZE_SESSION,
                    labelProvider = { DemoStrings.text(R.string.load_device_info_action) }
                )
            ) {
                return@launchSerializedCommand
            }
            DemoDiagnosticsStore.updateRecentSessionInitStage(null)
            appendCommandSend("initializeSession", DemoStrings.text(R.string.send_basic_command))
            updateUiState { copy(isLoadingSettings = true) }
            runCatching {
                withContext(Dispatchers.IO) {
                    val initialization = session.initialize()
                    val definitions = loadSettingEntriesFor(session)
                    DeviceSnapshot(
                        info = initialization.info,
                        batteryInfo = initialization.batteryInfo,
                        modelConfigApplied = initialization.modelConfigApplied,
                        resolvedModel = session.getResolvedModelKey(),
                        capability = session.getDeviceCapabilitySummary(),
                        settingDefinitions = definitions,
                        settingOptionsByKey = loadSettingOptionsFor(session, definitions),
                    )
                }
            }.onSuccess { snapshot ->
                applyDeviceState(snapshot)
                updateUiState {
                    copy(
                        lastActionResult = localizedAction {
                            DemoStrings.text(R.string.device_info_loaded)
                        },
                        errorMessage = null,
                        isLoadingSettings = false,
                    )
                }
                val selectedModelKey = preferredModelKey()
                if (snapshot.modelConfigApplied) {
                    appendEvent(
                        DebugEventSource.COMMAND,
                        DebugEventLevel.Info,
                        DemoStrings.format(R.string.model_config_applied_after_refresh, displayModelLabel(selectedModelKey))
                    )
                }
                appendEvent(
                    DebugEventSource.COMMAND,
                    DebugEventLevel.Debug,
                    formatSdkResolvedModelSummary(selectedModelKey, snapshot.resolvedModel)
                )
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Info, "${DemoStrings.text(R.string.info)} firmware=${snapshot.info.firmwareVersion} hardware=${snapshot.info.hardwareVersion} serial=${snapshot.info.serialNumber}")
                appendEvent(
                    DebugEventSource.COMMAND,
                    DebugEventLevel.Debug,
                    "${DemoStrings.text(R.string.version_parsed)} family=${snapshot.info.versionFormatFamily} boot=${snapshot.info.versionBootCode} series=${snapshot.info.versionSeriesCode} transport=${snapshot.info.versionTransportCode}${snapshot.info.versionTransportSuffix} wireless=${snapshot.info.versionWirelessCode} bt=${snapshot.info.versionBluetoothCode} chip=${snapshot.info.versionChipsetCode}${snapshot.info.versionChipsetSuffix}/${snapshot.info.hardwareVersion} release=${snapshot.info.versionReleaseCode} ext=${snapshot.info.versionExtensionCode}"
                )
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Info, "${DemoStrings.text(R.string.battery)} raw=${snapshot.batteryInfo.rawText}")
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Info, "${DemoStrings.text(R.string.battery_parsed)} voltage=${snapshot.batteryInfo.voltageText} percent=${snapshot.batteryInfo.percent}")
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Info, "${DemoStrings.text(R.string.capability)} ${snapshot.capability.displaySummary}")
            }.onFailure { error ->
                updateUiState { copy(isLoadingSettings = false) }
                reportError({ DemoStrings.text(R.string.failed_to_load_device_info) }, error)
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Error, "${DemoStrings.text(R.string.refresh_info_failed)}: ${error.demoErrorDetail()}")
            }
        }
    }

    fun requestBatteryLevel() {
        launchSerializedCommand {
            val session = requireBoundSession() ?: run {
                appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, DemoStrings.text(R.string.no_active_session))
                return@launchSerializedCommand
            }
            if (!ensureOperationSupport(
                    session = session,
                    operation = DemoSessionOperation.GET_BATTERY_INFO,
                    labelProvider = { DemoStrings.text(R.string.load_battery_info_action) }
                )
            ) {
                return@launchSerializedCommand
            }
            appendCommandSend("getBatteryInfo", DemoStrings.text(R.string.send_basic_command))
            runCatching {
                withContext(Dispatchers.IO) { session.getBatteryInfo() }
            }.onSuccess { batteryInfo ->
                reportAction {
                    DemoStrings.text(R.string.battery_info_loaded)
                }
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Info, "${DemoStrings.text(R.string.battery)} raw=${batteryInfo.rawText}")
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Info, "${DemoStrings.text(R.string.battery_parsed)} voltage=${batteryInfo.voltageText} percent=${batteryInfo.percent}")
            }.onFailure { error ->
                reportError({ DemoStrings.text(R.string.failed_to_load_battery_info) }, error)
            }
        }
    }

    fun requestStorageUsage() {
        runAction(
            actionLabelProvider = { DemoStrings.text(R.string.load_memory_usage_action) },
            successLogProvider = { DemoStrings.text(R.string.memory_usage_loaded) },
            commandText = "getStorageUsage",
        ) { session ->
            val usage = session.getStorageUsage()
            appendEvent(
                DebugEventSource.COMMAND,
                DebugEventLevel.Info,
                "${DemoStrings.text(R.string.memory_usage)} count=${usage.barcodeCount} used=${usage.used}/${usage.capacity} remaining=${usage.remaining} raw=${usage.rawText}"
            )
        }
    }

    fun readBluetoothFirmwareVersion() {
        launchSerializedCommand {
            val session = requireBoundSession() ?: run {
                appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, DemoStrings.text(R.string.no_active_session))
                return@launchSerializedCommand
            }
            val definition = _uiState.value.settingDefinitions.firstOrNull {
                it.entryKey == BT_FIRMWARE_VERSION_SETTING_KEY
            } ?: withContext(Dispatchers.IO) {
                session.findCapabilityEntry(BT_FIRMWARE_VERSION_SETTING_KEY)
            } ?: run {
                reportRawError(
                    DemoStrings.text(R.string.setting_read_not_supported),
                    IllegalArgumentException(BT_FIRMWARE_VERSION_SETTING_KEY),
                )
                return@launchSerializedCommand
            }
            if (!definition.supportsRead) {
                reportRawError(
                    DemoStrings.text(R.string.setting_read_not_supported),
                    IllegalArgumentException(BT_FIRMWARE_VERSION_SETTING_KEY),
                )
                return@launchSerializedCommand
            }
            setSettingOperation(BT_FIRMWARE_VERSION_SETTING_KEY, ProductSettingOperation.READING)
            appendCommandSend("AT+VER", DemoStrings.text(R.string.send_command))
            runCatching {
                withContext(Dispatchers.IO) { session.readCapabilityValue(definition.entryKey) }
            }.onSuccess { value ->
                applyCapabilityValue(definition, value, highlight = true)
                val summary = formatCapabilityValueSummary(definition, value, optionsFor(definition))
                reportAction {
                    "${DemoStrings.text(R.string.bluetooth_firmware_version_loaded)}: $summary"
                }
                appendEvent(
                    DebugEventSource.COMMAND,
                    DebugEventLevel.Info,
                    "${DemoStrings.text(R.string.bluetooth_firmware_version)} AT+VER -> $summary",
                )
            }.onFailure { error ->
                reportError({ DemoStrings.text(R.string.failed_to_load_bluetooth_firmware_version) }, error)
            }.also {
                setSettingOperation(BT_FIRMWARE_VERSION_SETTING_KEY, null)
            }
        }
    }

    fun setAckBeepEnabled(enabled: Boolean) = runAction(
        actionLabelProvider = {
            DemoStrings.text(if (enabled) R.string.ack_beep_on_action else R.string.ack_beep_off_action)
        },
        successLogProvider = {
            DemoStrings.text(if (enabled) R.string.beep_command_sent else R.string.ack_beep_off_command_sent)
        },
        commandText = "setAckBeepEnabled($enabled)",
        refreshCachedState = true,
        requiredOperation = DemoSessionOperation.SET_ACK_BEEP_ENABLED,
    ) { it.setAckBeepEnabled(enabled) }

    fun setVibrationEnabled(enabled: Boolean) = runAction(
        actionLabelProvider = {
            DemoStrings.text(if (enabled) R.string.vibrate_on_action else R.string.vibrate_off_action)
        },
        successLogProvider = {
            DemoStrings.text(if (enabled) R.string.vibrate_on_command_sent else R.string.vibrate_off_command_sent)
        },
        commandText = "setVibrationEnabled($enabled)",
        refreshCachedState = true,
        requiredOperation = DemoSessionOperation.SET_VIBRATION_ENABLED,
    ) { it.setVibrationEnabled(enabled) }

    fun performQuickAction(action: QuickActionCommand) {
        when (action) {
            QuickActionCommand.REFRESH_INFO -> requestInfo()
            QuickActionCommand.GET_BATTERY -> requestBatteryLevel()
            QuickActionCommand.READ_BT_FIRMWARE_VERSION -> readBluetoothFirmwareVersion()
            QuickActionCommand.GET_MEMORY_USAGE -> requestStorageUsage()
            QuickActionCommand.SET_ACK_BEEP_ENABLED -> setAckBeepEnabled(true)
            QuickActionCommand.SET_ACK_BEEP_DISABLED -> setAckBeepEnabled(false)
            QuickActionCommand.SET_VIBRATION_ENABLED -> setVibrationEnabled(true)
            QuickActionCommand.SET_VIBRATION_DISABLED -> setVibrationEnabled(false)
        }
    }

    fun reloadCapabilityCatalog() {
        capabilityCatalogCacheKey = null
        appendEvent(
            DebugEventSource.UI,
            DebugEventLevel.Info,
            DemoStrings.text(R.string.capability_catalog_retry_logged),
        )
        val modelKey = _uiState.value.publicCapabilitySummary?.modelKey
            ?.takeIf { it.isNotBlank() }
            ?: preferredModelKey()
        schedulePreviewLoad(modelKey, force = true)
    }

    fun onCapabilityDomainOpened(domain: CapabilityCatalogDomain) {
        appendEvent(
            DebugEventSource.UI,
            DebugEventLevel.Info,
            DemoStrings.format(
                R.string.capability_domain_opened_logged,
                domain.title,
                domain.items.size,
            ),
        )
    }

    fun logRiskActionCancelled(label: String) {
        appendEvent(
            DebugEventSource.UI,
            DebugEventLevel.Info,
            DemoStrings.format(R.string.risk_action_cancelled_logged, label),
        )
    }

    fun triggerScan() = runAction(
        actionLabelProvider = { DemoStrings.text(R.string.trigger_scan_action) },
        successLogProvider = { DemoStrings.text(R.string.trigger_scan_command_sent) },
        commandText = "triggerScan",
        requiredOperation = DemoSessionOperation.TRIGGER_SCAN,
    ) { it.triggerScan() }

    fun executeCapabilityAction(definition: CapabilityEntry) {
        executeCapabilityAction(definition, null)
    }

    fun executeCapabilityAction(definition: CapabilityEntry, inputValue: String?) {
        if (definition.requiresValue) {
            val payload = inputValue.orEmpty().trim()
            if (payload.isEmpty()) {
                reportRawAction(DemoStrings.text(R.string.device_action_value_required))
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Warn, DemoStrings.text(R.string.device_action_value_required))
                return
            }
            runAction(
                actionLabelProvider = { deviceActionLabel(definition) },
                successLogProvider = { "${deviceActionLabel(definition)} ${DemoStrings.text(R.string.command_completed)}" },
                commandText = "executeCapabilityAction(${definition.entryKey}, value=$payload)",
            ) { session ->
                session.executeCapabilityAction(definition.entryKey, payload.encodeToByteArray())
            }
            return
        }
        runAction(
            actionLabelProvider = { deviceActionLabel(definition) },
            successLogProvider = { "${deviceActionLabel(definition)} ${DemoStrings.text(R.string.command_completed)}" },
            commandText = "executeCapabilityAction(${definition.entryKey})",
        ) { session ->
            session.executeCapabilityAction(definition.entryKey)
        }
    }

    fun readCapabilitySetting(definition: CapabilityEntry) {
        launchSerializedCommand {
            val session = requireBoundSession() ?: return@launchSerializedCommand
            if (_uiState.value.settingOperations.containsKey(definition.entryKey)) {
                return@launchSerializedCommand
            }
            if (!definition.supportsRead) {
                reportRawError(DemoStrings.text(R.string.setting_read_not_supported), IllegalArgumentException(definition.entryKey))
                return@launchSerializedCommand
            }
            setSettingOperation(definition.entryKey, ProductSettingOperation.READING)
            runCatching {
                withContext(Dispatchers.IO) { session.readCapabilityValue(definition.entryKey) }
            }.onSuccess { value ->
                applyCapabilityValue(definition, value)
                reportAction {
                    "${DemoStrings.text(R.string.read_setting_success)}: ${definition.semanticKey.ifBlank { definition.entryKey }}"
                }
                appendEvent(
                    DebugEventSource.COMMAND,
                    DebugEventLevel.Info,
                    "${DemoStrings.text(R.string.read_setting_success)} ${definition.entryKey} -> ${formatCapabilityValueSummary(definition, value, optionsFor(definition))}",
                )
            }.onFailure { error ->
                reportError({ "${DemoStrings.text(R.string.setting_read_failed)}: ${definition.entryKey}" }, error)
            }.also {
                setSettingOperation(definition.entryKey, null)
            }
        }
    }

    fun readDeviceCharsetSetting() {
        launchSerializedCommand {
            val session = requireBoundSession() ?: return@launchSerializedCommand
            val definition = _uiState.value.settingDefinitions.firstOrNull { it.semanticKey == "DeviceCharset" || it.entryKey == "setting.DeviceCharset" }
                ?: withContext(Dispatchers.IO) { session.findCapabilityEntry("setting.DeviceCharset") }
                ?: run {
                    reportRawError(DemoStrings.text(R.string.setting_read_not_supported), IllegalArgumentException("DeviceCharset"))
                    return@launchSerializedCommand
                }
            if (!definition.supportsRead) {
                reportRawError(DemoStrings.text(R.string.setting_read_not_supported), IllegalArgumentException(definition.entryKey))
                return@launchSerializedCommand
            }
            setSettingOperation(definition.entryKey, ProductSettingOperation.READING)
            runCatching {
                withContext(Dispatchers.IO) { session.readCapabilityValue(definition.entryKey) }
            }.onSuccess { value ->
                applyCapabilityValue(definition, value)
                appendEvent(
                    DebugEventSource.COMMAND,
                    DebugEventLevel.Info,
                    "${DemoStrings.text(R.string.read_setting_success)} ${definition.entryKey} -> ${formatCapabilityValueSummary(definition, value, optionsFor(definition))}",
                )
            }.onFailure { error ->
                reportError({ "${DemoStrings.text(R.string.setting_read_failed)}: ${definition.entryKey}" }, error)
            }.also {
                setSettingOperation(definition.entryKey, null)
            }
        }
    }

    fun readAllReadableSettings(definitions: List<CapabilityEntry>) {
        launchSerializedCommand {
            val session = requireBoundSession() ?: return@launchSerializedCommand
            val readableDefinitions = definitions.filter { it.supportsRead }
            readableDefinitions.forEach { definition ->
                setSettingOperation(definition.entryKey, ProductSettingOperation.READING)
            }
            readableDefinitions.forEach { definition ->
                runCatching {
                    withContext(Dispatchers.IO) { session.readCapabilityValue(definition.entryKey) }
                }.onSuccess { value ->
                    applyCapabilityValue(definition, value)
                }.onFailure { error ->
                    appendEvent(
                        DebugEventSource.SDK,
                        DebugEventLevel.Warn,
                        "${DemoStrings.text(R.string.setting_read_failed)} ${definition.entryKey}: ${error.demoErrorDetail()}",
                    )
                }.also {
                    setSettingOperation(definition.entryKey, null)
                }
            }
            reportAction { DemoStrings.text(R.string.visible_settings_read_complete) }
        }
    }

    fun writeCapabilitySetting(definition: CapabilityEntry, draft: ProductSettingDraft) {
        launchSerializedCommand {
            val session = requireBoundSession() ?: return@launchSerializedCommand
            if (_uiState.value.settingOperations.containsKey(definition.entryKey)) {
                return@launchSerializedCommand
            }
            if (!definition.supportsWrite) {
                reportRawError(DemoStrings.text(R.string.setting_write_not_supported), IllegalArgumentException(definition.entryKey))
                return@launchSerializedCommand
            }
            val options = optionsFor(definition)
            val validationError = validateSettingDraft(definition, draft, options)
            if (validationError != null) {
                updateUiState {
                    copy(settingValidationErrors = settingValidationErrors + (definition.entryKey to validationError))
                }
                reportRawError("${DemoStrings.text(R.string.setting_write_failed)}: ${definition.entryKey}", IllegalArgumentException(validationError))
                return@launchSerializedCommand
            }
            val value = runCatching { encodeSettingDraft(definition, draft, options) }
                .getOrElse { error ->
                    reportError({ "${DemoStrings.text(R.string.setting_write_failed)}: ${definition.entryKey}" }, error)
                    return@launchSerializedCommand
                }
            updateUiState {
                copy(settingValidationErrors = settingValidationErrors - definition.entryKey)
            }
            setSettingOperation(definition.entryKey, ProductSettingOperation.WRITING)
            runCatching {
                withContext(Dispatchers.IO) { session.writeCapabilityValue(definition.entryKey, value, draft.persist) }
            }.onSuccess { response ->
                appendEvent(
                    DebugEventSource.COMMAND,
                    DebugEventLevel.Info,
                    formatCommandResponse("${DemoStrings.text(R.string.write_setting_action)} ${definition.entryKey}", response),
                )
                if (definition.supportsRead) {
                    runCatching {
                        withContext(Dispatchers.IO) { session.readCapabilityValue(definition.entryKey) }
                    }.onSuccess { refreshed ->
                        applyCapabilityValue(definition, refreshed, highlight = true)
                    }
                } else {
                    markSettingRecentlyUpdated(definition.entryKey)
                }
                reportAction {
                    "${DemoStrings.text(R.string.write_setting_success)}: ${definition.semanticKey.ifBlank { definition.entryKey }}"
                }
            }.onFailure { error ->
                reportError({ "${DemoStrings.text(R.string.setting_write_failed)}: ${definition.entryKey}" }, error)
            }.also {
                setSettingOperation(definition.entryKey, null)
            }
        }
    }

    fun updateSettingDraft(key: String, draft: ProductSettingDraft) {
        updateUiState {
            copy(
                settingDrafts = settingDrafts + (key to draft),
                settingValidationErrors = settingValidationErrors - key,
            )
        }
    }

    fun resetSettingDraft(key: String) {
        val definition = _uiState.value.settingDefinitions.firstOrNull { it.entryKey == key } ?: return
        val currentValue = _uiState.value.capabilityReadValues[key]
        val previousPersist = _uiState.value.settingDrafts[key]?.persist ?: false
        val options = _uiState.value.settingOptionsByKey[key].orEmpty()
        updateUiState {
            copy(
                settingDrafts = settingDrafts + (
                    key to defaultDraftForSetting(
                        definition = definition,
                        options = options,
                        currentValue = currentValue,
                        previousPersist = previousPersist,
                    )
                ),
                settingValidationErrors = settingValidationErrors - key,
            )
        }
    }

    fun setExpandedSettingGroup(groupKey: String?) {
        updateUiState { copy(expandedSettingGroupKey = groupKey) }
    }

    fun applyDataRule(label: String, rule: DataRule) {
        val labelProvider = { dataRuleCommandLabel(rule, label) }
        launchSerializedCommand {
            val session = requireBoundSession() ?: run {
                appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, DemoStrings.text(R.string.no_active_session))
                return@launchSerializedCommand
            }
            if (!ensureOperationSupport(
                    session = session,
                    operation = DemoSessionOperation.APPLY_DATA_RULE,
                    labelProvider = labelProvider
                )
            ) {
                return@launchSerializedCommand
            }
            appendCommandSend(labelProvider(), DemoStrings.text(R.string.send_command))
            runCatching {
                withContext(Dispatchers.IO) { session.applyDataRule(rule) }
            }.onSuccess { response ->
                reportAction {
                    "${DemoStrings.text(R.string.data_rule_command_completed)}: ${labelProvider()}"
                }
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Info, formatCommandResponse("${DemoStrings.text(R.string.data_rule)} $label", response))
            }.onFailure { error ->
                reportError({
                    "${DemoStrings.text(R.string.data_rule_command_execution_failed)}: ${labelProvider()}"
                }, error)
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Error, "${DemoStrings.text(R.string.data_rule_failed)}: $label: ${error.demoErrorDetail()}")
            }
        }
    }

    fun setScanTextCharset(charset: ScanTextCharset) {
        val session = requireBoundSession() ?: run {
            appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, DemoStrings.text(R.string.no_active_session))
            return
        }
        session.setScanTextCharset(charset)
        updateUiState {
            copy(
                scanCharsetSummary = charset.displayName,
                lastActionResult = localizedAction {
                    "${DemoStrings.text(R.string.local_scan_charset_switched)} ${charset.displayName}"
                },
                errorMessage = null,
            )
        }
        appendEvent(DebugEventSource.UI, DebugEventLevel.Info, "${DemoStrings.text(R.string.scan_text_charset_set_to)} ${charset.displayName}")
    }

    fun setScanTextTerminator(terminatorBytes: ByteArray) {
        val session = requireBoundSession() ?: run {
            appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, DemoStrings.text(R.string.no_active_session))
            return
        }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { session.setScanTextTerminator(terminatorBytes) }
            }.onSuccess {
                val summary = terminatorBytes.toHexSummary()
                updateUiState {
                    copy(
                        scanTerminatorSummary = summary,
                        lastActionResult = localizedAction {
                            "${DemoStrings.text(R.string.local_scan_terminator_switched)} $summary"
                        },
                        errorMessage = null,
                    )
                }
                appendEvent(DebugEventSource.UI, DebugEventLevel.Info, "${DemoStrings.text(R.string.scan_terminator_set_to)} $summary")
            }.onFailure { error ->
                reportError({ DemoStrings.text(R.string.failed_to_set_local_scan_terminator) }, error)
            }
        }
    }

    fun disconnect() {
        val session = requireBoundSession() ?: run {
            appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, DemoStrings.text(R.string.no_active_session))
            return
        }
        viewModelScope.launch {
            pendingSessionFailure = null
            runCatching {
                withContext(Dispatchers.IO) { session.disconnect() }
            }.onSuccess {
                appendEvent(DebugEventSource.SESSION, DebugEventLevel.Info, DemoStrings.text(R.string.session_disconnected))
                handleSessionClosed(
                    status = uiText(R.string.session_disconnected),
                    lastAction = uiText(R.string.device_disconnected)
                )
            }.onFailure { error ->
                reportError({ DemoStrings.text(R.string.failed_to_disconnect_device) }, error)
            }
        }
    }

    private fun startObserving() {
        if (observing) return
        observing = true
        val session = requireBoundSession() ?: return
        val device = boundDevice ?: return
        sessionObservationJobs = DemoSessionCoordinator.observe(
            scope = viewModelScope,
            device = device,
            session = session,
            acceptTerminalStateAfterCoordinatorClear = true,
            onState = { state ->
                val previousState = lastObservedSessionState
                lastObservedSessionState = state
                DemoDiagnosticsStore.updateSessionState(state)
                updateUiState {
                    copy(
                        statusSummary = statusText(state),
                        hasReadySession = state == SessionState.READY,
                        diagnosticsSummary = DemoDiagnosticsStore.summaryText(),
                    )
                }
                if (state == SessionState.READY && previousState != SessionState.READY) {
                    reportAction {
                        DemoStrings.text(R.string.device_ready_commands_executable)
                    }
                    if (!autoInfoRequested) {
                        autoInfoRequested = true
                        viewModelScope.launch {
                            delay(AUTO_INFO_REQUEST_DELAY_MS)
                            if (boundSession === session && session.state.value == SessionState.READY) {
                                appendEvent(
                                    DebugEventSource.COMMAND,
                                    DebugEventLevel.Debug,
                                    DemoStrings.text(R.string.auto_refresh_info_delayed)
                                )
                                requestInfo()
                            }
                        }
                    }
                }
                if (state != previousState) {
                    appendEvent(
                        DebugEventSource.SESSION,
                        DebugEventLevel.Info,
                        "${DemoStrings.text(R.string.session_state)}: ${statusText(state).asStringForCurrentLanguage()}"
                    )
                }
                if (state == SessionState.DISCONNECTED || state == SessionState.ERROR) {
                    val failure = pendingSessionFailure
                    handleSessionClosed(
                        status = failure?.let(::sessionFailureStatusSummary)
                            ?: if (state == SessionState.ERROR) {
                                uiText(R.string.device_connection_error)
                            } else {
                                uiText(R.string.session_disconnected)
                            },
                        lastAction = failure?.let(::sessionFailureActionSummaryText)
                            ?: if (state == SessionState.ERROR) {
                                uiText(R.string.device_connection_interrupted)
                            } else {
                                uiText(R.string.device_connection_closed)
                            }
                    )
                }
            },
            onScan = { event ->
                applyScanEvent(event)
            },
            onFailure = { failure ->
                handleSessionFailure(failure)
            },
        )
        sessionObservationJobs += viewModelScope.launch {
            session.initializationStages.collect { event ->
                handleSessionInitializationStage(event)
            }
        }
    }

    private fun runAction(
        actionLabelProvider: (() -> String)? = null,
        successLogProvider: () -> String,
        commandText: String? = null,
        refreshCachedState: Boolean = false,
        requiredOperation: DemoSessionOperation? = null,
        block: suspend (ScannerSession) -> Unit
    ) {
        launchSerializedCommand {
            val session = requireBoundSession() ?: run {
                appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, DemoStrings.text(R.string.no_active_session))
                return@launchSerializedCommand
            }
            if (requiredOperation != null &&
                !ensureOperationSupport(session, requiredOperation, actionLabelProvider ?: successLogProvider)
            ) {
                return@launchSerializedCommand
            }
            commandText?.let { appendCommandSend(it) }
            runCatching {
                withContext(Dispatchers.IO) { block(session) }
            }.onSuccess {
                val successLog = successLogProvider()
                if (refreshCachedState) {
                    applyCachedSuccessState(session, successLog, successLogProvider)
                } else {
                    reportAction(successLogProvider)
                }
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Info, successLog)
            }.onFailure { error ->
                reportError({ DemoStrings.text(R.string.quick_action_execution_failed) }, error)
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Error, "${DemoStrings.text(R.string.quick_action_failed)}: ${error.demoErrorDetail()}")
            }
        }
    }

    private suspend fun ensureOperationSupport(
        session: ScannerSession,
        operation: DemoSessionOperation,
        labelProvider: () -> String,
    ): Boolean {
        return commandRunner.ensureOperationSupport(
            session = session,
            operation = operation,
            labelProvider = labelProvider,
            unsupportedReasonProvider = ::unsupportedDemoSessionOperationReason,
            onUnsupported = { label, reason ->
                reportError({ "$label：$reason" }, IllegalStateException(reason))
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Warn, "$label: $reason")
            },
        )
    }

    private fun launchSerializedCommand(block: suspend () -> Unit) {
        commandRunner.launch(block)
    }

    private fun requireBoundSession(): ScannerSession? {
        return commandRunner.requireReadySession(
            session = boundSession,
            onMissing = {
                appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, DemoStrings.text(R.string.no_active_session))
            },
            onNotReady = { state ->
                val reasonProvider = { DemoStrings.text(R.string.session_not_ready_action_hint) }
                lastActionResultProvider = null
                errorMessageProvider = reasonProvider
                updateUiState {
                    copy(
                        errorMessage = dynamicText(reasonProvider),
                        lastActionResult = null,
                        diagnosticsSummary = DemoDiagnosticsStore.summaryText(),
                    )
                }
                appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, "${DemoStrings.text(R.string.session_state)}: $state / ${reasonProvider()}")
            },
        )
    }

    private fun reportBusyCommand() {
        val messageProvider = { DemoStrings.text(R.string.command_already_running_wait) }
        lastActionResultProvider = null
        errorMessageProvider = messageProvider
        updateUiState {
            copy(
                errorMessage = dynamicText(messageProvider),
                lastActionResult = null,
                diagnosticsSummary = DemoDiagnosticsStore.summaryText(),
            )
        }
        appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Warn, messageProvider())
    }

    private fun clearBoundSession() {
        boundDevice = null
        boundSession = null
        boundSessionHandle = null
    }

    private fun applyFakeSessionState(preferredModelKey: String) {
        val handle = boundSessionHandle ?: return
        val modelKey = preferredModelKey.ifBlank { "CS7501" }
        val allEntries = runCatching {
            ScannerSdk.getCapabilityEntries(modelKey, handle.transportType)
        }.getOrDefault(emptyList())
        val fakeCapabilityEntries = allEntries.filter { it.kind == CapabilityEntryKind.SETTING }
        val fakeActionEntries = allEntries.filter { it.kind == CapabilityEntryKind.ACTION }
        val fakeDomains = runCatching {
            ScannerSdk.getCapabilityDomains(modelKey, handle.transportType)
        }.getOrDefault(emptyList())
        val labels = loadCapabilityLabels()
        val fakeCapability = (handle as? FakeDemoSessionHandle)?.publicCapabilitySummary
            ?: previewPublicCapabilityFor(modelKey, fakeCapabilityEntries)
        val fakeOptions = loadSettingOptionsFor(modelKey, fakeCapabilityEntries)
        DemoDiagnosticsStore.updateSessionState(handle.state.value)
        DemoDiagnosticsStore.updateResolvedModel(preferredModelKey)
        updateUiState {
            copy(
                statusSummary = statusText(handle.state.value),
                hasReadySession = handle.state.value == SessionState.READY,
                sdkResolvedModelSummary = formatSdkResolvedModelSummaryText(preferredModelKey, preferredModelKey),
                capabilitySummary = fakeCapability?.let(::formatCapabilitySummary)
                    ?: uiText(R.string.capability_summary_not_loaded),
                initializationSummary = uiText(R.string.session_initialization_completed),
                publicCapabilitySummary = fakeCapability,
                capabilityDomains = fakeDomains,
                capabilityEntries = allEntries,
                capabilityCatalog = buildCapabilityCatalog(fakeDomains, allEntries, labels),
                isLoadingCapabilityCatalog = false,
                capabilityCatalogError = null,
                actionCapabilityEntries = fakeActionEntries,
                settingDefinitions = fakeCapabilityEntries,
                settingOptionsByKey = fakeOptions,
                settingGroups = groupProductSettings(fakeCapabilityEntries),
                settingsReadSupported = fakeCapability?.supportsSettingsRead ?: false,
                settingsWriteSupported = fakeCapability?.supportsSettingsWrite ?: false,
                diagnosticsSummary = DemoDiagnosticsStore.summaryText(),
                lastActionResult = localizedAction {
                    DemoStrings.text(R.string.fake_session_ready)
                },
                errorMessage = null,
            )
        }
        appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, DemoStrings.text(R.string.fake_session_ready))
    }

    private fun handleSessionClosed(status: UiText, lastAction: UiText) {
        if (closeHandled) return
        closeHandled = true
        setDebugVisible(false)
        sessionObservationJobs.forEach { it.cancel() }
        sessionObservationJobs = emptyList()
        clearBoundSession()
        DemoSessionCoordinator.clear()
        DemoDiagnosticsStore.updateSessionState(null)
        pendingSessionFailure = null
        updateUiState {
            copy(
                statusSummary = status,
                initializationSummary = uiText(R.string.session_initialization_idle),
                hasReadySession = false,
                lastActionResult = lastAction,
                errorMessage = null,
                bluetoothFirmwareVersionSummary = uiText(R.string.setting_value_not_loaded),
                diagnosticsSummary = DemoDiagnosticsStore.summaryText(),
            )
        }
        _closePageEvents.tryEmit(Unit)
    }

    private fun handleSessionFailure(failure: SessionFailure) {
        pendingSessionFailure = failure
        val detail = buildString {
            append(failure.message)
            append(" [")
            append(transportIssueSummary(failure.issue))
            failure.platformErrorCode?.let { code -> append(", raw=$code") }
            append("]")
        }
        DemoDiagnosticsStore.updateRecentFailure("${DemoStrings.text(R.string.connection_issue)}: $detail")
        updateUiState {
            copy(
                statusSummary = sessionFailureStatusSummary(failure),
                initializationSummary = uiText(R.string.session_initialization_failed_short),
                lastActionResult = localizedAction {
                    DemoStrings.text(R.string.device_connection_interrupted)
                },
                errorMessage = rawDisplayText(detail),
                diagnosticsSummary = DemoDiagnosticsStore.summaryText(),
            )
        }
        appendEvent(DebugEventSource.BLE, DebugEventLevel.Error, "${DemoStrings.text(R.string.connection_issue)}: $detail")
    }

    private fun handleSessionInitializationStage(event: SessionInitializationStageEvent) {
        val diagnosticsSummary = sessionInitializationStageDiagnosticsSummary(event)
        val uiSummary = sessionInitializationStageUiSummary(event)
        DemoDiagnosticsStore.updateRecentSessionInitStage(diagnosticsSummary)
        updateUiState {
            copy(
                diagnosticsSummary = DemoDiagnosticsStore.summaryText(),
                initializationSummary = uiSummary,
            )
        }
        val level = if (!event.success || event.stage == SessionInitializationStage.FAILED) {
            DebugEventLevel.Error
        } else {
            DebugEventLevel.Info
        }
        appendEvent(
            DebugEventSource.SDK,
            level,
            "${DemoStrings.text(R.string.session_initialization_log_prefix)}: ${uiSummary.asStringForCurrentLanguage()} " +
                "[trace=${event.traceId}, model=${displayModelLabel(event.selectedModelKey)}]"
        )
    }

    fun setDebugVisible(visible: Boolean) {
        if (visible) {
            if (sdkDebugJob == null) {
                sdkDebugJob = viewModelScope.launch {
                    ScannerSdk.debugEvents.collect { message ->
                        appendSdkDebug(message)
                    }
                }
            }
        } else {
            sdkDebugJob?.cancel()
            sdkDebugJob = null
        }
    }

    private fun appendEvent(source: DebugEventSource, level: DebugEventLevel, message: String) {
        val event = DebugEvent(source, level, redactDemoLogMessage(message))
        AppLogStore.append(event)
        updateUiState { copy(events = (events + event).takeLast(200)) }
    }

    private fun appendSdkDebug(message: String) {
        val source = when {
            message.startsWith("core:") -> DebugEventSource.CORE
            message.startsWith("BLE") || message.contains("onConnectionStateChange") || message.contains("onServicesDiscovered") -> DebugEventSource.BLE
            else -> DebugEventSource.SDK
        }
        val level = if (message.contains("failed", ignoreCase = true) || message.contains("error", ignoreCase = true)) {
            DebugEventLevel.Error
        } else {
            DebugEventLevel.Debug
        }
        appendEvent(source, level, message)
    }

    private fun applyScanEvent(event: ScanEvent) {
        val displayText = escapeControlText(event.text).ifBlank {
            DemoStrings.text(R.string.empty_value)
        }
        val rawSummary = event.rawBytes.toHexSummary()
        val localCharset = boundSession?.getScanTextCharset() ?: ScanTextCharset.UTF_8
        val textLength = event.textBytes.size
        val rawLength = event.rawBytes.size
        appendEvent(
            DebugEventSource.SCAN,
            DebugEventLevel.Info,
            "type=${event.barcodeType} charset=${localCharset.displayName} " +
                "textBytesLength=$textLength rawBytesLength=$rawLength",
        )
        updateUiState {
            copy(
                scanCount = scanCount + 1,
                lastScanText = displayText,
                lastScanMeta = rawDisplayText(
                    DemoStrings.format(
                        R.string.last_scan_meta_format,
                        event.barcodeType,
                        textLength,
                        rawLength,
                        formatScanTimestamp(event.timestampMs),
                    )
                ),
                lastScanRawHex = rawSummary,
                lastActionResult = localizedAction {
                    DemoStrings.format(R.string.scan_received_count, scanCount + 1)
                },
                errorMessage = null,
            )
        }
    }

    private fun sessionFailureStatusSummary(failure: SessionFailure): UiText {
        val label = failure.issue.localizedStatusLabel()
        return sdkText(label.localizationKey, label.fallbackDisplayName)
    }

    private fun sessionInitializationStageSummary(event: SessionInitializationStageEvent): String {
        return sessionInitializationStageDiagnosticsSummary(event)
    }

    private fun sessionInitializationStageDiagnosticsSummary(event: SessionInitializationStageEvent): String {
        val stageLabel = when (event.stage) {
            SessionInitializationStage.STARTED -> "started"
            SessionInitializationStage.READING_DEVICE_INFO -> "readingDeviceInfo"
            SessionInitializationStage.READING_BATTERY -> "readingBattery"
            SessionInitializationStage.READING_CAPABILITY_SUMMARY -> "readingCapabilitySummary"
            SessionInitializationStage.READING_OPERATION_SUPPORT -> "readingOperationSupport"
            SessionInitializationStage.COMPLETED -> "completed"
            SessionInitializationStage.FAILED -> "failed"
            SessionInitializationStage.UNKNOWN -> "unknown"
        }
        return buildString {
            append(stageLabel)
            append(" trace=").append(event.traceId)
            append(" model=").append(displayModelLabel(event.selectedModelKey))
            if (!event.success || event.errorCode != 0) {
                append(" error=").append(event.errorCode)
            }
            event.message?.takeIf { it.isNotBlank() }?.let { append(" msg=").append(it) }
        }
    }

    private fun sessionInitializationStageUiSummary(event: SessionInitializationStageEvent): UiText {
        return when (event.stage) {
            SessionInitializationStage.STARTED -> uiText(R.string.session_initialization_started)
            SessionInitializationStage.READING_DEVICE_INFO -> uiText(R.string.session_initialization_reading_device_info)
            SessionInitializationStage.READING_BATTERY -> uiText(R.string.session_initialization_reading_battery)
            SessionInitializationStage.READING_CAPABILITY_SUMMARY ->
                uiText(R.string.session_initialization_reading_capability)
            SessionInitializationStage.READING_OPERATION_SUPPORT ->
                uiText(R.string.session_initialization_reading_operation_support)
            SessionInitializationStage.COMPLETED -> uiText(R.string.session_initialization_completed)
            SessionInitializationStage.FAILED -> uiText(R.string.session_initialization_failed_short)
            SessionInitializationStage.UNKNOWN -> uiText(R.string.session_initialization_unknown)
        }
    }

    private fun sessionFailureActionSummaryText(failure: SessionFailure): UiText =
        joinedText(sessionFailureStatusSummary(failure), rawDisplayText(": ${failure.message}"))

    private fun transportIssueSummary(issue: TransportIssue): String {
        val label = issue.localizedSdkLabel()
        return SdkLabelResolver.resolve(label.localizationKey, label.fallbackDisplayName)
    }

    private fun updateUiState(transform: CommandConsoleUiState.() -> CommandConsoleUiState) {
        _uiState.value = _uiState.value.transform()
    }

    private fun reportRawAction(message: String) {
        lastActionResultProvider = null
        errorMessageProvider = null
        updateUiState { copy(lastActionResult = rawDisplayText(message), errorMessage = null) }
    }

    private fun reportAction(provider: () -> String) {
        lastActionResultProvider = provider
        errorMessageProvider = null
        updateUiState { copy(lastActionResult = dynamicText(provider), errorMessage = null) }
    }

    private fun reportRawError(prefix: String, error: Throwable) {
        lastActionResultProvider = null
        errorMessageProvider = null
        val detail = error.demoErrorDetail()
        updateUiState { copy(errorMessage = rawDisplayText("$prefix：$detail")) }
        appendEvent(DebugEventSource.SDK, DebugEventLevel.Error, "$prefix: $detail")
    }

    private fun reportError(prefixProvider: () -> String, error: Throwable) {
        errorMessageProvider = { "${prefixProvider()}：${error.demoErrorDetail()}" }
        updateUiState { copy(errorMessage = errorMessageProvider?.let(::dynamicText)) }
        appendEvent(DebugEventSource.SDK, DebugEventLevel.Error, "${prefixProvider()}: ${error.demoErrorDetail()}")
    }

    private fun localizedAction(provider: () -> String): UiText {
        lastActionResultProvider = provider
        errorMessageProvider = null
        return dynamicText(provider)
    }

    private fun applyCapabilityValue(definition: CapabilityEntry, value: CapabilityValue, highlight: Boolean = false) {
        updateUiState {
            val previousPersist = settingDrafts[definition.entryKey]?.persist ?: false
            val options = settingOptionsByKey[definition.entryKey].orEmpty()
            val valueSummary = formatCapabilityValueSummary(definition, value, options)
            copy(
                capabilityReadValues = capabilityReadValues + (definition.entryKey to value),
                settingDrafts = settingDrafts + (
                    definition.entryKey to defaultDraftForSetting(definition, options, value, previousPersist)
                ),
                settingValidationErrors = settingValidationErrors - definition.entryKey,
                bluetoothFirmwareVersionSummary = if (definition.semanticKey == BT_FIRMWARE_VERSION_SETTING_KEY) {
                    rawDisplayText(valueSummary.ifBlank { DemoStrings.text(R.string.setting_value_not_loaded) })
                } else {
                    bluetoothFirmwareVersionSummary
                },
                deviceCharsetSummary = if (
                    definition.semanticKey == "DeviceCharset" || definition.entryKey == "setting.DeviceCharset"
                ) {
                    rawDisplayText(valueSummary)
                } else {
                    deviceCharsetSummary
                },
            )
        }
        if (highlight) {
            markSettingRecentlyUpdated(definition.entryKey)
        }
    }

    private fun setSettingOperation(key: String, operation: ProductSettingOperation?) {
        updateUiState {
            copy(
                settingOperations = when (operation) {
                    null -> settingOperations - key
                    else -> settingOperations + (key to operation)
                },
            )
        }
    }

    private fun loadActionEntriesForModel(modelKey: String = preferredModelKey()): List<CapabilityEntry> {
        if (modelKey.isBlank()) {
            return emptyList()
        }
        return runCatching {
            ScannerSdk.getCapabilityEntries(modelKey, TransportType.BLE_GATT)
                .filter { it.kind == CapabilityEntryKind.ACTION }
        }
            .getOrElse { error ->
                appendEvent(DebugEventSource.SDK, DebugEventLevel.Warn, "loadActionEntriesForModel: ${error.demoErrorDetail()}")
                emptyList()
            }
    }

    private fun markSettingRecentlyUpdated(key: String) {
        settingHighlightJobs.remove(key)?.cancel()
        updateUiState {
            copy(settingRecentlyUpdatedKeys = settingRecentlyUpdatedKeys + key)
        }
        settingHighlightJobs[key] = viewModelScope.launch {
            delay(1800)
            updateUiState {
                copy(settingRecentlyUpdatedKeys = settingRecentlyUpdatedKeys - key)
            }
            settingHighlightJobs.remove(key)
        }
    }

    private fun appendCommandSend(commandText: String, prefix: String = DemoStrings.text(R.string.send_command)) {
        appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Debug, "$prefix: $commandText")
    }

    private fun ByteArray.toHexSummary(): String {
        return joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
    }

    private fun formatScanTimestamp(timestampMs: Long): String =
        SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestampMs))

    private fun formatCommandResponse(prefix: String, response: CommandResponse): String {
        return CommandResponseFormatter.format(
            prefix = prefix,
            response = response,
            commandTextCharset = currentCommandTextDisplayCharset(),
            recordCharset = currentRecordDisplayCharset(),
        )
    }

    private fun currentRecordDisplayCharset() =
        (boundSession?.getScanTextCharset() ?: ScanTextCharset.UTF_8).charset

    private fun currentCommandTextDisplayCharset() =
        (boundSession?.getScanTextCharset() ?: ScanTextCharset.UTF_8).charset

    private fun schedulePreviewLoad(modelKey: String, force: Boolean = false) {
        previewLoadJob?.cancel()
        if (modelKey.isBlank()) {
            return
        }
        val transportType = boundSessionHandle?.transportType
            ?: boundDevice?.transportType
            ?: TransportType.BLE_GATT
        val cacheKey = "$modelKey:${transportType.name}:${DemoLocaleController.currentLanguage.name}"
        if (!force && capabilityCatalogCacheKey == cacheKey && _uiState.value.capabilityCatalog.isNotEmpty()) {
            return
        }
        updateUiState {
            copy(
                isLoadingCapabilityCatalog = true,
                capabilityCatalogError = null,
            )
        }
        capabilityCatalogRequestedKey = cacheKey
        appendEvent(
            DebugEventSource.SDK,
            DebugEventLevel.Debug,
            DemoStrings.format(R.string.capability_catalog_loading_logged, modelKey, transportType.name),
        )
        val session = boundSession
        previewLoadJob = viewModelScope.launch {
            val preview = runCatching {
                withContext(Dispatchers.IO) { loadConsolePreview(modelKey, transportType, session) }
            }.getOrElse { error ->
                appendEvent(
                    DebugEventSource.SDK,
                    DebugEventLevel.Warn,
                    "loadConsolePreview: ${error.demoErrorDetail()}",
                )
                if (capabilityCatalogRequestedKey == cacheKey) {
                    updateUiState {
                        copy(
                            isLoadingCapabilityCatalog = false,
                            capabilityCatalogError = uiText(R.string.capability_catalog_load_failed),
                        )
                    }
                }
                return@launch
            }
            if (capabilityCatalogRequestedKey != cacheKey) {
                return@launch
            }
            capabilityCatalogCacheKey = cacheKey
            updateUiState {
                copy(
                    capabilitySummary = formatCapabilitySummary(preview.capability),
                    publicCapabilitySummary = preview.capability,
                    capabilityDomains = preview.domains,
                    capabilityEntries = preview.entries,
                    capabilityCatalog = preview.catalog,
                    isLoadingCapabilityCatalog = false,
                    capabilityCatalogError = null,
                    actionCapabilityEntries = preview.actions,
                    settingDefinitions = preview.settings,
                    settingOptionsByKey = preview.optionsByKey,
                    settingGroups = preview.groups,
                    settingsReadSupported = preview.capability.supportsSettingsRead,
                    settingsWriteSupported = preview.capability.supportsSettingsWrite,
                )
            }
            appendEvent(
                DebugEventSource.SDK,
                DebugEventLevel.Info,
                DemoStrings.format(
                    R.string.capability_catalog_loaded_logged,
                    preview.catalog.size,
                    preview.entries.count { it.visibleByDefault },
                ),
            )
        }
    }

    private suspend fun loadConsolePreview(
        modelKey: String,
        transportType: TransportType,
        session: ScannerSession?,
    ): ConsolePreview {
        val domains = session?.getCapabilityDomains()
            ?: ScannerSdk.getCapabilityDomains(modelKey, transportType)
        val entries = session?.getCapabilityEntries()
            ?: ScannerSdk.getCapabilityEntries(modelKey, transportType)
        val settings = entries.filter { it.kind == CapabilityEntryKind.SETTING }
        val actions = entries.filter { it.kind == CapabilityEntryKind.ACTION }
        val labels = loadCapabilityLabels()
        val optionsByKey = settings.associate { definition ->
            definition.entryKey to productSettingOptions(definition, labels)
        }
        return ConsolePreview(
            capability = checkNotNull(previewPublicCapabilityFor(modelKey, settings)),
            domains = domains,
            entries = entries,
            catalog = buildCapabilityCatalog(domains, entries, labels),
            actions = actions,
            settings = settings,
            optionsByKey = optionsByKey,
            groups = groupProductSettings(settings, labels),
        )
    }

    private fun loadCapabilityLabels(): List<CapabilityLabel> =
        CapabilityLabelKind.entries.flatMap { kind ->
            runCatching { ScannerSdk.getCapabilityLabels(kind) }.getOrElse { emptyList() }
        }

    private fun applyDeviceState(snapshot: DeviceSnapshot) {
        val summary = DeviceStateSummaryFormatter.format(snapshot.info)
        val selectedModelKey = preferredModelKey()
        DemoDiagnosticsStore.updateSelectedModel(selectedModelKey)
        DemoDiagnosticsStore.updateResolvedModel(snapshot.resolvedModel)
        val definitions = snapshot.settingDefinitions
        val optionsByKey = snapshot.settingOptionsByKey
        val labels = loadCapabilityLabels()

        updateUiState {
            copy(
                infoSummary = summary.infoSummary,
                selectedModelSummary = formatSelectedModelSummaryText(selectedModelKey),
                sdkResolvedModelSummary = formatSdkResolvedModelSummaryText(selectedModelKey, snapshot.resolvedModel),
                capabilitySummary = formatCapabilitySummary(snapshot.capability),
                initializationSummary = uiText(R.string.session_initialization_completed),
                publicCapabilitySummary = snapshot.capability,
                settingDefinitions = definitions,
                settingOptionsByKey = optionsByKey,
                settingGroups = groupProductSettings(definitions, labels),
                settingsReadSupported = snapshot.capability.supportsSettingsRead,
                settingsWriteSupported = snapshot.capability.supportsSettingsWrite,
                settingDrafts = definitions.associate { definition ->
                    val options = optionsByKey[definition.entryKey].orEmpty()
                    val previousPersist = settingDrafts[definition.entryKey]?.persist ?: false
                    val currentValue = capabilityReadValues[definition.entryKey]
                    definition.entryKey to defaultDraftForSetting(definition, options, currentValue, previousPersist)
                },
                deviceCharsetSummary = summary.deviceCharsetSummary,
                deviceTerminalSummary = summary.deviceTerminalSummary,
                bluetoothFirmwareVersionSummary = rawDisplayText(
                    snapshot.info.bluetoothFirmwareVersion.ifBlank {
                        DemoStrings.text(R.string.setting_value_not_loaded)
                    }
                ),
                diagnosticsSummary = DemoDiagnosticsStore.summaryText(),
            )
        }
        schedulePreviewLoad(snapshot.resolvedModel.ifBlank { selectedModelKey })
    }

    private suspend fun refreshCachedState(session: ScannerSession) {
        runCatching {
            withContext(Dispatchers.IO) {
                val definitions = loadSettingEntriesFor(session)
                DeviceSnapshot(
                    info = session.getCachedInfo(),
                    batteryInfo = com.netumscan.scannersdk.model.BatteryInfo(rawText = "", voltageText = "", percent = -1),
                    modelConfigApplied = false,
                    resolvedModel = session.getResolvedModelKey(),
                    capability = session.getDeviceCapabilitySummary(),
                    settingDefinitions = definitions,
                    settingOptionsByKey = loadSettingOptionsFor(session, definitions),
                )
            }
        }.onSuccess { snapshot ->
            val summary = CachedDeviceStateSummaryLoader.load(infoProvider = { snapshot.info })
            updateUiState {
                copy(
                    infoSummary = summary.infoSummary,
                    selectedModelSummary = formatSelectedModelSummaryText(preferredModelKey()),
                    sdkResolvedModelSummary = formatSdkResolvedModelSummaryText(preferredModelKey(), snapshot.resolvedModel),
                    capabilitySummary = formatCapabilitySummary(snapshot.capability),
                    initializationSummary = uiText(R.string.session_initialization_completed),
                    publicCapabilitySummary = snapshot.capability,
                    settingDefinitions = snapshot.settingDefinitions,
                    settingOptionsByKey = snapshot.settingOptionsByKey,
                    settingGroups = groupProductSettings(snapshot.settingDefinitions, loadCapabilityLabels()),
                    settingsReadSupported = snapshot.capability.supportsSettingsRead,
                    settingsWriteSupported = snapshot.capability.supportsSettingsWrite,
                    deviceCharsetSummary = summary.deviceCharsetSummary,
                    deviceTerminalSummary = summary.deviceTerminalSummary,
                )
            }
            schedulePreviewLoad(snapshot.resolvedModel.ifBlank { preferredModelKey() })
        }.onFailure { error ->
            appendEvent(DebugEventSource.SDK, DebugEventLevel.Warn, "${DemoStrings.text(R.string.refresh_cached_state_failed)}: ${error.demoErrorDetail()}")
        }
    }

    private suspend fun refreshLocalizedCachedState(session: ScannerSession) {
        runCatching {
            withContext(Dispatchers.IO) {
                val definitions = loadSettingEntriesFor(session)
                DeviceSnapshot(
                    info = session.getCachedInfo(),
                    batteryInfo = com.netumscan.scannersdk.model.BatteryInfo(rawText = "", voltageText = "", percent = -1),
                    modelConfigApplied = false,
                    resolvedModel = session.getResolvedModelKey(),
                    capability = session.getDeviceCapabilitySummary(),
                    settingDefinitions = definitions,
                    settingOptionsByKey = loadSettingOptionsFor(session, definitions),
                )
            }
        }.onSuccess { snapshot ->
            val summary = CachedDeviceStateSummaryLoader.load(infoProvider = { snapshot.info })
            updateUiState {
                copy(
                    infoSummary = summary.infoSummary,
                    selectedModelSummary = formatSelectedModelSummaryText(preferredModelKey()),
                    sdkResolvedModelSummary = formatSdkResolvedModelSummaryText(preferredModelKey(), snapshot.resolvedModel),
                    capabilitySummary = formatCapabilitySummary(snapshot.capability),
                    initializationSummary = uiText(R.string.session_initialization_completed),
                    publicCapabilitySummary = snapshot.capability,
                    settingDefinitions = snapshot.settingDefinitions,
                    settingOptionsByKey = snapshot.settingOptionsByKey,
                    settingGroups = groupProductSettings(snapshot.settingDefinitions, loadCapabilityLabels()),
                    settingsReadSupported = snapshot.capability.supportsSettingsRead,
                    settingsWriteSupported = snapshot.capability.supportsSettingsWrite,
                    deviceCharsetSummary = summary.deviceCharsetSummary,
                    deviceTerminalSummary = summary.deviceTerminalSummary,
                )
            }
            schedulePreviewLoad(snapshot.resolvedModel.ifBlank { preferredModelKey() })
        }.onFailure { error ->
            appendEvent(DebugEventSource.SDK, DebugEventLevel.Warn, "${DemoStrings.text(R.string.refresh_cached_state_failed)}: ${error.demoErrorDetail()}")
        }
    }

    private suspend fun applyCachedSuccessState(
        session: ScannerSession,
        actionResult: String,
        actionResultProvider: (() -> String)? = null,
    ) {
        runCatching {
            withContext(Dispatchers.IO) {
                val definitions = loadSettingEntriesFor(session)
                val snapshot = DeviceSnapshot(
                    info = session.getCachedInfo(),
                    batteryInfo = com.netumscan.scannersdk.model.BatteryInfo(rawText = "", voltageText = "", percent = -1),
                    modelConfigApplied = false,
                    resolvedModel = session.getResolvedModelKey(),
                    capability = session.getDeviceCapabilitySummary(),
                    settingDefinitions = definitions,
                    settingOptionsByKey = loadSettingOptionsFor(session, definitions),
                )
                snapshot to CachedCommandSuccessStateLoader.load(
                    actionResult = actionResult,
                    summaryProvider = {
                        CachedDeviceStateSummaryLoader.load(infoProvider = { snapshot.info })
                    },
                )
            }
        }.onSuccess { (snapshot, state) ->
            updateUiState {
                copy(
                    infoSummary = state.infoSummary,
                    selectedModelSummary = formatSelectedModelSummaryText(preferredModelKey()),
                    sdkResolvedModelSummary = formatSdkResolvedModelSummaryText(preferredModelKey(), snapshot.resolvedModel),
                    capabilitySummary = formatCapabilitySummary(snapshot.capability),
                    initializationSummary = uiText(R.string.session_initialization_completed),
                    publicCapabilitySummary = snapshot.capability,
                    settingDefinitions = snapshot.settingDefinitions,
                    settingOptionsByKey = snapshot.settingOptionsByKey,
                    settingGroups = groupProductSettings(snapshot.settingDefinitions, loadCapabilityLabels()),
                    settingsReadSupported = snapshot.capability.supportsSettingsRead,
                    settingsWriteSupported = snapshot.capability.supportsSettingsWrite,
                    deviceCharsetSummary = state.deviceCharsetSummary,
                    deviceTerminalSummary = state.deviceTerminalSummary,
                    lastActionResult = actionResultProvider?.let(::localizedAction) ?: rawDisplayText(state.lastActionResult),
                    errorMessage = null,
                )
            }
            schedulePreviewLoad(snapshot.resolvedModel.ifBlank { preferredModelKey() })
        }.onFailure { error ->
            appendEvent(DebugEventSource.SDK, DebugEventLevel.Warn, "${DemoStrings.text(R.string.refresh_cached_state_failed)}: ${error.demoErrorDetail()}")
            if (actionResultProvider == null) {
                reportRawAction(actionResult)
            } else {
                reportAction(actionResultProvider)
            }
        }
    }

    private fun dataRuleCommandLabel(rule: DataRule, fallback: String): String {
        val kind = when (rule) {
            is DataRule.SetSuffix -> DataRuleKind.SUFFIX
            is DataRule.SetPrefix -> DataRuleKind.PREFIX
            is DataRule.HideEnd -> DataRuleKind.HIDE_END
            is DataRule.HideMiddle -> DataRuleKind.HIDE_MIDDLE
            is DataRule.HideStart -> DataRuleKind.HIDE_START
            is DataRule.Replace -> DataRuleKind.REPLACE
        }
        val label = kind.localizedSdkLabel()
        return SdkLabelResolver.resolve(label.localizationKey, label.fallbackDisplayName).ifBlank { fallback }
    }

    private fun statusText(state: SessionState): UiText {
        val label = state.localizedLabel()
        return sdkText(label.localizationKey, label.fallbackDisplayName)
    }

    private fun preferredModelKey(): String = DemoSessionCoordinator.preferredModelKey

    private fun loadSettingEntriesForModel(modelKey: String): List<CapabilityEntry> {
        if (modelKey == "") {
            return emptyList()
        }
        return runCatching {
            ScannerSdk.getCapabilityEntries(modelKey, TransportType.BLE_GATT)
                .filter { it.kind == CapabilityEntryKind.SETTING }
        }.getOrDefault(emptyList())
    }

    private fun loadSettingOptionsFor(
        modelKey: String,
        definitions: List<CapabilityEntry>,
    ): Map<String, List<ProductSettingOption>> {
        if (modelKey == "" || definitions.isEmpty()) {
            return emptyMap()
        }
        val labels = loadCapabilityLabels()
        return definitions.associate { definition ->
            definition.entryKey to productSettingOptions(definition, labels)
        }
    }

    private suspend fun loadSettingOptionsFor(
        session: ScannerSession,
        definitions: List<CapabilityEntry>,
    ): Map<String, List<ProductSettingOption>> {
        val labels = loadCapabilityLabels()
        return definitions.associate { definition ->
            definition.entryKey to productSettingOptions(definition, labels)
        }
    }

    private suspend fun loadSettingEntriesFor(session: ScannerSession): List<CapabilityEntry> =
        session.getCapabilityEntries().filter { it.kind == CapabilityEntryKind.SETTING }

    private fun optionsFor(definition: CapabilityEntry): List<ProductSettingOption> =
        _uiState.value.settingOptionsByKey[definition.entryKey].orEmpty()

    private fun previewPublicCapabilityFor(
        modelKey: String,
        definitions: List<CapabilityEntry>,
    ): DeviceCapabilitySummary? {
        if (modelKey == "" && definitions.isEmpty()) {
            return null
        }
        val resolved = if (modelKey == "") "CS7501" else modelKey
        return DeviceCapabilitySummary(
            modelKey = resolved,
            modelName = displayModelLabel(resolved),
            supportsScanControl = false,
            supportsDeviceCommands = true,
            supportsSettingsRead = definitions.any { it.supportsRead },
            supportsSettingsWrite = definitions.any { it.supportsWrite },
            supportsDataRules = true,
            supportsBattery = true,
            supportStatus = DeviceSupportStatus.CODE_ONLY,
        )
    }

    private data class DeviceSnapshot(
        val info: ScannerInfo,
        val batteryInfo: com.netumscan.scannersdk.model.BatteryInfo,
        val modelConfigApplied: Boolean,
        val resolvedModel: String,
        val capability: DeviceCapabilitySummary,
        val settingDefinitions: List<CapabilityEntry>,
        val settingOptionsByKey: Map<String, List<ProductSettingOption>>,
    )

    private data class ConsolePreview(
        val capability: DeviceCapabilitySummary,
        val domains: List<CapabilityDomain>,
        val entries: List<CapabilityEntry>,
        val catalog: List<CapabilityCatalogDomain>,
        val actions: List<CapabilityEntry>,
        val settings: List<CapabilityEntry>,
        val optionsByKey: Map<String, List<ProductSettingOption>>,
        val groups: List<ProductSettingGroup>,
    )
}
