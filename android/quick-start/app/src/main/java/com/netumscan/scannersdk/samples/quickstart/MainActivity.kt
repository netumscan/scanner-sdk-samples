package com.netumscan.scannersdk.samples.quickstart

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.netumscan.scannersdk.ScannerSdk
import com.netumscan.scannersdk.ScannerSession
import com.netumscan.scannersdk.SessionState
import com.netumscan.scannersdk.TransportType
import com.netumscan.scannersdk.model.DeviceModelId
import com.netumscan.scannersdk.model.DiscoveredDevice
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    QuickStartScreen()
                }
            }
        }
    }
}

@Composable
private fun QuickStartScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val devices = remember { mutableStateListOf<DiscoveredDevice>() }
    val scans = remember { mutableStateListOf<String>() }
    var status by remember { mutableStateOf("Idle") }
    var session by remember { mutableStateOf<ScannerSession?>(null) }
    var scanJob by remember { mutableStateOf<Job?>(null) }

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        status = "Permissions updated"
    }

    LaunchedEffect(Unit) {
        launch {
            ScannerSdk.discoveryEvents.collect { device ->
                val index = devices.indexOfFirst { it.deviceId == device.deviceId }
                if (index >= 0) {
                    devices[index] = device
                } else {
                    devices.add(device)
                }
            }
        }
        launch {
            ScannerSdk.discoveryFailures.collectLatest { failure ->
                status = "Discovery failed: ${failure.code}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Scanner SDK Quick Start", style = MaterialTheme.typography.headlineSmall)
        Text(status, style = MaterialTheme.typography.bodyMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { permissionLauncher.launch(permissions) }) {
                Text("Permissions")
            }
        }

        Button(
            onClick = {
                scope.launch {
                    runCatching {
                        ScannerSdk.initialize(context.applicationContext)
                        devices.clear()
                        status = "Discovering"
                        ScannerSdk.startDiscovery(TransportType.BLE_GATT, DeviceModelId.UNKNOWN)
                    }.onFailure { error ->
                        status = error.message ?: error::class.java.simpleName
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Start BLE Discovery")
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(devices, key = { it.deviceId }) { device ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(device.name.ifBlank { "(unnamed)" }, style = MaterialTheme.typography.titleMedium)
                    Text("${device.deviceId}  ${device.transportType}  RSSI=${device.rssi ?: "-"}")
                    Button(
                        onClick = {
                            scope.launch {
                                runCatching {
                                    ScannerSdk.stopDiscovery()
                                    status = "Connecting"
                                    val connected = ScannerSdk.connectReady(device)
                                    session = connected
                                    status = "Connected: ${connected.state.value}"
                                    scanJob?.cancel()
                                    scanJob = launch {
                                        connected.scanEvents.collect { event ->
                                            scans.add(0, event.text)
                                        }
                                    }
                                }.onFailure { error ->
                                    status = error.message ?: error::class.java.simpleName
                                }
                            }
                        },
                    ) {
                        Text("Connect")
                    }
                }
                HorizontalDivider()
            }
        }

        Button(
            onClick = {
                scope.launch {
                    scanJob?.cancel()
                    session?.disconnect()
                    session = null
                    status = SessionState.DISCONNECTED.name
                }
            },
            enabled = session != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Disconnect")
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text("Scans", style = MaterialTheme.typography.titleMedium)
        scans.take(5).forEach { Text(it) }
    }
}
