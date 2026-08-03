package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.ScannerSession
import com.netumscan.scannersdk.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

internal class SessionCommandRunner(
    private val scope: CoroutineScope,
    private val mutex: Mutex,
    private val setExecuting: (Boolean) -> Unit,
    private val onBusy: () -> Unit,
) {
    fun launch(block: suspend () -> Unit) {
        if (!mutex.tryLock()) {
            onBusy()
            return
        }
        scope.launch {
            setExecuting(true)
            try {
                block()
            } finally {
                setExecuting(false)
                mutex.unlock()
            }
        }
    }

    fun requireReadySession(
        session: ScannerSession?,
        onMissing: () -> Unit,
        onNotReady: (SessionState) -> Unit,
    ): ScannerSession? {
        val currentSession = session ?: run {
            onMissing()
            return null
        }
        val state = currentSession.state.value
        if (state != SessionState.READY) {
            onNotReady(state)
            return null
        }
        return currentSession
    }

    suspend fun ensureOperationSupport(
        session: ScannerSession,
        operation: DemoSessionOperation,
        labelProvider: () -> String,
        unsupportedReasonProvider: (DemoSessionOperation) -> String,
        onUnsupported: (String, String) -> Unit,
    ): Boolean {
        val supported = withContext(Dispatchers.IO) {
            session.getOperationSupport().supports(operation)
        }
        if (supported) return true
        val label = labelProvider()
        val reason = unsupportedReasonProvider(operation)
        onUnsupported(label, reason)
        return false
    }
}
