package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirestoreSyncManager
import com.example.data.local.*
import com.example.data.repository.ZoddRepository
import com.example.ui.theme.AppPalette
import com.example.ui.theme.DarkModeOption
import com.example.util.EmailNotificationData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("zodd_user_prefs", Context.MODE_PRIVATE)

    private val _selectedPalette = MutableStateFlow(
        AppPalette.values().find { it.id == prefs.getString("pref_palette", AppPalette.AVIATION.id) } ?: AppPalette.AVIATION
    )
    val selectedPalette: StateFlow<AppPalette> = _selectedPalette.asStateFlow()

    private val _darkModeOption = MutableStateFlow(
        DarkModeOption.values().find { it.id == prefs.getString("pref_dark_mode", DarkModeOption.SYSTEM.id) } ?: DarkModeOption.SYSTEM
    )
    val darkModeOption: StateFlow<DarkModeOption> = _darkModeOption.asStateFlow()

    fun setPalette(palette: AppPalette) {
        _selectedPalette.value = palette
        prefs.edit().putString("pref_palette", palette.id).apply()
    }

    fun setDarkMode(option: DarkModeOption) {
        _darkModeOption.value = option
        prefs.edit().putString("pref_dark_mode", option.id).apply()
    }

    private val db = ZoddDatabase.getDatabase(application)
    private val syncManager = FirestoreSyncManager(db.zoddDao())
    val repository = ZoddRepository(application, db.zoddDao(), syncManager)

    init {
        viewModelScope.launch {
            repository.ensureOfficialDirections()
            repository.purgeAogMissions()
            // Cloud sync to Firebase
            repository.syncWithFirestore()
        }
        viewModelScope.launch {
            repository.directionsFlow.collect { dirList ->
                if (dirList.isNotEmpty()) {
                    com.example.data.auth.AuthManager.syncWithDirections(dirList)
                }
            }
        }
    }

    // Data streams
    val lots: StateFlow<List<LotEntity>> = repository.lotsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val techniciens: StateFlow<List<TechnicienEntity>> = repository.techniciensFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val directions: StateFlow<List<DirectionEntity>> = repository.directionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val baremes: StateFlow<List<BaremeTarifEntity>> = repository.baremesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val coutsMission: StateFlow<List<CoutMissionEntity>> = repository.coutsMissionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOdds: StateFlow<List<OddEntity>> = repository.oddsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFrais: StateFlow<List<FraisEntity>> = repository.allFraisFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDocs: StateFlow<List<DocumentRattachementEntity>> = repository.allDocumentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allValidations: StateFlow<List<ValidationEntity>> = repository.allValidationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Selected Lot
    private val _selectedLotN = MutableStateFlow<String?>(null)
    val selectedLotN: StateFlow<String?> = _selectedLotN.asStateFlow()

    val currentLot: StateFlow<LotEntity?> = _selectedLotN
        .flatMapLatest { lotN ->
            if (lotN != null) repository.getLotFlow(lotN) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentLotOdds: StateFlow<List<OddEntity>> = _selectedLotN
        .flatMapLatest { lotN ->
            if (lotN != null) repository.getOddsForLotFlow(lotN) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentLotFrais: StateFlow<List<FraisEntity>> = _selectedLotN
        .flatMapLatest { lotN ->
            if (lotN != null) repository.getFraisForLotFlow(lotN) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentLotDocs: StateFlow<List<DocumentRattachementEntity>> = _selectedLotN
        .flatMapLatest { lotN ->
            if (lotN != null) repository.getDocumentsForLotFlow(lotN) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentLotValidations: StateFlow<List<ValidationEntity>> = _selectedLotN
        .flatMapLatest { lotN ->
            if (lotN != null) repository.getValidationsForLotFlow(lotN) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Tab in Lot Detail (BASCULEMENT_VUES: "LES ODD", "FRAIS", "JUSTIFICATIFS", "VALIDATION")
    private val _currentLotTab = MutableStateFlow("LES ODD")
    val currentLotTab: StateFlow<String> = _currentLotTab.asStateFlow()

    // Sync status
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncMessage = MutableStateFlow("Prêt (Mode Hors Ligne / Cloud Firebase)")
    val lastSyncMessage: StateFlow<String> = _lastSyncMessage.asStateFlow()

    // Email Preview Dialog state
    private val _pendingEmailData = MutableStateFlow<EmailNotificationData?>(null)
    val pendingEmailData: StateFlow<EmailNotificationData?> = _pendingEmailData.asStateFlow()

    fun selectLot(lotN: String?, initialTab: String = "LES ODD") {
        _selectedLotN.value = lotN
        _currentLotTab.value = initialTab
    }

    fun switchLotTab(tab: String) {
        _currentLotTab.value = tab
        val lotN = _selectedLotN.value ?: return
        viewModelScope.launch {
            repository.updateLotView(lotN, tab)
        }
    }

    fun createLot(lotN: String, libelle: String, detailDivers: String = "") {
        viewModelScope.launch {
            repository.createLot(lotN, libelle, detailDivers)
            selectLot(lotN)
        }
    }

    fun addOdd(
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
    ) {
        viewModelScope.launch {
            repository.addOdd(oddN, lotN, matricule, siteProvenance, echelle, siteIntervention, dateDebut, dateFin, mission, avanceSurMission, detailDivers)
        }
    }

    fun updateOdd(odd: OddEntity) {
        viewModelScope.launch {
            repository.updateOdd(odd)
        }
    }

    fun deleteOdd(odd: OddEntity) {
        viewModelScope.launch {
            repository.deleteOdd(odd)
        }
    }

    fun addFrais(
        idFrais: String,
        oddN: String,
        lotN: String,
        categorie: String,
        sousCategorie: String,
        unite: String,
        quantite: Double,
        pUnit: Double,
        estComptabilise: Boolean = true
    ) {
        viewModelScope.launch {
            repository.addFrais(idFrais, oddN, lotN, System.currentTimeMillis(), categorie, sousCategorie, unite, quantite, pUnit, estComptabilise)
        }
    }

    fun toggleFraisComptabilise(frais: FraisEntity, lotN: String) {
        viewModelScope.launch {
            repository.toggleFraisComptabilise(frais, lotN)
        }
    }

    fun deleteFrais(frais: FraisEntity, lotN: String) {
        viewModelScope.launch {
            repository.deleteFrais(frais, lotN)
        }
    }

    fun updateFrais(frais: FraisEntity, lotN: String) {
        viewModelScope.launch {
            repository.updateFrais(frais, lotN)
        }
    }

    fun attachDocument(idDoc: String, oddN: String, typeDoc: String, objet: String, scanUri: String?) {
        viewModelScope.launch {
            repository.attachDocument(idDoc, oddN, typeDoc, objet, scanUri)
        }
    }

    fun updateDocument(doc: DocumentRattachementEntity) {
        viewModelScope.launch {
            repository.updateDocument(doc)
        }
    }

    fun deleteDocument(doc: DocumentRattachementEntity) {
        viewModelScope.launch {
            repository.deleteDocument(doc)
        }
    }

    fun figerLot(lotN: String) {
        viewModelScope.launch {
            try {
                val (pdfFile, emailData) = repository.figerLot(lotN)
                _pendingEmailData.value = emailData
                _lastSyncMessage.value = "Lot $lotN figé avec succès. Rapport PDF généré."
                Toast.makeText(getApplication(), "Lot $lotN figé en mode lecture seule ! PDF généré.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun processValidation(
        lotN: String,
        directionCode: String,
        isApproved: Boolean,
        commentaire: String,
        signataire: String
    ) {
        viewModelScope.launch {
            val emailData = repository.processDirectionValidation(lotN, directionCode, isApproved, commentaire, signataire)
            if (emailData != null) {
                _pendingEmailData.value = emailData
            }
            val status = if (isApproved) "validé" else "rejeté"
            Toast.makeText(getApplication(), "Dossier $lotN $status par $directionCode. Notification prête.", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerNotificationForLot(lotN: String) {
        viewModelScope.launch {
            try {
                val emailData = repository.prepareNotificationForCurrentDirection(lotN)
                if (emailData != null) {
                    _pendingEmailData.value = emailData
                } else {
                    Toast.makeText(getApplication(), "Impossible de préparer la notification pour le lot $lotN.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun dismissEmailDialog() {
        _pendingEmailData.value = null
    }

    fun syncWithFirestore() {
        viewModelScope.launch {
            _isSyncing.value = true
            _lastSyncMessage.value = "Synchronisation Firestore en cours..."
            val result = repository.syncWithFirestore()
            _isSyncing.value = false
            result.onSuccess { count ->
                _lastSyncMessage.value = "Synchronisé avec Firebase ($count éléments)"
                Toast.makeText(getApplication(), "Synchronisation réussie avec Firebase !", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                _lastSyncMessage.value = "Mode hors ligne actif (${err.localizedMessage ?: "Stocké localement"})"
                Toast.makeText(getApplication(), "Mode hors ligne actif. Données stockées localement.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openOrSharePdf(context: Context, lotN: String) {
        viewModelScope.launch {
            try {
                repository.downloadOrOpenLotReportPdf(context, lotN)
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur génération PDF : ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun downloadDefinitivePdf(context: Context, lotN: String) {
        viewModelScope.launch {
            try {
                val file = repository.downloadOrOpenLotReportPdf(context, lotN)
                if (file != null) {
                    Toast.makeText(context, "📄 Rapport définitif généré et téléchargé : ${file.name}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur lors du téléchargement : ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun sendValidationEmail(lotN: String, targetDirCode: String? = null) {
        viewModelScope.launch {
            try {
                val emailData = repository.prepareValidationEmailForLot(lotN, targetDirCode)
                if (emailData != null) {
                    _pendingEmailData.value = emailData
                } else {
                    Toast.makeText(getApplication(), "Direction introuvable ou erreur de préparation.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "Erreur préparation email : ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- REFERENTIELS & BASE PARAMETERS MANAGEMENT ---
    fun saveTechnicien(technicien: TechnicienEntity) {
        viewModelScope.launch {
            repository.saveTechnicien(technicien)
            Toast.makeText(getApplication(), "Technicien ${technicien.matricule} enregistré.", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteTechnicien(technicien: TechnicienEntity) {
        viewModelScope.launch {
            repository.deleteTechnicien(technicien)
            Toast.makeText(getApplication(), "Technicien ${technicien.matricule} supprimé.", Toast.LENGTH_SHORT).show()
        }
    }

    fun resetTechniciensWorkload() {
        viewModelScope.launch {
            repository.resetTechniciensWorkload()
            Toast.makeText(getApplication(), "Charges de travail des techniciens remises à zéro.", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveDirection(direction: DirectionEntity) {
        viewModelScope.launch {
            repository.saveDirection(direction)
            Toast.makeText(getApplication(), "Direction ${direction.code} mise à jour.", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteDirection(direction: DirectionEntity) {
        viewModelScope.launch {
            repository.deleteDirection(direction)
            Toast.makeText(getApplication(), "Direction ${direction.code} supprimée.", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveBareme(bareme: BaremeTarifEntity) {
        viewModelScope.launch {
            repository.saveBareme(bareme)
            Toast.makeText(getApplication(), "Barème enregistré.", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteBareme(bareme: BaremeTarifEntity) {
        viewModelScope.launch {
            repository.deleteBareme(bareme)
            Toast.makeText(getApplication(), "Barème supprimé.", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveCoutMission(cout: CoutMissionEntity) {
        viewModelScope.launch {
            repository.saveCoutMission(cout)
            _selectedLotN.value?.let { repository.recalculateLotTotals(it) }
            Toast.makeText(getApplication(), "Coût unitaire mission enregistré.", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteCoutMission(cout: CoutMissionEntity) {
        viewModelScope.launch {
            repository.deleteCoutMission(cout)
            _selectedLotN.value?.let { repository.recalculateLotTotals(it) }
            Toast.makeText(getApplication(), "Coût unitaire mission supprimé.", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateTechniciens() {
        viewModelScope.launch {
            repository.updateTechniciens()
            Toast.makeText(getApplication(), "Onglet Techniciens mis à jour avec succès.", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateDirections() {
        viewModelScope.launch {
            repository.updateDirections()
            Toast.makeText(getApplication(), "Onglet Directions mis à jour avec succès.", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateBaremes() {
        viewModelScope.launch {
            repository.updateBaremes()
            Toast.makeText(getApplication(), "Onglet Barèmes mis à jour : Coût unitaire mission par type de mission appliqué.", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateCoutsMission() {
        viewModelScope.launch {
            repository.updateCoutsMission()
            Toast.makeText(getApplication(), "Onglet Coûts Missions mis à jour avec succès.", Toast.LENGTH_SHORT).show()
        }
    }

    fun resetReferentielsToDefaults() {
        viewModelScope.launch {
            repository.resetReferentielsToDefaults()
            Toast.makeText(getApplication(), "Paramètres de base réinitialisés avec succès.", Toast.LENGTH_LONG).show()
        }
    }
}
