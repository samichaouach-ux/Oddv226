package com.example.data.repository

import android.content.Context
import com.example.data.firebase.FirestoreSyncManager
import com.example.data.local.*
import com.example.util.EmailNotificationData
import com.example.util.EmailNotifier
import com.example.util.PdfReportGenerator
import com.example.util.SiteDistances
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File

class ZoddRepository(
    private val context: Context,
    private val dao: ZoddDao,
    private val syncManager: FirestoreSyncManager
) {
    init {
        // Applique l'état demandé de la base de données : TAT-0023-26 non figé / validations annulées,
        // et DJE-0005-26 avec seulement visa DM actif.
        CoroutineScope(Dispatchers.IO).launch {
            applyRequestedDatabaseState()
        }
    }

    suspend fun applyRequestedDatabaseState() {
        try {
            // 1. Rendre le lot TAT-0023-26 comme lot Non figé et annuler les validations faites
            val lotTat = dao.getLotById("TAT-0023-26")
            if (lotTat != null) {
                dao.updateLot(
                    lotTat.copy(
                        figerLot = false,
                        statutValidation = "OUVERT",
                        directionActuelle = "DM",
                        versionPdfPath = null,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                val vals = dao.getValidationsForLot("TAT-0023-26")
                vals.forEach { v ->
                    dao.updateValidation(
                        v.copy(
                            valide = "EN_ATTENTE",
                            dateValidation = null,
                            commentaire = "",
                            signataire = "",
                            historiqueVal = "En attente du visa initial de la Direction Maintenance",
                            reactiviteJours = 0.0,
                            emailEnvoye = false
                        )
                    )
                }
            }

            // 2. Pour le Lot DJE-0005-26 annuler toutes les validations et garder seulement celle du DM
            var lotDje = dao.getLotById("DJE-0005-26")
            val now = System.currentTimeMillis()
            if (lotDje == null) {
                lotDje = LotEntity(
                    lotN = "DJE-0005-26",
                    libelle = "Assistance Escale & Renfort Ligne - Djerba Zarzis",
                    dateCreation = now - (6 * 86400000L),
                    basculementVues = "LES ODD",
                    figerLot = true,
                    statutValidation = "VALIDE_DM",
                    directionActuelle = "DCT",
                    totalMission = 1.0,
                    totalCoutOdd = 685.000,
                    totalHebergement = 280.000,
                    totalTransport = 225.000,
                    totalFrais = 50.000,
                    isSynced = true
                )
                dao.insertLot(lotDje)
            } else {
                dao.updateLot(
                    lotDje.copy(
                        figerLot = true,
                        basculementVues = "LES ODD",
                        statutValidation = "VALIDE_DM",
                        directionActuelle = "DCT",
                        updatedAt = now
                    )
                )
            }

            val directions = listOf("DM", "DCT", "DGRT", "DCST", "AUDIT", "DG", "DAF")
            val existingVals = dao.getValidationsForLot("DJE-0005-26")
            if (existingVals.isEmpty()) {
                val newVals = directions.mapIndexed { index, dir ->
                    if (dir == "DM") {
                        ValidationEntity(
                            idVal = "VAL-DJE-DM",
                            lotN = "DJE-0005-26",
                            direction = "DM",
                            ordre = 1,
                            valide = "VALIDE",
                            dateValidation = now - (5 * 86400000L),
                            commentaire = "Mission conforme au planning technique d'intervention.",
                            signataire = "Ing. Mehdi Ben Amor",
                            historiqueVal = "Approuvé par Direction Maintenance",
                            ttalMission = 1.0,
                            ttalCoutOdd = 685.0,
                            ttalHebergement = 280.0,
                            ttalFrais = 275.0,
                            reactiviteJours = 1.0,
                            emailEnvoye = true,
                            isSynced = true
                        )
                    } else {
                        ValidationEntity(
                            idVal = "VAL-DJE-$dir",
                            lotN = "DJE-0005-26",
                            direction = dir,
                            ordre = index + 1,
                            valide = "EN_ATTENTE",
                            dateValidation = null,
                            commentaire = "",
                            signataire = "",
                            reactiviteJours = 0.0,
                            isSynced = true
                        )
                    }
                }
                dao.insertValidations(newVals)
            } else {
                existingVals.forEach { v ->
                    if (v.direction == "DM") {
                        dao.updateValidation(
                            v.copy(
                                valide = "VALIDE",
                                dateValidation = v.dateValidation ?: (now - (5 * 86400000L)),
                                commentaire = "Mission conforme au planning technique d'intervention.",
                                signataire = "Ing. Mehdi Ben Amor",
                                historiqueVal = "Approuvé par Direction Maintenance",
                                reactiviteJours = 1.0
                            )
                        )
                    } else {
                        dao.updateValidation(
                            v.copy(
                                valide = "EN_ATTENTE",
                                dateValidation = null,
                                commentaire = "",
                                signataire = "",
                                historiqueVal = "",
                                reactiviteJours = 0.0
                            )
                        )
                    }
                }
            }

            // 3. Assurer que le Lot figé TAT-0019-26 existe et possède ses ODDs, Frais et Justificatifs
            var lotTat19 = dao.getLotById("TAT-0019-26")
            if (lotTat19 == null) {
                lotTat19 = LotEntity(
                    lotN = "TAT-0019-26",
                    libelle = "Visite Licence & Formation Avionique - Sfax",
                    dateCreation = now - (10 * 86400000L),
                    basculementVues = "LES ODD",
                    figerLot = true,
                    statutValidation = "VALIDE_DCT",
                    directionActuelle = "DGRT",
                    totalMission = 2.0,
                    totalCoutOdd = 920.000,
                    totalHebergement = 520.000,
                    totalTransport = 300.000,
                    totalFrais = 100.000,
                    isSynced = true
                )
                dao.insertLot(lotTat19)
            } else if (!lotTat19.figerLot || lotTat19.basculementVues != "LES ODD") {
                dao.updateLot(lotTat19.copy(figerLot = true, basculementVues = "LES ODD"))
            }

            val valsTat19 = dao.getValidationsForLot("TAT-0019-26")
            if (valsTat19.isEmpty()) {
                val newVals = listOf(
                    ValidationEntity(idVal = "VAL-0019-DM", lotN = "TAT-0019-26", direction = "DM", ordre = 1, valide = "VALIDE", dateValidation = now - (8 * 86400000L), commentaire = "Mission conforme au planning technique d'intervention.", signataire = "Ing. Mehdi Ben Amor", historiqueVal = "Approuvé par Direction Maintenance", ttalMission = 2.0, ttalCoutOdd = 920.0, ttalHebergement = 520.0, ttalFrais = 400.0, reactiviteJours = 1.2, emailEnvoye = true, isSynced = true),
                    ValidationEntity(idVal = "VAL-0019-DCT", lotN = "TAT-0019-26", direction = "DCT", ordre = 2, valide = "VALIDE", dateValidation = now - (6 * 86400000L), commentaire = "Contrôles avionique et visas conformes aux normes PART-66/145.", signataire = "Mme Salma Kallel", historiqueVal = "Approuvé par Direction Contrôle Technique", ttalMission = 2.0, ttalCoutOdd = 920.0, isSynced = true),
                    ValidationEntity(idVal = "VAL-0019-DGRT", lotN = "TAT-0019-26", direction = "DGRT", ordre = 3, valide = "EN_ATTENTE", isSynced = true),
                    ValidationEntity(idVal = "VAL-0019-DCST", lotN = "TAT-0019-26", direction = "DCST", ordre = 4, valide = "EN_ATTENTE", isSynced = true),
                    ValidationEntity(idVal = "VAL-0019-AUDIT", lotN = "TAT-0019-26", direction = "AUDIT", ordre = 5, valide = "EN_ATTENTE", isSynced = true),
                    ValidationEntity(idVal = "VAL-0019-DG", lotN = "TAT-0019-26", direction = "DG", ordre = 6, valide = "EN_ATTENTE", isSynced = true),
                    ValidationEntity(idVal = "VAL-0019-DAF", lotN = "TAT-0019-26", direction = "DAF", ordre = 7, valide = "EN_ATTENTE", isSynced = true)
                )
                dao.insertValidations(newVals)
            }

            val oddsTat19 = dao.getOddsForLot("TAT-0019-26")
            if (oddsTat19.isEmpty()) {
                val oddSfx = OddEntity(
                    oddN = "SFX-0019-26",
                    lotN = "TAT-0019-26",
                    matricule = "MAT-1042",
                    siteProvenance = "Tunis",
                    echelle = 500,
                    siteIntervention = "Sfax",
                    dateDebut = now - (10 * 86400000L),
                    dateFin = now - (8 * 86400000L),
                    mission = "Formation Avionique",
                    modeEdit = false,
                    avanceSurMission = 150.0,
                    totalOdd = 520.0,
                    isSynced = true
                )
                val oddMir = OddEntity(
                    oddN = "MIR-0019-26",
                    lotN = "TAT-0019-26",
                    matricule = "MAT-2088",
                    siteProvenance = "Monastir",
                    echelle = 400,
                    siteIntervention = "Sfax",
                    dateDebut = now - (9 * 86400000L),
                    dateFin = now - (8 * 86400000L),
                    mission = "Visite Licence",
                    modeEdit = false,
                    avanceSurMission = 0.0,
                    totalOdd = 400.0,
                    isSynced = true
                )
                dao.insertOdd(oddSfx)
                dao.insertOdd(oddMir)

                dao.insertFrais(
                    FraisEntity(
                        idFrais = "FR-19-01",
                        oddN = "SFX-0019-26",
                        date = now - (10 * 86400000L),
                        categorie = "Hébergement",
                        sousCategorie = "LPD",
                        unite = "Nuité",
                        quantite = 2.0,
                        pUnit = 160.0,
                        montant = 320.0,
                        aComptabiliser = 320.0,
                        estComptabilise = true,
                        isSynced = true
                    )
                )
                dao.insertFrais(
                    FraisEntity(
                        idFrais = "FR-19-02",
                        oddN = "SFX-0019-26",
                        date = now - (10 * 86400000L),
                        categorie = "Transport",
                        sousCategorie = "Voiture personnelle",
                        unite = "Taux/Km",
                        quantite = 270.0,
                        pUnit = 0.450,
                        montant = 121.5,
                        aComptabiliser = 121.5,
                        estComptabilise = true,
                        isSynced = true
                    )
                )
                dao.insertFrais(
                    FraisEntity(
                        idFrais = "FR-19-03",
                        oddN = "MIR-0019-26",
                        date = now - (9 * 86400000L),
                        categorie = "Hébergement",
                        sousCategorie = "1/2 Pension",
                        unite = "Nuité",
                        quantite = 1.0,
                        pUnit = 140.0,
                        montant = 140.0,
                        aComptabiliser = 140.0,
                        estComptabilise = true,
                        isSynced = true
                    )
                )

                dao.insertDocument(
                    DocumentRattachementEntity(
                        idDoc = "DOC-19-01",
                        oddN = "SFX-0019-26",
                        typeDocument = "Facture hôtel",
                        objet = "Hôtel Les Oliviers Palace Sfax (Chambre 204)",
                        dateEnvoie = now - (8 * 86400000L),
                        scanDocUri = null,
                        isSynced = true
                    )
                )
                dao.insertDocument(
                    DocumentRattachementEntity(
                        idDoc = "DOC-19-02",
                        oddN = "SFX-0019-26",
                        typeDocument = "Ticket péage",
                        objet = "Péage Autoroute A1 Tunis - Sfax aller-retour",
                        dateEnvoie = now - (8 * 86400000L),
                        scanDocUri = null,
                        isSynced = true
                    )
                )
                dao.insertDocument(
                    DocumentRattachementEntity(
                        idDoc = "DOC-19-03",
                        oddN = "MIR-0019-26",
                        typeDocument = "Ordre mission",
                        objet = "Ordre de mission signé DM n° 2026/09",
                        dateEnvoie = now - (8 * 86400000L),
                        scanDocUri = null,
                        isSynced = true
                    )
                )
            }

            // 4. Assurer que le Lot figé DJE-0005-26 possède ses ODDs, Frais et Justificatifs
            val oddsDje = dao.getOddsForLot("DJE-0005-26")
            if (oddsDje.isEmpty()) {
                val oddDje = OddEntity(
                    oddN = "DJE-0042-26",
                    lotN = "DJE-0005-26",
                    matricule = "MAT-1042",
                    siteProvenance = "Tunis",
                    echelle = 500,
                    siteIntervention = "Djerba",
                    dateDebut = now - (6 * 86400000L),
                    dateFin = now - (4 * 86400000L),
                    mission = "Intervention sur avion",
                    modeEdit = false,
                    avanceSurMission = 100.0,
                    totalOdd = 685.0,
                    isSynced = true
                )
                dao.insertOdd(oddDje)

                dao.insertFrais(
                    FraisEntity(
                        idFrais = "FR-DJE-01",
                        oddN = "DJE-0042-26",
                        date = now - (6 * 86400000L),
                        categorie = "Hébergement",
                        sousCategorie = "1/2 Pension",
                        unite = "Nuité",
                        quantite = 2.0,
                        pUnit = 140.0,
                        montant = 280.0,
                        aComptabiliser = 280.0,
                        estComptabilise = true,
                        isSynced = true
                    )
                )
                dao.insertFrais(
                    FraisEntity(
                        idFrais = "FR-DJE-02",
                        oddN = "DJE-0042-26",
                        date = now - (6 * 86400000L),
                        categorie = "Transport",
                        sousCategorie = "Billet transport",
                        unite = "Billet",
                        quantite = 1.0,
                        pUnit = 225.0,
                        montant = 225.0,
                        aComptabiliser = 225.0,
                        estComptabilise = true,
                        isSynced = true
                    )
                )
                dao.insertFrais(
                    FraisEntity(
                        idFrais = "FR-DJE-03",
                        oddN = "DJE-0042-26",
                        date = now - (5 * 86400000L),
                        categorie = "Divers",
                        sousCategorie = "Repas",
                        unite = "Repas",
                        quantite = 2.0,
                        pUnit = 25.0,
                        montant = 50.0,
                        aComptabiliser = 50.0,
                        estComptabilise = true,
                        isSynced = true
                    )
                )

                dao.insertDocument(
                    DocumentRattachementEntity(
                        idDoc = "DOC-DJE-01",
                        oddN = "DJE-0042-26",
                        typeDocument = "Billet transport",
                        objet = "Billet électronique Tunisair Express TUN-DJE",
                        dateEnvoie = now - (4 * 86400000L),
                        scanDocUri = null,
                        isSynced = true
                    )
                )
                dao.insertDocument(
                    DocumentRattachementEntity(
                        idDoc = "DOC-DJE-02",
                        oddN = "DJE-0042-26",
                        typeDocument = "Facture hôtel",
                        objet = "Facture Hôtel Djerba Plaza Thalasso (2 nuités)",
                        dateEnvoie = now - (4 * 86400000L),
                        scanDocUri = null,
                        isSynced = true
                    )
                )
                dao.insertDocument(
                    DocumentRattachementEntity(
                        idDoc = "DOC-DJE-03",
                        oddN = "DJE-0042-26",
                        typeDocument = "Ordre mission",
                        objet = "Rapport d'intervention technique A320 TS-IMB",
                        dateEnvoie = now - (4 * 86400000L),
                        scanDocUri = null,
                        isSynced = true
                    )
                )
            }
        } catch (_: Exception) {}
    }
    // Flows
    val lotsFlow: Flow<List<LotEntity>> = dao.getAllLotsFlow()
    val oddsFlow: Flow<List<OddEntity>> = dao.getAllOddsFlow()
    val allFraisFlow: Flow<List<FraisEntity>> = dao.getAllFraisFlow()
    val allDocumentsFlow: Flow<List<DocumentRattachementEntity>> = dao.getAllDocumentsFlow()
    val allValidationsFlow: Flow<List<ValidationEntity>> = dao.getAllValidationsFlow()
    val techniciensFlow: Flow<List<TechnicienEntity>> = dao.getAllTechniciensFlow()
    val directionsFlow: Flow<List<DirectionEntity>> = dao.getAllDirectionsFlow()
    val baremesFlow: Flow<List<BaremeTarifEntity>> = dao.getAllBaremesFlow()
    val coutsMissionFlow: Flow<List<CoutMissionEntity>> = dao.getAllCoutsMissionFlow()

    fun getLotFlow(lotN: String): Flow<LotEntity?> = dao.getLotFlow(lotN)
    fun getOddsForLotFlow(lotN: String): Flow<List<OddEntity>> = dao.getOddsForLotFlow(lotN)
    fun getFraisForLotFlow(lotN: String): Flow<List<FraisEntity>> = dao.getFraisForLotFlow(lotN)
    fun getDocumentsForLotFlow(lotN: String): Flow<List<DocumentRattachementEntity>> = dao.getDocumentsForLotFlow(lotN)
    fun getValidationsForLotFlow(lotN: String): Flow<List<ValidationEntity>> = dao.getValidationsForLotFlow(lotN)

    suspend fun getLotById(lotN: String): LotEntity? = dao.getLotById(lotN)
    suspend fun getTechnicien(matricule: String): TechnicienEntity? = dao.getTechnicien(matricule)
    suspend fun getDirection(code: String): DirectionEntity? = dao.getDirection(code)

    // --- LOT OPERATIONS ---
    suspend fun createLot(lotN: String, libelle: String, detailDivers: String = ""): LotEntity {
        val lot = LotEntity(
            lotN = lotN.trim().uppercase(),
            libelle = libelle.trim(),
            dateCreation = System.currentTimeMillis(),
            basculementVues = "LES ODD",
            figerLot = false,
            statutValidation = "EN_COURS",
            directionActuelle = "DM",
            detailDivers = detailDivers.trim()
        )
        dao.insertLot(lot)
        syncManager.syncSingleLot(lot)
        return lot
    }

    suspend fun updateLotView(lotN: String, newView: String) {
        val lot = dao.getLotById(lotN) ?: return
        dao.updateLot(lot.copy(basculementVues = newView))
    }

    suspend fun recalculateLotTotals(lotN: String) {
        val lot = dao.getLotById(lotN) ?: return
        val odds = dao.getOddsForLot(lotN)
        val frais = dao.getFraisForLot(lotN)
        val baremes = dao.getAllBaremesSync()
        val coutsMission = dao.getAllCoutsMissionSync()

        var totalOddSum = 0.0
        var totalCoutMissionSum = 0.0
        for (odd in odds) {
            val oddFrais = frais.filter { it.oddN == odd.oddN }
            val sumOddComptabilise = oddFrais.filter { it.estComptabilise }.sumOf { it.montant }
            
            // Calcul du coût de la mission selon la formule de gestion SWITCH (MontantODD Price)
            val dureeJours = SiteDistances.getMissionDurationDays(odd.dateDebut, odd.dateFin)
            val tauxMissionJournalier = SiteDistances.getMissionDailyRateByTypeAndEchelle(
                missionType = odd.mission,
                echelle = odd.echelle,
                coutsMission = coutsMission,
                baremes = baremes
            )
            val hasHebergementFrais = oddFrais.any {
                it.categorie.contains("Héberg", ignoreCase = true) || it.categorie.contains("Heberg", ignoreCase = true)
            }
            val coutMission = SiteDistances.calculateCoutMission(
                delaisJours = dureeJours,
                prixUnitaireMission = tauxMissionJournalier,
                echelle = odd.echelle,
                mission = odd.mission,
                hasHebergementFrais = hasHebergementFrais
            )
            totalCoutMissionSum += coutMission

            // Coût total de la mission dans l'ODD = Coût mission (durée * taux unitaire échelle) + Frais comptabilisés - Avance
            val totalBrutOdd = coutMission + sumOddComptabilise
            val netOdd = (totalBrutOdd - odd.avanceSurMission).coerceAtLeast(0.0)
            if (odd.totalOdd != netOdd) {
                dao.updateOdd(odd.copy(totalOdd = netOdd, isSynced = false, updatedAt = System.currentTimeMillis()))
            }
            totalOddSum += netOdd
        }

        val comptabilises = frais.filter { it.estComptabilise }
        val totalHeberg = comptabilises.filter { it.categorie.contains("Héberg", true) }.sumOf { it.montant }
        val totalTransp = comptabilises.filter { it.categorie.contains("Transp", true) }.sumOf { it.montant }
        val totalDiv = comptabilises.filter { it.categorie.contains("Div", true) }.sumOf { it.montant }

        val updated = lot.copy(
            totalMission = totalCoutMissionSum,
            totalCoutOdd = totalOddSum,
            totalHebergement = totalHeberg,
            totalTransport = totalTransp,
            totalFrais = totalDiv,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        dao.updateLot(updated)
        syncManager.syncSingleLot(updated)
    }

    // --- ODD OPERATIONS ---
    suspend fun addOdd(
        oddN: String,
        lotN: String,
        matricule: String,
        siteProvenance: String,
        echelle: Int,
        siteIntervention: String,
        dateDebut: Long,
        dateFin: Long,
        mission: String,
        avanceSurMission: Double = 0.0,
        detailDivers: String = ""
    ): OddEntity {
        val odd = OddEntity(
            oddN = oddN.trim().uppercase(),
            lotN = lotN,
            matricule = matricule,
            siteProvenance = siteProvenance,
            echelle = echelle,
            siteIntervention = siteIntervention,
            dateDebut = dateDebut,
            dateFin = dateFin,
            mission = mission,
            avanceSurMission = avanceSurMission,
            detailDivers = detailDivers.trim(),
            totalOdd = 0.0
        )
        dao.insertOdd(odd)
        recalculateLotTotals(lotN)
        return odd
    }

    suspend fun updateOdd(odd: OddEntity) {
        dao.updateOdd(odd.copy(isSynced = false, updatedAt = System.currentTimeMillis()))
        recalculateLotTotals(odd.lotN)
    }

    suspend fun deleteOdd(odd: OddEntity) {
        dao.deleteOdd(odd)
        recalculateLotTotals(odd.lotN)
    }

    // --- FRAIS OPERATIONS ---
    suspend fun addFrais(
        idFrais: String,
        oddN: String,
        lotN: String,
        date: Long,
        categorie: String,
        sousCategorie: String,
        unite: String,
        quantite: Double,
        pUnit: Double,
        estComptabilise: Boolean = true
    ): FraisEntity {
        val montant = quantite * pUnit
        val frais = FraisEntity(
            idFrais = idFrais.trim().uppercase(),
            oddN = oddN,
            date = date,
            categorie = categorie,
            sousCategorie = sousCategorie,
            unite = unite,
            quantite = quantite,
            pUnit = pUnit,
            montant = montant,
            aComptabiliser = if (estComptabilise) montant else 0.0,
            estComptabilise = estComptabilise
        )
        dao.insertFrais(frais)
        recalculateLotTotals(lotN)
        return frais
    }

    suspend fun toggleFraisComptabilise(frais: FraisEntity, lotN: String) {
        val newComptabilise = !frais.estComptabilise
        val updated = frais.copy(
            estComptabilise = newComptabilise,
            aComptabiliser = if (newComptabilise) frais.montant else 0.0,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        dao.updateFrais(updated)
        recalculateLotTotals(lotN)
    }

    suspend fun deleteFrais(frais: FraisEntity, lotN: String) {
        dao.deleteFrais(frais)
        recalculateLotTotals(lotN)
    }

    // --- DOCUMENT RATTACHEMENT ---
    suspend fun attachDocument(
        idDoc: String,
        oddN: String,
        typeDoc: String,
        objet: String,
        uri: String?
    ) {
        val doc = DocumentRattachementEntity(
            idDoc = idDoc.trim().uppercase(),
            oddN = oddN,
            typeDocument = typeDoc,
            objet = objet,
            dateEnvoie = System.currentTimeMillis(),
            scanDocUri = uri
        )
        dao.insertDocument(doc)
    }

    // --- WORKFLOW: FIGER LOT (GEL & PDF & EMAIL TO DM) ---
    suspend fun figerLot(lotN: String): Pair<File, EmailNotificationData?> {
        val lot = dao.getLotById(lotN) ?: throw IllegalStateException("Lot $lotN introuvable")
        val odds = dao.getOddsForLot(lotN)
        val frais = dao.getFraisForLot(lotN)

        // 1. Initialize validation chain if not already present
        var existingValidations = dao.getValidationsForLot(lotN)
        if (existingValidations.isEmpty()) {
            val directions = listOf("DM", "DCT", "DGRT", "DCST", "AUDIT", "DG", "DAF")
            val newValidations = directions.mapIndexed { index, dir ->
                ValidationEntity(
                    idVal = "VAL-${lotN}-$dir",
                    lotN = lotN,
                    direction = dir,
                    ordre = index + 1,
                    valide = "EN_ATTENTE"
                )
            }
            dao.insertValidations(newValidations)
            existingValidations = newValidations
        }

        val techniciens = dao.getAllTechniciens()
        val documents = dao.getDocumentsForLot(lotN)
        val directions = dao.getAllDirections()

        // 2. Generate PDF report
        val pdfFile = PdfReportGenerator.generateLotReportPdf(
            context = context,
            lot = lot.copy(figerLot = true),
            odds = odds,
            fraisList = frais,
            validations = existingValidations,
            techniciens = techniciens,
            documents = documents,
            directions = directions
        )

        // 3. Prepare email for DM (first direction)
        val dmDir = dao.getDirection("DM")
        val netTotal = (lot.totalCoutOdd - odds.sumOf { it.avanceSurMission }).coerceAtLeast(0.0)
        val emailData = if (dmDir != null) {
            EmailNotifier.prepareEmailForDirection(
                lot = lot,
                directionCode = "DM",
                directionName = dmDir.libelle,
                directionEmail = dmDir.email,
                directorName = dmDir.directeur,
                directorPhone = dmDir.telephone,
                pdfFile = pdfFile,
                oddsCount = odds.size,
                netTotal = netTotal
            )
        } else null

        // 4. Update Lot to FIGE and mode lecture seule
        val updatedLot = lot.copy(
            figerLot = true,
            statutValidation = "FIGE_EN_ATTENTE_DM",
            directionActuelle = "DM",
            versionPdfPath = pdfFile.absolutePath,
            emailDmEnvoye = true,
            basculementVues = "VALIDATION",
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        dao.updateLot(updatedLot)
        syncManager.syncSingleLot(updatedLot)

        return Pair(pdfFile, emailData)
    }

    // --- WORKFLOW: VALIDATION DIRECTION PAR DIRECTION ---
    suspend fun processDirectionValidation(
        lotN: String,
        directionCode: String,
        isApproved: Boolean,
        commentaire: String,
        signataire: String
    ): EmailNotificationData? {
        val lot = dao.getLotById(lotN) ?: return null
        var validations = dao.getValidationsForLot(lotN).sortedBy { it.ordre }
        if (validations.isEmpty()) {
            val directionsList = listOf("DM", "DCT", "DGRT", "DCST", "AUDIT", "DG", "DAF")
            val newValidations = directionsList.mapIndexed { index, dir ->
                ValidationEntity(
                    idVal = "VAL-${lotN}-$dir",
                    lotN = lotN,
                    direction = dir,
                    ordre = index + 1,
                    valide = "EN_ATTENTE"
                )
            }
            dao.insertValidations(newValidations)
            validations = newValidations
        }

        val currentStep = validations.firstOrNull { it.direction == directionCode } ?: validations.first()

        val now = System.currentTimeMillis()
        // Calculate reactivite in days
        val prevTimestamp = if (currentStep.ordre == 1) lot.dateCreation else {
            val prev = validations.getOrNull(currentStep.ordre - 2)
            prev?.dateValidation ?: lot.dateCreation
        }
        val diffMs = now - prevTimestamp
        val reactiviteDays = String.format("%.2f", (diffMs.toDouble() / (1000.0 * 3600.0 * 24.0)).coerceAtLeast(0.1)).toDouble()

        val odds = dao.getOddsForLot(lotN)
        val frais = dao.getFraisForLot(lotN)
        val techniciens = dao.getAllTechniciens()
        val documents = dao.getDocumentsForLot(lotN)
        val directions = dao.getAllDirections()

        if (isApproved) {
            val nextStep = validations.getOrNull(currentStep.ordre)
            val isFinalDirection = nextStep == null || directionCode == "DAF"

            val updatedStep = currentStep.copy(
                valide = "VALIDE",
                dateValidation = now,
                commentaire = commentaire,
                signataire = signataire,
                historiqueVal = "Approuvé par $directionCode ($signataire)",
                ttalMission = lot.totalMission,
                ttalCoutOdd = lot.totalCoutOdd,
                ttalHebergement = lot.totalHebergement,
                ttalFrais = lot.totalFrais,
                reactiviteJours = reactiviteDays,
                updatedAt = now,
                isSynced = false
            )
            dao.updateValidation(updatedStep)

            val updatedLot = if (isFinalDirection) {
                // ARCHIVAGE FINAL DAF
                val arcId = "ARC-2026-${lotN.takeLast(4)}"
                val finalStep = updatedStep.copy(idArc = arcId, dateArchivage = now)
                dao.updateValidation(finalStep)

                lot.copy(
                    statutValidation = "ARCHIVE",
                    directionActuelle = "ARCHIVE",
                    updatedAt = now,
                    isSynced = false
                )
            } else {
                lot.copy(
                    statutValidation = "VALIDE_${directionCode}",
                    directionActuelle = nextStep.direction,
                    updatedAt = now,
                    isSynced = false
                )
            }
            dao.updateLot(updatedLot)
            syncManager.syncSingleLot(updatedLot)

            val freshValidations = dao.getValidationsForLot(lotN)
            val pdfFile = PdfReportGenerator.generateLotReportPdf(
                context = context,
                lot = updatedLot,
                odds = odds,
                fraisList = frais,
                validations = freshValidations,
                techniciens = techniciens,
                documents = documents,
                directions = directions
            )
            dao.updateLot(updatedLot.copy(versionPdfPath = pdfFile.absolutePath))

            val netTotal = (updatedLot.totalCoutOdd - odds.sumOf { it.avanceSurMission }).coerceAtLeast(0.0)

            if (!isFinalDirection && nextStep != null) {
                val nextDirEntity = dao.getDirection(nextStep.direction) ?: DirectionEntity(nextStep.direction, "Direction ${nextStep.direction}", "Directeur ${nextStep.direction}", "+216 71 855 100", "direction-${nextStep.direction.lowercase()}@tunisair-technics.tn", nextStep.ordre)
                return EmailNotifier.prepareEmailForDirection(
                    lot = updatedLot,
                    directionCode = nextStep.direction,
                    directionName = nextDirEntity.libelle,
                    directionEmail = nextDirEntity.email,
                    directorName = nextDirEntity.directeur,
                    directorPhone = nextDirEntity.telephone,
                    pdfFile = pdfFile,
                    oddsCount = odds.size,
                    netTotal = netTotal
                )
            } else {
                // Dossier finalisé par DAF : notification de clôture à la Direction Générale et DM
                val dgDir = dao.getDirection("DG") ?: DirectionEntity("DG", "Direction Générale", "M. Hedi Mansour", "+216 71 855 100", "dg-secretariat@tunisair-technics.tn", 6)
                return EmailNotifier.prepareEmailForDirection(
                    lot = updatedLot,
                    directionCode = "DG",
                    directionName = "Direction Générale (Clôture & Archivage DAF)",
                    directionEmail = dgDir.email,
                    directorName = dgDir.directeur,
                    directorPhone = dgDir.telephone,
                    pdfFile = pdfFile,
                    oddsCount = odds.size,
                    netTotal = netTotal
                )
            }
        } else {
            // Rejet du dossier
            val updatedStep = currentStep.copy(
                valide = "REFUSE",
                dateValidation = now,
                commentaire = commentaire,
                signataire = signataire,
                historiqueVal = "Rejeté par $directionCode : $commentaire",
                reactiviteJours = reactiviteDays,
                updatedAt = now,
                isSynced = false
            )
            dao.updateValidation(updatedStep)
            val updatedLot = lot.copy(
                statutValidation = "REFUSE_${directionCode}",
                updatedAt = now,
                isSynced = false
            )
            dao.updateLot(updatedLot)
            syncManager.syncSingleLot(updatedLot)

            val freshValidations = dao.getValidationsForLot(lotN)
            val pdfFile = PdfReportGenerator.generateLotReportPdf(
                context = context,
                lot = updatedLot,
                odds = odds,
                fraisList = frais,
                validations = freshValidations,
                techniciens = techniciens,
                documents = documents,
                directions = directions
            )
            val netTotal = (updatedLot.totalCoutOdd - odds.sumOf { it.avanceSurMission }).coerceAtLeast(0.0)
            val dmDir = dao.getDirection("DM") ?: DirectionEntity("DM", "Direction de la Maintenance", "Ing. Mehdi Ben Amor", "+216 71 855 101", "dm-validation@tunisair-technics.tn", 1)
            return EmailNotifier.prepareEmailForDirection(
                lot = updatedLot,
                directionCode = "DM",
                directionName = "Direction Maintenance (Avis de Refus / Régularisation)",
                directionEmail = dmDir.email,
                directorName = dmDir.directeur,
                directorPhone = dmDir.telephone,
                pdfFile = pdfFile,
                oddsCount = odds.size,
                netTotal = netTotal
            )
        }
    }

    suspend fun prepareNotificationForCurrentDirection(lotN: String): EmailNotificationData? {
        val lot = dao.getLotById(lotN) ?: return null
        val targetDir = if (lot.directionActuelle == "ARCHIVE") "DAF" else lot.directionActuelle
        val dirEntity = dao.getDirection(targetDir) ?: DirectionEntity(targetDir, "Direction $targetDir", "Directeur $targetDir", "+216 71 855 100", "direction-${targetDir.lowercase()}@tunisair-technics.tn", 1)
        val odds = dao.getOddsForLot(lotN)
        val frais = dao.getFraisForLot(lotN)
        val validations = dao.getValidationsForLot(lotN)
        val techniciens = dao.getAllTechniciens()
        val documents = dao.getDocumentsForLot(lotN)
        val directions = dao.getAllDirections()

        val pdfFile = PdfReportGenerator.generateLotReportPdf(
            context = context,
            lot = lot,
            odds = odds,
            fraisList = frais,
            validations = validations,
            techniciens = techniciens,
            documents = documents,
            directions = directions
        )

        val netTotal = (lot.totalCoutOdd - odds.sumOf { it.avanceSurMission }).coerceAtLeast(0.0)
        return EmailNotifier.prepareEmailForDirection(
            lot = lot,
            directionCode = targetDir,
            directionName = dirEntity.libelle,
            directionEmail = dirEntity.email,
            directorName = dirEntity.directeur,
            directorPhone = dirEntity.telephone,
            pdfFile = pdfFile,
            oddsCount = odds.size,
            netTotal = netTotal
        )
    }

    // --- REFERENTIELS OPERATIONS & UPDATES ---
    suspend fun saveTechnicien(technicien: TechnicienEntity) {
        dao.insertTechnicien(technicien)
    }

    suspend fun deleteTechnicien(technicien: TechnicienEntity) {
        dao.deleteTechnicien(technicien)
    }

    suspend fun resetTechniciensWorkload() {
        dao.resetTechniciensWorkload()
    }

    suspend fun updateTechniciens() {
        dao.migrateTechMatriculesToMat()
        dao.migrateOddMatriculesToMat()
        val techList = dao.getTechnicien("MAT-1042")
        if (techList == null) {
            val defaults = listOf(
                TechnicienEntity("MAT-1042", "M.", "Trabelsi", "Anis", "Tunis", "Technicien B1 A320/A330", 500, "TN59 0401 2345 6789 0123 4567", null, 42, 18),
                TechnicienEntity("MAT-2088", "M.", "Chouikh", "Tarek", "Monastir", "Mécanicien Cellule & Moteurs", 400, "TN59 1002 9876 5432 1098 7654", null, 35, 14),
                TechnicienEntity("MAT-3105", "Mme", "Ayadi", "Mariem", "Sfax", "Avionique B2 Systèmes de Bord", 600, "TN59 1403 4567 8901 2345 6789", null, 50, 22),
                TechnicienEntity("MAT-4019", "M.", "Bouzid", "Walid", "Djerba", "Inspecteur CND / Qualité", 700, "TN59 0804 1122 3344 5566 7788", null, 28, 11),
                TechnicienEntity("MAT-5022", "M.", "Guesmi", "Sami", "Tozeur", "Technicien Piste & Escales", 300, "TN59 0705 9988 7766 5544 3322", null, 19, 9),
                TechnicienEntity("MAT-6091", "M.", "Ben Salem", "Yassine", "Tunis", "Ingénieur Support Opérations", 800, "TN59 0206 5544 3322 1100 9988", null, 64, 27)
            )
            dao.insertTechniciens(defaults)
        }
    }

    suspend fun saveDirection(direction: DirectionEntity) {
        dao.insertDirection(direction)
        syncManager.syncDirection(direction)
    }

    suspend fun deleteDirection(direction: DirectionEntity) {
        dao.deleteDirection(direction)
    }

    suspend fun ensureOfficialDirections() {
        val existing = dao.getAllDirections()
        if (existing.isEmpty()) {
            val directions = listOf(
                DirectionEntity("DM", "Direction de la Maintenance", "Ing. Mehdi Ben Amor", "+216 71 855 101", "dm-validation@tunisair-technics.tn", 1),
                DirectionEntity("DCT", "Direction du Contrôle Technique", "Mme Salma Kallel", "+216 71 855 102", "dct-validation@tunisair-technics.tn", 2),
                DirectionEntity("DGRT", "Direction de Gestion des Ressources Techniques", "M. Karim Dridi", "+216 71 855 103", "dgrt-direction@tunisair-technics.tn", 3),
                DirectionEntity("DCST", "Direction de la Coordination et du Support Technique", "Cdt. Nabil Trabelsi", "+216 71 855 104", "dcst-securite@tunisair-technics.tn", 4),
                DirectionEntity("AUDIT", "Direction de l'Audit et de la Qualité", "Mme Fatma Bouazizi", "+216 71 855 105", "audit-qualite@tunisair-technics.tn", 5),
                DirectionEntity("DG", "Direction Générale", "M. Hedi Mansour", "+216 71 855 100", "dg-secretariat@tunisair-technics.tn", 6),
                DirectionEntity("DAF", "Direction Administrative et Financière", "Mme Sonia Ghariani", "+216 71 855 107", "daf-comptabilite@tunisair-technics.tn", 7)
            )
            dao.insertDirections(directions)
        }
    }

    suspend fun updateDirections() {
        ensureOfficialDirections()
    }

    suspend fun saveBareme(bareme: BaremeTarifEntity) {
        dao.insertBareme(bareme)
    }

    suspend fun deleteBareme(bareme: BaremeTarifEntity) {
        dao.deleteBareme(bareme)
    }

    suspend fun updateBaremes() {
        // 1. Remplacer 'Coût Journalier Mission' par 'Coût Unitaire Mission par Type de Mission'
        val allBaremes = dao.getAllBaremesSync()
        allBaremes.forEach { b ->
            if (b.designation.contains("Coût Journalier Mission", ignoreCase = true)) {
                val updatedDesignation = b.designation.replace("Coût Journalier Mission", "Coût Unitaire Mission par Type de Mission", ignoreCase = true)
                dao.updateBareme(b.copy(designation = updatedDesignation))
            }
        }
    }

    suspend fun saveCoutMission(cout: CoutMissionEntity) {
        dao.insertCoutMission(cout)
        val lots = dao.getUnsyncedLots()
    }

    suspend fun deleteCoutMission(cout: CoutMissionEntity) {
        dao.deleteCoutMission(cout)
    }

    suspend fun purgeAogMissions() {
        dao.deleteAogCoutsMission()
        dao.migrateTechMatriculesToMat()
        dao.migrateOddMatriculesToMat()
    }

    suspend fun updateCoutsMission() {
        dao.deleteAogCoutsMission()
        val couts = dao.getAllCoutsMissionSync()
        if (couts.isEmpty()) {
            val defaults = listOf(
                CoutMissionEntity(typeMission = "Visite Programmée", echelleMin = 1, echelleMax = 5, coutUnitaireJournalier = 55.0, observation = "Site visite A/C en hangar"),
                CoutMissionEntity(typeMission = "Visite Programmée", echelleMin = 6, echelleMax = 10, coutUnitaireJournalier = 70.0, observation = "Site visite technicien B1/B2"),
                CoutMissionEntity(typeMission = "Visite Programmée", echelleMin = 11, echelleMax = 15, coutUnitaireJournalier = 85.0, observation = "Supervision site / Avionique"),
                CoutMissionEntity(typeMission = "Visite Programmée", echelleMin = 16, echelleMax = 20, coutUnitaireJournalier = 105.0, observation = "Responsable de visite programmée"),
                CoutMissionEntity(typeMission = "Intervention sur avion", echelleMin = 1, echelleMax = 5, coutUnitaireJournalier = 60.0, observation = "Opérations d'entretien en ligne"),
                CoutMissionEntity(typeMission = "Intervention sur avion", echelleMin = 6, echelleMax = 10, coutUnitaireJournalier = 80.0, observation = "Remplacement d'équipements LRU"),
                CoutMissionEntity(typeMission = "Intervention sur avion", echelleMin = 11, echelleMax = 15, coutUnitaireJournalier = 100.0, observation = "Intervention systèmes critiques"),
                CoutMissionEntity(typeMission = "Intervention sur avion", echelleMin = 16, echelleMax = 20, coutUnitaireJournalier = 125.0, observation = "Validation technique et remise en service"),
                CoutMissionEntity(typeMission = "Assistance technique", echelleMin = 1, echelleMax = 5, coutUnitaireJournalier = 50.0, observation = "Assistance escale province"),
                CoutMissionEntity(typeMission = "Assistance technique", echelleMin = 6, echelleMax = 10, coutUnitaireJournalier = 65.0, observation = "Assistance escale technicien confirmé"),
                CoutMissionEntity(typeMission = "Assistance technique", echelleMin = 11, echelleMax = 15, coutUnitaireJournalier = 80.0, observation = "Assistance technique senior"),
                CoutMissionEntity(typeMission = "Assistance technique", echelleMin = 16, echelleMax = 20, coutUnitaireJournalier = 95.0, observation = "Coordination technique escale"),
                CoutMissionEntity(typeMission = "Renfort hangar", echelleMin = 1, echelleMax = 5, coutUnitaireJournalier = 45.0, observation = "Renfort équipe de révision"),
                CoutMissionEntity(typeMission = "Renfort hangar", echelleMin = 6, echelleMax = 10, coutUnitaireJournalier = 60.0, observation = "Renfort spécialisé cellules/moteurs"),
                CoutMissionEntity(typeMission = "Renfort hangar", echelleMin = 11, echelleMax = 15, coutUnitaireJournalier = 75.0, observation = "Renfort encadrement technique"),
                CoutMissionEntity(typeMission = "Renfort hangar", echelleMin = 16, echelleMax = 20, coutUnitaireJournalier = 90.0, observation = "Maîtrise d'œuvre hangar"),
                CoutMissionEntity(typeMission = "Formation", echelleMin = 1, echelleMax = 5, coutUnitaireJournalier = 35.0, observation = "Stage ou qualification type"),
                CoutMissionEntity(typeMission = "Formation", echelleMin = 6, echelleMax = 10, coutUnitaireJournalier = 45.0, observation = "Recyclage réglementaire PART-145"),
                CoutMissionEntity(typeMission = "Formation", echelleMin = 11, echelleMax = 15, coutUnitaireJournalier = 60.0, observation = "Formation spécialisée / Formateur"),
                CoutMissionEntity(typeMission = "Formation", echelleMin = 16, echelleMax = 20, coutUnitaireJournalier = 75.0, observation = "Audit de formation / Instructeur"),
                CoutMissionEntity(typeMission = "Transport", echelleMin = 1, echelleMax = 5, coutUnitaireJournalier = 30.0, observation = "Acheminement pièces et outillages"),
                CoutMissionEntity(typeMission = "Transport", echelleMin = 6, echelleMax = 10, coutUnitaireJournalier = 40.0, observation = "Convoyage logistique"),
                CoutMissionEntity(typeMission = "Transport", echelleMin = 11, echelleMax = 15, coutUnitaireJournalier = 50.0, observation = "Mission logistique urgente"),
                CoutMissionEntity(typeMission = "Transport", echelleMin = 16, echelleMax = 20, coutUnitaireJournalier = 65.0, observation = "Supervision transport outillage lourd")
            )
            dao.insertCoutsMission(defaults)
        }
    }

    suspend fun resetReferentielsToDefaults() {
        dao.clearTechniciens()
        dao.clearDirections()
        dao.clearBaremes()
        dao.clearCoutsMission()
        ZoddDatabase.populateInitialData(dao)
    }

    suspend fun generateDefinitiveLotReportPdf(lotN: String): File? {
        val lot = dao.getLotById(lotN) ?: return null
        val odds = dao.getOddsForLot(lotN)
        val frais = dao.getFraisForLot(lotN)
        val validations = dao.getValidationsForLot(lotN)
        val techniciens = dao.getAllTechniciens()
        val documents = dao.getDocumentsForLot(lotN)
        val directions = dao.getAllDirections()

        val pdfFile = PdfReportGenerator.generateLotReportPdf(
            context = context,
            lot = lot,
            odds = odds,
            fraisList = frais,
            validations = validations,
            techniciens = techniciens,
            documents = documents,
            directions = directions
        )
        val updatedLot = lot.copy(versionPdfPath = pdfFile.absolutePath, updatedAt = System.currentTimeMillis())
        dao.updateLot(updatedLot)
        return pdfFile
    }

    suspend fun downloadOrOpenLotReportPdf(context: Context, lotN: String): File? {
        val lot = dao.getLotById(lotN) ?: return null
        val odds = dao.getOddsForLot(lotN)
        val frais = dao.getFraisForLot(lotN)
        val validations = dao.getValidationsForLot(lotN)
        val techniciens = dao.getAllTechniciens()
        val documents = dao.getDocumentsForLot(lotN)
        val directions = dao.getAllDirections()

        val file = PdfReportGenerator.downloadOrOpenLotReportPdf(
            context = context,
            lot = lot,
            odds = odds,
            fraisList = frais,
            validations = validations,
            techniciens = techniciens,
            documents = documents,
            directions = directions
        )
        val updatedLot = lot.copy(versionPdfPath = file.absolutePath, updatedAt = System.currentTimeMillis())
        dao.updateLot(updatedLot)
        return file
    }

    suspend fun prepareValidationEmailForLot(lotN: String, targetDirCode: String? = null): EmailNotificationData? {
        val lot = dao.getLotById(lotN) ?: return null
        val odds = dao.getOddsForLot(lotN)
        val frais = dao.getFraisForLot(lotN)
        val validations = dao.getValidationsForLot(lotN)
        val techniciens = dao.getAllTechniciens()
        val documents = dao.getDocumentsForLot(lotN)
        val directions = dao.getAllDirections()

        // Generate fresh definitive PDF report
        val pdfFile = PdfReportGenerator.generateLotReportPdf(
            context = context,
            lot = lot,
            odds = odds,
            fraisList = frais,
            validations = validations,
            techniciens = techniciens,
            documents = documents,
            directions = directions
        )
        dao.updateLot(lot.copy(versionPdfPath = pdfFile.absolutePath))

        val targetCode = targetDirCode ?: if (lot.directionActuelle != "ARCHIVE") lot.directionActuelle else "DM"
        val dirEntity = dao.getDirection(targetCode) ?: directions.firstOrNull { it.code == targetCode } ?: dao.getDirection("DM")
        if (dirEntity == null) return null

        val netTotal = (lot.totalCoutOdd - odds.sumOf { it.avanceSurMission }).coerceAtLeast(0.0)

        return EmailNotifier.prepareEmailForDirection(
            lot = lot,
            directionCode = dirEntity.code,
            directionName = dirEntity.libelle,
            directionEmail = dirEntity.email,
            directorName = dirEntity.directeur,
            directorPhone = dirEntity.telephone,
            pdfFile = pdfFile,
            oddsCount = odds.size,
            netTotal = netTotal
        )
    }

    suspend fun syncWithFirestore(): Result<Int> {
        return syncManager.syncAll()
    }
}
