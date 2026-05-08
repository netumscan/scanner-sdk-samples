package com.netumscan.scannersdk.demo

import kotlin.test.Test
import kotlin.test.assertEquals

class DemoScenarioPresetTest {
    @Test
    fun presetsKeepExpectedScenarioOrder() {
        assertEquals(
            listOf(
                DemoScenarioPresetKind.QUICK_SCAN,
                DemoScenarioPresetKind.READ_INFO,
                DemoScenarioPresetKind.READ_BATTERY,
                DemoScenarioPresetKind.MASTER_COMMANDS,
                DemoScenarioPresetKind.MODULE_PARAMETERS,
                DemoScenarioPresetKind.DATA_RULE_BUILDER,
            ),
            DemoScenarioPresets.all().map { it.kind },
        )
    }
}
