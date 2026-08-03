package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.TransportType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class DemoViewModelSupportedModelsTest {
    @Test
    fun selectingTransport_loadsModelsFromSdkProviderAndSelectsSupportedFallback() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            var requestedTransport: TransportType? = null
            val vm = DemoViewModel(
                supportedModelsLoader = { transport ->
                    requestedTransport = transport
                    listOf(
                        DemoSupportedModel("NT-1228BC", "NT-1228BC", "nt", "NT series"),
                        DemoSupportedModel("C740", "C740", "c", "C series"),
                    )
                },
                supportedModelsDispatcher = dispatcher,
            )

            vm.setTransportMode(DemoTransportMode.BLE)
            advanceUntilIdle()

            assertEquals(TransportType.BLE_GATT, requestedTransport)
            assertEquals(listOf("NT-1228BC", "C740"), vm.uiState.value.supportedModels.map { it.modelKey })
            assertEquals("NT-1228BC", vm.uiState.value.selectedModelKey)
            assertFalse(vm.uiState.value.isLoadingSupportedModels)
            assertEquals(null, vm.uiState.value.supportedModelsError)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun modelProviderFailure_exposesRetryableErrorAndClearsStaleModels() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val vm = DemoViewModel(
                supportedModelsLoader = { error("catalog unavailable") },
                supportedModelsDispatcher = dispatcher,
            )

            vm.setTransportMode(DemoTransportMode.SPP)
            advanceUntilIdle()

            assertEquals(emptyList(), vm.uiState.value.supportedModels)
            assertFalse(vm.uiState.value.isLoadingSupportedModels)
            assertNotNull(vm.uiState.value.supportedModelsError)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
