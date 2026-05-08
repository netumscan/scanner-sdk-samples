package com.netumscan.scannersdk.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.netumscan.scannersdk.demo.ui.theme.DemoTheme

class CommandConsoleActivity : ComponentActivity() {
    private val vm by viewModels<CommandConsoleViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DemoLocaleController.initialize(this)
        vm.initialize()
        setContent {
            DemoTheme {
                CommandConsoleRoute(
                    vm = vm,
                    onBack = { finish() },
                    onClose = { finish() },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        vm.setDebugVisible(true)
    }

    override fun onStop() {
        vm.setDebugVisible(false)
        super.onStop()
    }
}
