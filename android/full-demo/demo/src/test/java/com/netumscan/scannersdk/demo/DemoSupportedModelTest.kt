package com.netumscan.scannersdk.demo

import kotlin.test.Test
import kotlin.test.assertEquals

class DemoSupportedModelTest {
    @Test
    fun normalizeSupportedModels_preservesSdkOrderAndRemovesInvalidDuplicates() {
        val models = listOf(
            DemoSupportedModel("CS7501", "CS7501", "c_pro", "C Pro series"),
            DemoSupportedModel("", "Invalid", "", ""),
            DemoSupportedModel("NT-91", "NT-91", "fixed", "Fixed scanner module"),
            DemoSupportedModel("cs7501", "Duplicate", "c_pro", "C Pro series"),
        )

        assertEquals(listOf("CS7501", "NT-91"), normalizeSupportedModels(models).map { it.modelKey })
    }

    @Test
    fun selectSupportedModel_keepsCurrentSelectionOrFallsBackToFirstSdkModel() {
        val models = listOf(
            DemoSupportedModel("CS7501", "CS7501", "c_pro", "C Pro series"),
            DemoSupportedModel("NT-91", "NT-91", "fixed", "Fixed scanner module"),
        )

        assertEquals("NT-91", selectSupportedModel(models, "nt-91"))
        assertEquals("CS7501", selectSupportedModel(models, "missing"))
        assertEquals("", selectSupportedModel(emptyList(), "CS7501"))
    }

    @Test
    fun displayLabels_useSdkModelAndSeriesNames() {
        val model = DemoSupportedModel(
            modelKey = "NT212X",
            modelName = "NT212X module",
            seriesKey = "fixed_scanner_module",
            seriesName = "Fixed scanner module",
        )

        assertEquals("NT212X · NT212X module", model.primaryLabel)
        assertEquals("Fixed scanner module", model.secondaryLabel)
    }
}
