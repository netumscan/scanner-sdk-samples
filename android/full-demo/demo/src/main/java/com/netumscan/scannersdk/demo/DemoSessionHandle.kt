package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.ScannerSdk
import com.netumscan.scannersdk.ScannerSession
import com.netumscan.scannersdk.SessionState
import com.netumscan.scannersdk.TransportType
import com.netumscan.scannersdk.model.CapabilityEntryKind
import com.netumscan.scannersdk.model.DeviceCapabilitySummary
import com.netumscan.scannersdk.model.ScanEvent
import com.netumscan.scannersdk.model.ScanTextCharset
import com.netumscan.scannersdk.model.DeviceSupportStatus
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
    fun getScanTextTerminator(): ByteArray
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

    override fun getScanTextTerminator(): ByteArray = session.getScanTextTerminator()

    override suspend fun disconnect() {
        session.disconnect()
    }
}

internal class FakeDemoSessionHandle(
    override val deviceId: String,
    override val transportType: TransportType,
    val selectedModelKey: String,
    val publicCapabilitySummary: DeviceCapabilitySummary = fakeCapabilitySummary(selectedModelKey),
) : DemoSessionHandle {
    private val mutableState = MutableStateFlow(SessionState.READY)
    private val mutableScanEvents = MutableSharedFlow<ScanEvent>(extraBufferCapacity = 8)

    override val isFake: Boolean = true
    override val state: StateFlow<SessionState> = mutableState.asStateFlow()
    override val scanEvents: Flow<ScanEvent> = mutableScanEvents.asSharedFlow()
    override val realSession: ScannerSession? = null

    override fun getScanTextCharset(): ScanTextCharset = ScanTextCharset.UTF_8

    override fun getScanTextTerminator(): ByteArray = byteArrayOf(0x0D)

    override suspend fun disconnect() {
        mutableState.value = SessionState.DISCONNECTED
    }
}

private fun fakeCapabilitySummary(modelKey: String): DeviceCapabilitySummary {
    val resolved = if (modelKey == "") "CS7501" else modelKey
    val definitions = runCatching {
        ScannerSdk.getCapabilityEntries(resolved, TransportType.BLE_GATT)
            .filter { it.kind == CapabilityEntryKind.SETTING }
    }.getOrDefault(emptyList())
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
