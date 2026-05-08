package com.netumscan.scannersdk.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.netumscan.scannersdk.demo.ui.theme.DemoTheme

class AppLogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DemoLocaleController.initialize(this)
        setContent {
            DemoTheme {
                AppLogRoute(onBack = { finish() })
            }
        }
    }
}
