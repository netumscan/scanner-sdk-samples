package com.netumscan.scannersdk.demo

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ModuleCommandsSection(
    vm: CommandConsoleViewModel,
    state: ModuleConsoleSectionState,
    actions: ModuleConsoleSectionActions,
) {
    ExpandableSectionHeader(
        title = demoStringResource(R.string.module_commands),
        subtitle = demoStringResource(
            R.string.module_items_count,
            displayModuleFamilyLabel(state.activeModuleFamily),
            if (state.isNtc06hFamily) state.ntc06hSettings.size else state.modulePresets.size,
        ),
        expanded = state.moduleSettingsExpanded,
        onToggle = { actions.onModuleSettingsExpandedChange(!state.moduleSettingsExpanded) },
        testTag = DemoTestTags.CONSOLE_SECTION_MODULE_COMMANDS,
    )
    if (!state.moduleSettingsExpanded) {
        return
    }

    Spacer(modifier = Modifier.height(12.dp))
    when {
        state.canExecuteModuleCommands -> {
            ConsoleFeedbackBanner(
                text = demoStringResource(R.string.module_passthrough_available),
                tone = ConsoleBannerTone.Success,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        state.supportsModuleCommands -> {
            ConsoleFeedbackBanner(
                text = demoStringResource(R.string.module_capability_not_ready),
                tone = ConsoleBannerTone.Error,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    val testRecommendations = remember(state.activeModuleFamily) {
        moduleTestRecommendations(state.activeModuleFamily)
    }
    if (testRecommendations.isNotEmpty()) {
        ModuleTestGuideCard(
            family = state.activeModuleFamily,
            recommendations = testRecommendations,
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

    if (state.isNtc06hFamily) {
        Ntc06hModuleCatalog(
            vm = vm,
            state = state,
            actions = actions,
        )
    } else {
        GenericModuleCatalog(
            vm = vm,
            state = state,
            actions = actions,
        )
    }
}
