package com.netumscan.scannersdk.quickstart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.netumscan.scannersdk.SessionState
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class ScanRow(
    val id: Long,
    val barcodeType: Int,
    val text: String,
)

internal data class QuickStartUiState(
    val sdkVersion: String = "",
    val status: String = "Initializing SDK",
    val models: List<QuickModel> = emptyList(),
    val selectedModelKey: String = "",
    val isDiscovering: Boolean = false,
    val devices: List<QuickDevice> = emptyList(),
    val isConnecting: Boolean = false,
    val connectedDeviceName: String? = null,
    val isReady: Boolean = false,
    val canRetry: Boolean = false,
    val isRunningCommand: Boolean = false,
    val operationSupport: QuickOperationSupport = QuickOperationSupport(false, false, false),
    val deviceInfo: String = "Not loaded",
    val battery: String = "Not loaded",
    val lastAction: String = "",
    val scans: List<ScanRow> = emptyList(),
)

internal class QuickStartViewModel(
    private val backend: QuickStartBackend,
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuickStartUiState(sdkVersion = backend.sdkVersion))
    val uiState: StateFlow<QuickStartUiState> = _uiState.asStateFlow()

    private val scanSequence = AtomicLong()
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var session: QuickSession? = null
    private var sessionStateJob: Job? = null
    private var scanJob: Job? = null
    private var failureJob: Job? = null

    init {
        bindDiscoveryStreams()
        initialize()
    }

    fun selectModel(modelKey: String) {
        if (_uiState.value.isDiscovering || session != null) return
        if (_uiState.value.models.none { it.modelKey == modelKey }) return
        _uiState.update { it.copy(selectedModelKey = modelKey) }
    }

    fun startDiscovery() {
        if (_uiState.value.selectedModelKey.isBlank() || session != null) return
        viewModelScope.launch {
            runCatching {
                backend.startDiscovery(_uiState.value.selectedModelKey)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        status = "Discovering BLE scanners",
                        isDiscovering = true,
                        canRetry = false,
                        devices = emptyList(),
                        lastAction = "",
                    )
                }
            }.onFailure(::showFailure)
        }
    }

    fun stopDiscovery() {
        viewModelScope.launch {
            runCatching { backend.stopDiscovery() }
                .onSuccess {
                    _uiState.update { it.copy(status = "Discovery stopped", isDiscovering = false) }
                }
                .onFailure(::showFailure)
        }
    }

    fun connect(device: QuickDevice) {
        if (_uiState.value.isConnecting || session != null) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    status = "Connecting to ${device.displayName()}",
                    isConnecting = true,
                    isDiscovering = false,
                    canRetry = false,
                )
            }
            runCatching {
                runCatching { backend.stopDiscovery() }
                backend.connect(device)
            }.onSuccess { connected ->
                session = connected
                bindSession(connected)
                runCatching {
                    connected.operationSupport() to connected.resolvedModelKey()
                }.onSuccess { (support, modelKey) ->
                    if (session !== connected) return@onSuccess
                    _uiState.update {
                        it.copy(
                            status = "Connected and ready",
                            isConnecting = false,
                            connectedDeviceName = device.displayName(),
                            isReady = true,
                            operationSupport = support,
                            lastAction = "Resolved model: ${modelKey.ifBlank { device.modelKey }}",
                        )
                    }
                }.onFailure { error ->
                    disconnectAfterFailure(connected, error)
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isConnecting = false, canRetry = true) }
                showFailure(error)
            }
        }
    }

    fun disconnect() {
        val active = session ?: return
        viewModelScope.launch {
            runCatching { active.disconnect() }
                .onFailure(::showFailure)
            clearSession("Disconnected", retry = true)
        }
    }

    fun triggerScan() = runCommand(
        supported = _uiState.value.operationSupport.triggerScan,
        unsupportedMessage = "Trigger Scan is not supported by this session",
    ) {
        triggerScan()
        "Trigger Scan command sent"
    }

    fun refreshInfo() = runCommand(
        supported = _uiState.value.operationSupport.refreshInfo,
        unsupportedMessage = "Refresh Device Info is not supported by this session",
    ) {
        val summary = refreshInfo().displaySummary()
        _uiState.update { it.copy(deviceInfo = summary) }
        "Device information refreshed"
    }

    fun readBattery() = runCommand(
        supported = _uiState.value.operationSupport.battery,
        unsupportedMessage = "Read Battery is not supported by this session",
    ) {
        val summary = readBattery().displaySummary()
        _uiState.update { it.copy(battery = summary) }
        "Battery information refreshed"
    }

    fun showPermissionDenied() {
        _uiState.update {
            it.copy(
                status = "Bluetooth permission is required",
                isDiscovering = false,
                canRetry = true,
            )
        }
    }

    private fun initialize() {
        viewModelScope.launch {
            runCatching { backend.initialize() }
                .onSuccess { loaded ->
                    val models = loaded
                        .filter { it.modelKey.isNotBlank() }
                        .distinctBy { it.modelKey.lowercase() }
                        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.modelName })
                    val selected = models.firstOrNull { it.modelKey.equals("CS7501", ignoreCase = true) }
                        ?: models.firstOrNull()
                    _uiState.update {
                        it.copy(
                            sdkVersion = backend.sdkVersion,
                            status = if (models.isEmpty()) "No BLE models are available" else "Ready to discover",
                            models = models,
                            selectedModelKey = selected?.modelKey.orEmpty(),
                            canRetry = models.isNotEmpty(),
                        )
                    }
                }
                .onFailure(::showFailure)
        }
    }

    private fun bindDiscoveryStreams() {
        viewModelScope.launch {
            backend.discoveries.collect { device ->
                _uiState.update { state ->
                    val devices = state.devices
                        .filterNot { it.deviceId == device.deviceId }
                        .plus(device)
                        .sortedByDescending { it.rssi ?: Int.MIN_VALUE }
                    state.copy(devices = devices)
                }
            }
        }
        viewModelScope.launch {
            backend.discoveryFailures.collect { message ->
                _uiState.update {
                    it.copy(status = message, isDiscovering = false, canRetry = true)
                }
            }
        }
    }

    private fun bindSession(active: QuickSession) {
        cancelSessionJobs()
        sessionStateJob = viewModelScope.launch {
            active.states.collect { state ->
                if (session !== active) return@collect
                when (state) {
                    SessionState.READY -> _uiState.update { it.copy(status = "Connected and ready", isReady = true) }
                    SessionState.DISCONNECTED,
                    SessionState.ERROR -> clearSession(
                        if (state == SessionState.ERROR) "Connection error" else "Connection lost",
                        retry = true,
                    )
                    else -> _uiState.update { it.copy(status = state.name.lowercase().replaceFirstChar(Char::uppercase)) }
                }
            }
        }
        scanJob = viewModelScope.launch {
            active.scans.collect { event ->
                if (session !== active) return@collect
                val row = ScanRow(
                    id = scanSequence.incrementAndGet(),
                    barcodeType = event.barcodeType,
                    text = event.text,
                )
                _uiState.update { it.copy(scans = listOf(row).plus(it.scans).take(MAX_SCANS)) }
            }
        }
        failureJob = viewModelScope.launch {
            active.failures.collect { message ->
                if (session === active) {
                    _uiState.update { it.copy(status = message, lastAction = message) }
                }
            }
        }
    }

    private fun runCommand(
        supported: Boolean,
        unsupportedMessage: String,
        operation: suspend QuickSession.() -> String,
    ) {
        val active = session
        if (active == null || !_uiState.value.isReady) {
            _uiState.update { it.copy(lastAction = "Connect a ready scanner first") }
            return
        }
        if (!supported) {
            _uiState.update { it.copy(lastAction = unsupportedMessage) }
            return
        }
        if (_uiState.value.isRunningCommand) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRunningCommand = true) }
            runCatching { active.operation() }
                .onSuccess { message -> _uiState.update { it.copy(lastAction = message) } }
                .onFailure(::showFailure)
            _uiState.update { it.copy(isRunningCommand = false) }
        }
    }

    private suspend fun disconnectAfterFailure(active: QuickSession, error: Throwable) {
        runCatching { active.disconnect() }
        if (session === active) {
            clearSession("Connection setup failed", retry = true)
        }
        showFailure(error)
    }

    private fun clearSession(status: String, retry: Boolean) {
        session = null
        cancelSessionJobs()
        _uiState.update {
            it.copy(
                status = status,
                isConnecting = false,
                connectedDeviceName = null,
                isReady = false,
                canRetry = retry,
                isRunningCommand = false,
                operationSupport = QuickOperationSupport(false, false, false),
            )
        }
    }

    private fun cancelSessionJobs() {
        sessionStateJob?.cancel()
        scanJob?.cancel()
        failureJob?.cancel()
        sessionStateJob = null
        scanJob = null
        failureJob = null
    }

    private fun showFailure(error: Throwable) {
        val message = error.message?.takeIf(String::isNotBlank) ?: error::class.java.simpleName
        _uiState.update { it.copy(status = message, lastAction = message) }
    }

    override fun onCleared() {
        val active = session
        session = null
        cancelSessionJobs()
        cleanupScope.launch {
            runCatching { active?.disconnect() }
            runCatching { backend.shutdown() }
            cleanupScope.cancel()
        }
        super.onCleared()
    }

    companion object {
        private const val MAX_SCANS = 20

        fun factory(backend: QuickStartBackend): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    QuickStartViewModel(backend) as T
            }
    }
}

internal fun QuickDevice.displayName(): String = name.ifBlank { "Unnamed scanner" }
