package com.netumscan.scannersdk.demo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netumscan.scannersdk.demo.ui.theme.DemoColors
import com.netumscan.scannersdk.demo.ui.theme.DemoShapes

@Composable
internal fun DemoSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    body: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = DemoShapes.card,
        colors = CardDefaults.cardColors(containerColor = DemoColors.Surface),
        border = BorderStroke(1.dp, DemoColors.Outline.copy(alpha = 0.65f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = DemoColors.TextPrimary,
            )
            body()
        }
    }
}

@Composable
internal fun DemoSelectableChipButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    selectedColors: ButtonColors? = null,
    unselectedBorder: BorderStroke? = null,
    textAlign: TextAlign = TextAlign.Center,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.demoTestTag(testTag),
            colors = selectedColors ?: ButtonDefaults.buttonColors(),
        ) {
            Text(label, textAlign = textAlign)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.demoTestTag(testTag),
            border = unselectedBorder,
        ) {
            Text(label, textAlign = textAlign)
        }
    }
}

@Composable
internal fun DemoLabeledValueBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueFontSize: TextUnit = 13.sp,
    valueFontWeight: FontWeight = FontWeight.Normal,
    valueLineHeight: TextUnit = TextUnit.Unspecified,
) {
    Column(
        modifier = modifier
            .background(DemoColors.SurfaceMuted, DemoShapes.panel)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = label, fontSize = 11.sp, color = DemoColors.TextTertiary)
        Text(
            text = value,
            fontSize = valueFontSize,
            fontWeight = valueFontWeight,
            lineHeight = valueLineHeight,
            color = DemoColors.TextPrimary,
        )
    }
}

@Composable
internal fun DemoHintCard(
    text: String,
    modifier: Modifier = Modifier,
    shape: Shape = DemoShapes.chip,
    showBorder: Boolean = true,
    borderColor: Color = DemoColors.Outline.copy(alpha = 0.7f),
    backgroundColor: Color = DemoColors.SurfaceMuted,
    textColor: Color = DemoColors.TextTertiary,
    padding: Dp = 16.dp,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = if (showBorder) BorderStroke(1.dp, borderColor) else null,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(padding),
            fontSize = 13.sp,
            color = textColor,
        )
    }
}

@Composable
internal fun DemoFeedbackBanner(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, DemoShapes.chip)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = text, fontSize = 13.sp, color = textColor)
        if (actionLabel != null && onAction != null) {
            OutlinedButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
