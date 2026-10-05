package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class AppPalette(
    val id: String,
    val label: String,
    val subtitle: String,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainerLight: Color,
    val onPrimaryContainerLight: Color,
    val primaryContainerDark: Color,
    val onPrimaryContainerDark: Color,
    val secondary: Color,
    val darkPrimary: Color,
    val darkBackground: Color,
    val darkSurface: Color
) {
    AVIATION(
        id = "aviation",
        label = "Bleu Aéronautique",
        subtitle = "Tunisair Technics • Cobalt & Ciel",
        primary = Color(0xFF02457A),
        onPrimary = Color.White,
        primaryContainerLight = Color(0xFFD6E4FF),
        onPrimaryContainerLight = Color(0xFF001C3B),
        primaryContainerDark = Color(0xFF00325B),
        onPrimaryContainerDark = Color(0xFFD6E4FF),
        secondary = Color(0xFFD97706),
        darkPrimary = Color(0xFF38BDF8),
        darkBackground = Color(0xFF0B132B),
        darkSurface = Color(0xFF16223F)
    ),
    AMBER(
        id = "amber",
        label = "Or & Ambre Sahara",
        subtitle = "Chaleureux & Lumineux • Ocre & Bronze",
        primary = Color(0xFFB45309),
        onPrimary = Color.White,
        primaryContainerLight = Color(0xFFFFDDB8),
        onPrimaryContainerLight = Color(0xFF2B1700),
        primaryContainerDark = Color(0xFF6B3100),
        onPrimaryContainerDark = Color(0xFFFFDDB8),
        secondary = Color(0xFF02457A),
        darkPrimary = Color(0xFFFFB951),
        darkBackground = Color(0xFF19130D),
        darkSurface = Color(0xFF291E14)
    ),
    EMERALD(
        id = "emerald",
        label = "Vert Émeraude Technique",
        subtitle = "Aviation High-Tech • Jade & Céladon",
        primary = Color(0xFF0E6B47),
        onPrimary = Color.White,
        primaryContainerLight = Color(0xFFB4F2D0),
        onPrimaryContainerLight = Color(0xFF002113),
        primaryContainerDark = Color(0xFF005234),
        onPrimaryContainerDark = Color(0xFFB4F2D0),
        secondary = Color(0xFF0284C7),
        darkPrimary = Color(0xFF4ADE80),
        darkBackground = Color(0xFF0A1812),
        darkSurface = Color(0xFF132A1F)
    )
}

enum class DarkModeOption(val id: String, val label: String) {
    SYSTEM("system", "Automatique (Système)"),
    LIGHT("light", "Mode Clair"),
    DARK("dark", "Mode Sombre")
}
