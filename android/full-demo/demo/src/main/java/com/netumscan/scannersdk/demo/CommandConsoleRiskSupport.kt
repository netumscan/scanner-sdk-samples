package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.BasicDeviceCommand
import com.netumscan.scannersdk.model.MasterCommand
import com.netumscan.scannersdk.model.localizedRiskLabel

internal fun basicCommandRiskText(command: BasicDeviceCommand): String {
    val label = command.localizedRiskLabel()
    return SdkLabelResolver.resolve(label.localizationKey, label.fallbackDisplayName)
}

internal fun commandRiskText(command: ConsoleCommand): String {
    return when (command) {
        is ConsoleCommand.Basic -> basicCommandRiskText(command.command)
        is ConsoleCommand.Master -> masterCommandRiskText(command.command)
    }
}

internal fun masterCommandRiskText(command: MasterCommand): String {
    val label = command.localizedRiskLabel()
    return SdkLabelResolver.resolve(label.localizationKey, label.fallbackDisplayName)
}

internal fun quickActionRiskText(action: QuickActionCommand): String {
    return action.riskText
}
