package com.example.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.*
import com.example.ui.theme.*

private enum class WorkloadSortMode(val label: String) {
    MISSIONS("Nb missions"),
    COUT("Coût missions"),
    JOURS("Jours terrain")
}

private data class TechWorkloadItem(
    val tech: TechnicienEntity,
    val missionsCount: Int,
    val totalCost: Double,
    val totalJours: Int
)

@Composable
fun DashboardScreen(
    lots: List<LotEntity>,
    odds: List<OddEntity>,
    fraisList: List<FraisEntity>,
    validations: List<ValidationEntity>,
    techniciens: List<TechnicienEntity>,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f || MaterialTheme.colorScheme.background.luminance() < 0.5f
    val dynamicTitleColor = if (isDark) Color(0xFFF1F5F9) else AviationNavy
    val dynamicSiteTextColor = if (isDark) Color(0xFF93C5FD) else AviationNavy
    val dynamicSiteBgColor = if (isDark) Color(0xFF1E293B) else AviationBlue.copy(alpha = 0.12f)
    val dynamicSubtextColor = if (isDark) Color(0xFF94A3B8) else SlateMedium

    val totalCoutGlobal = lots.sumOf { it.totalCoutOdd }
    val totalMissionsGlobal = odds.size
    val totalHebergement = fraisList.filter { it.categorie.contains("Héberg", true) }.sumOf { it.aComptabiliser }
    val totalTransport = fraisList.filter { it.categorie.contains("Transp", true) }.sumOf { it.aComptabiliser }
    val totalDivers = fraisList.filter { it.categorie.contains("Div", true) }.sumOf { it.aComptabiliser }

    // Métriques spécifiques demandées pour le Badge Total Mission
    val lotsFigesSet = lots.filter { it.figerLot }.map { it.lotN }.toSet()
    val oddsLotsFigesCount = odds.count { it.lotN in lotsFigesSet }
    val totalJoursTerrain = odds.sumOf { com.example.util.SiteDistances.getMissionDurationDays(it.dateDebut, it.dateFin) }
    val nbTechEngages = odds.map { it.matricule }.filter { it.isNotBlank() }.distinct().size
    val nbHeuresTravaillees = totalJoursTerrain * 8

    // Grouping by Site (Provenance & Intervention)
    val sites = listOf("Tunis", "Monastir", "Sfax", "Djerba", "Tozeur")
    val coutParSite = sites.associateWith { site ->
        odds.filter { it.siteIntervention.equals(site, true) || it.siteProvenance.equals(site, true) }
            .sumOf { it.totalOdd }
    }
    val maxSiteCost = (coutParSite.values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)

    // Directions Reactivity average (in days)
    val directions = listOf("DM", "DCT", "DGRT", "DCST", "AUDIT", "DG", "DAF")
    val reactiviteParDir = directions.associateWith { dir ->
        val dirVals = validations.filter { it.direction == dir && it.reactiviteJours > 0 }
        if (dirVals.isNotEmpty()) dirVals.map { it.reactiviteJours }.average() else 1.2
    }

    // Workload sorting state
    var workloadSortMode by remember { mutableStateOf(WorkloadSortMode.MISSIONS) }

    // ODD Filter / Search state for Frais par ODD
    var oddSearchQuery by remember { mutableStateOf("") }
    var showAllOddsBreakdown by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dashboard Title
        item {
            Column {
                Text(
                    text = "Tableau de Bord zODD V.2-26",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Suivi budgétaire en temps réel, répartition des frais et charge de travail.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Global KPI Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = "Budget Engagé",
                    value = "%.3f TND".format(totalCoutGlobal),
                    subtitle = "${lots.size} Lots actifs (${lotsFigesSet.size} figés)",
                    icon = Icons.Default.MonetizationOn,
                    color = AviationBlue,
                    modifier = Modifier.weight(1f)
                )

                // Badge Total Mission détaillé avec les 4 métriques
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Total Missions",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(AmberGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "$totalMissionsGlobal ODD",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AmberGold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "(${lots.size} lots)",
                                fontSize = 10.sp,
                                color = dynamicSubtextColor,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Précisions exigées dans le Badge Total Mission
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.5.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("🔒 ODD Lots Figés :", fontSize = 9.sp, color = dynamicSubtextColor)
                                    Text("$oddsLotsFigesCount ODD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF6EE7B7) else EmeraldGreen)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("📅 Jours Terrain :", fontSize = 9.sp, color = dynamicSubtextColor)
                                    Text("$totalJoursTerrain j", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = dynamicTitleColor)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("👷 Techs Engagés :", fontSize = 9.sp, color = dynamicSubtextColor)
                                    Text("$nbTechEngages techs", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = dynamicTitleColor)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("⏱️ Heures Travaillées :", fontSize = 9.sp, color = dynamicSubtextColor)
                                    Text("${nbHeuresTravaillees} h", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 1. ACTUALISER : RÉPARTITION BUDGÉTAIRE PAR TYPE DE FRAIS ET PAR ODD
        // =========================================================================
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Répartition Budgétaire par Type de Frais et par ODD",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ventilation Hébergement, Transport et Divers pour chaque ODD",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "${odds.size} ODD",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Global summary split bar across all ODDs
                    val sumGlobalFrais = (totalHebergement + totalTransport + totalDivers).coerceAtLeast(0.0)
                    Text(
                        text = "Synthèse Globale de l'ensemble des ODD (Total : %.3f TND)".format(sumGlobalFrais),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FraisSplitBar(heberg = totalHebergement, transp = totalTransport, divers = totalDivers)
                    Spacer(modifier = Modifier.height(10.dp))
                    FraisLegendRow(heberg = totalHebergement, transp = totalTransport, divers = totalDivers)

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )

                    // Search input to filter ODDs
                    OutlinedTextField(
                        value = oddSearchQuery,
                        onValueChange = { oddSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Rechercher un ODD (ex: DJE-0005, TUN, matricule...)", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filtered ODDs list
                    val filteredOdds = odds.filter { odd ->
                        oddSearchQuery.isBlank() ||
                                odd.oddN.contains(oddSearchQuery, ignoreCase = true) ||
                                odd.matricule.contains(oddSearchQuery, ignoreCase = true) ||
                                odd.siteIntervention.contains(oddSearchQuery, ignoreCase = true) ||
                                odd.siteProvenance.contains(oddSearchQuery, ignoreCase = true)
                    }

                    val oddsToDisplay = if (showAllOddsBreakdown || oddSearchQuery.isNotBlank()) filteredOdds else filteredOdds.take(4)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        oddsToDisplay.forEach { odd ->
                            val oddFrais = fraisList.filter { it.oddN == odd.oddN }
                            val h = oddFrais.filter { it.categorie.contains("Héberg", true) }.sumOf { it.aComptabiliser }
                            val t = oddFrais.filter { it.categorie.contains("Transp", true) }.sumOf { it.aComptabiliser }
                            val d = oddFrais.filter { it.categorie.contains("Div", true) }.sumOf { it.aComptabiliser }
                            val totalFraisOdd = h + t + d

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(odd.oddN, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = dynamicTitleColor)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = dynamicSiteBgColor
                                            ) {
                                                Text(
                                                    text = "${odd.siteProvenance} ➔ ${odd.siteIntervention}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = dynamicSiteTextColor,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Frais : %.3f TND".format(totalFraisOdd),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${odd.matricule} • ${odd.mission}",
                                            fontSize = 10.sp,
                                            color = dynamicSubtextColor
                                        )
                                        Text(
                                            text = "Coût Mission: %.3f TND".format(odd.totalOdd),
                                            fontSize = 10.sp,
                                            color = if (isDark) Color(0xFFCBD5E1) else SlateLight
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    FraisSplitBar(heberg = h, transp = t, divers = d, height = 8.dp)

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("🏨 Héberg: %.3f".format(h), fontSize = 9.sp, color = CobaltPrimary, fontWeight = FontWeight.Medium)
                                        Text("✈️ Transp: %.3f".format(t), fontSize = 9.sp, color = SkyAccent, fontWeight = FontWeight.Medium)
                                        Text("🍽️ Divers: %.3f".format(d), fontSize = 9.sp, color = AmberGold, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        if (filteredOdds.size > 4 && oddSearchQuery.isBlank()) {
                            TextButton(
                                onClick = { showAllOddsBreakdown = !showAllOddsBreakdown },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(
                                    imageVector = if (showAllOddsBreakdown) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (showAllOddsBreakdown) "Réduire la liste" else "Voir l'ensemble des ${filteredOdds.size} ODD",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 2. RAJOUTER : RÉPARTITION BUDGÉTAIRE PAR TYPE DE FRAIS ET PAR LOT
        // =========================================================================
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Répartition Budgétaire par Type de Frais et par Lot",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ventilation des dépenses (Hébergement, Transport, Divers) par Lot",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldLight
                        ) {
                            Text(
                                text = "${lots.size} Lots",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val sumGlobalLotsFrais = lots.sumOf { lot ->
                        val lotOdds = odds.filter { it.lotN == lot.lotN }
                        fraisList.filter { f -> lotOdds.any { it.oddN == f.oddN } }.sumOf { it.aComptabiliser }
                    }
                    val globalLotH = lots.sumOf { lot ->
                        val lotOdds = odds.filter { it.lotN == lot.lotN }
                        fraisList.filter { f -> lotOdds.any { it.oddN == f.oddN } && f.categorie.contains("Héberg", true) }.sumOf { it.aComptabiliser }
                    }
                    val globalLotT = lots.sumOf { lot ->
                        val lotOdds = odds.filter { it.lotN == lot.lotN }
                        fraisList.filter { f -> lotOdds.any { it.oddN == f.oddN } && f.categorie.contains("Transp", true) }.sumOf { it.aComptabiliser }
                    }
                    val globalLotD = lots.sumOf { lot ->
                        val lotOdds = odds.filter { it.lotN == lot.lotN }
                        fraisList.filter { f -> lotOdds.any { it.oddN == f.oddN } && f.categorie.contains("Div", true) }.sumOf { it.aComptabiliser }
                    }

                    Text(
                        text = "Synthèse Globale par Lot (Total : %.3f TND)".format(sumGlobalLotsFrais),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FraisSplitBar(heberg = globalLotH, transp = globalLotT, divers = globalLotD)
                    Spacer(modifier = Modifier.height(10.dp))
                    FraisLegendRow(heberg = globalLotH, transp = globalLotT, divers = globalLotD)

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )

                    if (lots.isEmpty()) {
                        Text("Aucun lot disponible pour le moment.", fontSize = 12.sp, color = SlateMedium)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            lots.forEach { lot ->
                                val lotOdds = odds.filter { it.lotN == lot.lotN }
                                val lotFrais = fraisList.filter { f -> lotOdds.any { it.oddN == f.oddN } }
                                val h = lotFrais.filter { it.categorie.contains("Héberg", true) }.sumOf { it.aComptabiliser }
                                val t = lotFrais.filter { it.categorie.contains("Transp", true) }.sumOf { it.aComptabiliser }
                                val d = lotFrais.filter { it.categorie.contains("Div", true) }.sumOf { it.aComptabiliser }
                                val totalFraisLot = h + t + d

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = dynamicTitleColor, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(lot.lotN, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = dynamicTitleColor)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (lot.figerLot) EmeraldLight else AmberLight
                                                ) {
                                                    Text(
                                                        text = if (lot.figerLot) "FIGÉ (${lot.directionActuelle})" else "OUVERT",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (lot.figerLot) EmeraldGreen else AmberGold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "%.3f TND".format(totalFraisLot),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${lot.libelle} • ${lotOdds.size} ODD rattachés",
                                            fontSize = 11.sp,
                                            color = SlateMedium,
                                            maxLines = 1
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))
                                        FraisSplitBar(heberg = h, transp = t, divers = d, height = 12.dp)

                                        Spacer(modifier = Modifier.height(8.dp))
                                        FraisLegendRow(heberg = h, transp = t, divers = d)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 3. RAJOUTER : RÉPARTITION BUDGÉTAIRE PAR TYPE DE FRAIS ET PAR SITE DE PROVENANCE
        // =========================================================================
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Répartition Budgétaire par Type de Frais et par Site de Provenance",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Dépenses selon la base d'affectation d'origine des agents (Tunis, Monastir, Sfax...)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = AviationBlue, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val sumGlobalSiteFrais = sites.sumOf { site ->
                        val siteOdds = odds.filter { it.siteProvenance.equals(site, ignoreCase = true) }
                        fraisList.filter { f -> siteOdds.any { it.oddN == f.oddN } }.sumOf { it.aComptabiliser }
                    }
                    val globalSiteH = sites.sumOf { site ->
                        val siteOdds = odds.filter { it.siteProvenance.equals(site, ignoreCase = true) }
                        fraisList.filter { f -> siteOdds.any { it.oddN == f.oddN } && f.categorie.contains("Héberg", true) }.sumOf { it.aComptabiliser }
                    }
                    val globalSiteT = sites.sumOf { site ->
                        val siteOdds = odds.filter { it.siteProvenance.equals(site, ignoreCase = true) }
                        fraisList.filter { f -> siteOdds.any { it.oddN == f.oddN } && f.categorie.contains("Transp", true) }.sumOf { it.aComptabiliser }
                    }
                    val globalSiteD = sites.sumOf { site ->
                        val siteOdds = odds.filter { it.siteProvenance.equals(site, ignoreCase = true) }
                        fraisList.filter { f -> siteOdds.any { it.oddN == f.oddN } && f.categorie.contains("Div", true) }.sumOf { it.aComptabiliser }
                    }

                    Text(
                        text = "Synthèse Globale par Site de Provenance (Total : %.3f TND)".format(sumGlobalSiteFrais),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FraisSplitBar(heberg = globalSiteH, transp = globalSiteT, divers = globalSiteD)
                    Spacer(modifier = Modifier.height(10.dp))
                    FraisLegendRow(heberg = globalSiteH, transp = globalSiteT, divers = globalSiteD)

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        sites.forEach { site ->
                            val siteOdds = odds.filter { it.siteProvenance.equals(site, ignoreCase = true) }
                            val siteFrais = fraisList.filter { f -> siteOdds.any { it.oddN == f.oddN } }
                            val h = siteFrais.filter { it.categorie.contains("Héberg", true) }.sumOf { it.aComptabiliser }
                            val t = siteFrais.filter { it.categorie.contains("Transp", true) }.sumOf { it.aComptabiliser }
                            val d = siteFrais.filter { it.categorie.contains("Div", true) }.sumOf { it.aComptabiliser }
                            val totalFraisSite = h + t + d

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when (site) {
                                                            "Tunis" -> AviationNavy
                                                            "Monastir" -> AviationBlue
                                                            "Sfax" -> CobaltPrimary
                                                            "Djerba" -> SkyAccent
                                                            else -> AmberGold
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(site, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("(${siteOdds.size} missions émises)", fontSize = 10.sp, color = SlateMedium)
                                        }

                                        Text(
                                            text = "%.3f TND".format(totalFraisSite),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    FraisSplitBar(heberg = h, transp = t, divers = d, height = 10.dp)

                                    Spacer(modifier = Modifier.height(8.dp))
                                    FraisLegendRow(heberg = h, transp = t, divers = d)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cost Per Site Chart (Tunis, Monastir, Sfax, Djerba, Tozeur)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Coût Total des Missions par Site",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "En Dinars Tunisiens (TND)",
                            fontSize = 11.sp,
                            color = SlateMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    sites.forEach { site ->
                        val rawCost = coutParSite[site] ?: 0.0
                        val cost = if (rawCost.isNaN() || rawCost < 0.0) 0.0 else rawCost
                        val fraction = if (maxSiteCost > 0.0 && !cost.isNaN()) {
                            (cost / maxSiteCost).toFloat().coerceIn(0.05f, 1f)
                        } else {
                            0.05f
                        }

                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(site, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text("%.3f TND".format(cost), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color(0xFFE2E8F0))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            when (site) {
                                                "Tunis" -> AviationNavy
                                                "Monastir" -> AviationBlue
                                                "Sfax" -> CobaltPrimary
                                                "Djerba" -> SkyAccent
                                                else -> AmberGold
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Direction Reactivity Delay (Temps de Réactivité en jours)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Réactivité Moyenne des Directions (en Jours)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Délai moyen de traitement et signature hiérarchique avant transmission",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        directions.forEach { dir ->
                            val days = reactiviteParDir[dir] ?: 1.0
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (days <= 1.5) EmeraldLight else if (days <= 3.0) AmberLight else CrimsonLight
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "%.1f".format(days),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (days <= 1.5) EmeraldGreen else if (days <= 3.0) AmberGold else CrimsonRed
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(dir, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("j", fontSize = 9.sp, color = SlateLight)
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 4. CHARGE DE TRAVAIL PAR TECHNICIEN AVEC BOUTON RADIO ET DÉGRADÉ TOP 3
        // =========================================================================
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Charge de Travail par Technicien",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Classement décroissant et analyse comparative",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = AmberGold, modifier = Modifier.size(22.dp))
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Radio button selector: 3 boutons radio SUR LA MÊME LIGNE avec espacement compact
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CompositionLocalProvider(androidx.compose.material3.LocalMinimumInteractiveComponentSize provides 0.dp) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WorkloadSortMode.values().forEach { mode ->
                                    val isSelected = workloadSortMode == mode
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { workloadSortMode = mode }
                                            .padding(horizontal = 4.dp, vertical = 4.dp)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { workloadSortMode = mode },
                                            modifier = Modifier.size(20.dp),
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = mode.label,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Compute workloads
                    val workloadItems = techniciens.map { tech ->
                        val techOdds = odds.filter { it.matricule == tech.matricule }
                        val missionsCount = techOdds.size + tech.histMissions
                        val totalCost = techOdds.sumOf { it.totalOdd }
                        val totalJours = techOdds.sumOf { com.example.util.SiteDistances.getMissionDurationDays(it.dateDebut, it.dateFin) } + tech.histJours
                        TechWorkloadItem(tech, missionsCount, totalCost, totalJours)
                    }

                    // Sort in descending order according to the radio selection
                    val sortedWorkloads = remember(workloadItems, workloadSortMode) {
                        when (workloadSortMode) {
                            WorkloadSortMode.MISSIONS -> workloadItems.sortedWith(
                                compareByDescending<TechWorkloadItem> { it.missionsCount }
                                    .thenByDescending { it.totalCost }
                            )
                            WorkloadSortMode.COUT -> workloadItems.sortedWith(
                                compareByDescending<TechWorkloadItem> { it.totalCost }
                                    .thenByDescending { it.missionsCount }
                            )
                            WorkloadSortMode.JOURS -> workloadItems.sortedWith(
                                compareByDescending<TechWorkloadItem> { it.totalJours }
                                    .thenByDescending { it.missionsCount }
                            )
                        }
                    }

                    val maxVal = when (workloadSortMode) {
                        WorkloadSortMode.MISSIONS -> (sortedWorkloads.maxOfOrNull { it.missionsCount.toDouble() } ?: 1.0).coerceAtLeast(1.0)
                        WorkloadSortMode.COUT -> (sortedWorkloads.maxOfOrNull { it.totalCost } ?: 1.0).coerceAtLeast(1.0)
                        WorkloadSortMode.JOURS -> (sortedWorkloads.maxOfOrNull { it.totalJours.toDouble() } ?: 1.0).coerceAtLeast(1.0)
                    }

                    val defaultOutline = MaterialTheme.colorScheme.outline
                    val defaultSurface = MaterialTheme.colorScheme.surface
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        sortedWorkloads.forEachIndexed { index, item ->
                            val rank = index + 1
                            val isTop3 = rank <= 3

                            // Distinct Gradient Colors for the top 3
                            val topGradient = when (rank) {
                                1 -> Brush.horizontalGradient(
                                    listOf(Color(0xFFFFD54F), Color(0xFFFFB300), Color(0xFFFF8F00))
                                ) // Or Prestige
                                2 -> Brush.horizontalGradient(
                                    listOf(Color(0xFFECEFF1), Color(0xFFB0BEC5), Color(0xFF78909C))
                                ) // Argent Platine
                                3 -> Brush.horizontalGradient(
                                    listOf(Color(0xFFFFCC80), Color(0xFFFB8C00), Color(0xFFD84315))
                                ) // Bronze Cuivré
                                else -> null
                            }

                            val badgeGradient = when (rank) {
                                1 -> Brush.horizontalGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A)))
                                2 -> Brush.horizontalGradient(listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0)))
                                3 -> Brush.horizontalGradient(listOf(Color(0xFFFFEDD5), Color(0xFFFED7AA)))
                                else -> null
                            }

                            val badgeBorderColor = when (rank) {
                                1 -> Color(0xFFD97706)
                                2 -> Color(0xFF64748B)
                                3 -> Color(0xFFEA580C)
                                else -> defaultOutline.copy(alpha = 0.25f)
                            }

                            val badgeTextColor = when (rank) {
                                1 -> if (isDark) Color(0xFFFDE68A) else Color(0xFF78350F)
                                2 -> if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                                3 -> if (isDark) Color(0xFFFFEDD5) else Color(0xFF7C2D12)
                                else -> dynamicSubtextColor
                            }

                            val badgeLabel = when (rank) {
                                1 -> "🥇 1er"
                                2 -> "🥈 2e"
                                3 -> "🥉 3e"
                                else -> "#$rank"
                            }

                            val cardBg = when (rank) {
                                1 -> if (isDark) Color(0xFF292518) else Color(0xFFFFFBEB)
                                2 -> if (isDark) Color(0xFF1E2430) else Color(0xFFF8FAFC)
                                3 -> if (isDark) Color(0xFF2B1D15) else Color(0xFFFFF7ED)
                                else -> defaultSurface
                            }

                            val avatarBg = when (rank) {
                                1 -> if (isDark) Color(0xFF78350F) else Color(0xFFFDE68A)
                                2 -> if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                                3 -> if (isDark) Color(0xFF7C2D12) else Color(0xFFFED7AA)
                                else -> if (isDark) Color(0xFF1E293B) else IceBlue
                            }

                            val rawVal = when (workloadSortMode) {
                                WorkloadSortMode.MISSIONS -> item.missionsCount.toDouble()
                                WorkloadSortMode.COUT -> item.totalCost
                                WorkloadSortMode.JOURS -> item.totalJours.toDouble()
                            }
                            val currentVal = if (rawVal.isNaN() || rawVal < 0.0) 0.0 else rawVal
                            val fraction = if (maxVal > 0.0 && !currentVal.isNaN()) {
                                (currentVal / maxVal).toFloat().coerceIn(0.08f, 1f)
                            } else {
                                0.08f
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = cardBg,
                                border = BorderStroke(
                                    width = if (rank == 1) 1.5.dp else if (isTop3) 1.2.dp else 1.dp,
                                    color = if (isTop3) badgeBorderColor.copy(alpha = 0.65f) else defaultOutline.copy(alpha = 0.15f)
                                ),
                                shadowElevation = if (rank == 1) 3.dp else if (isTop3) 1.5.dp else 0.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            // Compact Podium Badge
                                            if (badgeGradient != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(badgeGradient)
                                                        .border(BorderStroke(1.dp, badgeBorderColor), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = badgeLabel,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = badgeTextColor
                                                    )
                                                }
                                            } else {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                                    border = BorderStroke(1.dp, badgeBorderColor)
                                                ) {
                                                    Text(
                                                        text = badgeLabel,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = badgeTextColor,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            // Monogram Avatar
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(avatarBg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = item.tech.nom.take(1) + item.tech.prenom.take(1),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 10.sp,
                                                    color = badgeTextColor
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                             Column(modifier = Modifier.weight(1f, fill = false)) {
                                                // Identité complète et Site de provenance SUR LA MÊME LIGNE
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${item.tech.civilite} ${item.tech.nom} ${item.tech.prenom}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = onSurfaceColor,
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = dynamicSiteBgColor
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.LocationOn,
                                                                contentDescription = null,
                                                                tint = if (isDark) Color(0xFF60A5FA) else AviationBlue,
                                                                modifier = Modifier.size(9.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                            Text(
                                                                text = item.tech.siteProvenance,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = dynamicSiteTextColor
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = "${item.tech.matricule} • Échelle ${item.tech.echelle}",
                                                    fontSize = 10.sp,
                                                    color = SlateMedium
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            when (workloadSortMode) {
                                                WorkloadSortMode.MISSIONS -> {
                                                    // 1ère ligne : Nb missions mis en valeur
                                                    Text(
                                                        text = "${item.missionsCount} missions",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 13.sp,
                                                        color = if (isTop3) badgeTextColor else primaryColor
                                                    )
                                                    // 2ème ligne : Jours terrain en couleur grisé
                                                    Text(
                                                        text = "${item.totalJours} j terrain",
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 10.sp,
                                                        color = SlateMedium
                                                    )
                                                    // 3ème ligne : Coût en couleur grisé
                                                    Text(
                                                        text = "Coût: %.3f TND".format(item.totalCost),
                                                        fontSize = 10.sp,
                                                        color = SlateLight
                                                    )
                                                }
                                                WorkloadSortMode.COUT -> {
                                                    // 1ère ligne : Coût de missions affiché en haut et mis en valeur
                                                    Text(
                                                        text = "%.3f TND".format(item.totalCost),
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 13.sp,
                                                        color = if (isTop3) badgeTextColor else primaryColor
                                                    )
                                                    // 2ème ligne : Nombre de jours en couleur grisé
                                                    Text(
                                                        text = "${item.totalJours} j terrain",
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 10.sp,
                                                        color = SlateMedium
                                                    )
                                                    // 3ème ligne : Nombre de missions en couleur grisé
                                                    Text(
                                                        text = "${item.missionsCount} missions",
                                                        fontSize = 10.sp,
                                                        color = SlateLight
                                                    )
                                                }
                                                WorkloadSortMode.JOURS -> {
                                                    // 1ère ligne : Jours terrain mis en valeur
                                                    Text(
                                                        text = "${item.totalJours} jours",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 13.sp,
                                                        color = if (isTop3) badgeTextColor else primaryColor
                                                    )
                                                    // 2ème ligne : Nombre de missions en couleur grisé
                                                    Text(
                                                        text = "${item.missionsCount} missions",
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 10.sp,
                                                        color = SlateMedium
                                                    )
                                                    // 3ème ligne : Coût en couleur grisé
                                                    Text(
                                                        text = "Coût: %.3f TND".format(item.totalCost),
                                                        fontSize = 10.sp,
                                                        color = SlateLight
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Gradient progress bar for the top 3, standard for the rest
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFE2E8F0))
                                    ) {
                                        if (topGradient != null) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(fraction)
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(topGradient)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(fraction)
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(primaryColor)
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
private fun FraisSplitBar(
    heberg: Double,
    transp: Double,
    divers: Double,
    modifier: Modifier = Modifier,
    height: Dp = 14.dp
) {
    val h = if (heberg.isNaN() || heberg < 0.0) 0.0 else heberg
    val t = if (transp.isNaN() || transp < 0.0) 0.0 else transp
    val d = if (divers.isNaN() || divers < 0.0) 0.0 else divers
    val total = h + t + d

    if (total <= 0.001) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(Color(0xFFE2E8F0)),
            contentAlignment = Alignment.Center
        ) {
            Text("Aucun frais comptabilisé", fontSize = 9.sp, color = SlateLight)
        }
    } else {
        val pHeberg = (h / total).toFloat().coerceIn(0f, 1f)
        val pTransp = (t / total).toFloat().coerceIn(0f, 1f)
        val pDivers = (d / total).toFloat().coerceIn(0f, 1f)

        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(height / 2))
        ) {
            if (pHeberg > 0.001f) {
                Box(modifier = Modifier.weight(pHeberg).fillMaxHeight().background(CobaltPrimary))
            }
            if (pTransp > 0.001f) {
                Box(modifier = Modifier.weight(pTransp).fillMaxHeight().background(SkyAccent))
            }
            if (pDivers > 0.001f) {
                Box(modifier = Modifier.weight(pDivers).fillMaxHeight().background(AmberGold))
            }
        }
    }
}

@Composable
private fun FraisLegendRow(
    heberg: Double,
    transp: Double,
    divers: Double,
    showPercentages: Boolean = true
) {
    val h = if (heberg.isNaN() || heberg < 0.0) 0.0 else heberg
    val t = if (transp.isNaN() || transp < 0.0) 0.0 else transp
    val d = if (divers.isNaN() || divers < 0.0) 0.0 else divers
    val total = (h + t + d).coerceAtLeast(0.001)

    val pctH = if (total > 0.01) ((h / total) * 100).toInt().coerceIn(0, 100) else 0
    val pctT = if (total > 0.01) ((t / total) * 100).toInt().coerceIn(0, 100) else 0
    val pctD = if (total > 0.01) ((d / total) * 100).toInt().coerceIn(0, 100) else 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LegendItem(
            color = CobaltPrimary,
            label = "Hébergement",
            amount = "%.3f TND".format(h) + if (showPercentages && total > 0.01) " ($pctH%)" else ""
        )
        LegendItem(
            color = SkyAccent,
            label = "Transport",
            amount = "%.3f TND".format(t) + if (showPercentages && total > 0.01) " ($pctT%)" else ""
        )
        LegendItem(
            color = AmberGold,
            label = "Divers",
            amount = "%.3f TND".format(d) + if (showPercentages && total > 0.01) " ($pctD%)" else ""
        )
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(subtitle, fontSize = 11.sp, color = SlateMedium)
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, amount: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Text(amount, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
