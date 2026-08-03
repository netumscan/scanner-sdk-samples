package com.netumscan.scannersdk.quickstart

import android.content.Context
import com.netumscan.scannersdk.ScannerSdk
import com.netumscan.scannersdk.ScannerSession
import com.netumscan.scannersdk.SessionState
import com.netumscan.scannersdk.TransportType
import com.netumscan.scannersdk.model.DiscoveredDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal data class QuickModel(
    val modelKey: String,
    val modelName: String,
)

internal data class QuickDevice(
    val deviceId: String,
    val name: String,
    val modelKey: String,
    val rssi: Int?,
)

internal data class QuickScanEvent(
    val barcodeType: Int,
    val text: String,
)

internal data class QuickOperationSupport(
    val refreshInfo: Boolean,
    val battery: Boolean,
    val triggerScan: Boolean,
)

internal data class QuickDeviceInfo(
    val name: String,
    val resolvedModel: String,
    val firmware: String,
    val hardware: String,
) {
    fun displaySummary(): String = listOf(
        name.takeIf(String::isNotBlank),
        resolvedModel.takeIf(String::isNotBlank),
        firmware.takeIf(String::isNotBlank)?.let { "FW $it" },
        hardware.takeIf(String::isNotBlank)?.let { "HW $it" },
    ).filterNotNull().joinToString(" · ").ifBlank { "No device information returned" }
}

internal data class QuickBattery(
    val percent: Int,
    val voltage: String,
) {
    fun displaySummary(): String = buildString {
        append("$percent%")
        if (voltage.isNotBlank()) append(" · $voltage")
    }
}

internal interface QuickSession {
    val states: Flow<SessionState>
    val scans: Flow<QuickScanEvent>
    val failures: Flow<String>

    suspend fun operationSupport(): QuickOperationSupport
    suspend fun resolvedModelKey(): String
    suspend fun triggerScan()
    suspend fun refreshInfo(): QuickDeviceInfo
    suspend fun readBattery(): QuickBattery
    suspend fun disconnect()
}

internal interface QuickStartBackend {
    val sdkVersion: String
    val discoveries: Flow<QuickDevice>
    val discoveryFailures: Flow<String>

    suspend fun initialize(): List<QuickModel>
    suspend fun startDiscovery(modelKey: String)
    suspend fun stopDiscovery()
    suspend fun connect(device: QuickDevice): QuickSession
    suspend fun shutdown()
}

internal class ScannerQuickStartBackend(
    context: Context,
) : QuickStartBackend {
    private val applicationContext = context.applicationContext
    private val discoveredDevices = linkedMapOf<String, DiscoveredDevice>()

    override val sdkVersion: String
        get() = ScannerSdk.version

    override val discoveries: Flow<QuickDevice> = ScannerSdk.discoveryEvents.map { device ->
        synchronized(discoveredDevices) {
            discoveredDevices[device.deviceId] = device
        }
        device.toQuickDevice()
    }

    override val discoveryFailures: Flow<String> = ScannerSdk.discoveryFailures.map { failure ->
        "Discovery failed: ${failure.code}"
    }

    override suspend fun initialize(): List<QuickModel> {
        ScannerSdk.initialize(applicationContext)
        return ScannerSdk.getSupportedDeviceModels(TransportType.BLE_GATT)
            .map { QuickModel(modelKey = it.modelKey, modelName = it.modelName) }
    }

    override suspend fun startDiscovery(modelKey: String) {
        synchronized(discoveredDevices) {
            discoveredDevices.clear()
        }
        ScannerSdk.startDiscovery(TransportType.BLE_GATT, modelKey)
    }

    override suspend fun stopDiscovery() {
        ScannerSdk.stopDiscovery()
    }

    override suspend fun connect(device: QuickDevice): QuickSession {
        val source = synchronized(discoveredDevices) {
            discoveredDevices[device.deviceId]
        } ?: error("The selected scanner is no longer available")
        return ScannerQuickSession(ScannerSdk.connectReady(source))
    }

    override suspend fun shutdown() {
        ScannerSdk.shutdown()
    }

    private fun DiscoveredDevice.toQuickDevice(): QuickDevice = QuickDevice(
        deviceId = deviceId,
        name = name,
        modelKey = modelKey,
        rssi = rssi,
    )
}

private class ScannerQuickSession(
    private val session: ScannerSession,
) : QuickSession {
    override val states: Flow<SessionState> = session.state
    override val scans: Flow<QuickScanEvent> = session.scanEvents.map {
        QuickScanEvent(barcodeType = it.barcodeType, text = it.text)
    }
    override val failures: Flow<String> = session.failures.map {
        "Session error: ${it.issue}"
    }

    override suspend fun operationSupport(): QuickOperationSupport {
        val support = session.getOperationSupport()
        return QuickOperationSupport(
            refreshInfo = support.supportsRefreshInfo,
            battery = support.supportsGetBatteryInfo,
            triggerScan = support.supportsTriggerScan,
        )
    }

    override suspend fun resolvedModelKey(): String = session.getResolvedModelKey()

    override suspend fun triggerScan() {
        session.triggerScan()
    }

    override suspend fun refreshInfo(): QuickDeviceInfo {
        val info = session.refreshInfo()
        return QuickDeviceInfo(
            name = info.name,
            resolvedModel = session.getResolvedModelKey(),
            firmware = info.firmwareVersion,
            hardware = info.hardwareVersion,
        )
    }

    override suspend fun readBattery(): QuickBattery {
        val battery = session.getBatteryInfo()
        return QuickBattery(percent = battery.percent, voltage = battery.voltageText)
    }

    override suspend fun disconnect() {
        session.disconnect()
    }
}
