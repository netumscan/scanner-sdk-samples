package com.netumscan.scannersdk.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netumscan.scannersdk.demo.ui.theme.DemoColors
import com.netumscan.scannersdk.demo.ui.theme.DemoShapes

@Composable
internal fun DemoScenarioPresetCard(
    presets: List<DemoScenarioPreset> = DemoScenarioPresets.all(),
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DemoShapes.panel,
        colors = CardDefaults.cardColors(containerColor = DemoColors.SurfaceAccent),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = demoStringResource(R.string.demo_scenario_presets),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DemoColors.TextPrimary,
            )
            presets.forEach { preset ->
                Text(
                    text = "${preset.titleProvider()}: ${preset.summaryProvider()}",
                    fontSize = 12.sp,
                    color = DemoColors.TextSecondary,
                )
            }
        }
    }
}
