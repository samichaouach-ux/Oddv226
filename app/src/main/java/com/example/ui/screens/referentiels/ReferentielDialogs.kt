package com.example.ui.screens.referentiels

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BaremeTarifEntity
import com.example.data.local.CoutMissionEntity
import com.example.data.local.DirectionEntity
import com.example.data.local.TechnicienEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTechnicienDialog(
    initialTechnicien: TechnicienEntity?,
    onDismiss: () -> Unit,
    onSave: (TechnicienEntity) -> Unit
) {
    val isEdit = initialTechnicien != null
    var matricule by remember { mutableStateOf(initialTechnicien?.matricule ?: "MAT-") }
    var civilite by remember { mutableStateOf(initialTechnicien?.civilite ?: "M.") }
    var nom by remember { mutableStateOf(initialTechnicien?.nom ?: "") }
    var prenom by remember { mutableStateOf(initialTechnicien?.prenom ?: "") }
    var siteProvenance by remember { mutableStateOf(initialTechnicien?.siteProvenance ?: "Tunis") }
    var fonction by remember { mutableStateOf(initialTechnicien?.fonction ?: "Technicien B1/B2") }
    var echelleText by remember { mutableStateOf(initialTechnicien?.echelle?.toString() ?: "500") }
    var rib by remember { mutableStateOf(initialTechnicien?.rib ?: "TN59 ") }
    var histJoursText by remember { mutableStateOf(initialTechnicien?.histJours?.toString() ?: "0") }
    var histMissionsText by remember { mutableStateOf(initialTechnicien?.histMissions?.toString() ?: "0") }

    val sites = listOf("Tunis", "Monastir", "Sfax", "Djerba", "Tozeur")
    var siteExpanded by remember { mutableStateOf(false) }

    val civilites = listOf("M.", "Mme")
    var civExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Modifier Technicien (${initialTechnicien?.matricule})" else "Nouveau Technicien",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = matricule,
                    onValueChange = { matricule = it.uppercase() },
                    label = { Text("Matricule (ex: MAT-1099)") },
                    enabled = !isEdit,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = civExpanded,
                        onExpandedChange = { civExpanded = !civExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = civilite,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Civilité") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = civExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = civExpanded,
                            onDismissRequest = { civExpanded = false }
                        ) {
                            civilites.forEach { civ ->
                                DropdownMenuItem(
                                    text = { Text(civ) },
                                    onClick = {
                                        civilite = civ
                                        civExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = siteExpanded,
                        onExpandedChange = { siteExpanded = !siteExpanded },
                        modifier = Modifier.weight(1.5f)
                    ) {
                        OutlinedTextField(
                            value = siteProvenance,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Site d'attache") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = siteExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = siteExpanded,
                            onDismissRequest = { siteExpanded = false }
                        ) {
                            sites.forEach { site ->
                                DropdownMenuItem(
                                    text = { Text(site) },
                                    onClick = {
                                        siteProvenance = site
                                        siteExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nom,
                        onValueChange = { nom = it },
                        label = { Text("Nom") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = prenom,
                        onValueChange = { prenom = it },
                        label = { Text("Prénom") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = fonction,
                    onValueChange = { fonction = it },
                    label = { Text("Fonction / Qualification") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = echelleText,
                        onValueChange = { echelleText = it },
                        label = { Text("Échelle (100-900)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = histMissionsText,
                        onValueChange = { histMissionsText = it },
                        label = { Text("Nb Missions") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = rib,
                    onValueChange = { rib = it },
                    label = { Text("RIB Bancaire (20 chiffres)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tech = TechnicienEntity(
                        matricule = matricule.trim(),
                        civilite = civilite,
                        nom = nom.trim(),
                        prenom = prenom.trim(),
                        siteProvenance = siteProvenance,
                        fonction = fonction.trim(),
                        echelle = echelleText.toIntOrNull() ?: 500,
                        rib = rib.trim(),
                        photo = initialTechnicien?.photo,
                        histJours = histJoursText.toIntOrNull() ?: 0,
                        histMissions = histMissionsText.toIntOrNull() ?: 0
                    )
                    onSave(tech)
                },
                enabled = matricule.isNotBlank() && nom.isNotBlank() && prenom.isNotBlank()
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun EditDirectionDialog(
    direction: DirectionEntity,
    onDismiss: () -> Unit,
    onSave: (DirectionEntity) -> Unit
) {
    var libelle by remember { mutableStateOf(direction.libelle) }
    var directeur by remember { mutableStateOf(direction.directeur) }
    var email by remember { mutableStateOf(direction.email) }
    var telephone by remember { mutableStateOf(direction.telephone) }
    var ordreText by remember { mutableStateOf(direction.ordre.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Modifier Direction ${direction.code}",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = libelle,
                    onValueChange = { libelle = it },
                    label = { Text("Nom complet de la Direction") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = directeur,
                    onValueChange = { directeur = it },
                    label = { Text("Directeur / Responsable signataire") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail institutionnel (Visa)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = telephone,
                        onValueChange = { telephone = it },
                        label = { Text("Téléphone") },
                        modifier = Modifier.weight(1.4f)
                    )
                    OutlinedTextField(
                        value = ordreText,
                        onValueChange = { ordreText = it },
                        label = { Text("Ordre circuit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.8f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = direction.copy(
                        libelle = libelle.trim(),
                        directeur = directeur.trim(),
                        email = email.trim(),
                        telephone = telephone.trim(),
                        ordre = ordreText.toIntOrNull() ?: direction.ordre
                    )
                    onSave(updated)
                },
                enabled = libelle.isNotBlank() && email.isNotBlank()
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBaremeDialog(
    initialBareme: BaremeTarifEntity?,
    onDismiss: () -> Unit,
    onSave: (BaremeTarifEntity) -> Unit
) {
    val isEdit = initialBareme != null
    var categorie by remember { mutableStateOf(initialBareme?.categorie ?: "Hébergement") }
    var designation by remember { mutableStateOf(initialBareme?.designation ?: "") }
    var echelleMinText by remember { mutableStateOf(initialBareme?.echelleMin?.toString() ?: "100") }
    var echelleMaxText by remember { mutableStateOf(initialBareme?.echelleMax?.toString() ?: "900") }
    var unite by remember { mutableStateOf(initialBareme?.unite ?: "Nuité") }
    var tarifText by remember { mutableStateOf(initialBareme?.tarifUnitaire?.let { "%.3f".format(it) } ?: "80.000") }

    val categories = listOf("Hébergement", "Transport", "Divers")
    var catExpanded by remember { mutableStateOf(false) }

    val unites = listOf("Jour", "Nuité", "Taux/Km", "Repas", "FF")
    var uniteExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Modifier Barème Tarifaire" else "Nouveau Barème",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded }
                ) {
                    OutlinedTextField(
                        value = categorie,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Catégorie") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    categorie = cat
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("Désignation / Intitulé") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = echelleMinText,
                        onValueChange = { echelleMinText = it },
                        label = { Text("Échelle Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = echelleMaxText,
                        onValueChange = { echelleMaxText = it },
                        label = { Text("Échelle Max") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = uniteExpanded,
                        onExpandedChange = { uniteExpanded = !uniteExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = unite,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unité") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uniteExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = uniteExpanded,
                            onDismissRequest = { uniteExpanded = false }
                        ) {
                            unites.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        unite = u
                                        uniteExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = tarifText,
                        onValueChange = { tarifText = it },
                        label = { Text("Tarif Unitaire (TND)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rate = tarifText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val entity = BaremeTarifEntity(
                        id = initialBareme?.id ?: 0,
                        categorie = categorie,
                        designation = designation.trim(),
                        echelleMin = echelleMinText.toIntOrNull() ?: 100,
                        echelleMax = echelleMaxText.toIntOrNull() ?: 900,
                        unite = unite,
                        tarifUnitaire = rate
                    )
                    onSave(entity)
                },
                enabled = designation.isNotBlank()
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCoutMissionDialog(
    initialCout: CoutMissionEntity?,
    onDismiss: () -> Unit,
    onSave: (CoutMissionEntity) -> Unit
) {
    val isEdit = initialCout != null
    val missionTypes = listOf(
        "Intervention sur avion",
        "Renfort hangar",
        "Assistance technique",
        "Formation",
        "Visite médicale",
        "Visite licence",
        "Transport"
    )
    var typeMission by remember { mutableStateOf(initialCout?.typeMission ?: missionTypes.first()) }
    var typeExpanded by remember { mutableStateOf(false) }

    var echelleMinText by remember { mutableStateOf(initialCout?.echelleMin?.toString() ?: "1") }
    var echelleMaxText by remember { mutableStateOf(initialCout?.echelleMax?.toString() ?: "5") }
    var tauxJournalierText by remember { mutableStateOf(initialCout?.coutUnitaireJournalier?.toString() ?: "65.0") }
    var observation by remember { mutableStateOf(initialCout?.observation ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Modifier Coût Unitaire Mission" else "Nouveau Coût Unitaire Mission",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Ce coût unitaire (taux journalier) sera appliqué pour cette tranche d'échelle lors du calcul automatique des missions ODD.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = typeMission,
                        onValueChange = { typeMission = it },
                        label = { Text("Type de Mission") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        missionTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    typeMission = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = echelleMinText,
                        onValueChange = { echelleMinText = it },
                        label = { Text("Échelle Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = echelleMaxText,
                        onValueChange = { echelleMaxText = it },
                        label = { Text("Échelle Max") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = tauxJournalierText,
                    onValueChange = { tauxJournalierText = it },
                    label = { Text("Taux Journalier (TND / jour)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = observation,
                    onValueChange = { observation = it },
                    label = { Text("Observation / Remarques (Optionnel)") },
                    placeholder = { Text("ex. Taux journalier forfaitaire intervention...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rate = tauxJournalierText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val entity = CoutMissionEntity(
                        id = initialCout?.id ?: 0,
                        typeMission = typeMission.trim(),
                        echelleMin = echelleMinText.toIntOrNull() ?: 1,
                        echelleMax = echelleMaxText.toIntOrNull() ?: 20,
                        coutUnitaireJournalier = rate,
                        observation = observation.trim()
                    )
                    onSave(entity)
                },
                enabled = typeMission.isNotBlank()
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
