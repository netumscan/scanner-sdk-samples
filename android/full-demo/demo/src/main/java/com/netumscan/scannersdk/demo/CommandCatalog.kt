package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.ScannerSdk
import com.netumscan.scannersdk.model.BasicDeviceCommand
import com.netumscan.scannersdk.model.CommandCode
import com.netumscan.scannersdk.model.CommandDescriptor
import com.netumscan.scannersdk.model.MasterCommand
import com.netumscan.scannersdk.model.MasterCommandCategory
import com.netumscan.scannersdk.model.MasterCommandMetadata
import com.netumscan.scannersdk.model.MasterCommandSection
import com.netumscan.scannersdk.model.ModuleCommandKind
import com.netumscan.scannersdk.model.localizedModuleActionPresetLabel
import com.netumscan.scannersdk.model.localizedLabel
import com.netumscan.scannersdk.model.localizedRiskLabel

sealed interface ConsoleCommand {
    data class Basic(val command: BasicDeviceCommand) : ConsoleCommand
    data class Master(val command: MasterCommand) : ConsoleCommand
}

private fun moduleCommandTitle(kind: ModuleCommandKind): String =
    kind.localizedLabel().let { SdkLabelResolver.resolve(it.localizationKey, it.fallbackDisplayName) }

private fun moduleActionPresetTitle(actionId: String, fallback: String): String =
    localizedModuleActionPresetLabel(actionId)?.let { SdkLabelResolver.resolve(it.localizationKey, it.fallbackDisplayName) } ?: fallback

private fun commandCodeTitle(command: CommandCode): String =
    command.localizedLabel().let { SdkLabelResolver.resolve(it.localizationKey, it.fallbackDisplayName) }

private fun commandCodeRiskText(command: CommandCode): String =
    command.localizedRiskLabel().let { SdkLabelResolver.resolve(it.localizationKey, it.fallbackDisplayName) }

private fun basicDeviceCommandTitle(command: BasicDeviceCommand): String =
    command.localizedLabel().let { SdkLabelResolver.resolve(it.localizationKey, it.fallbackDisplayName) }

private fun masterCommandTitle(command: MasterCommand): String =
    command.localizedLabel().let { SdkLabelResolver.resolve(it.localizationKey, it.fallbackDisplayName) }

enum class QuickActionCommand {
    REFRESH_INFO,
    GET_BATTERY,
    ACK_BEEP_ON,
    ACK_BEEP_OFF,
    VIBRATE_ON,
    VIBRATE_OFF,
    NTC06H_SAVE,
    NTC06H_ACK_ON,
    NTC06H_CODE39_ON,
    NTC06H_CODE39_OFF,
    NTC06H_EAN13_ON,
    NTC06H_EAN13_OFF,
    NTC06H_CODE128_ON,
    NTC06H_CODE128_OFF,
    NT212X_QR_READ,
    NT212X_QR_ON,
    NT212X_QR_OFF,
    NT280H_SCAN_KEY_MODE,
    NT280H_SCAN_AUTO_MODE,
    NT280H_SCAN_CONTINUOUS_MODE,
    NT280H_SLEEP_NEVER,
    NT280H_SLEEP_10S,
    NT280H_DUPLICATE_500MS,
    NT280H_LIGHT_HIGH,
    NT280H_SENSITIVITY_HIGH,
    SE4750_CAPABILITIES_READ,
    SE4750_AIM_ON,
    SE4750_AIM_OFF,
    SE4750_ILLUMINATION_ON,
    SE4750_ILLUMINATION_OFF,
    SE4750_ALL_SYMBOLOGY_ON,
    SE4750_ALL_SYMBOLOGY_OFF,
    SE4750_BEEP,
    SE4750_PAGER;

    val title: String
        get() = when (this) {
            REFRESH_INFO -> commandCodeTitle(CommandCode.GET_INFO)
            GET_BATTERY -> commandCodeTitle(CommandCode.GET_BATTERY_INFO)
            ACK_BEEP_ON -> commandCodeTitle(CommandCode.BEEP)
            ACK_BEEP_OFF -> commandCodeTitle(CommandCode.DISABLE_ACK_BEEP)
            VIBRATE_ON -> commandCodeTitle(CommandCode.VIBRATE_ON)
            VIBRATE_OFF -> commandCodeTitle(CommandCode.VIBRATE_OFF)
            NTC06H_SAVE -> moduleCommandTitle(ModuleCommandKind.SAVE_SETTINGS)
            NTC06H_ACK_ON -> moduleActionPresetTitle("ntc06h_ack_on", "Enable Setting ACK")
            NTC06H_CODE39_ON -> moduleActionPresetTitle("ntc06h_code39_on", "Code 39 On")
            NTC06H_CODE39_OFF -> moduleActionPresetTitle("ntc06h_code39_off", "Code 39 Off")
            NTC06H_EAN13_ON -> moduleActionPresetTitle("ntc06h_ean13_on", "EAN-13 On")
            NTC06H_EAN13_OFF -> moduleActionPresetTitle("ntc06h_ean13_off", "EAN-13 Off")
            NTC06H_CODE128_ON -> moduleActionPresetTitle("ntc06h_code128_on", "Code 128 On")
            NTC06H_CODE128_OFF -> moduleActionPresetTitle("ntc06h_code128_off", "Code 128 Off")
            NT212X_QR_READ -> moduleActionPresetTitle("nt212x_qr_read", "Read QR")
            NT212X_QR_ON -> moduleActionPresetTitle("nt212x_qr_on", "QR On")
            NT212X_QR_OFF -> moduleActionPresetTitle("nt212x_qr_off", "QR Off")
            NT280H_SCAN_KEY_MODE -> moduleActionPresetTitle("nt280h_scan_key", "Trigger Scan")
            NT280H_SCAN_AUTO_MODE -> moduleActionPresetTitle("nt280h_scan_auto", "Auto Scan")
            NT280H_SCAN_CONTINUOUS_MODE -> moduleActionPresetTitle("nt280h_scan_continuous", "Continuous Scan")
            NT280H_SLEEP_NEVER -> moduleActionPresetTitle("nt280h_sleep_never", "Never Sleep")
            NT280H_SLEEP_10S -> moduleActionPresetTitle("nt280h_sleep_10s", "Sleep 10s")
            NT280H_DUPLICATE_500MS -> moduleActionPresetTitle("nt280h_duplicate_500ms", "Duplicate 500ms")
            NT280H_LIGHT_HIGH -> moduleActionPresetTitle("nt280h_light_high", "High Illumination")
            NT280H_SENSITIVITY_HIGH -> moduleActionPresetTitle("nt280h_sensitivity_high", "High Sensitivity")
            SE4750_CAPABILITIES_READ -> moduleCommandTitle(ModuleCommandKind.CAPABILITIES_REQUEST)
            SE4750_AIM_ON -> moduleCommandTitle(ModuleCommandKind.AIM_ON)
            SE4750_AIM_OFF -> moduleCommandTitle(ModuleCommandKind.AIM_OFF)
            SE4750_ILLUMINATION_ON -> moduleCommandTitle(ModuleCommandKind.ILLUMINATION_ON)
            SE4750_ILLUMINATION_OFF -> moduleCommandTitle(ModuleCommandKind.ILLUMINATION_OFF)
            SE4750_ALL_SYMBOLOGY_ON -> moduleActionPresetTitle("se4750_all_on", "All Symbologies On")
            SE4750_ALL_SYMBOLOGY_OFF -> moduleActionPresetTitle("se4750_all_off", "All Symbologies Off")
            SE4750_BEEP -> moduleCommandTitle(ModuleCommandKind.BEEP)
            SE4750_PAGER -> moduleCommandTitle(ModuleCommandKind.PAGER_MOTOR_ACTIVATION)
        }

    val isDangerous: Boolean
        get() = when (this) {
            REFRESH_INFO -> sdkCommandDescriptor(CommandCode.GET_INFO, false).isDangerous
            GET_BATTERY -> sdkCommandDescriptor(CommandCode.GET_BATTERY_INFO, false).isDangerous
            ACK_BEEP_ON -> sdkCommandDescriptor(CommandCode.BEEP, true).isDangerous
            ACK_BEEP_OFF -> sdkCommandDescriptor(CommandCode.DISABLE_ACK_BEEP, true).isDangerous
            VIBRATE_ON -> sdkCommandDescriptor(CommandCode.VIBRATE_ON, true).isDangerous
            VIBRATE_OFF -> sdkCommandDescriptor(CommandCode.VIBRATE_OFF, true).isDangerous
            NTC06H_SAVE -> isDangerousModuleCommand(ModuleCommandKind.SAVE_SETTINGS, true)
            NTC06H_ACK_ON, NTC06H_CODE39_ON, NTC06H_CODE39_OFF, NTC06H_EAN13_ON, NTC06H_EAN13_OFF, NTC06H_CODE128_ON, NTC06H_CODE128_OFF,
            NT212X_QR_ON, NT212X_QR_OFF,
            NT280H_SCAN_KEY_MODE, NT280H_SCAN_AUTO_MODE, NT280H_SCAN_CONTINUOUS_MODE,
            NT280H_SLEEP_NEVER, NT280H_SLEEP_10S, NT280H_DUPLICATE_500MS, NT280H_LIGHT_HIGH, NT280H_SENSITIVITY_HIGH -> isDangerousModuleCommand(ModuleCommandKind.WRITE_PARAMETER, true)
            NT212X_QR_READ -> isDangerousModuleCommand(ModuleCommandKind.READ_PARAMETER, false)
            SE4750_CAPABILITIES_READ -> isDangerousModuleCommand(ModuleCommandKind.CAPABILITIES_REQUEST, false)
            SE4750_AIM_ON -> isDangerousModuleCommand(ModuleCommandKind.AIM_ON, true)
            SE4750_AIM_OFF -> isDangerousModuleCommand(ModuleCommandKind.AIM_OFF, true)
            SE4750_ILLUMINATION_ON -> isDangerousModuleCommand(ModuleCommandKind.ILLUMINATION_ON, true)
            SE4750_ILLUMINATION_OFF -> isDangerousModuleCommand(ModuleCommandKind.ILLUMINATION_OFF, true)
            SE4750_ALL_SYMBOLOGY_ON, SE4750_ALL_SYMBOLOGY_OFF -> isDangerousModuleCommand(ModuleCommandKind.CHANGE_ALL_CODE_TYPES, true)
            SE4750_BEEP -> isDangerousModuleCommand(ModuleCommandKind.BEEP, true)
            SE4750_PAGER -> isDangerousModuleCommand(ModuleCommandKind.PAGER_MOTOR_ACTIVATION, true)
        }

    val riskText: String
        get() = when (this) {
            REFRESH_INFO -> commandCodeRiskText(CommandCode.GET_INFO)
            GET_BATTERY -> commandCodeRiskText(CommandCode.GET_BATTERY_INFO)
            ACK_BEEP_ON -> commandCodeRiskText(CommandCode.BEEP)
            ACK_BEEP_OFF -> commandCodeRiskText(CommandCode.DISABLE_ACK_BEEP)
            VIBRATE_ON -> commandCodeRiskText(CommandCode.VIBRATE_ON)
            VIBRATE_OFF -> commandCodeRiskText(CommandCode.VIBRATE_OFF)
            NTC06H_SAVE ->
                DemoStrings.text(R.string.risk_ntc06h_save)
            NTC06H_ACK_ON ->
                DemoStrings.text(R.string.risk_ntc06h_ack_on)
            NTC06H_CODE39_ON ->
                DemoStrings.text(R.string.risk_ntc06h_code39_on)
            NTC06H_CODE39_OFF ->
                DemoStrings.text(R.string.risk_ntc06h_code39_off)
            NTC06H_EAN13_ON ->
                DemoStrings.text(R.string.risk_ntc06h_ean13_on)
            NTC06H_EAN13_OFF ->
                DemoStrings.text(R.string.risk_ntc06h_ean13_off)
            NTC06H_CODE128_ON ->
                DemoStrings.text(R.string.risk_ntc06h_code128_on)
            NTC06H_CODE128_OFF ->
                DemoStrings.text(R.string.risk_ntc06h_code128_off)
            NT212X_QR_READ ->
                DemoStrings.text(R.string.risk_nt212x_qr_read)
            NT212X_QR_ON ->
                DemoStrings.text(R.string.risk_nt212x_qr_on)
            NT212X_QR_OFF ->
                DemoStrings.text(R.string.risk_nt212x_qr_off)
            NT280H_SCAN_KEY_MODE ->
                DemoStrings.text(R.string.risk_nt280h_scan_key)
            NT280H_SCAN_AUTO_MODE ->
                DemoStrings.text(R.string.risk_nt280h_scan_auto)
            NT280H_SCAN_CONTINUOUS_MODE ->
                DemoStrings.text(R.string.risk_nt280h_scan_continuous)
            NT280H_SLEEP_NEVER ->
                DemoStrings.text(R.string.risk_nt280h_sleep_never)
            NT280H_SLEEP_10S ->
                DemoStrings.text(R.string.risk_nt280h_sleep_10s)
            NT280H_DUPLICATE_500MS ->
                DemoStrings.text(R.string.risk_nt280h_duplicate_500ms)
            NT280H_LIGHT_HIGH ->
                DemoStrings.text(R.string.risk_nt280h_light_high)
            NT280H_SENSITIVITY_HIGH ->
                DemoStrings.text(R.string.risk_nt280h_sensitivity_high)
            SE4750_CAPABILITIES_READ ->
                DemoStrings.text(R.string.risk_se4750_capabilities_read)
            SE4750_AIM_ON ->
                DemoStrings.text(R.string.risk_se4750_aim_on)
            SE4750_AIM_OFF ->
                DemoStrings.text(R.string.risk_se4750_aim_off)
            SE4750_ILLUMINATION_ON ->
                DemoStrings.text(R.string.risk_se4750_illumination_on)
            SE4750_ILLUMINATION_OFF ->
                DemoStrings.text(R.string.risk_se4750_illumination_off)
            SE4750_ALL_SYMBOLOGY_ON ->
                DemoStrings.text(R.string.risk_se4750_all_symbologies_on)
            SE4750_ALL_SYMBOLOGY_OFF ->
                DemoStrings.text(R.string.risk_se4750_all_symbologies_off)
            SE4750_BEEP ->
                DemoStrings.text(R.string.risk_se4750_beep)
            SE4750_PAGER ->
                DemoStrings.text(R.string.risk_se4750_pager_motor)
        }
}

data class CommandGroup<T>(
    val title: String,
    val commands: List<Pair<String, T>>,
    val key: String,
)

object CommandCatalog {
    fun titleFor(command: ConsoleCommand): String {
        return when (command) {
            is ConsoleCommand.Basic -> basicDeviceCommandTitle(command.command)
            is ConsoleCommand.Master -> masterCommandTitle(command.command)
        }
    }

    private fun basicDescriptor(command: BasicDeviceCommand): CommandDescriptor {
        return runCatching { ScannerSdk.getBasicDeviceCommandDescriptor(command) }
            .getOrElse { CommandDescriptor("", false) }
    }

    private fun masterDescriptor(command: MasterCommand): CommandDescriptor {
        return runCatching { ScannerSdk.getMasterCommandDescriptor(command) }
            .getOrElse { CommandDescriptor("", false) }
    }

    private fun <T> mergeGroups(
        title: String,
        groups: List<CommandGroup<T>>,
        key: String,
    ): CommandGroup<T> {
        return CommandGroup(title = title, commands = groups.flatMap { it.commands }, key = key)
    }


    val quickActionRows: List<CommandGroup<QuickActionCommand>>
        get() = listOf(
            CommandGroup(
                title = DemoStrings.text(R.string.quick_group_info_battery),
                commands = listOf(
                    QuickActionCommand.REFRESH_INFO.title to QuickActionCommand.REFRESH_INFO,
                    QuickActionCommand.GET_BATTERY.title to QuickActionCommand.GET_BATTERY,
                ),
                key = "INFO_BATTERY",
            ),
            CommandGroup(
                title = DemoStrings.text(R.string.quick_group_ack_beep),
                commands = listOf(
                    QuickActionCommand.ACK_BEEP_ON.title to QuickActionCommand.ACK_BEEP_ON,
                    QuickActionCommand.ACK_BEEP_OFF.title to QuickActionCommand.ACK_BEEP_OFF,
                ),
                key = "ACK_BEEP",
            ),
            CommandGroup(
                title = DemoStrings.text(R.string.quick_group_vibrate),
                commands = listOf(
                    QuickActionCommand.VIBRATE_ON.title to QuickActionCommand.VIBRATE_ON,
                    QuickActionCommand.VIBRATE_OFF.title to QuickActionCommand.VIBRATE_OFF,
                ),
                key = "VIBRATE",
            )
        )

    val basicCommands
        get() = listOf(
            BasicDeviceCommand.GET_VERSION,
            BasicDeviceCommand.FACTORY_RESET,
            BasicDeviceCommand.WRITE_CUSTOM_DEFAULTS,
            BasicDeviceCommand.RESTORE_CUSTOM_DEFAULTS,
            BasicDeviceCommand.NORMAL_MODE,
            BasicDeviceCommand.STORE_MODE,
            BasicDeviceCommand.UPLOAD_MEMORY_DATA,
            BasicDeviceCommand.GET_MEMORY_BARCODE_COUNT,
            BasicDeviceCommand.UPLOAD_MEMORY_DATA_AND_CLEAR,
            BasicDeviceCommand.GET_MEMORY_USAGE,
            BasicDeviceCommand.CLEAR_MEMORY,
            BasicDeviceCommand.AUTO_STORE_MODE_OFF,
            BasicDeviceCommand.AUTO_STORE_MODE_ON,
        ).map { command -> basicDeviceCommandTitle(command) to command }

    private val masterSectionCommands: LinkedHashMap<MasterCommandSection, List<MasterCommand>> =
        linkedMapOf(
            MasterCommandSection.POWER_SLEEP to listOf(
                MasterCommand.READ_SLEEP_TIME,
                MasterCommand.POWER_OFF,
                MasterCommand.SLEEP_TIME_1_MIN,
                MasterCommand.SLEEP_TIME_3_MIN,
                MasterCommand.SLEEP_TIME_5_MIN,
                MasterCommand.SLEEP_TIME_10_MIN,
                MasterCommand.SLEEP_TIME_30_MIN,
                MasterCommand.SLEEP_TIME_1_HOUR,
                MasterCommand.SLEEP_TIME_2_HOUR,
                MasterCommand.NEVER_SLEEP,
            ),
            MasterCommandSection.TIMESTAMP to listOf(
                MasterCommand.DISABLE_TIME_STAMP,
                MasterCommand.ENABLE_TIME_STAMP,
            ),
            MasterCommandSection.SCAN_MODE to listOf(
                MasterCommand.KEY_SCAN_MODE_DEFAULT,
                MasterCommand.CONTINUE_SCAN_MODE,
                MasterCommand.KEY_PULSE_SCAN_MODE,
                MasterCommand.HOST_TRIGGER_MODE,
            ),
            MasterCommandSection.SCAN_TIMING to listOf(
                MasterCommand.DECODE_OVERTIME_3S,
                MasterCommand.DECODE_OVERTIME_6S,
                MasterCommand.INTERVAL_TIME_500_MS,
                MasterCommand.INTERVAL_TIME_1000_MS,
                MasterCommand.IMMEDIATE_SCAN_1_S,
                MasterCommand.IMMEDIATE_SCAN_2_S,
                MasterCommand.IMMEDIATE_SCAN_3_S,
                MasterCommand.IMMEDIATE_SCAN_4_S,
                MasterCommand.IMMEDIATE_SCAN_5_S,
                MasterCommand.IMMEDIATE_SCAN_6_S,
                MasterCommand.IMMEDIATE_SCAN_7_S,
            ),
            MasterCommandSection.FEEDBACK_AUDIO to listOf(
                MasterCommand.MUTE,
                MasterCommand.HIGH_VOLUME,
                MasterCommand.MIDDLE_VOLUME,
                MasterCommand.LOW_VOLUME,
                MasterCommand.HIGH_TONE,
                MasterCommand.LOW_TONE,
                MasterCommand.BASE_CONNECT_BEEP_PROMPT_TOGGLE,
            ),
            MasterCommandSection.FEEDBACK_BEEP_PATTERN to listOf(
                MasterCommand.SDK_BEEP_B0,
                MasterCommand.SDK_BEEP_B1,
                MasterCommand.SDK_BEEP_B2,
                MasterCommand.SDK_BEEP_B3,
                MasterCommand.SDK_BEEP_B4,
                MasterCommand.SDK_BEEP_B5,
                MasterCommand.SDK_BEEP_B6,
                MasterCommand.SDK_BEEP_B7,
                MasterCommand.SDK_BEEP_B8,
                MasterCommand.SDK_BEEP_B9,
                MasterCommand.SDK_BEEP_B_COLON,
                MasterCommand.SDK_BEEP_B_SEMICOLON,
                MasterCommand.SDK_BEEP_B_LESS_THAN,
                MasterCommand.SDK_BEEP_B_EQUALS,
                MasterCommand.SDK_BEEP_B_GREATER_THAN,
                MasterCommand.SDK_BEEP_B_QUESTION,
                MasterCommand.SDK_BEEP_B_AT,
                MasterCommand.SDK_BEEP_B_A,
                MasterCommand.SDK_BEEP_B_B,
                MasterCommand.SDK_BEEP_B_C,
                MasterCommand.SDK_BEEP_B_D,
                MasterCommand.SDK_BEEP_B_E,
                MasterCommand.SDK_BEEP_B_F,
                MasterCommand.SDK_BEEP_B_G,
                MasterCommand.SDK_BEEP_B_H,
                MasterCommand.SDK_BEEP_B_I,
                MasterCommand.SDK_BEEP_B_J,
            ),
            MasterCommandSection.RF_TRANSPORT to listOf(
                MasterCommand.READ_INTERFACE_SETTING,
                MasterCommand.SWITCH_RF_24G_TRANSPORT,
            ),
            MasterCommandSection.RF_PAIRING to listOf(
                MasterCommand.RF_PAIR,
                MasterCommand.ONE_TO_ONE_PAIRING,
                MasterCommand.ONE_DONGLE_MANY_SCANNERS_PAIRING,
            ),
            MasterCommandSection.RF_KEYBOARD to listOf(
                MasterCommand.RF_DONGLE_COMPOSITE_DEVICE,
                MasterCommand.RF_DONGLE_VIRTUAL_COM,
                MasterCommand.RF_KEYBOARD_SPEED_HIGH,
                MasterCommand.READ_KEYBOARD_SPEED,
                MasterCommand.RF_KEYBOARD_SPEED_MEDIUM,
                MasterCommand.RF_KEYBOARD_SPEED_LOW,
                MasterCommand.SRAM_BUFFER_TOGGLE,
            ),
            MasterCommandSection.BLUETOOTH_TRANSPORT to listOf(
                MasterCommand.SWITCH_BLUETOOTH_TRANSPORT,
                MasterCommand.BLUETOOTH_HID,
                MasterCommand.BLUETOOTH_SPP,
                MasterCommand.BLUETOOTH_BLE,
                MasterCommand.BT_DONGLE_TRANSPORT_MODE,
            ),
            MasterCommandSection.BLUETOOTH_BEHAVIOR to listOf(
                MasterCommand.UNPAIR_BLUETOOTH_HID,
                MasterCommand.IOS_POPUP_HIDE_KEYBOARD,
                MasterCommand.HOLD_TRIGGER_4_SECONDS,
                MasterCommand.DOUBLE_CLICK_TRIGGER,
                MasterCommand.BT_HID_CAPS_LOCK_IGNORE,
                MasterCommand.HOLD_TRIGGER_8_SECONDS_SWAP_RF_BT,
                MasterCommand.READ_BT_HID_DELAY,
                MasterCommand.BT_HID_DELAY_HIGH,
                MasterCommand.BT_HID_DELAY_VALUE_6,
                MasterCommand.BT_HID_DELAY_MEDIUM,
                MasterCommand.BT_HID_DELAY_VALUE_18,
                MasterCommand.BT_HID_DELAY_LOW,
                MasterCommand.BT_HID_DELAY_VALUE_30,
                MasterCommand.BT_CONNECTED_NOT_SLEEP,
            ),
            MasterCommandSection.BLUETOOTH_INFO to listOf(
                MasterCommand.READ_BT_FIRMWARE_VERSION,
                MasterCommand.READ_BT_NAME,
                MasterCommand.READ_BT_ADDRESS,
                MasterCommand.REBOOT_BT,
                MasterCommand.RESTORE_BT_FACTORY_SETTINGS,
                MasterCommand.DISCONNECT_CURRENT_BT,
            ),
            MasterCommandSection.USB_INTERFACE to listOf(
                MasterCommand.SWITCH_USB_KEYBOARD_MODE,
                MasterCommand.SWITCH_USB_VIRTUAL_COM_MODE,
                MasterCommand.USB_AUTO_INTERFACE_SELECT_ON,
                MasterCommand.USB_AUTO_INTERFACE_SELECT_OFF,
            ),
            MasterCommandSection.USB_KEYBOARD to listOf(
                MasterCommand.USB_KEYBOARD_SPEED_LOW_DELAY,
                MasterCommand.USB_KEYBOARD_SPEED_HIGH,
                MasterCommand.USB_KEYBOARD_SPEED_VALUE_4,
                MasterCommand.USB_KEYBOARD_SPEED_MEDIUM,
                MasterCommand.USB_KEYBOARD_SPEED_VALUE_9,
                MasterCommand.USB_KEYBOARD_SPEED_LOW,
                MasterCommand.USB_HID_MULTI_KEY_ON,
                MasterCommand.USB_HID_MULTI_KEY_OFF,
            ),
            MasterCommandSection.KEY_MODIFIER to listOf(
                MasterCommand.CTRL_KEY_PREFIX_ON,
                MasterCommand.COMBINE_KEY_OFF,
                MasterCommand.ALT_KEY_PREFIX_ON,
                MasterCommand.NORMAL_KEY_CONFIG_2,
                MasterCommand.CASE_STRATEGY_NORMAL,
                MasterCommand.CASE_STRATEGY_SWAP,
                MasterCommand.CASE_STRATEGY_UPPER,
                MasterCommand.CASE_STRATEGY_LOWER,
                MasterCommand.NUM_LOCK_OFF,
                MasterCommand.NUM_LOCK_ON,
            ),
            MasterCommandSection.CHARSET to listOf(
                MasterCommand.READ_CURRENT_CHARSET,
                MasterCommand.CHARSET_AUTO,
                MasterCommand.CHARSET_GBK,
                MasterCommand.CHARSET_UTF8_WORD,
                MasterCommand.CHARSET_ISO_8859,
                MasterCommand.CHARSET_NORMAL,
                MasterCommand.CHARSET_UTF8_TXT,
            ),
            MasterCommandSection.RECEIVE_DEVICE to listOf(
                MasterCommand.READ_CURRENT_RECEIVE_DEVICE,
                MasterCommand.RECEIVE_DEVICE_WINDOWS,
                MasterCommand.RECEIVE_DEVICE_MAC_OS_IOS,
                MasterCommand.RECEIVE_DEVICE_ANDROID,
            ),
            MasterCommandSection.KEYBOARD_LAYOUT to listOf(
                MasterCommand.READ_KEYBOARD_LAYOUT,
                MasterCommand.KEYBOARD_LAYOUT_EN,
                MasterCommand.KEYBOARD_LAYOUT_FR,
                MasterCommand.KEYBOARD_LAYOUT_GE,
                MasterCommand.KEYBOARD_LAYOUT_IT,
                MasterCommand.KEYBOARD_LAYOUT_PT,
                MasterCommand.KEYBOARD_LAYOUT_ES,
                MasterCommand.KEYBOARD_LAYOUT_TK,
                MasterCommand.KEYBOARD_LAYOUT_TF,
                MasterCommand.KEYBOARD_LAYOUT_UK,
                MasterCommand.KEYBOARD_LAYOUT_CS,
                MasterCommand.KEYBOARD_LAYOUT_CY,
                MasterCommand.KEYBOARD_LAYOUT_HU,
                MasterCommand.KEYBOARD_LAYOUT_FB,
                MasterCommand.KEYBOARD_LAYOUT_PB,
                MasterCommand.KEYBOARD_LAYOUT_FC,
                MasterCommand.KEYBOARD_LAYOUT_HR,
                MasterCommand.KEYBOARD_LAYOUT_SK,
                MasterCommand.KEYBOARD_LAYOUT_SQ,
                MasterCommand.KEYBOARD_LAYOUT_DA,
                MasterCommand.KEYBOARD_LAYOUT_FI,
                MasterCommand.KEYBOARD_LAYOUT_EL,
                MasterCommand.KEYBOARD_LAYOUT_NL,
                MasterCommand.KEYBOARD_LAYOUT_NO,
                MasterCommand.KEYBOARD_LAYOUT_PL,
                MasterCommand.KEYBOARD_LAYOUT_SR,
                MasterCommand.KEYBOARD_LAYOUT_SL,
                MasterCommand.KEYBOARD_LAYOUT_SV,
                MasterCommand.KEYBOARD_LAYOUT_DS,
                MasterCommand.KEYBOARD_LAYOUT_JP,
                MasterCommand.KEYBOARD_LAYOUT_TH,
                MasterCommand.KEYBOARD_LAYOUT_AG,
                MasterCommand.KEYBOARD_LAYOUT_RU,
            ),
            MasterCommandSection.DATA_RULE to listOf(
                MasterCommand.CLEAR_ALL_PREFIX,
                MasterCommand.CLEAR_ALL_SUFFIX,
                MasterCommand.SCAN_OPERATION_1,
                MasterCommand.SCAN_OPERATION_2,
                MasterCommand.SCAN_OPERATION_5,
                MasterCommand.SCAN_OPERATION_6,
                MasterCommand.SCAN_OPERATION_7,
                MasterCommand.SCAN_OPERATION_8,
                MasterCommand.NUMERIC_CODE_0,
                MasterCommand.NUMERIC_CODE_1,
                MasterCommand.NUMERIC_CODE_2,
                MasterCommand.NUMERIC_CODE_3,
                MasterCommand.NUMERIC_CODE_4,
                MasterCommand.NUMERIC_CODE_5,
                MasterCommand.NUMERIC_CODE_6,
                MasterCommand.NUMERIC_CODE_7,
                MasterCommand.NUMERIC_CODE_8,
                MasterCommand.NUMERIC_CODE_9,
                MasterCommand.NUMERIC_CODE_A,
                MasterCommand.NUMERIC_CODE_B,
                MasterCommand.NUMERIC_CODE_C,
                MasterCommand.NUMERIC_CODE_D,
                MasterCommand.NUMERIC_CODE_E,
                MasterCommand.NUMERIC_CODE_F,
            ),
            MasterCommandSection.OUTPUT_FORMAT to listOf(
                MasterCommand.CLEAR_OUTPUT_FORMAT,
                MasterCommand.ENABLE_SUFFIX_OUTPUT,
                MasterCommand.ENABLE_PREFIX_OUTPUT,
                MasterCommand.ENABLE_HIDE_END_OUTPUT,
                MasterCommand.ENABLE_HIDE_MIDDLE_OUTPUT,
                MasterCommand.ENABLE_HIDE_START_OUTPUT,
            ),
            MasterCommandSection.REPLACE_RULE to listOf(
                MasterCommand.READ_REPLACE_SET,
                MasterCommand.CLEAR_REPLACE_SET,
            ),
            MasterCommandSection.TERMINATOR to listOf(
                MasterCommand.EXTRA_TERMINAL_NONE,
                MasterCommand.EXTRA_TERMINAL_CR,
                MasterCommand.EXTRA_TERMINAL_TAB,
                MasterCommand.EXTRA_TERMINAL_CRLF,
                MasterCommand.EXTRA_TERMINAL_LF,
            ),
            MasterCommandSection.DECODER_MODULE to listOf(
                MasterCommand.READ_DECODER_MODULE,
                MasterCommand.SET_DECODER_MODULE_0,
                MasterCommand.SET_DECODER_MODULE_1,
                MasterCommand.SET_DECODER_MODULE_2,
                MasterCommand.SET_DECODER_MODULE_3,
                MasterCommand.SET_DECODER_MODULE_4,
                MasterCommand.SET_DECODER_MODULE_5,
            ),
        )

    private val masterSectionOrder = masterSectionCommands.keys.toList()

    private val masterCategoryOrder = listOf(
        MasterCommandCategory.POWER,
        MasterCommandCategory.SCANNING,
        MasterCommandCategory.FEEDBACK,
        MasterCommandCategory.WIRELESS,
        MasterCommandCategory.WIRED,
        MasterCommandCategory.KEYBOARD_ENCODING,
        MasterCommandCategory.DATA_PROCESSING,
        MasterCommandCategory.MODULE,
    )

    private fun masterMetadata(command: MasterCommand) = runCatching {
        ScannerSdk.getMasterCommandMetadata(command)
    }.getOrElse {
        MasterCommandMetadata(
            category = MasterCommandCategory.UNKNOWN,
            section = MasterCommandSection.UNKNOWN,
        )
    }

    private fun sectionTitle(section: MasterCommandSection): String =
        section.localizedLabel().let { SdkLabelResolver.resolve(it.localizationKey, it.fallbackDisplayName) }

    private fun categoryTitle(category: MasterCommandCategory): String =
        category.localizedLabel().let { SdkLabelResolver.resolve(it.localizationKey, it.fallbackDisplayName) }

    val masterGroups: List<CommandGroup<MasterCommand>>
        get() = masterSectionOrder.mapNotNull { section ->
            val commands = masterSectionCommands[section].orEmpty()
                .filter { command -> masterMetadata(command).section == section }
                .map { command -> masterCommandTitle(command) to command }
            if (commands.isEmpty()) null else CommandGroup(sectionTitle(section), commands, key = section.name)
        }

    private fun unifiedBasicCommands(): List<Pair<String, ConsoleCommand>> =
        basicCommands.map { (label, command) -> label to ConsoleCommand.Basic(command) }

    val consoleGroups: List<CommandGroup<ConsoleCommand>>
        get() {
            val unifiedMasterGroups = masterGroups.map { group ->
                CommandGroup<ConsoleCommand>(
                    title = group.title,
                    commands = group.commands.map { (label, command) ->
                        label to ConsoleCommand.Master(command)
                    },
                    key = group.key,
                )
            }
            return listOf(
                CommandGroup<ConsoleCommand>(
                    title = basicGroupTitle(),
                    commands = unifiedBasicCommands(),
                    key = "BASIC",
                )
            ) + unifiedMasterGroups
        }

    private fun basicGroupTitle(): String =
        DemoStrings.text(R.string.basic_command_group)

    private fun masterBucketSectionsByCategory(): Map<MasterCommandCategory, List<CommandGroup<ConsoleCommand>>> {
        val sectionGroups = masterGroups.map { group ->
            val commands = group.commands.map { (label, command) ->
                label to ConsoleCommand.Master(command)
            }
            val firstCommand = group.commands.first().second
            Triple(
                masterMetadata(firstCommand).category,
                masterMetadata(firstCommand).section,
                CommandGroup<ConsoleCommand>(group.title, commands, key = group.key)
            )
        }
        return masterCategoryOrder.associateWith { category ->
            sectionGroups
                .filter { it.first == category }
                .sortedBy { masterSectionOrder.indexOf(it.second) }
                .map { it.third }
        }
    }

    val masterConsoleBuckets: List<CommandGroup<ConsoleCommand>>
        get() {
            val masterSections = masterBucketSectionsByCategory()
            return masterCategoryOrder.mapNotNull { category ->
                val groups = masterSections[category].orEmpty()
                if (category == MasterCommandCategory.POWER) {
                    val mergedGroups = listOf(
                        CommandGroup(basicGroupTitle(), unifiedBasicCommands(), key = "BASIC")
                    ) + groups
                    mergeGroups(categoryTitle(category), mergedGroups, key = category.name)
                } else if (groups.isNotEmpty()) {
                    mergeGroups(categoryTitle(category), groups, key = category.name)
                } else {
                    null
                }
            }
        }

    fun masterBucketSectionsForKey(key: String): List<CommandGroup<ConsoleCommand>> {
        val category = masterCategoryOrder.firstOrNull { it.name == key } ?: return emptyList()
        val categorySections = masterBucketSectionsByCategory()[category].orEmpty()
        return if (category == MasterCommandCategory.POWER) {
            listOf(CommandGroup(basicGroupTitle(), unifiedBasicCommands(), key = "BASIC")) + categorySections
        } else {
            categorySections
        }
    }

    val moduleConsoleBuckets: List<CommandGroup<ConsoleCommand>>
        get() = emptyList()

    val dangerousConsoleCommands: Set<ConsoleCommand>
        get() = consoleGroups
            .flatMap { group -> group.commands.map { it.second } }
            .filterTo(linkedSetOf()) { command ->
                when (command) {
                    is ConsoleCommand.Basic -> basicDescriptor(command.command).isDangerous
                    is ConsoleCommand.Master -> masterDescriptor(command.command).isDangerous
                }
            }

    val recommendedConsoleCommands: Set<ConsoleCommand> = linkedSetOf(
        ConsoleCommand.Basic(BasicDeviceCommand.GET_VERSION),
        ConsoleCommand.Basic(BasicDeviceCommand.GET_MEMORY_BARCODE_COUNT),
        ConsoleCommand.Master(MasterCommand.READ_SLEEP_TIME),
        ConsoleCommand.Master(MasterCommand.KEY_SCAN_MODE_DEFAULT),
        ConsoleCommand.Master(MasterCommand.CONTINUE_SCAN_MODE),
        ConsoleCommand.Master(MasterCommand.READ_BT_FIRMWARE_VERSION),
        ConsoleCommand.Master(MasterCommand.READ_BT_NAME),
        ConsoleCommand.Master(MasterCommand.READ_BT_ADDRESS),
        ConsoleCommand.Master(MasterCommand.READ_INTERFACE_SETTING),
        ConsoleCommand.Master(MasterCommand.READ_DECODER_MODULE),
        ConsoleCommand.Master(MasterCommand.READ_CURRENT_CHARSET),
        ConsoleCommand.Master(MasterCommand.READ_CURRENT_RECEIVE_DEVICE),
        ConsoleCommand.Master(MasterCommand.READ_KEYBOARD_LAYOUT),
        ConsoleCommand.Master(MasterCommand.EXTRA_TERMINAL_CR),
        ConsoleCommand.Master(MasterCommand.EXTRA_TERMINAL_CRLF),
    )

    val dangerousQuickActions = QuickActionCommand.entries.filterTo(linkedSetOf()) { it.isDangerous }
}

private fun sdkCommandDescriptor(command: CommandCode, fallbackDangerous: Boolean): CommandDescriptor {
    return runCatching { ScannerSdk.getCommandDescriptor(command) }
        .getOrElse { CommandDescriptor("", fallbackDangerous) }
}

private fun isDangerousModuleCommand(kind: ModuleCommandKind, fallbackDangerous: Boolean): Boolean {
    return runCatching { ScannerSdk.isModuleCommandDangerous(kind) }.getOrDefault(fallbackDangerous)
}
