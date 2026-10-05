package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lots")
data class LotEntity(
    @PrimaryKey
    val lotN: String, // ex: TAT-0023-26
    val libelle: String,
    val dateCreation: Long,
    val basculementVues: String = "LES ODD",
    val figerLot: Boolean = false,
    val versionPdfPath: String? = null,
    val statutValidation: String = "EN_COURS", // EN_COURS, FIGE, VALIDE_DM, VALIDE_DCT, VALIDE_DGRT, VALIDE_DCST, VALIDE_AUDIT, VALIDE_DG, ARCHIVE, REFUSE
    val directionActuelle: String = "DM", // DM, DCT, DGRT, DCST, AUDIT, DG, DAF, ARCHIVE
    val totalMission: Double = 0.0,
    val totalCoutOdd: Double = 0.0,
    val totalHebergement: Double = 0.0,
    val totalTransport: Double = 0.0,
    val totalFrais: Double = 0.0,
    val emailDmEnvoye: Boolean = false,
    val detailDivers: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "odds")
data class OddEntity(
    @PrimaryKey
    val oddN: String, // ex: DJE-0005-26
    val lotN: String,
    val matricule: String,
    val siteProvenance: String, // Tunis, Monastir, Sfax, Djerba, Tozeur
    val echelle: Int, // 100 to 900
    val siteIntervention: String,
    val dateDebut: Long,
    val dateFin: Long,
    val mission: String, // Assistance technique, Renfort hangar, Visite médicale, Formation, Visite licence, Intervention sur avion, Transport
    val modeEdit: Boolean = false,
    val totalOdd: Double = 0.0,
    val avanceSurMission: Double = 0.0, // Avance sur mission à déduire du montant total de la mission
    val detailDivers: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "frais")
data class FraisEntity(
    @PrimaryKey
    val idFrais: String,
    val oddN: String,
    val date: Long,
    val categorie: String, // Hébergement, Transport, Divers
    val sousCategorie: String, // LPD, 1/2 Pension, Pension complète / Voiture personnelle, Transport en commun, Escale / Repas, Autoroute, Indemnité spécifique
    val unite: String, // Nuité, Taux/Km, Repas, FF
    val quantite: Double,
    val pUnit: Double,
    val montant: Double,
    val aComptabiliser: Double,
    val estComptabilise: Boolean = true, // Possibilité de comptabiliser ou non chaque frais
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "documents_rattachement")
data class DocumentRattachementEntity(
    @PrimaryKey
    val idDoc: String,
    val oddN: String,
    val typeDocument: String, // Facture hôtel, Ticket péage, Billet transport, Reçu repas, Ordre mission, Autre
    val objet: String,
    val dateEnvoie: Long,
    val scanDocUri: String?,
    val fileSizeBytes: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "validations")
data class ValidationEntity(
    @PrimaryKey
    val idVal: String,
    val lotN: String,
    val direction: String, // DM, DCT, DGRT, DCST, AUDIT, DG, DAF
    val ordre: Int,
    val valide: String, // EN_ATTENTE, VALIDE, REFUSE
    val dateValidation: Long? = null,
    val commentaire: String = "",
    val signataire: String = "",
    val historiqueVal: String = "",
    val idArc: String? = null,
    val dateArchivage: Long? = null,
    val ttalMission: Double = 0.0,
    val ttalCoutOdd: Double = 0.0,
    val ttalHebergement: Double = 0.0,
    val ttalFrais: Double = 0.0,
    val reactiviteJours: Double = 0.0,
    val emailEnvoye: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "techniciens")
data class TechnicienEntity(
    @PrimaryKey
    val matricule: String,
    val civilite: String,
    val nom: String,
    val prenom: String,
    val siteProvenance: String,
    val fonction: String,
    val echelle: Int,
    val rib: String,
    val photo: String? = null,
    val histJours: Int = 0,
    val histMissions: Int = 0,
    val histCout: Double = 0.0
)

@Entity(tableName = "directions")
data class DirectionEntity(
    @PrimaryKey
    val code: String, // DM, DCT, DGRT, DCST, AUDIT, DG, DAF
    val libelle: String,
    val directeur: String,
    val telephone: String,
    val email: String,
    val ordre: Int
)

@Entity(tableName = "baremes")
data class BaremeTarifEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categorie: String,
    val designation: String,
    val echelleMin: Int,
    val echelleMax: Int,
    val unite: String,
    val tarifUnitaire: Double
)

@Entity(tableName = "couts_unitaires_mission")
data class CoutMissionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val typeMission: String, // ex: Dépannage AOG, Visite Programmée, Assistance Escale, Formation / Stage, etc.
    val echelleMin: Int = 1,
    val echelleMax: Int = 20,
    val coutUnitaireJournalier: Double, // TND par jour
    val observation: String = ""
)
