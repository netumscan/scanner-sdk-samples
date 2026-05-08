package com.netumscan.scannersdk.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun CommandConsoleRoute(
    vm: CommandConsoleViewModel,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentLanguage = DemoLocaleController.currentLanguage

    LaunchedEffect(vm) {
        vm.closePageEvents.collect { onClose() }
    }

    LaunchedEffect(currentLanguage) {
        vm.refreshLocalizedUi()
    }

    CommandConsolePageContent(
        vm = vm,
        uiState = uiState,
        context = context,
        onBack = onBack,
    )
}
