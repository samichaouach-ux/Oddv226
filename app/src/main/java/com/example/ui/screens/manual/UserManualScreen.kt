package com.example.ui.screens.manual

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import com.example.util.PdfReportGenerator
import kotlin.math.roundToInt

data class ManualChapter(
    val id: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tag: String,
    val sections: List<ManualSection>
)

data class ManualSection(
    val sectionTitle: String,
    val content: String,
    val frontEndAction: String? = null,
    val backEndProcess: String? = null,
    val tips: List<String> = emptyList(),
    val warning: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManualScreen(
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val chapters = remember { getManualChapters() }
    val expandedChapters = remember { mutableStateMapOf<Int, Boolean>().apply { put(1, true) } }

    // Text Zoom State (1.0 = 100%, ranging from 0.75f / 75% to 2.0f / 200%)
    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    var showContinuousSlider by remember { mutableStateOf(false) }

    // Dynamic font scaling helper
    val zsp: (Number) -> TextUnit = { size -> (size.toFloat() * zoomLevel).sp }

    val filteredChapters = chapters.filter { chapter ->
        val matchesSearch = chapter.title.contains(searchQuery, ignoreCase = true) ||
                chapter.subtitle.contains(searchQuery, ignoreCase = true) ||
                chapter.sections.any {
                    it.sectionTitle.contains(searchQuery, ignoreCase = true) ||
                            it.content.contains(searchQuery, ignoreCase = true) ||
                            (it.frontEndAction != null && it.frontEndAction.contains(searchQuery, ignoreCase = true)) ||
                            (it.backEndProcess != null && it.backEndProcess.contains(searchQuery, ignoreCase = true))
                }
        matchesSearch
    }

    val allExpanded = filteredChapters.isNotEmpty() && filteredChapters.all { expandedChapters[it.id] == true }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Image(
                                painter = painterResource(id = R.drawable.zodd_logo),
                                contentDescription = "Logo zODD v2-26",
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Manuel d'Utilisation zODD V.2-26",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tunisair Technics",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Remerciements à Madame Nahla Zaouia pour sa contribution à la réflexion et sa collaboration à la mise en place de cette application.",
                                    fontSize = 10.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 13.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // BOUTON TÉLÉCHARGER LE MANUEL EN PDF
                            Button(
                                onClick = {
                                    PdfReportGenerator.downloadOrOpenUserManualPdf(context, chapters)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Télécharger PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Quick zoom indicator in header
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (zoomLevel != 1.0f) AmberGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (zoomLevel != 1.0f) AmberGold else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.clickable { zoomLevel = 1.0f }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ZoomIn,
                                        contentDescription = null,
                                        tint = if (zoomLevel != 1.0f) AmberGold else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${(zoomLevel * 100).roundToInt()}%",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (zoomLevel != 1.0f) AmberGold else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (onClose != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = onClose) {
                                    Icon(Icons.Default.Close, contentDescription = "Fermer")
                                }
                            }
                        }
                    }

                    // Search inside manual
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Rechercher dans le manuel (Figer, ODD, Frais, DAF, Distances...)...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Effacer", modifier = Modifier.size(16.dp))
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    // Zoom and Display Controls Toolbar
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Left: Zoom stepper [-] [ % ] [+]
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FormatSize,
                                        contentDescription = "Zoom",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Zoom :",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Zoom Out Button
                                    FilledTonalIconButton(
                                        onClick = {
                                            zoomLevel = ((zoomLevel - 0.15f) * 100).roundToInt() / 100f
                                            if (zoomLevel < 0.75f) zoomLevel = 0.75f
                                        },
                                        enabled = zoomLevel > 0.75f,
                                        modifier = Modifier.size(28.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ZoomOut,
                                            contentDescription = "Diminuer la taille",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Reset / Current % button
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (zoomLevel != 1.0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                        modifier = Modifier.clickable { zoomLevel = 1.0f }
                                    ) {
                                        Text(
                                            text = "${(zoomLevel * 100).roundToInt()}%",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (zoomLevel != 1.0f) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Zoom In Button
                                    FilledTonalIconButton(
                                        onClick = {
                                            zoomLevel = ((zoomLevel + 0.15f) * 100).roundToInt() / 100f
                                            if (zoomLevel > 2.0f) zoomLevel = 2.0f
                                        },
                                        enabled = zoomLevel < 2.0f,
                                        modifier = Modifier.size(28.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ZoomIn,
                                            contentDescription = "Agrandir la taille",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Right: Presets & Tools
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Preset chips
                                    listOf(1.0f to "100%", 1.25f to "125%", 1.50f to "150%", 1.75f to "175%").forEach { (presetVal, label) ->
                                        val isSelected = ((zoomLevel * 100).roundToInt() == (presetVal * 100).roundToInt())
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                            ),
                                            modifier = Modifier.clickable { zoomLevel = presetVal }
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    // Fine slider toggle
                                    IconButton(
                                        onClick = { showContinuousSlider = !showContinuousSlider },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Tune,
                                            contentDescription = "Ajusteur continu",
                                            tint = if (showContinuousSlider) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Unfold all / Fold all
                                    IconButton(
                                        onClick = {
                                            if (allExpanded) {
                                                expandedChapters.clear()
                                            } else {
                                                filteredChapters.forEach { expandedChapters[it.id] = true }
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (allExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                                            contentDescription = if (allExpanded) "Tout replier" else "Tout déplier",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Continuous slider expanded view
                            AnimatedVisibility(visible = showContinuousSlider) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("75%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Slider(
                                        value = zoomLevel,
                                        onValueChange = { zoomLevel = (it * 100).roundToInt() / 100f },
                                        valueRange = 0.75f..2.0f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("200%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Official Logo & App Banner
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AviationNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.zodd_logo),
                            contentDescription = "Logo Officiel zODD v2-26",
                            modifier = Modifier
                                .size(74.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AmberGold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = "MANUEL D'UTILISATION OFFICIEL",
                                    fontSize = zsp(9),
                                    fontWeight = FontWeight.Bold,
                                    color = AviationNavy,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "zODD V.2-26",
                                fontWeight = FontWeight.Black,
                                fontSize = zsp(18),
                                color = Color.White
                            )
                            Text(
                                text = "Tunisair Technics • Gestion des Ordres de Déplacement & Frais",
                                fontSize = zsp(11),
                                lineHeight = zsp(15),
                                color = SlateLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = AmberGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Remerciements à Madame Nahla Zaouia pour sa contribution à la réflexion et sa collaboration à la mise en place de cette application.",
                                        fontSize = zsp(10),
                                        fontStyle = FontStyle.Italic,
                                        color = Color.White.copy(alpha = 0.95f),
                                        lineHeight = zsp(14)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick summary banner
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Référence Opérationnelle Tunisair Technics",
                                fontWeight = FontWeight.Bold,
                                fontSize = zsp(13),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ce manuel décrit le cycle de vie complet d'un dossier de mission : de la constitution du Lot et la saisie des ODD, au calcul des distances et frais, jusqu'au circuit des 7 signatures de validation.",
                            fontSize = zsp(12),
                            lineHeight = zsp(17),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Chapters List
            itemsIndexed(filteredChapters, key = { _, chap -> chap.id }) { _, chapter ->
                val isExpanded = expandedChapters[chapter.id] ?: false

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedChapters[chapter.id] = !isExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = chapter.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "Chapitre ${chapter.id}",
                                                fontSize = zsp(9),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = chapter.tag,
                                            fontSize = zsp(9),
                                            fontWeight = FontWeight.SemiBold,
                                            color = AmberGold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = chapter.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = zsp(14),
                                        lineHeight = zsp(19)
                                    )
                                    Text(
                                        text = chapter.subtitle,
                                        fontSize = zsp(11),
                                        lineHeight = zsp(15),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Réduire" else "Dérouler",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Expanded Sections
                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(10.dp))

                                chapter.sections.forEachIndexed { index, section ->
                                    if (index > 0) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }

                                    Text(
                                        text = section.sectionTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = zsp(13),
                                        lineHeight = zsp(18),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = section.content,
                                        fontSize = zsp(12),
                                        lineHeight = zsp(18),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    // Commandes & Interface (Front-end)
                                    if (!section.frontEndAction.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Commandes & Interface (Front-end) :",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = zsp(11),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = section.frontEndAction,
                                                    fontSize = zsp(11),
                                                    lineHeight = zsp(16),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    // Logique Métier & Système (Back-end)
                                    if (!section.backEndProcess.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Settings, contentDescription = null, tint = SlateMedium, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Logique Métier & Système (Back-end) :",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = zsp(11),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = section.backEndProcess,
                                                    fontSize = zsp(11),
                                                    lineHeight = zsp(16),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    if (section.tips.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                section.tips.forEach { tip ->
                                                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                                        Text("💡 ", fontSize = zsp(11))
                                                        Text(tip, fontSize = zsp(11), lineHeight = zsp(16), color = MaterialTheme.colorScheme.onSurface)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (section.warning != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = CrimsonLight,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                                                Icon(Icons.Default.Warning, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = section.warning,
                                                    fontSize = zsp(11),
                                                    color = CrimsonRed,
                                                    lineHeight = zsp(16),
                                                    fontWeight = FontWeight.SemiBold
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
}

private fun getManualChapters(): List<ManualChapter> {
    return listOf(
        ManualChapter(
            id = 1,
            title = "Présentation Générale & Gouvernance zODD",
            subtitle = "Architecture, principes directeurs et cycle de vie opérationnel",
            icon = Icons.Default.Airlines,
            tag = "FONDAMENTAUX & ARCHITECTURE",
            sections = listOf(
                ManualSection(
                    sectionTitle = "1.1 Rôle & Périmètre de l'Application zODD V.2-26",
                    content = "zODD V.2-26 est le progiciel institutionnel officiel de Tunisair Technics dédié à la gestion globale, logistique et financière des Ordres de Déplacement (ODD) des techniciens de maintenance aéronautique. L'application encadre tout le cycle de mission : constitution des lots de chantiers, planification des techniciens selon leur échelle indiciaire, calcul automatique des distances et des frais selon les barèmes DAF, numérisation des pièces justificatives probantes, et sécurisation du circuit des 7 visas hiérarchiques.",
                    frontEndAction = "Navigation intuitive via barre d'onglets principale : Tableau de bord, Lots de missions, ODD d'un lot, Validation hiérarchique, Référentiels et Manuel. Recherche globale dynamique multi-critères et indicateurs visuels immédiats.",
                    backEndProcess = "Architecture Clean Architecture Android MVVM avec persistance locale SQLite Room chiffrée. Synchronisation bi-directionnelle temps réel avec Cloud Firestore. Mode 100% autonome et déconnecté (offline-first).",
                    tips = listOf(
                        "Fonctionne 100% hors-ligne grâce à la base SQLite Room embarquée.",
                        "Synchronisation bi-directionnelle Cloud Firestore disponible à tout moment via le bouton 'Cloud'."
                    )
                ),
                ManualSection(
                    sectionTitle = "1.2 Structure Hiérarchique des Données & Intégrité Référentielle",
                    content = "Le modèle de données zODD est structuré selon une arborescence stricte à 4 niveaux hiérarchiques indissociables garantissant la conformité comptable :\n" +
                            "• NIVEAU 1 : Le Lot (dossier chapeau regroupant les ODD d'une intervention ou d'une période).\n" +
                            "• NIVEAU 2 : Les Ordres de Déplacement (ODD) individuels, rattachés à un technicien avec ses dates, son site d'origine et son escale d'intervention.\n" +
                            "• NIVEAU 3 : Les Lignes de Frais (Hébergement, Transport, Divers) rattachées de manière univoque à chaque ODD.\n" +
                            "• NIVEAU 4 : Les Pièces Justificatives GED (factures scannées, billets, péages) certifiant chaque dépense.",
                    frontEndAction = "Affichage visuel imbriqué : chaque carte d'ODD possède son propre accordéon 'Afficher détails & pièces ▼' qui centralise ses frais et documents sans dispersion visuelle.",
                    backEndProcess = "Contraintes de clés étrangères Room (`lotN`, `oddN`, `matricule`). Déclencheurs de recalcul automatique en cascade des totaux financiers du lot à chaque ajout, modification ou suppression.",
                    tips = listOf("La modification d'un frais met à jour instantanément les totaux de l'ODD, du Lot et du Tableau de bord sans rechargement.")
                ),
                ManualSection(
                    sectionTitle = "1.3 Hommage & Conduite du Changement",
                    content = "La mise en place de zODD V.2-26 résulte d'une étroite synergie entre la Direction Administrative et Financière (DAF) et les directions opérationnelles de maintenance aéronautique. L'application honore la contribution déterminante de Madame Nahla Zaoui (Chargée de Gestion & Traitement des lots) dans la formalisation des règles de gestion, la conformité des barèmes et la rigueur du traitement des dossiers de mission.",
                    frontEndAction = "Mention solennelle de reconnaissance figurant en en-tête du manuel d'utilisation et sur la page de garde officielle de tous les rapports PDF générés.",
                    backEndProcess = "Traçabilité des règles métier d'audit DAF intégrées nativement dans le moteur de calcul de l'application.",
                    tips = listOf("Consultez la version PDF officielle téléchargeable via le bouton 'Télécharger PDF' en haut de cet écran.")
                )
            )
        ),
        ManualChapter(
            id = 2,
            title = "Gestion des Lots & Filtrage par Profil Connecté",
            subtitle = "Création réservée, ségrégation des dossiers et bouton d'archivage",
            icon = Icons.Default.FolderSpecial,
            tag = "LOTS & CONTRÔLE D'ACCÈS",
            sections = listOf(
                ManualSection(
                    sectionTitle = "2.1 Création et Pilotage d'un Nouveau Lot",
                    content = "Pour ouvrir une nouvelle session de mission, cliquez sur '+ Nouveau Lot' sur l'écran d'accueil. Renseignez l'identifiant du lot (ex: TAT-0023-26) et un libellé clair décrivant le chantier (ex: 'Grande visite A320 Tunis & Djerba'). Le lot est le conteneur principal de validation et de facturation consolidée.\n\n" +
                            "★ DROIT DE CRÉATION RÉSERVÉ :\n" +
                            "Le bouton flottant '+ Nouveau Lot' est réservé exclusivement au profil Direction Générale (DG) et à Madame Nahla Zaoui (Chargée de Gestion). Pour les directeurs hiérarchiques connectés (DM, DCT, DGRT, etc.), ce bouton est automatiquement masqué car leur rôle est concentré sur le visa réglementaire des dossiers.",
                    frontEndAction = "Bouton flottant '+ Nouveau Lot' conditionnel selon le profil actif. Cartes de lots avec indicateurs d'état (En cours / Figé / Archivé), compteur d'ODD, montant consolidé et actions contextuelles.",
                    backEndProcess = "Insertion dans la table `lots` avec `statut = 'EN COURS'`, `figerLot = false`, `directionActuelle = 'DM'`. Création automatique de la séquence des 7 étapes de validation dans `validations`.",
                    tips = listOf("Le système initialise automatiquement le circuit de validation à l'étape DM (Direction Maintenance).")
                ),
                ManualSection(
                    sectionTitle = "2.2 Règles d'Affichage, Clic Direct & Ségrégation par Profil Connecté",
                    content = "Afin de garantir la confidentialité et d'éviter toute distraction opérationnelle, l'affichage des lots obéit à une règle stricte de ségrégation selon l'identité connectée :\n\n" +
                            "• CLIC DIRECT POUR OUVRIR LES DÉTAILS DU LOT :\n" +
                            "   - Un clic sur n'importe quelle carte de lot ou sur son bouton dédié « Consulter les Détails (ODD, Frais, Justificatifs) ➔ » ouvre instantanément l'ensemble du dossier détaillé.\n" +
                            "   - L'écran s'ouvre sur les 4 onglets complets : 'LES ODD' (missions, barèmes et calculs), 'FRAIS' (ventilation des dépenses), 'JUSTIFICATIFS' (GED et factures scannées) et 'VALIDATION' (circuit des 7 visas).\n\n" +
                            "• Profils DG & Madame Nahla Zaoui : Visualisent l'intégralité des dossiers sans restriction (lots 'En cours' d'édition, lots 'Figés', lots 'Archivés' ou vue d'ensemble 'Tous'). Seuls ces profils disposent du droit d'ouverture et de création de nouveaux lots ('+ Nouveau Lot').\n" +
                            "• Profils des Directions Hiérarchiques (DM, DCT, DGRT, DCST, AUDIT, DAF) :\n" +
                            "   1. N'affichent JAMAIS les lots en cours d'édition (les brouillons non figés demeurent invisibles et protégés contre toute modification prématurée).\n" +
                            "   2. Affichent TOUS les lots figés avec consultation intégrale de leurs détails : liste complète des ODD (technicien, dates, sites, règle de calcul, net), détails exhaustifs de tous les frais rattachés (Hébergement, Transport, Divers, comptabilisation) et consultation de l'ensemble des pièces justificatives GED numérisées.\n" +
                            "   3. MODE LECTURE SEULE STRICTEMENT APPLIQUÉ : Pour ces directions, la consultation est en lecture seule (les boutons d'ajout, modification ou suppression d'ODD, de frais et de pièces justificatives sont systématiquement verrouillés et masqués) afin d'assurer l'inviolabilité absolue des dossiers financiers.",
                    frontEndAction = "Clic direct sur la carte du lot ou sur le bouton 'Consulter les Détails ➔' ouvrant le dossier complet. Accordéon dépliable 'Aperçu rapide sur place ▼' et bandeau explicite '🔒 Mode Lecture Seule • Direction DIR'.",
                    backEndProcess = "Filtrage combiné Room / StateFlow isolant `lot.figerLot == true` pour les directions hiérarchiques et neutralisation de toutes les actions d'écriture (`isReadOnly = true`).",
                    tips = listOf(
                        "Cliquez directement sur n'importe quel lot pour inspecter immédiatement ses décomptes et pièces jointes.",
                        "Garantit une transparence absolue tout en préservant l'intégrité probante du dossier."
                    )
                ),
                ManualSection(
                    sectionTitle = "2.3 Filtre « Archivés » Standardisé",
                    content = "Le bouton de filtre 'Archivés' est configuré dans son état standard, strictement identique aux boutons 'En cours' et 'Figés' :\n" +
                            "• Toujours actif et cliquable sans cadenas ni restriction préalable.\n" +
                            "• Dès activation, il sélectionne et affiche exclusivement les dossiers clôturés ayant achevé avec succès l'ensemble du circuit hiérarchique (`statutValidation == 'ARCHIVE'`).",
                    frontEndAction = "FilterChip 'Archivés' cliquable, adoptant la charte graphique globale Material 3.",
                    backEndProcess = "Requête de sélection filtrant sur `statutValidation == 'ARCHIVE'` ou `directionActuelle == 'ARCHIVE'`.",
                    tips = listOf("Permet de consulter à tout moment les archives comptables définitives.")
                )
            )
        ),
        ManualChapter(
            id = 3,
            title = "Ordres de Déplacement (ODD) & Distances Inter-Sites",
            subtitle = "Affectation techniciens, règle des 50 km, barèmes et avances",
            icon = Icons.Default.FlightTakeoff,
            tag = "ODD & LOGISTIQUE TERRAIN",
            sections = listOf(
                ManualSection(
                    sectionTitle = "3.1 Saisie d'un ODD & Affectation du Technicien",
                    content = "Au sein du lot, cliquez sur 'Ajouter un Ordre de Déplacement'. Sélectionnez le technicien dans le référentiel : son matricule, son échelle indiciaire (100 à 900) et sa base de provenance par défaut sont automatiquement renseignés. Choisissez ensuite le site d'intervention (ex: Monastir, Djerba, Sfax...) et les dates de mission.",
                    frontEndAction = "Formulaire modal avec liste déroulante filtrable des techniciens. Saisie intuitive des dates avec calcul instantané de la durée. Sélection du type de mission officiel.",
                    backEndProcess = "Calcul automatique du nombre de jours de mission via `SiteDistances.getMissionDurationDays(dateDebut, dateFin)`. Liaison relationnelle par matricule et lotN.",
                    tips = listOf("L'échelle indiciaire du technicien détermine automatiquement les plafonds de remboursement pour les nuitées et les taux kilométriques.")
                ),
                ManualSection(
                    sectionTitle = "3.2 Matrice Officielle des Distances & Règle des 50 km (Audit DAF)",
                    content = "La plateforme intègre la matrice officielle des distances inter-sites des aéroports et centres techniques tunisiens :\n" +
                            "• Tunis ➔ Monastir : 165 km | Tunis ➔ Sfax : 270 km | Tunis ➔ Djerba : 510 km\n" +
                            "• Tunis ➔ Tozeur : 450 km | Tunis ➔ Tabarka : 175 km | Tunis ➔ Enfidha : 100 km\n\n" +
                            "★ RÈGLE IMPÉRATIVE DE LA FRANCHISE DE 50 KM EN ALLER-RETOUR :\n" +
                            "• Distance A/R brut = Distance simple × 2.\n" +
                            "• DÉDUCTION OBLIGATOIRE : La distance Aller-Retour est obligatoirement déduite de 50 kilomètres avant toute prise en compte financière.\n" +
                            "• FORMULE DE LA DIFFÉRENCE NETTE : Distance Nette Retenue = max(0, (Distance Aller × 2) - 50 km).\n" +
                            "• Exemples certifiés : Tunis ➔ Monastir (165 km) ➔ A/R brut = 330 km ➔ -50 km ➔ 280 km retenus. Tunis ➔ Sfax (270 km) ➔ A/R brut = 540 km ➔ -50 km ➔ 490 km retenus.",
                    frontEndAction = "Simulateur kilométrique interactif dans la fiche de chaque technicien. Dans la boîte d'ajout de frais, bouton 'Appliquer X km retenus en Quantité' qui pré-remplit la quantité nette en 1 clic.",
                    backEndProcess = "Module `SiteDistances` appliquant la formule `max(0, (distance * 2) - 50)`. Toute saisie manuelle sans déduction est signalée lors des contrôles de cohérence.",
                    tips = listOf(
                        "Le simulateur de l'écran Techniciens et la boîte de dialogue d'ajout de frais appliquent cette déduction de 50 km en 1 clic.",
                        "Cette règle est gravée dans le module SiteDistances pour garantir une conformité sans faille."
                    ),
                    warning = "Règle Audit DAF : Tout calcul de transport sur base de la distance A/R brute sans déduction des 50 km de franchise est irrecevable et fera l'objet d'un refus de visa."
                ),
                ManualSection(
                    sectionTitle = "3.3 Avance sur Mission & Net à Payer",
                    content = "Chaque Ordre de Déplacement (ODD) intègre la saisie d'une 'Avance sur mission' accordée au technicien avant son départ :\n" +
                            "• Déduction systématique : L'avance est déduite du montant total des dépenses comptabilisées de la mission : Net à Payer = max(0, Total Frais Comptabilisés - Avance sur Mission).\n" +
                            "• Gestion des soldes : L'ODD affiche distinctement le montant total brut des frais, le montant de l'avance déduite et le solde net restant à payer.\n" +
                            "• Impact sur le lot : Le coût consolidé du lot répercute en temps réel cette déduction pour un pilotage budgétaire exact.",
                    frontEndAction = "Champ de saisie 'Avance sur mission (TND)' dans l'accordéon financier de l'ODD. Affichage simultané en 3 lignes : Frais bruts, Avance perçue (-), Net à payer.",
                    backEndProcess = "Calcul en temps réel `val netAPayer = max(0.0, totalFrais - avance)`. Répercussion sur le coût global consolidé du lot dans la table `lots`.",
                    tips = listOf("Si aucune avance n'a été perçue, laissez la valeur à 0.000 TND.")
                ),
                ManualSection(
                    sectionTitle = "3.4 Règle Officielle du Coût de Mission (MontantODD: Price)",
                    content = "Le coût brut de la mission dans l'ODD est régi par la formule officielle de gestion Tunisair Technics :\n" +
                            "1. Règle Hébergement (Échelle < 5) :\n" +
                            "   Si un frais d'hébergement est rattaché à l'ODD et que le premier chiffre de l'échelle indiciaire du technicien est inférieur à 5 (ex: échelle 300, 400) :\n" +
                            "   Coût = Prix_Unitaire_Mission × (1 + (max(1, DelaisJours) - 1) × 0.6)\n\n" +
                            "2. Règle 💡 ASSISTANCE TECHNIQUE :\n" +
                            "   Pour toute mission d'assistance technique, le calcul s'effectue par tranches de 8 jours :\n" +
                            "   Coût = Prix_Unitaire_Mission × ⌈max(1, DelaisJours) / 8⌉\n\n" +
                            "3. Règle 🛠️ RENFORT HANGAR (> 30 jours, Échelle 5 ou 6) :\n" +
                            "   Pour un renfort hangar supérieur à 30 jours avec une échelle dont le premier chiffre est 5 ou 6 (ex: échelle 500, 600) :\n" +
                            "   Coût = (Prix_Unitaire_Mission × 30) + ((DelaisJours - 30) × Prix_Unitaire_Mission × 2/3)\n\n" +
                            "4. Règle Standard (Par Défaut) :\n" +
                            "   Pour toutes les autres missions :\n" +
                            "   Coût = Prix_Unitaire_Mission × max(1, DelaisJours)",
                    frontEndAction = "Badge visuel dans la carte d'ODD affichant la règle activée (ex: 'Règle Assistance Tech (tranches 8j)') et décomposition du calcul financier.",
                    backEndProcess = "Méthode `SiteDistances.calculateCoutMission(typeMission, delaiJours, echelle, hasHebergement, puMission)` exécutée automatiquement dans le ViewModel.",
                    tips = listOf(
                        "Chaque ODD affiche en temps réel la règle appliquée et le détail du calcul dans son volet financier.",
                        "Le simulateur de l'écran Référentiels permet de tester immédiatement les 4 cas de figure."
                    )
                ),
                ManualSection(
                    sectionTitle = "3.5 Remise à Zéro des Charges & Actions Modifier / Supprimer",
                    content = "Dans l'onglet 'Référentiels > Techniciens', un bouton d'action dédié 'RàZ Charges' (avec icône de remise à zéro) permet de réinitialiser simultanément les compteurs de charge de travail de tous les techniciens :\n" +
                            "• Remise à zéro des compteurs opérationnels : Le nombre de missions cumulées et les jours d'intervention cumulés sont remis à 0 pour chaque agent.\n" +
                            "• Préservation intégrale des données RH : Les matricules, noms, qualifications, échelles indiciaires, RIB et sites d'attachement demeurent intacts.\n" +
                            "• Boutons Modifier et Supprimer unifiés : Chaque fiche technicien dispose de son bouton 'Modifier' et de son bouton 'Supprimer' (icône poubelle rouge sur fond teinté).",
                    frontEndAction = "Bouton d'en-tête 'RàZ Charges' avec dialogue de confirmation sécurisé. Bouton 'Modifier' et bouton 'Supprimer' (carré rouge teinté) disposés côte-à-côte.",
                    backEndProcess = "`repository.resetTechniciensWorkload()` exécutant une requête UPDATE sur `techniciens` mettant `histMissions = 0` et `histJours = 0`.",
                    tips = listOf("Idéal en début de nouvel exercice ou pour recalculer les charges sur une nouvelle période budgétaire.")
                )
            )
        ),
        ManualChapter(
            id = 4,
            title = "Comptabilisation des Frais & Pièces Justificatives (GED)",
            subtitle = "Saisie par ODD, exclusion dynamique, barèmes et numérisation",
            icon = Icons.Default.MonetizationOn,
            tag = "COMPTABILITÉ & FRAIS",
            sections = listOf(
                ManualSection(
                    sectionTitle = "4.1 Saisie Unitaire des Dépenses par ODD",
                    content = "Dans l'onglet 'LES ODD' du lot, chaque carte d'ODD comporte un volet dépliable 'Afficher détails & pièces ▼'. Vous pouvez cliquer directement sur '+ Dépense' à l'intérieur de l'ODD pour que celui-ci soit automatiquement présélectionné.",
                    frontEndAction = "Bouton '+ Dépense' intégré dans chaque ODD. Sélecteur de catégorie (Hébergement, Transport, Divers), champ quantité, prix unitaire pré-rempli selon barème et champ d'observation.",
                    backEndProcess = "Insertion dans la table `frais` avec clé étrangère `oddN`. Calcul instantané `montantTotal = quantite * prixUnitaire` et sommation dans l'ODD parent.",
                    tips = listOf("Aucun risque de rattacher une dépense au mauvais technicien ou au mauvais ODD.")
                ),
                ManualSection(
                    sectionTitle = "4.2 Comptabilisation Dynamique des Frais & Affichage Grisé",
                    content = "Chaque ligne de dépense rattachée à un ODD dispose d'une option de comptabilisation personnalisée :\n" +
                            "• Prise en compte ou exclusion : Un clic sur le bouton bascule son statut en 'Non comptabilisé' (bouton rouge / rose pâle).\n" +
                            "• Affichage du montant en grisé : Le montant du frais reste visible à l'écran mais apparaît en couleur grisée pour conserver la lisibilité de la dépense sans équivoque.\n" +
                            "• Déduction automatique des totaux : Le montant non comptabilisé est immédiatement déduit du total de l'ODD et du coût consolidé du Lot.\n" +
                            "• Recalcul instantané en temps réel : Dès qu'une dépense est modifiée, tous les indicateurs et graphiques sont mis à jour sans rechargement.",
                    frontEndAction = "Bouton bascule 'Comptabilisé / Non comptabilisé'. Lorsque désactivé, le libellé passe en couleur grisé discrète et le badge d'état devient 'Non comptabilisé'.",
                    backEndProcess = "Attribut `aComptabiliser` mis à 0.0 dans Room. La requête d'agrégation filtre sur `aComptabiliser > 0`, recalculant instantanément les totaux de lot.",
                    tips = listOf("Le montant reste parfaitement visible pour les auditeurs tout en étant exclu des décomptes financiers.")
                ),
                ManualSection(
                    sectionTitle = "4.3 Grille des Barèmes Officiels (Hébergement & Transport Personnel)",
                    content = "• HÉBERGEMENT : LPD (Logement Petit Déjeuner), Demi-Pension, Pension Complète. Le tarif unitaire par nuitée est fixé selon l'échelle indiciaire du technicien.\n" +
                            "• TRANSPORT - VÉHICULE PERSONNEL : Formule officielle appliquée :\n" +
                            "   Frais = ((Distance entre les sites × 2) - 50 km) × Taux/km\n" +
                            "   La déduction forfaitaire obligatoire de 50 km sur l'Aller-Retour brut donne la quantité nette en kilomètres, multipliée par le taux indiciaire (ex: 0.220, 0.280 ou 0.350 TND/km selon échelle).\n" +
                            "• ESCALES & TRANSPORT COMMUN : Indemnité forfaitaire d'escale (60.750 TND) ou remboursement au réel sur justificatif.\n" +
                            "• DIVERS : Péages d'autoroute (au réel sur ticket), frais d'embarquement, repas d'astreinte exceptionnelle.",
                    frontEndAction = "Boutons de sélection rapide des catégories dans la boîte de dialogue de frais. Pré-remplissage automatique des montants selon la grille indiciaire.",
                    backEndProcess = "Table Room `baremes_tarifs` synchronisée. Recalcul automatique si les barèmes sont modifiés dans l'onglet Référentiels.",
                    tips = listOf("Dans la boîte d'ajout de frais, cliquez sur 'Appliquer X km retenus en Quantité' pour renseigner automatiquement la différence nette ((Distance × 2) - 50).")
                ),
                ManualSection(
                    sectionTitle = "4.4 Justificatifs GED : Parcourir un Fichier & Numérisation Caméra",
                    content = "Chaque dépense doit obligatoirement être certifiée par une pièce probante. Dans l'onglet 'JUSTIFICATIFS' ou dans chaque carte d'ODD, cliquez sur 'Rattacher une Pièce Justificative' (accessible sur les lots en édition) :\n\n" +
                            "• ACTION 1 : « 📂 PARCOURIR » : Ouvre le sélecteur Android sécurisé pour parcourir la mémoire de l'appareil et importer directement une facture PDF, un reçu scanné ou une image existante.\n" +
                            "• ACTION 2 : « 📸 SCANNER CAMÉRA » : Déclenche instantanément l'appareil photo du smartphone ou de la tablette pour photographier et numériser sur place le justificatif physique (tickets de péage d'autoroute, factures d'hôtel, notes de restaurant, ordres de mission signés).\n" +
                            "• APERÇU MINIATURE IMMÉDIAT : La pièce numérisée est affichée sous forme de vignette miniature avec badge vert confirmant que le document est prêt pour archivage probant.",
                    frontEndAction = "Dialogue de rattachement avec boutons côte-à-côte 'Parcourir' et 'Scanner Caméra'. Affichage en direct de la miniature scannée et badge 'Pièce jointe prête'.",
                    backEndProcess = "Enregistrement dans la table `documents` avec URI de stockage interne ou cache scanné. Rapprochement comptable avec `lotN` et `oddN`.",
                    tips = listOf(
                        "Le scan direct par caméra permet aux agents en déplacement de numériser leurs reçus dès leur émission sans attendre le retour au bureau.",
                        "Tous les justificatifs scannés sont intégrés au dossier d'audit PDF téléchargeable."
                    )
                )
            )
        ),
        ManualChapter(
            id = 5,
            title = "Verrouillage du Lot (« Figer Lot ») & Mode Lecture Seule Sécurisé",
            subtitle = "Affichage intégral des ODD/frais/justificatifs et inviolabilité des données",
            icon = Icons.Default.Lock,
            tag = "CLÔTURE & CONGÉLATION",
            sections = listOf(
                ManualSection(
                    sectionTitle = "5.1 Processus de Verrouillage & Consultation Intégrale en Lecture Seule",
                    content = "Une fois tous les ODD, frais et justificatifs saisis, le gestionnaire clique sur le bouton doré 'Figer Lot 🔓'. Une boîte de dialogue de confirmation rappelle les conséquences d'audit irréversibles :\n\n" +
                            "• AFFICHAGE INTÉGRAL EN MODE LECTURE SEULE : Pour les lots figés, l'ensemble des détails des ODD (missions, calculs indiciaires, dépenses rattachées et pièces jointes) est automatiquement déployé et consultable sans aucune restriction de visibilité.\n" +
                            "• DONNÉES STRICTEMENT NON MODIFIABLES : Les données des onglets 'LES ODD', 'FRAIS' et 'JUSTIFICATIFS' sont verrouillées en lecture seule. Les boutons d'ajout, de modification ou de suppression de frais/justificatifs sont neutralisés ou masqués.\n" +
                            "• APPARITION CONDITIONNELLE DE L'ONGLET VALIDATION : L'onglet 'VALIDATION' n'est accessible que lorsque le lot est figé. L'application bascule automatiquement sur cet onglet pour amorcer la signature hiérarchique.",
                    frontEndAction = "Bandeau d'information '🔒 MODE LECTURE SEULE - LOT FIGÉ'. Cartes d'ODD déployées par défaut avec affichage détaillé des barèmes et des justificatifs rattachés.",
                    backEndProcess = "Mise à jour `lot.figerLot = true`. Verrouillage des opérations d'écriture dans le ViewModel et génération du document d'audit officiel.",
                    tips = listOf(
                        "Les signataires et auditeurs peuvent examiner l'ensemble des pièces et décomptes sans risque d'altération accidentelle.",
                        "Garantit la stricte inviolabilité des données financières vis-à-vis des auditeurs internes et externes."
                    ),
                    warning = "Attention : Un lot figé ne peut plus être modifié, garantissant l'intégrité des données financières face aux auditeurs."
                )
            )
        ),
        ManualChapter(
            id = 6,
            title = "Circuit des 7 Directions & Habilitations de Signature Hiérarchique",
            subtitle = "Dates de notification/validation et restriction d'action de viser par profil",
            icon = Icons.Default.FactCheck,
            tag = "WORKFLOW DE VALIDATION",
            sections = listOf(
                ManualSection(
                    sectionTitle = "6.1 Chronologie des 7 Visas, Dates de Notification & Dates de Validation",
                    content = "Le dossier chemine séquentiellement à travers les 7 directions officielles de Tunisair Technics selon l'organigramme institutionnel :\n\n" +
                            "1. DM — Direction de la Maintenance : Validation technique des travaux exécutés et chantiers terrain.\n" +
                            "2. DCT — Direction du Contrôle Technique : Contrôle de conformité aéronautique, agréments PART-145 et navigabilité.\n" +
                            "3. DGRT — Direction de Gestion des Ressources Techniques : Gestion et affectation prévisionnelle des compétences.\n" +
                            "4. DCST — Direction de la Coordination et du Support Technique : Coordination logistique et logistique escale.\n" +
                            "5. AUDIT — Direction de l'Audit et de la Qualité : Certification qualité, conformité des barèmes et contrôle interne.\n" +
                            "6. DG — Direction Générale : Arbitrage stratégique, visa institutionnel et gouvernance de la mission.\n" +
                            "7. DAF — Direction Administrative et Financière : Ordonnancement comptable, vérification des pièces justificatives, décompte net (déduction des avances) et mise en paiement.\n\n" +
                            "★ DOUBLE HORODATAGE PRÉCIS DANS LA TIMELINE :\n" +
                            "Sous chaque sigle de direction dans la chronologie (`DirectionTimeline`), deux informations temporelles sont explicitement affichées :\n" +
                            "• 1ère ligne : 'Notif: jj/MM' — Date et heure exactes de transmission et notification du dossier à la direction.\n" +
                            "• 2ème ligne : 'Validé: jj/MM' — Date et heure effectives de signature du visa (ou mention 'En attente' si l'étape est en cours).",
                    frontEndAction = "Composant `DirectionTimeline` avec nœuds circulaires tricolores (Vert = Validé, Ambre = Étape courante, Gris = Attente) affichant la date de notification et juste au dessous la date de validation.",
                    backEndProcess = "Calcul en temps réel des timestamps de notification (`notifDate`) et de validation (`dateValidation`) stockés dans la table `validations`.",
                    tips = listOf(
                        "Permet de mesurer précisément le délai de réactivité (en jours) de chaque direction.",
                        "Toutes les dates sont inscrites de manière infalsifiable dans l'historique d'audit."
                    )
                ),
                ManualSection(
                    sectionTitle = "6.2 Règle Stricte d'Habilitation du Visa (Sécurité & Contrôle d'Accès)",
                    content = "L'intégrité de la chaîne hiérarchique impose un contrôle d'accès strict sur l'action d'approbation :\n\n" +
                            "• HABILITATION RÉSERVÉE À LA DIRECTION ACTIVE : L'action 'Viser (DIRECTION)' n'est cliquable QUE si l'utilisateur actuellement connecté appartient à la direction en attente de visa (ex: un utilisateur connecté sous le profil DCT ne peut viser que lorsque le lot est à l'étape DCT).\n" +
                            "• BOUTON BLOQUÉ POUR LES AUTRES DIRECTIONS : Si un autre profil consulte le dossier, le bouton de visa est automatiquement désactivé (grisé) et affiche clairement la mention 'Réservé à [Direction]'.\n" +
                            "• EXCEPTION RÉGLEMENTAIRE (Mme NAHLA ZAOUI) : En sa qualité de Chargée de Gestion & Traitement des lots, Madame Nahla Zaoui conserve la main complète sur l'ensemble des boutons 'Viser' de toutes les directions pour fluidifier le traitement et débloquer les chantiers urgents.",
                    frontEndAction = "Bouton d'approbation vert émeraude 'Viser (DIR)' activé uniquement pour la direction concernée ou Mme Nahla Zaoui. Pour les autres profils, bouton grisé 'Réservé à DIR'.",
                    backEndProcess = "Vérification dans Compose `val canViser = isNahla || (activeUser?.dirCode == lot.directionActuelle)`. Empêche toute signature indue à une étape non habilitée.",
                    tips = listOf("Pour tester le visa d'une direction, connectez-vous sous le profil de cette direction ou sous le profil de Mme Nahla Zaoui.")
                ),
                ManualSection(
                    sectionTitle = "6.3 Décision de Rejet ou Archivage Définitif",
                    content = "• En cas de refus motivé : Le dossier bascule au statut 'REFUSE' avec notification obligatoire du motif, stoppant le circuit pour réexamen technique.\n" +
                            "• Après le 7ème visa (DAF) : Le dossier reçoit son identifiant permanent d'archivage (ARC-2026-XXXX) et passe au statut définitif 'ARCHIVE'.",
                    frontEndAction = "Boîte de dialogue de signature avec champ de motif obligatoire en cas de rejet. Badge d'état global du lot mis à jour (Vert = Validé, Rouge = Rejeté, Bleu = Archivé).",
                    backEndProcess = "Mise à jour du statut global dans `lots`. Génération du numéro d'archive permanent et verrouillage définitif d'archivage comptable.",
                    tips = listOf("Les dossiers archivés restent consultables à tout moment via le filtre 'Archivés'.")
                )
            )
        ),
        ManualChapter(
            id = 7,
            title = "Tableau de Bord & Pilotage Budgétaire Avancé",
            subtitle = "3 boutons radio sur une ligne, tri décroissant et podium techniciens",
            icon = Icons.Default.InsertChart,
            tag = "PILOTAGE & TABLEAU DE BORD",
            sections = listOf(
                ManualSection(
                    sectionTitle = "7.1 Répartition Budgétaire Multi-Dimensionnelle des Frais",
                    content = "Le tableau de bord propose trois analyses distinctes de ventilation des frais (Hébergement, Transport, Divers) :\n\n" +
                            "• PAR ODD (Actualisé) : Synthèse globale tricolore (Bleu Cobalt = Hébergement, Bleu Ciel = Transport, Or = Divers) complétée par une barre de recherche en temps réel et les fiches individuelles de chaque mission avec jauges proportionnelles.\n" +
                            "• PAR LOT (Nouveau) : Ventilation des dépenses pour chaque lot de mission avec statut (Ouvert / Figé avec direction active), nombre d'ODD rattachés, montant total et pourcentages par type de frais.\n" +
                            "• PAR SITE DE PROVENANCE (Nouveau) : Analyse des coûts engagés selon la base d'origine des agents (Tunis, Monastir, Sfax, Djerba, Tozeur), avec compteur des missions émises et jauges de ventilation.",
                    frontEndAction = "Sélecteur de mode d'analyse en onglets ou puces. Barre de recherche multi-critères filtrant en temps réel par matricule, nom, code ODD ou destination.",
                    backEndProcess = "Calculs d'agrégation dynamique Room et calculs vectoriels en mémoire Flow/StateFlow pour une fluidité à 60 fps sans saccade.",
                    tips = listOf(
                        "La barre de recherche permet de filtrer instantanément par code ODD, matricule, ville de provenance ou de destination.",
                        "Le volet ODD permet de déplier l'ensemble des missions ou de se focaliser sur les 4 premières."
                    )
                ),
                ManualSection(
                    sectionTitle = "7.2 Sélecteur à 3 Boutons Radio sur la Même Ligne & Tri Décroissant",
                    content = "Dans la section 'Charge de Travail par Technicien', un sélecteur moderne compact regroupe 3 critères sur une seule et même ligne horizontale :\n" +
                            "• 'Nb missions' : Classe les techniciens par nombre total de missions décroissant.\n" +
                            "• 'Coût missions' : Classe les techniciens par montant financier total décroissant.\n" +
                            "• 'Jours terrain' : Classe les techniciens par nombre de jours d'intervention cumulés décroissant.\n" +
                            "Les espacements supérieurs et inférieurs ont été réduits pour une compacité optimale sur smartphone et tablette.",
                    frontEndAction = "Rangée de 3 boutons radio cliquables avec effet ripple et retour haptique, alignés horizontalement sans saut de ligne. Bascule instantanée du tri.",
                    backEndProcess = "Tri décroissant instantané via `remember(workloadItems, workloadSortMode)` appliquant `sortedWith(compareByDescending { ... })`.",
                    tips = listOf("La sélection réorganise immédiatement l'ordre du classement et réajuste la longueur des jauges.")
                ),
                ManualSection(
                    sectionTitle = "7.3 Affichage des Cartes Techniciens : Coût en Haut & Lignes Grisettes",
                    content = "La présentation des informations à l'intérieur des cartes techniciens a été spécialement soignée selon le mode de tri sélectionné :\n" +
                            "• En mode 'Coût missions' : Le coût total de mission (`%.3f TND`) s'affiche obligatoirement sur la 1ère ligne en haut, en typographie grasse et mis en valeur. Le nombre de jours terrain (`X j terrain`) est placé au-dessous, et s'affiche en couleur grisé discrète aux côtés du nombre de missions.\n" +
                            "• Règle d'harmonisation générale : Dans TOUS les modes de sélection, la métrique principale active est mise en valeur en haut, et la 2ème et 3ème lignes sont présentées en couleur grisé (`SlateMedium` / `SlateLight`).",
                    frontEndAction = "Affichage dynamique dans la colonne de droite de chaque carte : 1ère ligne contrastée et mise en valeur, 2ème et 3ème lignes en couleur grisé pour un confort visuel optimal.",
                    backEndProcess = "Composables Jetpack Compose avec gestion conditionnelle des couleurs selon `workloadSortMode` et application de `SlateMedium` / `SlateLight`.",
                    tips = listOf("Cette hiérarchie visuelle permet d'identifier au premier coup d'œil le critère de tri tout en conservant les autres métriques lisibles.")
                ),
                ManualSection(
                    sectionTitle = "7.4 Podium Interactif Prestige Top 3 (Or, Argent, Bronze)",
                    content = "Les 3 techniciens les plus actifs bénéficient d'un traitement visuel haut de gamme digne d'un tableau d'honneur :\n" +
                            "• 🥇 1er • Leader : Médaillon doré métallisé biseauté, badge d'excellence, fond teinté d'or et jauge dorée étincelante.\n" +
                            "• 🥈 2ème • Argent : Médaillon argent platine avec liseré contrasté, fond argenté clair et jauge platine.\n" +
                            "• 🥉 3ème • Bronze : Médaillon bronze cuivré chaleureux, fond ambré et jauge cuivrée.\n" +
                            "• Du 4ème au dernier : Présentation épurée sur carte standard avec numéro de rang sobre.",
                    frontEndAction = "Médaillons et dégradés d'or, argent et bronze pour les 3 premières places. Barre de progression proportionnelle au score maximal.",
                    backEndProcess = "Calcul du ratio `fraction = (currentVal / maxVal).coerceIn(0.08f, 1f)` pour chaque carte afin d'animer la jauge.",
                    tips = listOf("La jauge de progression s'ajuste dynamiquement en proportion du score le plus élevé.")
                )
            )
        ),
        ManualChapter(
            id = 8,
            title = "Référentiels & Adaptation Dynamique aux Thèmes",
            subtitle = "Adaptation des cartes au mode d'affichage, boutons d'action unifiés et personnalisation",
            icon = Icons.Default.Palette,
            tag = "RÉFÉRENTIELS & ERGONOMIE",
            sections = listOf(
                ManualSection(
                    sectionTitle = "8.1 Adaptation des Cartes Techniciens au Mode d'Affichage Choisi",
                    content = "Dans l'onglet 'Référentiels > Techniciens', toutes les cartes de techniciens s'adaptent désormais automatiquement et fidèlement au MODE D'AFFICHAGE CHOISI (Clair, Sombre ou Auto) :\n" +
                            "• En Mode Clair : Les cartes prennent la couleur de surface claire avec typographie sombre à haut contraste, badges de base et d'échelle harmonieux, et fond d'action épuré.\n" +
                            "• En Mode Sombre : Les cartes adoptent le fond de surface sombre avec typographie claire et contrastes préservés.\n" +
                            "• Fin des styles figés : La couleur de conteneur utilise nativement `MaterialTheme.colorScheme.surface`, garantissant une cohérence graphique parfaite avec tout le reste de l'application.",
                    frontEndAction = "Bascule instantanée des cartes techniciens lorsque l'utilisateur change de mode d'affichage dans le sélecteur d'aspect. Plus aucun bloc sombre figé en mode clair.",
                    backEndProcess = "Utilisation des tokens sémantiques Material 3 `MaterialTheme.colorScheme.surface`, `onSurface`, `primaryContainer` et `outlineVariant`.",
                    tips = listOf("Testez la bascule entre Mode Clair et Mode Sombre via l'icône palette 🎨 pour apprécier l'adaptation des fiches.")
                ),
                ManualSection(
                    sectionTitle = "8.2 Actions 'Modifier' et 'Supprimer' Unifiées sur Tous les Référentiels",
                    content = "Pour garantir une ergonomie sans faille sur chaque onglet de référence (Techniciens, Directions, Barèmes de tarifs, Coûts de mission) :\n" +
                            "• Bouton Modifier : Bouton compact avec icône stylo permettant d'éditer immédiatement les données de l'entité.\n" +
                            "• Bouton Supprimer : Bouton carré rouge avec fond teinté (`CrimsonRed` transparent) et icône corbeille, toujours visible et accessible sans jamais être masqué ou repoussé hors-écran.\n" +
                            "• Agencement compact : Les badges d'échelle et de matricule sont positionnés sur la ligne de sous-titre pour laisser toute la place aux boutons d'action.",
                    frontEndAction = "Bouton Modifier et bouton Supprimer (carré rouge teinté) disposés côte-à-côte à droite de chaque carte, avec surface tactile de 48dp.",
                    backEndProcess = "Appel des méthodes ViewModel correspondantes (`saveTechnicien`, `deleteTechnicien`, `saveDirection`, `deleteDirection`...). Confirmation préalable en cas de suppression.",
                    tips = listOf("Les suppressions sont sécurisées avec demande de confirmation.")
                ),
                ManualSection(
                    sectionTitle = "8.3 Personnalisation des Couleurs & Thème Graphique",
                    content = "Personnalisez l'application à tout moment via l'icône palette 🎨 dans la barre supérieure ou dans 'Référentiels > Installation & App'.\n" +
                            "• Section 1 (Mode d'affichage) : Mode Clair, Mode Sombre ou Automatique (selon le système du smartphone).\n" +
                            "• Section 2 (Couleur du Thème) : 6 palettes d'entreprise exclusives (Bleu Aéronautique, Rouge Tunisair, Vert Émeraude, Ambre, Indigo et Gris Ardoise).\n" +
                            "• Adaptation dynamique : Les préférences visuelles sont mémorisées sur votre terminal.",
                    frontEndAction = "Boîte de dialogue modale 'Personnaliser l'Aspect & Couleurs' avec aperçu des pastilles de teintes et sélection en 1 clic.",
                    backEndProcess = "Persistance des préférences dans SharedPreferences (`pref_dark_mode`, `pref_app_palette`) et rechargement dynamique du thème via `AppThemeState`.",
                    tips = listOf("Les préférences visuelles sont mémorisées sur votre terminal.")
                ),
                ManualSection(
                    sectionTitle = "8.4 Guide d'Installation Multisupport : Android, Ordinateur (PC Windows/Mac) & Apple iOS (iPad/iPhone)",
                    content = "L'application zODD V.2-26 est conçue pour s'adapter à l'ensemble du parc informatique et nomade de Tunisair Technics grâce à ses guides intégrés accessibles dans l'onglet 'Référentiels > Installation & App' :\n\n" +
                            "• 1. TÉLÉPHONE OU TABLETTE ANDROID (PACKAGE APK NATIF) :\n" +
                            "   - Binaire autonome `app-debug.apk` (33 Mo) précompilé avec base SQLite embarquée.\n" +
                            "   - Fonctionnement 100% hors-ligne en hangar aéronautique, piste ou escale isolée.\n" +
                            "   - Installation : Transférez le fichier APK sur le smartphone (USB, Drive ou e-mail), autorisez les sources inconnues et appuyez sur 'Installer'.\n\n" +
                            "• 2. ORDINATEUR (PC WINDOWS 10/11, MAC & LINUX) :\n" +
                            "   - Option A — Application de Bureau PWA : Dans Google Chrome ou Microsoft Edge, cliquez sur 'Installer l'application zODD' dans la barre d'adresses. L'application s'installe sur le Bureau Windows/Mac et s'ouvre dans une fenêtre dédiée sans barre d'outils de navigateur.\n" +
                            "   - Option B — Émulateur Android Autonome : Glissez le fichier `app-debug.apk` dans le sous-système Android de Windows 11 (WSA) ou dans un émulateur tel que BlueStacks 5 ou Android Studio pour une exécution locale hors-ligne sur grand écran.\n\n" +
                            "• 3. TABLETTE iPAD & iPHONE (APPLE iOS) :\n" +
                            "   - Option A — Application Écran d'Accueil (Safari PWA) : Ouvrez zODD dans Safari sur votre iPad ou iPhone, appuyez sur l'icône de partage iOS (carré avec flèche vers le haut) et sélectionnez 'Sur l'écran d'accueil'. L'application zODD s'ouvre alors en plein écran tactile natif sans aucune interface de navigateur.\n" +
                            "   - Option B — Déploiement Binaire d'Entreprise (.IPA) : Téléchargez les sources (Download ZIP) et signez le projet dans Xcode sous macOS avec votre compte Apple Developer Enterprise pour un déploiement MDM (Intune, MobileIron).",
                    frontEndAction = "Onglet 'Référentiels > Installation & App' et boîte de dialogue modale `InstallAppModalDialog` avec sélecteur 3 voies (Android, PC/Mac, iPad/iOS) et boutons de copie dans le presse-papiers.",
                    backEndProcess = "Composant `InstallAppModalDialog` avec gestion du presse-papiers Android (`ClipboardManager`), gestion multiplateforme PWA et support des bannières d'installation hors-ligne.",
                    tips = listOf(
                        "Sur PC / Mac, l'utilisation sur grand écran avec souris et pavé numérique physique accélère le contrôle comptable et l'audit des lots.",
                        "Sur iPad, l'accès 'Sur l'écran d'accueil' permet une utilisation tactile ergonomique en réunion de direction ou lors des visites de maintenance."
                    )
                )
            )
        ),
        ManualChapter(
            id = 9,
            title = "Synchronisation Cloud Firestore & Exports PDF",
            subtitle = "Sauvegarde décentralisée, intégrité hors-ligne et téléchargement PDF du manuel",
            icon = Icons.Default.CloudSync,
            tag = "CLOUD & EXPORTS PDF",
            sections = listOf(
                ManualSection(
                    sectionTitle = "9.1 Synchronisation Bi-Directionnelle Cloud Firestore & Mode Hors-Ligne",
                    content = "zODD V.2-26 intègre une passerelle de synchronisation avec Google Firebase Firestore :\n" +
                            "• Fonctionnement autonome hors-ligne : Vous pouvez travailler en zone blanche (hangar isolé, piste d'atterrissage, escale étrangère sans réseau). Toutes les saisies sont sauvegardées localement dans SQLite Room.\n" +
                            "• Bouton Cloud de synchronisation : Dès qu'une connexion réseau est rétablie, cliquez sur le bouton 'Cloud' dans la barre supérieure pour synchroniser de manière atomique les lots, ODD, frais, justificatifs, validations et référentiels.\n" +
                            "• Intégrité des données : Le système préserve l'antériorité des dossiers figés et horodate chaque échange.",
                    frontEndAction = "Bouton 'Cloud' avec indicateur circulaire de synchronisation en cours, Toast informatif et message de statut sous la barre supérieure.",
                    backEndProcess = "`FirestoreSyncManager` exécutant des batchs d'écriture sur les collections Firestore `lots`, `odds`, `frais`, `validations`, `techniciens`, `directions`, `baremes`, `couts_mission`.",
                    tips = listOf("En cas d'absence de réseau, le mode hors-ligne protège l'intégralité de vos saisies en local.")
                ),
                ManualSection(
                    sectionTitle = "9.2 Téléchargement & Génération du Manuel Officiel en Format PDF",
                    content = "Le présent manuel d'utilisation complet peut être téléchargé à tout moment sous la forme d'un document PDF officiel de haute qualité :\n" +
                            "• Document PDF multipages vectoriel : Mise en page A4 soignée, en-tête officiel Tunisair Technics, métadonnées d'édition, dédicace officielle à Mme Nahla Zaouia et sommaire exécutif.\n" +
                            "• Fiches détaillées complètes : L'intégralité des 9 chapitres est documentée avec le détail des commandes Front-end, de la logique métier Back-end, des barèmes et des avertissements d'audit.\n" +
                            "• Téléchargement et partage immédiats : Le fichier est généré dans le cache de l'application et mis à disposition via Android FileProvider pour ouverture immédiate dans votre lecteur PDF préféré (Acrobat, Drive...), enregistrement dans les Téléchargements ou partage par e-mail.",
                    frontEndAction = "Bouton 'Télécharger PDF' avec icône rouge dans la barre supérieure du Manuel d'Utilisation. Clic unique ouvrant le sélecteur Android d'ouverture ou de sauvegarde du document.",
                    backEndProcess = "`PdfReportGenerator.downloadOrOpenUserManualPdf(context, chapters)` générant le document via `android.graphics.pdf.PdfDocument`, calcul automatique des sauts de page A4 et intention `Intent.ACTION_VIEW` sécurisée par `FileProvider`.",
                    tips = listOf(
                        "Cliquez sur le bouton 'Télécharger PDF' en haut de cet écran pour conserver ce guide de référence sur votre ordinateur ou smartphone.",
                        "Le document PDF peut être imprimé ou joint aux dossiers d'audit pour attester de la conformité des procédures."
                    )
                )
            )
        ),
        ManualChapter(
            id = 10,
            title = "Portail d'Accès Sécurisé, Authentification & Rôles",
            subtitle = "Page d'accès initiale, connexion e-mail/mot de passe, Google et profils officiels",
            icon = Icons.Default.Security,
            tag = "SÉCURITÉ & AUTHENTIFICATION",
            sections = listOf(
                ManualSection(
                    sectionTitle = "10.1 Page d'Accès Initiale & Double Mode d'Authentification",
                    content = "La sécurité des données et la traçabilité des engagements financiers reposent sur une page d'accès obligatoire (`LoginScreen`) qui s'affiche systématiquement en premier à chaque ouverture ou réouverture de l'application :\n\n" +
                            "• ACCÈS PAR E-MAIL & MOT DE PASSE :\n" +
                            "   - Champ 'Adresse e-mail' avec validation syntaxique, clavier e-mail et icône dédiée.\n" +
                            "   - Champ 'Mot de passe' avec bouton interactif (icône œil) permettant d'afficher ou masquer les caractères saisis en toute sécurité.\n" +
                            "   - Bouton d'action 'Se connecter' pour une vérification immédiate des identifiants et l'ouverture de la session.\n\n" +
                            "• BOUTON OFFICIEL « CONNECTER AVEC GOOGLE » :\n" +
                            "   - Positionné juste au-dessous du formulaire e-mail/mot de passe.\n" +
                            "   - Permet une authentification biométrique ou fédérée instantanée en 1 clic sans ressaisie de mot de passe.",
                    frontEndAction = "Écran d'accueil d'authentification avec fond dégradé Aviation Navy, carte centrale épurée, formulaire e-mail/mot de passe et bouton 'Connecter avec google'.",
                    backEndProcess = "Contrôles dans `AuthManager.signInWithEmailPassword(email, password)` avec persistance de la session active dans `StateFlow<UserProfile?>`.",
                    tips = listOf(
                        "En cas d'erreur de saisie, un message d'alerte explicite s'affiche immédiatement au-dessus du bouton de connexion.",
                        "L'icône œil permet de vérifier facilement votre mot de passe avant validation."
                    )
                ),
                ManualSection(
                    sectionTitle = "10.2 Profils d'Accès Rapides & Déconnexion depuis le Menu",
                    content = "Pour faciliter les transitions et la simulation des visas sur le terrain, la page d'accès propose également la liste des profils hiérarchiques officiels de Tunisair Technics :\n\n" +
                            "• Madame Nahla Zaoui — Chargée de Gestion & Traitement des Lots (DAF) avec droits de supervision globale et visa étendu.\n" +
                            "• Directeurs des 7 entités : DM (Maintenance), DCT (Contrôle Technique), DGRT (Ressources Techniques), DCST (Support & Coordination), AUDIT (Audit & Qualité), DG (Direction Générale), DAF (Administration & Finances).\n\n" +
                            "★ DÉCONNEXION AUTOMATIQUE DEPUIS L'APPLICATION :\n" +
                            "Dans la barre supérieure de l'application, un clic sur le menu 'Profil & Contrôle d'Accès' ouvre la liste des profils. Dès la sélection d'un profil, l'application déconnecte immédiatement la session en cours et renvoie instantanément sur la page d'accès (`LoginScreen`) pour confirmation et nouvelle authentification.",
                    frontEndAction = "Liste des profils d'accès rapide avec badges de direction et rôle explicite. Boîte de dialogue 'Profil & Contrôle d'Accès' avec déconnexion instantanée.",
                    backEndProcess = "`AuthManager.signOut()` réinitialisant `_currentUser.value = null`, provoquant le basculement Compose immédiat vers `LoginScreen`.",
                    tips = listOf("Idéal pour passer rapidement du rôle de gestionnaire à celui de directeur de maintenance pour apposer un visa.")
                )
            )
        )
    )
}
