package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        LotEntity::class,
        OddEntity::class,
        FraisEntity::class,
        DocumentRattachementEntity::class,
        ValidationEntity::class,
        TechnicienEntity::class,
        DirectionEntity::class,
        BaremeTarifEntity::class,
        CoutMissionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ZoddDatabase : RoomDatabase() {
    abstract fun zoddDao(): ZoddDao

    companion object {
        @Volatile
        private var INSTANCE: ZoddDatabase? = null

        fun getDatabase(context: Context): ZoddDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZoddDatabase::class.java,
                    "zodd_v2_26.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(getDatabase(context).zoddDao())
                }
            }
        }

        suspend fun populateInitialData(dao: ZoddDao) {
            // Seed Directions: DM, DCT, DGRT, DCST, AUDIT, DG, DAF avec dénominations exactes
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

            // Seed Aviation Technicians with realistic scale, site, and specialties
            val techniciens = listOf(
                TechnicienEntity("MAT-1042", "M.", "Trabelsi", "Anis", "Tunis", "Technicien B1 A320/A330", 500, "TN59 0401 2345 6789 0123 4567", null, 42, 18),
                TechnicienEntity("MAT-2088", "M.", "Chouikh", "Tarek", "Monastir", "Mécanicien Cellule & Moteurs", 400, "TN59 1002 9876 5432 1098 7654", null, 35, 14),
                TechnicienEntity("MAT-3105", "Mme", "Ayadi", "Mariem", "Sfax", "Avionique B2 Systèmes de Bord", 600, "TN59 1403 4567 8901 2345 6789", null, 50, 22),
                TechnicienEntity("MAT-4019", "M.", "Bouzid", "Walid", "Djerba", "Inspecteur CND / Qualité", 700, "TN59 0804 1122 3344 5566 7788", null, 28, 11),
                TechnicienEntity("MAT-5022", "M.", "Guesmi", "Sami", "Tozeur", "Technicien Piste & Escales", 300, "TN59 0705 9988 7766 5544 3322", null, 19, 9),
                TechnicienEntity("MAT-6091", "M.", "Ben Salem", "Yassine", "Tunis", "Ingénieur Support Opérations", 800, "TN59 0206 5544 3322 1100 9988", null, 64, 27)
            )
            dao.insertTechniciens(techniciens)

            // Seed standard tariff barèmes (incluant Coûts Unitaires de Mission par Type de Mission)
            val baremes = listOf(
                BaremeTarifEntity(categorie = "Mission", designation = "Coût Unitaire Mission par Type de Mission (Échelle 100-300)", echelleMin = 100, echelleMax = 300, unite = "Jour", tarifUnitaire = 50.0),
                BaremeTarifEntity(categorie = "Mission", designation = "Coût Unitaire Mission par Type de Mission (Échelle 400-600)", echelleMin = 400, echelleMax = 600, unite = "Jour", tarifUnitaire = 75.0),
                BaremeTarifEntity(categorie = "Mission", designation = "Coût Unitaire Mission par Type de Mission (Échelle 700-900)", echelleMin = 700, echelleMax = 900, unite = "Jour", tarifUnitaire = 100.0),
                BaremeTarifEntity(categorie = "Hébergement", designation = "LPD (Échelle 100-300)", echelleMin = 100, echelleMax = 300, unite = "Nuité", tarifUnitaire = 80.0),
                BaremeTarifEntity(categorie = "Hébergement", designation = "1/2 Pension (Échelle 100-300)", echelleMin = 100, echelleMax = 300, unite = "Nuité", tarifUnitaire = 110.0),
                BaremeTarifEntity(categorie = "Hébergement", designation = "Pension complète (Échelle 100-300)", echelleMin = 100, echelleMax = 300, unite = "Nuité", tarifUnitaire = 140.0),
                BaremeTarifEntity(categorie = "Hébergement", designation = "LPD (Échelle 400-600)", echelleMin = 400, echelleMax = 600, unite = "Nuité", tarifUnitaire = 110.0),
                BaremeTarifEntity(categorie = "Hébergement", designation = "1/2 Pension (Échelle 400-600)", echelleMin = 400, echelleMax = 600, unite = "Nuité", tarifUnitaire = 140.0),
                BaremeTarifEntity(categorie = "Hébergement", designation = "Pension complète (Échelle 400-600)", echelleMin = 400, echelleMax = 600, unite = "Nuité", tarifUnitaire = 180.0),
                BaremeTarifEntity(categorie = "Hébergement", designation = "LPD (Échelle 700-900)", echelleMin = 700, echelleMax = 900, unite = "Nuité", tarifUnitaire = 140.0),
                BaremeTarifEntity(categorie = "Hébergement", designation = "1/2 Pension (Échelle 700-900)", echelleMin = 700, echelleMax = 900, unite = "Nuité", tarifUnitaire = 180.0),
                BaremeTarifEntity(categorie = "Hébergement", designation = "Pension complète (Échelle 700-900)", echelleMin = 700, echelleMax = 900, unite = "Nuité", tarifUnitaire = 230.0),
                BaremeTarifEntity(categorie = "Transport", designation = "Indemnité Kilométrique (Voiture perso)", echelleMin = 100, echelleMax = 900, unite = "Taux/Km", tarifUnitaire = 0.450),
                BaremeTarifEntity(categorie = "Transport", designation = "Forfait Inter-sites Tunis <-> Monastir (165km)", echelleMin = 100, echelleMax = 900, unite = "FF", tarifUnitaire = 74.250),
                BaremeTarifEntity(categorie = "Transport", designation = "Forfait Inter-sites Tunis <-> Sfax (270km)", echelleMin = 100, echelleMax = 900, unite = "FF", tarifUnitaire = 121.500),
                BaremeTarifEntity(categorie = "Transport", designation = "Forfait Inter-sites Monastir <-> Sfax (135km)", echelleMin = 100, echelleMax = 900, unite = "FF", tarifUnitaire = 60.750),
                BaremeTarifEntity(categorie = "Transport", designation = "Forfait Inter-sites Tunis <-> Djerba (500km)", echelleMin = 100, echelleMax = 900, unite = "FF", tarifUnitaire = 225.000),
                BaremeTarifEntity(categorie = "Divers", designation = "Indemnité Repas Journalier", echelleMin = 100, echelleMax = 900, unite = "Repas", tarifUnitaire = 25.000),
                BaremeTarifEntity(categorie = "Divers", designation = "Indemnité Spécifique Mission Hangar", echelleMin = 100, echelleMax = 900, unite = "FF", tarifUnitaire = 50.000),
                BaremeTarifEntity(categorie = "Divers", designation = "Péage Autoroute Inter-sites", echelleMin = 100, echelleMax = 900, unite = "FF", tarifUnitaire = 12.000)
            )
            dao.insertBaremes(baremes)

            // Seed Coûts Unitaires Mission par Type de Mission et par Échelle
            val coutsMissionInitiaux = listOf(
                // Visite Programmée / Site
                CoutMissionEntity(typeMission = "Visite Programmée", echelleMin = 1, echelleMax = 5, coutUnitaireJournalier = 55.0, observation = "Site visite A/C en hangar"),
                CoutMissionEntity(typeMission = "Visite Programmée", echelleMin = 6, echelleMax = 10, coutUnitaireJournalier = 70.0, observation = "Site visite technicien B1/B2"),
                CoutMissionEntity(typeMission = "Visite Programmée", echelleMin = 11, echelleMax = 15, coutUnitaireJournalier = 85.0, observation = "Supervision site / Avionique"),
                CoutMissionEntity(typeMission = "Visite Programmée", echelleMin = 16, echelleMax = 20, coutUnitaireJournalier = 105.0, observation = "Responsable de visite programmée"),

                // Intervention sur avion
                CoutMissionEntity(typeMission = "Intervention sur avion", echelleMin = 1, echelleMax = 5, coutUnitaireJournalier = 60.0, observation = "Opérations d'entretien en ligne"),
                CoutMissionEntity(typeMission = "Intervention sur avion", echelleMin = 6, echelleMax = 10, coutUnitaireJournalier = 80.0, observation = "Remplacement d'équipements LRU"),
                CoutMissionEntity(typeMission = "Intervention sur avion", echelleMin = 11, echelleMax = 15, coutUnitaireJournalier = 100.0, observation = "Intervention systèmes critiques"),
                CoutMissionEntity(typeMission = "Intervention sur avion", echelleMin = 16, echelleMax = 20, coutUnitaireJournalier = 125.0, observation = "Validation technique et remise en service"),

                // Assistance technique & escale
                CoutMissionEntity(typeMission = "Assistance technique", echelleMin = 1, echelleMax = 5, coutUnitaireJournalier = 50.0, observation = "Assistance escale province"),
                CoutMissionEntity(typeMission = "Assistance technique", echelleMin = 6, echelleMax = 10, coutUnitaireJournalier = 65.0, observation = "Assistance escale technicien confirmé"),
                CoutMissionEntity(typeMission = "Assistance technique", echelleMin = 11, echelleMax = 15, coutUnitaireJournalier = 80.0, observation = "Assistance technique senior"),
                CoutMissionEntity(typeMission = "Assistance technique", echelleMin = 16, echelleMax = 20, coutUnitaireJournalier = 95.0, observation = "Coordination technique escale"),

                // Renfort hangar
                CoutMissionEntity(typeMission = "Renfort hangar", echelleMin = 1, echelleMax = 5, coutUnitaireJournalier = 45.0, observation = "Renfort équipe de révision"),
                CoutMissionEntity(typeMission = "Renfort hangar", echelleMin = 6, echelleMax = 10, coutUnitaireJournalier = 60.0, observation = "Renfort spécialisé cellules/moteurs"),
                CoutMissionEntity(typeMission = "Renfort hangar", echelleMin = 11, echelleMax = 15, coutUnitaireJournalier = 75.0, observation = "Renfort encadrement technique"),
                CoutMissionEntity(typeMission = "Renfort hangar", echelleMin = 16, echelleMax = 20, coutUnitaireJournalier = 90.0, observation = "Maîtrise d'œuvre hangar"),

                // Formation
                CoutMissionEntity(typeMission = "Formation", echelleMin = 1, echelleMax = 5, coutUnitaireJournalier = 35.0, observation = "Stage ou qualification type"),
                CoutMissionEntity(typeMission = "Formation", echelleMin = 6, echelleMax = 10, coutUnitaireJournalier = 45.0, observation = "Recyclage réglementaire PART-145"),
                CoutMissionEntity(typeMission = "Formation", echelleMin = 11, echelleMax = 15, coutUnitaireJournalier = 60.0, observation = "Formation spécialisée / Formateur"),
                CoutMissionEntity(typeMission = "Formation", echelleMin = 16, echelleMax = 20, coutUnitaireJournalier = 75.0, observation = "Audit de formation / Instructeur"),

                // Transport
                CoutMissionEntity(typeMission = "Transport", echelleMin = 1, echelleMax = 5, coutUnitaireJournalier = 30.0, observation = "Acheminement pièces et outillages"),
                CoutMissionEntity(typeMission = "Transport", echelleMin = 6, echelleMax = 10, coutUnitaireJournalier = 40.0, observation = "Convoyage logistique"),
                CoutMissionEntity(typeMission = "Transport", echelleMin = 11, echelleMax = 15, coutUnitaireJournalier = 50.0, observation = "Mission logistique urgente"),
                CoutMissionEntity(typeMission = "Transport", echelleMin = 16, echelleMax = 20, coutUnitaireJournalier = 65.0, observation = "Supervision transport outillage lourd")
            )
            dao.insertCoutsMission(coutsMissionInitiaux)

            // Seed initial realistic demonstration lot: TAT-0023-26 (Non figé, validations annulées)
            val now = System.currentTimeMillis()
            val lot1 = LotEntity(
                lotN = "TAT-0023-26",
                libelle = "Maintenance Escale & Renfort A320 - Djerba / Monastir",
                dateCreation = now - (3 * 86400000L),
                basculementVues = "LES ODD",
                figerLot = false,
                statutValidation = "OUVERT",
                directionActuelle = "DM",
                totalMission = 3.0,
                totalCoutOdd = 1245.500,
                totalHebergement = 680.000,
                totalTransport = 445.500,
                totalFrais = 120.000,
                isSynced = true
            )
            dao.insertLot(lot1)

            val validationsLot1 = listOf(
                ValidationEntity(idVal = "VAL-0023-DM", lotN = "TAT-0023-26", direction = "DM", ordre = 1, valide = "EN_ATTENTE", isSynced = true),
                ValidationEntity(idVal = "VAL-0023-DCT", lotN = "TAT-0023-26", direction = "DCT", ordre = 2, valide = "EN_ATTENTE", isSynced = true),
                ValidationEntity(idVal = "VAL-0023-DGRT", lotN = "TAT-0023-26", direction = "DGRT", ordre = 3, valide = "EN_ATTENTE", isSynced = true),
                ValidationEntity(idVal = "VAL-0023-DCST", lotN = "TAT-0023-26", direction = "DCST", ordre = 4, valide = "EN_ATTENTE", isSynced = true),
                ValidationEntity(idVal = "VAL-0023-AUDIT", lotN = "TAT-0023-26", direction = "AUDIT", ordre = 5, valide = "EN_ATTENTE", isSynced = true),
                ValidationEntity(idVal = "VAL-0023-DG", lotN = "TAT-0023-26", direction = "DG", ordre = 6, valide = "EN_ATTENTE", isSynced = true),
                ValidationEntity(idVal = "VAL-0023-DAF", lotN = "TAT-0023-26", direction = "DAF", ordre = 7, valide = "EN_ATTENTE", isSynced = true)
            )
            dao.insertValidations(validationsLot1)

            val odd1 = OddEntity(
                oddN = "DJE-0005-26",
                lotN = "TAT-0023-26",
                matricule = "MAT-1042",
                siteProvenance = "Tunis",
                echelle = 500,
                siteIntervention = "Djerba",
                dateDebut = now - (3 * 86400000L),
                dateFin = now - (1 * 86400000L),
                mission = "Intervention sur avion",
                modeEdit = false,
                avanceSurMission = 100.0,
                totalOdd = 685.000,
                isSynced = true
            )
            val odd2 = OddEntity(
                oddN = "MIR-0012-26",
                lotN = "TAT-0023-26",
                matricule = "MAT-2088",
                siteProvenance = "Monastir",
                echelle = 400,
                siteIntervention = "Sfax",
                dateDebut = now - (2 * 86400000L),
                dateFin = now - (1 * 86400000L),
                mission = "Renfort hangar",
                modeEdit = false,
                avanceSurMission = 0.0,
                totalOdd = 460.500,
                isSynced = true
            )
            dao.insertOdd(odd1)
            dao.insertOdd(odd2)

            val frais1 = FraisEntity(
                idFrais = "FR-001-26",
                oddN = "DJE-0005-26",
                date = now - (3 * 86400000L),
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
            val frais2 = FraisEntity(
                idFrais = "FR-002-26",
                oddN = "DJE-0005-26",
                date = now - (3 * 86400000L),
                categorie = "Transport",
                sousCategorie = "Voiture personnelle",
                unite = "Taux/Km",
                quantite = 500.0,
                pUnit = 0.450,
                montant = 225.0,
                aComptabiliser = 225.0,
                estComptabilise = true,
                isSynced = true
            )
            val frais3 = FraisEntity(
                idFrais = "FR-003-26",
                oddN = "DJE-0005-26",
                date = now - (2 * 86400000L),
                categorie = "Divers",
                sousCategorie = "Repas",
                unite = "Repas",
                quantite = 2.0,
                pUnit = 25.0,
                montant = 50.0,
                aComptabiliser = 50.0,
                isSynced = true
            )
            val frais4 = FraisEntity(
                idFrais = "FR-004-26",
                oddN = "MIR-0012-26",
                date = now - (2 * 86400000L),
                categorie = "Transport",
                sousCategorie = "Escale",
                unite = "FF",
                quantite = 1.0,
                pUnit = 60.750,
                montant = 60.750,
                aComptabiliser = 60.750,
                isSynced = true
            )
            dao.insertFrais(frais1)
            dao.insertFrais(frais2)
            dao.insertFrais(frais3)
            dao.insertFrais(frais4)

            // Seed demo lot 2 (Already frozen and partially through validation circuit)
            val lot2 = LotEntity(
                lotN = "TAT-0019-26",
                libelle = "Visite Licence & Formation Avionique - Sfax",
                dateCreation = now - (10 * 86400000L),
                basculementVues = "VALIDATION",
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
            dao.insertLot(lot2)

            // Seed validation steps for lot2
            val validationSteps = listOf(
                ValidationEntity(
                    idVal = "VAL-0019-DM",
                    lotN = "TAT-0019-26",
                    direction = "DM",
                    ordre = 1,
                    valide = "VALIDE",
                    dateValidation = now - (8 * 86400000L),
                    commentaire = "Mission conforme au planning technique d'intervention.",
                    signataire = "Ing. Mehdi Ben Amor",
                    historiqueVal = "Approuvé par Direction Maintenance",
                    ttalMission = 2.0,
                    ttalCoutOdd = 920.0,
                    ttalHebergement = 520.0,
                    ttalFrais = 400.0,
                    reactiviteJours = 1.2,
                    emailEnvoye = true,
                    isSynced = true
                ),
                ValidationEntity(
                    idVal = "VAL-0019-DCT",
                    lotN = "TAT-0019-26",
                    direction = "DCT",
                    ordre = 2,
                    valide = "VALIDE",
                    dateValidation = now - (6 * 86400000L),
                    commentaire = "Contrôles avionique et visas conformes aux normes PART-66/145.",
                    signataire = "Mme Salma Kallel",
                    historiqueVal = "Approuvé par Direction Contrôle Technique",
                    ttalMission = 2.0,
                    ttalCoutOdd = 920.0,
                    ttalHebergement = 520.0,
                    ttalFrais = 400.0,
                    reactiviteJours = 2.0,
                    emailEnvoye = true,
                    isSynced = true
                ),
                ValidationEntity(
                    idVal = "VAL-0019-DGRT",
                    lotN = "TAT-0019-26",
                    direction = "DGRT",
                    ordre = 3,
                    valide = "EN_ATTENTE",
                    dateValidation = null,
                    commentaire = "",
                    signataire = "",
                    historiqueVal = "Dossier transmis pour vérification des temps de repos et vacations",
                    ttalMission = 2.0,
                    ttalCoutOdd = 920.0,
                    ttalHebergement = 520.0,
                    ttalFrais = 400.0,
                    reactiviteJours = 0.0,
                    emailEnvoye = true,
                    isSynced = true
                ),
                ValidationEntity(idVal = "VAL-0019-DCST", lotN = "TAT-0019-26", direction = "DCST", ordre = 4, valide = "EN_ATTENTE", isSynced = true),
                ValidationEntity(idVal = "VAL-0019-AUDIT", lotN = "TAT-0019-26", direction = "AUDIT", ordre = 5, valide = "EN_ATTENTE", isSynced = true),
                ValidationEntity(idVal = "VAL-0019-DG", lotN = "TAT-0019-26", direction = "DG", ordre = 6, valide = "EN_ATTENTE", isSynced = true),
                ValidationEntity(idVal = "VAL-0019-DAF", lotN = "TAT-0019-26", direction = "DAF", ordre = 7, valide = "EN_ATTENTE", isSynced = true)
            )
            dao.insertValidations(validationSteps)

            // Seed ODDs, Frais, and Documents for TAT-0019-26
            val oddSfx19 = OddEntity(
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
            val oddMir19 = OddEntity(
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
            dao.insertOdd(oddSfx19)
            dao.insertOdd(oddMir19)

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

            // Seed Lot DJE-0005-26 (Lot figé, seule la validation DM est gardée, toutes les autres annulées)
            val lotDje = LotEntity(
                lotN = "DJE-0005-26",
                libelle = "Assistance Escale & Renfort Ligne - Djerba Zarzis",
                dateCreation = now - (6 * 86400000L),
                basculementVues = "VALIDATION",
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

            val validationsDje = listOf(
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
                ),
                ValidationEntity(idVal = "VAL-DJE-DCT", lotN = "DJE-0005-26", direction = "DCT", ordre = 2, valide = "EN_ATTENTE", isSynced = true),
                ValidationEntity(idVal = "VAL-DJE-DGRT", lotN = "DJE-0005-26", direction = "DGRT", ordre = 3, valide = "EN_ATTENTE", isSynced = true),
                ValidationEntity(idVal = "VAL-DJE-DCST", lotN = "DJE-0005-26", direction = "DCST", ordre = 4, valide = "EN_ATTENTE", isSynced = true),
                ValidationEntity(idVal = "VAL-DJE-AUDIT", lotN = "DJE-0005-26", direction = "AUDIT", ordre = 5, valide = "EN_ATTENTE", isSynced = true),
                ValidationEntity(idVal = "VAL-DJE-DG", lotN = "DJE-0005-26", direction = "DG", ordre = 6, valide = "EN_ATTENTE", isSynced = true),
                ValidationEntity(idVal = "VAL-DJE-DAF", lotN = "DJE-0005-26", direction = "DAF", ordre = 7, valide = "EN_ATTENTE", isSynced = true)
            )
            dao.insertValidations(validationsDje)

            // Seed ODD, Frais, and Documents for DJE-0005-26
            val oddDje42 = OddEntity(
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
            dao.insertOdd(oddDje42)

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
    }
}
