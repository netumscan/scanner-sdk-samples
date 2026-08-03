package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.DeviceCapabilitySummary
import com.netumscan.scannersdk.model.SessionOperationSupport
import com.netumscan.scannersdk.model.DeviceSupportStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DemoModelSelectionTest {

    @Test
    fun formatSelectedModelSummary_marks_customer_choice_as_primary() {
        DemoLocaleController.setLanguageForTest(DemoLanguage.ZH)

        assertEquals(
            "客户选择型号: CS7501",
            formatSelectedModelSummary("CS7501")
        )
    }

    @Test
    fun displayModelLabel_uses_sdk_model_vocabulary() {
        assertEquals("CS7501", displayModelLabel("CS7501"))
        assertEquals("NT-91", displayModelLabel("NT-91"))
        assertEquals("NT-1228BC", displayModelLabel("NT-1228BC"))
    }

    @Test
    fun formatSdkResolvedModelSummary_marks_mismatch_against_selected_model() {
        DemoLocaleController.setLanguageForTest(DemoLanguage.ZH)

        assertEquals(
            "SDK 诊断型号: C740（与客户选择 CS7501 不一致）",
            formatSdkResolvedModelSummary(
                selectedModelKey = "CS7501",
                resolvedModel = "C740",
            )
        )
    }

    @Test
    fun discovery_feedback_refreshesAfterLanguageSwitch() {
        val vm = DemoViewModel()
        DemoLocaleController.setLanguageForTest(DemoLanguage.ZH)
        vm.setSelectedModel("NT-91")
        assertEquals(
            "测试目标型号已切换；下次扫描会按该型号做发现过滤",
            vm.uiState.value.lastActionResult.orEmpty()
        )

        DemoLocaleController.setLanguageForTest(DemoLanguage.EN)
        vm.refreshLocalizedUi()

        assertEquals(
            "Test target model updated; the next discovery will filter by this model",
            vm.uiState.value.lastActionResult.orEmpty()
        )
    }

    @Test
    fun formatCapabilitySummary_keeps_public_fields_in_expected_slots() {
        DemoLocaleController.setLanguageForTest(DemoLanguage.ZH)
        val capability = DeviceCapabilitySummary(
            modelKey = "CS7501",
            modelName = "CS7501",
            supportsScanControl = false,
            supportsDeviceCommands = true,
            supportsSettingsRead = true,
            supportsSettingsWrite = true,
            supportsDataRules = false,
            supportsBattery = true,
            supportStatus = DeviceSupportStatus.CODE_ONLY,
        )

        assertEquals(
            "deviceCommands=支持 / settingsRead=支持 / settingsWrite=支持 / dataRules=不支持 / battery=支持 / status=Code only",
            formatCapabilitySummary(capability).asStringForCurrentLanguage()
        )
    }

    @Test
    fun sessionOperationSupportSummary_maps_demo_operation_gates() {
        val support = SessionOperationSupport(
            supportsRefreshInfo = false,
            supportsInitializeSession = true,
            supportsGetBatteryInfo = false,
            supportsApplyDataRule = true,
            supportsTriggerScan = false,
            supportsSetAckBeepEnabled = true,
            supportsSetVibrationEnabled = true,
        )

        assertFalse(support.supports(DemoSessionOperation.REFRESH_INFO))
        assertTrue(support.supports(DemoSessionOperation.INITIALIZE_SESSION))
        assertFalse(support.supports(DemoSessionOperation.GET_BATTERY_INFO))
        assertTrue(support.supports(DemoSessionOperation.APPLY_DATA_RULE))
        assertTrue(support.supports(DemoSessionOperation.SET_ACK_BEEP_ENABLED))
        assertTrue(support.supports(DemoSessionOperation.SET_VIBRATION_ENABLED))
    }
}
