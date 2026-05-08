package com.netumscan.scannersdk.demo

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

internal object DemoTestTags {
    const val DISCOVERY_INIT_SDK_BUTTON = "discovery_init_sdk_button"
    const val DISCOVERY_START_BUTTON = "discovery_start_button"
    const val DISCOVERY_STOP_BUTTON = "discovery_stop_button"
    const val DISCOVERY_OPEN_CONSOLE_BUTTON = "discovery_open_console_button"
    const val DISCOVERY_DISCONNECT_BUTTON = "discovery_disconnect_button"
    const val DISCOVERY_MODEL_BUTTON = "discovery_model_button"
    const val DISCOVERY_TRANSPORT_BLE = "discovery_transport_ble"
    const val DISCOVERY_TRANSPORT_SPP = "discovery_transport_spp"
    const val DISCOVERY_DEVICE_LIST = "discovery_device_list"

    const val APP_LOG_LIST = "app_log_list"
    const val APP_LOG_TOP_BAR_TITLE = "app_log_top_bar_title"
    const val APP_LOG_FILTER_TITLE = "app_log_filter_title"
    const val APP_LOG_SEARCH_FIELD = "app_log_search_field"
    const val APP_LOG_CLEAR_BUTTON = "app_log_clear_button"
    const val APP_LOG_EMPTY_STATE = "app_log_empty_state"

    const val CONSOLE_PAGE_LIST = "console_page_list"
    const val CONSOLE_TOP_BAR_TITLE = "console_top_bar_title"
    const val CONSOLE_STATUS_TITLE = "console_status_title"
    const val CONSOLE_SCOPE_MASTER = "console_scope_master"
    const val CONSOLE_SCOPE_MODULE = "console_scope_module"
    const val CONSOLE_SECTION_QUICK_ACTIONS = "console_section_quick_actions"
    const val CONSOLE_SECTION_MODULE_COMMANDS = "console_section_module_commands"
    const val CONSOLE_SECTION_PARSE_ADVANCED = "console_section_parse_advanced"
    const val CONSOLE_SECTION_DATA_RULE_BUILDER = "console_section_data_rule_builder"
    const val CONSOLE_DANGER_DIALOG_TITLE = "console_danger_dialog_title"
    const val CONSOLE_DANGER_DIALOG_CONFIRM = "console_danger_dialog_confirm"
    const val CONSOLE_DANGER_DIALOG_CANCEL = "console_danger_dialog_cancel"
    const val CONSOLE_GENERIC_CUSTOM_CLOSE = "console_generic_custom_close"
    const val CONSOLE_NTC06H_CUSTOM_CLOSE = "console_ntc06h_custom_close"

    fun appLogSourceChip(filter: AppLogSourceFilter): String = "app_log_source_${filter.name.lowercase()}"

    fun discoveryDeviceCard(deviceId: String): String = "discovery_device_${deviceId.slug()}"

    fun consoleModuleFamilyHeader(familyKey: String): String = "console_module_family_${familyKey.slug()}"

    fun consoleModuleDomainTab(index: Int): String = "console_module_domain_tab_$index"

    fun consoleModuleCustomEdit(parameterId: Int): String = "console_module_custom_${parameterId.toString(16)}"

    fun consoleNtc06hCustomEdit(settingKey: String): String = "console_ntc06h_custom_${settingKey.slug()}"

    private fun String.slug(): String = replace(Regex("[^A-Za-z0-9_]"), "_")
}

internal fun Modifier.demoTestTag(tag: String?): Modifier {
    return if (tag.isNullOrBlank()) this else testTag(tag)
}
