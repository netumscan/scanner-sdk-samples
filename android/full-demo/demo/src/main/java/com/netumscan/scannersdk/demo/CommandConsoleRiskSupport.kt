package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.CapabilityEntry
import com.netumscan.scannersdk.model.CapabilityRiskLevel

internal fun commandRiskText(command: ConsoleCommand): String {
    return command.toString()
}

internal fun quickActionRiskText(action: QuickActionCommand): String {
    return action.riskText
}

internal fun capabilityRiskText(action: CapabilityEntry): String {
    return when (action.riskLevel) {
        CapabilityRiskLevel.NORMAL -> DemoStrings.text(R.string.device_action_risk_normal)
        CapabilityRiskLevel.DESTRUCTIVE -> DemoStrings.text(R.string.device_action_risk_destructive)
        CapabilityRiskLevel.CONNECTIVITY -> DemoStrings.text(R.string.device_action_risk_connectivity)
        CapabilityRiskLevel.DATA_LOSS -> DemoStrings.text(R.string.device_action_risk_data_loss)
    }
}

internal fun capabilityRiskLabel(level: CapabilityRiskLevel): String {
    return when (level) {
        CapabilityRiskLevel.NORMAL -> DemoStrings.text(R.string.risk_level_normal)
        CapabilityRiskLevel.DESTRUCTIVE -> DemoStrings.text(R.string.risk_level_destructive)
        CapabilityRiskLevel.CONNECTIVITY -> DemoStrings.text(R.string.risk_level_connectivity)
        CapabilityRiskLevel.DATA_LOSS -> DemoStrings.text(R.string.risk_level_data_loss)
    }
}
