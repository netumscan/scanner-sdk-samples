package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.ScannerSdk
import com.netumscan.scannersdk.ScannerSession
import com.netumscan.scannersdk.SessionState
import com.netumscan.scannersdk.model.DeviceModelId
import com.netumscan.scannersdk.model.DiscoveredDevice
import com.netumscan.scannersdk.model.ScanEvent
import com.netumscan.scannersdk.model.SessionFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal object DemoSessionCoordinator {
    var selectedDevice: DiscoveredDevice? = null
        private set
    var activeSession: ScannerSession? = null
        private set
    var activeSessionHandle: DemoSessionHandle? = null
        private set
    var preferredModelId: DeviceModelId = DeviceModelId.UNKNOWN
        private set

    fun bind(device: DiscoveredDevice, session: ScannerSession, preferredModelId: DeviceModelId) {
        selectedDevice = device
        activeSession = session
        activeSessionHandle = RealDemoSessionHandle(session, device.deviceId, device.transportType)
        this.preferredModelId = preferredModelId
    }

    fun bindFake(device: DiscoveredDevice, session: DemoSessionHandle, preferredModelId: DeviceModelId) {
        selectedDevice = device
        activeSession = null
        activeSessionHandle = session
        this.preferredModelId = preferredModelId
    }

    fun clear() {
        selectedDevice = null
        activeSession = null
        activeSessionHandle = null
        preferredModelId = DeviceModelId.UNKNOWN
    }

    fun observe(
        scope: CoroutineScope,
        device: DiscoveredDevice,
        session: ScannerSession,
        acceptTerminalStateAfterCoordinatorClear: Boolean = false,
        onState: suspend (SessionState) -> Unit,
        onScan: suspend (ScanEvent) -> Unit,
        onFailure: suspend (SessionFailure) -> Unit,
    ): List<Job> {
        return listOf(
            scope.launch {
                session.state.collect { state ->
                    if (!shouldDispatchObservedState(
                            isActiveSession = activeSession === session,
                            state = state,
                            acceptTerminalStateAfterCoordinatorClear = acceptTerminalStateAfterCoordinatorClear
                        )
                    ) {
                        return@collect
                    }
                    onState(state)
                    if (state == SessionState.DISCONNECTED || state == SessionState.ERROR) {
                        cancel()
                    }
                }
            },
            scope.launch {
                session.scanEvents.collect { event ->
                    if (activeSession !== session) return@collect
                    onScan(event)
                }
            },
            scope.launch {
                ScannerSdk.sessionFailures.collect { failure ->
                    if (failure.deviceId == device.deviceId && activeSession === session) {
                        onFailure(failure)
                    }
                }
            },
        )
    }
}

internal fun shouldDispatchObservedState(
    isActiveSession: Boolean,
    state: SessionState,
    acceptTerminalStateAfterCoordinatorClear: Boolean,
): Boolean {
    if (isActiveSession) return true
    return acceptTerminalStateAfterCoordinatorClear &&
        (state == SessionState.DISCONNECTED || state == SessionState.ERROR)
}
