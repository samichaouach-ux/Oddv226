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
                AppPalette.AMBER -> Color(0xFFFFFDF8)
                AppPalette.EMERALD -> Color(0xFFF4FBF7)
            },
            onBackground = SlateDark,
            surface = SurfaceWhite,
            onSurface = SlateDark,
            surfaceVariant = Color(0xFFF1F5F9),
            onSurfaceVariant = SlateMedium,
            outline = SurfaceBorder
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
