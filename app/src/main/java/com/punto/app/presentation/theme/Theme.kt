package com.punto.app.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.punto.app.domain.model.AccentColor

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = TealPrimaryContainer,
    onPrimaryContainer = TealOnPrimaryContainer,
    secondary = SlateSecondary,
    onSecondary = SlateOnSecondary,
    secondaryContainer = SlateSecondaryContainer,
    onSecondaryContainer = SlateOnSecondaryContainer,
    tertiary = AmberTertiary,
    onTertiary = AmberOnTertiary,
    tertiaryContainer = AmberTertiaryContainer,
    onTertiaryContainer = AmberOnTertiaryContainer,
    error = ErrorRed,
    onError = ErrorOnRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    // Sin esto Material 3 usa grises con tinte morado (lo que se veía en las tarjetas).
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    surfaceDim = LightSurfaceDim,
    surfaceBright = LightSurfaceBright,
    outlineVariant = LightOutlineVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    surfaceDim = DarkSurfaceDim,
    surfaceBright = DarkSurfaceBright,
    outlineVariant = DarkOutlineVariant,
)

private class AccentPalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
)

private fun lightPalette(accent: AccentColor): AccentPalette? = when (accent) {
    AccentColor.BLUE -> AccentPalette(Color(0xFF0B5FA5), Color.White, Color(0xFFD3E4FF), Color(0xFF001C38), Color(0xFF535F70), Color(0xFFD7E3F8), Color(0xFF101C2B))
    AccentColor.VIOLET -> AccentPalette(Color(0xFF6750A4), Color.White, Color(0xFFEADDFF), Color(0xFF21005D), Color(0xFF625B71), Color(0xFFE8DEF8), Color(0xFF1E192B))
    AccentColor.ROSE -> AccentPalette(Color(0xFFA8305E), Color.White, Color(0xFFFFD9E2), Color(0xFF3E001D), Color(0xFF75565F), Color(0xFFF4DDE3), Color(0xFF2B151B))
    AccentColor.AMBER -> AccentPalette(Color(0xFF8A5100), Color.White, Color(0xFFFFDCBE), Color(0xFF2C1600), Color(0xFF735A42), Color(0xFFF1DFD0), Color(0xFF291806))
    else -> null
}

private fun darkPalette(accent: AccentColor): AccentPalette? = when (accent) {
    AccentColor.BLUE -> AccentPalette(Color(0xFFA2C9FF), Color(0xFF00315C), Color(0xFF00497F), Color(0xFFD3E4FF), Color(0xFFBBC7DB), Color(0xFF3B4858), Color(0xFFD7E3F8))
    AccentColor.VIOLET -> AccentPalette(Color(0xFFD0BCFF), Color(0xFF381E72), Color(0xFF4F378B), Color(0xFFEADDFF), Color(0xFFCCC2DC), Color(0xFF4A4458), Color(0xFFE8DEF8))
    AccentColor.ROSE -> AccentPalette(Color(0xFFFFB1C8), Color(0xFF5E1133), Color(0xFF7B2949), Color(0xFFFFD9E2), Color(0xFFE3BDC6), Color(0xFF5B3F47), Color(0xFFF4DDE3))
    AccentColor.AMBER -> AccentPalette(Color(0xFFFFB871), Color(0xFF4A2800), Color(0xFF693C00), Color(0xFFFFDCBE), Color(0xFFE2C1A4), Color(0xFF5A4330), Color(0xFFF1DFD0))
    else -> null
}

private fun ColorScheme.withAccent(p: AccentPalette?): ColorScheme =
    if (p == null) this else copy(
        primary = p.primary,
        onPrimary = p.onPrimary,
        primaryContainer = p.primaryContainer,
        onPrimaryContainer = p.onPrimaryContainer,
        secondary = p.secondary,
        secondaryContainer = p.secondaryContainer,
        onSecondaryContainer = p.onSecondaryContainer,
        surfaceTint = p.primary,
    )

@Composable
fun PuntoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: AccentColor = AccentColor.TEAL,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        accent == AccentColor.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme.withAccent(darkPalette(accent))
        else -> LightColorScheme.withAccent(lightPalette(accent))
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PuntoTypography,
        content = content,
    )
}
