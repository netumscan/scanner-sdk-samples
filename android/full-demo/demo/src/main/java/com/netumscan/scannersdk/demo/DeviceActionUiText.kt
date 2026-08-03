package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.CapabilityEntry

internal fun deviceActionLabel(definition: CapabilityEntry): String =
    definition.semanticKey.ifBlank { definition.entryKey }

internal fun deviceActionGroupLabel(definition: CapabilityEntry): String =
    definition.groupKey.ifBlank { DemoStrings.text(R.string.all) }

internal fun deviceActionValueHint(definition: CapabilityEntry): String =
    definition.valueHint.ifBlank { DemoStrings.text(R.string.device_action_value_hint) }
