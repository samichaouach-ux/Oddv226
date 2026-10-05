package com.example.ui.screens.lots

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.*
import com.example.ui.components.DirectionTimeline
import com.example.ui.components.StatusBadge
import com.example.ui.screens.validation.ValidateStepDialog
import com.example.ui.theme.*
import com.example.util.SiteDistances
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotDetailScreen(
    lot: LotEntity,
    allLots: List<LotEntity> = emptyList(),
    onSelectAnotherLot: (String) -> Unit = {},
    odds: List<OddEntity>,
    fraisList: List<FraisEntity>,
    documents: List<DocumentRattachementEntity>,
    validations: List<ValidationEntity>,
    activeTab: String,
    onTabSelected: (String) -> Unit,
    onBack: () -> Unit,
    onFigerLot: (String) -> Unit,
    onAddOddClick: () -> Unit,
    onDeleteOdd: (OddEntity) -> Unit,
    onAddFraisClick: () -> Unit,
    onDeleteFrais: (FraisEntity) -> Unit,
    onAttachDocClick: () -> Unit,
    onAddFraisForOdd: (String) -> Unit = {},
    onAttachDocForOdd: (String) -> Unit = {},
    onOpenPdf: (Context, String) -> Unit,
    onValidateStep: (lotN: String, dirCode: String, isApproved: Boolean, comment: String, signataire: String) -> Unit,
    onSendValidationEmail: (lotN: String) -> Unit = {},
    baremes: List<BaremeTarifEntity> = emptyList(),
    coutsMission: List<com.example.data.local.CoutMissionEntity> = emptyList(),
    onToggleFraisComptabilise: (FraisEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeUser by com.example.data.auth.AuthManager.currentUser.collectAsStateWithLifecycle()
    val isDg = activeUser?.dirCode == "DG" || activeUser?.role?.contains("Générale", ignoreCase = true) == true
    val isNahla = activeUser?.displayName?.contains("Nahla", ignoreCase = true) == true ||
            activeUser?.role?.contains("Gestion", ignoreCase = true) == true
    val isDgOrNahla = isDg || isNahla
    val isReadOnly = lot.figerLot || !isDgOrNahla
    val userDir = activeUser?.dirCode ?: ""

    val tabs = if (lot.figerLot) listOf("LES ODD", "FRAIS", "JUSTIFICATIFS", "VALIDATION") else listOf("LES ODD", "FRAIS", "JUSTIFICATIFS")
    val effectiveActiveTab = if (!lot.figerLot && activeTab == "VALIDATION") "LES ODD" else if (activeTab in tabs) activeTab else "LES ODD"
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)

    var showFreezeConfirmDialog by remember { mutableStateOf(false) }
    var selectedDirForValidation by remember { mutableStateOf<String?>(null) }
    var showLotSelectorMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(lot.lotN, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusBadge(status = lot.statutValidation, isFige = lot.figerLot)
                        }
                        Text(
                            text = lot.libelle,
                            fontSize = 11.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    // Quick Lot Switcher button
                    if (allLots.size > 1) {
                        Box {
                            TextButton(onClick = { showLotSelectorMenu = true }) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Changer Lot", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            DropdownMenu(
                                expanded = showLotSelectorMenu,
                                onDismissRequest = { showLotSelectorMenu = false }
                            ) {
                                allLots.forEach { l ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (l.lotN == lot.lotN) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                }
                                                Text("${l.lotN} - ${l.libelle}", fontWeight = if (l.lotN == lot.lotN) FontWeight.Bold else FontWeight.Normal)
                                            }
                                        },
                                        onClick = {
                                            showLotSelectorMenu = false
                                            if (l.lotN != lot.lotN) {
                                                onSelectAnotherLot(l.lotN)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // L'icône PDF du rapport ne sera visible que pour les lots qui ont été figés
                    if (lot.figerLot) {
                        IconButton(onClick = { onOpenPdf(context, lot.lotN) }) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = "Télécharger Rapport PDF Définitif",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    // L'icône enveloppe (Envoi d'e-mail) a été supprimée selon les directives
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Context Banner: Exclusive Lot indication & Totals
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FilterAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Affichage exclusif des ODD du lot : ${lot.lotN}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            val totalLotNonComptabilise = fraisList.filter { !it.estComptabilise }.sumOf { it.montant }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "TOTAL : %.3f TND".format(lot.totalCoutOdd),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (totalLotNonComptabilise > 0.0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(+%.3f TND non comptab.)".format(totalLotNonComptabilise),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = SlateLight
                                    )
                                }
                            }
                            Text(
                                text = "(${odds.size} ODD • Héberg: %.3f • Transp: %.3f)".format(
                                    lot.totalHebergement, lot.totalTransport
                                ),
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = SlateMedium
                            )
                            if (lot.detailDivers.isNotBlank()) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Détail divers : ${lot.detailDivers}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                            }
                        }

                        if (!lot.figerLot) {
                            if (isDgOrNahla) {
                                Button(
                                    onClick = { showFreezeConfirmDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberGold)
                                ) {
                                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Figer Lot 🔓", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        } else {
                            FilledTonalButton(
                                onClick = { onOpenPdf(context, lot.lotN) }
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = CobaltPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Lot Figé (PDF)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Tab Row (BASCULEMENT_VUES: "LES ODD", "FRAIS", "JUSTIFICATIFS", "VALIDATION")
            TabRow(
                selectedTabIndex = tabs.indexOf(effectiveActiveTab).coerceAtLeast(0),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEach { tabName ->
                    val isSelected = effectiveActiveTab == tabName
                    Tab(
                        selected = isSelected,
                        onClick = { onTabSelected(tabName) },
                        text = {
                            Text(
                                text = tabName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (effectiveActiveTab) {
                "LES ODD" -> {
                    OddsTabContent(
                        lot = lot,
                        odds = odds,
                        fraisList = fraisList,
                        documents = documents,
                        dateFormat = dateFormat,
                        onAddOddClick = onAddOddClick,
                        onDeleteOdd = onDeleteOdd,
                        onAddFraisForOdd = onAddFraisForOdd,
                        onAttachDocForOdd = onAttachDocForOdd,
                        onDeleteFrais = onDeleteFrais,
                        baremes = baremes,
                        coutsMission = coutsMission,
                        onToggleFraisComptabilise = onToggleFraisComptabilise,
                        isReadOnly = isReadOnly,
                        activeUserDir = userDir
                    )
                }
                "FRAIS" -> {
                    FraisTabContent(
                        lot = lot,
                        fraisList = fraisList,
                        odds = odds,
                        onAddFraisClick = onAddFraisClick,
                        onDeleteFrais = onDeleteFrais,
                        onToggleFraisComptabilise = onToggleFraisComptabilise,
                        isReadOnly = isReadOnly,
                        activeUserDir = userDir
                    )
                }
                "JUSTIFICATIFS" -> {
                    JustificatifsTabContent(
                        lot = lot,
                        documents = documents,
                        dateFormat = dateFormat,
                        onAttachDocClick = onAttachDocClick,
                        isReadOnly = isReadOnly,
                        activeUserDir = userDir
                    )
                }
                "VALIDATION" -> {
                    ValidationTabContent(
                        lot = lot,
                        validations = validations,
                        dateFormat = dateFormat,
                        onValidateClick = { dirCode -> selectedDirForValidation = dirCode },
                        onOpenPdf = { onOpenPdf(context, lot.lotN) },
                        onSendNotification = { onSendValidationEmail(lot.lotN) }
                    )
                }
                else -> {
                    OddsTabContent(
                        lot = lot,
                        odds = odds,
                        fraisList = fraisList,
                        documents = documents,
                        dateFormat = dateFormat,
                        onAddOddClick = onAddOddClick,
                        onDeleteOdd = onDeleteOdd,
                        onAddFraisForOdd = onAddFraisForOdd,
                        onAttachDocForOdd = onAttachDocForOdd,
                        onDeleteFrais = onDeleteFrais,
                        baremes = baremes,
                        coutsMission = coutsMission,
                        onToggleFraisComptabilise = onToggleFraisComptabilise,
                        isReadOnly = isReadOnly,
                        activeUserDir = userDir
                    )
                }
            }
        }
    }

    // Freeze Lot Confirmation Dialog
    if (showFreezeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showFreezeConfirmDialog = false },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AmberGold) },
            title = { Text("Figer le Lot ${lot.lotN} ?") },
            text = {
                Text(
                    "Le verrouillage (« Figer Lot ») bascule l'ensemble des ODD et frais en MODE LECTURE SEULE. " +
                            "Il génère automatiquement le rapport officiel PDF et déclenche la notification e-mail vers la Direction Maintenance (DM) pour visa hiérarchique.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFreezeConfirmDialog = false
                        onFigerLot(lot.lotN)
                        onTabSelected("VALIDATION")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberGold)
                ) {
                    Text("Oui, Figer et Transmettre")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFreezeConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Direction Validation Step Dialog
    if (selectedDirForValidation != null) {
        val dir = selectedDirForValidation!!
        ValidateStepDialog(
            lotN = lot.lotN,
            directionCode = dir,
            directionName = "Direction $dir",
            defaultSignataire = "Directeur $dir",
            onDismiss = { selectedDirForValidation = null },
            onConfirm = { isApproved, comment, signataire ->
                onValidateStep(lot.lotN, dir, isApproved, comment, signataire)
                selectedDirForValidation = null
            }
        )
    }
}

// -------------------------------------------------------------------------
// TAB 1: LES ODD (AVEC DÉPENSES ET PIÈCES DE RATTACHEMENT PAR ODD)
// -------------------------------------------------------------------------
@Composable
private fun OddsTabContent(
    lot: LotEntity,
    odds: List<OddEntity>,
    fraisList: List<FraisEntity>,
    documents: List<DocumentRattachementEntity>,
    dateFormat: SimpleDateFormat,
    onAddOddClick: () -> Unit,
    onDeleteOdd: (OddEntity) -> Unit,
    onAddFraisForOdd: (String) -> Unit,
    onAttachDocForOdd: (String) -> Unit,
    onDeleteFrais: (FraisEntity) -> Unit,
    baremes: List<BaremeTarifEntity> = emptyList(),
    coutsMission: List<com.example.data.local.CoutMissionEntity> = emptyList(),
    onToggleFraisComptabilise: (FraisEntity) -> Unit = {},
    isReadOnly: Boolean = lot.figerLot,
    activeUserDir: String = ""
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isReadOnly) {
            item {
                Button(
                    onClick = onAddOddClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ajouter un Ordre de Déplacement (ODD)")
                }
            }
        } else {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AviationNavy, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (activeUserDir.isNotBlank()) "🔒 Mode Lecture Seule • Direction $activeUserDir (Détails des ODD, frais et justificatifs en consultation)"
                            else "🔒 Lot figé : Les ODD sont en lecture seule (données non modifiables).",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AviationNavy
                        )
                    }
                }
            }
        }

        if (odds.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FlightTakeoff,
                            contentDescription = null,
                            tint = SlateLight,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Aucun ODD rattaché à ce lot.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Cliquez sur 'Ajouter un Ordre de Déplacement' pour affecter une mission.",
                            fontSize = 12.sp,
                            color = SlateLight
                        )
                    }
                }
            }
        } else {
            items(odds, key = { it.oddN }) { odd ->
                val oddFrais = fraisList.filter { it.oddN == odd.oddN }
                val oddDocs = documents.filter { it.oddN == odd.oddN }
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
                val costCalculation = SiteDistances.computeDetailedCoutMission(
                    delaisJours = dureeJours,
                    prixUnitaireMission = tauxMissionJournalier,
                    echelle = odd.echelle,
                    mission = odd.mission,
                    hasHebergementFrais = hasHebergementFrais
                )
                val coutMission = costCalculation.coutTotal
                val totalOddFraisComptabilises = oddFrais.filter { it.estComptabilise }.sumOf { it.montant }
                val totalOddFraisNonComptabilises = oddFrais.filter { !it.estComptabilise }.sumOf { it.montant }
                val totalBrutMission = coutMission + totalOddFraisComptabilises
                val netOddTotal = (totalBrutMission - odd.avanceSurMission).coerceAtLeast(0.0)

                var isExpanded by remember { mutableStateOf(isReadOnly || lot.figerLot) }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Header ODD
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                    Text(
                                        text = odd.oddN,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "%.3f TND".format(netOddTotal),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (totalOddFraisNonComptabilises > 0.0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "(+%.3f)".format(totalOddFraisNonComptabilises),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = SlateLight
                                        )
                                    }
                                }
                                Text(
                                    text = "Net à payer ODD",
                                    fontSize = 10.sp,
                                    color = SlateMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Intervenant & Trajet
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Technicien : ${odd.matricule} • Échelle ${odd.echelle}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FlightTakeoff, contentDescription = null, modifier = Modifier.size(15.dp), tint = SkyAccent)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${odd.siteProvenance} ➔ ${odd.siteIntervention}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AviationNavy)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(15.dp), tint = AmberGold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mission : ${odd.mission}", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Période : Du ${dateFormat.format(Date(odd.dateDebut))} au ${dateFormat.format(Date(odd.dateFin))} ($dureeJours j)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (odd.detailDivers.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Détail divers / Remarques :",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = odd.detailDivers,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Financial breakdown box for this ODD (Coût Mission selon échelle + Frais - Avance)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Coût Mission [${costCalculation.regleAppliquee}] :",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = costCalculation.formuleDescription,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("%.3f TND".format(coutMission), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Dépenses comptabilisées (${oddFrais.count { it.estComptabilise }}) :", fontSize = 11.sp)
                                    Text("+%.3f TND".format(totalOddFraisComptabilises), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                if (totalOddFraisNonComptabilises > 0.0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Dépenses non comptabilisées (${oddFrais.count { !it.estComptabilise }}) :", fontSize = 11.sp, color = SlateLight)
                                        Text("%.3f TND (déduit)".format(totalOddFraisNonComptabilises), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SlateLight)
                                    }
                                }
                                if (odd.avanceSurMission > 0.0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Avance sur mission :", fontSize = 11.sp, color = CrimsonRed)
                                        Text("-%.3f TND".format(odd.avanceSurMission), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CrimsonRed)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Badges summary for Frais & Docs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${oddFrais.size} Dépenses",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AmberLight,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(13.dp), tint = AmberGold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${oddDocs.size} Justificatif(s)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF78350F)
                                    )
                                }
                            }

                            // Toggle Expand Button
                            IconButton(
                                onClick = { isExpanded = !isExpanded },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Afficher les dépenses et pièces de rattachement",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // EXPANDED SECTION: Dépenses & Pièces de rattachement
                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // 1. SOUS-SECTION : DÉPENSES DE CET ODD
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💰 Dépenses rattachées (${oddFrais.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (!isReadOnly) {
                                    TextButton(
                                        onClick = { onAddFraisForOdd(odd.oddN) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("+ Dépense", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (oddFrais.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Aucune dépense saisie pour cet ODD.",
                                        fontSize = 11.sp,
                                        color = SlateMedium,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    oddFrais.forEach { f ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = when (f.categorie) {
                                                                "Hébergement" -> MaterialTheme.colorScheme.primaryContainer
                                                                "Transport" -> AmberLight
                                                                else -> Color(0xFFF3E8FF)
                                                            }
                                                        ) {
                                                            Text(
                                                                text = f.categorie,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = when (f.categorie) {
                                                                    "Hébergement" -> MaterialTheme.colorScheme.primary
                                                                    "Transport" -> AmberGold
                                                                    else -> Color(0xFF7E22CE)
                                                                },
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(f.sousCategorie, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "${f.quantite} ${f.unite} × %.3f TND".format(f.pUnit),
                                                        fontSize = 10.sp,
                                                        color = SlateMedium
                                                    )
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            text = "%.3f TND".format(f.montant),
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 12.sp,
                                                            color = if (f.estComptabilise) MaterialTheme.colorScheme.primary else SlateLight
                                                        )
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = if (f.estComptabilise) EmeraldLight else Color(0xFFFEE2E2),
                                                            modifier = Modifier.clickable(enabled = !isReadOnly) {
                                                                onToggleFraisComptabilise(f)
                                                            }
                                                        ) {
                                                            Text(
                                                                text = if (f.estComptabilise) "✓ Comptabilisé" else "✗ Non comptabilisé",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (f.estComptabilise) EmeraldGreen else CrimsonRed,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                    if (!isReadOnly) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        IconButton(
                                                            onClick = { onDeleteFrais(f) },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.DeleteOutline,
                                                                contentDescription = "Supprimer",
                                                                tint = CrimsonRed,
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 2. SOUS-SECTION : PIÈCES DE RATTACHEMENT DE CET ODD
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📎 Pièces de rattachement (${oddDocs.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = AviationNavy
                                )
                                if (!isReadOnly) {
                                    TextButton(
                                        onClick = { onAttachDocForOdd(odd.oddN) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("+ Pièce jointe", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (oddDocs.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Aucune pièce justificative rattachée à cet ODD.",
                                        fontSize = 11.sp,
                                        color = SlateMedium,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    oddDocs.forEach { doc ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.ReceiptLong,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(doc.typeDocument, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Text("Objet : ${doc.objet}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text("Date : ${dateFormat.format(Date(doc.dateEnvoie))}", fontSize = 9.sp, color = SlateLight)
                                                }
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Scan certifié",
                                                    tint = EmeraldGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Actions: Supprimer ODD si non figé
                        if (!isReadOnly) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { isExpanded = !isExpanded }) {
                                    Text(
                                        text = if (isExpanded) "Masquer détails ▲" else "Afficher détails & pièces ▼",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                TextButton(
                                    onClick = { onDeleteOdd(odd) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = CrimsonRed)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Supprimer ODD", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// TAB 2: FRAIS
// -------------------------------------------------------------------------
@Composable
private fun FraisTabContent(
    lot: LotEntity,
    fraisList: List<FraisEntity>,
    odds: List<OddEntity>,
    onAddFraisClick: () -> Unit,
    onDeleteFrais: (FraisEntity) -> Unit,
    onToggleFraisComptabilise: (FraisEntity) -> Unit = {},
    isReadOnly: Boolean = lot.figerLot,
    activeUserDir: String = ""
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!isReadOnly) {
            item {
                Button(
                    onClick = onAddFraisClick,
                    enabled = odds.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PostAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (odds.isNotEmpty()) "Ajouter une Ligne de Frais" else "Ajoutez d'abord un ODD")
                }
            }
        } else {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AviationNavy, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (activeUserDir.isNotBlank()) "🔒 Mode Lecture Seule • Direction $activeUserDir (Détails des frais en consultation stricte)"
                            else "🔒 Lot figé : Les frais sont en lecture seule (données non modifiables).",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AviationNavy
                        )
                    }
                }
            }
        }

        if (fraisList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Aucun frais saisi pour ce lot.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(fraisList, key = { it.idFrais }) { f ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = f.oddN,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = f.categorie,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = AmberGold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = f.sousCategorie,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${f.quantite} ${f.unite} × %.3f TND".format(f.pUnit),
                                fontSize = 11.sp,
                                color = SlateMedium
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "%.3f TND".format(f.montant),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = if (f.estComptabilise) MaterialTheme.colorScheme.primary else SlateLight
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (f.estComptabilise) EmeraldLight else Color(0xFFFEE2E2),
                                modifier = Modifier.clickable(enabled = !isReadOnly) {
                                    onToggleFraisComptabilise(f)
                                }
                            ) {
                                Text(
                                    text = if (f.estComptabilise) "✓ Comptabilisé" else "✗ Non comptabilisé",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (f.estComptabilise) EmeraldGreen else CrimsonRed,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            if (!isReadOnly) {
                                Spacer(modifier = Modifier.height(2.dp))
                                IconButton(onClick = { onDeleteFrais(f) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = CrimsonRed, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// TAB 3: JUSTIFICATIFS (DOCUMENTS RATTACHEMENT)
// -------------------------------------------------------------------------
@Composable
private fun JustificatifsTabContent(
    lot: LotEntity,
    documents: List<DocumentRattachementEntity>,
    dateFormat: SimpleDateFormat,
    onAttachDocClick: () -> Unit,
    isReadOnly: Boolean = lot.figerLot,
    activeUserDir: String = ""
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!isReadOnly) {
            item {
                Button(
                    onClick = onAttachDocClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rattacher une Pièce Justificative (Scan/Photo)")
                }
            }
        } else {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AviationNavy, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (activeUserDir.isNotBlank()) "🔒 Mode Lecture Seule • Direction $activeUserDir (Détails des justificatifs en consultation stricte)"
                            else "🔒 Lot figé : Les pièces justificatives sont en lecture seule (données non modifiables).",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AviationNavy
                        )
                    }
                }
            }
        }

        if (documents.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = SlateLight, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Aucun justificatif rattaché à ce lot.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Factures d'hôtel, tickets autoroute, billets de train/avion...",
                            fontSize = 11.sp,
                            color = SlateLight
                        )
                    }
                }
            }
        } else {
            items(documents, key = { it.idDoc }) { doc ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(doc.typeDocument, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Objet : ${doc.objet}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Rattaché à l'ODD : ${doc.oddN} • Le ${dateFormat.format(Date(doc.dateEnvoie))}", fontSize = 11.sp, color = SlateMedium)
                        }

                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// TAB 4: VALIDATION HIÉRARCHIQUE & ARCHIVES
// -------------------------------------------------------------------------
@Composable
private fun ValidationTabContent(
    lot: LotEntity,
    validations: List<ValidationEntity>,
    dateFormat: SimpleDateFormat,
    onValidateClick: (String) -> Unit,
    onOpenPdf: () -> Unit,
    onSendNotification: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DirectionTimeline(
                validations = validations,
                currentDirection = lot.directionActuelle
            )
        }

        // Active Direction Action Prompt
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (lot.directionActuelle == "ARCHIVE") EmeraldLight else AmberLight
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (lot.directionActuelle == "ARCHIVE") Icons.Default.Verified else Icons.Default.PendingActions,
                            contentDescription = null,
                            tint = if (lot.directionActuelle == "ARCHIVE") EmeraldGreen else AmberGold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (lot.directionActuelle == "ARCHIVE") "Dossier Validé & Archivé Définitivement (DAF)" else "Étape Actuelle : ${lot.directionActuelle}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (lot.directionActuelle == "ARCHIVE") EmeraldGreen else Color(0xFF78350F)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (lot.directionActuelle != "ARCHIVE") {
                        Text(
                            text = "Ce lot est en attente du visa de la direction ${lot.directionActuelle}. " +
                                    "Consultez le PDF et enregistrez la décision d'approbation ou de rejet.",
                            fontSize = 12.sp,
                            color = Color(0xFF78350F)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val activeUser by com.example.data.auth.AuthManager.currentUser.collectAsStateWithLifecycle()
                            val isNahla = activeUser?.displayName?.contains("Nahla", ignoreCase = true) == true ||
                                    activeUser?.role?.contains("Gestion", ignoreCase = true) == true
                            val canViser = isNahla || (activeUser?.dirCode == lot.directionActuelle)

                            Button(
                                onClick = { if (canViser) onValidateClick(lot.directionActuelle) },
                                enabled = canViser,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldGreen,
                                    disabledContainerColor = Color(0xFFE2E8F0),
                                    disabledContentColor = SlateMedium
                                ),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (canViser) "Viser / Approuver (${lot.directionActuelle})" else "Viser réservé à ${lot.directionActuelle}",
                                    fontSize = 11.5.sp
                                )
                            }

                            if (lot.figerLot) {
                                OutlinedButton(
                                    onClick = onOpenPdf,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Voir PDF", fontSize = 12.sp)
                                }
                            }
                        }

                        if (lot.figerLot) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FilledTonalButton(
                                onClick = onSendNotification,
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Notifier ${lot.directionActuelle} (E-mail Officiel + Alerte WhatsApp)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Référence Archive : ARC-2026-${lot.lotN.takeLast(4)} • Toutes les 7 directions ont validé le dossier.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldGreen
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Historique & Visas des Directions",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        items(validations.sortedBy { it.ordre }) { valStep ->
            val isValidated = valStep.valide == "VALIDE"
            val isRejected = valStep.valide == "REFUSE"

            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isValidated -> EmeraldLight
                                    isRejected -> CrimsonLight
                                    else -> Color(0xFFF1F5F9)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = valStep.direction,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = when {
                                isValidated -> EmeraldGreen
                                isRejected -> CrimsonRed
                                else -> SlateMedium
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Direction ${valStep.direction}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (valStep.reactiviteJours > 0) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "Délai: ${valStep.reactiviteJours} j",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (isValidated) {
                            Text(
                                text = "Signé par : ${valStep.signataire} le ${valStep.dateValidation?.let { dateFormat.format(Date(it)) } ?: ""}",
                                fontSize = 11.sp,
                                color = SlateMedium
                            )
                            if (valStep.commentaire.isNotBlank()) {
                                Text("« ${valStep.commentaire} »", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else if (isRejected) {
                            Text("Rejeté : ${valStep.commentaire}", fontSize = 11.sp, color = CrimsonRed)
                        } else {
                            Text("En attente de transmission / signature", fontSize = 11.sp, color = SlateLight)
                        }
                    }

                    Icon(
                        imageVector = when {
                            isValidated -> Icons.Default.CheckCircle
                            isRejected -> Icons.Default.Cancel
                            else -> Icons.Default.HourglassEmpty
                        },
                        contentDescription = null,
                        tint = when {
                            isValidated -> EmeraldGreen
                            isRejected -> CrimsonRed
                            else -> SlateLight
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
