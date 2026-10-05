package com.example.ui.screens.lots

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddEditLotDialog(
    onDismiss: () -> Unit,
    onConfirm: (lotN: String, libelle: String, detailDivers: String) -> Unit
) {
    val yearSuffix = SimpleDateFormat("yy", Locale.FRANCE).format(Date())
    val randomNum = (10..99).random()
    var lotN by remember { mutableStateOf("TAT-00$randomNum-$yearSuffix") }
    var libelle by remember { mutableStateOf("") }
    var detailDivers by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nouveau Lot de Missions (LOT_N)",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Regroupement des interventions et ordres de déplacement",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = lotN,
                    onValueChange = { lotN = it.uppercase() },
                    label = { Text("Numéro de Lot (LOT_N)") },
                    placeholder = { Text("ex. TAT-0024-26") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = libelle,
                    onValueChange = {
                        libelle = it
                        isError = false
                    },
                    label = { Text("Libellé / Désignation du Lot") },
                    placeholder = { Text("ex. Renfort Hangar et Dépannage A320") },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("Veuillez saisir un libellé descriptif") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = detailDivers,
                    onValueChange = { detailDivers = it },
                    label = { Text("Détails divers / Remarques (Optionnel)") },
                    placeholder = { Text("ex. Priorité haute flotte A320/A330, outillage spécifique...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (libelle.isBlank()) {
                        isError = true
                    } else {
                        onConfirm(lotN, libelle, detailDivers)
                    }
                }
            ) {
                Text("Créer le Lot")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
