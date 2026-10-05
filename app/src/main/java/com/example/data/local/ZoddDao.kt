package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ZoddDao {

    // --- LOTS ---
    @Query("SELECT * FROM lots ORDER BY dateCreation DESC")
    fun getAllLotsFlow(): Flow<List<LotEntity>>

    @Query("SELECT * FROM lots WHERE lotN = :lotN LIMIT 1")
    suspend fun getLotById(lotN: String): LotEntity?

    @Query("SELECT * FROM lots WHERE lotN = :lotN LIMIT 1")
    fun getLotFlow(lotN: String): Flow<LotEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLot(lot: LotEntity)

    @Update
    suspend fun updateLot(lot: LotEntity)

    @Delete
    suspend fun deleteLot(lot: LotEntity)

    // --- ODD ---
    @Query("SELECT * FROM odds ORDER BY dateDebut DESC")
    fun getAllOddsFlow(): Flow<List<OddEntity>>

    @Query("SELECT * FROM odds WHERE lotN = :lotN ORDER BY oddN ASC")
    fun getOddsForLotFlow(lotN: String): Flow<List<OddEntity>>

    @Query("SELECT * FROM odds WHERE lotN = :lotN")
    suspend fun getOddsForLot(lotN: String): List<OddEntity>

    @Query("SELECT * FROM odds WHERE oddN = :oddN LIMIT 1")
    suspend fun getOddById(oddN: String): OddEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOdd(odd: OddEntity)

    @Update
    suspend fun updateOdd(odd: OddEntity)

    @Delete
    suspend fun deleteOdd(odd: OddEntity)

    // --- FRAIS ---
    @Query("SELECT * FROM frais ORDER BY date DESC")
    fun getAllFraisFlow(): Flow<List<FraisEntity>>

    @Query("SELECT * FROM frais WHERE oddN = :oddN ORDER BY date ASC")
    fun getFraisForOddFlow(oddN: String): Flow<List<FraisEntity>>

    @Query("SELECT * FROM frais WHERE oddN = :oddN")
    suspend fun getFraisForOdd(oddN: String): List<FraisEntity>

    @Query("SELECT f.* FROM frais f INNER JOIN odds o ON f.oddN = o.oddN WHERE o.lotN = :lotN")
    fun getFraisForLotFlow(lotN: String): Flow<List<FraisEntity>>

    @Query("SELECT f.* FROM frais f INNER JOIN odds o ON f.oddN = o.oddN WHERE o.lotN = :lotN")
    suspend fun getFraisForLot(lotN: String): List<FraisEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFrais(frais: FraisEntity)

    @Update
    suspend fun updateFrais(frais: FraisEntity)

    @Delete
    suspend fun deleteFrais(frais: FraisEntity)

    // --- DOCUMENTS ---
    @Query("SELECT * FROM documents_rattachement ORDER BY dateEnvoie DESC")
    fun getAllDocumentsFlow(): Flow<List<DocumentRattachementEntity>>

    @Query("SELECT * FROM documents_rattachement WHERE oddN = :oddN ORDER BY dateEnvoie DESC")
    fun getDocumentsForOddFlow(oddN: String): Flow<List<DocumentRattachementEntity>>

    @Query("SELECT d.* FROM documents_rattachement d INNER JOIN odds o ON d.oddN = o.oddN WHERE o.lotN = :lotN")
    fun getDocumentsForLotFlow(lotN: String): Flow<List<DocumentRattachementEntity>>

    @Query("SELECT d.* FROM documents_rattachement d INNER JOIN odds o ON d.oddN = o.oddN WHERE o.lotN = :lotN")
    suspend fun getDocumentsForLot(lotN: String): List<DocumentRattachementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: DocumentRattachementEntity)

    @Delete
    suspend fun deleteDocument(doc: DocumentRattachementEntity)

    // --- VALIDATIONS ---
    @Query("SELECT * FROM validations ORDER BY lotN, ordre ASC")
    fun getAllValidationsFlow(): Flow<List<ValidationEntity>>

    @Query("SELECT * FROM validations WHERE lotN = :lotN ORDER BY ordre ASC")
    fun getValidationsForLotFlow(lotN: String): Flow<List<ValidationEntity>>

    @Query("SELECT * FROM validations WHERE lotN = :lotN ORDER BY ordre ASC")
    suspend fun getValidationsForLot(lotN: String): List<ValidationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertValidation(validation: ValidationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertValidations(validations: List<ValidationEntity>)

    @Update
    suspend fun updateValidation(validation: ValidationEntity)

    @Query("DELETE FROM validations WHERE lotN = :lotN")
    suspend fun deleteValidationsForLot(lotN: String)

    // --- TECHNICIENS ---
    @Query("SELECT * FROM techniciens ORDER BY nom ASC, prenom ASC")
    fun getAllTechniciensFlow(): Flow<List<TechnicienEntity>>

    @Query("SELECT * FROM techniciens")
    suspend fun getAllTechniciens(): List<TechnicienEntity>

    @Query("SELECT * FROM techniciens WHERE matricule = :matricule LIMIT 1")
    suspend fun getTechnicien(matricule: String): TechnicienEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTechniciens(techniciens: List<TechnicienEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTechnicien(technicien: TechnicienEntity)

    @Update
    suspend fun updateTechnicien(technicien: TechnicienEntity)

    @Delete
    suspend fun deleteTechnicien(technicien: TechnicienEntity)

    @Query("DELETE FROM techniciens")
    suspend fun clearTechniciens()

    @Query("UPDATE techniciens SET matricule = REPLACE(matricule, 'TECH-', 'MAT-') WHERE matricule LIKE 'TECH-%'")
    suspend fun migrateTechMatriculesToMat()

    @Query("UPDATE odds SET matricule = REPLACE(matricule, 'TECH-', 'MAT-') WHERE matricule LIKE 'TECH-%'")
    suspend fun migrateOddMatriculesToMat()

    @Query("UPDATE techniciens SET histMissions = 0, histJours = 0")
    suspend fun resetTechniciensWorkload()

    // --- DIRECTIONS ---
    @Query("SELECT * FROM directions ORDER BY ordre ASC")
    fun getAllDirectionsFlow(): Flow<List<DirectionEntity>>

    @Query("SELECT * FROM directions ORDER BY ordre ASC")
    suspend fun getAllDirections(): List<DirectionEntity>

    @Query("SELECT * FROM directions WHERE code = :code LIMIT 1")
    suspend fun getDirection(code: String): DirectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirections(directions: List<DirectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirection(direction: DirectionEntity)

    @Update
    suspend fun updateDirection(direction: DirectionEntity)

    @Delete
    suspend fun deleteDirection(direction: DirectionEntity)

    @Query("DELETE FROM directions")
    suspend fun clearDirections()

    // --- BAREMES ---
    @Query("SELECT * FROM baremes ORDER BY categorie, designation")
    fun getAllBaremesFlow(): Flow<List<BaremeTarifEntity>>

    @Query("SELECT * FROM baremes ORDER BY categorie, echelleMin ASC")
    suspend fun getAllBaremesSync(): List<BaremeTarifEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBaremes(baremes: List<BaremeTarifEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBareme(bareme: BaremeTarifEntity)

    @Update
    suspend fun updateBareme(bareme: BaremeTarifEntity)

    @Delete
    suspend fun deleteBareme(bareme: BaremeTarifEntity)

    @Query("DELETE FROM baremes")
    suspend fun clearBaremes()

    // --- COUTS UNITAIRES MISSION ---
    @Query("SELECT * FROM couts_unitaires_mission ORDER BY typeMission ASC, echelleMin ASC")
    fun getAllCoutsMissionFlow(): Flow<List<CoutMissionEntity>>

    @Query("SELECT * FROM couts_unitaires_mission ORDER BY typeMission ASC, echelleMin ASC")
    suspend fun getAllCoutsMissionSync(): List<CoutMissionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoutMission(cout: CoutMissionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoutsMission(couts: List<CoutMissionEntity>)

    @Update
    suspend fun updateCoutMission(cout: CoutMissionEntity)

    @Delete
    suspend fun deleteCoutMission(cout: CoutMissionEntity)

    @Query("DELETE FROM couts_unitaires_mission WHERE typeMission LIKE '%AOG%' OR typeMission LIKE '%Dépannage%'")
    suspend fun deleteAogCoutsMission()

    @Query("DELETE FROM couts_unitaires_mission")
    suspend fun clearCoutsMission()

    // --- SYNC STATUS ---
    @Query("SELECT COUNT(*) FROM lots WHERE isSynced = 0")
    suspend fun getUnsyncedLotsCount(): Int

    @Query("SELECT * FROM lots WHERE isSynced = 0")
    suspend fun getUnsyncedLots(): List<LotEntity>

    @Query("SELECT * FROM odds WHERE isSynced = 0")
    suspend fun getUnsyncedOdds(): List<OddEntity>

    @Query("SELECT * FROM frais WHERE isSynced = 0")
    suspend fun getUnsyncedFrais(): List<FraisEntity>

    @Query("SELECT * FROM validations WHERE isSynced = 0")
    suspend fun getUnsyncedValidations(): List<ValidationEntity>
}
