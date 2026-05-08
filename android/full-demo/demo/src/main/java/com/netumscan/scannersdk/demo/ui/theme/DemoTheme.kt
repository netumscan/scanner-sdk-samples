package com.netumscan.scannersdk.demo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object DemoColors {
    val Page = Color(0xFFF3EFE7)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFEBE4D8)
    val SurfaceAccent = Color(0xFFDCE8DE)
    val SurfaceBrand = Color(0xFF1F3A2C)
    val SurfaceDanger = Color(0xFFFAE0DD)
    val TextPrimary = Color(0xFF1E2A24)
    val TextSecondary = Color(0xFF5E6B64)
    val TextTertiary = Color(0xFF7B6F61)
    val TextConsole = Color(0xFF24332B)
    val Accent = Color(0xFF8C5A32)
    val AccentStrong = Color(0xFF2F6A4F)
    val Outline = Color(0xFFD6CCBE)
    val Danger = Color(0xFF9D3D34)
}

object DemoShapes {
    val card = RoundedCornerShape(22.dp)
    val panel = RoundedCornerShape(18.dp)
    val chip = RoundedCornerShape(16.dp)
}

private val DemoLightScheme = lightColorScheme(
    primary = DemoColors.TextPrimary,
    secondary = DemoColors.TextSecondary,
    tertiary = DemoColors.Accent,
    background = DemoColors.Page,
    surface = DemoColors.Surface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DemoColors.TextPrimary,
    onSurface = DemoColors.TextPrimary,
)

private val DemoDarkScheme = darkColorScheme(
    primary = DemoColors.TextPrimary,
    secondary = DemoColors.TextSecondary,
    tertiary = DemoColors.Accent,
    background = DemoColors.Page,
    surface = DemoColors.Surface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DemoColors.TextPrimary,
    onSurface = DemoColors.TextPrimary,
)

@Composable
fun DemoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DemoLightScheme,
        shapes = MaterialTheme.shapes.copy(
            small = DemoShapes.chip,
            medium = DemoShapes.panel,
            large = DemoShapes.card,
        ),
        content = content
    )
}
