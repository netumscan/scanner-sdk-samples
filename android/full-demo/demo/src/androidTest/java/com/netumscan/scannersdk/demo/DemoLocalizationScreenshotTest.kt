package com.netumscan.scannersdk.demo

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.netumscan.scannersdk.TransportType
import com.netumscan.scannersdk.demo.ui.theme.DemoColors
import com.netumscan.scannersdk.demo.ui.theme.DemoTheme
import com.netumscan.scannersdk.model.DeviceModelId
import com.netumscan.scannersdk.model.DiscoveredDevice
import com.netumscan.scannersdk.model.ModuleFamily
import java.io.File
import java.io.FileOutputStream
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class DemoLocalizationScreenshotTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    companion object {
        @JvmStatic
        @BeforeClass
        fun cleanScreenshotDirectory() {
            outputDirectory().deleteRecursively()
        }

        private fun outputDirectory(): File {
            val arguments = InstrumentationRegistry.getArguments()
            val customPath = arguments.getString("demoScreenshotDir")
            if (!customPath.isNullOrBlank()) {
                return File(customPath)
            }
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            return File(context.filesDir, "demo-localization-screenshots")
        }
    }

    private enum class ScreenshotLanguage(val fileValue: String, val demoLanguage: DemoLanguage) {
        ZH("zh", DemoLanguage.ZH),
        EN("en", DemoLanguage.EN),
    }

    private enum class ScreenshotScenario(val fileValue: String) {
        DISCOVERY("discovery"),
        CONSOLE_MASTER("console_master"),
        CONSOLE_MODULE_PARAMETERS("console_module_parameters"),
        DANGER_DIALOG("danger_dialog"),
        ERROR_RECOVERY("error_recovery"),
        LOGS_EXPORT("logs_export"),
    }

    @Before
    fun setUp() {
        DemoSessionCoordinator.clear()
        AppLogStore.clear()
    }

    @Test
    fun captureZhDiscovery() = captureScenario(ScreenshotLanguage.ZH, ScreenshotScenario.DISCOVERY)

    @Test
    fun captureZhConsoleMaster() = captureScenario(ScreenshotLanguage.ZH, ScreenshotScenario.CONSOLE_MASTER)

    @Test
    fun captureZhConsoleModuleParameters() =
        captureScenario(ScreenshotLanguage.ZH, ScreenshotScenario.CONSOLE_MODULE_PARAMETERS)

    @Test
    fun captureZhDangerDialog() = captureScenario(ScreenshotLanguage.ZH, ScreenshotScenario.DANGER_DIALOG)

    @Test
    fun captureZhErrorRecovery() = captureScenario(ScreenshotLanguage.ZH, ScreenshotScenario.ERROR_RECOVERY)

    @Test
    fun captureZhLogsExport() = captureScenario(ScreenshotLanguage.ZH, ScreenshotScenario.LOGS_EXPORT)

    @Test
    fun captureEnDiscovery() = captureScenario(ScreenshotLanguage.EN, ScreenshotScenario.DISCOVERY)

    @Test
    fun captureEnConsoleMaster() = captureScenario(ScreenshotLanguage.EN, ScreenshotScenario.CONSOLE_MASTER)

    @Test
    fun captureEnConsoleModuleParameters() =
        captureScenario(ScreenshotLanguage.EN, ScreenshotScenario.CONSOLE_MODULE_PARAMETERS)

    @Test
    fun captureEnDangerDialog() = captureScenario(ScreenshotLanguage.EN, ScreenshotScenario.DANGER_DIALOG)

    @Test
    fun captureEnErrorRecovery() = captureScenario(ScreenshotLanguage.EN, ScreenshotScenario.ERROR_RECOVERY)

    @Test
    fun captureEnLogsExport() = captureScenario(ScreenshotLanguage.EN, ScreenshotScenario.LOGS_EXPORT)

    private fun captureScenario(language: ScreenshotLanguage, scenario: ScreenshotScenario) {
        DemoLocaleController.setLanguageForTest(composeRule.activity, language.demoLanguage)
        prepareScenario(scenario)
        captureScreenshot(language, scenario)
    }

    private fun prepareScenario(scenario: ScreenshotScenario) {
        when (scenario) {
            ScreenshotScenario.DISCOVERY -> {
                composeRule.setDiscoveryContent(discoveryState())
                waitUntilTagExists(DemoTestTags.DISCOVERY_DEVICE_LIST)
            }
            ScreenshotScenario.CONSOLE_MASTER -> {
                composeRule.setConsoleContent()
                waitUntilTagExists(DemoTestTags.CONSOLE_PAGE_LIST)
                composeRule.onNodeWithTag(DemoTestTags.CONSOLE_SECTION_QUICK_ACTIONS).performClick()
                composeRule.onNodeWithTag(DemoTestTags.CONSOLE_PAGE_LIST)
                    .performScrollToNode(hasTestTag(DemoTestTags.CONSOLE_SECTION_QUICK_ACTIONS))
            }
            ScreenshotScenario.CONSOLE_MODULE_PARAMETERS -> {
                composeRule.setConsoleContent(
                    initialState = CommandConsolePageInitialState(
                        startInModuleScope = true,
                        moduleSettingsExpanded = true,
                    )
                )
                waitUntilTagExists(DemoTestTags.CONSOLE_SECTION_MODULE_COMMANDS)
                composeRule.onNodeWithTag(DemoTestTags.CONSOLE_PAGE_LIST)
                    .performScrollToNode(hasTestTag(DemoTestTags.CONSOLE_SECTION_MODULE_COMMANDS))
            }
            ScreenshotScenario.DANGER_DIALOG -> {
                composeRule.setConsoleContent()
                waitUntilTagExists(DemoTestTags.CONSOLE_PAGE_LIST)
                composeRule.onNodeWithTag(DemoTestTags.CONSOLE_SECTION_QUICK_ACTIONS).performClick()
                composeRule.waitForIdle()
                composeRule.onNodeWithTag(DemoTestTags.CONSOLE_PAGE_LIST)
                    .performScrollToNode(hasText(SdkLabelResolver.resolve("nsdk.command_code.ack_beep_on", "Ack Beep On")))
                composeRule.onNodeWithText(SdkLabelResolver.resolve("nsdk.command_code.ack_beep_on", "Ack Beep On")).performClick()
                waitUntilTagExists(DemoTestTags.CONSOLE_DANGER_DIALOG_TITLE)
            }
            ScreenshotScenario.ERROR_RECOVERY -> {
                composeRule.setDiscoveryContent(
                    discoveryState(
                        errorMessage = DemoStrings.text(R.string.discovery_location_service_disabled),
                        blockerAction = DiscoveryBlockerAction.OPEN_LOCATION_SETTINGS,
                    )
                )
                waitUntilTagExists(DemoTestTags.DISCOVERY_DEVICE_LIST)
            }
            ScreenshotScenario.LOGS_EXPORT -> {
                seedLogs()
                composeRule.setContent {
                    DemoTheme {
                        AppLogRoute(onBack = {}, autoScrollToLatest = false)
                    }
                }
                waitUntilTagExists(DemoTestTags.APP_LOG_LIST)
            }
        }
        composeRule.waitForIdle()
    }

    private fun captureScreenshot(
        language: ScreenshotLanguage,
        scenario: ScreenshotScenario,
    ) {
        val file = File(
            screenshotOutputDirectory(),
            "android_${language.fileValue}_${scenario.fileValue}.png"
        )
        file.parentFile?.mkdirs()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        FileOutputStream(file).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "Failed to encode screenshot ${file.name}"
            }
        }
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.setDiscoveryContent(
        uiState: DiscoveryUiState,
    ) {
        setContent {
            DemoTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = DemoColors.Page) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DemoColors.Page)
                    ) {
                        DemoTopBar(
                            title = demoStringResource(R.string.app_name),
                            context = composeRule.activity,
                            subtitle = null,
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item {
                                DiscoveryWorkspaceCard(
                                    uiState = uiState,
                                    onInit = {},
                                    onStartDiscovery = {},
                                    onStopDiscovery = {},
                                    onSelectTransport = {},
                                    onOpenAppSettings = {},
                                    onOpenLocationSettings = {},
                                    onOpenBluetoothSettings = {},
                                    onDisconnect = {},
                                    onOpenActiveConsole = {},
                                    onSelectModel = {},
                                )
                            }
                            item {
                                DeviceListSection(
                                    canConnect = uiState.canConnectDiscoveredDevice,
                                    connectingDeviceId = uiState.connectingDeviceId,
                                    devices = uiState.devices,
                                    onOpenConsole = {},
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.setConsoleContent(
        initialState: CommandConsolePageInitialState = CommandConsolePageInitialState(),
    ) {
        val vm = CommandConsoleViewModel()
        setContent {
            DemoTheme {
                CommandConsolePageContent(
                    vm = vm,
                    uiState = CommandConsoleUiState(
                        currentModuleFamily = ModuleFamily.NTC06H,
                        supportsModuleCommands = true,
                        canExecuteModuleCommands = true,
                        hasReadySession = true,
                        capabilitySummary = uiText(R.string.capability_summary),
                        moduleSummary = rawDisplayText("NTC06H"),
                    ),
                    context = composeRule.activity,
                    onBack = {},
                    initialState = initialState,
                )
            }
        }
    }

    private fun discoveryState(
        errorMessage: String? = null,
        blockerAction: DiscoveryBlockerAction? = null,
    ): DiscoveryUiState {
        return DiscoveryUiState(
            statusSummary = uiText(R.string.sdk_ready),
            selectedDeviceSummary = uiText(R.string.no_connected_device),
            selectedModelId = DeviceModelId.NT91,
            selectedModelSummary = rawDisplayText(displayModelLabel(DeviceModelId.NT91)),
            selectedTransportMode = DemoTransportMode.BLE,
            selectedTransportSummary = dynamicText { DemoTransportMode.BLE.summary() },
            devices = listOf(
                DiscoveredDevice(
                    deviceId = "UI-TEST-NT91",
                    name = "NT91 Demo Scanner",
                    transportType = TransportType.BLE_GATT,
                    modelId = DeviceModelId.NT91,
                    rssi = -48,
                )
            ),
            lastActionResult = uiText(R.string.sdk_already_initialized),
            errorMessage = errorMessage?.let(::rawDisplayText),
            blockerAction = blockerAction,
            isInitialized = true,
        )
    }

    private fun seedLogs() {
        AppLogStore.append(
            DebugEvent(
                source = DebugEventSource.UI,
                level = DebugEventLevel.Info,
                message = "ui tapped connect",
                timestampMs = 1,
            )
        )
        AppLogStore.append(
            DebugEvent(
                source = DebugEventSource.SDK,
                level = DebugEventLevel.Warn,
                message = "sdk startup complete",
                timestampMs = 2,
            )
        )
        AppLogStore.append(
            DebugEvent(
                source = DebugEventSource.SCAN,
                level = DebugEventLevel.Error,
                message = "scan beta payload",
                timestampMs = 3,
            )
        )
    }

    private fun waitUntilTagExists(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun screenshotOutputDirectory(): File {
        return outputDirectory()
    }
}
