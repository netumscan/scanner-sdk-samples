package com.netumscan.scannersdk.demo

import android.content.Context
import com.netumscan.scannersdk.ProtocolChannelKind
import com.netumscan.scannersdk.model.DeviceModelId
import com.netumscan.scannersdk.model.DiscoveredDevice
import com.netumscan.scannersdk.model.DiscoveryFailure
import kotlinx.coroutines.flow.Flow

internal class DiscoveryCoordinator(
    private val backend: DemoDiscoveryBackend,
) {
    val isFakeMode: Boolean = backend.isFake
    val debugEvents: Flow<String> = backend.debugEvents
    val discoveryEvents: Flow<DiscoveredDevice> = backend.discoveryEvents
    val discoveryFailures: Flow<DiscoveryFailure> = backend.discoveryFailures

    suspend fun initialize(context: Context) {
        backend.initialize(context)
    }

    suspend fun stopDiscovery() {
        backend.stopDiscovery()
    }

    suspend fun startDiscovery(
        mode: DemoTransportMode,
        selectedModelId: DeviceModelId,
    ): DemoDiscoveryStartResult {
        return backend.startDiscovery(mode, selectedModelId)
    }

    suspend fun connectReady(
        device: DiscoveredDevice,
        channelKind: ProtocolChannelKind,
        selectedModelId: DeviceModelId,
        applyDecoderModule: Boolean,
    ): DemoConnectResult {
        return backend.connectReady(device, channelKind, selectedModelId, applyDecoderModule)
    }
}
