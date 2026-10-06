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
    ARDOISE_REPOSANTE(
        id = "ardoise",
        label = "Ardoise Douce & Sauge",
        subtitle = "Mode Repos des Yeux • Minéral anti-fatigue visuelle",
        primary = Color(0xFF2B5B6C),
        onPrimary = Color.White,
        primaryContainerLight = Color(0xFFE0EBEF),
        onPrimaryContainerLight = Color(0xFF132B33),
        primaryContainerDark = Color(0xFF1D3741),
        onPrimaryContainerDark = Color(0xFFCCE4ED),
        secondary = Color(0xFFD49244),
        darkPrimary = Color(0xFF7CC4D8),
        darkBackground = Color(0xFF11181D),
        darkSurface = Color(0xFF1A232A)
    ),
    SABLE_CHAUD(
        id = "sable",
        label = "Lin Chaud & Sépia Reposant",
        subtitle = "Papier & Tons Chauds • Confort de lecture sans éblouissement",
        primary = Color(0xFF7C5032),
        onPrimary = Color.White,
        primaryContainerLight = Color(0xFFF4ECE4),
        onPrimaryContainerLight = Color(0xFF331F12),
        primaryContainerDark = Color(0xFF3B271A),
        onPrimaryContainerDark = Color(0xFFF3DFCE),
        secondary = Color(0xFF4D6E62),
        darkPrimary = Color(0xFFE2B288),
        darkBackground = Color(0xFF171412),
        darkSurface = Color(0xFF231E1A)
    ),
    BRUME_LAVANDE(
        id = "lavande",
        label = "Lavande Douce & Brume",
        subtitle = "Apaisant & Anti-stress • Nuances pastel reposantes",
        primary = Color(0xFF4F517D),
        onPrimary = Color.White,
        primaryContainerLight = Color(0xFFE8E8F5),
        onPrimaryContainerLight = Color(0xFF1C1D38),
        primaryContainerDark = Color(0xFF2B2C4E),
        onPrimaryContainerDark = Color(0xFFE8E8F5),
        secondary = Color(0xFF3E6D7B),
        darkPrimary = Color(0xFFB5B7EA),
        darkBackground = Color(0xFF13131F),
        darkSurface = Color(0xFF1E1E2E)
    ),
    FORET_OLIVE(
        id = "olive",
        label = "Olive Douce & Sauge",
        subtitle = "Repos Oculaire Nature • Chlorophylle & Confort végétal",
        primary = Color(0xFF3B5C44),
        onPrimary = Color.White,
        primaryContainerLight = Color(0xFFDFEADF),
        onPrimaryContainerLight = Color(0xFF102517),
        primaryContainerDark = Color(0xFF1E3524),
        onPrimaryContainerDark = Color(0xFFDFEADF),
        secondary = Color(0xFF426575),
        darkPrimary = Color(0xFF8DCFA0),
        darkBackground = Color(0xFF101712),
        darkSurface = Color(0xFF19231C)
    )
}

enum class DarkModeOption(val id: String, val label: String) {
    SYSTEM("system", "Automatique (Système)"),
    LIGHT("light", "Mode Clair"),
    DARK("dark", "Mode Sombre")
}
