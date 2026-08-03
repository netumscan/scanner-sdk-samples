package com.netumscan.scannersdk.quickstart

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val quickStartViewModel: QuickStartViewModel = viewModel(
                factory = QuickStartViewModel.factory(
                    ScannerQuickStartBackend(applicationContext),
                ),
            )
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    QuickStartScreen(quickStartViewModel)
                }
            }
        }
    }
}

@Composable
private fun QuickStartScreen(viewModel: QuickStartViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.all { it }) viewModel.startDiscovery() else viewModel.showPermissionDenied()
    }

    fun startWithPermission() {
        val granted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (granted) viewModel.startDiscovery() else permissionLauncher.launch(requiredPermissions)
    }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.padding(top = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        stringResource(R.string.sdk_version, state.sdkVersion.ifBlank { "…" }),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(state.status, style = MaterialTheme.typography.bodyMedium)
                }
            }

            item {
                ModelPicker(
                    state = state,
                    onSelect = viewModel::selectModel,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = ::startWithPermission,
                        enabled = state.selectedModelKey.isNotBlank() && !state.isDiscovering &&
                            !state.isConnecting && !state.isReady,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            stringResource(
                                if (state.canRetry) R.string.retry_discovery else R.string.start_discovery,
                            ),
                        )
                    }
                    OutlinedButton(
                        onClick = viewModel::stopDiscovery,
                        enabled = state.isDiscovering,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.stop_discovery))
                    }
                    OutlinedButton(
                        onClick = viewModel::disconnect,
                        enabled = state.isReady || state.isConnecting,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.disconnect))
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.devices)) }
            if (state.devices.isEmpty()) {
                item { Text(stringResource(R.string.no_devices), style = MaterialTheme.typography.bodyMedium) }
            } else {
                items(state.devices, key = QuickDevice::deviceId) { device ->
                    DeviceRow(
                        device = device,
                        canConnect = !state.isConnecting && !state.isReady,
                        onConnect = { viewModel.connect(device) },
                    )
                }
            }

            item {
                QuickActions(state = state, viewModel = viewModel)
            }

            item {
                SummaryCard(
                    title = stringResource(R.string.device_info),
                    value = state.deviceInfo,
                )
            }
            item {
                SummaryCard(
                    title = stringResource(R.string.battery),
                    value = state.battery,
                )
            }
            if (state.lastAction.isNotBlank()) {
                item { Text(state.lastAction, style = MaterialTheme.typography.bodyMedium) }
            }

            item { SectionTitle(stringResource(R.string.recent_scans)) }
            if (state.scans.isEmpty()) {
                item { Text(stringResource(R.string.no_scans)) }
            } else {
                items(state.scans, key = ScanRow::id) { scan ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Text(scan.text.ifBlank { "(empty scan)" })
                        Text(
                            stringResource(R.string.scan_summary, scan.barcodeType, scan.text.length),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    HorizontalDivider()
                }
            }
            item { Text("", modifier = Modifier.padding(bottom = 16.dp)) }
        }
    }
}

@Composable
private fun ModelPicker(
    state: QuickStartUiState,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = state.models.firstOrNull { it.modelKey == state.selectedModelKey }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.target_model), fontWeight = FontWeight.SemiBold)
        OutlinedButton(
            onClick = { expanded = true },
            enabled = !state.isDiscovering && !state.isConnecting && !state.isReady && state.models.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(selected?.let { "${it.modelName} (${it.modelKey})" } ?: "No BLE models")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.models.forEach { model ->
                DropdownMenuItem(
                    text = { Text("${model.modelName} (${model.modelKey})") },
                    onClick = {
                        expanded = false
                        onSelect(model.modelKey)
                    },
                )
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: QuickDevice,
    canConnect: Boolean,
    onConnect: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(device.displayName(), fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(
                        R.string.device_summary,
                        device.modelKey.ifBlank { "Model unresolved" },
                        device.rssi?.toString() ?: "—",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(onClick = onConnect, enabled = canConnect) {
                Text(stringResource(R.string.connect))
            }
        }
    }
}

@Composable
private fun QuickActions(
    state: QuickStartUiState,
    viewModel: QuickStartViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.quick_actions))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = viewModel::triggerScan,
                enabled = state.isReady && state.operationSupport.triggerScan && !state.isRunningCommand,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.trigger_scan))
            }
            OutlinedButton(
                onClick = viewModel::refreshInfo,
                enabled = state.isReady && state.operationSupport.refreshInfo && !state.isRunningCommand,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.refresh_info))
            }
            OutlinedButton(
                onClick = viewModel::readBattery,
                enabled = state.isReady && state.operationSupport.battery && !state.isRunningCommand,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.read_battery))
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(value)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}
