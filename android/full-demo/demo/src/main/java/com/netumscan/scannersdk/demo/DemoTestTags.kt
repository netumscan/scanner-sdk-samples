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
    const val DISCOVERY_MODEL_STATUS = "discovery_model_status"
    const val DISCOVERY_MODEL_RETRY = "discovery_model_retry"
    const val DISCOVERY_TRANSPORT_BLE = "discovery_transport_ble"
    const val DISCOVERY_TRANSPORT_SPP = "discovery_transport_spp"
    const val DISCOVERY_DEVICE_LIST = "discovery_device_list"

    const val APP_LOG_LIST = "app_log_list"
    const val APP_LOG_TOP_BAR_TITLE = "app_log_top_bar_title"
    const val APP_LOG_FILTER_TITLE = "app_log_filter_title"
    const val APP_LOG_SEARCH_FIELD = "app_log_search_field"
    const val APP_LOG_CLEAR_BUTTON = "app_log_clear_button"
    const val APP_LOG_EMPTY_STATE = "app_log_empty_state"
    const val TOP_BAR_MENU = "top_bar_menu"
    const val APP_LOG_MENU_ITEM = "app_log_menu_item"

    const val CONSOLE_PAGE_LIST = "console_page_list"
    const val CONSOLE_TOP_BAR_TITLE = "console_top_bar_title"
    const val CONSOLE_STATUS_TITLE = "console_status_title"
    const val CONSOLE_SCOPE_MASTER = "console_scope_master"
    const val CONSOLE_SCOPE_SETTINGS = "console_scope_settings"
    const val CONSOLE_SCOPE_DATA_RULES = "console_scope_data_rules"
    const val CONSOLE_SCOPE_DIAGNOSTICS = "console_scope_diagnostics"
    const val CAPABILITY_DOMAIN_DETAIL_TITLE = "capability_domain_detail_title"
    const val CAPABILITY_DOMAIN_DETAIL_LIST = "capability_domain_detail_list"
    const val CONSOLE_SECTION_QUICK_ACTIONS = "console_section_quick_actions"
    const val CONSOLE_SECTION_SETTINGS = "console_section_settings"
    const val CONSOLE_SECTION_PARSE_ADVANCED = "console_section_parse_advanced"
    const val CONSOLE_SECTION_DATA_RULE_BUILDER = "console_section_data_rule_builder"
    const val CONSOLE_DANGER_DIALOG_TITLE = "console_danger_dialog_title"
    const val CONSOLE_DANGER_DIALOG_CONFIRM = "console_danger_dialog_confirm"
    const val CONSOLE_DANGER_DIALOG_CANCEL = "console_danger_dialog_cancel"
    const val CONSOLE_GENERIC_CUSTOM_CLOSE = "console_generic_custom_close"

    fun appLogSourceChip(filter: AppLogSourceFilter): String = "app_log_source_${filter.name.lowercase()}"

    fun discoveryDeviceCard(deviceId: String): String = "discovery_device_${deviceId.slug()}"

    fun discoverySupportedModel(modelKey: String): String = "discovery_model_${modelKey.slug()}"

    fun consoleSettingGroupHeader(groupKey: String): String = "console_setting_group_${groupKey.slug()}"

    fun capabilityDomain(domainKey: String): String = "capability_domain_${domainKey.slug()}"

    fun capabilitySettingGroup(familyKey: String, sectionKey: String): String =
        "capability_setting_group_${familyKey.slug()}_${sectionKey.slug()}"

    fun capabilitySetting(entryKey: String): String = "capability_setting_${entryKey.slug()}"

    fun capabilitySettingDialog(entryKey: String): String = "capability_setting_dialog_${entryKey.slug()}"

    private fun String.slug(): String = replace(Regex("[^A-Za-z0-9_]"), "_")
}

internal fun Modifier.demoTestTag(tag: String?): Modifier {
    return if (tag.isNullOrBlank()) this else testTag(tag)
}
