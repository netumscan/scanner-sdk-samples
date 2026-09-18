package com.netumscan.scannersdk.demo

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netumscan.scannersdk.ScannerSession
import com.netumscan.scannersdk.ScannerSdk
import com.netumscan.scannersdk.SessionState
import com.netumscan.scannersdk.TransportType
import com.netumscan.scannersdk.model.BleScanIssue
import com.netumscan.scannersdk.model.TransportIssue
import com.netumscan.scannersdk.model.DiscoveredDevice
import com.netumscan.scannersdk.model.DiscoveryFailureCode
import com.netumscan.scannersdk.model.DiscoveryFailure
import com.netumscan.scannersdk.model.ScanTextCharset
import com.netumscan.scannersdk.model.SessionFailure
import com.netumscan.scannersdk.model.localizedLabel as localizedSdkLabel
import com.netumscan.scannersdk.model.localizedStatusLabel as localizedSdkStatusLabel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun discoveryBlockerActionForFailure(
    failure: DiscoveryFailure,
    sdkInt: Int = Build.VERSION.SDK_INT,
): DiscoveryBlockerAction? {
    if (failure.recoverable) return null
    if (failure.code == DiscoveryFailureCode.BLE_ADAPTER_DISABLED ||
        failure.code == DiscoveryFailureCode.BLE_SCANNER_UNAVAILABLE
    ) {
        return DiscoveryBlockerAction.OPEN_BLUETOOTH_SETTINGS
    }
    if (sdkInt >= Build.VERSION_CODES.S &&
        failure.code in setOf(
            DiscoveryFailureCode.BLE_FILTERED_SCAN_FALLBACK_FAILED,
            DiscoveryFailureCode.BLE_UNFILTERED_SCAN_FAILED,
        ) &&
        failure.bleScanIssue in setOf(
            BleScanIssue.REGISTRATION_FAILED,
            BleScanIssue.INTERNAL_ERROR,
            BleScanIssue.OUT_OF_HARDWARE_RESOURCES,
            BleScanIssue.UNKNOWN,
        )
    ) {
        return DiscoveryBlockerAction.OPEN_LOCATION_SETTINGS
    }
    return null
}

class DemoViewModel(
    private val supportedModelsLoader: suspend (TransportType) -> List<DemoSupportedModel> = { transport ->
        ScannerSdk.getSupportedDeviceModels(transport).map { it.toDemoSupportedModel() }
    },
    private val supportedModelsDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private var selectedModelKey = "CS7501"
    private var selectedTransportMode: DemoTransportMode? = null
    private val bleDeviceStore = DemoDiscoveryDeviceStore()
    private val sppDeviceStore = DemoDiscoveryDeviceStore()
    private val _uiState = MutableStateFlow(DiscoveryUiState())
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()
    private val activeSession: ScannerSession?
        get() = DemoSessionCoordinator.activeSession
    private val activeSessionHandle: DemoSessionHandle?
        get() = DemoSessionCoordinator.activeSessionHandle
    private var discoveryCoordinator = DiscoveryCoordinator(RealDemoDiscoveryBackend)
    private var observingSdk = false
    private var sessionObservationJobs: List<Job> = emptyList()
    private var sdkDebugJob: Job? = null
    private var supportedModelsLoadJob: Job? = null
    private var isInitialized = false
    private var lastObservedSessionState: SessionState? = null
    private var lastActionResultProvider: (() -> String)? = null
    private var errorMessageProvider: (() -> String)? = null
    private var connectingDeviceId: String? = null

    init {
        DemoDiagnosticsStore.updateSelectedModel(selectedModelKey)
    }

    fun enableFakeMode() {
        if (isInitialized || hasActiveSession()) return
        discoveryCoordinator = DiscoveryCoordinator(FakeDemoDiscoveryBackend())
        DemoDiagnosticsStore.updateFakeMode(true)
        refreshDiagnosticsSummary()
        appendEvent(DebugEventSource.SDK, DebugEventLevel.Warn, "Fake demo mode enabled")
    }

    internal fun updatePlatformDiagnostics(platform: DemoPlatformDiagnostics) {
        DemoDiagnosticsStore.updatePlatform(platform)
        refreshDiagnosticsSummary()
    }

    fun initialize(context: Context) {
        if (isInitialized) {
            reportAction {
                DemoStrings.text(R.string.sdk_already_initialized)
            }
            return
        }
        viewModelScope.launch {
            runCatching {
                discoveryCoordinator.initialize(context)
            }.onSuccess {
                isInitialized = true
                DemoDiagnosticsStore.updateSdkInitialized(true)
                DemoDiagnosticsStore.updateFakeMode(discoveryCoordinator.isFakeMode)
                startObservingSdk()
                updateUiState {
                    copy(
                        statusSummary = uiText(R.string.sdk_initialized),
                        lastActionResult = localizedAction {
                            DemoStrings.text(R.string.sdk_initialization_completed)
                        },
                        errorMessage = null,
                        isInitialized = true,
                    )
                }
                appendEvent(DebugEventSource.SDK, DebugEventLevel.Info, DemoStrings.text(R.string.scanner_sdk_initialized))
            }.onFailure { error ->
                isInitialized = false
                DemoDiagnosticsStore.updateSdkInitialized(false)
                updateUiState { copy(isInitialized = false) }
                reportError({ DemoStrings.text(R.string.sdk_initialization_failed) }, error)
            }
        }
    }

    fun startDiscovery() {
        if (!requireInitializedForDiscovery()) return
        if (hasActiveSession()) {
            reportAction {
                DemoStrings.text(R.string.disconnect_before_new_discovery)
            }
            return
        }
        val mode = selectedTransportMode ?: run {
            requireSelectedMode(DemoTransportMode.BLE)
            return
        }
        if (mode == DemoTransportMode.SPP) {
            startSppDiscovery()
            return
        }
        if (!requireSelectedMode(DemoTransportMode.BLE)) return
        viewModelScope.launch {
            runCatching {
                discoveryCoordinator.stopDiscovery()
                discoveryCoordinator.startDiscovery(DemoTransportMode.BLE, selectedModelKey)
            }.onSuccess { result ->
                bleDeviceStore.clear()
                result.devices.forEach(bleDeviceStore::upsert)
                val discoveryModelKey = selectedModelKey
                updateUiState { copy(devices = visibleDevicesFor(DemoTransportMode.BLE), isDiscovering = true) }
                reportAction {
                    "${DemoStrings.text(R.string.started_ble_discovery)}：${selectedModelSummary(discoveryModelKey)}"
                }
                appendEvent(
                    DebugEventSource.SDK,
                    DebugEventLevel.Info,
                    "${DemoStrings.text(R.string.started_discovery_for_target_model)}: ${selectedModelSummary(selectedModelKey)}"
                )
            }.onFailure { error ->
                updateUiState { copy(isDiscovering = false) }
                if (!shouldSuppressDiscoveryStartError(error)) {
                    reportError({ DemoStrings.text(R.string.failed_to_start_discovery) }, error)
                }
            }
        }
    }

    fun stopDiscovery() {
        if (!requireInitializedForDiscovery()) return
        if (!_uiState.value.isDiscovering) {
            reportAction {
                DemoStrings.text(R.string.discovery_not_running)
            }
            return
        }
        viewModelScope.launch {
            runCatching {
                discoveryCoordinator.stopDiscovery()
            }.onSuccess {
                updateUiState { copy(isDiscovering = false) }
                reportAction { DemoStrings.text(R.string.discovery_stopped) }
                appendEvent(DebugEventSource.SDK, DebugEventLevel.Info, DemoStrings.text(R.string.stopped_discovery))
            }.onFailure { error ->
                updateUiState { copy(isDiscovering = false) }
                reportError({ DemoStrings.text(R.string.failed_to_stop_discovery) }, error)
            }
        }
    }

    fun logUiEvent(text: String) {
        appendEvent(DebugEventSource.UI, DebugEventLevel.Info, text)
    }

    fun reportDiscoveryPermissionsDenied(
        deniedPermissions: List<String>,
        includeAndroid12LocationNote: Boolean,
    ) {
        val messageProvider = {
            val missingSummary = deniedPermissions
                .distinct()
                .joinToString(separator = ", ") { permissionLabel(it) }
            val reason = if (includeAndroid12LocationNote) {
                DemoStrings.text(R.string.android_12_location_permission_rom_note)
            } else {
                DemoStrings.text(R.string.bluetooth_discovery_permission_reason)
            }
            if (missingSummary.isBlank()) {
                DemoStrings.text(R.string.discovery_permissions_denied)
            } else {
                DemoStrings.format(R.string.discovery_permissions_denied_with_missing, missingSummary, reason)
            }
        }
        val message = messageProvider()
        lastActionResultProvider = null
        errorMessageProvider = messageProvider
        updateUiState {
            copy(
                statusSummary = uiText(R.string.discovery_blocked),
                lastActionResult = null,
                errorMessage = dynamicText(messageProvider),
                blockerAction = DiscoveryBlockerAction.OPEN_APP_SETTINGS,
            )
        }
        appendEvent(DebugEventSource.UI, DebugEventLevel.Warn, message)
    }

    fun reportDiscoveryLocationDisabled() {
        val messageProvider = { DemoStrings.text(R.string.discovery_location_service_disabled) }
        val message = messageProvider()
        lastActionResultProvider = null
        errorMessageProvider = messageProvider
        updateUiState {
            copy(
                statusSummary = uiText(R.string.discovery_blocked),
                lastActionResult = null,
                errorMessage = dynamicText(messageProvider),
                blockerAction = DiscoveryBlockerAction.OPEN_LOCATION_SETTINGS,
            )
        }
        appendEvent(DebugEventSource.UI, DebugEventLevel.Warn, message)
    }

    fun setSelectedModel(modelKey: String) {
        if (hasActiveSession()) {
            reportAction {
                DemoStrings.text(R.string.disconnect_before_changing_model)
            }
            return
        }
        val supportedModels = _uiState.value.supportedModels
        if (supportedModels.isNotEmpty() && supportedModels.none { it.modelKey == modelKey }) {
            return
        }
        selectedModelKey = modelKey
        DemoDiagnosticsStore.updateSelectedModel(modelKey)
        DemoDiagnosticsStore.updateResolvedModel("")
        updateUiState {
            copy(
                selectedModelKey = modelKey,
                selectedModelSummary = selectedModelSummaryText(modelKey),
                lastActionResult = localizedAction {
                    DemoStrings.text(R.string.test_target_model_updated_filter)
                },
                errorMessage = null,
            )
        }
        appendEvent(
            DebugEventSource.UI,
            DebugEventLevel.Info,
            "${DemoStrings.text(R.string.test_target_model)}: ${selectedModelSummary(modelKey)}"
        )
    }

    fun setTransportMode(mode: DemoTransportMode) {
        if (hasActiveSession()) {
            reportAction {
                DemoStrings.text(R.string.disconnect_before_changing_transport)
            }
            return
        }
        val wasSelected = selectedTransportMode == mode
        selectedTransportMode = mode
        DemoDiagnosticsStore.updateTransport(mode)
        if (isInitialized) {
            viewModelScope.launch {
                runCatching { discoveryCoordinator.stopDiscovery() }
            }
        }
        updateUiState {
            copy(
                selectedTransportMode = mode,
                selectedTransportSummary = modeSummaryText(mode),
                devices = visibleDevicesFor(mode),
                isDiscovering = false,
                lastActionResult = localizedAction {
                    val label = if (wasSelected) {
                        DemoStrings.text(R.string.transport_mode_confirmed)
                    } else {
                        DemoStrings.text(R.string.transport_mode_updated)
                    }
                    "$label: ${modeSummary(mode)}"
                },
                errorMessage = null,
            )
        }
        appendEvent(
            DebugEventSource.UI,
            DebugEventLevel.Info,
            "${DemoStrings.text(R.string.transport_mode)}: ${modeSummary(mode)}"
        )
        loadSupportedModels(mode)
    }

    fun retrySupportedModels() {
        selectedTransportMode?.let(::loadSupportedModels)
    }

    fun startSppDiscovery() {
        if (!requireInitializedForDiscovery()) return
        if (!requireSelectedMode(DemoTransportMode.SPP)) return
        viewModelScope.launch {
            runCatching {
                discoveryCoordinator.stopDiscovery()
                sppDeviceStore.clear()
                updateUiState { copy(devices = emptyList(), isDiscovering = false) }
                discoveryCoordinator.startDiscovery(DemoTransportMode.SPP, selectedModelKey)
            }.onSuccess { result ->
                result.devices.forEach(sppDeviceStore::upsert)
                updateUiState {
                    copy(
                        isDiscovering = true,
                        devices = visibleDevicesFor(DemoTransportMode.SPP),
                        lastActionResult = localizedAction {
                            DemoStrings.text(R.string.started_spp_discovery)
                        },
                        errorMessage = null,
                    )
                }
                appendEvent(
                    DebugEventSource.SDK,
                    DebugEventLevel.Info,
                    DemoStrings.text(R.string.started_spp_discovery)
                )
            }.onFailure { error ->
                updateUiState { copy(isDiscovering = false) }
                reportError({ DemoStrings.text(R.string.failed_to_start_spp_discovery) }, error)
            }
        }
    }

    fun connect(device: DiscoveredDevice, onConnected: (() -> Unit)? = null) {
        if (device.connectable == false) return
        val mode = DemoTransportMode.fromTransportType(device.transportType) ?: return
        if (!isInitialized) {
            reportAction {
                DemoStrings.text(R.string.initialize_sdk_before_discovery)
            }
            return
        }
        if (!requireSelectedMode(mode)) return
        if (hasActiveSession()) {
            val messageProvider = { DemoStrings.text(R.string.disconnect_before_connecting_another_device) }
            val message = messageProvider()
            reportAction(messageProvider)
            appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, message)
            return
        }
        connectingDeviceId?.let { pendingDeviceId ->
            val messageProvider = {
                if (pendingDeviceId == device.deviceId) {
                    DemoStrings.text(R.string.connection_already_in_progress)
                } else {
                    DemoStrings.text(R.string.connection_in_progress_wait)
                }
            }
            val message = messageProvider()
            reportAction(messageProvider)
            appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, message)
            return
        }
        connectingDeviceId = device.deviceId
        updateUiState { copy(isConnecting = true, connectingDeviceId = device.deviceId, isDiscovering = false) }
        viewModelScope.launch {
            appendEvent(DebugEventSource.SESSION, DebugEventLevel.Info, "${DemoStrings.text(R.string.connect_device)}: ${device.deviceId}")
            runCatching {
                withContext(Dispatchers.IO) {
                    discoveryCoordinator.stopDiscovery()
                }
            }.onSuccess {
                appendEvent(
                    DebugEventSource.SDK,
                    DebugEventLevel.Info,
                    DemoStrings.text(R.string.stopped_discovery_before_connect)
                )
            }.onFailure { error ->
                appendEvent(
                    DebugEventSource.SDK,
                    DebugEventLevel.Warn,
                    "${DemoStrings.text(R.string.failed_to_stop_discovery_before_connect)}: ${error.demoErrorDetail()}"
                )
            }
            sessionObservationJobs.forEach { it.cancel() }
            sessionObservationJobs = emptyList()
            DemoSessionCoordinator.clear()
            updateUiState {
                copy(
                    selectedDeviceSummary = rawDisplayText("${device.name.ifBlank { DemoStrings.unknownDeviceName }} / ${device.deviceId}"),
                    statusSummary = uiText(R.string.session_connecting),
                    hasActiveSession = false,
                    isConnecting = true,
                    connectingDeviceId = device.deviceId,
                    isDiscovering = false,
                )
            }
            val connectionModelKey = resolveConnectionModelKey(
                selectedModelKey = selectedModelKey,
                discoveredModelKey = device.modelKey,
            )
            runCatching {
                withContext(Dispatchers.IO) {
                    discoveryCoordinator.connectReady(
                        device = device,
                        selectedModelKey = connectionModelKey,
                    )
                }
            }.onSuccess { result ->
                val session = result.session
                val fakeSession = result.fakeSession
                check(session != null || fakeSession != null) { "Discovery backend did not return a session" }
                if (session != null) {
                    DemoSessionCoordinator.bind(device, session, connectionModelKey)
                } else if (fakeSession != null) {
                    DemoSessionCoordinator.bindFake(device, fakeSession, connectionModelKey)
                }
                DemoDiagnosticsStore.updateSelectedModel(connectionModelKey)
                DemoDiagnosticsStore.updateTransport(mode)
                DemoDiagnosticsStore.updateSessionState(session?.state?.value ?: fakeSession?.state?.value)
                DemoDiagnosticsStore.updateFakeMode(discoveryCoordinator.isFakeMode)
                updateUiState { copy(isConnecting = false, connectingDeviceId = null, hasActiveSession = true) }
                appendEvent(
                    DebugEventSource.SESSION,
                    DebugEventLevel.Info,
                    "${DemoStrings.text(R.string.selected_model)}: ${displayModelLabel(connectionModelKey)}"
                )
                lastObservedSessionState = null
                if (session != null) {
                    observeMainSession(device, session, connectionModelKey)
                }
                appendEvent(
                    DebugEventSource.SESSION,
                    DebugEventLevel.Info,
                    "${DemoStrings.text(R.string.session_initialized_by_selected_model)}: ${selectedModelSummary(connectionModelKey)}"
                )
                reportAction {
                    if (fakeSession != null) {
                        DemoStrings.text(R.string.device_ready_open_console)
                    } else {
                        "${DemoStrings.text(R.string.session_initialized_by_selected_model)}: ${selectedModelSummary(connectionModelKey)}"
                    }
                }
                refreshDiagnosticsSummary()
                onConnected?.invoke()
            }.onFailure { error ->
                updateUiState { copy(isConnecting = false, connectingDeviceId = null) }
                reportError({ DemoStrings.text(R.string.failed_to_connect_device) }, error)
            }.also {
                if (connectingDeviceId == device.deviceId) {
                    connectingDeviceId = null
                    updateUiState { copy(isConnecting = false, connectingDeviceId = null) }
                }
            }
        }
    }

    fun disconnect() {
        val session = activeSessionHandle ?: run {
            reportAction {
                DemoStrings.text(R.string.no_active_session)
            }
            return
        }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { session.disconnect() }
            }.onSuccess {
                appendEvent(DebugEventSource.SESSION, DebugEventLevel.Info, DemoStrings.text(R.string.session_disconnected))
                closeDiscoverySession(
                    status = uiText(R.string.session_disconnected),
                    lastAction = uiText(R.string.device_disconnected),
                )
            }.onFailure { error ->
                reportError({ DemoStrings.text(R.string.failed_to_disconnect_device) }, error)
            }
        }
    }

    fun requestInfo() {
        viewModelScope.launch {
            val session = activeSession ?: run {
                appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, DemoStrings.text(R.string.no_active_session))
                return@launch
            }
            if (!ensureOperationSupport(
                    session = session,
                    operation = DemoSessionOperation.REFRESH_INFO,
                    labelProvider = { DemoStrings.text(R.string.load_device_info_action) }
                )
            ) {
                return@launch
            }
            runCatching {
                withContext(Dispatchers.IO) { session.refreshInfo() }
            }.onSuccess { info ->
                reportAction { DemoStrings.text(R.string.device_info_loaded) }
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Info, "${DemoStrings.text(R.string.info)} firmware=${info.firmwareVersion} hardware=${info.hardwareVersion} serial=${info.serialNumber}")
                appendEvent(
                    DebugEventSource.COMMAND,
                    DebugEventLevel.Debug,
                    "${DemoStrings.text(R.string.version_parsed)} family=${info.versionFormatFamily} boot=${info.versionBootCode} series=${info.versionSeriesCode} transport=${info.versionTransportCode}${info.versionTransportSuffix} wireless=${info.versionWirelessCode} bt=${info.versionBluetoothCode} chip=${info.versionChipsetCode}${info.versionChipsetSuffix}/${info.hardwareVersion} release=${info.versionReleaseCode} ext=${info.versionExtensionCode}"
                )
            }.onFailure { error ->
                reportError({ DemoStrings.text(R.string.failed_to_load_device_info) }, error)
            }
        }
    }

    fun requestBatteryLevel() {
        viewModelScope.launch {
            val session = activeSession ?: run {
                appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, DemoStrings.text(R.string.no_active_session))
                return@launch
            }
            if (!ensureOperationSupport(
                    session = session,
                    operation = DemoSessionOperation.GET_BATTERY_INFO,
                    labelProvider = { DemoStrings.text(R.string.load_battery_info_action) }
                )
            ) {
                return@launch
            }
            runCatching {
                withContext(Dispatchers.IO) { session.getBatteryInfo() }
            }.onSuccess { batteryInfo ->
                reportAction { DemoStrings.text(R.string.battery_info_loaded) }
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Info, "${DemoStrings.text(R.string.battery)} raw=${batteryInfo.rawText}")
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Info, "${DemoStrings.text(R.string.battery_parsed)} voltage=${batteryInfo.voltageText} percent=${batteryInfo.percent}")
            }.onFailure { error ->
                reportError({ DemoStrings.text(R.string.failed_to_load_battery_info) }, error)
            }
        }
    }

    fun setAckBeepEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val session = activeSession ?: run {
                appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, DemoStrings.text(R.string.no_active_session))
                return@launch
            }
            if (!ensureOperationSupport(
                    session = session,
                    operation = DemoSessionOperation.SET_ACK_BEEP_ENABLED,
                    labelProvider = {
                        DemoStrings.text(if (enabled) R.string.ack_beep_on_action else R.string.ack_beep_off_action)
                    }
                )
            ) {
                return@launch
            }
            runCatching {
                session.setAckBeepEnabled(enabled)
            }.onSuccess {
                val success = if (enabled) R.string.beep_command_sent else R.string.ack_beep_off_command_sent
                reportAction { DemoStrings.text(success) }
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Info, DemoStrings.text(success))
            }.onFailure { error ->
                val failure = if (enabled) R.string.beep_command_failed else R.string.disable_ack_beep_failed
                reportError({ DemoStrings.text(failure) }, error)
            }
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val session = activeSession ?: run {
                appendEvent(DebugEventSource.SESSION, DebugEventLevel.Warn, DemoStrings.text(R.string.no_active_session))
                return@launch
            }
            if (!ensureOperationSupport(
                    session = session,
                    operation = DemoSessionOperation.SET_VIBRATION_ENABLED,
                    labelProvider = {
                        DemoStrings.text(if (enabled) R.string.vibrate_on_action else R.string.vibrate_off_action)
                    }
                )
            ) {
                return@launch
            }
            runCatching {
                session.setVibrationEnabled(enabled)
            }.onSuccess {
                val success = if (enabled) R.string.vibrate_on_command_sent else R.string.vibrate_off_command_sent
                reportAction { DemoStrings.text(success) }
                appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Info, DemoStrings.text(success))
            }.onFailure { error ->
                val failure = if (enabled) R.string.enable_vibration_failed else R.string.disable_vibration_failed
                reportError({ DemoStrings.text(failure) }, error)
            }
        }
    }

    private fun observeMainSession(
        device: DiscoveredDevice,
        session: ScannerSession,
        connectionModelKey: String,
    ) {
        sessionObservationJobs.forEach { it.cancel() }
        sessionObservationJobs = DemoSessionCoordinator.observe(
            scope = viewModelScope,
            device = device,
            session = session,
            onState = { state ->
                val previousState = lastObservedSessionState
                lastObservedSessionState = state
                DemoDiagnosticsStore.updateSessionState(state)
                updateUiState {
                    copy(
                        statusSummary = statusText(state),
                        hasActiveSession = state != SessionState.DISCONNECTED && state != SessionState.ERROR,
                        isConnecting = false,
                        connectingDeviceId = null,
                        lastActionResult = if (state == SessionState.READY && previousState != SessionState.READY) {
                            localizedAction {
                                DemoStrings.text(R.string.device_ready_open_console)
                            }
                        } else {
                            lastActionResult
                        },
                        errorMessage = when {
                            state == SessionState.ERROR && errorMessage == null ->
                                localizedError {
                                    DemoStrings.text(R.string.device_connection_error)
                                }
                            else -> errorMessage
                        }
                    )
                }
                if (state != previousState) {
                    appendEvent(
                        DebugEventSource.SESSION,
                        DebugEventLevel.Info,
                        "${DemoStrings.text(R.string.session_state)}: ${statusText(state).asStringForCurrentLanguage()}"
                    )
                }
                if (state == SessionState.DISCONNECTED || state == SessionState.ERROR) {
                    closeDiscoverySession(
                        status = if (state == SessionState.ERROR) {
                            uiText(R.string.device_connection_error)
                        } else {
                            uiText(R.string.session_disconnected)
                        },
                        lastAction = if (state == SessionState.ERROR) {
                            uiText(R.string.device_connection_interrupted)
                        } else {
                            uiText(R.string.device_connection_closed)
                        },
                        clearError = state != SessionState.ERROR,
                    )
                }
            },
            onScan = {
                // Scan output is surfaced by the console page; the discovery page only owns connection state.
            },
            onFailure = { failure ->
                handleSessionFailure(failure)
            },
        )
    }

    private fun startObservingSdk() {
        if (observingSdk) return
        observingSdk = true
        viewModelScope.launch {
            discoveryCoordinator.discoveryEvents.collect { device ->
                val mode = DemoTransportMode.fromTransportType(device.transportType) ?: return@collect
                val visible = storeFor(mode).upsert(device)
                if (selectedTransportMode == mode) {
                    updateUiState { copy(devices = visible) }
                }
            }
        }
        viewModelScope.launch {
            discoveryCoordinator.discoveryFailures.collect { failure ->
                handleDiscoveryFailure(failure)
            }
        }
    }

    fun setDebugVisible(visible: Boolean) {
        if (visible) {
            if (sdkDebugJob == null) {
                sdkDebugJob = viewModelScope.launch {
                    discoveryCoordinator.debugEvents.collect { message ->
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

    private fun handleDiscoveryFailure(failure: DiscoveryFailure) {
        val summaryLabel = failure.localizedSdkStatusLabel()
        val summary = sdkText(summaryLabel.localizationKey, summaryLabel.fallbackDisplayName)
        val blockerAction = discoveryBlockerActionForFailure(failure)
        val detail = buildString {
            append(failure.message)
            failure.bleScanIssue?.let { issue ->
                append(" [")
                append(bleScanIssueSummary(issue))
                failure.platformErrorCode?.let { code -> append(", raw=$code") }
                append("]")
            } ?: failure.platformErrorCode?.let { code ->
                append(" (raw=$code)")
            }
            if (blockerAction == DiscoveryBlockerAction.OPEN_LOCATION_SETTINGS) {
                append(" ")
                append(
                    DemoStrings.text(R.string.android_12_location_permission_rom_note)
                )
            }
        }
        DemoDiagnosticsStore.updateRecentFailure("${DemoStrings.text(R.string.discovery_issue)}: $detail")
        updateUiState {
            copy(
                statusSummary = summary,
                lastActionResult = when {
                    failure.recoverable -> localizedAction {
                        DemoStrings.text(R.string.filtered_discovery_failed_switched)
                    }
                    failure.code == DiscoveryFailureCode.BLE_ADAPTER_DISABLED ->
                        localizedAction {
                            DemoStrings.text(R.string.enable_bluetooth_first)
                        }
                    failure.code == DiscoveryFailureCode.BLE_SCANNER_UNAVAILABLE ->
                        localizedAction {
                            DemoStrings.text(R.string.bluetooth_scanner_unavailable)
                        }
                    blockerAction == DiscoveryBlockerAction.OPEN_LOCATION_SETTINGS ->
                        localizedAction {
                            DemoStrings.text(R.string.android_12_location_permission_rom_note)
                        }
                    else -> lastActionResult
                },
                errorMessage = if (failure.recoverable) null else rawDisplayText(detail),
                blockerAction = blockerAction,
                isDiscovering = failure.recoverable && isDiscovering,
                diagnosticsSummary = DemoDiagnosticsStore.summaryText(),
            )
        }
        appendEvent(
            DebugEventSource.BLE,
            if (failure.recoverable) DebugEventLevel.Warn else DebugEventLevel.Error,
            "${DemoStrings.text(R.string.discovery_issue)}: $detail"
        )
    }

    private fun handleSessionFailure(failure: SessionFailure) {
        val summaryLabel = failure.issue.localizedSdkStatusLabel()
        val summary = sdkText(summaryLabel.localizationKey, summaryLabel.fallbackDisplayName)
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
                statusSummary = summary,
                lastActionResult = localizedAction {
                    DemoStrings.text(R.string.device_connection_interrupted)
                },
                errorMessage = rawDisplayText(detail),
                diagnosticsSummary = DemoDiagnosticsStore.summaryText(),
            )
        }
        appendEvent(DebugEventSource.BLE, DebugEventLevel.Error, "${DemoStrings.text(R.string.connection_issue)}: $detail")
    }

    private fun bleScanIssueSummary(issue: BleScanIssue): String {
        val label = issue.localizedSdkLabel()
        return SdkLabelResolver.resolve(label.localizationKey, label.fallbackDisplayName)
    }

    private fun transportIssueSummary(issue: TransportIssue): String {
        val label = issue.localizedSdkLabel()
        return SdkLabelResolver.resolve(label.localizationKey, label.fallbackDisplayName)
    }

    private fun shouldSuppressDiscoveryStartError(error: Throwable): Boolean {
        val message = error.message ?: return false
        return error is IllegalStateException && message.startsWith("BLE discovery unavailable:")
    }

    fun clearLogs() {
        updateUiState { copy(events = emptyList()) }
    }

    fun refreshLocalizedUi() {
        val selectedDevice = DemoSessionCoordinator.selectedDevice
        val latestState = activeSessionHandle?.state?.value
        DemoDiagnosticsStore.updateSdkInitialized(isInitialized)
        DemoDiagnosticsStore.updateTransport(selectedTransportMode)
        DemoDiagnosticsStore.updateSelectedModel(selectedModelKey)
        DemoDiagnosticsStore.updateSessionState(latestState)
        updateUiState {
            copy(
                statusSummary = when {
                    latestState != null -> statusText(latestState)
                    isInitialized -> uiText(R.string.sdk_initialized)
                    else -> uiText(R.string.sdk_not_initialized)
                },
                selectedDeviceSummary = if (selectedDevice == null) {
                    uiText(R.string.no_connected_device)
                } else {
                    rawDisplayText("${selectedDevice.name.ifBlank { DemoStrings.unknownDeviceName }} / ${selectedDevice.deviceId}")
                },
                selectedModelSummary = if (selectedModelKey.isBlank() && supportedModels.isEmpty()) {
                    uiText(R.string.no_supported_models_for_transport)
                } else {
                    selectedModelSummaryText(selectedModelKey)
                },
                selectedTransportMode = selectedTransportMode,
                selectedTransportSummary = selectedTransportMode?.let(::modeSummaryText)
                    ?: uiText(R.string.transport_not_selected),
                devices = selectedTransportMode?.let(::visibleDevicesFor).orEmpty(),
                isInitialized = isInitialized,
                hasActiveSession = hasActiveSession(),
                connectingDeviceId = connectingDeviceId,
                diagnosticsSummary = DemoDiagnosticsStore.summaryText(),
                lastActionResult = lastActionResultProvider?.let(::dynamicText) ?: lastActionResult,
                errorMessage = errorMessageProvider?.let(::dynamicText) ?: errorMessage,
            )
        }
    }

    private fun updateUiState(transform: DiscoveryUiState.() -> DiscoveryUiState) {
        _uiState.value = _uiState.value.transform()
    }

    private fun refreshDiagnosticsSummary() {
        updateUiState { copy(diagnosticsSummary = DemoDiagnosticsStore.summaryText()) }
    }

    private fun modeSummary(mode: DemoTransportMode): String = mode.summary()

    private fun modeSummaryText(mode: DemoTransportMode): UiText = dynamicText { modeSummary(mode) }

    private fun selectedModelSummary(modelKey: String): String = displayModelLabel(modelKey)

    private fun selectedModelSummaryText(modelKey: String): UiText = rawDisplayText(selectedModelSummary(modelKey))

    private fun loadSupportedModels(mode: DemoTransportMode) {
        supportedModelsLoadJob?.cancel()
        updateUiState {
            copy(
                supportedModels = emptyList(),
                isLoadingSupportedModels = true,
                supportedModelsError = null,
            )
        }
        appendEvent(
            DebugEventSource.SDK,
            DebugEventLevel.Info,
            DemoStrings.format(R.string.supported_models_loading_logged, mode.summary()),
        )
        supportedModelsLoadJob = viewModelScope.launch {
            runCatching {
                withContext(supportedModelsDispatcher) {
                    normalizeSupportedModels(supportedModelsLoader(mode.sdkTransport))
                }
            }.onSuccess { models ->
                if (selectedTransportMode != mode) return@onSuccess
                val resolvedModelKey = selectSupportedModel(models, selectedModelKey)
                selectedModelKey = resolvedModelKey
                DemoDiagnosticsStore.updateSelectedModel(resolvedModelKey)
                DemoDiagnosticsStore.updateResolvedModel("")
                updateUiState {
                    copy(
                        selectedModelKey = resolvedModelKey,
                        selectedModelSummary = if (resolvedModelKey.isBlank()) {
                            uiText(R.string.no_supported_models_for_transport)
                        } else {
                            selectedModelSummaryText(resolvedModelKey)
                        },
                        supportedModels = models,
                        isLoadingSupportedModels = false,
                        supportedModelsError = null,
                    )
                }
                appendEvent(
                    DebugEventSource.SDK,
                    DebugEventLevel.Info,
                    DemoStrings.format(R.string.supported_models_loaded_logged, mode.summary(), models.size),
                )
            }.onFailure { error ->
                if (selectedTransportMode != mode) return@onFailure
                updateUiState {
                    copy(
                        supportedModels = emptyList(),
                        isLoadingSupportedModels = false,
                        supportedModelsError = uiText(R.string.supported_models_load_failed),
                    )
                }
                appendEvent(
                    DebugEventSource.SDK,
                    DebugEventLevel.Error,
                    "${DemoStrings.format(R.string.supported_models_load_failed_logged, mode.summary())}: " +
                        DemoErrorFormatter.detail(error),
                )
            }
        }
    }

    private fun hasActiveSession(): Boolean {
        val state = activeSessionHandle?.state?.value ?: return false
        return state != SessionState.DISCONNECTED && state != SessionState.ERROR
    }

    private fun permissionLabel(permission: String): String = when (permission) {
        Manifest.permission.BLUETOOTH_SCAN -> DemoStrings.text(R.string.permission_bluetooth_scan)
        Manifest.permission.BLUETOOTH_CONNECT -> DemoStrings.text(R.string.permission_bluetooth_connect)
        Manifest.permission.ACCESS_FINE_LOCATION -> DemoStrings.text(R.string.permission_fine_location)
        else -> permission
    }

    private fun storeFor(mode: DemoTransportMode): DemoDiscoveryDeviceStore {
        return when (mode) {
            DemoTransportMode.BLE -> bleDeviceStore
            DemoTransportMode.SPP -> sppDeviceStore
        }
    }

    private fun visibleDevicesFor(mode: DemoTransportMode): List<DiscoveredDevice> {
        return storeFor(mode).visibleDevices()
    }

    private fun requireSelectedMode(requiredMode: DemoTransportMode): Boolean {
        val selected = selectedTransportMode
        if (selected == requiredMode) {
            return true
        }
        val messageProvider = if (selected == null) {
            { DemoStrings.text(R.string.select_transport_before_scan) }
        } else {
            { DemoStrings.text(R.string.transport_mode_mismatch) + ": ${modeSummary(selected)} != ${modeSummary(requiredMode)}" }
        }
        val message = messageProvider()
        reportAction(messageProvider)
        appendEvent(DebugEventSource.UI, DebugEventLevel.Warn, message)
        return false
    }

    private fun requireInitializedForDiscovery(): Boolean {
        if (isInitialized) {
            return true
        }
        val messageProvider = { DemoStrings.text(R.string.initialize_sdk_before_discovery) }
        val message = messageProvider()
        reportAction(messageProvider)
        appendEvent(DebugEventSource.UI, DebugEventLevel.Warn, message)
        return false
    }

    private fun closeDiscoverySession(
        status: UiText,
        lastAction: UiText,
        clearError: Boolean = true,
    ) {
        sessionObservationJobs.forEach { it.cancel() }
        sessionObservationJobs = emptyList()
        DemoSessionCoordinator.clear()
        DemoDiagnosticsStore.updateSessionState(null)
        DemoDiagnosticsStore.updateResolvedModel("")
        connectingDeviceId = null
        updateUiState {
            copy(
                statusSummary = status,
                selectedDeviceSummary = uiText(R.string.no_connected_device),
                hasActiveSession = false,
                isConnecting = false,
                connectingDeviceId = null,
                lastActionResult = lastAction,
                errorMessage = if (clearError) null else errorMessage,
                diagnosticsSummary = DemoDiagnosticsStore.summaryText(),
            )
        }
    }

    private suspend fun ensureOperationSupport(
        session: ScannerSession,
        operation: DemoSessionOperation,
        labelProvider: () -> String,
    ): Boolean {
        val supported = withContext(Dispatchers.IO) {
            session.getOperationSupport().supports(operation)
        }
        if (supported) {
            return true
        }
        val reason = unsupportedDemoSessionOperationReason(operation)
        reportError({ "${labelProvider()}：$reason" }, IllegalStateException(reason))
        appendEvent(DebugEventSource.COMMAND, DebugEventLevel.Warn, "${labelProvider()}: $reason")
        return false
    }

    private fun reportRawAction(message: String) {
        lastActionResultProvider = null
        errorMessageProvider = null
        updateUiState { copy(lastActionResult = rawDisplayText(message), errorMessage = null, blockerAction = null) }
    }

    private fun reportAction(provider: () -> String) {
        lastActionResultProvider = provider
        errorMessageProvider = null
        updateUiState { copy(lastActionResult = dynamicText(provider), errorMessage = null, blockerAction = null) }
    }

    private fun reportRawError(prefix: String, error: Throwable) {
        lastActionResultProvider = null
        errorMessageProvider = null
        val detail = error.demoErrorDetail()
        updateUiState { copy(errorMessage = rawDisplayText("$prefix：$detail"), blockerAction = null) }
        appendEvent(DebugEventSource.SDK, DebugEventLevel.Error, "$prefix: $detail")
    }

    private fun reportError(prefixProvider: () -> String, error: Throwable) {
        lastActionResultProvider = null
        errorMessageProvider = { "${prefixProvider()}：${error.demoErrorDetail()}" }
        updateUiState { copy(errorMessage = errorMessageProvider?.let(::dynamicText), blockerAction = null) }
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

    private fun currentRecordDisplayCharset() =
        (activeSessionHandle?.getScanTextCharset() ?: ScanTextCharset.UTF_8).charset

    private fun currentCommandTextDisplayCharset() =
        (activeSessionHandle?.getScanTextCharset() ?: ScanTextCharset.UTF_8).charset

    private fun statusText(state: SessionState): UiText = when (state) {
        SessionState.IDLE -> uiText(R.string.session_idle)
        SessionState.CONNECTING -> uiText(R.string.session_connecting)
        SessionState.CONNECTED -> uiText(R.string.session_connected_preparing)
        SessionState.READY -> uiText(R.string.session_console_ready)
        SessionState.BUSY -> uiText(R.string.session_busy)
        SessionState.RECONNECTING -> uiText(R.string.session_reconnecting)
        SessionState.DISCONNECTED -> uiText(R.string.session_disconnected)
        SessionState.ERROR -> uiText(R.string.session_connection_error)
        SessionState.DISCOVERING -> uiText(R.string.session_discovering)
    }
}
