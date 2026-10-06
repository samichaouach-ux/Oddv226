package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun InstallAppModalDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedPlatform by remember { mutableStateOf("Android") } // "Android", "PC", "iOS"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.InstallMobile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Installer l'Application",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Téléphone • Ordinateur • iPad / iPhone",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3-Way Platform Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalButton(
                        onClick = { selectedPlatform = "Android" },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (selectedPlatform == "Android") EmeraldGreen else Color.Transparent,
                            contentColor = if (selectedPlatform == "Android") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Android", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = { selectedPlatform = "PC" },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (selectedPlatform == "PC") AmberGold else Color.Transparent,
                            contentColor = if (selectedPlatform == "PC") Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Computer, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PC / Mac", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = { selectedPlatform = "iOS" },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (selectedPlatform == "iOS") AviationBlue else Color.Transparent,
                            contentColor = if (selectedPlatform == "iOS") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PhoneIphone, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("iPad / iOS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedPlatform) {
                        "Android" -> {
                            // ANDROID PHONE INSTRUCTIONS
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EmeraldGreen.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Fichier Binaire Android (APK) Disponible",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = EmeraldGreen
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Le fichier binaire 'app-debug.apk' (33 Mo) est compilé et prêt à l'emploi. Il fonctionne 100% hors-ligne sur tout smartphone ou tablette Android.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Comment télécharger & installer sur votre téléphone Android :",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "1. Dans le menu de configuration d'AI Studio (en haut à droite de votre écran), cliquez sur les options / paramètres puis choisissez 'Export' ou 'Download APK' (ou 'Download ZIP').\n" +
                                       "2. Vous récupérez le fichier binaire 'app-debug.apk' sur votre machine.\n" +
                                       "3. Envoyez le fichier sur votre téléphone (par WhatsApp, Google Drive, e-mail ou câble USB).\n" +
                                       "4. Sur votre smartphone Android, touchez le fichier .apk téléchargé.\n" +
                                       "5. Si demandé, autorisez l'installation d'applications de sources inconnues (Sécurité > Sources inconnues).\n" +
                                       "6. Appuyez sur 'Installer' : l'application zODD est installée sur votre écran d'accueil !",
                                fontSize = 11.sp,
                                lineHeight = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val instructions = "Guide d'installation zODD Android :\n" +
                                            "1. Téléchargez le fichier APK depuis le menu Export d'AI Studio (app-debug.apk).\n" +
                                            "2. Transférez le fichier sur votre smartphone Android.\n" +
                                            "3. Ouvrez le fichier et cliquez sur 'Installer'."
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Instructions Android zODD", instructions))
                                    Toast.makeText(context, "Instructions copiées dans le presse-papiers !", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copier le Guide d'Installation Android", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        "PC" -> {
                            // COMPUTER (PC / MAC) INSTRUCTIONS
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AmberGold.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Computer, contentDescription = null, tint = AmberGold, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Installation & Utilisation sur Ordinateur (PC Windows / Mac / Linux)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Utilisez zODD avec un confort maximal : affichage plein écran, clavier physique, souris, double écran et raccourcis.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Méthodes recommandées pour PC Windows & Mac :",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• Méthode 1 — PWA / Application de Bureau (Chrome, Edge ou Safari) :\n" +
                                       "  1. Ouvrez l'application zODD dans Google Chrome ou Microsoft Edge sur votre ordinateur.\n" +
                                       "  2. Dans la barre d'adresse du navigateur, cliquez sur l'icône 'Installer zODD' (ou Menu ⋮ > 'Enregistrer et partager' > 'Installer cette page sous forme d'application').\n" +
                                       "  3. L'application s'ouvre dans une fenêtre autonome de bureau sans barre de navigateur, avec raccourci sur le Bureau et la barre des tâches.\n\n" +
                                       "• Méthode 2 — Émulateur Android Natif (Exécution 100% Hors-Ligne) :\n" +
                                       "  1. Récupérez le fichier 'app-debug.apk' généré (via GitHub Actions ou export AI Studio).\n" +
                                       "  2. Sur Windows 11 : Utilisez le sous-système Windows pour Android (WSA) pour une intégration native transparente.\n" +
                                       "  3. Sur PC / Mac : Installez un émulateur performant (BlueStacks 5, NoxPlayer, LDPlayer ou Android Studio).\n" +
                                       "  4. Glissez-déposez simplement 'app-debug.apk' dans l'émulateur : zODD se lance immédiatement avec persistance locale intégrale SQLite.\n\n" +
                                       "• Méthode 3 — Export du Code Source pour Développeurs :\n" +
                                       "  Ouvrez le projet complet dans Android Studio pour Windows/Mac/Linux et lancez-le en mode émulateur haute définition.",
                                fontSize = 11.sp,
                                lineHeight = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val instructions = "Guide d'installation zODD sur Ordinateur (PC/Mac) :\n" +
                                            "1. Méthode Bureau : Dans Chrome/Edge, cliquez sur 'Installer l'application' dans la barre d'adresse pour un raccourci direct Bureau.\n" +
                                            "2. Méthode APK Hors-Ligne : Glissez 'app-debug.apk' dans BlueStacks, WSA (Windows 11) ou Android Studio."
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Instructions PC zODD", instructions))
                                    Toast.makeText(context, "Guide Ordinateur copié dans le presse-papiers !", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copier le Guide Ordinateur (PC / Mac)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                            }
                        }

                        "iOS" -> {
                            // IPAD & IPHONE (IOS) INSTRUCTIONS
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AviationBlue.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, AviationBlue.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PhoneIphone, contentDescription = null, tint = AviationBlue, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Installation & Utilisation sur iPad & iPhone (Apple iOS)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = AviationBlue
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Déploiement optimisé pour l'écran tactile des tablettes iPad d'atelier/hangar et smartphones iPhone.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Deux solutions pour installer sur iPad et iPhone :",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• Solution 1 — Installation Progressive (Web App PWA / Plein Écran) :\n" +
                                       "  1. Ouvrez l'URL de l'application zODD dans Safari sur votre iPad ou iPhone.\n" +
                                       "  2. Touchez le bouton 'Partager' d'iOS (l'icône de carré avec flèche vers le haut en bas sur iPhone, ou en haut à droite sur iPad).\n" +
                                       "  3. Faites défiler vers le bas et sélectionnez 'Sur l'écran d'accueil' (Add to Home Screen).\n" +
                                       "  4. Validez le nom 'zODD V.2-26' et appuyez sur 'Ajouter'.\n" +
                                       "  5. L'icône officielle zODD apparaît sur l'écran d'accueil : elle s'exécute en plein écran autonome, sans barre d'adresse ni navigation Safari, offrant une expérience application native complète.\n\n" +
                                       "• Solution 2 — Binaire d'Entreprise iOS (.IPA / Xcode) :\n" +
                                       "  Pour les services informatiques IT de Tunisair souhaitant un déploiement MDM/Intune ou Apple Enterprise :\n" +
                                       "  1. Exportez le code source du projet (menu Export > Download ZIP).\n" +
                                       "  2. Ouvrez le projet dans Xcode sur macOS.\n" +
                                       "  3. Signez le build avec le profil d'approvisionnement Apple Developer de Tunisair Technics pour distribuer le binaire .ipa.",
                                fontSize = 11.sp,
                                lineHeight = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val instructions = "Guide d'installation zODD iPad & iPhone (iOS) :\n" +
                                            "1. Ouvrez zODD dans Safari sur iPad/iPhone.\n" +
                                            "2. Touchez Partager (icône flèche vers le haut).\n" +
                                            "3. Choisissez 'Sur l'écran d'accueil' puis 'Ajouter'.\n" +
                                            "4. L'application fonctionne en plein écran autonome comme une application native Apple."
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Instructions iPad zODD", instructions))
                                    Toast.makeText(context, "Instructions iPad / iOS copiées dans le presse-papiers !", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AviationBlue),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copier le Guide iPad / iPhone (iOS)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Fermer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
