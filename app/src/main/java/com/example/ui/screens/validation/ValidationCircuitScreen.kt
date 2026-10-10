package com.example.ui.screens.validation

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DirectionEntity
import com.example.data.local.LotEntity
import com.example.data.local.ValidationEntity
import com.example.ui.components.DirectionTimeline
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ValidationCircuitScreen(
    lots: List<LotEntity>,
    directions: List<DirectionEntity>,
    allValidations: List<ValidationEntity>,
    onSelectLot: (String) -> Unit,
    onValidateStep: (lotN: String, dirCode: String, isApproved: Boolean, comment: String, signataire: String) -> Unit,
    onOpenPdf: (Context, String) -> Unit,
    onTriggerNotification: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeUser by com.example.data.auth.AuthManager.currentUser.collectAsStateWithLifecycle()
    val isNahla = activeUser?.displayName?.contains("Nahla", ignoreCase = true) == true ||
            activeUser?.role?.contains("Gestion", ignoreCase = true) == true

    val dirCodes = listOf("Toutes", "DM", "DCT", "DGRT", "DCST", "AUDIT", "DG", "DAF", "ARCHIVE")
    var selectedFilter by remember { mutableStateOf("Toutes") }
    var selectedLotForAction by remember { mutableStateOf<Pair<LotEntity, String>?>(null) }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)

    val lotsUnderValidation = lots.filter { lot ->
        lot.figerLot
    }

    val filteredLots = lotsUnderValidation.filter { lot ->
        if (selectedFilter == "Toutes") true
        else if (selectedFilter == "ARCHIVE") lot.directionActuelle == "ARCHIVE" || lot.statutValidation == "ARCHIVE"
        else lot.directionActuelle == selectedFilter
    }

    Scaffold(modifier = modifier) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Circuit de Validation Hiérarchique",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Approbation progressive des Ordres de Déplacement et transmission automatique par e-mail.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Direction selector chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(dirCodes) { dir ->
                        val count = if (dir == "Toutes") lotsUnderValidation.size
                        else if (dir == "ARCHIVE") lots.count { it.directionActuelle == "ARCHIVE" || it.statutValidation == "ARCHIVE" }
                        else lotsUnderValidation.count { it.directionActuelle == dir }

                        FilterChip(
                            selected = selectedFilter == dir,
                            onClick = { selectedFilter = dir },
                            label = { Text("$dir ($count)", fontSize = 12.sp) }
                        )
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
                            Icon(Icons.Default.TaskAlt, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(52.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Aucun dossier en attente pour cette direction",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tous les ordres de déplacement sont à jour ou déjà transmis.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredLots, key = { it.lotN }) { lot ->
                    val lotValidations = allValidations.filter { it.lotN == lot.lotN }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(lot.lotN, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                                StatusBadge(status = lot.statutValidation, isFige = lot.figerLot)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(lot.libelle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Créé le : ${dateFormat.format(Date(lot.dateCreation))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Total : %.3f TND".format(lot.totalCoutOdd), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            DirectionTimeline(
                                validations = lotValidations,
                                currentDirection = lot.directionActuelle
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (lot.figerLot) {
                                    OutlinedButton(
                                        onClick = { onOpenPdf(context, lot.lotN) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("PDF", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { onTriggerNotification(lot.lotN) },
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Notifier", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                if (lot.directionActuelle != "ARCHIVE") {
                                    val canViser = isNahla || (activeUser?.dirCode == lot.directionActuelle)

                                    Button(
                                        onClick = {
                                            if (canViser) {
                                                selectedLotForAction = Pair(lot, lot.directionActuelle)
                                            }
                                        },
                                        enabled = canViser,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldGreen,
                                            disabledContainerColor = Color(0xFFE2E8F0),
                                            disabledContentColor = SlateMedium
                                        ),
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (canViser) "Viser (${lot.directionActuelle})" else "Réservé à ${lot.directionActuelle}",
                                            fontSize = 11.5.sp
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = { onSelectLot(lot.lotN) },
                                        colors = ButtonDefaults.buttonColors(containerColor = AviationBlue),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Détails ODD", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedLotForAction != null) {
        val (lot, dir) = selectedLotForAction!!
        val dirInfo = directions.firstOrNull { it.code == dir }
        ValidateStepDialog(
            lotN = lot.lotN,
            directionCode = dir,
            directionName = dirInfo?.libelle ?: "Direction $dir",
            defaultSignataire = dirInfo?.directeur ?: "Directeur $dir",
            onDismiss = { selectedLotForAction = null },
            onConfirm = { isApproved, comment, signataire ->
                onValidateStep(lot.lotN, dir, isApproved, comment, signataire)
                selectedLotForAction = null
            }
        )
    }
}
