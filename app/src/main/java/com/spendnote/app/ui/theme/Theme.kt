package com.spendnote.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val IncomeGreen = Color(0xFF16A34A)
val ExpenseRed = Color(0xFFDC2626)
val BrandStart = Color(0xFF0F766E)
val BrandEnd = Color(0xFF155E75)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F766E), onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1), onPrimaryContainer = Color(0xFF042F2E),
    secondary = Color(0xFF475569), onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1), onSecondaryContainer = Color(0xFF042F2E),
    tertiary = Color(0xFF0EA5E9),
    background = Color(0xFFF2F4F5), onBackground = Color(0xFF0F172A),
    surface = Color.White, onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE6EBEE), onSurfaceVariant = Color(0xFF64748B),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF8FAFB),
    surfaceContainerHighest = Color(0xFFEFF2F4),
    surfaceContainerLow = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5EEAD4), onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF134E4A), onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFF94A3B8), onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF134E4A), onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFF38BDF8),
    background = Color(0xFF0C1014), onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF161B21), onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF242B33), onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainer = Color(0xFF161B21),
    surfaceContainerHigh = Color(0xFF1C222A),
    surfaceContainerHighest = Color(0xFF242B33),
    surfaceContainerLow = Color(0xFF12161B),
)

@Composable
fun SpendNoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
