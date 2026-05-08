package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.SessionState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DemoSessionCoordinatorTest {
    @Test
    fun shouldDispatchObservedState_keeps_non_terminal_states_bound_to_active_session() {
        assertTrue(
            shouldDispatchObservedState(
                isActiveSession = true,
                state = SessionState.READY,
                acceptTerminalStateAfterCoordinatorClear = false,
            )
        )

        assertFalse(
            shouldDispatchObservedState(
                isActiveSession = false,
                state = SessionState.READY,
                acceptTerminalStateAfterCoordinatorClear = true,
            )
        )
    }

    @Test
    fun shouldDispatchObservedState_allows_terminal_console_state_after_coordinator_clear() {
        assertTrue(
            shouldDispatchObservedState(
                isActiveSession = false,
                state = SessionState.ERROR,
                acceptTerminalStateAfterCoordinatorClear = true,
            )
        )
        assertTrue(
            shouldDispatchObservedState(
                isActiveSession = false,
                state = SessionState.DISCONNECTED,
                acceptTerminalStateAfterCoordinatorClear = true,
            )
        )

        assertFalse(
            shouldDispatchObservedState(
                isActiveSession = false,
                state = SessionState.ERROR,
                acceptTerminalStateAfterCoordinatorClear = false,
            )
        )
    }
}
