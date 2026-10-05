package com.example.ui.screens.validation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldGreen

@Composable
fun ValidateStepDialog(
    lotN: String,
    directionCode: String,
    directionName: String,
    defaultSignataire: String,
    onDismiss: () -> Unit,
    onConfirm: (isApproved: Boolean, commentaire: String, signataire: String) -> Unit
) {
    var isApproved by remember { mutableStateOf(true) }
    var signataire by remember { mutableStateOf(defaultSignataire) }
    var commentaire by remember { mutableStateOf("") }
    var isCommentError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Visa & Décision Hiérarchique",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "$directionCode - $directionName",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Dossier Lot N° : $lotN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Information banner on dual notification
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚡ À chaque validation, zODD transmet un e-mail officiel à la direction concernée avec le rapport PDF joint, et alerte immédiatement le Directeur sur WhatsApp pour attirer son attention sur l'e-mail et la nécessité de le valider sous peu.",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Decision selector (Approve vs Reject)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isApproved,
                        onClick = {
                            isApproved = true
                            isCommentError = false
                        },
                        label = { Text("Approuver (Valider)", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldGreen.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldGreen
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isApproved,
                        onClick = {
                            isApproved = false
                        },
                        label = { Text("Rejeter Dossier", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimsonRed.copy(alpha = 0.2f),
                            selectedLabelColor = CrimsonRed
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = signataire,
                    onValueChange = { signataire = it },
                    label = { Text("Nom du Signataire Responsable") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = commentaire,
                    onValueChange = {
                        commentaire = it
                        if (it.isNotBlank()) isCommentError = false
                    },
                    label = { Text(if (isApproved) "Commentaire ou observations (facultatif)" else "Motif impératif du refus") },
                    placeholder = {
                        Text(if (isApproved) "ex. Ordre de mission conforme au planning technique." else "ex. Justificatif de transport manquant.")
                    },
                    minLines = 3,
                    isError = isCommentError,
                    supportingText = if (isCommentError) {
                        { Text("Le motif de refus est obligatoire pour rejeter un dossier.") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isApproved && commentaire.isBlank()) {
                        isCommentError = true
                    } else {
                        onConfirm(
                            isApproved,
                            if (commentaire.isBlank() && isApproved) "Validé conforme" else commentaire,
                            signataire.ifBlank { defaultSignataire }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isApproved) EmeraldGreen else CrimsonRed
                )
            ) {
                Text(if (isApproved) "Confirmer la Validation" else "Signer le Refus")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
