package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.ScannerSession
import com.netumscan.scannersdk.model.CommandResponse
import com.netumscan.scannersdk.model.ModuleCommandKind
import com.netumscan.scannersdk.model.ModuleFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class ModuleCommandRequest(
    val family: ModuleFamily,
    val kind: ModuleCommandKind,
    val parameterId: Int = 0,
    val payloadBytes: ByteArray = byteArrayOf(),
    val persist: Boolean = false,
)

internal class ModuleCommandRunner(
    private val canExecuteModuleCommands: () -> Boolean,
    private val notReadyReason: () -> String,
    private val onNotReady: (String, String) -> Unit,
) {
    fun ensureReady(label: String, labelProvider: () -> String): Boolean {
        if (canExecuteModuleCommands()) return true
        val reason = notReadyReason()
        onNotReady(label, reason)
        return false
    }

    suspend fun execute(session: ScannerSession, request: ModuleCommandRequest): CommandResponse {
        return withContext(Dispatchers.IO) {
            session.executeModuleCommand(
                family = request.family,
                kind = request.kind,
                parameterId = request.parameterId,
                payloadBytes = request.payloadBytes,
                persist = request.persist,
            )
        }
    }

    fun isNtc06hSilentAckTimeout(error: Throwable): Boolean {
        val detail = error.demoErrorDetail()
        return detail.contains("code: 5") ||
            detail.contains("code=5") ||
            detail.contains("timeout", ignoreCase = true)
    }
}
