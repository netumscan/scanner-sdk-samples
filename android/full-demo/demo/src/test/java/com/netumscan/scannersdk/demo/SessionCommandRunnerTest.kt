package com.netumscan.scannersdk.demo

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.sync.Mutex
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionCommandRunnerTest {
    @Test
    fun launchReportsBusyWhenCommandMutexIsAlreadyLocked() = runTest {
        val mutex = Mutex()
        mutex.lock()
        var busyCount = 0
        val runner = SessionCommandRunner(
            scope = TestScope(testScheduler),
            mutex = mutex,
            setExecuting = {},
            onBusy = { busyCount += 1 },
        )

        runner.launch {
            error("locked mutex should prevent command execution")
        }

        assertEquals(1, busyCount)
        mutex.unlock()
    }
}
