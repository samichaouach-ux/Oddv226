package com.example.util

/**
 * Référentiel Officiel des Distances Inter-Sites de Tunisair Technics
 * Distances routières certifiées (en kilomètres) entre les aéroports et centres de maintenance en Tunisie.
 */
object SiteDistances {

    val SITES = listOf("Tunis", "Monastir", "Sfax", "Djerba", "Tozeur", "Tabarka", "Enfidha", "Gafsa")

    // Matrice symétrique des distances routières officielles (en kilomètres)
    private val distanceMap: Map<Pair<String, String>, Int> = mapOf(
        // Depuis Tunis (Base Principale TUN - Tunis Carthage)
        Pair("Tunis", "Monastir") to 165,
        Pair("Tunis", "Sfax") to 270,
        Pair("Tunis", "Djerba") to 510,
        Pair("Tunis", "Tozeur") to 450,
        Pair("Tunis", "Tabarka") to 175,
        Pair("Tunis", "Enfidha") to 100,
        Pair("Tunis", "Gafsa") to 360,

        // Depuis Monastir (MIR - Habib Bourguiba)
        Pair("Monastir", "Sfax") to 135,
        Pair("Monastir", "Djerba") to 380,
        Pair("Monastir", "Tozeur") to 360,
        Pair("Monastir", "Tabarka") to 310,
        Pair("Monastir", "Enfidha") to 65,
        Pair("Monastir", "Gafsa") to 260,

        // Depuis Sfax (SFA - Thyna)
        Pair("Sfax", "Djerba") to 255,
        Pair("Sfax", "Tozeur") to 290,
        Pair("Sfax", "Tabarka") to 420,
        Pair("Sfax", "Enfidha") to 170,
        Pair("Sfax", "Gafsa") to 205,

        // Depuis Djerba (DJE - Zarzis)
        Pair("Djerba", "Tozeur") to 310,
        Pair("Djerba", "Tabarka") to 660,
        Pair("Djerba", "Enfidha") to 415,
        Pair("Djerba", "Gafsa") to 295,

        // Depuis Tozeur (TOE - Nefta)
        Pair("Tozeur", "Tabarka") to 510,
        Pair("Tozeur", "Enfidha") to 370,
        Pair("Tozeur", "Gafsa") to 95,

        // Depuis Tabarka (TBJ - Aïn Draham)
        Pair("Tabarka", "Enfidha") to 255,
        Pair("Tabarka", "Gafsa") to 380,

        // Depuis Enfidha (NBE - Hammamet)
        Pair("Enfidha", "Gafsa") to 270
    )

    /**
     * Récupère la distance en km entre deux sites.
     * Si les deux sites sont identiques, retourne 0 km.
     */
    fun getDistanceKm(siteA: String, siteB: String): Int {
        val cleanA = cleanSiteName(siteA)
        val cleanB = cleanSiteName(siteB)

        if (cleanA.equals(cleanB, ignoreCase = true)) return 0

        distanceMap[Pair(cleanA, cleanB)]?.let { return it }
        distanceMap[Pair(cleanB, cleanA)]?.let { return it }

        // Valeur par défaut si non répertorié
        return 200
    }

    /**
     * Retourne toutes les distances depuis un site de provenance vers tous les autres sites
     */
    fun getDistancesFromProvenance(siteProvenance: String): List<Pair<String, Int>> {
        val cleanProv = cleanSiteName(siteProvenance)
        return SITES
            .filter { !it.equals(cleanProv, ignoreCase = true) }
            .map { destination -> destination to getDistanceKm(cleanProv, destination) }
            .sortedBy { it.second }
    }

    /**
     * Règle de gestion Tunisair Technics :
     * Franchise forfaitaire de 50 km déduite sur la distance totale Aller et Retour.
     */
    const val FRANCHISE_DEDUCTION_KM = 50

    /**
     * Calcule la distance nette retenue pour le remboursement.
     * Pour un déplacement Aller-Retour, déduction stricte de 50 km (minimum 0 km).
     */
    fun getNetDistanceKm(distanceKm: Int, allerRetour: Boolean = true): Int {
        if (distanceKm == 0) return 0
        val raw = if (allerRetour) distanceKm * 2 else distanceKm
        return if (allerRetour) maxOf(0, raw - FRANCHISE_DEDUCTION_KM) else raw
    }

    /**
     * Retourne le détail textuel de l'application de la franchise
     */
    fun getDistanceFormulaText(distanceKm: Int): String {
        if (distanceKm == 0) return "0 km (Même base)"
        val rawAR = distanceKm * 2
        val net = getNetDistanceKm(distanceKm, allerRetour = true)
        return "$rawAR km A/R - 50 km (déduction) = $net km nets"
    }

    /**
     * Estimation du temps de trajet moyen par la route
     */
    fun getEstimatedDuration(distanceKm: Int): String {
        if (distanceKm == 0) return "0 min (Même base)"
        val hours = distanceKm / 80
        val minutes = ((distanceKm % 80) * 60) / 80
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes} min"
    }

    /**
     * Estimation de l'indemnité kilométrique selon le barème indiciaire (aller-retour avec franchise de 50 km déduite)
     */
    fun getEstimatedIndemniteKm(distanceKm: Int, echelle: Int, allerRetour: Boolean = true): Double {
        val netKm = getNetDistanceKm(distanceKm, allerRetour)
        val ratePerKm = when {
            echelle >= 700 -> 0.350 // Cadres supérieurs / Ingénieurs en chef
            echelle >= 500 -> 0.280 // Maîtrise / Techniciens confirmés
            else -> 0.220           // Exécution / Techniciens juniors
        }
        return netKm * ratePerKm
    }

    /**
     * Taux journalier de mission selon le type de mission et l'échelle du technicien.
     * Recherche prioritaire dans les coûts unitaires de mission configurés dans le référentiel,
     * puis dans les barèmes généraux, sinon applique le barème indiciaire standard.
     */
    fun getMissionDailyRateByTypeAndEchelle(
        missionType: String,
        echelle: Int,
        coutsMission: List<com.example.data.local.CoutMissionEntity> = emptyList(),
        baremes: List<com.example.data.local.BaremeTarifEntity> = emptyList()
    ): Double {
        val normEchelle = if (echelle > 50) ((echelle - 100) / 40 + 1).coerceIn(1, 20) else echelle.coerceIn(1, 20)

        // 1. Recherche prioritaire dans les coûts unitaires de mission
        if (coutsMission.isNotEmpty()) {
            val matchedExact = coutsMission.firstOrNull { c ->
                val cClean = c.typeMission.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
                val mClean = missionType.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
                val typeMatch = missionType.isNotBlank() && (
                    c.typeMission.trim().equals(missionType.trim(), ignoreCase = true) ||
                    c.typeMission.contains(missionType, ignoreCase = true) ||
                    missionType.contains(c.typeMission, ignoreCase = true) ||
                    (cClean.isNotEmpty() && (cClean == mClean || cClean.contains(mClean) || mClean.contains(cClean)))
                )
                val echelleMatch = (normEchelle >= c.echelleMin && normEchelle <= c.echelleMax) ||
                        (echelle >= c.echelleMin && echelle <= c.echelleMax)
                typeMatch && echelleMatch
            }
            if (matchedExact != null) {
                return matchedExact.coutUnitaireJournalier
            }

            // Fallback par échelle dans coutsMission si type non trouvé
            val matchedByEchelleOnly = coutsMission.firstOrNull { c ->
                (normEchelle >= c.echelleMin && normEchelle <= c.echelleMax) ||
                        (echelle >= c.echelleMin && echelle <= c.echelleMax)
            }
            if (matchedByEchelleOnly != null) {
                return matchedByEchelleOnly.coutUnitaireJournalier
            }
        }

        // 2. Recherche dans baremes avec categorie 'Mission'
        val customBareme = baremes.firstOrNull { b ->
            b.categorie.equals("Mission", ignoreCase = true) &&
                    ((echelle >= b.echelleMin && echelle <= b.echelleMax) ||
                            (normEchelle >= b.echelleMin && normEchelle <= b.echelleMax))
        }
        if (customBareme != null) {
            return customBareme.tarifUnitaire
        }

        // 3. Barème indiciaire standard par palier
        return when {
            normEchelle >= 16 || echelle >= 700 -> 120.000 // Cadres & Inspecteurs CND
            normEchelle >= 11 || echelle >= 400 -> 90.000  // Techniciens B1/B2 confirmés
            normEchelle >= 6 || echelle >= 200 -> 70.000   // Techniciens qualifiés
            else -> 50.000                                 // Techniciens Piste / Juniors
        }
    }

    /**
     * Taux journalier de mission selon l'échelle du technicien (compatibilité).
     */
    fun getMissionDailyRate(echelle: Int, baremes: List<com.example.data.local.BaremeTarifEntity> = emptyList()): Double {
        return getMissionDailyRateByTypeAndEchelle("", echelle, emptyList(), baremes)
    }

    /**
     * Calcule la durée d'une mission en jours pleins à partir des timestamps de début et fin.
     */
    fun getMissionDurationDays(dateDebut: Long, dateFin: Long): Int {
        if (dateFin <= dateDebut) return 1
        val days = ((dateFin - dateDebut) / 86400000L).toInt()
        return if (days < 1) 1 else days
    }

    /**
     * Calcule le délai d'hébergement automatique (durée de la mission déduite de 1).
     * Règle : délai hébergements = max(0, durée mission - 1).
     */
    fun getDelaiHebergementAuto(dureeMissionJours: Int): Int {
        return (dureeMissionJours - 1).coerceAtLeast(0)
    }

    /**
     * Extrait le premier chiffre de l'échelle indiciaire : NUMBER(LEFT([Echelle], 1))
     */
    fun getEchelleFirstDigit(echelle: Int): Int {
        val s = echelle.toString().trim()
        return s.firstOrNull { it.isDigit() }?.digitToInt() ?: 1
    }

    fun isAssistanceTechniqueMission(mission: String): Boolean {
        val clean = mission.trim().uppercase()
        return clean.contains("ASSISTANCE TECHNIQUE") ||
                clean.contains("ASSISTANCE") ||
                clean == "💡ASSISTANCE TECHNIQUE"
    }

    fun isRenfortHangarMission(mission: String): Boolean {
        val clean = mission.trim().uppercase()
        return clean.contains("RENFORT HANGAR") ||
                clean.contains("RENFORT") ||
                clean == "🛠️RENFORT HANGAR"
    }

    /**
     * Calcul officiel du coût de mission (MontantODD: Price) selon la règle SWITCH de gestion :
     *
     * SWITCH( TRUE,
     *   AND(
     *     ISNOTBLANK(FILTER("FRAIS", AND([ODD N] = [_THISROW].[ODD N], [Catégorie Frais] = "Hebergement" ))),
     *     NUMBER(LEFT([Echelle], 1)) < 5
     *   ),
     *   ([Prix_Unitaire_Mission] * (1 + ((MAX(LIST(1, [DelaisJours])) - 1) * 0.6))),
     *
     *   [Mission] = "💡ASSISTANCE TECHNIQUE",
     *   ([Prix_Unitaire_Mission] * CEILING(MAX(LIST(1, [DelaisJours])) / 8.0)),
     *
     *   AND(
     *     [Mission] = "🛠️RENFORT HANGAR",
     *     [DelaisJours] > 30,
     *     NUMBER(LEFT([Echelle], 1)) >= 5,
     *     NUMBER(LEFT([Echelle], 1)) <= 6
     *   ),
     *   (([Prix_Unitaire_Mission] * 30) + (([DelaisJours] - 30) * [Prix_Unitaire_Mission] * 2 / 3)),
     *
     *   ([Prix_Unitaire_Mission] * MAX(LIST(1, [DelaisJours])))
     * )
     */
    fun calculateCoutMission(
        delaisJours: Int,
        prixUnitaireMission: Double,
        echelle: Int,
        mission: String,
        hasHebergementFrais: Boolean
    ): Double {
        return computeDetailedCoutMission(
            delaisJours = delaisJours,
            prixUnitaireMission = prixUnitaireMission,
            echelle = echelle,
            mission = mission,
            hasHebergementFrais = hasHebergementFrais
        ).coutTotal
    }

    fun computeDetailedCoutMission(
        delaisJours: Int,
        prixUnitaireMission: Double,
        echelle: Int,
        mission: String,
        hasHebergementFrais: Boolean
    ): MissionCostResult {
        val effectiveDays = maxOf(1, delaisJours)
        val echelleFirstDigit = getEchelleFirstDigit(echelle)

        // Condition 1 : Présence d'un frais d'Hébergement ET Premier chiffre échelle < 5
        // Formule : [Prix_Unitaire_Mission] * (1 + ((MAX(LIST(1, [DelaisJours])) - 1) * 0.6))
        if (hasHebergementFrais && echelleFirstDigit < 5) {
            val factor = 1.0 + ((effectiveDays - 1) * 0.6)
            val total = prixUnitaireMission * factor
            return MissionCostResult(
                coutTotal = total,
                regleAppliquee = "Hébergement (Échelle < 5)",
                formuleDescription = "%.3f TND × (1 + ($effectiveDays - 1) × 0.6) = %.3f TND".format(prixUnitaireMission, total),
                tauxJournalierBase = prixUnitaireMission
            )
        }

        // Condition 2 : Mission = "💡ASSISTANCE TECHNIQUE"
        // Formule : [Prix_Unitaire_Mission] * CEILING(MAX(LIST(1, [DelaisJours])) / 8.0)
        if (isAssistanceTechniqueMission(mission)) {
            val tranches = kotlin.math.ceil(effectiveDays / 8.0)
            val total = prixUnitaireMission * tranches
            return MissionCostResult(
                coutTotal = total,
                regleAppliquee = "Assistance Technique (tranches 8j)",
                formuleDescription = "%.3f TND × ⌈$effectiveDays / 8⌉ (${tranches.toInt()} tranche(s)) = %.3f TND".format(prixUnitaireMission, total),
                tauxJournalierBase = prixUnitaireMission
            )
        }

        // Condition 3 : Mission = "🛠️RENFORT HANGAR" ET DelaisJours > 30 ET Premier chiffre échelle in 5..6
        // Formule : ([Prix_Unitaire_Mission] * 30) + (([DelaisJours] - 30) * [Prix_Unitaire_Mission] * 2 / 3)
        if (isRenfortHangarMission(mission) && effectiveDays > 30 && echelleFirstDigit in 5..6) {
            val joursSupplementaires = effectiveDays - 30
            val base30 = prixUnitaireMission * 30.0
            val extraPart = joursSupplementaires * prixUnitaireMission * (2.0 / 3.0)
            val total = base30 + extraPart
            return MissionCostResult(
                coutTotal = total,
                regleAppliquee = "Renfort Hangar (>30j, Échelle 5-6)",
                formuleDescription = "(%.3f TND × 30j) + ($joursSupplementaires j × %.3f TND × 2/3) = %.3f TND".format(prixUnitaireMission, prixUnitaireMission, total),
                tauxJournalierBase = prixUnitaireMission
            )
        }

        // Condition 4 (Défaut) : [Prix_Unitaire_Mission] * MAX(LIST(1, [DelaisJours]))
        val total = prixUnitaireMission * effectiveDays
        return MissionCostResult(
            coutTotal = total,
            regleAppliquee = "Standard",
            formuleDescription = "$effectiveDays j × %.3f TND/j = %.3f TND".format(prixUnitaireMission, total),
            tauxJournalierBase = prixUnitaireMission
        )
    }

    private fun cleanSiteName(site: String): String {
        val s = site.trim()
        return when {
            s.contains("Tunis", ignoreCase = true) -> "Tunis"
            s.contains("Monastir", ignoreCase = true) -> "Monastir"
            s.contains("Sfax", ignoreCase = true) -> "Sfax"
            s.contains("Djerba", ignoreCase = true) -> "Djerba"
            s.contains("Tozeur", ignoreCase = true) -> "Tozeur"
            s.contains("Tabarka", ignoreCase = true) -> "Tabarka"
            s.contains("Enfidha", ignoreCase = true) -> "Enfidha"
            s.contains("Gafsa", ignoreCase = true) -> "Gafsa"
            else -> s
        }
    }
}

/**
 * Résultat détaillé du calcul du coût de mission
 */
data class MissionCostResult(
    val coutTotal: Double,
    val regleAppliquee: String,
    val formuleDescription: String,
    val tauxJournalierBase: Double
)
