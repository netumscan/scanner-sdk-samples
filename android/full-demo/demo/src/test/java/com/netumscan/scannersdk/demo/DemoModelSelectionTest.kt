package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.CommandSetKind
import com.netumscan.scannersdk.model.DeviceCapabilitySummary
import com.netumscan.scannersdk.model.DeviceFormFactor
import com.netumscan.scannersdk.model.DeviceModelId
import com.netumscan.scannersdk.model.ModuleFamily
import com.netumscan.scannersdk.model.SessionOperationSupportSummary
import com.netumscan.scannersdk.model.SupportStatus
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
            formatSelectedModelSummary(DeviceModelId.CS7501)
        )
    }

    @Test
    fun displayModelLabel_uses_sdk_model_vocabulary() {
        assertEquals("CS7501", displayModelLabel(DeviceModelId.CS7501))
        assertEquals("NT-91", displayModelLabel(DeviceModelId.NT91))
        assertEquals("NT-1228BC", displayModelLabel(DeviceModelId.NT1228BC))
    }

    @Test
    fun formatSdkResolvedModelSummary_marks_mismatch_against_selected_model() {
        DemoLocaleController.setLanguageForTest(DemoLanguage.ZH)

        assertEquals(
            "SDK 诊断型号: C740（与客户选择 CS7501 不一致）",
            formatSdkResolvedModelSummary(
                selectedModelId = DeviceModelId.CS7501,
                resolvedModel = DeviceModelId.C740,
            )
        )
    }

    @Test
    fun discovery_feedback_refreshesAfterLanguageSwitch() {
        val vm = DemoViewModel()
        DemoLocaleController.setLanguageForTest(DemoLanguage.ZH)
        vm.setSelectedModel(DeviceModelId.NT91)
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
    fun mergePreferredCapabilityWithRuntime_keeps_selected_model_metadata_and_runtime_bridge_flags() {
        val preferred = DeviceCapabilitySummary(
            modelId = DeviceModelId.CS7501,
            modelName = "CS7501",
            defaultCommandSet = CommandSetKind.MASTER_WITH_MODULE_INFO,
            formFactor = DeviceFormFactor.MASTER_WITH_MODULE,
            moduleFamily = ModuleFamily.NT212X,
            supportsBasicDeviceCommands = true,
            supportsMasterCommands = true,
            supportsNativeModuleCommands = false,
            supportsModuleCommandBridge = false,
            supportsModuleCommands = false,
            supportsScannerMaster = true,
            supportsModulePassthrough = false,
            supportStatus = SupportStatus.CODE_ONLY,
        )
        val runtime = preferred.copy(
            supportsModuleCommandBridge = true,
            supportsModuleCommands = true,
            supportsScannerMaster = true,
        )

        val merged = mergePreferredCapabilityWithRuntime(preferred, runtime)

        assertEquals(DeviceModelId.CS7501, merged.modelId)
        assertEquals(ModuleFamily.NT212X, merged.moduleFamily)
        assertTrue(merged.supportsModuleCommandBridge)
        assertTrue(merged.supportsModuleCommands)
        assertFalse(merged.supportsModulePassthrough)
    }

    @Test
    fun formatCapabilitySummary_keeps_format_arguments_in_expected_slots() {
        DemoLocaleController.setLanguageForTest(DemoLanguage.ZH)
        val capability = DeviceCapabilitySummary(
            modelId = DeviceModelId.CS7501,
            modelName = "CS7501",
            defaultCommandSet = CommandSetKind.MASTER_WITH_MODULE_INFO,
            formFactor = DeviceFormFactor.MASTER_WITH_MODULE,
            moduleFamily = ModuleFamily.NT212X,
            supportsBasicDeviceCommands = true,
            supportsMasterCommands = true,
            supportsNativeModuleCommands = false,
            supportsModuleCommandBridge = true,
            supportsModuleCommands = true,
            supportsScannerMaster = true,
            supportsModulePassthrough = false,
            supportStatus = SupportStatus.CODE_ONLY,
        )

        assertEquals(
            "整机含模组信息 / 整机带模组 / master=支持 / scannerMaster=支持 / status=仅代码支持",
            formatCapabilitySummary(capability).asStringForCurrentLanguage()
        )
    }

    @Test
    fun sessionOperationSupportSummary_maps_demo_operation_gates() {
        val support = SessionOperationSupportSummary(
            supportsRefreshInfo = false,
            supportsInitializeSession = true,
            supportsGetBatteryInfo = false,
            supportsExecuteBasicDeviceCommands = true,
            supportsExecuteTextCommands = false,
            supportsExecuteDataRuleCommands = true,
            supportsDefaultModuleCommandProbe = true,
            supportsTriggerScan = false,
            supportsBeep = true,
            supportsDisableAckBeep = false,
            supportsVibrateOn = true,
            supportsVibrateOff = false,
        )

        assertFalse(support.supports(DemoSessionOperation.REFRESH_INFO))
        assertTrue(support.supports(DemoSessionOperation.INITIALIZE_SESSION))
        assertFalse(support.supports(DemoSessionOperation.GET_BATTERY_INFO))
        assertTrue(support.supports(DemoSessionOperation.BASIC_DEVICE_COMMANDS))
        assertFalse(support.supports(DemoSessionOperation.TEXT_COMMANDS))
        assertTrue(support.supports(DemoSessionOperation.DATA_RULE_COMMANDS))
        assertTrue(support.supports(DemoSessionOperation.BEEP))
        assertFalse(support.supports(DemoSessionOperation.DISABLE_ACK_BEEP))
        assertTrue(support.supports(DemoSessionOperation.VIBRATE_ON))
        assertFalse(support.supports(DemoSessionOperation.VIBRATE_OFF))
    }
}
