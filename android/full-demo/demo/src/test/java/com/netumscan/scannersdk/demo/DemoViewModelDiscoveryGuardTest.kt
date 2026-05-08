package com.netumscan.scannersdk.demo

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DemoViewModelDiscoveryGuardTest {
    @Test
    fun startDiscoveryBeforeInitialization_reportsVisibleGuidance() {
        val originalLanguage = DemoLocaleController.currentLanguage
        try {
            DemoLocaleController.setLanguageForTest(DemoLanguage.EN)
            val vm = DemoViewModel()

            vm.startDiscovery()

            assertFalse(vm.uiState.value.isDiscovering)
            assertTrue(vm.uiState.value.lastActionResult.orEmpty().contains("Initialize the SDK"))
        } finally {
            DemoLocaleController.setLanguageForTest(originalLanguage)
        }
    }

    @Test
    fun stopDiscoveryBeforeInitialization_reportsVisibleGuidance() {
        val originalLanguage = DemoLocaleController.currentLanguage
        try {
            DemoLocaleController.setLanguageForTest(DemoLanguage.EN)
            val vm = DemoViewModel()

            vm.stopDiscovery()

            assertFalse(vm.uiState.value.isDiscovering)
            assertTrue(vm.uiState.value.lastActionResult.orEmpty().contains("Initialize the SDK"))
        } finally {
            DemoLocaleController.setLanguageForTest(originalLanguage)
        }
    }
}
