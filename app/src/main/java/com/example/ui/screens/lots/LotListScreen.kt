package com.example.ui.screens.lots

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.DocumentRattachementEntity
import com.example.data.local.FraisEntity
import com.example.data.local.LotEntity
import com.example.data.local.OddEntity
import com.example.data.local.ValidationEntity
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotListScreen(
    lots: List<LotEntity>,
    allOdds: List<OddEntity> = emptyList(),
    allFrais: List<FraisEntity> = emptyList(),
    allDocs: List<DocumentRattachementEntity> = emptyList(),
    allValidations: List<ValidationEntity> = emptyList(),
    onSelectLot: (String) -> Unit,
    onCreateLotClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeUser by com.example.data.auth.AuthManager.currentUser.collectAsStateWithLifecycle()
    val isDg = activeUser?.dirCode == "DG" || activeUser?.role?.contains("Générale", ignoreCase = true) == true
    val isNahla = activeUser?.displayName?.contains("Nahla", ignoreCase = true) == true ||
            activeUser?.role?.contains("Gestion", ignoreCase = true) == true
    val isDgOrNahla = isDg || isNahla
    val userDir = activeUser?.dirCode ?: ""

    // Filtrage des lots selon le profil :
    // - DG et Mme Nahla Zaoui visualisent tous les lots (En cours, Figés, Archivés, Tous)
    // - Directions hiérarchiques connectées (DM, DCT, DGRT, DCST, AUDIT, DAF) :
    //   Affichent TOUS les lots figés en mode lecture seule (ODD, Frais, Justificatifs) et n'affichent jamais les lots en édition
    val baseLots = remember(lots, isDgOrNahla) {
        if (isDgOrNahla) {
            lots
        } else {
            lots.filter { it.figerLot }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Tous") }
    var currentViewMode by remember { mutableStateOf("Lots") } // "Lots" or "ODD par Lot"
    var selectedLotFilterForOdds by remember { mutableStateOf(baseLots.firstOrNull()?.lotN ?: "") }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)

    // Update selectedLotFilterForOdds if empty and lots become available
    LaunchedEffect(baseLots) {
        if (selectedLotFilterForOdds.isBlank() && baseLots.isNotEmpty()) {
            selectedLotFilterForOdds = baseLots.first().lotN
        }
    }

    val filteredLots = baseLots.filter { lot ->
        val matchesQuery = lot.lotN.contains(searchQuery, ignoreCase = true) ||
                lot.libelle.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "En cours" -> !lot.figerLot && !lot.statutValidation.startsWith("VALIDE")
            "Figés" -> lot.figerLot && lot.statutValidation != "ARCHIVE"
            "Archivés" -> lot.statutValidation == "ARCHIVE"
            else -> true
        }
        matchesQuery && matchesFilter
    }

    Scaffold(
        floatingActionButton = {
            if (isDgOrNahla) {
                ExtendedFloatingActionButton(
                    onClick = onCreateLotClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nouveau Lot", fontWeight = FontWeight.Bold) }
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Hero Banner Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner),
                        contentDescription = "Bannière zODD",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AviationNavy.copy(alpha = 0.78f))
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "zODD V.2-26",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "by msc",
                                color = IceBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Organisation des ODD, calcul automatique et suivi des dépenses",
                            color = IceBlue,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val figeLots = lots.filter { it.figerLot }
                        val totalFige = figeLots.sumOf { it.totalCoutOdd }
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${figeLots.size} Lot(s) figé(s)",
                                color = AmberGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Total : %.3f TND".format(totalFige),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // View Mode Switcher: "Lots de mission" vs "ODD filtrés par lot"
            item {
                TabRow(
                    selectedTabIndex = if (currentViewMode == "Lots") 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = currentViewMode == "Lots",
                        onClick = { currentViewMode = "Lots" },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lots de Mission (${lots.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                    Tab(
                        selected = currentViewMode == "ODD par Lot",
                        onClick = { currentViewMode = "ODD par Lot" },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ODD d'un Lot Donné", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                }
            }

            // VIEW 1: LOTS LIST
            if (currentViewMode == "Lots") {
                // Search & Filter Bar
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Rechercher un lot (TAT-..., libellé)...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Effacer")
                                    }
                                }
                            } else null,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val availableFilters = if (isDgOrNahla) listOf("Tous", "En cours", "Figés", "Archivés") else listOf("Tous", "Figés", "Archivés")

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            availableFilters.forEach { filter ->
                                FilterChip(
                                    selected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter },
                                    label = { Text(filter, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }

                if (filteredLots.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = SlateLight,
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Aucun lot trouvé",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Cliquez sur '+ Nouveau Lot' pour ouvrir une session de mission.",
                                    fontSize = 12.sp,
                                    color = SlateLight
                                )
                            }
                        }
                    }
                } else {
                    items(filteredLots, key = { it.lotN }) { lot ->
                        val lotOdds = allOdds.filter { it.lotN == lot.lotN }
                        val lotOddIds = lotOdds.map { it.oddN }.toSet()
                        val lotFrais = allFrais.filter { it.oddN in lotOddIds }
                        val lotDocs = allDocs.filter { it.oddN in lotOddIds }
                        LotCardItem(
                            lot = lot,
                            odds = lotOdds,
                            fraisList = lotFrais,
                            documents = lotDocs,
                            dateFormat = dateFormat,
                            isReadOnly = !isDgOrNahla || lot.figerLot,
                            userDir = userDir,
                            onClick = { onSelectLot(lot.lotN) }
                        )
                    }
                }
            } else {
                // VIEW 2: EXCLUSIVELY DISPLAY ODDS FOR A SELECTED LOT
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sélectionnez un Lot pour n'afficher que ses ODD :",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Lot selection chips (baseLots: tous les lots pour DG/Nahla, uniquement lots figés pour directions)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(baseLots, key = { it.lotN }) { l ->
                                val isSelected = l.lotN == selectedLotFilterForOdds
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedLotFilterForOdds = l.lotN },
                                    label = {
                                        Text("${l.lotN} (${allOdds.count { it.lotN == l.lotN }} ODD)")
                                    },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }

                        val activeSelectedLot = baseLots.find { it.lotN == selectedLotFilterForOdds } ?: baseLots.firstOrNull()
                        if (activeSelectedLot != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Lot ${activeSelectedLot.lotN} : ${activeSelectedLot.libelle}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Total : %.3f TND • Dir : ${activeSelectedLot.directionActuelle}".format(activeSelectedLot.totalCoutOdd),
                                            fontSize = 11.sp,
                                            color = SlateMedium
                                        )
                                    }
                                    Button(
                                        onClick = { onSelectLot(activeSelectedLot.lotN) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("Ouvrir ce Lot", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                if (!isDgOrNahla) {
                    item {
                        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f || MaterialTheme.colorScheme.background.luminance() < 0.5f
                        val readOnlyTextColor = if (isDark) Color.White else Color.Black
                        val readOnlyBgColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                        val readOnlyBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = readOnlyBgColor,
                            border = BorderStroke(1.dp, readOnlyBorderColor),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = readOnlyTextColor, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "🔒 Mode Lecture Seule • Direction $userDir (Détails ODD, Frais et Justificatifs consultables)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = readOnlyTextColor
                                )
                            }
                        }
                    }
                }

                val oddsForThisLot = allOdds.filter { it.lotN == selectedLotFilterForOdds }

                if (oddsForThisLot.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aucun ODD rattaché au lot $selectedLotFilterForOdds.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(oddsForThisLot, key = { it.oddN }) { odd ->
                        val oddFrais = allFrais.filter { it.oddN == odd.oddN }
                        val oddDocs = allDocs.filter { it.oddN == odd.oddN }
                        var isExpanded by remember { mutableStateOf(true) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = odd.oddN,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    Text(
                                        text = "%.3f TND".format(odd.totalOdd),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Technicien : ${odd.matricule} • ${odd.siteProvenance} ➔ ${odd.siteIntervention}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Mission : ${odd.mission}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("💰 ${oddFrais.size} Dépenses", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AviationNavy)
                                        Text("📎 ${oddDocs.size} Pièces", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                                    }

                                    TextButton(onClick = { isExpanded = !isExpanded }) {
                                        Text(if (isExpanded) "Masquer ▲" else "Voir Dépenses & Pièces ▼", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (isExpanded) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                    Text("Dépenses de cet ODD :", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    if (oddFrais.isEmpty()) {
                                        Text("Aucun frais enregistré.", fontSize = 11.sp, color = SlateLight)
                                    } else {
                                        oddFrais.forEach { f ->
                                            Text(
                                                text = "• ${f.categorie} (${f.sousCategorie}) : ${f.quantite} ${f.unite} × %.3f = %.3f TND".format(f.pUnit, f.aComptabiliser),
                                                fontSize = 11.sp,
                                                color = SlateMedium
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text("Pièces de rattachement :", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AmberGold)
                                    if (oddDocs.isEmpty()) {
                                        Text("Aucun justificatif rattaché.", fontSize = 11.sp, color = SlateLight)
                                    } else {
                                        oddDocs.forEach { d ->
                                            Text(
                                                text = "• ${d.typeDocument} : ${d.objet}",
                                                fontSize = 11.sp,
                                                color = SlateMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LotCardItem(
    lot: LotEntity,
    odds: List<OddEntity> = emptyList(),
    fraisList: List<FraisEntity> = emptyList(),
    documents: List<DocumentRattachementEntity> = emptyList(),
    dateFormat: SimpleDateFormat,
    isReadOnly: Boolean = false,
    userDir: String = "",
    onClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f || MaterialTheme.colorScheme.background.luminance() < 0.5f
    val readOnlyTextColor = if (isDark) Color.White else Color.Black
    val readOnlyBgColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val readOnlyBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = { isExpanded = !isExpanded }),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = lot.lotN,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateFormat.format(Date(lot.dateCreation)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(status = lot.statutValidation, isFige = lot.figerLot)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = lot.libelle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )

            if (lot.detailDivers.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Détail : ${lot.detailDivers}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsTransit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${lot.totalMission.toInt()} ODD",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = AmberGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Dir: ${lot.directionActuelle}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = "%.3f TND".format(lot.totalCoutOdd),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // BOUTON DIRECT DE CONSULTATION DU LOT (ACCORDÉON SUR PLACE)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isExpanded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isExpanded) "Masquer les Détails du Dossier ▲"
                    else if (lot.figerLot) "Consulter les Détails (ODD, Frais, Justifs) ▼"
                    else "Ouvrir & Consulter le Dossier ▼",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))

                // Notice mode lecture seule ou gestion
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = readOnlyBgColor,
                    border = BorderStroke(1.dp, readOnlyBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (lot.figerLot) Icons.Default.Lock else Icons.Default.Edit,
                            contentDescription = null,
                            tint = readOnlyTextColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lot.figerLot) {
                                if (userDir.isNotBlank()) "🔒 Mode Lecture Seule • Direction $userDir (Détails complets en consultation)"
                                else "🔒 Lot figé : Consultation intégrale en lecture seule."
                            } else {
                                "✏️ Dossier en cours de gestion • Direction ${lot.directionActuelle}"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = readOnlyTextColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // DÉTAILS DES ODD DU LOT
                Text(
                    text = "📋 Ordres de Déplacement rattachés (${odds.size}) :",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (odds.isEmpty()) {
                        Text("Aucun ODD rattaché à ce lot.", fontSize = 11.sp, color = SlateLight)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            odds.forEach { odd ->
                                val oddFrais = fraisList.filter { it.oddN == odd.oddN }
                                val oddDocs = documents.filter { it.oddN == odd.oddN }
                                val netOdd = (odd.totalOdd - odd.avanceSurMission).coerceAtLeast(0.0)

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        text = odd.oddN,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Tech: ${odd.matricule} (Éch. ${odd.echelle})",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = "%.3f TND".format(netOdd),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Trajet : ${odd.siteProvenance} ➔ ${odd.siteIntervention} • Mission : ${odd.mission}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Période : Du ${dateFormat.format(Date(odd.dateDebut))} au ${dateFormat.format(Date(odd.dateFin))}",
                                            fontSize = 10.sp,
                                            color = SlateMedium
                                        )

                                        // FRAIS DE CET ODD
                                        if (oddFrais.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "💰 Frais rattachés (${oddFrais.size}) :",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AmberGold
                                            )
                                            oddFrais.forEach { f ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "• ${f.categorie} (${f.sousCategorie}) : ${f.quantite} ${f.unite} × %.3f".format(f.pUnit),
                                                        fontSize = 10.sp,
                                                        color = if (f.estComptabilise) MaterialTheme.colorScheme.onSurface else SlateLight,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Text(
                                                        text = "%.3f TND %s".format(f.montant, if (f.estComptabilise) "✓" else "(non comptab.)"),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (f.estComptabilise) MaterialTheme.colorScheme.primary else SlateLight
                                                    )
                                                }
                                            }
                                        }

                                        // JUSTIFICATIFS DE CET ODD
                                        if (oddDocs.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "📎 Justificatifs certifiés (${oddDocs.size}) :",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldGreen
                                            )
                                            oddDocs.forEach { d ->
                                                Text(
                                                    text = "• ${d.typeDocument} : ${d.objet} (${dateFormat.format(Date(d.dateEnvoie))})",
                                                    fontSize = 10.sp,
                                                    color = SlateMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lot.figerLot) "Dossier Complet en Plein Écran ➔" else "Gérer le Dossier en Plein Écran ➔",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(onClick = { isExpanded = false }) {
                        Text("Replier ▲", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun LotReadOnlyDetailsDialog(
    lot: LotEntity,
    odds: List<OddEntity>,
    fraisList: List<FraisEntity>,
    documents: List<DocumentRattachementEntity>,
    dateFormat: SimpleDateFormat,
    userDir: String,
    onDismiss: () -> Unit,
    onOpenFullDetail: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f || MaterialTheme.colorScheme.background.luminance() < 0.5f
    val readOnlyTextColor = if (isDark) Color.White else Color.Black
    val readOnlyBgColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val readOnlyBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lot ${lot.lotN}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    StatusBadge(status = lot.statutValidation, isFige = lot.figerLot)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = lot.libelle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                // Bandeau Mode Lecture Seule
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = readOnlyBgColor,
                    border = BorderStroke(1.dp, readOnlyBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = readOnlyTextColor, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (userDir.isNotBlank()) "🔒 Mode Lecture Seule • Direction $userDir"
                            else "🔒 Lot figé : Consultation intégrale en lecture seule",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = readOnlyTextColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Métriques financières du lot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total : %.3f TND".format(lot.totalCoutOdd),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Dir : ${lot.directionActuelle}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Onglets de navigation : ODD, Frais, Justificatifs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "ODD (${odds.size})",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Frais (${fraisList.size})",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                "Justifs (${documents.size})",
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Contenu de l'onglet actif
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    when (selectedTab) {
                        0 -> {
                            if (odds.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("Aucun ODD rattaché à ce lot.", fontSize = 12.sp, color = SlateLight)
                                }
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(odds, key = { it.oddN }) { odd ->
                                        val oddFrais = fraisList.filter { it.oddN == odd.oddN }
                                        val oddDocs = documents.filter { it.oddN == odd.oddN }
                                        val netOdd = (odd.totalOdd - odd.avanceSurMission).coerceAtLeast(0.0)

                                        Card(
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.primaryContainer
                                                    ) {
                                                        Text(
                                                            text = odd.oddN,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = "%.3f TND".format(netOdd),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Tech : ${odd.matricule} • Échelle ${odd.echelle}",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp
                                                )
                                                Text(
                                                    text = "Trajet : ${odd.siteProvenance} ➔ ${odd.siteIntervention}",
                                                    fontSize = 11.sp
                                                )
                                                Text(
                                                    text = "Mission : ${odd.mission}",
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "Période : Du ${dateFormat.format(Date(odd.dateDebut))} au ${dateFormat.format(Date(odd.dateFin))}",
                                                    fontSize = 10.sp,
                                                    color = SlateMedium
                                                )
                                                if (oddFrais.isNotEmpty() || oddDocs.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        if (oddFrais.isNotEmpty()) {
                                                            Text("💰 ${oddFrais.size} frais", fontSize = 10.sp, color = AmberGold, fontWeight = FontWeight.Bold)
                                                        }
                                                        if (oddDocs.isNotEmpty()) {
                                                            Text("📎 ${oddDocs.size} docs", fontSize = 10.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            if (fraisList.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("Aucun frais enregistré pour ce lot.", fontSize = 12.sp, color = SlateLight)
                                }
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(fraisList, key = { it.idFrais }) { f ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
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
                                                        Text("ODD ${f.oddN}", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Text(
                                                        text = "${f.sousCategorie} : ${f.quantite} ${f.unite} × %.3f".format(f.pUnit),
                                                        fontSize = 10.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    text = "%.3f TND %s".format(f.montant, if (f.estComptabilise) "✓" else "(non c.)"),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (f.estComptabilise) MaterialTheme.colorScheme.primary else SlateLight
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            if (documents.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("Aucune pièce justificative rattachée.", fontSize = 12.sp, color = SlateLight)
                                }
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(documents, key = { it.idDoc }) { d ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(d.typeDocument, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = EmeraldGreen)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("• ODD ${d.oddN}", fontSize = 10.sp, color = SlateMedium)
                                                    }
                                                    Text(d.objet, fontSize = 10.5.sp)
                                                    Text(
                                                        "Reçu le : ${dateFormat.format(Date(d.dateEnvoie))}",
                                                        fontSize = 9.5.sp,
                                                        color = SlateMedium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onOpenFullDetail()
                }
            ) {
                Text("Ouvrir Dossier Plein Écran ➔", fontSize = 11.5.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}
