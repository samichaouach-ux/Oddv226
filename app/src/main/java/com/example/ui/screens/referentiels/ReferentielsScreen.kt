package com.example.ui.screens.referentiels

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.data.local.BaremeTarifEntity
import com.example.data.local.CoutMissionEntity
import com.example.data.local.DirectionEntity
import com.example.data.local.TechnicienEntity
import com.example.ui.screens.manual.UserManualScreen
import com.example.ui.theme.*
import com.example.util.SiteDistances

@Composable
fun ReferentielsScreen(
    techniciens: List<TechnicienEntity>,
    directions: List<DirectionEntity>,
    baremes: List<BaremeTarifEntity>,
    coutsMission: List<CoutMissionEntity> = emptyList(),
    onSaveTechnicien: (TechnicienEntity) -> Unit,
    onDeleteTechnicien: (TechnicienEntity) -> Unit,
    onSaveDirection: (DirectionEntity) -> Unit,
    onDeleteDirection: (DirectionEntity) -> Unit = {},
    onSaveBareme: (BaremeTarifEntity) -> Unit,
    onDeleteBareme: (BaremeTarifEntity) -> Unit,
    onSaveCoutMission: (CoutMissionEntity) -> Unit = {},
    onDeleteCoutMission: (CoutMissionEntity) -> Unit = {},
    onUpdateTechniciens: () -> Unit = {},
    onUpdateDirections: () -> Unit = {},
    onUpdateCoutsMission: () -> Unit = {},
    onUpdateBaremes: () -> Unit = {},
    onResetDefaults: () -> Unit,
    onResetTechniciensWorkload: () -> Unit = {},
    onOpenThemeSelector: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tabs = listOf("Manuel d'Utilisation", "Techniciens", "Directions", "Coûts Missions", "Barèmes", "Installation & App")
    var selectedTab by remember { mutableStateOf(tabs.first()) }

    // Dialog states
    var technicienToEdit by remember { mutableStateOf<TechnicienEntity?>(null) }
    var showTechDialog by remember { mutableStateOf(false) }
    var techToDelete by remember { mutableStateOf<TechnicienEntity?>(null) }

    var directionToEdit by remember { mutableStateOf<DirectionEntity?>(null) }
    var showDirDialog by remember { mutableStateOf(false) }
    var directionToDelete by remember { mutableStateOf<DirectionEntity?>(null) }

    var coutMissionToEdit by remember { mutableStateOf<CoutMissionEntity?>(null) }
    var showCoutMissionDialog by remember { mutableStateOf(false) }
    var coutMissionToDelete by remember { mutableStateOf<CoutMissionEntity?>(null) }

    var baremeToEdit by remember { mutableStateOf<BaremeTarifEntity?>(null) }
    var showBaremeDialog by remember { mutableStateOf(false) }
    var baremeToDelete by remember { mutableStateOf<BaremeTarifEntity?>(null) }

    var showResetConfirmDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = tabs.indexOf(selectedTab),
            edgePadding = 8.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabs.forEach { tabTitle ->
                val isSelected = selectedTab == tabTitle
                Tab(
                    selected = isSelected,
                    onClick = { selectedTab = tabTitle },
                    text = {
                        Text(
                            text = tabTitle,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            "Manuel d'Utilisation" -> UserManualScreen()
            "Techniciens" -> TechniciensTab(
                techniciens = techniciens,
                onAddClick = {
                    technicienToEdit = null
                    showTechDialog = true
                },
                onEditClick = { tech ->
                    technicienToEdit = tech
                    showTechDialog = true
                },
                onDeleteClick = { tech ->
                    techToDelete = tech
                },
                onUpdateClick = onUpdateTechniciens,
                onResetWorkloadClick = onResetTechniciensWorkload
            )
            "Directions" -> DirectionsTab(
                directions = directions,
                onEditClick = { dir ->
                    directionToEdit = dir
                    showDirDialog = true
                },
                onDeleteClick = { dir ->
                    directionToDelete = dir
                },
                onUpdateClick = onUpdateDirections
            )
            "Coûts Missions" -> CoutsMissionTab(
                coutsMission = coutsMission,
                onAddClick = {
                    coutMissionToEdit = null
                    showCoutMissionDialog = true
                },
                onEditClick = { cout ->
                    coutMissionToEdit = cout
                    showCoutMissionDialog = true
                },
                onDeleteClick = { cout ->
                    coutMissionToDelete = cout
                },
                onUpdateClick = onUpdateCoutsMission
            )
            "Barèmes" -> BaremesTab(
                baremes = baremes,
                onAddClick = {
                    baremeToEdit = null
                    showBaremeDialog = true
                },
                onEditClick = { b ->
                    baremeToEdit = b
                    showBaremeDialog = true
                },
                onDeleteClick = { b ->
                    baremeToDelete = b
                },
                onUpdateClick = onUpdateBaremes
            )
            "Installation & App" -> InstallationAndSettingsTab(
                onResetDefaultsClick = { showResetConfirmDialog = true },
                onOpenThemeSelector = onOpenThemeSelector
            )
        }
    }

    // Modal Dialogs
    if (showTechDialog) {
        EditTechnicienDialog(
            initialTechnicien = technicienToEdit,
            onDismiss = { showTechDialog = false },
            onSave = { tech ->
                showTechDialog = false
                onSaveTechnicien(tech)
            }
        )
    }

    if (techToDelete != null) {
        AlertDialog(
            onDismissRequest = { techToDelete = null },
            title = { Text("Supprimer Technicien", fontWeight = FontWeight.Bold) },
            text = { Text("Voulez-vous vraiment supprimer le technicien ${techToDelete?.matricule} (${techToDelete?.nom} ${techToDelete?.prenom}) ?") },
            confirmButton = {
                Button(
                    onClick = {
                        techToDelete?.let { onDeleteTechnicien(it) }
                        techToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { techToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (showDirDialog && directionToEdit != null) {
        EditDirectionDialog(
            direction = directionToEdit!!,
            onDismiss = { showDirDialog = false },
            onSave = { updatedDir ->
                showDirDialog = false
                onSaveDirection(updatedDir)
            }
        )
    }

    if (directionToDelete != null) {
        AlertDialog(
            onDismissRequest = { directionToDelete = null },
            title = { Text("Supprimer Direction", fontWeight = FontWeight.Bold) },
            text = { Text("Voulez-vous vraiment supprimer la direction ${directionToDelete?.code} (${directionToDelete?.libelle}) ?") },
            confirmButton = {
                Button(
                    onClick = {
                        directionToDelete?.let { onDeleteDirection(it) }
                        directionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { directionToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (showBaremeDialog) {
        EditBaremeDialog(
            initialBareme = baremeToEdit,
            onDismiss = { showBaremeDialog = false },
            onSave = { b ->
                showBaremeDialog = false
                onSaveBareme(b)
            }
        )
    }

    if (showCoutMissionDialog) {
        EditCoutMissionDialog(
            initialCout = coutMissionToEdit,
            onDismiss = { showCoutMissionDialog = false },
            onSave = { c ->
                showCoutMissionDialog = false
                onSaveCoutMission(c)
            }
        )
    }

    if (coutMissionToDelete != null) {
        AlertDialog(
            onDismissRequest = { coutMissionToDelete = null },
            title = { Text("Supprimer Coût Unitaire Mission", fontWeight = FontWeight.Bold) },
            text = { Text("Voulez-vous supprimer le tarif pour la mission \"${coutMissionToDelete?.typeMission}\" (Échelle ${coutMissionToDelete?.echelleMin}-${coutMissionToDelete?.echelleMax}) de ${coutMissionToDelete?.coutUnitaireJournalier} TND/jour ?") },
            confirmButton = {
                Button(
                    onClick = {
                        coutMissionToDelete?.let { onDeleteCoutMission(it) }
                        coutMissionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { coutMissionToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (baremeToDelete != null) {
        AlertDialog(
            onDismissRequest = { baremeToDelete = null },
            title = { Text("Supprimer Barème", fontWeight = FontWeight.Bold) },
            text = { Text("Voulez-vous supprimer le barème \"${baremeToDelete?.designation}\" (${baremeToDelete?.tarifUnitaire} TND) ?") },
            confirmButton = {
                Button(
                    onClick = {
                        baremeToDelete?.let { onDeleteBareme(it) }
                        baremeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { baremeToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Restaurer les Référentiels d'Usine", fontWeight = FontWeight.Bold) },
            text = { Text("Cette action va réinitialiser l'annuaire des 7 directions, la liste des techniciens qualifiés et les barèmes tarifaires officiels de Tunisair Technics à leurs valeurs par défaut. Continuer ?") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        onResetDefaults()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    Text("Confirmer la Réinitialisation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun TechniciensTab(
    techniciens: List<TechnicienEntity>,
    onAddClick: () -> Unit,
    onEditClick: (TechnicienEntity) -> Unit,
    onDeleteClick: (TechnicienEntity) -> Unit,
    onUpdateClick: () -> Unit = {},
    onResetWorkloadClick: () -> Unit = {}
) {
    var showFullMatrix by remember { mutableStateOf(false) }
    var matrixSelectedProvenance by remember { mutableStateOf("Tous") }
    var showResetWorkloadDialog by remember { mutableStateOf(false) }

    if (showResetWorkloadDialog) {
        AlertDialog(
            onDismissRequest = { showResetWorkloadDialog = false },
            icon = { Icon(Icons.Default.RestartAlt, contentDescription = null, tint = AmberGold, modifier = Modifier.size(28.dp)) },
            title = { Text("Réinitialiser les charges de travail ?") },
            text = {
                Text(
                    "Cette opération remet à zéro les compteurs d'activité de l'ensemble des techniciens (nombre cumulé des missions et total des jours d'intervention / coûts associés).\n\n" +
                            "Les fiches administratives (matricules MAT-, noms, spécialités, échelons indiciaires et coordonnées bancaires) restent strictement inchangées.\n\n" +
                            "Voulez-vous confirmer cette réinitialisation générale ?",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetWorkloadDialog = false
                        onResetWorkloadClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberGold)
                ) {
                    Text("Oui, Réinitialiser à Zéro", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetWorkloadDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Référentiel des Techniciens (${techniciens.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Affectations de base, échelons indiciaires et distances vers les sites",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { showResetWorkloadDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold),
                        border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.7f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RàZ Charges", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onAddClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ajouter", fontSize = 12.sp)
                    }
                }
            }
        }

        // Official Inter-Site Distances Matrix Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Route, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Distances Inter-Sites Officielles (Tunisair Technics)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Matrice kilométrique des bases et aéroports de Tunisie",
                                    fontSize = 10.sp,
                                    color = SlateMedium
                                )
                            }
                        }

                        TextButton(
                            onClick = { showFullMatrix = !showFullMatrix },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (showFullMatrix) "Masquer ▲" else "Consulter ▼",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (showFullMatrix) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "⚖️ Règle officielle : Déduction forfaitaire de 50 km appliquée sur la distance totale Aller-Retour (Net = max(0, A/R - 50 km)).",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }

                        // Site selector chips: "Tous les sites", "Tunis", "Monastir", "Sfax", "Djerba", "Tozeur"
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(listOf("Tous") + SiteDistances.SITES) { site ->
                                val isSelected = site == matrixSelectedProvenance
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { matrixSelectedProvenance = site },
                                    label = {
                                        Text(
                                            text = if (site == "Tous") "Tous les sites" else site,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                                    } else null
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val sitesToDisplay = if (matrixSelectedProvenance == "Tous") SiteDistances.SITES else listOf(matrixSelectedProvenance)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            sitesToDisplay.forEach { site ->
                                val dists = SiteDistances.getDistancesFromProvenance(site)

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Origin site badge
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = AviationNavy,
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.LocationOn,
                                                    contentDescription = null,
                                                    tint = SkyAccent,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = site,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 11.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }

                                        // Destinations on the SAME LINE (horizontal scrollable)
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            dists.forEach { (targetSite, dist) ->
                                                val netAR = SiteDistances.getNetDistanceKm(dist, allerRetour = true)
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.surface,
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.ArrowForward,
                                                            contentDescription = null,
                                                            tint = SkyAccent,
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = targetSite,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                                                        ) {
                                                            Text(
                                                                text = "$dist km",
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = 10.sp,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "(A/R: $netAR km)",
                                                            fontSize = 9.sp,
                                                            color = SlateLight
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
        }

        itemsIndexed(techniciens, key = { _, tech -> tech.matricule }) { _, tech ->
            val destinations = remember(tech.siteProvenance) {
                SiteDistances.getDistancesFromProvenance(tech.siteProvenance)
            }
            var showDistances by remember { mutableStateOf(false) }
            var simTargetSite by remember { mutableStateOf(destinations.firstOrNull()?.first ?: "Monastir") }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header Technicien
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(19.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tech.nom.take(1) + tech.prenom.take(1),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = "${tech.civilite} ${tech.nom} ${tech.prenom}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${tech.matricule} • Base : ",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    ) {
                                        Text(
                                            text = tech.siteProvenance,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "Éch. ${tech.echelle}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { onEditClick(tech) },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Modifier", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            }
                            IconButton(
                                onClick = { onDeleteClick(tech) },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(CrimsonRed.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer Technicien", tint = CrimsonRed, modifier = Modifier.size(17.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Informations professionnelles et bancaires
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Engineering, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Spécialité / Fonction : ${tech.fonction}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "RIB : ${tech.rib}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Répartition de la charge de travail : Missions et Jours terrain
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            // 1. Missions effectuées
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FlightTakeoff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Missions effectuées : ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${tech.histMissions} missions",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // 2. Jours terrain placé juste au dessous
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = AmberGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Jours terrain : ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${tech.histJours} j d'intervention",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberGold
                                )
                            }
                        }
                    }

                    // Section : Distances entre site de provenance et sites d'intervention
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Distances depuis ${tech.siteProvenance} vers interventions",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        TextButton(
                            onClick = { showDistances = !showDistances },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (showDistances) "Masquer ▲" else "Afficher (${destinations.size} sites) ▼",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (showDistances) {
                        Spacer(modifier = Modifier.height(6.dp))

                        // Table of distances to destinations
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Distances kilométriques et durées estimées depuis ${tech.siteProvenance} :",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    destinations.forEach { (targetSite, distKm) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.ArrowForward,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = targetSite,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "(~${SiteDistances.getEstimatedDuration(distKm)})",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Text(
                                                text = "$distKm km",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(8.dp))

                                // Interactive Km Indemnity Simulator for this specific technician
                                Text(
                                    text = "Simulateur d'indemnité kilométrique pour ${tech.prenom} (Échelle ${tech.echelle}) :",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Target selection
                                    var simMenuExpanded by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { simMenuExpanded = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                "Vers : $simTargetSite ▼",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = simMenuExpanded,
                                            onDismissRequest = { simMenuExpanded = false }
                                        ) {
                                            destinations.forEach { (dst, _) ->
                                                DropdownMenuItem(
                                                    text = { Text("Vers $dst") },
                                                    onClick = {
                                                        simTargetSite = dst
                                                        simMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    val targetDist = SiteDistances.getDistanceKm(tech.siteProvenance, simTargetSite)
                                    val netKm = SiteDistances.getNetDistanceKm(targetDist, allerRetour = true)
                                    val indemniteEstimee = SiteDistances.getEstimatedIndemniteKm(targetDist, tech.echelle, allerRetour = true)

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                        modifier = Modifier.weight(1.3f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = "${targetDist * 2} km A/R - 50 km = $netKm km",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            )
                                            Text(
                                                text = "%.3f TND".format(indemniteEstimee),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary
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
private fun DirectionsTab(
    directions: List<DirectionEntity>,
    onEditClick: (DirectionEntity) -> Unit,
    onDeleteClick: (DirectionEntity) -> Unit = {},
    onUpdateClick: () -> Unit = {}
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Directions & Visas Hiérarchiques (${directions.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Modifier les directeurs, les e-mails institutionnels et les téléphones pour les visas",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(directions.sortedBy { it.ordre }, key = { it.code }) { dir ->
            Card(
                shape = RoundedCornerShape(12.dp),
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
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AviationBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dir.code,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Étape ${dir.ordre} : ${dir.libelle}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = "Directeur : ${dir.directeur}",
                            fontSize = 12.sp,
                            color = SlateMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "E-mail : ${dir.email}",
                            fontSize = 11.sp,
                            color = SkyAccent
                        )
                        Text(
                            text = "Tél : ${dir.telephone}",
                            fontSize = 11.sp,
                            color = SlateLight
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { onEditClick(dir) },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AviationNavy)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Modifier", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        IconButton(
                            onClick = { onDeleteClick(dir) },
                            modifier = Modifier
                                .size(28.dp)
                                .background(CrimsonRed.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer Direction", tint = CrimsonRed, modifier = Modifier.size(17.dp))
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// ONGLET : COÛTS UNITAIRES PAR TYPE DE MISSION ET PAR ÉCHELLE
// -------------------------------------------------------------------------
@Composable
private fun CoutsMissionTab(
    coutsMission: List<CoutMissionEntity>,
    onAddClick: () -> Unit,
    onEditClick: (CoutMissionEntity) -> Unit,
    onDeleteClick: (CoutMissionEntity) -> Unit,
    onUpdateClick: () -> Unit = {}
) {
    var selectedTypeFilter by remember { mutableStateOf("Tous") }
    val cleanedCouts = remember(coutsMission) {
        coutsMission.filter {
            !it.typeMission.contains("AOG", ignoreCase = true) && !it.typeMission.contains("Dépannage", ignoreCase = true)
        }
    }
    val distinctTypes = remember(cleanedCouts) {
        listOf("Tous") + cleanedCouts.map { it.typeMission }.distinct()
    }

    val filteredList = remember(cleanedCouts, selectedTypeFilter) {
        if (selectedTypeFilter == "Tous") {
            cleanedCouts.sortedWith(compareBy({ it.typeMission }, { it.echelleMin }))
        } else {
            cleanedCouts.filter { it.typeMission == selectedTypeFilter }.sortedBy { it.echelleMin }
        }
    }

    // Quick Simulator state
    var simMissionType by remember { mutableStateOf("Assistance technique") }
    var simEchelle by remember { mutableIntStateOf(500) }
    var simDays by remember { mutableIntStateOf(3) }
    var simHasHebergement by remember { mutableStateOf(false) }
    var simTypeExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Coûts Unitaires par Type de Mission (${cleanedCouts.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tarif journalier par type de mission et échelle pour le calcul automatique des ODD",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ajouter", fontSize = 12.sp)
                }
            }
        }

        // Info Banner with Detailed Calculation Conditions for each mission type and echelle
        item {
            var showDetails by remember { mutableStateOf(true) }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Calculate, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Règles de Calcul Automatique des Coûts",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Conditions de valorisation par type de mission et palier d'échelle",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        TextButton(
                            onClick = { showDetails = !showDetails },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (showDetails) "Réduire ▲" else "Détails ▼",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (showDetails) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "📐 Formules et conditions officielles de valorisation :",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AviationNavy
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // 1. Hébergement rattaché + Échelle < 500
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "🏨 Avec hébergement rattaché (Échelle < 500)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = AmberGold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "• Condition : Présence d'un frais d'hébergement dans l'ODD et 1er chiffre de l'échelle < 5 (échelles 100 à 499).\n• Formule : Taux Journalier × [ 1 + (Jours - 1) × 0.60 ]\n• Règle : 1er jour indemnisé à 100%, abattement de 40% (taux réduit à 60%) pour les jours restants.",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // 2. Assistance technique
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "💡 Assistance technique (Escale)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = AviationBlue
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "• Condition : Type de mission = 'Assistance technique'.\n• Formule : Taux Journalier × ⌈ Jours / 8 ⌉ (arrondi supérieur)\n• Règle : Facturation par bloc forfaitaire hebdomadaire de 8 jours (1 à 8j = 1 tranche, 9 à 16j = 2 tranches...).",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // 3. Renfort hangar
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "🛠️ Renfort hangar longue durée (> 30 jours, Échelle 500-600)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = EmeraldGreen
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "• Condition : Type de mission = 'Renfort hangar', durée > 30 jours et 1er chiffre d'échelle = 5 ou 6.\n• Formule : (Taux Journalier × 30j) + [ (Jours - 30j) × Taux Journalier × 2/3 ]\n• Règle : Taux plein à 100% sur les 30 premiers jours, puis rémunération aux 2/3 (66.7%) au-delà.",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // 4. Régime standard
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "✈️ Régime standard (Intervention sur avion, Visite, Formation, Transport)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = AviationNavy
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "• Condition : Hors conditions spécifiques 1, 2 et 3.\n• Formule : Taux Journalier × Jours effectifs de mission.",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Grille des taux par type de mission et palier d'échelle
                        Text(
                            text = "📊 Grille des taux journaliers par type de mission et échelle :",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AviationNavy
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val missionTypesGrouping = cleanedCouts.groupBy { it.typeMission }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            missionTypesGrouping.forEach { (type, coutsList) ->
                                val sorted = coutsList.sortedBy { it.echelleMin }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = type,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = AviationNavy
                                            )
                                            val regleName = when {
                                                type.contains("Assistance", ignoreCase = true) -> "Tranches 8j"
                                                type.contains("hangar", ignoreCase = true) -> "2/3 après 30j (éch 5-6)"
                                                else -> "Standard (100%/j)"
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                            ) {
                                                Text(
                                                    text = regleName,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Horizontal scroll of scales for this mission type on the same line
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            sorted.forEach { c ->
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = "Éch. ${c.echelleMin}-${c.echelleMax} :",
                                                            fontSize = 10.sp,
                                                            color = SlateMedium
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "%.1f TND/j".format(c.coutUnitaireJournalier),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Grille indiciaire d'équivalence
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ℹ️ Équivalence indiciaire des échelles : Éch. 1-5 (Indice 100-300) • Éch. 6-10 (Indice 400-500) • Éch. 11-15 (Indice 600-700) • Éch. 16-20 (Indice 800-900).",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Filter chips by Mission Type
        item {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(distinctTypes) { type ->
                    val isSelected = type == selectedTypeFilter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTypeFilter = type },
                        label = {
                            Text(
                                text = if (type == "Tous") "Tous (${cleanedCouts.size})" else type,
                                fontSize = 11.sp
                            )
                        },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }
        }

        // Interactive Simulator Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Preview, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Simulateur de Taux Journalier",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mission Type selector
                        Box(modifier = Modifier.weight(1.3f)) {
                            OutlinedButton(
                                onClick = { simTypeExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(simMissionType, fontSize = 11.sp, maxLines = 1)
                            }
                            DropdownMenu(
                                expanded = simTypeExpanded,
                                onDismissRequest = { simTypeExpanded = false }
                            ) {
                                cleanedCouts.map { it.typeMission }.distinct().forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type, fontSize = 12.sp) },
                                        onClick = {
                                            simMissionType = type
                                            simTypeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Echelle Selector
                        OutlinedButton(
                            onClick = {
                                simEchelle = when (simEchelle) {
                                    400 -> 450
                                    450 -> 500
                                    500 -> 550
                                    550 -> 600
                                    else -> 400
                                }
                            },
                            modifier = Modifier.weight(0.9f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Éch. $simEchelle ↻", fontSize = 11.sp)
                        }

                        // Days Selector
                        OutlinedButton(
                            onClick = {
                                simDays = when (simDays) {
                                    1 -> 3
                                    3 -> 5
                                    5 -> 8
                                    8 -> 10
                                    10 -> 16
                                    16 -> 30
                                    30 -> 35
                                    35 -> 45
                                    else -> 1
                                }
                            },
                            modifier = Modifier.weight(0.7f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("$simDays j ↻", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Hébergement toggle for simulator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Frais d'hébergement rattaché :",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilterChip(
                            selected = simHasHebergement,
                            onClick = { simHasHebergement = !simHasHebergement },
                            label = {
                                Text(if (simHasHebergement) "🏨 Inclus (Oui)" else "🏨 Non inclus (Non)", fontSize = 10.sp)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val resolvedRate = SiteDistances.getMissionDailyRateByTypeAndEchelle(
                        missionType = simMissionType,
                        echelle = simEchelle,
                        coutsMission = coutsMission,
                        baremes = emptyList()
                    )
                    val costSim = SiteDistances.computeDetailedCoutMission(
                        delaisJours = simDays,
                        prixUnitaireMission = resolvedRate,
                        echelle = simEchelle,
                        mission = simMissionType,
                        hasHebergementFrais = simHasHebergement
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Règle : ${costSim.regleAppliquee}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = costSim.formuleDescription,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "%.3f TND".format(costSim.coutTotal),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // List of unit cost items
        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun coût unitaire défini pour ce filtre.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredList, key = { it.id }) { cout ->
                val icon = when {
                    cout.typeMission.contains("avion", ignoreCase = true) -> Icons.Default.FlightTakeoff
                    cout.typeMission.contains("hangar", ignoreCase = true) -> Icons.Default.Build
                    cout.typeMission.contains("technique", ignoreCase = true) -> Icons.Default.Handyman
                    cout.typeMission.contains("Formation", ignoreCase = true) -> Icons.Default.School
                    cout.typeMission.contains("médicale", ignoreCase = true) -> Icons.Default.LocalHospital
                    cout.typeMission.contains("licence", ignoreCase = true) -> Icons.Default.Badge
                    cout.typeMission.contains("Transport", ignoreCase = true) -> Icons.Default.DirectionsTransit
                    else -> Icons.Default.Work
                }

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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = cout.typeMission,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = "Échelle ${cout.echelleMin} - ${cout.echelleMax}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (cout.observation.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = cout.observation,
                                        fontSize = 10.sp,
                                        color = SlateMedium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "%.3f TND".format(cout.coutUnitaireJournalier),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "/ jour",
                                fontSize = 10.sp,
                                color = SlateMedium
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { onEditClick(cout) },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AviationNavy)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Modifier", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            IconButton(
                                onClick = { onDeleteClick(cout) },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(CrimsonRed.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer Coût", tint = CrimsonRed, modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BaremesTab(
    baremes: List<BaremeTarifEntity>,
    onAddClick: () -> Unit,
    onEditClick: (BaremeTarifEntity) -> Unit,
    onDeleteClick: (BaremeTarifEntity) -> Unit,
    onUpdateClick: () -> Unit = {}
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Barèmes des Frais & Tarifs (${baremes.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Grille de calcul des nuitées, taux kilométriques, forfaits et coûts unitaires de mission par type de mission",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ajouter", fontSize = 12.sp)
                }
            }
        }

        items(baremes, key = { it.id }) { b ->
            val displayDesignation = if (b.designation.contains("Coût Journalier Mission", ignoreCase = true)) {
                b.designation.replace("Coût Journalier Mission", "Coût Unitaire Mission par Type de Mission", ignoreCase = true)
            } else if (b.designation.contains("Coût Journalier", ignoreCase = true)) {
                b.designation.replace("Coût Journalier", "Coût Unitaire Mission par Type de Mission", ignoreCase = true)
            } else {
                b.designation
            }

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
                        Text(
                            text = displayDesignation,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Catégorie : ${b.categorie} • Échelle : ${b.echelleMin}-${b.echelleMax}",
                            fontSize = 11.sp,
                            color = SlateMedium
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "%.3f TND".format(b.tarifUnitaire),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "/ ${b.unite}",
                            fontSize = 11.sp,
                            color = SlateMedium
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { onEditClick(b) },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AviationNavy)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Modifier", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        IconButton(
                            onClick = { onDeleteClick(b) },
                            modifier = Modifier
                                .size(28.dp)
                                .background(CrimsonRed.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer Barème", tint = CrimsonRed, modifier = Modifier.size(17.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallationAndSettingsTab(
    onResetDefaultsClick: () -> Unit,
    onOpenThemeSelector: () -> Unit = {}
) {
    val context = LocalContext.current
    val sharedAppUrl = "https://ais-pre-ot4oskhxnxatsuyrueu6tq-463050112606.europe-west2.run.app"
    var showModalInstallDialog by remember { mutableStateOf(false) }

    if (showModalInstallDialog) {
        com.example.ui.components.InstallAppModalDialog(
            onDismiss = { showModalInstallDialog = false }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Installation Mobile & Paramètres Système",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Personnalisation des couleurs, installation sur smartphone Android, iPhone, iPad ou PC",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showModalInstallDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Assistant 📲", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section 0: Theme and Appearance Customization
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Aspect Visuel & Couleurs de l'Application",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Palette de couleurs (Bleu Aéro, Rouge Tunisair, Vert Émeraude, Ambre, Indigo, Ardoise) & Mode Sombre/Clair",
                                fontSize = 11.sp,
                                color = SlateMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Adaptez l'interface graphique selon vos préférences ou votre identité visuelle. Le choix est sauvegardé sur votre appareil.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onOpenThemeSelector,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Personnaliser les Couleurs & Thème 🎨", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Section 1: Android APK Installation Guide
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Android, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "1. Installer sur Téléphone ou Tablette Android (Fichier APK)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Package natif 'app-debug.apk' (33 Mo) • 100% Hors-Ligne",
                                fontSize = 11.sp,
                                color = SlateMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Étapes pour installer sur votre téléphone Android :",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Étape 1 : Dans le menu supérieur d'AI Studio (en haut à droite), cliquez sur les paramètres et choisissez 'Export' > 'Download APK'.\n" +
                               "• Étape 2 : Transférez le fichier 'app-debug.apk' sur votre smartphone (par WhatsApp, Google Drive, e-mail ou câble USB).\n" +
                               "• Étape 3 : Sur le téléphone, ouvrez le fichier téléchargé. Si demandé, autorisez l'installation d'applications de sources inconnues.\n" +
                               "• Étape 4 : Cliquez sur 'Installer'. L'icône zODD apparaît sur l'écran d'accueil !",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showModalInstallDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ouvrir l'Assistant d'Installation Android", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Section 2: Computer (PC Windows / Mac) Guide
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AmberGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Computer, contentDescription = null, tint = Color.Black)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "2. Installer & Utiliser sur Ordinateur (PC Windows / Mac / Linux)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Application de Bureau (PWA), Émulateur Android WSA / BlueStacks ou Navigateur",
                                fontSize = 11.sp,
                                color = SlateMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• Mode Bureau PWA (Recommandé) : Dans Google Chrome ou Microsoft Edge, cliquez sur 'Installer l'application' dans la barre d'adresses. L'application dispose alors de son icône sur le Bureau Windows/Mac et se lance dans une fenêtre dédiée.\n" +
                               "• Mode Hors-Ligne (Émulateur Android) : Téléchargez 'app-debug.apk' et glissez-le dans BlueStacks 5, le sous-système Android de Windows 11 (WSA) ou Android Studio pour une utilisation 100% autonome hors-ligne.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showModalInstallDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Computer, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guide PC / Mac Détaillé", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                        }

                        OutlinedButton(
                            onClick = {
                                val instructions = "Guide zODD sur Ordinateur (PC/Mac) :\n" +
                                        "1. Application Bureau : Dans Chrome/Edge, cliquez sur 'Installer l'application' pour créer le raccourci Bureau.\n" +
                                        "2. Fichier APK Hors-ligne : Glissez app-debug.apk dans BlueStacks ou Windows Subsystem for Android."
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Instructions PC zODD", instructions))
                                Toast.makeText(context, "Instructions PC copiées dans le presse-papiers !", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copier", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section 3: iPad & iPhone (iOS) Guide
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AviationBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhoneIphone, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "3. Installer & Utiliser sur iPad & iPhone (Apple iOS)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Icône Écran d'Accueil Safari (Plein Écran) & Package IPA Xcode",
                                fontSize = 11.sp,
                                color = SlateMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• Installation Immédiate sur iPad / iPhone : Ouvrez zODD dans Safari. Touchez le bouton 'Partager' (flèche vers le haut) et sélectionnez 'Sur l'écran d'accueil'. L'application zODD s'installe avec son icône sur votre écran et s'exécute en plein écran tactile natif sans barre de navigateur Safari.\n\n" +
                               "• Déploiement Binaire iOS (.IPA) : Pour une installation via un outil de gestion d'entreprise (MDM), téléchargez les sources complètes (Download ZIP) et signez le projet dans Xcode avec votre certificat d'entreprise Apple.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showModalInstallDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AviationBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhoneIphone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guide iPad / iOS Détaillé", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val instructions = "Guide d'installation zODD iPad & iPhone (iOS) :\n" +
                                        "1. Ouvrez zODD dans Safari sur votre iPad ou iPhone.\n" +
                                        "2. Touchez 'Partager' (icône carré avec flèche vers le haut).\n" +
                                        "3. Choisissez 'Sur l'écran d'accueil' puis 'Ajouter'.\n" +
                                        "4. Lancez zODD directement depuis l'écran d'accueil en plein écran natif."
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Instructions iPad zODD", instructions))
                                Toast.makeText(context, "Instructions iPad / iOS copiées !", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copier", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section 3: Cloud Database & Architecture
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Configuration & Base de Données",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Base locale : SQLite Room Database embarquée (fonctionnement 100% hors-ligne garanti)\n" +
                               "• Cloud Firebase : Firestore projet ai-studio-47bb18e7-4836-4703-a370-7b48d1e624f1\n" +
                               "• Devise de facturation : Dinar Tunisien (TND / millimes)\n" +
                               "• Générateur de rapports : PDF certifié avec signatures numériques et visas",
                        fontSize = 12.sp,
                        color = SlateMedium,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Section 4: Factory Reset Button
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CrimsonRed.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Réinitialisation des Paramètres de Base",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CrimsonRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Restaure les 7 directions, techniciens et barèmes initiaux de Tunisair Technics si vous souhaitez annuler vos modifications.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onResetDefaultsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restaurer les Paramètres d'Usine", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
