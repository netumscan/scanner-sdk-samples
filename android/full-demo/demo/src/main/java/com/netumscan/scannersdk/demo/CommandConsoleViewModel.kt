package com.netumscan.scannersdk.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netumscan.scannersdk.ScannerSdk
import com.netumscan.scannersdk.ScannerSession
import com.netumscan.scannersdk.SessionState
import com.netumscan.scannersdk.localizedLabel
import com.netumscan.scannersdk.model.CommandCode
import com.netumscan.scannersdk.model.CommandResponse
import com.netumscan.scannersdk.model.BasicDeviceCommand
import com.netumscan.scannersdk.model.BleTransportIssue
import com.netumscan.scannersdk.model.DataRuleCommand
import com.netumscan.scannersdk.model.DataRuleKind
import com.netumscan.scannersdk.model.DiscoveredDevice
import com.netumscan.scannersdk.model.DeviceCapabilitySummary
import com.netumscan.scannersdk.model.DeviceModelId
import com.netumscan.scannersdk.model.MasterCommand
import com.netumscan.scannersdk.model.ModuleCommandKind
import com.netumscan.scannersdk.model.ModuleFamily
import com.netumscan.scannersdk.model.ModuleParameterUiKind
import com.netumscan.scannersdk.model.Ntc06hSettingCatalog
import com.netumscan.scannersdk.model.Nt212xParameterAliases
import com.netumscan.scannersdk.model.ScanTextCharset
import com.netumscan.scannersdk.model.ScannerInfo
import com.netumscan.scannersdk.model.SessionFailure
import com.netumscan.scannersdk.model.TransportFailureCode
import com.netumscan.scannersdk.model.localizedLabel as localizedSdkLabel
import com.netumscan.scannersdk.model.localizedStatusLabel
import com.netumscan.scannersdk.saveNtc06hSettings
import com.netumscan.scannersdk.writeNtc06hSetting
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

internal fun mergePreferredCapabilityWithRuntime(
    preferredCapability: DeviceCapabilitySummary?,
    runtimeCapability: DeviceCapabilitySummary,
): DeviceCapabilitySummary {
    return preferredCapability?.copy(
        supportsBasicDeviceCommands = runtimeCapability.supportsBasicDeviceCommands,
        supportsMasterCommands = runtimeCapability.supportsMasterCommands,
        supportsNativeModuleCommands = runtimeCapability.supportsNativeModuleCommands,
        supportsModuleCommandBridge = runtimeCapability.supportsModuleCommandBridge,
        supportsModuleCommands = runtimeCapability.supportsModuleCommands,
        supportsScannerMaster = runtimeCapability.supportsScannerMaster,
        supportsModulePassthrough = runtimeCapability.supportsModulePassthrough,
    ) ?: runtimeCapability
}

internal fun formatCapabilitySummary(capability: DeviceCapabilitySummary): UiText =
    capability.toCapabilitySummaryUi().asText()

class CommandConsoleViewModel : ViewModel() {
    private companion object {
        const val AUTO_INFO_REQUEST_DELAY_MS = 1200L
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
    private var lastActionResultProvider: (() -> String)? = null
    private var errorMessageProvider: (() -> String)? = null
    private val commandExecutionMutex = Mutex()
    private val commandRunner = SessionCommandRunner(
        scope = viewModelScope,
        mutex = commandExecutionMutex,
        setExecuting = { executing -> updateUiState { copy(isExecuting = executing) } },
        onBusy = ::reportBusyCommand,
    )
    private val moduleCommandRunner = ModuleCommandRunner(
        canExecuteModuleCommands = { _uiState.value.canExecuteModuleCommands },
        notReadyReason = { DemoStrings.text(R.string.module_command_not_ready_reason) },
        onNotReady = { label, reason ->
            reportError({ label }, IllegalStateException(reason))
            appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Warn, "$label: $reason")
        },
    )

    fun refreshLocalizedUi() {
        val device = boundDevice
        val session = boundSession
        val latestState = session?.state?.value
        val scanCharset = session?.getScanTextCharset() ?: ScanTextCharset.UTF_8
        val scanTerminator = session?.getScanTerminator()?.toHexSummary() ?: "0D"
        val selectedModelId = preferredModelId()
        DemoDiagnosticsStore.updateSelectedModel(selectedModelId)
        DemoDiagnosticsStore.updateSessionState(latestState)
        updateUiState {
            copy(
                statusSummary = latestState?.let(::statusText) ?: uiText(R.string.not_connected),
                deviceSummary = if (device == null) {
                    uiText(R.string.no_selected_device)
                } else {
                    rawDisplayText("${device.name.ifBlank { DemoStrings.unknownDeviceName }} / ${device.deviceId}")
                },
                selectedModelSummary = formatSelectedModelSummaryText(selectedModelId),
                scanCharsetSummary = scanCharset.displayName,
                scanTerminatorSummary = scanTerminator,
                hasReadySession = latestState == SessionState.READY,
                diagnosticsSummary = DemoDiagnosticsStore.summaryText(),
                lastActionResult = lastActionResultProvider?.let(::dynamicText) ?: lastActionResult,
                errorMessage = errorMessageProvider?.let(::dynamicText) ?: errorMessage,
            )
        }
        if (session == null) {
            updateUiState {
                copy(
                    infoSummary = uiText(R.string.device_info_not_loaded),
                    sdkResolvedModelSummary = uiText(R.string.sdk_resolved_model_not_loaded),
                    capabilitySummary = uiText(R.string.capability_summary_not_loaded),
                    moduleSummary = uiText(R.string.module_capability_not_loaded),
                    currentModuleFamily = ModuleFamily.UNKNOWN,
                    supportsModuleCommands = false,
                    canExecuteModuleCommands = false,
                    deviceCharsetSummary = uiText(R.string.device_config_not_auto_queried),
                    deviceTerminalSummary = uiText(R.string.device_terminator_not_auto_queried),
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
        boundDevice = DemoSessionCoordinator.selectedDevice
        boundSession = DemoSessionCoordinator.activeSession
        boundSessionHandle = DemoSessionCoordinator.activeSessionHandle
        val preferredModelId = DemoSessionCoordinator.preferredModelId
        val preferredCapability = loadCapabilityFor(preferredModelId)
        val device = boundDevice
        val session = boundSession
        val scanCharset = session?.getScanTextCharset() ?: ScanTextCharset.UTF_8
        val scanTerminator = session?.getScanTerminator()?.toHexSummary() ?: "0D"
        DemoDiagnosticsStore.updateSelectedModel(preferredModelId)
        DemoDiagnosticsStore.updateSessionState(session?.state?.value)
        updateUiState {
            copy(
                deviceSummary = if (device == null) {
                    uiText(R.string.no_selected_device)
                } else {
                    rawDisplayText("${device.name.ifBlank { DemoStrings.unknownDeviceName }} / ${device.deviceId}")
                },
                selectedModelSummary = formatSelectedModelSummaryText(preferredModelId),
                sdkResolvedModelSummary = uiText(R.string.sdk_resolved_model_not_loaded),
                capabilitySummary = preferredCapability?.let(::formatCapabilitySummary)
                    ?: uiText(R.string.capability_summary_not_loaded),
                moduleSummary = preferredCapability?.let(::formatModuleSummary)
                    ?: uiText(R.string.module_capability_not_loaded),
                currentModuleFamily = preferredCapability?.moduleFamily ?: ModuleFamily.UNKNOWN,
                supportsModuleCommands = preferredCapability?.supportsModuleCommands ?: false,
                canExecuteModuleCommands = false,
                deviceCharsetSummary = uiText(R.string.device_config_not_auto_queried),
                deviceTerminalSummary = uiText(R.string.device_terminator_not_auto_queried),
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
            applyFakeSessionState(preferredModelId)
            return
        }
        startObserving()
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
            appendCommandSend(sdkCommandText(CommandCode.GET_INFO, "\$SW#VER"))
            runCatching {
                withContext(Dispatchers.IO) {
                    val initialization = session.initializeSession(applyModelConfig = true)
                    DeviceSnapshot(
                        info = initialization.info,
                        batteryInfo = initialization.batteryInfo,
                        modelConfigApplied = initialization.modelConfigApplied,
                        resolvedModel = session.getResolvedModelId(),
                        capability = session.getDeviceCapabilitySummary(),
                    )
                }
            }.onSuccess { snapshot ->
                val selectedModelId = preferredModelId()
                val effectiveCapability = effectiveCapabilityFor(selectedModelId, snapshot.capability)
                val canExecuteModuleCommands = queryModuleCommandAvailability(session)
                applyDeviceState(snapshot, effectiveCapability, canExecuteModuleCommands)
                updateUiState {
                    copy(
                        lastActionResult = localizedAction {
                            DemoStrings.text(R.string.device_info_loaded)
                        },
                        errorMessage = null
                    )
                }
                if (snapshot.modelConfigApplied) {
                    appendEvent(
                        DebugEventSource.COMMAND,
                        DebugEventLevel.Info,
                        DemoStrings.format(R.string.model_config_applied_after_refresh, displayModelLabel(selectedModelId))
                    )
                }
                appendEvent(
                    DebugEventSource.COMMAND,
                    DebugEventLevel.Debug,
                    formatSdkResolvedModelSummary(selectedModelId, snapshot.resolvedModel)
                )
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Info, "${DemoStrings.text(R.string.info)} firmware=${snapshot.info.firmwareVersion} hardware=${snapshot.info.hardwareVersion} serial=${snapshot.info.serialNumber}")
                appendEvent(
                    DebugEventSource.COMMAND,
                    DebugEventLevel.Debug,
                    "${DemoStrings.text(R.string.version_parsed)} family=${snapshot.info.versionFormatFamily} boot=${snapshot.info.versionBootCode} series=${snapshot.info.versionSeriesCode} transport=${snapshot.info.versionTransportCode}${snapshot.info.versionTransportSuffix} wireless=${snapshot.info.versionWirelessCode} bt=${snapshot.info.versionBluetoothCode} chip=${snapshot.info.versionChipsetCode}${snapshot.info.versionChipsetSuffix}/${snapshot.info.hardwareVersion} release=${snapshot.info.versionReleaseCode} ext=${snapshot.info.versionExtensionCode}"
                )
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Info, "${DemoStrings.text(R.string.battery)} raw=${snapshot.batteryInfo.rawText}")
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Info, "${DemoStrings.text(R.string.battery_parsed)} voltage=${snapshot.batteryInfo.voltageText} percent=${snapshot.batteryInfo.percent}")
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Info, "${DemoStrings.text(R.string.capability)} ${effectiveCapability.displaySummary}")
            }.onFailure { error ->
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
            appendCommandSend(sdkCommandText(CommandCode.GET_BATTERY_INFO, "%BAT_VOL#"))
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

    fun beep() = runAction(
        actionLabelProvider = { DemoStrings.text(R.string.ack_beep_on_action) },
        successLogProvider = { DemoStrings.text(R.string.beep_command_sent) },
        commandText = sdkCommandText(CommandCode.BEEP, "%ACKBEEP#1"),
        refreshCachedState = true,
        requiredOperation = DemoSessionOperation.BEEP,
    ) { it.beep() }

    fun disableAckBeep() = runAction(
        actionLabelProvider = { DemoStrings.text(R.string.ack_beep_off_action) },
        successLogProvider = { DemoStrings.text(R.string.ack_beep_off_command_sent) },
        commandText = sdkCommandText(CommandCode.DISABLE_ACK_BEEP, "%ACKBEEP#0"),
        refreshCachedState = true,
        requiredOperation = DemoSessionOperation.DISABLE_ACK_BEEP,
    ) { it.disableAckBeep() }

    fun vibrateOn() = runAction(
        actionLabelProvider = { DemoStrings.text(R.string.vibrate_on_action) },
        successLogProvider = { DemoStrings.text(R.string.vibrate_on_command_sent) },
        commandText = sdkCommandText(CommandCode.VIBRATE_ON, "\$MOTO#0"),
        refreshCachedState = true,
        requiredOperation = DemoSessionOperation.VIBRATE_ON,
    ) { it.vibrateOn() }

    fun vibrateOff() = runAction(
        actionLabelProvider = { DemoStrings.text(R.string.vibrate_off_action) },
        successLogProvider = { DemoStrings.text(R.string.vibrate_off_command_sent) },
        commandText = sdkCommandText(CommandCode.VIBRATE_OFF, "\$MOTO#1"),
        refreshCachedState = true,
        requiredOperation = DemoSessionOperation.VIBRATE_OFF,
    ) { it.vibrateOff() }

    fun performQuickAction(action: QuickActionCommand) {
        when (action) {
            QuickActionCommand.REFRESH_INFO -> requestInfo()
            QuickActionCommand.GET_BATTERY -> requestBatteryLevel()
            QuickActionCommand.ACK_BEEP_ON -> beep()
            QuickActionCommand.ACK_BEEP_OFF -> disableAckBeep()
            QuickActionCommand.VIBRATE_ON -> vibrateOn()
            QuickActionCommand.VIBRATE_OFF -> vibrateOff()
            QuickActionCommand.NTC06H_SAVE -> saveNtc06hSettings(DemoStrings.text(R.string.save_ntc06h_settings))
            QuickActionCommand.NTC06H_ACK_ON -> writeNtc06hSetting(
                settingKey = "ack_02421",
                label = DemoStrings.text(R.string.ntc06h_enable_setting_ack),
                saveAfterWrite = false,
            )
            QuickActionCommand.NTC06H_CODE39_ON -> executeNtc06hQuickSetting(
                settingKey = "symbology_linear_00221",
                label = DemoStrings.text(R.string.ntc06h_enable_code39)
            )
            QuickActionCommand.NTC06H_CODE39_OFF -> executeNtc06hQuickSetting(
                settingKey = "symbology_linear_00220",
                label = DemoStrings.text(R.string.ntc06h_disable_code39)
            )
            QuickActionCommand.NTC06H_EAN13_ON -> executeNtc06hQuickSetting(
                settingKey = "symbology_upc_ean_00361",
                label = DemoStrings.text(R.string.ntc06h_enable_ean13)
            )
            QuickActionCommand.NTC06H_EAN13_OFF -> executeNtc06hQuickSetting(
                settingKey = "symbology_upc_ean_00360",
                label = DemoStrings.text(R.string.ntc06h_disable_ean13)
            )
            QuickActionCommand.NTC06H_CODE128_ON -> executeNtc06hQuickSetting(
                settingKey = "symbology_linear_00691",
                label = DemoStrings.text(R.string.ntc06h_enable_code128)
            )
            QuickActionCommand.NTC06H_CODE128_OFF -> executeNtc06hQuickSetting(
                settingKey = "symbology_linear_00690",
                label = DemoStrings.text(R.string.ntc06h_disable_code128)
            )
            QuickActionCommand.NT212X_QR_READ -> executeNt212xQrCodeSwitch(readOnly = true, enabled = false)
            QuickActionCommand.NT212X_QR_ON -> executeNt212xQrCodeSwitch(readOnly = false, enabled = true)
            QuickActionCommand.NT212X_QR_OFF -> executeNt212xQrCodeSwitch(readOnly = false, enabled = false)
            QuickActionCommand.NT280H_SCAN_KEY_MODE -> executeNt280hQuickParameter(
                parameterId = 0xA102,
                label = DemoStrings.text(R.string.nt280h_switch_to_trigger_scan),
                payload = 0x01,
            )
            QuickActionCommand.NT280H_SCAN_AUTO_MODE -> executeNt280hQuickParameter(
                parameterId = 0xA102,
                label = DemoStrings.text(R.string.nt280h_switch_to_auto_scan),
                payload = 0x02,
            )
            QuickActionCommand.NT280H_SCAN_CONTINUOUS_MODE -> executeNt280hQuickParameter(
                parameterId = 0xA102,
                label = DemoStrings.text(R.string.nt280h_switch_to_continuous_scan),
                payload = 0x03,
            )
            QuickActionCommand.NT280H_SLEEP_NEVER -> executeNt280hQuickParameter(
                parameterId = 0xA107,
                label = DemoStrings.text(R.string.nt280h_set_never_sleep),
                payload = 0x01,
            )
            QuickActionCommand.NT280H_SLEEP_10S -> executeNt280hQuickParameter(
                parameterId = 0xA107,
                label = DemoStrings.text(R.string.nt280h_set_sleep_10s),
                payload = 0x07,
            )
            QuickActionCommand.NT280H_DUPLICATE_500MS -> executeNt280hQuickParameter(
                parameterId = 0xA108,
                label = DemoStrings.text(R.string.nt280h_set_duplicate_delay_500ms),
                payload = 0x05,
            )
            QuickActionCommand.NT280H_LIGHT_HIGH -> executeNt280hQuickParameter(
                parameterId = 0xA109,
                label = DemoStrings.text(R.string.nt280h_set_high_illumination),
                payload = 0x03,
            )
            QuickActionCommand.NT280H_SENSITIVITY_HIGH -> executeNt280hQuickParameter(
                parameterId = 0xA10A,
                label = DemoStrings.text(R.string.nt280h_set_high_sensitivity),
                payload = 0x03,
            )
            QuickActionCommand.SE4750_CAPABILITIES_READ -> executeSe4750ModuleCommand(
                kind = ModuleCommandKind.CAPABILITIES_REQUEST,
                label = DemoStrings.text(R.string.se4750_read_capabilities)
            )
            QuickActionCommand.SE4750_AIM_ON -> executeSe4750ModuleCommand(
                kind = ModuleCommandKind.AIM_ON,
                label = DemoStrings.text(R.string.se4750_enable_aim_pattern)
            )
            QuickActionCommand.SE4750_AIM_OFF -> executeSe4750ModuleCommand(
                kind = ModuleCommandKind.AIM_OFF,
                label = DemoStrings.text(R.string.se4750_disable_aim_pattern)
            )
            QuickActionCommand.SE4750_ILLUMINATION_ON -> executeSe4750ModuleCommand(
                kind = ModuleCommandKind.ILLUMINATION_ON,
                label = DemoStrings.text(R.string.se4750_enable_illumination)
            )
            QuickActionCommand.SE4750_ILLUMINATION_OFF -> executeSe4750ModuleCommand(
                kind = ModuleCommandKind.ILLUMINATION_OFF,
                label = DemoStrings.text(R.string.se4750_disable_illumination)
            )
            QuickActionCommand.SE4750_ALL_SYMBOLOGY_ON -> executeSe4750ModuleCommand(
                kind = ModuleCommandKind.CHANGE_ALL_CODE_TYPES,
                label = DemoStrings.text(R.string.se4750_enable_all_symbologies),
                payloadBytes = byteArrayOf(0x01)
            )
            QuickActionCommand.SE4750_ALL_SYMBOLOGY_OFF -> executeSe4750ModuleCommand(
                kind = ModuleCommandKind.CHANGE_ALL_CODE_TYPES,
                label = DemoStrings.text(R.string.se4750_disable_all_symbologies),
                payloadBytes = byteArrayOf(0x00)
            )
            QuickActionCommand.SE4750_BEEP -> executeSe4750ModuleCommand(
                kind = ModuleCommandKind.BEEP,
                label = DemoStrings.text(R.string.se4750_trigger_beep)
            )
            QuickActionCommand.SE4750_PAGER -> executeSe4750ModuleCommand(
                kind = ModuleCommandKind.PAGER_MOTOR_ACTIVATION,
                label = DemoStrings.text(R.string.se4750_trigger_pager_motor)
            )
        }
    }

    private fun executeNtc06hQuickSetting(
        settingKey: String,
        label: String,
    ) {
        writeNtc06hSetting(
            settingKey = settingKey,
            label = label,
            saveAfterWrite = true,
        )
    }

    private fun executeNt212xQrCodeSwitch(readOnly: Boolean, enabled: Boolean) {
        val label = if (readOnly) {
            DemoStrings.text(R.string.nt212x_read_qr_code_switch)
        } else if (enabled) {
            DemoStrings.text(R.string.nt212x_enable_qr_code)
        } else {
            DemoStrings.text(R.string.nt212x_disable_qr_code)
        }
        if (readOnly) {
            readNt212xParameter(
                parameterId = 0xF025,
                label = label,
                parameterKind = ModuleParameterUiKind.BOOL,
            )
        } else {
            writeNt212xParameter(
                parameterId = 0xF025,
                label = label,
                payloadBytes = byteArrayOf((if (enabled) 0x01 else 0x00).toByte()),
                persist = true,
                parameterKind = ModuleParameterUiKind.BOOL,
            )
        }
    }

    private fun executeNt280hQuickParameter(
        parameterId: Int,
        label: String,
        payload: Int,
    ) {
        writeModuleParameter(
            family = ModuleFamily.NT280H,
            parameterId = parameterId,
            label = label,
            payloadBytes = byteArrayOf(payload.toByte()),
            persist = true,
            parameterKind = ModuleParameterUiKind.ENUM,
        )
    }

    private fun executeSe4750ModuleCommand(
        kind: ModuleCommandKind,
        label: String,
        payloadBytes: ByteArray = byteArrayOf(),
    ) {
        executeModuleParameter(
            kind = kind,
            family = ModuleFamily.SE4750,
            parameterId = 0,
            payloadBytes = payloadBytes,
            persist = false,
            label = label,
            parameterKind = ModuleParameterUiKind.UNKNOWN,
        )
    }

    fun readNt212xParameter(
        parameterId: Int,
        label: String,
        parameterKind: ModuleParameterUiKind = ModuleParameterUiKind.UNKNOWN,
    ) {
        executeModuleParameter(
            kind = ModuleCommandKind.READ_PARAMETER,
            family = ModuleFamily.NT212X,
            parameterId = parameterId,
            payloadBytes = byteArrayOf(),
            persist = false,
            label = label,
            parameterKind = parameterKind,
            labelProvider = { ModuleCommandPresentation.parameterCommandLabel(ModuleCommandKind.READ_PARAMETER, ModuleFamily.NT212X, parameterId, label) },
        )
    }

    fun writeNt212xParameter(
        parameterId: Int,
        label: String,
        payloadBytes: ByteArray,
        persist: Boolean,
        parameterKind: ModuleParameterUiKind = ModuleParameterUiKind.UNKNOWN,
    ) {
        executeModuleParameter(
            kind = ModuleCommandKind.WRITE_PARAMETER,
            family = ModuleFamily.NT212X,
            parameterId = parameterId,
            payloadBytes = payloadBytes,
            persist = persist,
            label = label,
            parameterKind = parameterKind,
            labelProvider = { ModuleCommandPresentation.parameterCommandLabel(ModuleCommandKind.WRITE_PARAMETER, ModuleFamily.NT212X, parameterId, label) },
        )
    }

    fun readModuleParameter(
        family: ModuleFamily,
        parameterId: Int,
        label: String,
        parameterKind: ModuleParameterUiKind = ModuleParameterUiKind.UNKNOWN,
    ) {
        executeModuleParameter(
            kind = ModuleCommandKind.READ_PARAMETER,
            family = family,
            parameterId = parameterId,
            payloadBytes = byteArrayOf(),
            persist = false,
            label = label,
            parameterKind = parameterKind,
            labelProvider = { ModuleCommandPresentation.parameterCommandLabel(ModuleCommandKind.READ_PARAMETER, family, parameterId, label) },
        )
    }

    fun writeModuleParameter(
        family: ModuleFamily,
        parameterId: Int,
        label: String,
        payloadBytes: ByteArray,
        persist: Boolean,
        parameterKind: ModuleParameterUiKind = ModuleParameterUiKind.UNKNOWN,
    ) {
        executeModuleParameter(
            kind = ModuleCommandKind.WRITE_PARAMETER,
            family = family,
            parameterId = parameterId,
            payloadBytes = payloadBytes,
            persist = persist,
            label = label,
            parameterKind = parameterKind,
            labelProvider = { ModuleCommandPresentation.parameterCommandLabel(ModuleCommandKind.WRITE_PARAMETER, family, parameterId, label) },
        )
    }

    fun writeNtc06hSetting(
        settingKey: String,
        label: String,
        saveAfterWrite: Boolean,
    ) {
        val setting = Ntc06hSettingCatalog.findByKey(settingKey)
            ?: run {
                reportError({ label }, IllegalArgumentException(DemoStrings.text(R.string.unknown_ntc06h_setting_key)))
                return
            }
        executeNtc06hWrite(
            settingCode = setting.settingCode,
            displayCode = setting.displayCode,
            debugTarget = "setting=${setting.displayCode}",
            label = label,
            labelProvider = { ModuleCommandPresentation.ntc06hSettingLabel(settingKey, label) },
            saveAfterWrite = saveAfterWrite,
        )
    }

    fun writeNtc06hRawSetting(
        settingCode: String,
        label: String,
        saveAfterWrite: Boolean,
    ) {
        executeNtc06hWrite(
            settingCode = settingCode,
            displayCode = settingCode,
            debugTarget = "rawSetting=$settingCode",
            label = label,
            labelProvider = { label },
            saveAfterWrite = saveAfterWrite,
        )
    }

    private fun executeNtc06hWrite(
        settingCode: String,
        displayCode: String,
        debugTarget: String,
        label: String,
        labelProvider: () -> String,
        saveAfterWrite: Boolean,
    ) {
        launchSerializedCommand {
            val session = requireBoundSession() ?: run {
                appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, DemoStrings.text(R.string.no_active_session))
                return@launchSerializedCommand
            }
            if (!ensureModuleCommandExecutionReady(label, labelProvider)) {
                return@launchSerializedCommand
            }
            appendEvent(
                DebugEventSource.COMMAND,
                DebugEventLevel.Debug,
                "$label: family=${ModuleFamily.NTC06H} kind=${ModuleCommandKind.WRITE_PARAMETER} $debugTarget saveAfter=$saveAfterWrite"
            )
            runCatching {
                withContext(Dispatchers.IO) {
                    val response = session.writeNtc06hSetting(settingCode)
                    if (saveAfterWrite) {
                        response to session.saveNtc06hSettings()
                    } else {
                        response to null
                    }
                }
            }.onSuccess { (writeResponse, saveResponse) ->
                reportAction {
                    "${labelProvider()} ${DemoStrings.text(R.string.command_completed)}"
                }
                appendEvent(
                    DebugEventSource.COMMAND,
                    DebugEventLevel.Info,
                    formatCommandResponse("$label ${DemoStrings.text(R.string.command_write)}", writeResponse)
                )
                saveResponse?.let {
                    appendEvent(
                        DebugEventSource.COMMAND,
                        DebugEventLevel.Info,
                        formatCommandResponse("$label ${DemoStrings.text(R.string.command_save)}", it)
                    )
                }
            }.onFailure { error ->
                if (handleNtc06hWriteTimeout(label, labelProvider, displayCode, saveAfterWrite, error)) {
                    return@launchSerializedCommand
                }
                reportError({
                    "${labelProvider()} ${DemoStrings.text(R.string.command_response_failed)}"
                }, error)
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Error, "$label ${DemoStrings.text(R.string.command_response_failed)}: ${error.demoErrorDetail()}")
            }
        }
    }

    fun saveNtc06hSettings(label: String) {
        val labelProvider = { DemoStrings.text(R.string.save_ntc06h_settings) }
        launchSerializedCommand {
            val session = requireBoundSession() ?: run {
                appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, DemoStrings.text(R.string.no_active_session))
                return@launchSerializedCommand
            }
            if (!ensureModuleCommandExecutionReady(label, labelProvider)) {
                return@launchSerializedCommand
            }
            appendEvent(
                DebugEventSource.COMMAND,
                DebugEventLevel.Debug,
                "$label: family=${ModuleFamily.NTC06H} kind=${ModuleCommandKind.SAVE_SETTINGS}"
            )
            runCatching {
                withContext(Dispatchers.IO) { session.saveNtc06hSettings() }
            }.onSuccess { response ->
                reportAction {
                    "${labelProvider()} ${DemoStrings.text(R.string.command_completed)}"
                }
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Info, formatCommandResponse(label, response))
            }.onFailure { error ->
                reportError({
                    "${labelProvider()} ${DemoStrings.text(R.string.command_response_failed)}"
                }, error)
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Error, "$label ${DemoStrings.text(R.string.command_response_failed)}: ${error.demoErrorDetail()}")
            }
        }
    }

    fun executeBasicDeviceCommand(command: BasicDeviceCommand) {
        launchSerializedCommand {
            val session = requireBoundSession() ?: run {
                appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, DemoStrings.text(R.string.no_active_session))
                return@launchSerializedCommand
            }
            if (!ensureOperationSupport(
                    session = session,
                    operation = DemoSessionOperation.BASIC_DEVICE_COMMANDS,
                    labelProvider = { basicCommandLabel(command) }
                )
            ) {
                return@launchSerializedCommand
            }
            appendCommandSend(basicCommandText(command), DemoStrings.text(R.string.send_basic_command))
            runCatching {
                withContext(Dispatchers.IO) { session.executeBasicDeviceCommand(command) }
            }.onSuccess { response ->
                val commandDisplay = basicCommandLabel(command)
                reportAction {
                    "${DemoStrings.text(R.string.basic_command_completed)}: ${basicCommandLabel(command)}"
                }
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Info, formatCommandResponse("${DemoStrings.text(R.string.basic_command)} $commandDisplay", response))
            }.onFailure { error ->
                val commandDisplay = basicCommandLabel(command)
                reportError({
                    "${DemoStrings.text(R.string.basic_command_execution_failed)}: ${basicCommandLabel(command)}"
                }, error)
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Error, "${DemoStrings.text(R.string.basic_command_failed)}: $commandDisplay: ${error.demoErrorDetail()}")
            }
        }
    }

    fun executeMasterCommand(command: MasterCommand) {
        launchSerializedCommand {
            val session = requireBoundSession() ?: run {
                appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, DemoStrings.text(R.string.no_active_session))
                return@launchSerializedCommand
            }
            if (!ensureMasterCommandSupport(session, command) { masterCommandLabel(command) }) {
                return@launchSerializedCommand
            }
            appendCommandSend(masterCommandText(command), DemoStrings.text(R.string.send_master_command))
            runCatching {
                withContext(Dispatchers.IO) { session.executeMasterCommand(command) }
            }.onSuccess { response ->
                val commandDisplay = masterCommandLabel(command)
                applyCachedSuccessState(
                    session = session,
                    actionResult = "${DemoStrings.text(R.string.master_command_completed)}: $commandDisplay",
                    actionResultProvider = {
                        "${DemoStrings.text(R.string.master_command_completed)}: ${masterCommandLabel(command)}"
                    }
                )
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Info, formatCommandResponse("${DemoStrings.text(R.string.master_command)} $commandDisplay", response))
            }.onFailure { error ->
                val commandDisplay = masterCommandLabel(command)
                reportError({
                    "${DemoStrings.text(R.string.master_command_execution_failed)}: ${masterCommandLabel(command)}"
                }, error)
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Error, "${DemoStrings.text(R.string.master_command_failed)}: $commandDisplay: ${error.demoErrorDetail()}")
            }
        }
    }

    fun executeTextCommand(label: String, commandText: String) {
        launchSerializedCommand {
            val session = requireBoundSession() ?: run {
                appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, DemoStrings.text(R.string.no_active_session))
                return@launchSerializedCommand
            }
            if (!ensureOperationSupport(
                    session = session,
                    operation = DemoSessionOperation.TEXT_COMMANDS,
                    labelProvider = { label }
                )
            ) {
                return@launchSerializedCommand
            }
            appendCommandSend(commandText, DemoStrings.text(R.string.send_text_command))
            runCatching {
                withContext(Dispatchers.IO) { session.executeTextCommand(commandText) }
            }.onSuccess { response ->
                applyCachedSuccessState(session, "${DemoStrings.text(R.string.text_command_completed)}: $label")
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Info, formatCommandResponse("${DemoStrings.text(R.string.text_command)} $label", response))
            }.onFailure { error ->
                reportError({ "${DemoStrings.text(R.string.text_command_execution_failed)}: $label" }, error)
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Error, "${DemoStrings.text(R.string.text_command_failed)}: $label: ${error.demoErrorDetail()}")
            }
        }
    }

    fun executeDataRuleCommand(label: String, command: DataRuleCommand) {
        val labelProvider = { dataRuleCommandLabel(command, label) }
        launchSerializedCommand {
            val session = requireBoundSession() ?: run {
                appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, DemoStrings.text(R.string.no_active_session))
                return@launchSerializedCommand
            }
            if (!ensureOperationSupport(
                    session = session,
                    operation = DemoSessionOperation.DATA_RULE_COMMANDS,
                    labelProvider = labelProvider
                )
            ) {
                return@launchSerializedCommand
            }
            runCatching {
                withContext(Dispatchers.IO) { session.executeDataRuleCommand(command) }
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

    fun setScanTerminator(terminatorBytes: ByteArray) {
        val session = requireBoundSession() ?: run {
            appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, DemoStrings.text(R.string.no_active_session))
            return
        }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { session.setScanTerminator(terminatorBytes) }
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
                DemoDiagnosticsStore.updateSessionState(state)
                updateUiState {
                    copy(
                        statusSummary = statusText(state),
                        hasReadySession = state == SessionState.READY,
                        diagnosticsSummary = DemoDiagnosticsStore.summaryText(),
                    )
                }
                if (state == SessionState.READY) {
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
                updateUiState {
                    copy(canExecuteModuleCommands = if (state == SessionState.READY) canExecuteModuleCommands else false)
                }
                appendEvent(DebugEventSource.SESSION, DebugEventLevel.Info, "${DemoStrings.text(R.string.session_state)}: $state")
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
                appendEvent(DebugEventSource.SCAN, DebugEventLevel.Info, "${DemoStrings.text(R.string.log_source_scan)}: ${escapeControlText(event.text)}")
            },
            onFailure = { failure ->
                handleSessionFailure(failure)
            },
        )
    }

    private fun runAction(
        actionLabelProvider: (() -> String)? = null,
        successLogProvider: () -> String,
        commandText: String? = null,
        refreshCachedState: Boolean = false,
        requiredOperation: DemoSessionOperation? = null,
        block: suspend (com.netumscan.scannersdk.ScannerSession) -> Unit
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

    private suspend fun ensureMasterCommandSupport(
        session: ScannerSession,
        command: MasterCommand,
        labelProvider: () -> String,
    ): Boolean {
        return commandRunner.ensureMasterCommandSupport(
            session = session,
            command = command,
            labelProvider = labelProvider,
            unsupportedReasonProvider = ::unsupportedMasterCommandReason,
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

    private fun applyFakeSessionState(preferredModelId: DeviceModelId) {
        val handle = boundSessionHandle ?: return
        val fakeCapability = (handle as? FakeDemoSessionHandle)?.capabilitySummary
        DemoDiagnosticsStore.updateSessionState(handle.state.value)
        DemoDiagnosticsStore.updateResolvedModel(preferredModelId)
        updateUiState {
            copy(
                statusSummary = statusText(handle.state.value),
                hasReadySession = handle.state.value == SessionState.READY,
                sdkResolvedModelSummary = formatSdkResolvedModelSummaryText(preferredModelId, preferredModelId),
                capabilitySummary = fakeCapability?.let(::formatCapabilitySummary)
                    ?: uiText(R.string.capability_summary_not_loaded),
                moduleSummary = fakeCapability?.let(::formatModuleSummary)
                    ?: uiText(R.string.module_capability_not_loaded),
                currentModuleFamily = fakeCapability?.moduleFamily ?: ModuleFamily.UNKNOWN,
                supportsModuleCommands = fakeCapability?.supportsModuleCommands ?: false,
                canExecuteModuleCommands = fakeCapability?.supportsModuleCommands ?: false,
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
                hasReadySession = false,
                lastActionResult = lastAction,
                errorMessage = null,
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
            append(transportFailureCodeSummary(failure.code))
            failure.bleTransportIssue?.let { issue -> append(", ble=").append(bleTransportIssueSummary(issue)) }
            failure.platformErrorCode?.let { code -> append(", raw=$code") }
            append("]")
        }
        DemoDiagnosticsStore.updateRecentFailure("${DemoStrings.text(R.string.connection_issue)}: $detail")
        updateUiState {
            copy(
                statusSummary = sessionFailureStatusSummary(failure),
                lastActionResult = localizedAction {
                    DemoStrings.text(R.string.device_connection_interrupted)
                },
                errorMessage = rawDisplayText(detail),
                diagnosticsSummary = DemoDiagnosticsStore.summaryText(),
            )
        }
        appendEvent(DebugEventSource.BLE, DebugEventLevel.Error, "${DemoStrings.text(R.string.connection_issue)}: $detail")
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
        val event = DebugEvent(source, level, message)
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

    private fun sessionFailureStatusSummary(failure: SessionFailure): UiText {
        val label = failure.code.localizedStatusLabel()
        return sdkText(label.localizationKey, label.fallbackDisplayName)
    }

    private fun sessionFailureActionSummaryText(failure: SessionFailure): UiText =
        joinedText(sessionFailureStatusSummary(failure), rawDisplayText(": ${failure.message}"))

    private fun bleTransportIssueSummary(issue: BleTransportIssue): String {
        val label = issue.localizedSdkLabel()
        return SdkLabelResolver.resolve(label.localizationKey, label.fallbackDisplayName)
    }

    private fun transportFailureCodeSummary(code: TransportFailureCode): String {
        val label = code.localizedSdkLabel()
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

    private fun localizedError(provider: () -> String): UiText {
        errorMessageProvider = provider
        return dynamicText(provider)
    }

    private fun handleNtc06hWriteTimeout(
        label: String,
        labelProvider: () -> String,
        settingCode: String,
        saveAfterWrite: Boolean,
        error: Throwable,
    ): Boolean {
        if (!moduleCommandRunner.isNtc06hSilentAckTimeout(error)) {
            return false
        }
        val messageProvider = {
            ModuleCommandPresentation.ntc06hWriteTimeoutMessage(labelProvider(), settingCode, saveAfterWrite)
        }
        val message = messageProvider()
        reportAction(messageProvider)
        appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Warn, message)
        appendEvent(DebugEventSource.SDK, DebugEventLevel.Warn, "${label}: ${error.demoErrorDetail()}")
        return true
    }

    private fun appendCommandSend(commandText: String, prefix: String = DemoStrings.text(R.string.send_command)) {
        appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Debug, "$prefix: $commandText frameHex=${toHex(encodeSsiCommand(commandText))}")
    }

    private fun toHex(bytes: ByteArray): String {
        return bytes.joinToString(" ") { byte ->
            "%02X".format(byte.toInt() and 0xFF)
        }
    }

    private fun encodeSsiCommand(commandText: String): ByteArray {
        val payload = commandText.toByteArray(StandardCharsets.US_ASCII)
        val frame = ArrayList<Byte>(payload.size + 6)
        frame.add(0x02)
        frame.add((payload.size + 4).toByte())
        frame.add(0x0A)
        payload.forEach { frame.add(it) }

        val checksum = checksum(frame.drop(1).toByteArray())
        frame.add(((checksum ushr 8) and 0xFF).toByte())
        frame.add((checksum and 0xFF).toByte())
        frame.add(0x03)
        return frame.toByteArray()
    }

    private fun checksum(bytes: ByteArray): Int {
        var sum = 0
        var weight = bytes.size
        bytes.forEach { byte ->
            sum += (byte.toInt() and 0xFF) * weight
            if (weight > 0) {
                weight -= 1
            }
        }
        return (0x10000 - (sum and 0xFFFF)) and 0xFFFF
    }

    private fun ByteArray.toHexSummary(): String {
        return joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
    }

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

    private fun applyDeviceState(
        snapshot: DeviceSnapshot,
        effectiveCapability: DeviceCapabilitySummary,
        canExecuteModuleCommands: Boolean,
    ) {
        val summary = DeviceStateSummaryFormatter.format(snapshot.info)
        val selectedModelId = preferredModelId()
        DemoDiagnosticsStore.updateSelectedModel(selectedModelId)
        DemoDiagnosticsStore.updateResolvedModel(snapshot.resolvedModel)

        updateUiState {
            copy(
                infoSummary = summary.infoSummary,
                selectedModelSummary = formatSelectedModelSummaryText(selectedModelId),
                sdkResolvedModelSummary = formatSdkResolvedModelSummaryText(selectedModelId, snapshot.resolvedModel),
                capabilitySummary = formatCapabilitySummary(effectiveCapability),
                moduleSummary = formatModuleSummary(effectiveCapability),
                currentModuleFamily = effectiveCapability.moduleFamily,
                supportsModuleCommands = effectiveCapability.supportsModuleCommands,
                canExecuteModuleCommands = canExecuteModuleCommands,
                deviceCharsetSummary = summary.deviceCharsetSummary,
                deviceTerminalSummary = summary.deviceTerminalSummary,
                diagnosticsSummary = DemoDiagnosticsStore.summaryText(),
            )
        }
    }

    private suspend fun refreshCachedState(session: ScannerSession) {
        runCatching {
            withContext(Dispatchers.IO) {
                DeviceSnapshot(
                    info = session.getCachedInfo(),
                    batteryInfo = com.netumscan.scannersdk.model.BatteryInfo(rawText = "", voltageText = "", percent = -1),
                    modelConfigApplied = false,
                    resolvedModel = session.getResolvedModelId(),
                    capability = session.getDeviceCapabilitySummary(),
                )
            }
        }.onSuccess { snapshot ->
            val selectedModelId = preferredModelId()
            val effectiveCapability = effectiveCapabilityFor(selectedModelId, snapshot.capability)
            val canExecuteModuleCommands = queryModuleCommandAvailability(session)
            val summary = CachedDeviceStateSummaryLoader.load(
                infoProvider = { snapshot.info },
            )
            updateUiState {
                copy(
                    infoSummary = summary.infoSummary,
                    selectedModelSummary = formatSelectedModelSummaryText(selectedModelId),
                    sdkResolvedModelSummary = formatSdkResolvedModelSummaryText(selectedModelId, snapshot.resolvedModel),
                    capabilitySummary = formatCapabilitySummary(effectiveCapability),
                    moduleSummary = formatModuleSummary(effectiveCapability),
                    currentModuleFamily = effectiveCapability.moduleFamily,
                    supportsModuleCommands = effectiveCapability.supportsModuleCommands,
                    canExecuteModuleCommands = canExecuteModuleCommands,
                    deviceCharsetSummary = summary.deviceCharsetSummary,
                    deviceTerminalSummary = summary.deviceTerminalSummary,
                )
            }
        }.onFailure { error ->
            appendEvent(DebugEventSource.SDK, DebugEventLevel.Warn, "${DemoStrings.text(R.string.refresh_cached_state_failed)}: ${error.demoErrorDetail()}")
        }
    }

    private suspend fun refreshLocalizedCachedState(session: ScannerSession) {
        runCatching {
            withContext(Dispatchers.IO) {
                DeviceSnapshot(
                    info = session.getCachedInfo(),
                    batteryInfo = com.netumscan.scannersdk.model.BatteryInfo(rawText = "", voltageText = "", percent = -1),
                    modelConfigApplied = false,
                    resolvedModel = session.getResolvedModelId(),
                    capability = session.getDeviceCapabilitySummary(),
                )
            }
        }.onSuccess { snapshot ->
            val selectedModelId = preferredModelId()
            val effectiveCapability = effectiveCapabilityFor(selectedModelId, snapshot.capability)
            val summary = CachedDeviceStateSummaryLoader.load(
                infoProvider = { snapshot.info },
            )
            val canExecuteModuleCommands = session.state.value == SessionState.READY &&
                _uiState.value.canExecuteModuleCommands
            updateUiState {
                copy(
                    infoSummary = summary.infoSummary,
                    selectedModelSummary = formatSelectedModelSummaryText(selectedModelId),
                    sdkResolvedModelSummary = formatSdkResolvedModelSummaryText(selectedModelId, snapshot.resolvedModel),
                    capabilitySummary = formatCapabilitySummary(effectiveCapability),
                    moduleSummary = formatModuleSummary(effectiveCapability),
                    currentModuleFamily = effectiveCapability.moduleFamily,
                    supportsModuleCommands = effectiveCapability.supportsModuleCommands,
                    canExecuteModuleCommands = canExecuteModuleCommands,
                    deviceCharsetSummary = summary.deviceCharsetSummary,
                    deviceTerminalSummary = summary.deviceTerminalSummary,
                )
            }
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
                val snapshot = DeviceSnapshot(
                    info = session.getCachedInfo(),
                    batteryInfo = com.netumscan.scannersdk.model.BatteryInfo(rawText = "", voltageText = "", percent = -1),
                    modelConfigApplied = false,
                    resolvedModel = session.getResolvedModelId(),
                    capability = session.getDeviceCapabilitySummary(),
                )
                snapshot to CachedCommandSuccessStateLoader.load(
                    actionResult = actionResult,
                    summaryProvider = {
                        CachedDeviceStateSummaryLoader.load(
                            infoProvider = { snapshot.info },
                        )
                    },
                )
            }
        }.onSuccess { (snapshot, state) ->
            val selectedModelId = preferredModelId()
            val effectiveCapability = effectiveCapabilityFor(selectedModelId, snapshot.capability)
            val canExecuteModuleCommands = queryModuleCommandAvailability(session)
            updateUiState {
                copy(
                    infoSummary = state.infoSummary,
                    selectedModelSummary = formatSelectedModelSummaryText(selectedModelId),
                    sdkResolvedModelSummary = formatSdkResolvedModelSummaryText(selectedModelId, snapshot.resolvedModel),
                    capabilitySummary = formatCapabilitySummary(effectiveCapability),
                    moduleSummary = formatModuleSummary(effectiveCapability),
                    currentModuleFamily = effectiveCapability.moduleFamily,
                    supportsModuleCommands = effectiveCapability.supportsModuleCommands,
                    canExecuteModuleCommands = canExecuteModuleCommands,
                    deviceCharsetSummary = state.deviceCharsetSummary,
                    deviceTerminalSummary = state.deviceTerminalSummary,
                    lastActionResult = actionResultProvider?.let(::localizedAction) ?: rawDisplayText(state.lastActionResult),
                    errorMessage = null,
                )
            }
        }.onFailure { error ->
            appendEvent(DebugEventSource.SDK, DebugEventLevel.Warn, "${DemoStrings.text(R.string.refresh_cached_state_failed)}: ${error.demoErrorDetail()}")
            if (actionResultProvider == null) {
                reportRawAction(actionResult)
            } else {
                reportAction(actionResultProvider)
            }
        }
    }

    private suspend fun queryModuleCommandAvailability(
        session: ScannerSession,
    ): Boolean {
        if (session.state.value != SessionState.READY) {
            return false
        }
        return runCatching {
            session.getOperationSupportSummary().supportsDefaultModuleCommandProbe
        }.getOrDefault(false)
    }

    private fun executeModuleParameter(
        kind: ModuleCommandKind,
        family: ModuleFamily,
        parameterId: Int,
        payloadBytes: ByteArray,
        persist: Boolean,
        label: String,
        parameterKind: ModuleParameterUiKind,
        labelProvider: (() -> String)? = null,
    ) {
        val dynamicLabel = labelProvider ?: { label }
        launchSerializedCommand {
            val session = requireBoundSession() ?: run {
                appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, DemoStrings.text(R.string.no_active_session))
                return@launchSerializedCommand
            }
            if (!moduleCommandRunner.ensureReady(label, dynamicLabel)) {
                return@launchSerializedCommand
            }
            appendEvent(
                DebugEventSource.COMMAND,
                DebugEventLevel.Debug,
                ModuleCommandPresentation.parameterDebugSummary(
                    label = label,
                    family = family,
                    kind = kind,
                    parameterId = parameterId,
                    payloadBytes = payloadBytes,
                    persist = persist,
                )
            )
            runCatching {
                moduleCommandRunner.execute(
                    session = session,
                    request = ModuleCommandRequest(
                        family = family,
                        kind = kind,
                        parameterId = parameterId,
                        payloadBytes = payloadBytes,
                        persist = persist,
                    ),
                )
            }.onSuccess { response ->
                val responseSummary = ModuleCommandPresentation.moduleResponse(
                    label = label,
                    family = family,
                    parameterId = parameterId,
                    parameterKind = parameterKind,
                    response = response,
                    commandTextCharset = currentCommandTextDisplayCharset(),
                    recordCharset = currentRecordDisplayCharset(),
                )
                reportAction {
                    "${dynamicLabel()} ${DemoStrings.text(R.string.command_completed)}"
                }
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Info, responseSummary)
            }.onFailure { error ->
                reportError({
                    "${dynamicLabel()} ${DemoStrings.text(R.string.command_response_failed)}"
                }, error)
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Error, "$label ${DemoStrings.text(R.string.command_response_failed)}: ${error.demoErrorDetail()}")
            }
        }
    }

    private suspend fun executeModuleCommand(
        session: ScannerSession,
        family: ModuleFamily,
        kind: ModuleCommandKind,
        parameterId: Int,
        payloadBytes: ByteArray,
        persist: Boolean,
    ): CommandResponse {
        return moduleCommandRunner.execute(
            session,
            ModuleCommandRequest(
                family = family,
                kind = kind,
                parameterId = parameterId,
                payloadBytes = payloadBytes,
                persist = persist,
            ),
        )
    }

    private fun dataRuleCommandLabel(command: DataRuleCommand, fallback: String): String {
        val kind = when (command) {
            is DataRuleCommand.SetSuffix -> DataRuleKind.SUFFIX
            is DataRuleCommand.SetPrefix -> DataRuleKind.PREFIX
            is DataRuleCommand.HideEnd -> DataRuleKind.HIDE_END
            is DataRuleCommand.HideMiddle -> DataRuleKind.HIDE_MIDDLE
            is DataRuleCommand.HideStart -> DataRuleKind.HIDE_START
            is DataRuleCommand.Replace -> DataRuleKind.REPLACE
        }
        val label = kind.localizedSdkLabel()
        return SdkLabelResolver.resolve(label.localizationKey, label.fallbackDisplayName).ifBlank { fallback }
    }

    private fun ensureModuleCommandExecutionReady(
        label: String,
        labelProvider: () -> String,
    ): Boolean {
        if (_uiState.value.canExecuteModuleCommands) {
            return true
        }
        val reason = DemoStrings.text(R.string.module_command_not_ready_reason)
        reportError(labelProvider, IllegalStateException(reason))
        appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Warn, "$label: $reason")
        return false
    }

    private fun formatModuleSummary(capability: DeviceCapabilitySummary): UiText {
        val familyLabel = capability.moduleFamily.localizedSdkLabel()
        return uiText(
            R.string.module_summary_format,
            sdkText(familyLabel.localizationKey, familyLabel.fallbackDisplayName),
            supportFlagText(capability.supportsNativeModuleCommands),
            supportFlagText(capability.supportsModuleCommandBridge),
            supportFlagText(capability.supportsModuleCommands),
            supportFlagText(capability.supportsModulePassthrough),
        )
    }

    private fun basicCommandText(command: BasicDeviceCommand): String {
        return runCatching { ScannerSdk.getBasicDeviceCommandDescriptor(command).text }
            .getOrDefault(command.name)
    }

    private fun basicCommandLabel(command: BasicDeviceCommand): String {
        val label = command.localizedSdkLabel()
        return SdkLabelResolver.resolve(label.localizationKey, label.fallbackDisplayName)
    }

    private fun masterCommandText(command: MasterCommand): String {
        return runCatching { ScannerSdk.getMasterCommandDescriptor(command).text }
            .getOrDefault(command.name)
    }

    private fun masterCommandLabel(command: MasterCommand): String {
        val label = command.localizedSdkLabel()
        return SdkLabelResolver.resolve(label.localizationKey, label.fallbackDisplayName)
    }

    private fun sdkCommandText(command: CommandCode, fallback: String): String {
        return runCatching { ScannerSdk.getCommandDescriptor(command).text }.getOrDefault(fallback)
    }

    private fun statusText(state: SessionState): UiText {
        val label = state.localizedLabel()
        return sdkText(label.localizationKey, label.fallbackDisplayName)
    }

    private fun preferredModelId(): DeviceModelId = DemoSessionCoordinator.preferredModelId

    private fun loadCapabilityFor(modelId: DeviceModelId): DeviceCapabilitySummary? {
        if (modelId == DeviceModelId.UNKNOWN) {
            return null
        }
        return runCatching { ScannerSdk.getDeviceModelProfile(modelId)?.capability }.getOrNull()
    }

    private fun effectiveCapabilityFor(
        preferredModelId: DeviceModelId,
        fallbackCapability: DeviceCapabilitySummary,
    ): DeviceCapabilitySummary {
        return mergePreferredCapabilityWithRuntime(
            preferredCapability = loadCapabilityFor(preferredModelId),
            runtimeCapability = fallbackCapability,
        )
    }

    private data class DeviceSnapshot(
        val info: ScannerInfo,
        val batteryInfo: com.netumscan.scannersdk.model.BatteryInfo,
        val modelConfigApplied: Boolean,
        val resolvedModel: DeviceModelId,
        val capability: DeviceCapabilitySummary,
    )
}
