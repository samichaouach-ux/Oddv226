package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun StatusBadge(
    status: String,
    isFige: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f || MaterialTheme.colorScheme.background.luminance() < 0.5f
    val (bgColor, textColor, borderColor, icon, label) = when {
        status == "ARCHIVE" -> Quintuple(
            EmeraldLight, EmeraldGreen, EmeraldGreen, Icons.Default.Archive, "ARCHIVÉ DAF"
        )
        status.startsWith("VALIDE") -> Quintuple(
            EmeraldLight, EmeraldGreen, EmeraldGreen, Icons.Default.CheckCircle, status.replace('_', ' ')
        )
        status.startsWith("REFUSE") -> Quintuple(
            CrimsonLight, CrimsonRed, CrimsonRed, Icons.Default.Close, "REFUSÉ (${status.removePrefix("REFUSE_")})"
        )
        isFige || status.startsWith("FIGE") -> Quintuple(
            if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
            if (isDark) Color.White else Color.Black,
            if (isDark) Color(0xFF475569) else Color(0xFF94A3B8),
            Icons.Default.Lock,
            "FIGÉ (Lecture Seule)"
        )
        else -> Quintuple(
            AmberLight, AmberGold, AmberGold, Icons.Default.Edit, "EN ÉDITION"
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
