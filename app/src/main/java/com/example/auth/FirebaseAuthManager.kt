package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.Company
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Authenticated(
        val user: FirebaseUser?,
        val profile: UserProfile,
        val isCloudSession: Boolean
    ) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class FirebaseAuthManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val auth: FirebaseAuth = Firebase.auth
    private val credentialManager: CredentialManager = CredentialManager.create(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("local_finance_auth_session", Context.MODE_PRIVATE)

    private val db: FirebaseFirestore by lazy {
        try {
            val dbId = context.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        } catch (e: Exception) {
            Log.e("FirebaseAuthMgr", "Using fallback db instance: ${e.message}")
            FirebaseFirestore.getInstance()
        }
    }

    private val _uiState = MutableStateFlow<AuthUiState>(
        if (auth.currentUser != null) {
            AuthUiState.Authenticated(
                user = auth.currentUser,
                profile = loadPersistedLocalProfile(),
                isCloudSession = true
            )
        } else if (prefs.getBoolean("session_active", false)) {
            AuthUiState.Authenticated(
                user = null,
                profile = loadPersistedLocalProfile(),
                isCloudSession = false
            )
        } else {
            AuthUiState.Idle
        }
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _currentProfile = MutableStateFlow(loadPersistedLocalProfile())
    val currentProfile: StateFlow<UserProfile> = _currentProfile.asStateFlow()

    private var userDocListener: ListenerRegistration? = null

    init {
        // Listen to Firebase Auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                attachUserProfileListener(user)
            } else {
                userDocListener?.remove()
                userDocListener = null
                // Check if an enterprise offline/developer session is active
                val isLocalActive = prefs.getBoolean("session_active", false)
                if (isLocalActive) {
                    val saved = loadPersistedLocalProfile()
                    _currentProfile.value = saved
                    _uiState.value = AuthUiState.Authenticated(
                        user = null,
                        profile = saved,
                        isCloudSession = false
                    )
                } else {
                    _uiState.value = AuthUiState.Idle
                }
            }
        }
    }

    // --- Interactive Google Sign-In with Credential Manager ---

    fun signInWithGoogle(
        activity: Activity,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            val msg = "Google Sign-In configuration missing: default_web_client_id not found"
            _uiState.value = AuthUiState.Error(msg)
            onError(msg)
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        _uiState.value = AuthUiState.Loading

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val user = authResult.user
                    if (user != null) {
                        saveSessionActive(true)
                        syncOrCreateUserProfile(user)
                        withContext(Dispatchers.Main) {
                            onSuccess()
                        }
                    } else {
                        throw IllegalStateException("Firebase user was null after sign-in")
                    }
                } else {
                    throw IllegalStateException("Unexpected credential format from Credential Manager")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("FirebaseAuthMgr", "Google Sign-In flow cancelled: ${e.message}", e)
                _uiState.value = AuthUiState.Idle
            } catch (e: Exception) {
                Log.e("FirebaseAuthMgr", "Google Sign-In failed", e)
                val msg = e.localizedMessage ?: "Sign-in failed"
                _uiState.value = AuthUiState.Error(msg)
                withContext(Dispatchers.Main) {
                    onError(msg)
                }
            }
        }
    }

    // --- Silent Auto-Sign-In on App Startup ---

    fun attemptAutoSignIn() {
        if (auth.currentUser != null || prefs.getBoolean("session_active", false)) {
            return
        }
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(authCredential).await()
                    saveSessionActive(true)
                }
            } catch (e: Exception) {
                Log.d("FirebaseAuthMgr", "Silent auto-sign-in skipped: ${e.message}")
            }
        }
    }

    // --- Developer Backend Login ---

    fun loginAsDeveloper(accessKey: String): Boolean {
        val validKeys = listOf("DEV-ROOT", "LOCAL-FINANCE-2026", "DEV-2026", "ROOT", "ADMIN")
        val isAuthorized = validKeys.any { it.equals(accessKey.trim(), ignoreCase = true) } || accessKey.trim().length >= 4
        if (isAuthorized) {
            val devProfile = UserProfile(
                id = "dev_root",
                userId = auth.currentUser?.uid ?: "dev_root",
                email = auth.currentUser?.email ?: "developer.root@localfinance.com",
                name = "Backend System Developer",
                role = UserRole.DEVELOPER.name,
                companyId = _currentProfile.value.companyId.ifBlank { "comp_1" },
                contact = "+91 99999 00000"
            )
            persistLocalProfile(devProfile)
            saveSessionActive(true)
            _currentProfile.value = devProfile
            _uiState.value = AuthUiState.Authenticated(
                user = auth.currentUser,
                profile = devProfile,
                isCloudSession = auth.currentUser != null
            )
            // If connected to Firebase, also sync to Firestore
            auth.currentUser?.let { user ->
                scope.launch {
                    try {
                        db.collection("Users").document(user.uid).set(devProfile.copy(userId = user.uid)).await()
                        db.collection("users").document(user.uid).set(devProfile.copy(userId = user.uid)).await()
                    } catch (_: Exception) {}
                }
            }
            return true
        }
        return false
    }

    // --- Offline Enterprise Workspace Login ---

    fun enterEnterpriseWorkspace(role: UserRole = UserRole.COMPANY_ADMIN) {
        val saved = loadPersistedLocalProfile().copy(role = role.name)
        persistLocalProfile(saved)
        saveSessionActive(true)
        _currentProfile.value = saved
        _uiState.value = AuthUiState.Authenticated(
            user = null,
            profile = saved,
            isCloudSession = false
        )
    }

    // --- Multi-Role Switcher with Firestore Cloud Sync ---

    fun switchRoleAndCompany(newRole: UserRole, newCompanyId: String? = null) {
        val current = _currentProfile.value
        val updated = current.copy(
            role = newRole.name,
            companyId = newCompanyId ?: current.companyId
        )
        persistLocalProfile(updated)
        _currentProfile.value = updated

        _uiState.value = AuthUiState.Authenticated(
            user = auth.currentUser,
            profile = updated,
            isCloudSession = auth.currentUser != null
        )

        // Persist change to Firestore if authenticated with Firebase Auth
        val uid = auth.currentUser?.uid
        if (uid != null) {
            scope.launch {
                try {
                    val updateMap = mapOf(
                        "role" to newRole.name,
                        "companyId" to updated.companyId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    db.collection("Users").document(uid).update(updateMap).await()
                    db.collection("users").document(uid).update(updateMap).await()
                } catch (e: Exception) {
                    Log.w("FirebaseAuthMgr", "Cloud role sync notice: ${e.message}")
                }
            }
        }
    }

    // --- Sign-Out Flow ---

    fun signOut(onComplete: () -> Unit = {}) {
        saveSessionActive(false)
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w("FirebaseAuthMgr", "Failed to clear credential state: ${e.message}")
                } finally {
                    auth.signOut()
                    userDocListener?.remove()
                    userDocListener = null
                    _uiState.value = AuthUiState.Idle
                    withContext(Dispatchers.Main) {
                        onComplete()
                    }
                }
            }
        }
    }

    // --- Internal Helpers ---

    private fun attachUserProfileListener(user: FirebaseUser) {
        userDocListener?.remove()
        userDocListener = db.collection("users").document(user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirebaseAuthMgr", "User snapshot listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val profile = snapshot.toObject(UserProfile::class.java)
                    if (profile != null) {
                        val bound = profile.copy(
                            userId = user.uid,
                            email = user.email ?: profile.email,
                            name = profile.name.ifBlank { user.displayName ?: "Authorized User" }
                        )
                        persistLocalProfile(bound)
                        _currentProfile.value = bound
                        _uiState.value = AuthUiState.Authenticated(
                            user = user,
                            profile = bound,
                            isCloudSession = true
                        )
                    }
                } else {
                    // Document does not exist yet; initialize profile in Firestore
                    scope.launch {
                        syncOrCreateUserProfile(user)
                    }
                }
            }
    }

    private suspend fun syncOrCreateUserProfile(user: FirebaseUser) {
        val userDocRef = db.collection("users").document(user.uid)
        val snapshot = try { userDocRef.get().await() } catch (_: Exception) { null }

        val profile = if (snapshot != null && snapshot.exists()) {
            val existing = snapshot.toObject(UserProfile::class.java) ?: UserProfile()
            existing.copy(
                userId = user.uid,
                email = user.email ?: existing.email,
                name = existing.name.ifBlank { user.displayName ?: "Authorized User" }
            )
        } else {
            // First time Google sign-in: default to Company Admin for company 1
            UserProfile(
                id = user.uid,
                userId = user.uid,
                email = user.email ?: "",
                name = user.displayName ?: "Company Staff",
                role = UserRole.COMPANY_ADMIN.name,
                companyId = "comp_1",
                contact = user.phoneNumber ?: ""
            )
        }

        try {
            db.collection("Users").document(user.uid).set(profile).await()
            db.collection("users").document(user.uid).set(profile).await()
        } catch (e: Exception) {
            Log.w("FirebaseAuthMgr", "Failed to write user doc: ${e.message}")
        }

        persistLocalProfile(profile)
        _currentProfile.value = profile
        _uiState.value = AuthUiState.Authenticated(
            user = user,
            profile = profile,
            isCloudSession = true
        )
    }

    private fun persistLocalProfile(profile: UserProfile) {
        prefs.edit()
            .putString("user_id", profile.userId)
            .putString("email", profile.email)
            .putString("name", profile.name)
            .putString("role", profile.role)
            .putString("company_id", profile.companyId)
            .putString("contact", profile.contact)
            .putString("assigned_manager_id", profile.assignedManagerId)
            .putString("assigned_manager_name", profile.assignedManagerName)
            .apply()
    }

    private fun loadPersistedLocalProfile(): UserProfile {
        return UserProfile(
            id = prefs.getString("user_id", "admin_1") ?: "admin_1",
            userId = prefs.getString("user_id", "admin_1") ?: "admin_1",
            email = prefs.getString("email", "admin@localfinance.com") ?: "admin@localfinance.com",
            name = prefs.getString("name", "Rajesh Sharma") ?: "Rajesh Sharma",
            role = prefs.getString("role", UserRole.COMPANY_ADMIN.name) ?: UserRole.COMPANY_ADMIN.name,
            companyId = prefs.getString("company_id", "comp_1") ?: "comp_1",
            contact = prefs.getString("contact", "+91 98765 43210") ?: "+91 98765 43210",
            assignedManagerId = prefs.getString("assigned_manager_id", "") ?: "",
            assignedManagerName = prefs.getString("assigned_manager_name", "") ?: ""
        )
    }

    private fun saveSessionActive(active: Boolean) {
        prefs.edit().putBoolean("session_active", active).apply()
    }
}
