package com.netumscan.scannersdk.demo

sealed interface ConsoleCommand

enum class QuickActionCommand {
    REFRESH_INFO,
    GET_BATTERY,
    READ_BT_FIRMWARE_VERSION,
    GET_MEMORY_USAGE,
    SET_ACK_BEEP_ENABLED,
    SET_ACK_BEEP_DISABLED,
    SET_VIBRATION_ENABLED,
    SET_VIBRATION_DISABLED;

    val title: String
        get() = when (this) {
            REFRESH_INFO -> "Get Info"
            GET_BATTERY -> DemoStrings.text(R.string.load_battery_info_action)
            READ_BT_FIRMWARE_VERSION -> "Read Bluetooth Firmware Version"
            GET_MEMORY_USAGE -> DemoStrings.text(R.string.load_memory_usage_action)
            SET_ACK_BEEP_ENABLED -> DemoStrings.text(R.string.ack_beep_on_action)
            SET_ACK_BEEP_DISABLED -> DemoStrings.text(R.string.ack_beep_off_action)
            SET_VIBRATION_ENABLED -> DemoStrings.text(R.string.vibrate_on_action)
            SET_VIBRATION_DISABLED -> DemoStrings.text(R.string.vibrate_off_action)
        }

    val isDangerous: Boolean
        get() = when (this) {
            REFRESH_INFO,
            GET_BATTERY,
            READ_BT_FIRMWARE_VERSION,
            GET_MEMORY_USAGE -> false
            SET_ACK_BEEP_ENABLED,
            SET_ACK_BEEP_DISABLED,
            SET_VIBRATION_ENABLED,
            SET_VIBRATION_DISABLED -> true
        }

    val riskText: String
        get() = when (this) {
            REFRESH_INFO,
            GET_BATTERY,
            READ_BT_FIRMWARE_VERSION,
            GET_MEMORY_USAGE ->
                "This is a read-only action and should not trigger a danger confirmation."
            SET_ACK_BEEP_ENABLED ->
                "This enables the device acknowledgment beep and changes feedback behavior immediately."
            SET_ACK_BEEP_DISABLED ->
                "This disables the device acknowledgment beep and changes feedback behavior immediately."
            SET_VIBRATION_ENABLED ->
                "This enables vibration feedback and changes feedback behavior immediately."
            SET_VIBRATION_DISABLED ->
                "This disables vibration feedback and changes feedback behavior immediately."
        }
}

data class CommandGroup<T>(
    val title: String,
    val commands: List<Pair<String, T>>,
    val key: String,
)

object CommandCatalog {
    fun titleFor(command: ConsoleCommand): String = command.toString()

    val quickActionRows: List<CommandGroup<QuickActionCommand>>
        get() = listOf(
            CommandGroup(
                title = DemoStrings.text(R.string.quick_group_ack_beep),
                commands = listOf(
                    QuickActionCommand.SET_ACK_BEEP_ENABLED.title to QuickActionCommand.SET_ACK_BEEP_ENABLED,
                    QuickActionCommand.SET_ACK_BEEP_DISABLED.title to QuickActionCommand.SET_ACK_BEEP_DISABLED,
                ),
                key = "ACK_BEEP",
            ),
            CommandGroup(
                title = DemoStrings.text(R.string.quick_group_vibrate),
                commands = listOf(
                    QuickActionCommand.SET_VIBRATION_ENABLED.title to QuickActionCommand.SET_VIBRATION_ENABLED,
                    QuickActionCommand.SET_VIBRATION_DISABLED.title to QuickActionCommand.SET_VIBRATION_DISABLED,
                ),
                key = "VIBRATE",
            )
        )

    val consoleGroups: List<CommandGroup<ConsoleCommand>>
        get() = emptyList()

    val masterConsoleBuckets: List<CommandGroup<ConsoleCommand>>
        get() = emptyList()

    fun masterBucketSectionsForKey(key: String): List<CommandGroup<ConsoleCommand>> = emptyList()

    val moduleConsoleBuckets: List<CommandGroup<ConsoleCommand>>
        get() = emptyList()

    val dangerousConsoleCommands: Set<ConsoleCommand>
        get() = emptySet()

    val dangerousQuickActions: Set<QuickActionCommand>
        get() = QuickActionCommand.entries.filterTo(linkedSetOf()) { it.isDangerous }

    val recommendedConsoleCommands: Set<ConsoleCommand>
        get() = emptySet()
}
