package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ValidationEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DirectionTimeline(
    validations: List<ValidationEntity>,
    currentDirection: String,
    modifier: Modifier = Modifier
) {
    val directions = listOf("DM", "DCT", "DGRT", "DCST", "AUDIT", "DG", "DAF")
    val dateFormat = SimpleDateFormat("dd/MM", Locale.FRANCE)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Circuit de Signature Hiérarchique (DM -> DAF)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                directions.forEachIndexed { index, dirCode ->
                    val step = validations.firstOrNull { it.direction == dirCode }
                    val isValidated = step?.valide == "VALIDE"
                    val isRejected = step?.valide == "REFUSE"
                    val isCurrent = currentDirection == dirCode && !isValidated && !isRejected

                    // Node Circle & Label
                    val notifDate: Long? = if (index == 0) {
                        step?.updatedAt
                    } else {
                        val prevStep = validations.firstOrNull { it.direction == directions[index - 1] }
                        prevStep?.dateValidation ?: if (isCurrent) step?.updatedAt else null
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(82.dp)
                    ) {
                        val (circleBg, iconTint, icon) = when {
                            isValidated -> Triple(EmeraldGreen, Color.White, Icons.Default.Check)
                            isRejected -> Triple(CrimsonRed, Color.White, Icons.Default.Close)
                            isCurrent -> Triple(AmberGold, Color.White, Icons.Default.HourglassTop)
                            else -> Triple(Color(0xFFE2E8F0), Color(0xFF64748B), Icons.Default.Schedule)
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(circleBg)
                                .border(
                                    width = if (isCurrent) 2.dp else 0.dp,
                                    color = if (isCurrent) AmberLight else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = dirCode,
                            fontSize = 11.5.sp,
                            fontWeight = if (isCurrent || isValidated) FontWeight.Bold else FontWeight.Normal,
                            color = if (isValidated) EmeraldGreen else if (isCurrent) AmberGold else MaterialTheme.colorScheme.onSurface
                        )

                        // 1. Date de notification
                        Text(
                            text = if (notifDate != null && (index == 0 || prevStepValidated(validations, directions, index))) {
                                "Notif: ${dateFormat.format(Date(notifDate))}"
                            } else {
                                "Notif: --"
                            },
                            fontSize = 8.sp,
                            color = SlateMedium,
                            maxLines = 1
                        )

                        // 2. Date de validation juste au dessous
                        Text(
                            text = when {
                                isValidated && step?.dateValidation != null -> "Validé: ${dateFormat.format(Date(step.dateValidation))}"
                                isRejected -> "Rejeté"
                                isCurrent -> "En attente"
                                else -> "En attente"
                            },
                            fontSize = 8.sp,
                            fontWeight = if (isValidated) FontWeight.Bold else FontWeight.Normal,
                            color = if (isValidated) EmeraldGreen else if (isCurrent) AmberGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    // Connector line between nodes
                    if (index < directions.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(2.dp)
                                .background(
                                    if (isValidated) EmeraldGreen else Color(0xFFCBD5E1)
                                )
                        )
                    }
                }
            }
        }
    }
}

private fun prevStepValidated(validations: List<ValidationEntity>, directions: List<String>, index: Int): Boolean {
    if (index == 0) return true
    val prevCode = directions.getOrNull(index - 1) ?: return false
    val prevStep = validations.firstOrNull { it.direction == prevCode }
    return prevStep?.valide == "VALIDE"
}
