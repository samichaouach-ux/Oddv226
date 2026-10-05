package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt
import com.example.R
import com.example.ui.components.EmailPreviewDialog
import com.example.ui.components.ThemeSelectorDialog
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.lots.*
import com.example.ui.screens.manual.UserManualScreen
import com.example.ui.screens.referentiels.ReferentielsScreen
import com.example.ui.screens.validation.ValidationCircuitScreen
import com.example.ui.theme.*

enum class AppDestination(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    LOTS("Lots & ODD", Icons.Default.FolderSpecial),
    VALIDATION("Validation", Icons.Default.FactCheck),
    DASHBOARD("Tableau de Bord", Icons.Default.InsertChart),
    REFERENTIELS("Référentiels", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val lots by viewModel.lots.collectAsStateWithLifecycle()
    val techniciens by viewModel.techniciens.collectAsStateWithLifecycle()
    val directions by viewModel.directions.collectAsStateWithLifecycle()
    val baremes by viewModel.baremes.collectAsStateWithLifecycle()
    val allOdds by viewModel.allOdds.collectAsStateWithLifecycle()
    val allFrais by viewModel.allFrais.collectAsStateWithLifecycle()
    val allDocs by viewModel.allDocs.collectAsStateWithLifecycle()
    val allValidations by viewModel.allValidations.collectAsStateWithLifecycle()
    val coutsMission by viewModel.coutsMission.collectAsStateWithLifecycle()

    val selectedLotN by viewModel.selectedLotN.collectAsStateWithLifecycle()
    val currentLot by viewModel.currentLot.collectAsStateWithLifecycle()
    val currentLotOdds by viewModel.currentLotOdds.collectAsStateWithLifecycle()
    val currentLotFrais by viewModel.currentLotFrais.collectAsStateWithLifecycle()
    val currentLotDocs by viewModel.currentLotDocs.collectAsStateWithLifecycle()
    val currentLotValidations by viewModel.currentLotValidations.collectAsStateWithLifecycle()
    val currentLotTab by viewModel.currentLotTab.collectAsStateWithLifecycle()

    val selectedPalette by viewModel.selectedPalette.collectAsStateWithLifecycle()
    val darkModeOption by viewModel.darkModeOption.collectAsStateWithLifecycle()

    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val pendingEmailData by viewModel.pendingEmailData.collectAsStateWithLifecycle()

    var currentDestination by remember { mutableStateOf(AppDestination.LOTS) }

    // Dialog controllers
    var showCreateLotDialog by remember { mutableStateOf(false) }
    var showAddOddDialog by remember { mutableStateOf(false) }
    var showAddFraisDialog by remember { mutableStateOf(false) }
    var showAttachDocDialog by remember { mutableStateOf(false) }
    var showThemeSelectorDialog by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }
    var showRoleSwitcherDialog by remember { mutableStateOf(false) }

    // Preselected ODD for Frais / Doc dialogs
    var preselectedOddForFrais by remember { mutableStateOf<String?>(null) }
    var preselectedOddForDoc by remember { mutableStateOf<String?>(null) }
    var showInstallAppDialog by remember { mutableStateOf(false) }

    // Global Zoom State (0.75x to 1.6x) applied to all pages
    var globalZoomLevel by remember { mutableFloatStateOf(1.0f) }
    var showGlobalZoomBar by remember { mutableStateOf(false) }

    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * globalZoomLevel
    )

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.zodd_logo),
                                    contentDescription = "Logo zODD v2-26",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "zODD V.2-26",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "by msc",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AviationBlue,
                                            modifier = Modifier.padding(bottom = 1.dp)
                                        )
                                    }
                                    Text(
                                        text = "Logistique & Frais de Mission",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        actions = {
                            // Action Zoom Généralisé à toutes les pages
                            IconButton(onClick = { showGlobalZoomBar = !showGlobalZoomBar }) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (globalZoomLevel != 1.0f) AmberGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (globalZoomLevel != 1.0f) AmberGold else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ZoomIn,
                                            contentDescription = "Zoom global",
                                            tint = if (globalZoomLevel != 1.0f) AmberGold else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "${(globalZoomLevel * 100).roundToInt()}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (globalZoomLevel != 1.0f) AmberGold else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // Bouton d'Installation Mobile (Android APK / iOS)
                            FilledTonalButton(
                                onClick = { showInstallAppDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Installer", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Installer 📲", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Manuel d'Utilisation Action
                            IconButton(onClick = { showManualDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "Manuel d'utilisation",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Palette / Appearance Action
                            IconButton(onClick = { showThemeSelectorDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Aspect & Couleurs",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            TextButton(
                                onClick = { viewModel.syncWithFirestore() },
                                enabled = !isSyncing
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = "Sync",
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSyncing) "Sync..." else "Cloud",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                            }

                            // Role Switcher & User Profile
                            IconButton(onClick = { showRoleSwitcherDialog = true }) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = "Profil & Rôle",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Barre de Zoom Rapide Généralisée
                    if (showGlobalZoomBar) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shadowElevation = 3.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Zoom :",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { globalZoomLevel = (globalZoomLevel - 0.1f).coerceAtLeast(0.75f) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Zoom arrière", modifier = Modifier.size(16.dp))
                                    }
                                    TextButton(
                                        onClick = { globalZoomLevel = 1.0f },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) {
                                        Text(
                                            text = "${(globalZoomLevel * 100).roundToInt()}%",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { globalZoomLevel = (globalZoomLevel + 0.1f).coerceAtMost(1.6f) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Zoom avant", modifier = Modifier.size(16.dp))
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(0.85f, 1.0f, 1.15f, 1.30f).forEach { preset ->
                                        val isSelected = (globalZoomLevel * 100).roundToInt() == (preset * 100).roundToInt()
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { globalZoomLevel = preset },
                                            label = { Text("${(preset * 100).roundToInt()}%", fontSize = 10.sp) },
                                            modifier = Modifier.height(28.dp)
                                        )
                                    }
                                    IconButton(onClick = { showGlobalZoomBar = false }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Fermer barre zoom", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                AppDestination.values().forEach { dest ->
                    val isSelected = currentDestination == dest
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            currentDestination = dest
                            if (dest != AppDestination.LOTS) {
                                viewModel.selectLot(null)
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = {
                            Text(
                                text = dest.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = selectedPalette.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentDestination) {
                AppDestination.LOTS -> {
                    val activeLot = currentLot ?: lots.find { it.lotN == selectedLotN }
                    val activeOdds = if (currentLotOdds.isNotEmpty()) currentLotOdds else allOdds.filter { it.lotN == selectedLotN }
                    val activeLotOddIds = activeOdds.map { it.oddN }.toSet()
                    val activeFrais = if (currentLotFrais.isNotEmpty()) currentLotFrais else allFrais.filter { it.oddN in activeLotOddIds }
                    val activeDocs = if (currentLotDocs.isNotEmpty()) currentLotDocs else allDocs.filter { it.oddN in activeLotOddIds }
                    val activeValidations = if (currentLotValidations.isNotEmpty()) currentLotValidations else allValidations.filter { it.lotN == selectedLotN }

                    if (selectedLotN != null && activeLot != null) {
                        LotDetailScreen(
                            lot = activeLot,
                            allLots = lots,
                            onSelectAnotherLot = { viewModel.selectLot(it) },
                            odds = activeOdds,
                            fraisList = activeFrais,
                            documents = activeDocs,
                            validations = activeValidations,
                            activeTab = currentLotTab,
                            onTabSelected = { viewModel.switchLotTab(it) },
                            onBack = { viewModel.selectLot(null) },
                            onFigerLot = { viewModel.figerLot(it) },
                            onAddOddClick = { showAddOddDialog = true },
                            onDeleteOdd = { viewModel.deleteOdd(it) },
                            onAddFraisClick = {
                                preselectedOddForFrais = null
                                showAddFraisDialog = true
                            },
                            onDeleteFrais = { viewModel.deleteFrais(it, activeLot.lotN) },
                            onAttachDocClick = {
                                preselectedOddForDoc = null
                                showAttachDocDialog = true
                            },
                            onAddFraisForOdd = { oddN ->
                                preselectedOddForFrais = oddN
                                showAddFraisDialog = true
                            },
                            onAttachDocForOdd = { oddN ->
                                preselectedOddForDoc = oddN
                                showAttachDocDialog = true
                            },
                            onOpenPdf = { ctx, lotN -> viewModel.openOrSharePdf(ctx, lotN) },
                            onValidateStep = { lotN, dir, approved, comment, sign ->
                                viewModel.processValidation(lotN, dir, approved, comment, sign)
                            },
                            onSendValidationEmail = { lotN ->
                                viewModel.triggerNotificationForLot(lotN)
                            },
                            baremes = baremes,
                            coutsMission = coutsMission,
                            onToggleFraisComptabilise = { viewModel.toggleFraisComptabilise(it, activeLot.lotN) }
                        )
                    } else {
                        LotListScreen(
                            lots = lots,
                            allOdds = allOdds,
                            allFrais = allFrais,
                            allDocs = allDocs,
                            allValidations = allValidations,
                            onSelectLot = { viewModel.selectLot(it) },
                            onCreateLotClick = { showCreateLotDialog = true }
                        )
                    }
                }
                AppDestination.VALIDATION -> {
                    ValidationCircuitScreen(
                        lots = lots,
                        directions = directions,
                        allValidations = allValidations,
                        onSelectLot = {
                            viewModel.selectLot(it)
                            currentDestination = AppDestination.LOTS
                        },
                        onValidateStep = { lotN, dir, approved, comment, sign ->
                            viewModel.processValidation(lotN, dir, approved, comment, sign)
                        },
                        onOpenPdf = { ctx, lotN -> viewModel.openOrSharePdf(ctx, lotN) },
                        onTriggerNotification = { lotN -> viewModel.triggerNotificationForLot(lotN) }
                    )
                }
                AppDestination.DASHBOARD -> {
                    DashboardScreen(
                        lots = lots,
                        odds = allOdds,
                        fraisList = allFrais,
                        validations = allValidations,
                        techniciens = techniciens
                    )
                }
                AppDestination.REFERENTIELS -> {
                    ReferentielsScreen(
                        techniciens = techniciens,
                        directions = directions,
                        baremes = baremes,
                        coutsMission = coutsMission,
                        onSaveTechnicien = { viewModel.saveTechnicien(it) },
                        onDeleteTechnicien = { viewModel.deleteTechnicien(it) },
                        onSaveDirection = { viewModel.saveDirection(it) },
                        onDeleteDirection = { viewModel.deleteDirection(it) },
                        onSaveBareme = { viewModel.saveBareme(it) },
                        onDeleteBareme = { viewModel.deleteBareme(it) },
                        onSaveCoutMission = { viewModel.saveCoutMission(it) },
                        onDeleteCoutMission = { viewModel.deleteCoutMission(it) },
                        onUpdateTechniciens = { viewModel.updateTechniciens() },
                        onUpdateDirections = { viewModel.updateDirections() },
                        onUpdateCoutsMission = { viewModel.updateCoutsMission() },
                        onUpdateBaremes = { viewModel.updateBaremes() },
                        onResetDefaults = { viewModel.resetReferentielsToDefaults() },
                        onResetTechniciensWorkload = { viewModel.resetTechniciensWorkload() },
                        onOpenThemeSelector = { showThemeSelectorDialog = true }
                    )
                }
            }
        }
    }

    // Modal Dialogs
    if (showCreateLotDialog) {
        AddEditLotDialog(
            onDismiss = { showCreateLotDialog = false },
            onConfirm = { lotN, libelle, detailDivers ->
                showCreateLotDialog = false
                viewModel.createLot(lotN, libelle, detailDivers)
            }
        )
    }

        val activeLotForDialog = currentLot
        if (showAddOddDialog && activeLotForDialog != null) {
            AddEditOddDialog(
                lotN = activeLotForDialog.lotN,
                techniciens = techniciens,
                onDismiss = { showAddOddDialog = false },
                onConfirm = { oddN, matricule, prov, ech, inter, deb, fin, miss, avance, detailDivers ->
                    showAddOddDialog = false
                    viewModel.addOdd(oddN, activeLotForDialog.lotN, matricule, prov, ech, inter, deb, fin, miss, avance, detailDivers)
                }
            )
        }

        if (showAddFraisDialog && activeLotForDialog != null) {
            AddEditFraisDialog(
                odds = currentLotOdds,
                baremes = baremes,
                preselectedOddN = preselectedOddForFrais,
                onDismiss = {
                    showAddFraisDialog = false
                    preselectedOddForFrais = null
                },
                onConfirm = { idFrais, oddN, cat, sousCat, unite, qte, pUnit, estComptabilise ->
                    showAddFraisDialog = false
                    preselectedOddForFrais = null
                    viewModel.addFrais(idFrais, oddN, activeLotForDialog.lotN, cat, sousCat, unite, qte, pUnit, estComptabilise)
                }
            )
        }

        if (showAttachDocDialog && activeLotForDialog != null) {
            AttachDocumentDialog(
                odds = currentLotOdds,
                preselectedOddN = preselectedOddForDoc,
                onDismiss = {
                    showAttachDocDialog = false
                    preselectedOddForDoc = null
                },
                onConfirm = { idDoc, oddN, typeDoc, objet, scanUri ->
                    showAttachDocDialog = false
                    preselectedOddForDoc = null
                    viewModel.attachDocument(idDoc, oddN, typeDoc, objet, scanUri)
                }
            )
        }

        if (showThemeSelectorDialog) {
            ThemeSelectorDialog(
                currentPalette = selectedPalette,
                currentDarkMode = darkModeOption,
                onSelectPalette = { viewModel.setPalette(it) },
                onSelectDarkMode = { viewModel.setDarkMode(it) },
                onDismiss = { showThemeSelectorDialog = false }
            )
        }

        if (showManualDialog) {
            Dialog(
                onDismissRequest = { showManualDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UserManualScreen(onClose = { showManualDialog = false })
                }
            }
        }

        if (showInstallAppDialog) {
            com.example.ui.components.InstallAppModalDialog(
                onDismiss = { showInstallAppDialog = false }
            )
        }

        if (pendingEmailData != null) {
            EmailPreviewDialog(
                emailData = pendingEmailData!!,
                onDismiss = { viewModel.dismissEmailDialog() }
            )
        }

        if (showRoleSwitcherDialog) {
            val activeUser by com.example.data.auth.AuthManager.currentUser.collectAsStateWithLifecycle()
            AlertDialog(
                onDismissRequest = { showRoleSwitcherDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ManageAccounts, contentDescription = null, tint = AviationBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Profil & Contrôle d'Accès", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeUser?.let { u ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldGreen.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Connecté : ${u.displayName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("${u.role} (${u.email})", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("BASCULER VERS UN AUTRE RÔLE SIGNATAIRE :", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateMedium)

                        // Base de données Firebase Console Statut
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldGreen.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CloudDone, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Base de Données Cloud Firebase : Connectée • Synchro Active",
                                    fontSize = 10.sp,
                                    color = EmeraldGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        val currentRoles by com.example.data.auth.AuthManager.rolesState.collectAsStateWithLifecycle()
                        val displayRoles = remember(directions, currentRoles) {
                            if (directions.isNotEmpty()) {
                                val list = mutableListOf<com.example.data.auth.AuthManager.RoleItem>()
                                list.add(
                                    com.example.data.auth.AuthManager.RoleItem(
                                        roleName = "Chargée de Gestion & Traitement des Lots",
                                        dirCode = "DAF",
                                        displayName = "Mme Nahla Zaoui",
                                        email = "nahla.zaoui@tunisair-technics.tn",
                                        description = "Contrôle, saisie et traitement réglementaire des lots ODD"
                                    )
                                )
                                directions.sortedBy { it.ordre }.forEach { dir ->
                                    list.add(
                                        com.example.data.auth.AuthManager.RoleItem(
                                            roleName = "${dir.libelle} (${dir.code})",
                                            dirCode = dir.code,
                                            displayName = dir.directeur,
                                            email = dir.email,
                                            description = "Visa hiérarchique étape n°${dir.ordre} - ${dir.code}"
                                        )
                                    )
                                }
                                list.add(
                                    com.example.data.auth.AuthManager.RoleItem(
                                        roleName = "Contrôleur de Gestion DAF & Direction",
                                        dirCode = "DAF",
                                        displayName = "M. Sami Chaouach",
                                        email = "samichaouach@gmail.com",
                                        description = "Contrôle de gestion, audit analytique ERP & liquidation"
                                    )
                                )
                                list
                            } else currentRoles
                        }

                        displayRoles.forEach { roleItem ->
                            val isNahla = roleItem.displayName.contains("Nahla", ignoreCase = true)
                            val isCurrent = activeUser?.role == roleItem.roleName
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent) AviationBlue.copy(alpha = 0.18f) else if (isNahla) AmberGold.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isNahla) androidx.compose.foundation.BorderStroke(1.dp, AmberGold) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showRoleSwitcherDialog = false
                                        // Déconnexion de l'actuel profil pour ouvrir la page d'accès
                                        com.example.data.auth.AuthManager.signOut()
                                    }
                            ) {
                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        when {
                                            isNahla -> Icons.Default.AssignmentInd
                                            roleItem.dirCode == "DM" -> Icons.Default.Build
                                            roleItem.dirCode == "DAF" -> Icons.Default.AccountBalance
                                            else -> Icons.Default.Badge
                                        },
                                        contentDescription = null,
                                        tint = if (isNahla) AmberGold else AviationBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(roleItem.displayName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(roleItem.roleName, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showRoleSwitcherDialog = false }) {
                        Text("Fermer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        com.example.data.auth.AuthManager.signOut()
                        showRoleSwitcherDialog = false
                    }) {
                        Text("Déconnexion", color = Color.Red)
                    }
                }
            )
        }
    }
}
