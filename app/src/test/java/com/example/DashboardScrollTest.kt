package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.example.data.local.*
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class DashboardScrollTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDashboardScrollDoesNotCrash() {
        val lots = listOf(
            LotEntity("TAT-0023-26", "Mission de renfort Hangar A320 Tunis", System.currentTimeMillis()),
            LotEntity("TAT-0024-26", "Assistance escale Djerba & Monastir", System.currentTimeMillis())
        )
        val odds = listOf(
            OddEntity("DJE-0005-26", "TAT-0023-26", "MAT-1042", "Tunis", 500, "Djerba", 0L, 0L, "Assistance technique", totalOdd = 250.0),
            OddEntity("MIR-0012-26", "TAT-0023-26", "MAT-2088", "Monastir", 400, "Monastir", 0L, 0L, "Renfort hangar", totalOdd = 0.0),
            OddEntity("TUN-0033-26", "TAT-0024-26", "MAT-3105", "Sfax", 600, "Tunis", 0L, 0L, "Intervention sur avion", totalOdd = 120.0),
            OddEntity("TOE-0001-26", "TAT-0024-26", "MAT-5022", "Tozeur", 300, "Tozeur", 0L, 0L, "Transport", totalOdd = 0.0)
        )
        val frais = listOf(
            FraisEntity("F1", "DJE-0005-26", 0L, "Hébergement", "LPD", "Nuité", 2.0, 80.0, 160.0, 160.0),
            FraisEntity("F2", "DJE-0005-26", 0L, "Transport", "Transport", "Km", 1.0, 50.0, 50.0, 50.0),
            FraisEntity("F3", "DJE-0005-26", 0L, "Divers", "Repas", "Repas", 2.0, 20.0, 40.0, 40.0),
            FraisEntity("F4", "TUN-0033-26", 0L, "Divers", "Repas", "Repas", 1.0, 25.0, 25.0, 25.0)
        )
        val techniciens = listOf(
            TechnicienEntity("MAT-1042", "M.", "Trabelsi", "Anis", "Tunis", "Technicien B1 A320/A330", 500, "TN59", null, 42, 18),
            TechnicienEntity("MAT-2088", "M.", "Chouikh", "Tarek", "Monastir", "Mécanicien Cellule & Moteurs", 400, "TN59", null, 35, 14),
            TechnicienEntity("MAT-3105", "Mme", "Ayadi", "Mariem", "Sfax", "Avionique B2 Systèmes de Bord", 600, "TN59", null, 50, 22),
            TechnicienEntity("MAT-4019", "M.", "Bouzid", "Walid", "Djerba", "Inspecteur CND / Qualité", 700, "TN59", null, 28, 11),
            TechnicienEntity("MAT-5022", "M.", "Guesmi", "Sami", "Tozeur", "Technicien Piste & Escales", 300, "TN59", null, 19, 9),
            TechnicienEntity("MAT-6091", "M.", "Ben Salem", "Yassine", "Tunis", "Ingénieur Support Opérations", 800, "TN59", null, 64, 27)
        )
        val validations = listOf(
            ValidationEntity("V1", "TAT-0023-26", "DM", 1, "VALIDE", reactiviteJours = 1.0)
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                DashboardScreen(
                    lots = lots,
                    odds = odds,
                    fraisList = frais,
                    validations = validations,
                    techniciens = techniciens
                )
            }
        }

        // Swipe up multiple times to scroll to the very bottom
        repeat(5) {
            composeTestRule.onRoot().performTouchInput { swipeUp() }
            composeTestRule.waitForIdle()
        }
    }
}
