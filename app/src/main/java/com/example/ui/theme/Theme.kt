package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = TurquoiseDeep,
    onPrimary = Color.White,
    primaryContainer = TurquoiseLightContainer,
    onPrimaryContainer = TurquoiseHeaderDark,
    secondary = GoldSoft,
    onSecondary = Color.White,
    secondaryContainer = GoldContainerLight,
    onSecondaryContainer = GoldOnContainer,
    tertiary = TurquoiseRich,
    onTertiary = Color.White,
    background = CreamLight,
    onBackground = InkDark,
    surface = CreamCard,
    onSurface = InkDark,
    surfaceVariant = SandstoneMuted,
    onSurfaceVariant = InkMuted,
    outline = SandstoneBorder,
    error = TerracottaError,
    onError = Color.White,
    errorContainer = TerracottaContainer,
    onErrorContainer = TerracottaError
)

private val DarkColorScheme = darkColorScheme(
    primary = NightTurquoisePrimary,
    onPrimary = TurquoiseHeaderDark,
    primaryContainer = NightTurquoiseContainer,
    onPrimaryContainer = TurquoiseLightContainer,
    secondary = NightGoldSecondary,
    onSecondary = GoldOnContainer,
    secondaryContainer = NightGoldContainer,
    onSecondaryContainer = GoldContainerLight,
    tertiary = TurquoiseMuted,
    onTertiary = Color.White,
    background = NightTurquoiseBg,
    onBackground = NightCreamText,
    surface = NightTurquoiseSurface,
    onSurface = NightCreamText,
    surfaceVariant = NightTurquoiseElevated,
    onSurfaceVariant = NightMutedText,
    outline = Color(0xFF2B4950),
    error = Color(0xFFF28B82),
    onError = Color(0xFF410E0B)
)

private val SoltaniyehShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SoltaniyehTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SoltaniyehTypography,
            shapes = SoltaniyehShapes
        ) {
            ProvideTextStyle(value = SoltaniyehTypography.bodyLarge) {
                content()
            }
        }
    }
}
