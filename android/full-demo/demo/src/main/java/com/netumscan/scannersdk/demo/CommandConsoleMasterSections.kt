package com.netumscan.scannersdk.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.netumscan.scannersdk.demo.ui.theme.DemoColors

@Composable
internal fun MasterQuickActionsSection(
    vm: CommandConsoleViewModel,
    state: MasterConsoleSectionState,
    actions: MasterConsoleSectionActions,
) {
    ExpandableSectionHeader(
        title = demoStringResource(R.string.master_quick_actions),
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
internal fun MasterCommandsSection(
    vm: CommandConsoleViewModel,
    state: MasterConsoleSectionState,
    actions: MasterConsoleSectionActions,
) {
    ExpandableSectionHeader(
        title = demoStringResource(R.string.master_commands),
        subtitle = state.selectedMasterTabItem?.let {
            demoStringResource(R.string.selected_master_command_count, it.title, state.selectedMasterCommandCount)
        },
        expanded = state.commandsExpanded,
        onToggle = { actions.onCommandsExpandedChange(!state.commandsExpanded) },
    )
    if (!state.commandsExpanded) {
        return
    }

    Spacer(modifier = Modifier.height(12.dp))
    ScrollableTabRow(
        selectedTabIndex = state.selectedMasterTab,
        containerColor = Color.Transparent,
        contentColor = DemoColors.TextPrimary,
        edgePadding = 0.dp,
    ) {
        state.masterTabItems.forEachIndexed { index, item ->
            Tab(
                selected = state.selectedMasterTab == index,
                onClick = { actions.onSelectedMasterTabChange(index) },
                text = { Text(item.title) },
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    if (state.selectedTabSections.isEmpty()) {
        ConsoleHint(
            demoStringResource(R.string.empty_command_group),
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        state.selectedTabSections.forEach { section ->
            GroupedCommandCard(
                title = section.title,
                summary = demoStringResource(R.string.command_count, section.commands.size),
            ) {
                CommandGrid(
                    commands = section.commands,
                    enabled = state.canExecuteMasterCommands,
                    isDangerous = { it in CommandCatalog.dangerousConsoleCommands },
                    onClick = { label, command ->
                        if (command in CommandCatalog.dangerousConsoleCommands) {
                            actions.onPendingCommandChange(command)
                        } else {
                            when (command) {
                                is ConsoleCommand.Basic -> vm.executeBasicDeviceCommand(command.command)
                                is ConsoleCommand.Master -> vm.executeMasterCommand(command.command)
                            }
                        }
                    },
                )
            }
        }
    }
}
