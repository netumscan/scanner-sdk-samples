enum DemoAccessibility {
    static let discoveryList = "ios_demo.discovery.list"
    static let discoveryLastActionBanner = "ios_demo.discovery.last_action_banner"
    static let discoveryErrorBanner = "ios_demo.discovery.error_banner"
    static let discoveryRecoveryActionButton = "ios_demo.discovery.recovery_action_button"
    static let selectedModelPicker = "ios_demo.discovery.selected_model_picker"
    static let supportedModelsStatus = "ios_demo.discovery.supported_models_status"
    static let supportedModelsRetryButton = "ios_demo.discovery.supported_models_retry_button"
    static let initSdkButton = "ios_demo.discovery.init_sdk_button"
    static let startDiscoveryButton = "ios_demo.discovery.start_discovery_button"
    static let stopDiscoveryButton = "ios_demo.discovery.stop_discovery_button"
    static let discoveryDisconnectButton = "ios_demo.discovery.disconnect_button"
    static let openConsoleButton = "ios_demo.discovery.open_console_button"
    static let noDevicesText = "ios_demo.discovery.no_devices_text"

    static let appLogsToolbarButton = "ios_demo.toolbar.app_logs_button"

    static let consoleList = "ios_demo.console.list"
    static let consoleSummaryCard = "ios_demo.console.summary_card"
    static let consoleDisconnectButton = "ios_demo.console.disconnect_button"
    static let consoleAckBeepOnButton = "ios_demo.console.ack_beep_on_button"
    static let consoleAckBeepOffButton = "ios_demo.console.ack_beep_off_button"
    static let consoleVibrateOnButton = "ios_demo.console.vibrate_on_button"
    static let consoleVibrateOffButton = "ios_demo.console.vibrate_off_button"
    static let logsList = "ios_demo.logs.list"
    static let logsFocusSdkButton = "ios_demo.logs.focus_sdk_button"
    static let logsExportFilteredButton = "ios_demo.logs.export_filtered_button"
    static let logsExportAllButton = "ios_demo.logs.export_all_button"
    static let logsClearButton = "ios_demo.logs.clear_button"

    static func consoleOperationScopeButton(_ scope: String) -> String {
        "ios_demo.console.operation_scope.\(scope)"
    }

    static func consoleTab(_ tab: String) -> String {
        "ios_demo.console.tab.\(tab)"
    }

    static func capabilityDomain(_ key: String) -> String {
        "ios_demo.console.capability_domain.\(key)"
    }

    static func capabilityDomainDetail(_ key: String) -> String {
        "ios_demo.console.capability_domain_detail.\(key)"
    }

    static func capabilitySettingGroup(_ family: String, _ section: String) -> String {
        "ios_demo.console.capability_group.\(family).\(section)"
    }

    static func capabilitySetting(_ key: String) -> String {
        "ios_demo.console.capability_setting.\(key)"
    }

    static func capabilitySettingEditor(_ key: String) -> String {
        "ios_demo.console.capability_setting_editor.\(key)"
    }
}
