package com.netumscan.scannersdk.quickstart

import com.netumscan.scannersdk.SessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuickStartViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialization selects CS7501 and sorts the supported model list`() = runTest(dispatcher) {
        val backend = FakeBackend(
            models = listOf(
                QuickModel("Z200", "Zulu"),
                QuickModel("CS7501", "CS7501"),
                QuickModel("A100", "Alpha"),
                QuickModel("cs7501", "Duplicate"),
            ),
        )

        val viewModel = QuickStartViewModel(backend)
        advanceUntilIdle()

        assertEquals("CS7501", viewModel.uiState.value.selectedModelKey)
        assertEquals(listOf("A100", "CS7501", "Z200"), viewModel.uiState.value.models.map { it.modelKey })
        assertEquals("Ready to discover", viewModel.uiState.value.status)
    }

    @Test
    fun `discovery replaces duplicate devices without exposing another row`() = runTest(dispatcher) {
        val backend = FakeBackend()
        val viewModel = QuickStartViewModel(backend)
        advanceUntilIdle()

        viewModel.startDiscovery()
        advanceUntilIdle()
        backend.emitDevice(QuickDevice("private-id", "Scanner", "CS7501", -70))
        backend.emitDevice(QuickDevice("private-id", "Scanner", "CS7501", -30))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.devices.size)
        assertEquals(-30, viewModel.uiState.value.devices.single().rssi)
    }

    @Test
    fun `connection receives only the latest twenty scans and clears on disconnect`() = runTest(dispatcher) {
        val session = FakeSession()
        val backend = FakeBackend(session = session)
        val viewModel = QuickStartViewModel(backend)
        advanceUntilIdle()
        val device = QuickDevice("private-id", "Scanner", "CS7501", -20)

        viewModel.connect(device)
        advanceUntilIdle()
        repeat(21) { session.emitScan(QuickScanEvent(it, "scan-$it")) }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isReady)
        assertEquals(20, state.scans.size)
        assertEquals("scan-20", state.scans.first().text)
        assertEquals("scan-1", state.scans.last().text)

        session.states.value = SessionState.DISCONNECTED
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isReady)
        assertTrue(viewModel.uiState.value.canRetry)
        assertEquals("Connection lost", viewModel.uiState.value.status)
    }

    @Test
    fun `quick actions call supported session operations and publish safe summaries`() = runTest(dispatcher) {
        val session = FakeSession()
        val viewModel = QuickStartViewModel(FakeBackend(session = session))
        advanceUntilIdle()
        viewModel.connect(QuickDevice("private-id", "Scanner", "CS7501", -20))
        advanceUntilIdle()

        viewModel.triggerScan()
        advanceUntilIdle()
        viewModel.refreshInfo()
        advanceUntilIdle()
        viewModel.readBattery()
        advanceUntilIdle()

        assertEquals(1, session.triggerCalls)
        assertEquals(1, session.infoCalls)
        assertEquals(1, session.batteryCalls)
        assertEquals("Scanner · CS7501 · FW 1.2.3 · HW A", viewModel.uiState.value.deviceInfo)
        assertEquals("75% · 3.8V", viewModel.uiState.value.battery)
    }

    @Test
    fun `unsupported actions stay disabled and do not call the session`() = runTest(dispatcher) {
        val session = FakeSession(
            support = QuickOperationSupport(refreshInfo = false, battery = false, triggerScan = false),
        )
        val viewModel = QuickStartViewModel(FakeBackend(session = session))
        advanceUntilIdle()
        viewModel.connect(QuickDevice("private-id", "Scanner", "CS7501", -20))
        advanceUntilIdle()

        viewModel.triggerScan()
        viewModel.refreshInfo()
        viewModel.readBattery()
        advanceUntilIdle()

        assertEquals(0, session.triggerCalls)
        assertEquals(0, session.infoCalls)
        assertEquals(0, session.batteryCalls)
        assertEquals("Read Battery is not supported by this session", viewModel.uiState.value.lastAction)
    }

    @Test
    fun `permission denial enables manual discovery recovery`() = runTest(dispatcher) {
        val viewModel = QuickStartViewModel(FakeBackend())
        advanceUntilIdle()

        viewModel.showPermissionDenied()

        assertEquals("Bluetooth permission is required", viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.canRetry)
        assertFalse(viewModel.uiState.value.isDiscovering)
    }

    @Test
    fun `command failure is surfaced without leaving the command busy`() = runTest(dispatcher) {
        val session = FakeSession(failCommands = true)
        val viewModel = QuickStartViewModel(FakeBackend(session = session))
        advanceUntilIdle()
        viewModel.connect(QuickDevice("private-id", "Scanner", "CS7501", -20))
        advanceUntilIdle()

        viewModel.triggerScan()
        advanceUntilIdle()

        assertEquals("Command failed", viewModel.uiState.value.lastAction)
        assertFalse(viewModel.uiState.value.isRunningCommand)
        assertTrue(viewModel.uiState.value.isReady)
    }
}

private class FakeBackend(
    private val models: List<QuickModel> = listOf(
        QuickModel("CS7501", "CS7501"),
        QuickModel("C750", "C750"),
    ),
    private val session: FakeSession = FakeSession(),
) : QuickStartBackend {
    private val discoveryEvents = MutableSharedFlow<QuickDevice>(extraBufferCapacity = 8)
    private val failureEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)

    override val sdkVersion: String = "1.0.0"
    override val discoveries: Flow<QuickDevice> = discoveryEvents
    override val discoveryFailures: Flow<String> = failureEvents

    override suspend fun initialize(): List<QuickModel> = models
    override suspend fun startDiscovery(modelKey: String) = Unit
    override suspend fun stopDiscovery() = Unit
    override suspend fun connect(device: QuickDevice): QuickSession = session
    override suspend fun shutdown() = Unit

    fun emitDevice(device: QuickDevice) {
        discoveryEvents.tryEmit(device)
    }
}

private class FakeSession(
    private val support: QuickOperationSupport = QuickOperationSupport(
        refreshInfo = true,
        battery = true,
        triggerScan = true,
    ),
    private val failCommands: Boolean = false,
) : QuickSession {
    override val states = MutableStateFlow(SessionState.READY)
    private val scanEvents = MutableSharedFlow<QuickScanEvent>(extraBufferCapacity = 32)
    private val failureEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    override val scans: Flow<QuickScanEvent> = scanEvents
    override val failures: Flow<String> = failureEvents

    var triggerCalls = 0
    var infoCalls = 0
    var batteryCalls = 0

    override suspend fun operationSupport(): QuickOperationSupport = support
    override suspend fun resolvedModelKey(): String = "CS7501"

    override suspend fun triggerScan() {
        if (failCommands) error("Command failed")
        triggerCalls += 1
    }

    override suspend fun refreshInfo(): QuickDeviceInfo {
        infoCalls += 1
        return QuickDeviceInfo("Scanner", "CS7501", "1.2.3", "A")
    }

    override suspend fun readBattery(): QuickBattery {
        batteryCalls += 1
        return QuickBattery(75, "3.8V")
    }

    override suspend fun disconnect() {
        states.value = SessionState.DISCONNECTED
    }

    fun emitScan(event: QuickScanEvent) {
        scanEvents.tryEmit(event)
    }
}
