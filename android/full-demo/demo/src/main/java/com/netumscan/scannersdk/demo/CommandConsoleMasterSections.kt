package com.netumscan.scannersdk.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun MasterQuickActionsSection(
    vm: CommandConsoleViewModel,
    state: MasterConsoleSectionState,
    actions: MasterConsoleSectionActions,
) {
    ExpandableSectionHeader(
        title = demoStringResource(R.string.action_quick_actions),
        subtitle = demoStringResource(R.string.action_count, state.quickActionCount),
        expanded = state.quickActionsExpanded,
        onToggle = { actions.onQuickActionsExpandedChange(!state.quickActionsExpanded) },
        testTag = DemoTestTags.CONSOLE_SECTION_QUICK_ACTIONS,
    )
    if (!state.quickActionsExpanded) {
        return
    }

    Spacer(modifier = Modifier.height(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        state.quickActionRows.forEach { row ->
            GroupedCommandCard(
                title = row.title,
                summary = demoStringResource(R.string.action_count, row.commands.size),
            ) {
                CommandButtonRow(
                    actions = row.commands.map { (label, action) ->
                        commandButtonAction(label, state.canExecuteMasterCommands, isDangerous = action.isDangerous) {
                            if (action.isDangerous) {
                                actions.onPendingQuickActionChange(action)
                            } else {
                                vm.performQuickAction(action)
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
internal fun MasterCapabilityActionsSection(
    vm: CommandConsoleViewModel,
    state: MasterConsoleSectionState,
    actions: MasterConsoleSectionActions,
    actionInputValue: (String) -> String,
    onActionInputChange: (String, String) -> Unit,
) {
    ExpandableSectionHeader(
        title = demoStringResource(R.string.device_actions),
        subtitle = demoStringResource(R.string.action_count, state.actionCapabilityEntries.size),
        expanded = state.capabilityActionsExpanded,
        onToggle = { actions.onCapabilityActionsExpandedChange(!state.capabilityActionsExpanded) },
    )
    if (!state.capabilityActionsExpanded) {
        return
    }

    Spacer(modifier = Modifier.height(12.dp))
    val groupedActions = state.actionCapabilityEntries
        .groupBy { it.groupKey }
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)
    if (groupedActions.isEmpty()) {
        ConsoleHint(demoStringResource(R.string.empty_command_group))
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        groupedActions.forEach { (group, definitions) ->
            val riskSummary = definitions
                .map { capabilityRiskLabel(it.riskLevel) }
                .distinct()
                .joinToString(" / ")
            GroupedCommandCard(
                title = definitions.firstOrNull()?.let(::deviceActionGroupLabel)
                    ?: group.ifBlank { demoStringResource(R.string.all) },
                summary = riskSummary.ifBlank {
                    demoStringResource(R.string.action_count, definitions.size)
                },
            ) {
                val simpleActions = definitions.filter { !it.requiresValue }
                if (simpleActions.isNotEmpty()) {
                    CommandButtonRow(
                        actions = simpleActions.map { definition ->
                            commandButtonAction(
                                label = deviceActionLabel(definition),
                                enabled = state.canExecuteMasterCommands,
                                isDangerous = definition.riskLevel != com.netumscan.scannersdk.model.CapabilityRiskLevel.NORMAL,
                            ) {
                                if (definition.riskLevel != com.netumscan.scannersdk.model.CapabilityRiskLevel.NORMAL) {
                                    actions.onPendingCapabilityActionChange(definition)
                                } else {
                                    vm.executeCapabilityAction(definition)
                                }
                            }
                        },
                    )
                }
                definitions.filter { it.requiresValue }.forEach { definition ->
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = actionInputValue(definition.entryKey),
                        onValueChange = { onActionInputChange(definition.entryKey, it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(deviceActionLabel(definition)) },
                        placeholder = { Text(deviceActionValueHint(definition)) },
                        supportingText = {
                            Text(capabilityRiskText(definition))
                        },
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (definition.riskLevel != com.netumscan.scannersdk.model.CapabilityRiskLevel.NORMAL) {
                                actions.onPendingCapabilityActionChange(definition)
                            } else {
                                vm.executeCapabilityAction(definition, actionInputValue(definition.entryKey))
                            }
                        },
                        enabled = state.canExecuteMasterCommands,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(demoStringResource(R.string.execute_device_action))
                    }
                }
            }
        }
    }
}
