package com.netumscan.scannersdk.demo

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModuleCommandRunnerTest {
    @Test
    fun ensureReadyReportsUnsupportedModuleCommandState() {
        var warning: String? = null
        val runner = ModuleCommandRunner(
            canExecuteModuleCommands = { false },
            notReadyReason = { "module not ready" },
            onNotReady = { label, reason -> warning = "$label: $reason" },
        )

        val ready = runner.ensureReady("Read module", labelProvider = { "Read module" })

        assertFalse(ready)
        assertTrue(warning?.contains("module not ready") == true)
    }

    @Test
    fun ntc06hTimeoutClassifierAcceptsSdkTimeoutText() {
        val runner = ModuleCommandRunner(
            canExecuteModuleCommands = { true },
            notReadyReason = { "" },
            onNotReady = { _, _ -> },
        )

        assertTrue(runner.isNtc06hSilentAckTimeout(IllegalStateException("ScannerError code=5 timeout")))
    }
}
