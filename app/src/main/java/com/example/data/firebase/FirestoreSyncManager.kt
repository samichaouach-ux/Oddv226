package com.example.data.firebase

import android.util.Log
import com.example.data.local.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class FirestoreSyncManager(
    private val dao: ZoddDao
) {
    private val tag = "FirestoreSyncManager"

    private fun getFirestoreSafe(): FirebaseFirestore? {
        val app = try {
            com.google.firebase.FirebaseApp.getInstance()
        } catch (_: Throwable) {
            null
        } ?: return null

        return try {
            FirebaseFirestore.getInstance(app, "ai-studio-47bb18e7-4836-4703-a370-7b48d1e624f1")
        } catch (e1: Throwable) {
            try {
                FirebaseFirestore.getInstance("ai-studio-47bb18e7-4836-4703-a370-7b48d1e624f1")
            } catch (e2: Throwable) {
                try {
                    FirebaseFirestore.getInstance(app)
                } catch (e3: Throwable) {
                    Log.w(tag, "FirebaseFirestore instance not ready: ${e3.message}")
                    null
                }
            }
        }
    }

    suspend fun syncAll(): Result<Int> = withContext(Dispatchers.IO) {
        withTimeoutOrNull(3000L) {
            try {
                val firestore = getFirestoreSafe()
                    ?: return@withTimeoutOrNull Result.failure(Exception("Service cloud non disponible ou hors-ligne"))

                var syncedItems = 0

            // 1. Sync unsynced Lots
            val unsyncedLots = dao.getUnsyncedLots()
            for (lot in unsyncedLots) {
                val data = hashMapOf(
                    "lotN" to lot.lotN,
                    "libelle" to lot.libelle,
                    "dateCreation" to lot.dateCreation,
                    "basculementVues" to lot.basculementVues,
                    "figerLot" to lot.figerLot,
                    "versionPdfPath" to (lot.versionPdfPath ?: ""),
                    "statutValidation" to lot.statutValidation,
                    "directionActuelle" to lot.directionActuelle,
                    "totalMission" to lot.totalMission,
                    "totalCoutOdd" to lot.totalCoutOdd,
                    "totalHebergement" to lot.totalHebergement,
                    "totalTransport" to lot.totalTransport,
                    "totalFrais" to lot.totalFrais,
                    "emailDmEnvoye" to lot.emailDmEnvoye,
                    "detailDivers" to lot.detailDivers,
                    "updatedAt" to lot.updatedAt
                )
                firestore.collection("lots").document(lot.lotN).set(data, SetOptions.merge()).await()
                dao.updateLot(lot.copy(isSynced = true))
                syncedItems++
            }

            // 2. Sync unsynced ODDs
            val unsyncedOdds = dao.getUnsyncedOdds()
            for (odd in unsyncedOdds) {
                val data = hashMapOf(
                    "oddN" to odd.oddN,
                    "lotN" to odd.lotN,
                    "matricule" to odd.matricule,
                    "siteProvenance" to odd.siteProvenance,
                    "echelle" to odd.echelle,
                    "siteIntervention" to odd.siteIntervention,
                    "dateDebut" to odd.dateDebut,
                    "dateFin" to odd.dateFin,
                    "mission" to odd.mission,
                    "modeEdit" to odd.modeEdit,
                    "totalOdd" to odd.totalOdd,
                    "avanceSurMission" to odd.avanceSurMission,
                    "detailDivers" to odd.detailDivers,
                    "updatedAt" to odd.updatedAt
                )
                firestore.collection("odds").document(odd.oddN).set(data, SetOptions.merge()).await()
                dao.updateOdd(odd.copy(isSynced = true))
                syncedItems++
            }

            // 3. Sync unsynced Frais
            val unsyncedFrais = dao.getUnsyncedFrais()
            for (f in unsyncedFrais) {
                val data = hashMapOf(
                    "idFrais" to f.idFrais,
                    "oddN" to f.oddN,
                    "date" to f.date,
                    "categorie" to f.categorie,
                    "sousCategorie" to f.sousCategorie,
                    "unite" to f.unite,
                    "quantite" to f.quantite,
                    "pUnit" to f.pUnit,
                    "montant" to f.montant,
                    "aComptabiliser" to f.aComptabiliser,
                    "estComptabilise" to f.estComptabilise,
                    "updatedAt" to f.updatedAt
                )
                firestore.collection("frais").document(f.idFrais).set(data, SetOptions.merge()).await()
                dao.updateFrais(f.copy(isSynced = true))
                syncedItems++
            }

            // 4. Sync unsynced Validations
            val unsyncedValidations = dao.getUnsyncedValidations()
            for (v in unsyncedValidations) {
                val data = hashMapOf(
                    "idVal" to v.idVal,
                    "lotN" to v.lotN,
                    "direction" to v.direction,
                    "ordre" to v.ordre,
                    "valide" to v.valide,
                    "dateValidation" to (v.dateValidation ?: 0L),
                    "commentaire" to v.commentaire,
                    "signataire" to v.signataire,
                    "historiqueVal" to v.historiqueVal,
                    "idArc" to (v.idArc ?: ""),
                    "dateArchivage" to (v.dateArchivage ?: 0L),
                    "ttalMission" to v.ttalMission,
                    "ttalCoutOdd" to v.ttalCoutOdd,
                    "ttalHebergement" to v.ttalHebergement,
                    "ttalFrais" to v.ttalFrais,
                    "reactiviteJours" to v.reactiviteJours,
                    "emailEnvoye" to v.emailEnvoye,
                    "updatedAt" to v.updatedAt
                )
                firestore.collection("validations").document(v.idVal).set(data, SetOptions.merge()).await()
                dao.updateValidation(v.copy(isSynced = true))
                syncedItems++
            }

            // 5. Sync Directions to Cloud Firestore
            val allDirections = dao.getAllDirections()
            for (dir in allDirections) {
                val data = hashMapOf(
                    "code" to dir.code,
                    "libelle" to dir.libelle,
                    "directeur" to dir.directeur,
                    "telephone" to dir.telephone,
                    "email" to dir.email,
                    "ordre" to dir.ordre,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore.collection("directions").document(dir.code).set(data, SetOptions.merge()).await()
                syncedItems++
            }

            Log.d(tag, "Sync completed successfully: $syncedItems items updated")
            Result.success(syncedItems)
        } catch (e: Exception) {
            Log.w(tag, "Firestore sync failed or offline: ${e.message}")
            Result.failure(e)
        }
    } ?: Result.failure(Exception("Délai cloud dépassé (mode hors-ligne)"))
}

    suspend fun syncDirection(dir: DirectionEntity) = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestoreSafe() ?: return@withContext
            val data = hashMapOf(
                "code" to dir.code,
                "libelle" to dir.libelle,
                "directeur" to dir.directeur,
                "telephone" to dir.telephone,
                "email" to dir.email,
                "ordre" to dir.ordre,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("directions").document(dir.code).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(tag, "Sync direction offline: ${e.message}")
        }
    }

    suspend fun syncSingleLot(lot: LotEntity) = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestoreSafe() ?: return@withContext
            val data = hashMapOf(
                "lotN" to lot.lotN,
                "libelle" to lot.libelle,
                "dateCreation" to lot.dateCreation,
                "basculementVues" to lot.basculementVues,
                "figerLot" to lot.figerLot,
                "versionPdfPath" to (lot.versionPdfPath ?: ""),
                "statutValidation" to lot.statutValidation,
                "directionActuelle" to lot.directionActuelle,
                "totalMission" to lot.totalMission,
                "totalCoutOdd" to lot.totalCoutOdd,
                "totalHebergement" to lot.totalHebergement,
                "totalTransport" to lot.totalTransport,
                "totalFrais" to lot.totalFrais,
                "detailDivers" to lot.detailDivers,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("lots").document(lot.lotN).set(data, SetOptions.merge()).await()
            dao.updateLot(lot.copy(isSynced = true))
        } catch (e: Exception) {
            Log.w(tag, "Sync single lot offline: ${e.message}")
        }
    }
}
