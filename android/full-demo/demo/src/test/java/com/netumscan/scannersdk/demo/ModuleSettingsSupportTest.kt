package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.ModuleFamily
import com.netumscan.scannersdk.model.ModuleNumericInputKind
import com.netumscan.scannersdk.model.Ntc06hSettingCatalog
import com.netumscan.scannersdk.model.moduleNumericInputTextFromPayload
import com.netumscan.scannersdk.model.moduleNumericInputToPayloadHex
import com.netumscan.scannersdk.model.parseModuleParameterId
import com.netumscan.scannersdk.model.parseModulePayloadHex
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ModuleSettingsSupportTest {
    private lateinit var originalLanguage: DemoLanguage

    @Before
    fun setUp() {
        originalLanguage = DemoLocaleController.currentLanguage
        DemoLocaleController.setLanguageForTest(DemoLanguage.ZH)
    }

    @After
    fun tearDown() {
        DemoLocaleController.setLanguageForTest(originalLanguage)
    }

    @Test
    fun parseModuleParameterId_acceptsShortAndLongHex() {
        assertEquals(0xF025, parseModuleParameterId("F025"))
        assertEquals(0x38, parseModuleParameterId("0x38"))
    }

    @Test
    fun parseModulePayloadHex_acceptsSpacedHex() {
        assertContentEquals(byteArrayOf(0x01, 0x0D, 0x0A), parseModulePayloadHex("01 0D 0A"))
    }

    @Test
    fun parseModulePayloadHex_rejectsOddLength() {
        assertFailsWith<IllegalArgumentException> {
            parseModulePayloadHex("0")
        }
    }

    @Test
    fun se4750_presets_expose_quick_values_for_high_frequency_numeric_parameters() {
        val presets = ModuleSettingsCatalog.presetsFor(ModuleFamily.SE4750).associateBy { it.parameterId }

        assertEquals(listOf("0.5 秒", "1.0 秒", "3.0 秒", "5.0 秒", "9.9 秒"), presets.getValue(0x88).quickValueOptions.map { it.label() })
        assertEquals(listOf("0.0 秒", "0.6 秒", "1.0 秒", "2.0 秒"), presets.getValue(0x89).quickValueOptions.map { it.label() })
        assertEquals(listOf("0.1 秒", "0.2 秒", "0.5 秒", "1.0 秒"), presets.getValue(0x90).quickValueOptions.map { it.label() })
        assertEquals(listOf("低", "中", "高"), presets.getValue(0xF19C).quickValueOptions.map { it.label() })
        assertEquals(listOf("1", "5", "10"), presets.getValue(0xF19D).quickValueOptions.map { it.label() })
        assertEquals(listOf("0 毫秒", "200 毫秒", "400 毫秒", "1000 毫秒"), presets.getValue(0xF1D0).quickValueOptions.map { it.label() })
        assertTrue(presets.getValue(0xF1D0).hint().contains("200 毫秒"))
    }

    @Test
    fun se4750_numeric_input_helpers_encode_decode_and_validate_ranges() {
        val presets = ModuleSettingsCatalog.presetsFor(ModuleFamily.SE4750).associateBy { it.parameterId }

        val decodeTimeoutSpec = presets.getValue(0x88).numericInputSpec!!
        assertEquals("05", moduleNumericInputToPayloadHex(decodeTimeoutSpec, "5"))
        assertEquals("5", moduleNumericInputTextFromPayload(decodeTimeoutSpec, "05"))
        assertTrue(moduleNumericInputError(decodeTimeoutSpec, "4")!!.contains("5-99"))

        val aimBrightnessSpec = presets.getValue(0xF19C).numericInputSpec!!
        assertEquals("02", moduleNumericInputToPayloadHex(aimBrightnessSpec, "2"))
        assertEquals("2", moduleNumericInputTextFromPayload(aimBrightnessSpec, "02"))
        assertTrue(moduleNumericInputError(aimBrightnessSpec, "3")!!.contains("0-2"))

        val pdfTimeoutSpec = presets.getValue(0xF1D0).numericInputSpec!!
        assertEquals("00C8", moduleNumericInputToPayloadHex(pdfTimeoutSpec, "200"))
        assertEquals("200", moduleNumericInputTextFromPayload(pdfTimeoutSpec, "00C8"))
        assertTrue(moduleNumericInputError(pdfTimeoutSpec, "5001")!!.contains("0-5000"))
    }

    @Test
    fun nt212x_length_presets_use_decimal_numeric_input() {
        val presets = ModuleSettingsCatalog.presetsFor(ModuleFamily.NT212X).associateBy { it.parameterId }

        val code39Length1Spec = presets.getValue(0x12).numericInputSpec!!
        assertEquals(ModuleNumericInputKind.UINT8_DECIMAL, code39Length1Spec.kind)
        assertEquals(0, code39Length1Spec.minValue)
        assertEquals(99, code39Length1Spec.maxValue)
        assertEquals("14", moduleNumericInputTextFromPayload(code39Length1Spec, "0E"))
        assertEquals("0E", moduleNumericInputToPayloadHex(code39Length1Spec, "14"))

        val code39Length2Spec = presets.getValue(0x13).numericInputSpec!!
        assertEquals("99", moduleNumericInputTextFromPayload(code39Length2Spec, "63"))
        assertTrue(moduleNumericInputError(code39Length2Spec, "100")!!.contains("0-99"))
    }

    @Test
    fun enum_option_labels_come_from_sdk_metadata() {
        val preset = ModuleSettingsCatalog.presetsFor(ModuleFamily.NT280H)
            .first { it.definition.key == "A0_00" }

        assertEquals(
            listOf("关闭 ACK 响应", "开启 ACK 响应"),
            preset.enumOptions.map { it.label() }
        )
    }

    @Test
    fun ntc06h_setting_title_comes_from_generated_sdk_resources() {
        val setting = Ntc06hSettingCatalog.findByKey("system_000b0")!!

        DemoLocaleController.setLanguageForTest(DemoLanguage.ZH)
        assertEquals("恢复出厂值", setting.localizedTitle())

        DemoLocaleController.setLanguageForTest(DemoLanguage.EN)
        assertEquals("Factory Reset", setting.localizedTitle())
    }

    @Test
    fun ntc06h_group_titles_resolve_for_current_language() {
        DemoLocaleController.setLanguageForTest(DemoLanguage.ZH)
        val domainGroup = buildNtc06hDomainGroups().first { it.families.isNotEmpty() }
        val familyGroup = domainGroup.families.first { it.sections.isNotEmpty() }
        val sectionGroup = familyGroup.sections.first()

        val zhDomainTitle = ModuleSettingsCatalog.domainTitleForKey(domainGroup.key)
        val zhFamilyTitle = ModuleSettingsCatalog.familyTitleForKey(familyGroup.key)
        val zhSectionTitle = ModuleSettingsCatalog.sectionTitleForKey(sectionGroup.key)

        DemoLocaleController.setLanguageForTest(DemoLanguage.EN)
        assertNotEquals(zhDomainTitle, ModuleSettingsCatalog.domainTitleForKey(domainGroup.key))
        assertNotEquals(zhFamilyTitle, ModuleSettingsCatalog.familyTitleForKey(familyGroup.key))
        assertNotEquals(zhSectionTitle, ModuleSettingsCatalog.sectionTitleForKey(sectionGroup.key))
    }
}
