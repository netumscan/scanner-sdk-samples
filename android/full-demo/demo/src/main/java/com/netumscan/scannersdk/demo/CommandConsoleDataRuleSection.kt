package com.netumscan.scannersdk.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netumscan.scannersdk.demo.ui.theme.DemoColors
import com.netumscan.scannersdk.model.MasterCommand

@Composable
internal fun DataRuleConsoleSection(
    vm: CommandConsoleViewModel,
    state: DataRuleConsoleSectionState,
    actions: DataRuleConsoleSectionActions,
) {
    ExpandableSectionHeader(
        title = demoStringResource(R.string.parsing_advanced),
        expanded = state.parseStateExpanded,
        onToggle = { actions.onParseStateExpandedChange(!state.parseStateExpanded) },
        testTag = DemoTestTags.CONSOLE_SECTION_PARSE_ADVANCED,
    )
    if (state.parseStateExpanded) {
        Spacer(modifier = Modifier.height(10.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ParseStateCard(
                title = demoStringResource(R.string.device_state),
            ) {
                Button(
                    onClick = { vm.executeMasterCommand(MasterCommand.READ_CURRENT_CHARSET) },
                    enabled = state.canExecuteMasterCommands,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(demoStringResource(R.string.read_charset_only))
                }
            }
            ParseStateCard(
                title = demoStringResource(R.string.local_sdk_parse),
            ) {
                Text(
                    text = "${demoStringResource(R.string.local_charset)}: ${state.scanCharsetSummary}",
                    fontSize = 13.sp,
                    color = DemoColors.TextSecondary,
                )
                Spacer(modifier = Modifier.height(10.dp))
                CharsetSelectorRow(
                    selected = state.scanCharsetSummary,
                    enabled = state.canExecuteMasterCommands,
                    onSelect = { vm.setScanTextCharset(it) },
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${demoStringResource(R.string.local_terminator)}: ${state.scanTerminatorSummary}",
                    fontSize = 13.sp,
                    color = DemoColors.TextSecondary,
                )
                Spacer(modifier = Modifier.height(10.dp))
                TerminatorSelectorRow(
                    selected = state.scanTerminatorSummary,
                    enabled = state.canExecuteMasterCommands,
                    onSelect = { vm.setScanTerminator(it.bytes) },
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    ExpandableSectionHeader(
        title = demoStringResource(R.string.data_rule_builder),
        expanded = state.dataRuleExpanded,
        onToggle = { actions.onDataRuleExpandedChange(!state.dataRuleExpanded) },
        testTag = DemoTestTags.CONSOLE_SECTION_DATA_RULE_BUILDER,
    )
    if (!state.dataRuleExpanded) {
        return
    }

    Spacer(modifier = Modifier.height(12.dp))
    val builderModeLabel = dataRuleFormModeLabel(state.builderMode)
    DataRuleBuilderCard(
        mode = state.builderMode,
        valueA = state.builderValueA,
        valueB = state.builderValueB,
        enabled = state.canExecuteMasterCommands,
        onModeChange = actions.onBuilderModeChange,
        onValueAChange = actions.onBuilderValueAChange,
        onValueBChange = actions.onBuilderValueBChange,
        onExecute = {
            val command = buildDataRuleCommand(
                mode = state.builderMode,
                valueA = state.builderValueA,
                valueB = state.builderValueB,
            )
            vm.executeDataRuleCommand(builderModeLabel, command)
        },
    )
}
