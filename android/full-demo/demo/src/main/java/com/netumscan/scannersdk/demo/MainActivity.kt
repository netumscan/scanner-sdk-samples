package com.netumscan.scannersdk.demo

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netumscan.scannersdk.ScannerSdk
import com.netumscan.scannersdk.localizedLabel
import com.netumscan.scannersdk.model.DiscoveredDevice
import com.netumscan.scannersdk.demo.ui.theme.DemoColors
import com.netumscan.scannersdk.demo.ui.theme.DemoShapes
import com.netumscan.scannersdk.demo.ui.theme.DemoTheme

internal fun discoveryPermissionsForSdk(
    sdkInt: Int,
    mode: DemoTransportMode = DemoTransportMode.BLE,
): Array<String> {
    return when (mode) {
        DemoTransportMode.BLE -> if (sdkInt >= Build.VERSION_CODES.S) {
            // NE2210/Android 16 suppresses discovery callbacks without location permission.
            // Android 12+ requires coarse and fine location to be requested together.
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        DemoTransportMode.SPP -> if (sdkInt >= Build.VERSION_CODES.S) {
            // Keep the same ROM compatibility contract for Classic discovery.
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}

internal fun requiresLocationServiceForDiscovery(
    sdkInt: Int,
    mode: DemoTransportMode? = DemoTransportMode.BLE,
): Boolean {
    return mode != null && sdkInt < Build.VERSION_CODES.S
}

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_FAKE_MODE = "scanner-sdk-demo-fake"
    }

    private val vm by viewModels<DemoViewModel>()
    private var pendingPermissionAction: (() -> Unit)? = null
    private var pendingPermissionNames: Array<String> = emptyArray()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (pendingPermissionNames.all { result[it] == true || hasPermission(it) }) {
            val action = pendingPermissionAction
            pendingPermissionAction = null
            pendingPermissionNames = emptyArray()
            refreshPlatformDiagnostics()
            action?.invoke()
        } else {
            val deniedPermissions = pendingPermissionNames
                .filter { result[it] != true && !hasPermission(it) }
            pendingPermissionAction = null
            pendingPermissionNames = emptyArray()
            refreshPlatformDiagnostics()
            vm.reportDiscoveryPermissionsDenied(
                deniedPermissions = deniedPermissions,
                includeAndroid12LocationNote = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    deniedPermissions.any {
                        it == Manifest.permission.ACCESS_COARSE_LOCATION ||
                            it == Manifest.permission.ACCESS_FINE_LOCATION
                    },
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DemoLocaleController.initialize(this)
        if (isDebuggable() && intent.getBooleanExtra(EXTRA_FAKE_MODE, false)) {
            vm.enableFakeMode()
        }
        setContent {
            DemoTheme {
                MainRoute(
                    vm = vm,
                    onInit = { vm.initialize(this) },
                    onStartDiscovery = { handleStartDiscovery() },
                    onStopDiscovery = { vm.stopDiscovery() },
                    onSelectTransport = vm::setTransportMode,
                    onOpenAppSettings = { openAppSettings() },
                    onOpenLocationSettings = { openLocationSettings() },
                    onOpenBluetoothSettings = { openBluetoothSettings() },
                    onDisconnect = { vm.disconnect() },
                    onOpenActiveConsole = {
                        startActivity(Intent(this, CommandConsoleActivity::class.java))
                    },
                    onOpenConsole = { device ->
                        vm.connect(device) {
                            startActivity(Intent(this, CommandConsoleActivity::class.java))
                        }
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        refreshPlatformDiagnostics()
        vm.refreshLocalizedUi()
        vm.setDebugVisible(true)
    }

    override fun onStop() {
        vm.setDebugVisible(false)
        super.onStop()
    }

    private fun handleStartDiscovery() {
        refreshPlatformDiagnostics()
        runWithRequiredPermissions {
            startDiscoveryIfReady()
        }
    }

    private fun runWithRequiredPermissions(
        mode: DemoTransportMode? = vm.uiState.value.selectedTransportMode,
        action: () -> Unit,
    ) {
        if (hasAllRequiredPermissions(mode)) {
            action()
        } else {
            pendingPermissionAction = action
            pendingPermissionNames = requiredPermissions(mode)
            permissionLauncher.launch(pendingPermissionNames)
        }
    }

    private fun startDiscoveryIfReady() {
        val locationEnabled = isLocationEnabled()
        vm.logUiEvent("Location enabled=$locationEnabled")
        if (requiresLocationServiceForDiscovery(Build.VERSION.SDK_INT, vm.uiState.value.selectedTransportMode) && !locationEnabled) {
            vm.reportDiscoveryLocationDisabled()
            return
        }
        vm.startDiscovery()
    }

    private fun requiredPermissions(
        mode: DemoTransportMode? = vm.uiState.value.selectedTransportMode,
    ): Array<String> {
        return mode?.let { discoveryPermissionsForSdk(Build.VERSION.SDK_INT, it) } ?: emptyArray()
    }

    private fun hasAllRequiredPermissions(
        mode: DemoTransportMode? = vm.uiState.value.selectedTransportMode,
    ): Boolean = requiredPermissions(mode).all(::hasPermission)

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(): Boolean {
        val manager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.isLocationEnabled
        } else {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    private fun refreshPlatformDiagnostics() {
        vm.updatePlatformDiagnostics(
            DemoPlatformDiagnostics(
                demoVersion = demoVersionName(),
                demoBuild = BuildConfig.VERSION_CODE.toString(),
                sdkVersion = runCatching { ScannerSdk.version }.getOrDefault("-"),
                sdkCommit = BuildConfig.SDK_COMMIT,
                androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                bluetoothEnabled = bluetoothEnabled(),
                locationEnabled = isLocationEnabled(),
                bluetoothScanPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    hasPermission(Manifest.permission.BLUETOOTH_SCAN)
                } else {
                    null
                },
                bluetoothConnectPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    null
                },
                fineLocationPermissionGranted = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION),
            )
        )
    }

    private fun bluetoothEnabled(): Boolean? {
        return runCatching {
            (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)
                ?.adapter
                ?.isEnabled
        }.getOrNull()
    }

    private fun demoVersionName(): String {
        return runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "-"
        }.getOrDefault("-")
    }

    private fun isDebuggable(): Boolean {
        return (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        )
        startActivity(intent)
    }

    private fun openLocationSettings() {
        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    private fun openBluetoothSettings() {
        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }
}

@Composable
private fun MainRoute(
    vm: DemoViewModel,
    onInit: () -> Unit,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onSelectTransport: (DemoTransportMode) -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenActiveConsole: () -> Unit,
    onOpenConsole: (DiscoveredDevice) -> Unit,
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentLanguage = DemoLocaleController.currentLanguage

    LaunchedEffect(currentLanguage) {
        vm.refreshLocalizedUi()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = DemoColors.Page) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DemoColors.Page)
        ) {
            DemoTopBar(
                title = demoStringResource(R.string.app_name),
                context = context,
                subtitle = null
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    DiscoveryWorkspaceCard(
                        uiState = uiState,
                        onInit = onInit,
                        onStartDiscovery = onStartDiscovery,
                        onStopDiscovery = onStopDiscovery,
                        onSelectTransport = onSelectTransport,
                        onOpenAppSettings = onOpenAppSettings,
                        onOpenLocationSettings = onOpenLocationSettings,
                        onOpenBluetoothSettings = onOpenBluetoothSettings,
                        onDisconnect = onDisconnect,
                        onOpenActiveConsole = onOpenActiveConsole,
                        onSelectModel = vm::setSelectedModel,
                        onRetrySupportedModels = vm::retrySupportedModels,
                    )
                }
                item {
                    DeviceListSection(
                        canConnect = uiState.canConnectDiscoveredDevice,
                        connectingDeviceId = uiState.connectingDeviceId,
                        devices = uiState.devices,
                        onOpenConsole = onOpenConsole
                    )
                }
            }
        }
    }
}

@Composable
internal fun DiscoveryWorkspaceCard(
    uiState: DiscoveryUiState,
    onInit: () -> Unit,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onSelectTransport: (DemoTransportMode) -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenActiveConsole: () -> Unit,
    onSelectModel: (String) -> Unit,
    onRetrySupportedModels: () -> Unit,
) {
    val selectedModelSummary = uiState.selectedModelSummary.asString()
    val lastActionResult = uiState.lastActionResult?.asString()
    val errorMessage = uiState.errorMessage?.asString()

    DemoSectionCard(title = demoStringResource(R.string.discovery_controls)) {
        lastActionResult?.let {
            Spacer(modifier = Modifier.height(10.dp))
            DemoFeedbackBanner(
                text = it,
                backgroundColor = DemoColors.SurfaceAccent,
                textColor = DemoColors.TextPrimary,
            )
        }
        errorMessage?.let {
            Spacer(modifier = Modifier.height(10.dp))
            DemoFeedbackBanner(
                text = it,
                backgroundColor = DemoColors.SurfaceDanger,
                textColor = DemoColors.Danger,
                actionLabel = uiState.blockerAction?.let { action ->
                    when (action) {
                        DiscoveryBlockerAction.OPEN_APP_SETTINGS -> demoStringResource(R.string.open_app_settings)
                        DiscoveryBlockerAction.OPEN_LOCATION_SETTINGS -> demoStringResource(R.string.open_location_settings)
                        DiscoveryBlockerAction.OPEN_BLUETOOTH_SETTINGS -> demoStringResource(R.string.open_bluetooth_settings)
                    }
                },
                onAction = uiState.blockerAction?.let { action ->
                    when (action) {
                        DiscoveryBlockerAction.OPEN_APP_SETTINGS -> onOpenAppSettings
                        DiscoveryBlockerAction.OPEN_LOCATION_SETTINGS -> onOpenLocationSettings
                        DiscoveryBlockerAction.OPEN_BLUETOOTH_SETTINGS -> onOpenBluetoothSettings
                    }
                },
            )
        }

        Button(
            onClick = onInit,
            modifier = Modifier
                .fillMaxWidth()
                .demoTestTag(DemoTestTags.DISCOVERY_INIT_SDK_BUTTON),
            enabled = uiState.canInitializeSdk,
            colors = ButtonDefaults.buttonColors(
                containerColor = DemoColors.SurfaceBrand,
                contentColor = Color.White
            )
        ) {
            Text(if (uiState.isInitialized) demoStringResource(R.string.sdk_ready) else demoStringResource(R.string.init_sdk))
        }

        TransportSelector(
            selectedTransportMode = uiState.selectedTransportMode,
            onSelectTransport = onSelectTransport,
            enabled = uiState.canChangeDiscoveryTarget,
        )

        var modelMenuExpanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { modelMenuExpanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .demoTestTag(DemoTestTags.DISCOVERY_MODEL_BUTTON),
                border = BorderStroke(1.dp, DemoColors.Outline),
                enabled = uiState.canChangeDiscoveryTarget &&
                    uiState.selectedTransportMode != null &&
                    !uiState.isLoadingSupportedModels &&
                    uiState.supportedModels.isNotEmpty(),
            ) {
                Text(
                    text = if (uiState.isLoadingSupportedModels) {
                        demoStringResource(R.string.loading_supported_models)
                    } else {
                        selectedModelSummary
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = DemoColors.TextPrimary
                )
            }
            DropdownMenu(
                expanded = modelMenuExpanded,
                onDismissRequest = { modelMenuExpanded = false },
                // Keep the popup inside the usable screen instead of letting a long
                // model list extend behind the system navigation bar on some ROMs.
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = minOf(360.dp, (LocalConfiguration.current.screenHeightDp / 2).dp))
            ) {
                uiState.supportedModels.forEach { model ->
                    DropdownMenuItem(
                        modifier = Modifier.demoTestTag(DemoTestTags.discoverySupportedModel(model.modelKey)),
                        text = {
                            Column {
                                Text(
                                    text = model.primaryLabel,
                                    color = DemoColors.TextPrimary,
                                )
                                if (model.secondaryLabel.isNotBlank()) {
                                    Text(
                                        text = model.secondaryLabel,
                                        color = DemoColors.TextSecondary,
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                        },
                        onClick = {
                            modelMenuExpanded = false
                            onSelectModel(model.modelKey)
                        }
                    )
                }
            }
        }

        when {
            uiState.selectedTransportMode == null -> {
                Text(
                    text = demoStringResource(R.string.select_transport_to_load_models),
                    modifier = Modifier.demoTestTag(DemoTestTags.DISCOVERY_MODEL_STATUS),
                    color = DemoColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }
            uiState.isLoadingSupportedModels -> {
                Text(
                    text = demoStringResource(R.string.loading_supported_models),
                    modifier = Modifier.demoTestTag(DemoTestTags.DISCOVERY_MODEL_STATUS),
                    color = DemoColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }
            uiState.supportedModelsError != null -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = uiState.supportedModelsError.asString(),
                        modifier = Modifier
                            .weight(1f)
                            .demoTestTag(DemoTestTags.DISCOVERY_MODEL_STATUS),
                        color = DemoColors.Danger,
                        fontSize = 12.sp,
                    )
                    OutlinedButton(
                        onClick = onRetrySupportedModels,
                        modifier = Modifier.demoTestTag(DemoTestTags.DISCOVERY_MODEL_RETRY),
                    ) {
                        Text(demoStringResource(R.string.retry))
                    }
                }
            }
            uiState.supportedModels.isEmpty() -> {
                Text(
                    text = demoStringResource(R.string.no_supported_models_for_transport),
                    modifier = Modifier.demoTestTag(DemoTestTags.DISCOVERY_MODEL_STATUS),
                    color = DemoColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }
            else -> {
                Text(
                    text = demoStringResource(
                        R.string.supported_models_loaded_summary,
                        uiState.supportedModels.size,
                    ),
                    modifier = Modifier.demoTestTag(DemoTestTags.DISCOVERY_MODEL_STATUS),
                    color = DemoColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }
        }

        if (uiState.selectedTransportMode == null) {
            DemoHintCard(
                text = demoStringResource(R.string.select_transport_before_scan),
                borderColor = DemoColors.Outline,
            )
        } else if (uiState.hasActiveSession) {
            DemoHintCard(
                text = demoStringResource(R.string.disconnect_before_new_discovery),
                borderColor = DemoColors.Outline,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier
                        .weight(1f)
                        .demoTestTag(DemoTestTags.DISCOVERY_DISCONNECT_BUTTON),
                    border = BorderStroke(1.dp, DemoColors.Outline),
                    enabled = uiState.canDisconnect,
                ) {
                    Text(demoStringResource(R.string.disconnect))
                }
                Button(
                    onClick = onOpenActiveConsole,
                    modifier = Modifier
                        .weight(1f)
                        .demoTestTag(DemoTestTags.DISCOVERY_OPEN_CONSOLE_BUTTON),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DemoColors.SurfaceBrand,
                        contentColor = Color.White
                    ),
                    enabled = uiState.canOpenConsole,
                ) {
                    Text(demoStringResource(R.string.open_console))
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartDiscovery,
                    modifier = Modifier
                        .weight(1f)
                        .demoTestTag(DemoTestTags.DISCOVERY_START_BUTTON),
                    enabled = uiState.canStartDiscovery,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DemoColors.SurfaceBrand,
                        contentColor = Color.White
                    )
                ) {
                    Text(demoStringResource(R.string.start_discovery))
                }
                OutlinedButton(
                    onClick = onStopDiscovery,
                    modifier = Modifier
                        .weight(1f)
                        .demoTestTag(DemoTestTags.DISCOVERY_STOP_BUTTON),
                    border = BorderStroke(1.dp, DemoColors.Outline),
                    enabled = uiState.canStopDiscovery,
                ) {
                    Text(demoStringResource(R.string.stop_discovery))
                }
            }
        }
    }
}

@Composable
private fun TransportSelector(
    selectedTransportMode: DemoTransportMode?,
    onSelectTransport: (DemoTransportMode) -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DemoTransportMode.entries.forEach { mode ->
            val text = mode.summary()
            val selected = selectedTransportMode == mode
            if (selected) {
                Button(
                    onClick = { onSelectTransport(mode) },
                    modifier = Modifier
                        .weight(1f)
                        .demoTestTag(transportModeTestTag(mode)),
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DemoColors.SurfaceBrand,
                        contentColor = Color.White
                    )
                ) {
                    Text(text)
                }
            } else {
                OutlinedButton(
                    onClick = { onSelectTransport(mode) },
                    modifier = Modifier
                        .weight(1f)
                        .demoTestTag(transportModeTestTag(mode)),
                    border = BorderStroke(1.dp, DemoColors.Outline),
                    enabled = enabled,
                ) {
                    Text(text)
                }
            }
        }
    }
}

@Composable
internal fun DeviceListSection(
    canConnect: Boolean,
    connectingDeviceId: String?,
    devices: List<DiscoveredDevice>,
    onOpenConsole: (DiscoveredDevice) -> Unit,
) {
    DemoSectionCard(
        title = demoStringResource(R.string.devices),
        modifier = Modifier.demoTestTag(DemoTestTags.DISCOVERY_DEVICE_LIST),
    ) {
        if (devices.isEmpty()) {
            DemoHintCard(
                text = demoStringResource(R.string.no_devices_yet),
                borderColor = DemoColors.Outline,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                devices.forEach { device ->
                    DeviceCard(
                        device = device,
                        clickEnabled = canConnect,
                        isConnecting = connectingDeviceId == device.deviceId,
                        onClick = { onOpenConsole(device) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: DiscoveredDevice,
    clickEnabled: Boolean,
    isConnecting: Boolean,
    onClick: () -> Unit,
) {
    val title = device.name.ifBlank { demoStringResource(R.string.unnamed_device) }
    val signal = device.rssi?.let { "RSSI $it" }
    val transportLabel = device.transportType.localizedLabel().let { SdkLabelResolver.resolve(it.localizationKey, it.fallbackDisplayName) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .demoTestTag(DemoTestTags.discoveryDeviceCard(device.deviceId)),
        shape = DemoShapes.card,
        colors = CardDefaults.cardColors(
            containerColor = if (isConnecting) DemoColors.SurfaceAccent else DemoColors.Surface,
            disabledContainerColor = if (isConnecting) DemoColors.SurfaceAccent else DemoColors.Surface,
        ),
        enabled = clickEnabled && device.connectable != false,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
                    Text(text = device.deviceId, fontSize = 13.sp, color = DemoColors.TextSecondary)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .background(DemoColors.SurfaceAccent, DemoShapes.chip)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(text = transportLabel, fontSize = 11.sp, color = DemoColors.AccentStrong, fontWeight = FontWeight.SemiBold)
                    }
                    signal?.let {
                        Text(text = it, fontSize = 11.sp, color = DemoColors.TextSecondary)
                    }
                }
            }
        }
    }
}

private fun transportModeTestTag(mode: DemoTransportMode): String = when (mode) {
    DemoTransportMode.BLE -> DemoTestTags.DISCOVERY_TRANSPORT_BLE
    DemoTransportMode.SPP -> DemoTestTags.DISCOVERY_TRANSPORT_SPP
}
