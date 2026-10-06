package com.example.ui.screens.lots

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.OddEntity
import com.example.ui.theme.AviationBlue
import com.example.ui.theme.EmeraldGreen
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachDocumentDialog(
    odds: List<OddEntity>,
    preselectedOddN: String? = null,
    existingDoc: com.example.data.local.DocumentRattachementEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (idDoc: String, oddN: String, typeDoc: String, objet: String, scanUri: String?) -> Unit
) {
    val context = LocalContext.current
    val randomId = remember { (100..999).random() }
    var idDoc by remember { mutableStateOf(existingDoc?.idDoc ?: "DOC-$randomId-26") }
    var selectedOddN by remember { mutableStateOf(existingDoc?.oddN ?: preselectedOddN ?: odds.firstOrNull()?.oddN ?: "") }

    val docTypes = listOf(
        "Facture hôtel / hébergement",
        "Ticket péage / autoroute",
        "Billet de transport / avion / train",
        "Note de frais repas",
        "Ordre de mission signé",
        "Autre justificatif"
    )
    var selectedType by remember {
        mutableStateOf(
            if (existingDoc != null && docTypes.contains(existingDoc.typeDocument)) existingDoc.typeDocument
            else docTypes.first()
        )
    }
    var objet by remember { mutableStateOf(existingDoc?.objet ?: "") }

    var attachedFileUri by remember { mutableStateOf<String?>(existingDoc?.scanDocUri) }
    var attachedFileName by remember { mutableStateOf<String?>(existingDoc?.scanDocUri?.substringAfterLast('/')) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Launcher for file selection (PDF, Image, etc.)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            attachedFileUri = it.toString()
            capturedBitmap = null
            // Extract filename if possible
            val path = it.lastPathSegment ?: "document_${System.currentTimeMillis()}"
            attachedFileName = path.substringAfterLast('/')
        }
    }

    // Launcher for camera scan
    val cameraScanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            capturedBitmap = it
            attachedFileName = "scan_camera_${System.currentTimeMillis()}.jpg"
            // Save to internal cache
            try {
                val file = File(context.cacheDir, "scan_$idDoc.jpg")
                val fos = FileOutputStream(file)
                it.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                fos.flush()
                fos.close()
                attachedFileUri = Uri.fromFile(file).toString()
            } catch (_: Exception) {
                attachedFileUri = "scan://$idDoc.jpg"
            }
        }
    }

    var expandedOdd by remember { mutableStateOf(false) }
    var expandedType by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingDoc != null) "Modifier la Pièce Justificative" else "Rattachement de Pièce Justificative",
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
                    value = idDoc,
                    onValueChange = { idDoc = it.uppercase() },
                    label = { Text("ID Document (ID_DOC)") },
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

                // Select Type Document
                ExposedDropdownMenuBox(
                    expanded = expandedType,
                    onExpandedChange = { expandedType = it }
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type de Document") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false }
                    ) {
                        docTypes.forEach { dt ->
                            DropdownMenuItem(
                                text = { Text(dt) },
                                onClick = {
                                    selectedType = dt
                                    expandedType = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = objet,
                    onValueChange = { objet = it },
                    label = { Text("Objet / Description de la pièce") },
                    placeholder = { Text("ex. Facture Hôtel Iberostar Djerba 2 nuitées") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Mode d'ajout de la pièce :",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Actions: File picker OR Camera Scan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Parcourir", fontSize = 11.5.sp)
                    }

                    Button(
                        onClick = { cameraScanLauncher.launch(null) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AviationBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scanner Caméra", fontSize = 11.5.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scan / capture status box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (attachedFileUri != null) EmeraldGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (attachedFileUri != null) androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f)) else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (capturedBitmap != null) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Aperçu scan",
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, EmeraldGreen, RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = if (attachedFileUri != null) Icons.Default.CheckCircle else Icons.Default.Description,
                                contentDescription = null,
                                tint = if (attachedFileUri != null) EmeraldGreen else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (attachedFileUri != null) "Pièce jointe prête" else "Aucun document sélectionné",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (attachedFileUri != null) EmeraldGreen else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = attachedFileName ?: "Fichier numérique par défaut généré si aucun scan externe",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        val scanUri = attachedFileUri ?: "scan://$idDoc.jpg"
                        onConfirm(idDoc, selectedOddN, selectedType, objet.ifBlank { selectedType }, scanUri)
                    }
                }
            ) {
                Text(if (existingDoc != null) "Enregistrer les Modifications" else "Rattacher le Justificatif")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
