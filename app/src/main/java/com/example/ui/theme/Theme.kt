package com.example.ui.theme

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

fun getAppColorScheme(palette: AppPalette, isDark: Boolean): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = palette.darkPrimary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.primaryContainerDark,
            onPrimaryContainer = palette.onPrimaryContainerDark,
            secondary = palette.secondary,
            onSecondary = Color.White,
            background = palette.darkBackground,
            onBackground = Color(0xFFF8FAFC),
            surface = palette.darkSurface,
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = palette.primaryContainerDark.copy(alpha = 0.7f),
            onSurfaceVariant = Color(0xFFCBD5E1),
            outline = Color(0xFF475569)
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.primaryContainerLight,
            onPrimaryContainer = palette.onPrimaryContainerLight,
            secondary = palette.secondary,
            onSecondary = Color.White,
            background = when (palette) {
                AppPalette.AVIATION -> Color(0xFFF8FAFC)
                AppPalette.ARDOISE_REPOSANTE -> Color(0xFFF5F7F8)
                AppPalette.SABLE_CHAUD -> Color(0xFFFAF7F2)
                AppPalette.BRUME_LAVANDE -> Color(0xFFF8F8FC)
                AppPalette.FORET_OLIVE -> Color(0xFFF5F8F6)
            },
            onBackground = when (palette) {
                AppPalette.SABLE_CHAUD -> Color(0xFF2D241E)
                AppPalette.ARDOISE_REPOSANTE -> Color(0xFF1E293B)
                AppPalette.BRUME_LAVANDE -> Color(0xFF1F2033)
                AppPalette.FORET_OLIVE -> Color(0xFF1B2B1E)
                else -> SlateDark
            },
            surface = when (palette) {
                AppPalette.SABLE_CHAUD -> Color(0xFFFFFDFB)
                AppPalette.ARDOISE_REPOSANTE -> Color(0xFFFFFFFF)
                AppPalette.BRUME_LAVANDE -> Color(0xFFFFFFFF)
                AppPalette.FORET_OLIVE -> Color(0xFFFFFFFF)
                else -> SurfaceWhite
            },
            onSurface = when (palette) {
                AppPalette.SABLE_CHAUD -> Color(0xFF2D241E)
                AppPalette.ARDOISE_REPOSANTE -> Color(0xFF1E293B)
                AppPalette.BRUME_LAVANDE -> Color(0xFF1F2033)
                AppPalette.FORET_OLIVE -> Color(0xFF1B2B1E)
                else -> SlateDark
            },
            surfaceVariant = when (palette) {
                AppPalette.SABLE_CHAUD -> Color(0xFFF2ECE4)
                AppPalette.ARDOISE_REPOSANTE -> Color(0xFFEAEFF1)
                AppPalette.BRUME_LAVANDE -> Color(0xFFEDEDF6)
                AppPalette.FORET_OLIVE -> Color(0xFFEBF0EB)
                else -> Color(0xFFF1F5F9)
            },
            onSurfaceVariant = when (palette) {
                AppPalette.SABLE_CHAUD -> Color(0xFF5D4A3D)
                AppPalette.ARDOISE_REPOSANTE -> Color(0xFF475569)
                AppPalette.BRUME_LAVANDE -> Color(0xFF4D4F66)
                AppPalette.FORET_OLIVE -> Color(0xFF46594B)
                else -> SlateMedium
            },
            outline = when (palette) {
                AppPalette.SABLE_CHAUD -> Color(0xFFE2D8CC)
                AppPalette.ARDOISE_REPOSANTE -> Color(0xFFDCE3E6)
                AppPalette.BRUME_LAVANDE -> Color(0xFFDDDDEB)
                AppPalette.FORET_OLIVE -> Color(0xFFDAE2DA)
                else -> SurfaceBorder
            }
        )
    }
}

@Composable
fun MyApplicationTheme(
    palette: AppPalette = AppPalette.AVIATION,
    darkModeOption: DarkModeOption = DarkModeOption.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (darkModeOption) {
        DarkModeOption.SYSTEM -> systemInDark
        DarkModeOption.LIGHT -> false
        DarkModeOption.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> getAppColorScheme(palette, isDark)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
