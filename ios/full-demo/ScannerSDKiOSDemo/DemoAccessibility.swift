enum DemoAccessibility {
    static let discoveryList = "ios_demo.discovery.list"
    static let discoveryLastActionBanner = "ios_demo.discovery.last_action_banner"
    static let discoveryErrorBanner = "ios_demo.discovery.error_banner"
    static let discoveryRecoveryActionButton = "ios_demo.discovery.recovery_action_button"
    static let selectedModelPicker = "ios_demo.discovery.selected_model_picker"
    static let initSdkButton = "ios_demo.discovery.init_sdk_button"
    static let startDiscoveryButton = "ios_demo.discovery.start_discovery_button"
    static let stopDiscoveryButton = "ios_demo.discovery.stop_discovery_button"
    static let discoveryDisconnectButton = "ios_demo.discovery.disconnect_button"
    static let openConsoleButton = "ios_demo.discovery.open_console_button"
    static let noDevicesText = "ios_demo.discovery.no_devices_text"

    static let appLogsToolbarButton = "ios_demo.toolbar.app_logs_button"

    static let consoleList = "ios_demo.console.list"
    static let consoleSummaryCard = "ios_demo.console.summary_card"
    static let consoleLastActionBanner = "ios_demo.console.last_action_banner"
    static let consoleErrorBanner = "ios_demo.console.error_banner"
    static let consoleQuickActionsSection = "ios_demo.console.quick_actions_section"
    static let consoleMasterCommandsSection = "ios_demo.console.master_commands_section"
    static let consoleParsingAdvancedSection = "ios_demo.console.parsing_advanced_section"
    static let consoleDataRuleBuilderSection = "ios_demo.console.data_rule_builder_section"
    static let consoleModuleCommandsSection = "ios_demo.console.module_commands_section"
    static let consoleModuleParameterCatalog = "ios_demo.console.module_parameter_catalog"

    static func consoleOperationScopeButton(_ scope: String) -> String {
        "ios_demo.console.operation_scope.\(scope)"
    }

    static func consoleQuickActionButton(_ actionID: String) -> String {
        "ios_demo.console.quick_action.\(actionID)"
    }

    static let logsList = "ios_demo.logs.list"
    static let logsFocusSdkButton = "ios_demo.logs.focus_sdk_button"
    static let logsExportFilteredButton = "ios_demo.logs.export_filtered_button"
    static let logsExportAllButton = "ios_demo.logs.export_all_button"
    static let logsClearButton = "ios_demo.logs.clear_button"
}
