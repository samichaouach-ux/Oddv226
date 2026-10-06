package com.example.ui.screens.lots

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Route
import androidx.compose.ui.Alignment
import com.example.data.local.BaremeTarifEntity
import com.example.data.local.OddEntity
import com.example.ui.theme.AviationNavy
import com.example.ui.theme.SlateMedium
import com.example.util.SiteDistances
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditFraisDialog(
    odds: List<OddEntity>,
    baremes: List<BaremeTarifEntity>,
    preselectedOddN: String? = null,
    existingFrais: com.example.data.local.FraisEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        idFrais: String,
        oddN: String,
        categorie: String,
        sousCategorie: String,
        unite: String,
        quantite: Double,
        pUnit: Double,
        estComptabilise: Boolean
    ) -> Unit
) {
    val randomId = (100..999).random()
    var idFrais by remember { mutableStateOf(existingFrais?.idFrais ?: "FR-$randomId-26") }
    var selectedOddN by remember { mutableStateOf(existingFrais?.oddN ?: preselectedOddN ?: odds.firstOrNull()?.oddN ?: "") }
    var estComptabilise by remember { mutableStateOf(existingFrais?.estComptabilise ?: true) }

    val categories = listOf("Hébergement", "Transport", "Divers")
    var selectedCat by remember { mutableStateOf(existingFrais?.categorie ?: categories[0]) }

    val sousCategories = when (selectedCat) {
        "Hébergement" -> listOf("LPD", "1/2 Pension", "Pension complète")
        "Transport" -> listOf("Voiture personnelle", "Transport en commun", "Escale")
        else -> listOf("Repas", "Autoroute", "Indemnité spécifique")
    }
    var selectedSousCat by remember(selectedCat) { mutableStateOf(existingFrais?.sousCategorie ?: sousCategories.first()) }

    val defaultUnite = when (selectedCat) {
        "Hébergement" -> "Nuité"
        "Transport" -> if (selectedSousCat == "Voiture personnelle") "Taux/Km" else "FF"
        else -> if (selectedSousCat == "Repas") "Repas" else "FF"
    }
    var unite by remember(selectedCat, selectedSousCat) { mutableStateOf(existingFrais?.unite ?: defaultUnite) }

    var quantiteStr by remember {
        mutableStateOf(
            existingFrais?.quantite?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "1"
        )
    }
    var pUnitStr by remember(selectedCat, selectedSousCat) {
        if (existingFrais != null && existingFrais.categorie == selectedCat && existingFrais.sousCategorie == selectedSousCat) {
            mutableStateOf(String.format(Locale.US, "%.3f", existingFrais.pUnit))
        } else {
            val defaultPUnit = when {
                selectedCat == "Hébergement" && selectedSousCat == "LPD" -> "110.000"
                selectedCat == "Hébergement" && selectedSousCat == "1/2 Pension" -> "140.000"
                selectedCat == "Hébergement" && selectedSousCat == "Pension complète" -> "180.000"
                selectedCat == "Transport" && selectedSousCat == "Voiture personnelle" -> "0.450"
                selectedCat == "Transport" && selectedSousCat == "Escale" -> "60.750"
                selectedCat == "Divers" && selectedSousCat == "Repas" -> "25.000"
                selectedCat == "Divers" && selectedSousCat == "Autoroute" -> "12.000"
                else -> "50.000"
            }
            mutableStateOf(defaultPUnit)
        }
    }

    var expandedOdd by remember { mutableStateOf(false) }
    var expandedCat by remember { mutableStateOf(false) }
    var expandedSousCat by remember { mutableStateOf(false) }

    val currentOdd = odds.firstOrNull { it.oddN == selectedOddN }
    val dureeMissionJours = remember(currentOdd) {
        if (currentOdd != null) SiteDistances.getMissionDurationDays(currentOdd.dateDebut, currentOdd.dateFin) else 2
    }
    val delaiHebergementAuto = remember(dureeMissionJours) {
        SiteDistances.getDelaiHebergementAuto(dureeMissionJours)
    }

    // Mise à jour automatique de la quantité pour l'hébergement
    LaunchedEffect(selectedCat, currentOdd) {
        if (selectedCat == "Hébergement") {
            quantiteStr = delaiHebergementAuto.toString()
            unite = "Nuité"
        }
    }

    val quantite = quantiteStr.toDoubleOrNull() ?: 1.0
    val pUnit = pUnitStr.toDoubleOrNull() ?: 0.0
    val montantBrut = quantite * pUnit
    val totalAComptabiliser = if (estComptabilise) montantBrut else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingFrais != null) "Modifier le Frais de Mission" else "Ajouter une Ligne de Frais",
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
                    value = idFrais,
                    onValueChange = { idFrais = it.uppercase() },
                    label = { Text("ID Frais (ID_FRAIS)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Select ODD
                ExposedDropdownMenuBox(
                    expanded = expandedOdd,
                    onExpandedChange = { expandedOdd = it }
                ) {
                    OutlinedTextField(
                        value = selectedOddN.ifBlank { "Sélectionner un ODD..." },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("ODD Rattaché (ODD_N)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOdd) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedOdd,
                        onDismissRequest = { expandedOdd = false }
                    ) {
                        odds.forEach { odd ->
                            DropdownMenuItem(
                                text = { Text("${odd.oddN} (${odd.siteProvenance} -> ${odd.siteIntervention})") },
                                onClick = {
                                    selectedOddN = odd.oddN
                                    expandedOdd = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Select Catégorie
                ExposedDropdownMenuBox(
                    expanded = expandedCat,
                    onExpandedChange = { expandedCat = it }
                ) {
                    OutlinedTextField(
                        value = selectedCat,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Catégorie Frais") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCat,
                        onDismissRequest = { expandedCat = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCat = cat
                                    expandedCat = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Select Sous-Catégorie
                ExposedDropdownMenuBox(
                    expanded = expandedSousCat,
                    onExpandedChange = { expandedSousCat = it }
                ) {
                    OutlinedTextField(
                        value = selectedSousCat,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sous-Catégorie / Désignation") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSousCat) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSousCat,
                        onDismissRequest = { expandedSousCat = false }
                    ) {
                        sousCategories.forEach { sc ->
                            DropdownMenuItem(
                                text = { Text(sc) },
                                onClick = {
                                    selectedSousCat = sc
                                    expandedSousCat = false
                                }
                            )
                        }
                    }
                }

                // If accommodation selected, show automatic duration - 1 calculation banner
                if (selectedCat == "Hébergement" && currentOdd != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "🏨 Règle Hébergement Automatique :",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Durée mission : $dureeMissionJours jour(s)\nDélai hébergements = Durée - 1 = $delaiHebergementAuto nuitée(s)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    quantiteStr = delaiHebergementAuto.toString()
                                    unite = "Nuité"
                                },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Appliquer $delaiHebergementAuto nuitée(s)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // If transport & car selected, show 50 km deduction calculator for the chosen ODD
                if (selectedCat == "Transport" && currentOdd != null) {
                    val rawDistKm = SiteDistances.getDistanceKm(currentOdd.siteProvenance, currentOdd.siteIntervention)
                    val rawArKm = rawDistKm * 2
                    val netKm = SiteDistances.getNetDistanceKm(rawDistKm, allerRetour = true)

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Route, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "🚗 Transport Personnel : ((Distance × 2) - 50 km) × Taux/km",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Trajet inter-sites : ${currentOdd.siteProvenance} ➔ ${currentOdd.siteIntervention}\n" +
                                        "Distance simple : $rawDistKm km | Aller-Retour ($rawDistKm × 2) : $rawArKm km\n" +
                                        "Formule : (($rawDistKm × 2) - 50) = $netKm km retenus pour indemnisation",
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    quantiteStr = netKm.toString()
                                    unite = "Km"
                                },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Appliquer $netKm km retenus en Quantité", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Unité & Quantité
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = unite,
                        onValueChange = { unite = it },
                        label = { Text("Unité") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = quantiteStr,
                        onValueChange = { quantiteStr = it },
                        label = { Text("Quantité") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Prix unitaire
                OutlinedTextField(
                    value = pUnitStr,
                    onValueChange = { pUnitStr = it },
                    label = { Text("Prix Unitaire (P_Unit en TND)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // BOUTON RADIO : À COMPTABILISER (OUI / NON)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "À comptabiliser au total des frais :",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = estComptabilise,
                                onClick = { estComptabilise = true }
                            )
                            Text(
                                text = "Oui (Inclure au total des frais)",
                                fontSize = 12.sp,
                                fontWeight = if (estComptabilise) FontWeight.Bold else FontWeight.Normal,
                                color = if (estComptabilise) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = !estComptabilise,
                                onClick = { estComptabilise = false }
                            )
                            Text(
                                text = "Non (Exclure du total des frais)",
                                fontSize = 12.sp,
                                fontWeight = if (!estComptabilise) FontWeight.Bold else FontWeight.Normal,
                                color = if (!estComptabilise) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Total Calculated Box
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (estComptabilise) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Montant brut du frais :", fontSize = 12.sp)
                            Text(
                                text = "%.3f TND".format(Locale.US, montantBrut),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (estComptabilise) "Pris en compte au total :" else "Pris en compte au total (Exclu) :",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (estComptabilise) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "%.3f TND".format(Locale.US, totalAComptabiliser),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = if (estComptabilise) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedOddN.isNotBlank()) {
                        onConfirm(
                            idFrais,
                            selectedOddN,
                            selectedCat,
                            selectedSousCat,
                            unite,
                            quantite,
                            pUnit,
                            estComptabilise
                        )
                    }
                }
            ) {
                Text(if (existingFrais != null) "Enregistrer les Modifications" else "Enregistrer Frais")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
