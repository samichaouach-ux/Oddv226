package com.example.ui.screens.lots

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Info
import com.example.data.local.OddEntity
import com.example.data.local.TechnicienEntity
import com.example.ui.theme.AviationNavy
import com.example.ui.theme.SlateMedium
import com.example.util.SiteDistances
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditOddDialog(
    lotN: String,
    techniciens: List<TechnicienEntity>,
    existingOdd: OddEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        oddN: String,
        matricule: String,
        siteProvenance: String,
        echelle: Int,
        siteIntervention: String,
        dateDebut: Long,
        dateFin: Long,
        mission: String,
        avanceSurMission: Double,
        detailDivers: String
    ) -> Unit
) {
    val sites = listOf("Tunis", "Monastir", "Sfax", "Djerba", "Tozeur")
    val missions = listOf(
        "💡 ASSISTANCE TECHNIQUE",
        "🛠️ RENFORT HANGAR",
        "✈️ Intervention sur avion",
        "🩺 Visite médicale",
        "🎓 Formation",
        "📋 Visite licence",
        "🚐 Transport"
    )

    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }

    val randomNum = (100..999).random()
    var oddN by remember { mutableStateOf(existingOdd?.oddN ?: "ODD-$randomNum-26") }
    var selectedTech by remember {
        mutableStateOf(
            if (existingOdd != null) techniciens.firstOrNull { it.matricule == existingOdd.matricule } ?: techniciens.firstOrNull()
            else techniciens.firstOrNull()
        )
    }
    var siteProvenance by remember { mutableStateOf(existingOdd?.siteProvenance ?: selectedTech?.siteProvenance ?: "Tunis") }
    var echelle by remember { mutableIntStateOf(existingOdd?.echelle ?: selectedTech?.echelle ?: 500) }
    var siteIntervention by remember { mutableStateOf(existingOdd?.siteIntervention ?: "Monastir") }
    var selectedMission by remember {
        val existing = existingOdd?.mission
        val match = if (existing != null) {
            missions.firstOrNull {
                it.contains(existing, ignoreCase = true) || existing.contains(it, ignoreCase = true) ||
                it.replace(Regex("[^a-zA-Z0-9]"), "").trim().equals(existing.replace(Regex("[^a-zA-Z0-9]"), "").trim(), ignoreCase = true)
            } ?: existing
        } else {
            missions[0]
        }
        mutableStateOf(match)
    }
    var detailDivers by remember { mutableStateOf(existingOdd?.detailDivers ?: "") }
    
    // Dates de début et fin de mission
    var dateDebutMillis by remember {
        mutableLongStateOf(existingOdd?.dateDebut ?: System.currentTimeMillis())
    }
    var dateFinMillis by remember {
        mutableLongStateOf(existingOdd?.dateFin ?: (System.currentTimeMillis() + 2 * 86400000L))
    }
    var dateDebutStr by remember {
        mutableStateOf(sdf.format(Date(dateDebutMillis)))
    }
    var dateFinStr by remember {
        mutableStateOf(sdf.format(Date(dateFinMillis)))
    }

    val dureeJours by remember(dateDebutMillis, dateFinMillis) {
        derivedStateOf {
            SiteDistances.getMissionDurationDays(dateDebutMillis, dateFinMillis)
        }
    }
    val delaiHebergementAuto by remember(dureeJours) {
        derivedStateOf {
            SiteDistances.getDelaiHebergementAuto(dureeJours)
        }
    }

    var avanceSurMissionStr by remember {
        val initialAvance = if (existingOdd != null && existingOdd.avanceSurMission > 0.0) {
            String.format(Locale.US, "%.3f", existingOdd.avanceSurMission)
        } else "0.000"
        mutableStateOf(initialAvance)
    }

    var expandedTech by remember { mutableStateOf(false) }
    var expandedProvenance by remember { mutableStateOf(false) }
    var expandedIntervention by remember { mutableStateOf(false) }
    var expandedMission by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingOdd != null) "Modifier l'ODD (${existingOdd.oddN})" else "Nouvel Ordre de Déplacement (ODD)",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = oddN,
                    onValueChange = { oddN = it.uppercase() },
                    label = { Text("Numéro ODD (ODD_N)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Select Technician Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedTech,
                    onExpandedChange = { expandedTech = it }
                ) {
                    OutlinedTextField(
                        value = selectedTech?.let { "${it.matricule} - ${it.nom} ${it.prenom} (Éch. ${it.echelle})" } ?: "Sélectionner...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Technicien Assigné (MATRICULE)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTech) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTech,
                        onDismissRequest = { expandedTech = false }
                    ) {
                        techniciens.forEach { tech ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("${tech.matricule} • ${tech.nom} ${tech.prenom}", fontWeight = FontWeight.SemiBold)
                                        Text("${tech.fonction} • Site: ${tech.siteProvenance} • Échelle ${tech.echelle}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    selectedTech = tech
                                    siteProvenance = tech.siteProvenance
                                    echelle = tech.echelle
                                    expandedTech = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Site Provenance Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedProvenance,
                    onExpandedChange = { expandedProvenance = it }
                ) {
                    OutlinedTextField(
                        value = siteProvenance,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Site de Provenance") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProvenance) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedProvenance,
                        onDismissRequest = { expandedProvenance = false }
                    ) {
                        sites.forEach { site ->
                            DropdownMenuItem(
                                text = { Text(site) },
                                onClick = {
                                    siteProvenance = site
                                    expandedProvenance = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Site Intervention Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedIntervention,
                    onExpandedChange = { expandedIntervention = it }
                ) {
                    OutlinedTextField(
                        value = siteIntervention,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Site d'Intervention") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedIntervention) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedIntervention,
                        onDismissRequest = { expandedIntervention = false }
                    ) {
                        sites.forEach { site ->
                            DropdownMenuItem(
                                text = { Text(site) },
                                onClick = {
                                    siteIntervention = site
                                    expandedIntervention = false
                                }
                            )
                        }
                    }
                }

                val currentDistKm = SiteDistances.getDistanceKm(siteProvenance, siteIntervention)
                val currentNetKm = SiteDistances.getNetDistanceKm(currentDistKm, allerRetour = true)
                val currentDuration = SiteDistances.getEstimatedDuration(currentDistKm)
                val currentEstimIndemnite = SiteDistances.getEstimatedIndemniteKm(currentDistKm, echelle, allerRetour = true)

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Route, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Distance Inter-Sites : $currentDistKm km (Aller simple)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${currentDistKm * 2} km A/R - 50 km déduits = $currentNetKm km nets (~$currentDuration)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Indemnité A/R :",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "%.3f TND".format(currentEstimIndemnite),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mission type Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedMission,
                    onExpandedChange = { expandedMission = it }
                ) {
                    OutlinedTextField(
                        value = selectedMission,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type d'Intervention (MISSION)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMission) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMission,
                        onDismissRequest = { expandedMission = false }
                    ) {
                        missions.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m) },
                                onClick = {
                                    selectedMission = m
                                    expandedMission = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Date début mission et Date fin mission
                Text(
                    text = "Calendrier de la Mission :",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = dateDebutStr,
                        onValueChange = { input ->
                            dateDebutStr = input
                            try {
                                val parsed = sdf.parse(input)
                                if (parsed != null) {
                                    dateDebutMillis = parsed.time
                                }
                            } catch (_: Exception) {}
                        },
                        label = { Text("Date début mission") },
                        placeholder = { Text("JJ/MM/AAAA") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = dateFinStr,
                        onValueChange = { input ->
                            dateFinStr = input
                            try {
                                val parsed = sdf.parse(input)
                                if (parsed != null) {
                                    dateFinMillis = parsed.time
                                }
                            } catch (_: Exception) {}
                        },
                        label = { Text("Date fin mission") },
                        placeholder = { Text("JJ/MM/AAAA") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick date adjuster chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Régler fin :", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf(1, 2, 3, 5).forEach { days ->
                        SuggestionChip(
                            onClick = {
                                val newEnd = dateDebutMillis + (days * 86400000L)
                                dateFinMillis = newEnd
                                dateFinStr = sdf.format(Date(newEnd))
                            },
                            label = { Text("+$days j", fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Durée calculée et délai hébergement automatique
                val isAssistance = SiteDistances.isAssistanceTechniqueMission(selectedMission)
                val isRenfort = SiteDistances.isRenfortHangarMission(selectedMission)
                val echelleFirstDigit = SiteDistances.getEchelleFirstDigit(echelle)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Durée mission : $dureeJours jour(s)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Échelle : $echelle (1er ch.: $echelleFirstDigit)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🏨 Délai hébergements calculé : $dureeJours j - 1 = $delaiHebergementAuto nuitée(s)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val ruleNote = when {
                            isAssistance -> "💡 Formule Assistance Tech : Calcul par tranches de 8 jours (⌈$dureeJours / 8⌉ = ${kotlin.math.ceil(dureeJours / 8.0).toInt()} tranche(s))"
                            isRenfort && dureeJours > 30 && echelleFirstDigit in 5..6 -> "🛠️ Formule Renfort Hangar : 30j plein tarif + (${dureeJours - 30}j × 2/3)"
                            echelleFirstDigit < 5 -> "🏨 Formule Échelle < 5 : Si hébergement rattaché ➔ 1 + ($dureeJours - 1) × 0.6"
                            else -> "⚡ Formule Standard : $dureeJours jour(s) × Taux unitaire"
                        }
                        Text(
                            text = ruleNote,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Avance sur mission
                OutlinedTextField(
                    value = avanceSurMissionStr,
                    onValueChange = { avanceSurMissionStr = it },
                    label = { Text("Avance sur mission accordée (TND)") },
                    placeholder = { Text("0.000") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    supportingText = {
                        Text(
                            "Montant déduit automatiquement du total brut de la mission.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Détail divers / Remarques de mission
                OutlinedTextField(
                    value = detailDivers,
                    onValueChange = { detailDivers = it },
                    label = { Text("Détails divers / Remarques de mission") },
                    placeholder = { Text("ex. Immatriculation avion TS-IMU, outillage spécifique, consigne...") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val matricule = selectedTech?.matricule ?: existingOdd?.matricule ?: "MAT-1042"
                    val avance = avanceSurMissionStr.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    onConfirm(
                        oddN,
                        matricule,
                        siteProvenance,
                        echelle,
                        siteIntervention,
                        dateDebutMillis,
                        dateFinMillis,
                        selectedMission,
                        avance,
                        detailDivers
                    )
                }
            ) {
                Text(if (existingOdd != null) "Mettre à jour l'ODD" else "Enregistrer l'ODD")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
