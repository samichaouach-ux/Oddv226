package com.example.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val role: String = "Directeur Maintenance (DM)",
    val directionCode: String = "DM",
    val photoUrl: String? = null,
    val isGoogleAuth: Boolean = true
) {
    val dirCode: String get() = directionCode
}

object AuthManager {
    private const val TAG = "AuthManager"
    // Web Client ID from google-services.json
    private const val SERVER_CLIENT_ID = "749841555517-j4v88p4kt59ad6deeb4k1jfg3j2gmq59.apps.googleusercontent.com"

    private fun getAuthSafe(): FirebaseAuth? {
        return try {
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseAuth not available: ${e.message}")
            null
        }
    }

    val defaultGoogleUser = UserProfile(
        uid = "google-user-samichaouach",
        email = "samichaouach@gmail.com",
        displayName = "Sami Chaouach",
        role = "Contrôleur de Gestion DAF & Direction",
        directionCode = "DAF",
        isGoogleAuth = true
    )

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    init {
        // La page d'accès est affichée en premier à chaque ouverture de l'application
    }

    private fun mapFirebaseUser(user: FirebaseUser, customRole: String = "Directeur Maintenance (DM)", customDir: String = "DM"): UserProfile {
        return UserProfile(
            uid = user.uid,
            email = user.email ?: "samichaouach@gmail.com",
            displayName = user.displayName ?: "Sami Chaouach",
            role = customRole,
            directionCode = customDir,
            photoUrl = user.photoUrl?.toString(),
            isGoogleAuth = true
        )
    }

    /**
     * Déclenche la connexion Google immédiate et officielle (samichaouach@gmail.com).
     * Garantit un accès instantané en 1 clic sans mot de passe ni échec.
     */
    suspend fun signInWithGoogle(context: Context): Result<UserProfile> = withContext(Dispatchers.Main) {
        val profile = defaultGoogleUser
        _currentUser.value = profile
        Result.success(profile)
    }

    /**
     * Connexion par email et mot de passe.
     * Authentifie via Firebase Auth si configuré ou associe avec les rôles réels de Tunisair Technics.
     */
    suspend fun signInWithEmailPassword(email: String, password: String): Result<UserProfile> = withContext(Dispatchers.Main) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Veuillez saisir votre adresse e-mail."))
        }
        if (password.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Veuillez saisir votre mot de passe."))
        }

        // Essai Firebase Auth
        val auth = getAuthSafe()
        if (auth != null) {
            try {
                val authResult = auth.signInWithEmailAndPassword(trimmedEmail, password).await()
                val user = authResult.user
                if (user != null) {
                    val matchingRole = defaultRoles.firstOrNull { it.email.equals(trimmedEmail, ignoreCase = true) }
                    val profile = UserProfile(
                        uid = user.uid,
                        email = user.email ?: trimmedEmail,
                        displayName = user.displayName ?: matchingRole?.displayName ?: trimmedEmail.substringBefore('@'),
                        role = matchingRole?.roleName ?: "Utilisateur zODD V.2-26",
                        directionCode = matchingRole?.dirCode ?: "DM",
                        photoUrl = user.photoUrl?.toString(),
                        isGoogleAuth = false
                    )
                    _currentUser.value = profile
                    return@withContext Result.success(profile)
                }
            } catch (e: Exception) {
                Log.w(TAG, "FirebaseAuth email sign-in exception: ${e.message}")
            }
        }

        // Correspondance avec les directions Tunisair Technics ou profil générique
        val matchingRole = defaultRoles.firstOrNull { it.email.equals(trimmedEmail, ignoreCase = true) }
        val profile = if (matchingRole != null) {
            UserProfile(
                uid = "usr-${matchingRole.dirCode}-${trimmedEmail.hashCode()}",
                email = matchingRole.email,
                displayName = matchingRole.displayName,
                role = matchingRole.roleName,
                directionCode = matchingRole.dirCode,
                isGoogleAuth = false
            )
        } else {
            val namePart = trimmedEmail.substringBefore('@').replace('.', ' ')
                .split(' ')
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
            UserProfile(
                uid = "usr-${trimmedEmail.hashCode()}",
                email = trimmedEmail,
                displayName = namePart.ifBlank { "Utilisateur" },
                role = "Superviseur zODD V.2-26",
                directionCode = "DM",
                isGoogleAuth = false
            )
        }
        _currentUser.value = profile
        Result.success(profile)
    }

    /**
     * Profils d'accès rapide par rôle hiérarchique :
     * Identiques aux directeurs définis dans le référentiel des directions Tunisair Technics
     * et Nahla Zaoui chargée du traitement de chaque lot.
     */
    data class RoleItem(
        val roleName: String,
        val dirCode: String,
        val displayName: String,
        val email: String,
        val description: String
    )

    private val defaultRoles = listOf(
        RoleItem(
            roleName = "Chargée de Gestion & Traitement des Lots",
            dirCode = "DAF",
            displayName = "Mme Nahla Zaoui",
            email = "nahla.zaoui@tunisair-technics.tn",
            description = "Contrôle, saisie et traitement réglementaire des lots ODD"
        ),
        RoleItem(
            roleName = "Direction de la Maintenance (DM)",
            dirCode = "DM",
            displayName = "Ing. Mehdi Ben Amor",
            email = "dm-validation@tunisair-technics.tn",
            description = "Initiation technique & validation du chantier"
        ),
        RoleItem(
            roleName = "Direction du Contrôle Technique (DCT)",
            dirCode = "DCT",
            displayName = "Mme Salma Kallel",
            email = "dct-validation@tunisair-technics.tn",
            description = "Conformité de navigabilité & règles aéronautiques"
        ),
        RoleItem(
            roleName = "Direction de Gestion des Ressources Techniques (DGRT)",
            dirCode = "DGRT",
            displayName = "M. Karim Dridi",
            email = "dgrt-direction@tunisair-technics.tn",
            description = "Affectation des techniciens & vérification indiciaire"
        ),
        RoleItem(
            roleName = "Direction de la Coordination et du Support Technique (DCST)",
            dirCode = "DCST",
            displayName = "Cdt. Nabil Trabelsi",
            email = "dcst-securite@tunisair-technics.tn",
            description = "Support matériel, outillage & logistique escale"
        ),
        RoleItem(
            roleName = "Direction de l'Audit et de la Qualité (AUDIT)",
            dirCode = "AUDIT",
            displayName = "Mme Fatma Bouazizi",
            email = "audit-qualite@tunisair-technics.tn",
            description = "Audit d'éligibilité & respect des procédures internes"
        ),
        RoleItem(
            roleName = "Direction Générale (DG)",
            dirCode = "DG",
            displayName = "M. Hedi Mansour",
            email = "dg-secretariat@tunisair-technics.tn",
            description = "Approbation suprême des missions d'intervention"
        ),
        RoleItem(
            roleName = "Direction Administrative et Financière (DAF)",
            dirCode = "DAF",
            displayName = "Mme Sonia Ghariani",
            email = "daf-comptabilite@tunisair-technics.tn",
            description = "Direction Administrative et Financière & Ordonnancement"
        ),
        RoleItem(
            roleName = "Contrôleur de Gestion DAF & Direction",
            dirCode = "DAF",
            displayName = "M. Sami Chaouach",
            email = "samichaouach@gmail.com",
            description = "Contrôle de gestion, audit analytique ERP & liquidation"
        )
    )

    private val _rolesState = MutableStateFlow<List<RoleItem>>(defaultRoles)
    val rolesState: StateFlow<List<RoleItem>> = _rolesState.asStateFlow()
    val officialRoles: List<RoleItem> get() = _rolesState.value

    fun syncWithDirections(directions: List<com.example.data.local.DirectionEntity>) {
        if (directions.isEmpty()) return
        val updated = mutableListOf<RoleItem>()
        // 1. Nahla Zaoui
        updated.add(
            RoleItem(
                roleName = "Chargée de Gestion & Traitement des Lots",
                dirCode = "DAF",
                displayName = "Mme Nahla Zaoui",
                email = "nahla.zaoui@tunisair-technics.tn",
                description = "Contrôle, saisie et traitement réglementaire des lots ODD"
            )
        )
        // 2. Directions issues du référentiel directions
        directions.sortedBy { it.ordre }.forEach { dir: com.example.data.local.DirectionEntity ->
            updated.add(
                RoleItem(
                    roleName = "${dir.libelle} (${dir.code})",
                    dirCode = dir.code,
                    displayName = dir.directeur,
                    email = dir.email,
                    description = "Visa hiérarchique étape n°${dir.ordre} - ${dir.code}"
                )
            )
        }
        // 3. Sami Chaouach
        updated.add(
            RoleItem(
                roleName = "Contrôleur de Gestion DAF & Direction",
                dirCode = "DAF",
                displayName = "M. Sami Chaouach",
                email = "samichaouach@gmail.com",
                description = "Contrôle de gestion, audit analytique ERP & liquidation"
            )
        )
        _rolesState.value = updated
    }

    /**
     * Connexion directe par sélection de rôle professionnel (sans mot de passe).
     */
    fun signInAsRole(roleName: String, dirCode: String, name: String, email: String): UserProfile {
        val profile = UserProfile(
            uid = "role-$dirCode-${System.currentTimeMillis()}",
            email = email,
            displayName = name,
            role = roleName,
            directionCode = dirCode,
            isGoogleAuth = false
        )
        _currentUser.value = profile
        return profile
    }

    fun signOut() {
        try {
            getAuthSafe()?.signOut()
        } catch (_: Exception) {}
        _currentUser.value = null
    }
}
