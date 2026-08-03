package com.netumscan.scannersdk.demo

import android.content.Context
import com.netumscan.scannersdk.ScannerSdk
import com.netumscan.scannersdk.ScannerSession
import com.netumscan.scannersdk.TransportType
import com.netumscan.scannersdk.model.DiscoveredDevice
import com.netumscan.scannersdk.model.DiscoveryFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow

internal data class DemoDiscoveryStartResult(
    val devices: List<DiscoveredDevice> = emptyList(),
)

internal data class DemoConnectResult(
    val session: ScannerSession?,
    val fakeSession: DemoSessionHandle? = null,
)

internal interface DemoDiscoveryBackend {
    val isFake: Boolean
    val debugEvents: Flow<String>
    val discoveryEvents: Flow<DiscoveredDevice>
    val discoveryFailures: Flow<DiscoveryFailure>

    suspend fun initialize(context: Context)
    suspend fun stopDiscovery()
    suspend fun startDiscovery(mode: DemoTransportMode, selectedModelKey: String): DemoDiscoveryStartResult
    suspend fun connectReady(
        device: DiscoveredDevice,
        selectedModelKey: String,
    ): DemoConnectResult
}

internal object RealDemoDiscoveryBackend : DemoDiscoveryBackend {
    override val isFake: Boolean = false
    override val debugEvents: Flow<String> = ScannerSdk.debugEvents
    override val discoveryEvents: Flow<DiscoveredDevice> = ScannerSdk.discoveryEvents
    override val discoveryFailures: Flow<DiscoveryFailure> = ScannerSdk.discoveryFailures

    override suspend fun initialize(context: Context) {
        ScannerSdk.initialize(context)
    }

    override suspend fun stopDiscovery() {
        ScannerSdk.stopDiscovery()
    }

    override suspend fun startDiscovery(
        mode: DemoTransportMode,
        selectedModelKey: String,
    ): DemoDiscoveryStartResult {
        ScannerSdk.startDiscovery(mode.sdkTransport, selectedModelKey)
        return DemoDiscoveryStartResult()
    }

    override suspend fun connectReady(
        device: DiscoveredDevice,
        selectedModelKey: String,
    ): DemoConnectResult {
        return DemoConnectResult(
            session = ScannerSdk.connectReady(
                deviceId = device.deviceId,
                transportType = device.transportType,
                selectedModelKey = selectedModelKey,
            )
        )
    }
}

internal class FakeDemoDiscoveryBackend : DemoDiscoveryBackend {
    private val debugFlow = MutableSharedFlow<String>(extraBufferCapacity = 32)

    override val isFake: Boolean = true
    override val debugEvents: Flow<String> = debugFlow
    override val discoveryEvents: Flow<DiscoveredDevice> = emptyFlow()
    override val discoveryFailures: Flow<DiscoveryFailure> = emptyFlow()

    override suspend fun initialize(context: Context) {
        debugFlow.tryEmit("fake initialize context=${context.packageName}")
    }

    override suspend fun stopDiscovery() {
        debugFlow.tryEmit("fake discovery stopped")
    }

    override suspend fun startDiscovery(
        mode: DemoTransportMode,
        selectedModelKey: String,
    ): DemoDiscoveryStartResult {
        debugFlow.tryEmit("fake discovery started mode=$mode selectedModel=$selectedModelKey")
        return DemoDiscoveryStartResult(fakeDevices(mode, selectedModelKey))
    }

    override suspend fun connectReady(
        device: DiscoveredDevice,
        selectedModelKey: String,
    ): DemoConnectResult {
        debugFlow.tryEmit(
            "fake connectReady device=${device.deviceId} transport=${device.transportType} " +
                "selectedModel=$selectedModelKey"
        )
        return DemoConnectResult(
            session = null,
            fakeSession = FakeDemoSessionHandle(
                deviceId = device.deviceId,
                transportType = device.transportType,
                selectedModelKey = selectedModelKey,
            ),
        )
    }

    private fun fakeDevices(mode: DemoTransportMode, selectedModelKey: String): List<DiscoveredDevice> {
        val model = if (selectedModelKey == "") "CS7501" else selectedModelKey
        val transport = when (mode) {
            DemoTransportMode.BLE -> TransportType.BLE_GATT
            DemoTransportMode.SPP -> TransportType.SPP_CLASSIC
        }
        return listOf(
            DiscoveredDevice(
                deviceId = "FAKE-${mode.name}-$model-001",
                name = "Fake ${mode.summary()} Scanner",
                transportType = transport,
                modelKey = model,
                matchReason = "fake-demo",
                rssi = if (mode == DemoTransportMode.BLE) -48 else null,
            )
        )
    }
}
