package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.ScannerSession
import com.netumscan.scannersdk.SessionState
import com.netumscan.scannersdk.TransportType
import com.netumscan.scannersdk.model.DeviceCapabilitySummary
import com.netumscan.scannersdk.model.DeviceModelId
import com.netumscan.scannersdk.model.ModuleFamily
import com.netumscan.scannersdk.model.ScanEvent
import com.netumscan.scannersdk.model.ScanTextCharset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

internal interface DemoSessionHandle {
    val isFake: Boolean
    val deviceId: String
    val transportType: TransportType
    val state: StateFlow<SessionState>
    val scanEvents: Flow<ScanEvent>
    val realSession: ScannerSession?
    fun getScanTextCharset(): ScanTextCharset
    fun getScanTerminator(): ByteArray
    suspend fun disconnect()
}

internal class RealDemoSessionHandle(
    private val session: ScannerSession,
    override val deviceId: String,
    override val transportType: TransportType,
) : DemoSessionHandle {
    override val isFake: Boolean = false
    override val state: StateFlow<SessionState> = session.state
    override val scanEvents: Flow<ScanEvent> = session.scanEvents
    override val realSession: ScannerSession = session

    override fun getScanTextCharset(): ScanTextCharset = session.getScanTextCharset()

    override fun getScanTerminator(): ByteArray = session.getScanTerminator()

    override suspend fun disconnect() {
        session.disconnect()
    }
}

internal class FakeDemoSessionHandle(
    override val deviceId: String,
    override val transportType: TransportType,
    val selectedModelId: DeviceModelId,
    val capabilitySummary: DeviceCapabilitySummary = fakeCapabilitySummary(selectedModelId),
) : DemoSessionHandle {
    private val mutableState = MutableStateFlow(SessionState.READY)
    private val mutableScanEvents = MutableSharedFlow<ScanEvent>(extraBufferCapacity = 8)

    override val isFake: Boolean = true
    override val state: StateFlow<SessionState> = mutableState.asStateFlow()
    override val scanEvents: Flow<ScanEvent> = mutableScanEvents.asSharedFlow()
    override val realSession: ScannerSession? = null

    override fun getScanTextCharset(): ScanTextCharset = ScanTextCharset.UTF_8

    override fun getScanTerminator(): ByteArray = byteArrayOf(0x0D)

    override suspend fun disconnect() {
        mutableState.value = SessionState.DISCONNECTED
    }
}

private fun fakeCapabilitySummary(modelId: DeviceModelId): DeviceCapabilitySummary {
    val resolved = if (modelId == DeviceModelId.UNKNOWN) DeviceModelId.CS7501 else modelId
    return DeviceCapabilitySummary(
        modelId = resolved,
        modelName = displayModelLabel(resolved),
        defaultCommandSet = com.netumscan.scannersdk.model.CommandSetKind.MASTER_WITH_MODULE_INFO,
        formFactor = com.netumscan.scannersdk.model.DeviceFormFactor.MASTER_WITH_MODULE,
        moduleFamily = ModuleFamily.NT212X,
        supportsBasicDeviceCommands = true,
        supportsMasterCommands = true,
        supportsNativeModuleCommands = false,
        supportsModuleCommandBridge = true,
        supportsModuleCommands = true,
        supportsScannerMaster = true,
        supportsModulePassthrough = true,
        supportStatus = com.netumscan.scannersdk.model.SupportStatus.CODE_ONLY,
    )
}
