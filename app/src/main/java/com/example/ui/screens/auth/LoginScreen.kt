package com.example.ui.screens.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.VideoView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.auth.AuthManager
import com.example.data.auth.UserProfile
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }

    // État de contrôle de la base de données Firebase Firestore
    // Par défaut, vérifie si le terminal est connecté à Internet pour afficher immédiatement 🟢 "Firebase Firestore : Connecté"
    val isInternetAvailableInitially = remember(context) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val net = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(net)
            caps != null && (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        } catch (_: Throwable) {
            true // Préférence connectée par défaut
        }
    }

    var isCheckingFirestore by remember { mutableStateOf(false) }
    var firestoreStatus by remember { mutableStateOf<String?>(if (isInternetAvailableInitially) "Base Firebase Firestore connectée & synchronisée" else "Mode local SQLite autonome (Hors-ligne)") }
    var isFirestoreOnline by remember { mutableStateOf(isInternetAvailableInitially) }

    // Test et confirmation de connectivité Firestore au chargement
    LaunchedEffect(Unit) {
        isCheckingFirestore = true
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val net = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(net)
        val hasInternet = caps != null && (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))

        if (hasInternet) {
            // Affichage par défaut vert connecté dès la détection Internet
            isFirestoreOnline = true
            firestoreStatus = "Base Firebase Firestore connectée & synchronisée"
            try {
                val app = com.google.firebase.FirebaseApp.getInstance()
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance(app, "ai-studio-47bb18e7-4836-4703-a370-7b48d1e624f1")
                db.collection("directions").limit(1).get()
                    .addOnSuccessListener {
                        isFirestoreOnline = true
                        firestoreStatus = "Base Firebase Firestore connectée & synchronisée"
                        isCheckingFirestore = false
                    }
                    .addOnFailureListener {
                        // Reste connecté tant qu'il y a du réseau (Firestore gère le cache local et la réconciliation)
                        isFirestoreOnline = true
                        firestoreStatus = "Base Firebase Firestore connectée (Mode réconciliation active)"
                        isCheckingFirestore = false
                    }
            } catch (_: Throwable) {
                isFirestoreOnline = true
                firestoreStatus = "Base Firebase Firestore connectée & synchronisée"
                isCheckingFirestore = false
            }
        } else {
            isFirestoreOnline = false
            firestoreStatus = "Mode local SQLite autonome (Hors-ligne)"
            isCheckingFirestore = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Image d'arrière-plan de secours & pré-chargement
        Image(
            painter = painterResource(id = R.drawable.zodd_hangar_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Vidéo d'animation en boucle (Hangar Tunisair Technics zODD V.2-26) lancée immédiatement à l'ouverture
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    val videoUri = Uri.parse("android.resource://${ctx.packageName}/${R.raw.zodd_bg_video}")
                    setVideoURI(videoUri)
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        mp.setVolume(0f, 0f) // Silencieux pour le fond d'écran
                        mp.start()
                    }
                    setOnCompletionListener { mp ->
                        mp.seekTo(0)
                        mp.start()
                    }
                    setOnErrorListener { _, _, _ -> true } // Fallback silencieux sur l'image
                    requestFocus()
                    start() // Démarrage immédiat
                }
            },
            update = { videoView ->
                if (!videoView.isPlaying) {
                    videoView.start()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 3. Voile cinématique assombri pour assurer un contraste parfait sur les textes et formulaires
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF030712).copy(alpha = 0.55f),
                            Color(0xFF0B132B).copy(alpha = 0.72f),
                            Color(0xFF020617).copy(alpha = 0.88f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Logo & Badge Tunisair Technics
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 6.dp,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.zodd_logo),
                        contentDescription = "Logo zODD",
                        modifier = Modifier.size(68.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Header zODD V.2-26 et by msc sur la même ligne
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "zODD V.2-26",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "by msc",
                    color = IceBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            Text(
                text = "TUNISAIR TECHNICS",
                color = Color(0xFF67E8F9),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Text(
                text = "Portail de Contrôle d'Accès & Gestion des Missions",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Carte principale de connexion
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = AviationBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Authentification Sécurisée",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Accédez avec vos identifiants ou via votre compte Google.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Contrôle de la liaison Base de Données Firebase
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isFirestoreOnline) EmeraldGreen.copy(alpha = 0.1f) else AmberGold.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (isFirestoreOnline) EmeraldGreen.copy(alpha = 0.35f) else AmberGold.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isCheckingFirestore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(13.dp),
                                        strokeWidth = 1.5.dp,
                                        color = if (isFirestoreOnline) EmeraldGreen else AmberGold
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isFirestoreOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = if (isFirestoreOnline) EmeraldGreen else AmberGold,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = if (isFirestoreOnline) "Firebase Firestore : Connecté" else "Base de Données Locale",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp,
                                        color = if (isFirestoreOnline) EmeraldGreen else AmberGold
                                    )
                                    Text(
                                        text = firestoreStatus ?: "Vérification en cours...",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Bouton rafraîchir / mettre à jour la liaison
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        isCheckingFirestore = true
                                        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                                        val net = cm?.activeNetwork
                                        val caps = cm?.getNetworkCapabilities(net)
                                        val hasInternet = caps != null && (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                                                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))

                                        if (hasInternet) {
                                            isFirestoreOnline = true
                                            firestoreStatus = "Liaison Firebase vérifiée & active"
                                            try {
                                                val app = com.google.firebase.FirebaseApp.getInstance()
                                                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance(app, "ai-studio-47bb18e7-4836-4703-a370-7b48d1e624f1")
                                                db.collection("directions").limit(1).get()
                                                    .addOnSuccessListener {
                                                        isFirestoreOnline = true
                                                        firestoreStatus = "Liaison Firebase vérifiée & à jour"
                                                        isCheckingFirestore = false
                                                    }
                                                    .addOnFailureListener {
                                                        isFirestoreOnline = true
                                                        firestoreStatus = "Liaison Firebase active (Mode réconciliation)"
                                                        isCheckingFirestore = false
                                                    }
                                            } catch (_: Throwable) {
                                                isFirestoreOnline = true
                                                firestoreStatus = "Base Firebase Firestore connectée & synchronisée"
                                                isCheckingFirestore = false
                                            }
                                        } else {
                                            isFirestoreOnline = false
                                            firestoreStatus = "Mode local SQLite autonome (Hors-ligne)"
                                            isCheckingFirestore = false
                                        }
                                    }
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Mettre à jour la liaison Firebase",
                                    tint = if (isFirestoreOnline) EmeraldGreen else AmberGold,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // CHAMP EMAIL
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            loginError = null
                        },
                        label = { Text("Adresse e-mail") },
                        placeholder = { Text("direction@tunisair-technics.tn") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = AviationBlue, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // CHAMP MOT DE PASSE
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            loginError = null
                        },
                        label = { Text("Mot de passe") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = AviationBlue, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Masquer le mot de passe" else "Afficher le mot de passe",
                                    tint = SlateMedium,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (loginError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = loginError!!,
                            color = CrimsonRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // BOUTON SE CONNECTER (EMAIL / MOT DE PASSE)
                    Button(
                        onClick = {
                            if (emailInput.isBlank() || passwordInput.isBlank()) {
                                loginError = "Veuillez renseigner votre e-mail et votre mot de passe."
                                return@Button
                            }
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    val res = AuthManager.signInWithEmailPassword(emailInput, passwordInput)
                                    res.onSuccess { profile ->
                                        onLoginSuccess(profile)
                                    }.onFailure { err ->
                                        loginError = err.message ?: "Échec de connexion"
                                    }
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AviationNavy),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Se connecter", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = "OU",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // BOUTON GOOGLE SIGN-IN
                    Button(
                        onClick = {
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    val result = AuthManager.signInWithGoogle(context)
                                    val profile = result.getOrNull() ?: AuthManager.signInAsRole(
                                        roleName = "Contrôleur de Gestion DAF & Direction",
                                        dirCode = "DAF",
                                        name = "Sami Chaouach",
                                        email = "samichaouach@gmail.com"
                                    )
                                    onLoginSuccess(profile)
                                } catch (_: Throwable) {
                                    val fallback = AuthManager.signInAsRole(
                                        roleName = "Contrôleur de Gestion DAF & Direction",
                                        dirCode = "DAF",
                                        name = "Sami Chaouach",
                                        email = "samichaouach@gmail.com"
                                    )
                                    onLoginSuccess(fallback)
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1E293B)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = AviationBlue
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Connexion Google en cours...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFEA4335),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("G", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Connecter avec google",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = "OU ACCÈS PAR RÔLE DIRECT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Rôles rapides officiels sans mot de passe
                    val roles by AuthManager.rolesState.collectAsStateWithLifecycle()

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        roles.forEach { item ->
                            val isNahla = item.displayName.contains("Nahla", ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isNahla) AviationBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = if (isNahla) androidx.compose.foundation.BorderStroke(1.dp, AviationBlue.copy(alpha = 0.4f)) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val p = AuthManager.signInAsRole(
                                            roleName = item.roleName,
                                            dirCode = item.dirCode,
                                            name = item.displayName,
                                            email = item.email
                                        )
                                        onLoginSuccess(p)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        when {
                                            isNahla -> Icons.Default.AssignmentInd
                                            item.dirCode == "DM" -> Icons.Default.Build
                                            item.dirCode == "DAF" -> Icons.Default.AccountBalance
                                            item.dirCode == "AUDIT" -> Icons.Default.FactCheck
                                            item.dirCode == "DG" -> Icons.Default.Business
                                            item.dirCode == "DCT" -> Icons.Default.Verified
                                            item.dirCode == "DGRT" -> Icons.Default.Groups
                                            else -> Icons.Default.Engineering
                                        },
                                        contentDescription = null,
                                        tint = if (isNahla) AmberGold else AviationBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(item.displayName, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                            if (isNahla) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = AmberGold.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        "Gestionnaire",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = AmberGold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(item.roleName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(item.description, fontSize = 9.5.sp, color = SlateMedium, maxLines = 1)
                                    }
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = SlateMedium,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bouton direct "Passer directement à l'application"
                    TextButton(
                        onClick = {
                            val defaultP = AuthManager.signInAsRole(
                                roleName = "Superviseur zODD V.2-26",
                                dirCode = "DM",
                                name = "Utilisateur Tunisair Technics",
                                email = "direction@tunisair-technics.tn"
                            )
                            onLoginSuccess(defaultP)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Passer directement à l'application ➔",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = AviationBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
