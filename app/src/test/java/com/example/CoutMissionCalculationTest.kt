package com.example

import com.example.util.SiteDistances
import org.junit.Assert.assertEquals
import org.junit.Test

class CoutMissionCalculationTest {

    @Test
    fun testCondition1_hebergementFraisAndEchelleLessThan5() {
        val prixUnitaire = 50.0
        val delaisJours = 5
        val echelle = 400 // Premier chiffre = 4 < 5
        val hasHebergementFrais = true
        val mission = "Intervention sur avion"

        // Formule : Prix_Unitaire * (1 + (max(1, DelaisJours) - 1) * 0.6)
        // 50.0 * (1 + (5 - 1) * 0.6) = 50.0 * (1 + 2.4) = 50.0 * 3.4 = 170.0
        val result = SiteDistances.calculateCoutMission(
            delaisJours = delaisJours,
            prixUnitaireMission = prixUnitaire,
            echelle = echelle,
            mission = mission,
            hasHebergementFrais = hasHebergementFrais
        )
        assertEquals(170.0, result, 0.001)

        val detailed = SiteDistances.computeDetailedCoutMission(
            delaisJours = delaisJours,
            prixUnitaireMission = prixUnitaire,
            echelle = echelle,
            mission = mission,
            hasHebergementFrais = hasHebergementFrais
        )
        assertEquals("Hébergement (Échelle < 5)", detailed.regleAppliquee)
    }

    @Test
    fun testCondition1_1DayEdgeCase() {
        val prixUnitaire = 60.0
        val delaisJours = 1
        val echelle = 300 // Premier chiffre = 3 < 5
        val hasHebergementFrais = true
        val mission = "Visite médicale"

        // Formule : 60.0 * (1 + (1 - 1) * 0.6) = 60.0 * 1.0 = 60.0
        val result = SiteDistances.calculateCoutMission(
            delaisJours = delaisJours,
            prixUnitaireMission = prixUnitaire,
            echelle = echelle,
            mission = mission,
            hasHebergementFrais = hasHebergementFrais
        )
        assertEquals(60.0, result, 0.001)
    }

    @Test
    fun testCondition2_assistanceTechnique_tranchesDe8Jours() {
        val prixUnitaire = 80.0
        val echelle = 500
        val hasHebergementFrais = false

        // 1 jour -> 1 tranche -> 80.0
        val res1 = SiteDistances.calculateCoutMission(
            delaisJours = 1,
            prixUnitaireMission = prixUnitaire,
            echelle = echelle,
            mission = "💡ASSISTANCE TECHNIQUE",
            hasHebergementFrais = hasHebergementFrais
        )
        assertEquals(80.0, res1, 0.001)

        // 8 jours -> 1 tranche -> 80.0
        val res8 = SiteDistances.calculateCoutMission(
            delaisJours = 8,
            prixUnitaireMission = prixUnitaire,
            echelle = echelle,
            mission = "Assistance technique",
            hasHebergementFrais = hasHebergementFrais
        )
        assertEquals(80.0, res8, 0.001)

        // 9 jours -> 2 tranches -> 160.0
        val res9 = SiteDistances.calculateCoutMission(
            delaisJours = 9,
            prixUnitaireMission = prixUnitaire,
            echelle = echelle,
            mission = "💡 ASSISTANCE TECHNIQUE",
            hasHebergementFrais = hasHebergementFrais
        )
        assertEquals(160.0, res9, 0.001)

        // 20 jours -> ceil(20/8) = 3 tranches -> 240.0
        val res20 = SiteDistances.calculateCoutMission(
            delaisJours = 20,
            prixUnitaireMission = prixUnitaire,
            echelle = echelle,
            mission = "ASSISTANCE TECHNIQUE",
            hasHebergementFrais = hasHebergementFrais
        )
        assertEquals(240.0, res20, 0.001)
    }

    @Test
    fun testCondition3_renfortHangar_moreThan30DaysAndEchelle5to6() {
        val prixUnitaire = 60.0
        val delaisJours = 45
        val echelle = 500 // Premier chiffre = 5 (in 5..6)
        val mission = "🛠️RENFORT HANGAR"
        val hasHebergementFrais = false

        // Formule : (60.0 * 30) + ((45 - 30) * 60.0 * 2 / 3) = 1800 + (15 * 40) = 2400.0
        val result = SiteDistances.calculateCoutMission(
            delaisJours = delaisJours,
            prixUnitaireMission = prixUnitaire,
            echelle = echelle,
            mission = mission,
            hasHebergementFrais = hasHebergementFrais
        )
        assertEquals(2400.0, result, 0.001)

        val detailed = SiteDistances.computeDetailedCoutMission(
            delaisJours = delaisJours,
            prixUnitaireMission = prixUnitaire,
            echelle = 600, // Premier chiffre = 6 (in 5..6)
            mission = "Renfort hangar",
            hasHebergementFrais = hasHebergementFrais
        )
        assertEquals(2400.0, detailed.coutTotal, 0.001)
        assertEquals("Renfort Hangar (>30j, Échelle 5-6)", detailed.regleAppliquee)
    }

    @Test
    fun testCondition3_notTriggeredIfDays30OrLess() {
        val prixUnitaire = 60.0
        val delaisJours = 30 // pas > 30
        val echelle = 500
        val mission = "🛠️RENFORT HANGAR"
        val hasHebergementFrais = false

        // Standard : 60.0 * 30 = 1800.0
        val result = SiteDistances.calculateCoutMission(
            delaisJours = delaisJours,
            prixUnitaireMission = prixUnitaire,
            echelle = echelle,
            mission = mission,
            hasHebergementFrais = hasHebergementFrais
        )
        assertEquals(1800.0, result, 0.001)
    }

    @Test
    fun testCondition3_notTriggeredIfEchelleOutside5to6() {
        val prixUnitaire = 60.0
        val delaisJours = 35 // > 30
        val echelle = 700 // Premier chiffre = 7 (not in 5..6)
        val mission = "🛠️RENFORT HANGAR"
        val hasHebergementFrais = false

        // Standard : 60.0 * 35 = 2100.0
        val result = SiteDistances.calculateCoutMission(
            delaisJours = delaisJours,
            prixUnitaireMission = prixUnitaire,
            echelle = echelle,
            mission = mission,
            hasHebergementFrais = hasHebergementFrais
        )
        assertEquals(2100.0, result, 0.001)
    }

    @Test
    fun testDefaultCase_standardMultiplication() {
        val prixUnitaire = 75.0
        val delaisJours = 4
        val echelle = 500
        val mission = "Formation"
        val hasHebergementFrais = false

        // Formule standard : 75.0 * 4 = 300.0
        val result = SiteDistances.calculateCoutMission(
            delaisJours = delaisJours,
            prixUnitaireMission = prixUnitaire,
            echelle = echelle,
            mission = mission,
            hasHebergementFrais = hasHebergementFrais
        )
        assertEquals(300.0, result, 0.001)
    }

    @Test
    fun testLeftEchelleExtraction() {
        assertEquals(4, SiteDistances.getEchelleFirstDigit(400))
        assertEquals(5, SiteDistances.getEchelleFirstDigit(520))
        assertEquals(6, SiteDistances.getEchelleFirstDigit(600))
        assertEquals(7, SiteDistances.getEchelleFirstDigit(750))
        assertEquals(3, SiteDistances.getEchelleFirstDigit(300))
        assertEquals(1, SiteDistances.getEchelleFirstDigit(1))
        assertEquals(4, SiteDistances.getEchelleFirstDigit(4))
    }
}
