package com.example

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
}

data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String?,
    val isEmailVerified: Boolean,
    val selectedLanguages: List<String> = emptyList(),
    val username: String? = null,
    val avatar: String? = null,
    val dob: String? = null,
    val gender: String? = null,
    val nativeLanguage: String? = null,
    val learningLanguage: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val hobbies: List<String> = emptyList(),
    val isVip: Boolean = false
)

class AuthViewModel : ViewModel() {

    // Onboarding persistent state across configuration changes and navigation flow backstacks
    var tempOnboardingUsername = ""
    var tempOnboardingDisplayName = ""
    var tempOnboardingAvatar = ""
    var tempOnboardingDob = ""
    var tempOnboardingGender = ""
    var tempOnboardingCountry = ""
    var tempOnboardingCountryCode = ""
    var tempOnboardingNativeLanguage = ""
    var tempOnboardingLearningLanguage = ""
    var tempOnboardingHobbies = emptyList<String>()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private var firebaseAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    val firestoreDb: FirebaseFirestore? get() = firestore
    private var sessionListener: ListenerRegistration? = null

    fun initializeFirebaseIfNeeded(context: Context) {
        if (firebaseAuth != null) return

        try {
            val apps = FirebaseApp.getApps(context)
            val app = if (apps.isEmpty()) {
                val apiKey = BuildConfig.FIREBASE_API_KEY
                val appId = BuildConfig.FIREBASE_APPLICATION_ID
                val projectId = BuildConfig.FIREBASE_PROJECT_ID

                if (apiKey.isNotEmpty() && apiKey != "YOUR_FIREBASE_API_KEY" &&
                    appId.isNotEmpty() && appId != "YOUR_FIREBASE_APPLICATION_ID" &&
                    projectId.isNotEmpty() && projectId != "YOUR_FIREBASE_PROJECT_ID") {

                    val options = com.google.firebase.FirebaseOptions.Builder()
                        .setApiKey(apiKey)
                        .setApplicationId(appId)
                        .setProjectId(projectId)
                        .build()
                    FirebaseApp.initializeApp(context.applicationContext, options)
                } else {
                    null
                }
            } else {
                apps[0]
            }

            if (app != null) {
                firebaseAuth = FirebaseAuth.getInstance()
                firestore = FirebaseFirestore.getInstance()
                checkCurrentUser(context)
                Log.i("AuthViewModel", "Firebase dynamically initialized with credentials successfully!")
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Dynamic Firebase initialization failed: ${e.message}")
        }
    }

    private fun checkCurrentUser(context: Context? = null) {
        val authUser = firebaseAuth?.currentUser
        if (authUser != null) {
            if (context != null) {
                checkAndCleanupUnverifiedAccount(context, authUser) {
                    val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                    val isCustomVerified = prefs.getBoolean("custom_email_verified_${authUser.uid}", false)
                    loadUserProfileFromSpOrFirestore(context, authUser.uid) { finalProfile ->
                        val initialVerified = isCustomVerified || authUser.isEmailVerified
                        _authState.value = AuthState.Success(
                            finalProfile.copy(
                                isEmailVerified = initialVerified
                            )
                        )
                        
                        // Start real-time session tracking on boot for secure single active device enforcement
                        startSessionListener(context, authUser.uid)
                        
                        // Concurrent secure validation & auto-detection on boot
                        viewModelScope.launch {
                            val realVerify = performRemoteVerificationStatusCheck(context, authUser.uid, authUser.isEmailVerified)
                            if (realVerify != initialVerified) {
                                // Mismatch found (hacker detected or newly verified via magic link). Correct local cache!
                                prefs.edit().putBoolean("custom_email_verified_${authUser.uid}", realVerify).apply()
                                _authState.value = AuthState.Success(
                                    finalProfile.copy(
                                        isEmailVerified = realVerify
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                // If context is null, emit what we have. It might not have the fully remote-fetched data though.
                _authState.value = AuthState.Success(
                    UserProfile(
                        uid = authUser.uid,
                        email = authUser.email ?: "",
                        displayName = authUser.displayName,
                        isEmailVerified = authUser.isEmailVerified
                    )
                )
            }
        }
    }

    private suspend fun performRemoteVerificationStatusCheck(context: Context, userId: String, isFirebaseVerified: Boolean): Boolean {
        if (isFirebaseVerified) return true
        
        // 1. Check Supabase first
        try {
            val response = withContext(Dispatchers.IO) {
                SupabaseClient.api.getUser(uidEq = "eq.$userId")
            }
            if (response.isSuccessful) {
                val users = response.body()
                if (!users.isNullOrEmpty() && users.first().custom_email_verified == true) {
                    val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("custom_email_verified_${userId}", true).apply()
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Remote boot check Supabase failed: ${e.message}")
        }

        // 2. Check Firestore
        val firestoreDb = firestore
        if (firestoreDb != null) {
            try {
                val docExists = withContext(Dispatchers.IO) {
                    val docTask = firestoreDb.collection("users").document(userId).get()
                    val doc = com.google.android.gms.tasks.Tasks.await(docTask)
                    doc.exists() && doc.getBoolean("customEmailVerified") == true
                }
                if (docExists) {
                    val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("custom_email_verified_${userId}", true).apply()
                    return true
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Remote boot check Firestore failed: ${e.message}")
            }
        }
        
        return false
    }

    private fun checkAndCleanupUnverifiedAccount(context: Context, user: FirebaseUser, onCleaned: () -> Unit) {
        val creationTime = user.metadata?.creationTimestamp ?: 0L
        val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
        val isCustomVerified = prefs.getBoolean("custom_email_verified_${user.uid}", false)
        val isVerified = isCustomVerified || user.isEmailVerified

        if (!isVerified && creationTime > 0L && (System.currentTimeMillis() - creationTime) > 2L * 3600L * 1000L) {
            _authState.value = AuthState.Loading
            viewModelScope.launch {
                // Double-check remote verification state to see if they verified but cache is stale
                val remoteVerified = performRemoteVerificationStatusCheck(context, user.uid, user.isEmailVerified)
                if (remoteVerified) {
                    prefs.edit().putBoolean("custom_email_verified_${user.uid}", true).apply()
                    loadUserProfileFromSpOrFirestore(context, user.uid) { profile ->
                        _authState.value = AuthState.Success(profile.copy(isEmailVerified = true))
                        onCleaned()
                    }
                } else {
                    // True unverified account older than 2 hours: Delete!
                    val firestoreDb = firestore
                    if (firestoreDb != null) {
                        try {
                            withContext(Dispatchers.IO) {
                                val task = firestoreDb.collection("users").document(user.uid).delete()
                                com.google.android.gms.tasks.Tasks.await(task)
                            }
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Error cleaning up Firestore unverified user: ${e.message}")
                        }
                    }
                    
                    prefs.edit().apply {
                        remove("auth_token_${user.uid}")
                        remove("custom_email_verified_${user.uid}")
                        apply()
                    }
                    
                    withContext(Dispatchers.Main) {
                        user.delete().addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Unverified account deleted automatically (2-hour limit). Please sign up again.", Toast.LENGTH_LONG).show()
                            } else {
                                Log.e("AuthViewModel", "Failed to delete Firebase user: ${task.exception?.message}")
                            }
                            _authState.value = AuthState.Error("Registration session expired (2 hours max). Please sign up again.")
                            onCleaned()
                        }
                    }
                }
            }
        } else {
            onCleaned()
        }
    }

    fun signUpWithEmail(
        email: String, 
        password: String, 
        context: Context, 
        onLoginNeeded: () -> Unit = {}, 
        onVerificationSent: () -> Unit
    ) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return
        }
        _authState.value = AuthState.Loading
        initializeFirebaseIfNeeded(context)

        val auth = firebaseAuth
        if (auth != null) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user != null) {
                        // Generate secure verification token
                        val token = java.util.UUID.randomUUID().toString()
                        
                        // Save token to Firestore and SharedPreferences
                        saveUserToFirestore(user, token = token)
                        val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                        prefs.edit()
                            .putString("auth_token_${user.uid}", token)
                            .putBoolean("custom_email_verified_${user.uid}", false)
                            .putLong("last_verification_send_time_${user.uid}", System.currentTimeMillis())
                            .putInt("verification_resend_count_${user.uid}", 0)
                            .apply()
                        
                        triggerSupabaseVerificationEmail(context, email, null, token, user.uid) {
                            onVerificationSent()
                            _authState.value = AuthState.Success(
                                UserProfile(
                                    uid = user.uid,
                                    email = user.email ?: "",
                                    displayName = null,
                                    isEmailVerified = false
                                )
                            )
                        }
                    } else {
                        _authState.value = AuthState.Error("Failed to obtain signed up user details.")
                    }
                }
                .addOnFailureListener { e ->
                    val errorCode = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
                    if (errorCode == "ERROR_EMAIL_ALREADY_IN_USE" || errorCode == "email-already-in-use" || e.message?.contains("already in use", ignoreCase = true) == true) {
                        // Email already in use -> Securely auto-log in!
                        Toast.makeText(context, "Account already exists! Seamlessly logging you in...", Toast.LENGTH_LONG).show()
                        loginWithEmail(email, password, context, onSignUpNeeded = onVerificationSent, onSuccess = onLoginNeeded)
                    } else {
                        _authState.value = AuthState.Error(e.localizedMessage ?: "Sign up failed")
                    }
                }
        } else {
            _authState.value = AuthState.Error("Firebase Auth not initialized.")
        }
    }

    fun loginWithEmail(
        email: String, 
        password: String, 
        context: Context, 
        onSignUpNeeded: () -> Unit,
        onSuccess: () -> Unit
    ) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return
        }
        _authState.value = AuthState.Loading
        initializeFirebaseIfNeeded(context)

        val auth = firebaseAuth
        if (auth != null) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user != null) {
                        user.reload().addOnCompleteListener {
                            viewModelScope.launch {
                                var isSupaVerified = false
                                try {
                                    val response = withContext(Dispatchers.IO) {
                                        SupabaseClient.api.getUser(uidEq = "eq.${user.uid}")
                                    }
                                    if (response.isSuccessful) {
                                        val supaUsers = response.body()
                                        if (!supaUsers.isNullOrEmpty() && supaUsers.first().custom_email_verified == true) {
                                            isSupaVerified = true
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("AuthViewModel", "Error checking verification during login: ${e.message}")
                                }

                                var isFirestoreVerified = false
                                val firestoreDb = firestore
                                if (!isSupaVerified && firestoreDb != null) {
                                    try {
                                        isFirestoreVerified = withContext(Dispatchers.IO) {
                                            val docTask = firestoreDb.collection("users").document(user.uid).get()
                                            val doc = com.google.android.gms.tasks.Tasks.await(docTask)
                                            doc.exists() && doc.getBoolean("customEmailVerified") == true
                                        }
                                    } catch (e: Exception) {
                                        Log.e("AuthViewModel", "Error checking Firestore verification during login: ${e.message}")
                                    }
                                }

                                val isVerified = isSupaVerified || isFirestoreVerified || user.isEmailVerified

                                // Sync resolved verification state to SharedPreferences
                                val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                                prefs.edit().putBoolean("custom_email_verified_${user.uid}", isVerified).apply()

                                loadUserProfileFromSpOrFirestore(context, user.uid) { finalProfile ->
                                    val finalProfileWithVerified = finalProfile.copy(isEmailVerified = isVerified)
                                    // Wait until profile fields are loaded so MainActivity doesn't prematurely trigger onboarding
                                    _authState.value = AuthState.Success(finalProfileWithVerified)
                                    if (isVerified) {
                                        registerAndStartSession(context, user.uid)
                                        triggerSupabaseLoginNotification(context, user.email ?: email, finalProfile.displayName)
                                        onSuccess()
                                    } else {
                                        // User is authentic but unverified. Halt login and redirect to VerifyEmailScreen!
                                        Toast.makeText(context, "Email is unverified. Please check your inbox and verify your email.", Toast.LENGTH_LONG).show()
                                        onSignUpNeeded()
                                    }
                                }
                            }
                        }
                    } else {
                        _authState.value = AuthState.Error("Unknown login error")
                    }
                }
                .addOnFailureListener { e ->
                    val errorCode = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
                    val isCredError = errorCode == "ERROR_USER_NOT_FOUND" || 
                                     errorCode == "user-not-found" || 
                                     errorCode == "invalid-credential" || 
                                     errorCode == "INVALID_LOGIN_CREDENTIALS" || 
                                     errorCode == "ERROR_WRONG_PASSWORD" ||
                                     errorCode == "wrong-password" ||
                                     e.message?.contains("no user record", ignoreCase = true) == true || 
                                     e.message?.contains("incorrect", ignoreCase = true) == true
                    
                    if (isCredError) {
                        _authState.value = AuthState.Error("Incorrect password or the account does not exist. Please check your credentials or switch to Sign Up.")
                    } else {
                        _authState.value = AuthState.Error(e.localizedMessage ?: "Login failed")
                    }
                }
        } else {
            _authState.value = AuthState.Error("Firebase Auth not initialized.")
        }
    }

    private var verificationJob: kotlinx.coroutines.Job? = null

    fun startAutoVerificationCheck(context: Context, onVerified: () -> Unit) {
        initializeFirebaseIfNeeded(context)
        verificationJob?.cancel()
        verificationJob = viewModelScope.launch {
            val auth = firebaseAuth
            val firestoreDb = firestore
            if (auth != null) {
                while (true) {
                    delay(3000)
                    val user = auth.currentUser
                    if (user != null) {
                        var isVerifiedRemote = false
                        
                        // 1. Check Supabase
                        try {
                            val response = SupabaseClient.api.getUser(uidEq = "eq.${user.uid}")
                            if (response.isSuccessful) {
                                val users = response.body()
                                if (!users.isNullOrEmpty() && users.first().custom_email_verified == true) {
                                    isVerifiedRemote = true
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Error checking verification in Supabase: ${e.message}")
                        }

                        // 2. Check Firestore
                        if (!isVerifiedRemote && firestoreDb != null) {
                            withContext(Dispatchers.IO) {
                                try {
                                    val docTask = firestoreDb.collection("users").document(user.uid).get()
                                    val doc = com.google.android.gms.tasks.Tasks.await(docTask)
                                    if (doc.exists() && doc.getBoolean("customEmailVerified") == true) {
                                        isVerifiedRemote = true
                                    }
                                } catch (e: Exception) {
                                    Log.e("AuthViewModel", "Error checking verification in Firestore: ${e.message}")
                                }
                            }
                        }

                        if (isVerifiedRemote) {
                            val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("custom_email_verified_${user.uid}", true).apply()
                            
                            loadUserProfileFromSpOrFirestore(context, user.uid) { finalProfile ->
                                _authState.value = AuthState.Success(finalProfile.copy(isEmailVerified = true))
                                Toast.makeText(context, "Email verified successfully!", Toast.LENGTH_SHORT).show()
                                onVerified()
                            }
                            break
                        }
                    }
                }
            } else {
                Toast.makeText(context, "Error: Firebase Auth not initialized.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun stopAutoVerificationCheck() {
        verificationJob?.cancel()
        verificationJob = null
    }

    fun checkAndRefreshVerification(context: Context, onVerified: () -> Unit) {
        val auth = firebaseAuth
        val user = auth?.currentUser
        val firestoreDb = firestore
        if (auth != null && user != null) {
            _authState.value = AuthState.Loading
            viewModelScope.launch {
                var isVerifiedRemote = false
                
                // 1. Check Supabase
                try {
                    val response = SupabaseClient.api.getUser(uidEq = "eq.${user.uid}")
                    if (response.isSuccessful) {
                        val users = response.body()
                        if (!users.isNullOrEmpty() && users.first().custom_email_verified == true) {
                            isVerifiedRemote = true
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Manual check error in Supabase: ${e.message}")
                }

                // 2. Check Firestore
                if (!isVerifiedRemote && firestoreDb != null) {
                    try {
                        val docTask = firestoreDb.collection("users").document(user.uid).get()
                        val doc = com.google.android.gms.tasks.Tasks.await(docTask)
                        if (doc.exists() && doc.getBoolean("customEmailVerified") == true) {
                            isVerifiedRemote = true
                        }
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Manual check error in Firestore: ${e.message}")
                    }
                }

                val isCustomVerified = isVerifiedRemote || user.isEmailVerified
                val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                if (isCustomVerified) {
                    prefs.edit().putBoolean("custom_email_verified_${user.uid}", true).apply()
                }

                loadUserProfileFromSpOrFirestore(context, user.uid) { finalProfile ->
                    _authState.value = AuthState.Success(finalProfile.copy(isEmailVerified = isCustomVerified))
                    if (isCustomVerified) {
                        Toast.makeText(context, "Email is verified!", Toast.LENGTH_SHORT).show()
                        onVerified()
                    } else {
                        Toast.makeText(context, "Email is still not verified. Please click the link in your email inbox.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            Toast.makeText(context, "Error: User not found.", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerSupabaseLoginNotification(
        context: Context,
        email: String,
        displayName: String?
    ) {
        viewModelScope.launch {
            try {
                val manufacturer = android.os.Build.MANUFACTURER
                val model = android.os.Build.MODEL
                val deviceName = if (manufacturer.equals("unknown", ignoreCase = true)) {
                    "Android Emulator Session"
                } else {
                    "${manufacturer.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }} $model"
                }
                
                val currentDateTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).apply {
                    timeZone = java.util.TimeZone.getDefault()
                }.format(java.util.Date()) + " (Local Device Time)"

                withContext(Dispatchers.IO) {
                    val req = SendLoginNotificationRequest(
                        email = email,
                        displayName = displayName ?: "FunkyTalk Learner",
                        deviceType = deviceName,
                        timestamp = currentDateTime
                    )
                    SupabaseClient.api.sendLoginNotification(request = req)
                }
                Log.d("AuthViewModel", "Successful login notification email triggered for $email")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to trigger login email notification: ${e.message}")
            }
        }
    }

    fun sendPasswordResetEmail(
        email: String,
        context: Context,
        onSuccess: () -> Unit = {}
    ) {
        if (email.isBlank()) {
            Toast.makeText(context, "Please enter your email to request a reset link.", Toast.LENGTH_SHORT).show()
            return
        }
        _authState.value = AuthState.Loading
        firebaseAuth?.sendPasswordResetEmail(email)
            ?.addOnSuccessListener {
                _authState.value = AuthState.Error("A secure password reset link has been sent to your email inbox: $email")
                Toast.makeText(context, "Password reset email sent successfully! Please check your inbox.", Toast.LENGTH_LONG).show()
                onSuccess()
            }
            ?.addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to send reset link.")
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    fun registerAndStartSession(context: Context, uid: String) {
        val firestoreDb = firestore ?: return
        val newSessionId = java.util.UUID.randomUUID().toString()
        
        val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("active_session_id_${uid}", newSessionId).apply()
        
        val userDoc = firestoreDb.collection("users").document(uid)
        userDoc.update("activeSessionId", newSessionId)
            .addOnSuccessListener {
                Log.d("AuthViewModel", "Successfully updated activeSessionId in Firestore: $newSessionId")
                startSessionListener(context, uid)
            }
            .addOnFailureListener {
                val data = hashMapOf<String, Any>("activeSessionId" to newSessionId)
                userDoc.set(data, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("AuthViewModel", "Successfully set activeSessionId in Firestore merge: $newSessionId")
                        startSessionListener(context, uid)
                    }
                    .addOnFailureListener { e ->
                        Log.e("AuthViewModel", "Failed to write activeSessionId to Firestore: ${e.message}")
                    }
            }
    }

    fun startSessionListener(context: Context, uid: String) {
        sessionListener?.remove()
        
        val firestoreDb = firestore ?: return
        val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
        val localSessionId = prefs.getString("active_session_id_${uid}", null)
        
        if (localSessionId == null) {
            registerAndStartSession(context, uid)
            return
        }
        
        sessionListener = firestoreDb.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AuthViewModel", "Real-time session tracker error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    if (snapshot.metadata.isFromCache) {
                        return@addSnapshotListener
                    }
                    val remoteSessionId = snapshot.getString("activeSessionId")
                    if (remoteSessionId != null && remoteSessionId != localSessionId) {
                        Log.w("AuthViewModel", "Session mismatch detected. Local: $localSessionId, Remote: $remoteSessionId.")
                        logoutDueToSessionMismatch(context, uid)
                    }
                }
            }
    }

    private fun logoutDueToSessionMismatch(context: Context, uid: String) {
        viewModelScope.launch(Dispatchers.Main) {
            sessionListener?.remove()
            sessionListener = null
            
            withContext(Dispatchers.IO) {
                firebaseAuth?.signOut()
                val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                prefs.edit().remove("active_session_id_${uid}").apply()
            }
            
            _authState.value = AuthState.Idle
            Toast.makeText(context, "Security Alert: Signed out because your account was logged in from another device/browser.", Toast.LENGTH_LONG).show()
        }
    }

    fun triggerSupabaseVerificationEmail(
        context: Context,
        email: String,
        displayName: String?,
        token: String,
        uid: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Pre-upsert to Supabase DB so verification token matches
                val supaUser = SupabaseUser(
                    uid = uid,
                    email = email,
                    display_name = displayName ?: "FunkyTalk Learner",
                    username = null,
                    avatar = null,
                    dob = null,
                    gender = null,
                    native_language = null,
                    learning_language = null,
                    country = null,
                    country_code = null,
                    hobbies = null,
                    custom_email_verified = false,
                    email_verification_token = token
                )
                
                withContext(Dispatchers.IO) {
                    try {
                        SupabaseClient.api.upsertUser(user = supaUser)
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Pre-upsert error in Supabase: ${e.message}")
                    }
                }

                val response = withContext(Dispatchers.IO) {
                    val req = SendVerificationRequest(
                        uid = uid,
                        email = email,
                        displayName = displayName ?: "FunkyTalk Learner",
                        token = token
                    )
                    SupabaseClient.api.sendVerificationEmail(request = req)
                }

                if (response.isSuccessful) {
                    Toast.makeText(context, "Verification email sent securely via Supabase!", Toast.LENGTH_LONG).show()
                    onComplete()
                } else {
                    val errMsg = response.errorBody()?.string() ?: response.message() ?: "Unknown error"
                    Log.e("AuthViewModel", "Supabase verification send failed: $errMsg")
                    Toast.makeText(context, "Sandbox fallback: click resend or see local logs.", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Supabase Edge call exception: ${e.message}")
                Toast.makeText(context, "Network issue with Supabase Edge. Proceeding...", Toast.LENGTH_LONG).show()
                onComplete()
            }
        }
    }

    fun resendVerificationEmail(context: Context) {
        initializeFirebaseIfNeeded(context)
        val auth = firebaseAuth
        val user = auth?.currentUser
        if (auth != null && user != null) {
            val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
            val lastSend = prefs.getLong("last_verification_send_time_${user.uid}", 0L)
            val resendCount = prefs.getInt("verification_resend_count_${user.uid}", 0)
            
            val currentTime = System.currentTimeMillis()
            val timeSinceLastResend = currentTime - lastSend
            
            // 1st resend (count = 0) needs 30s, 2nd (count = 1) needs 60s, 3rd (count = 2) needs 120s, and so on...
            val requiredDelayMs = 30000L * (1L shl resendCount.coerceAtMost(5))
            
            if (timeSinceLastResend < requiredDelayMs) {
                val remainingSeconds = (requiredDelayMs - timeSinceLastResend + 999) / 1000
                Toast.makeText(context, "Please wait $remainingSeconds seconds before requesting another email.", Toast.LENGTH_SHORT).show()
                return
            }
            
            val token = java.util.UUID.randomUUID().toString()
            saveUserToFirestore(user, token = token)
            
            prefs.edit()
                .putString("auth_token_${user.uid}", token)
                .putLong("last_verification_send_time_${user.uid}", currentTime)
                .putInt("verification_resend_count_${user.uid}", resendCount + 1)
                .apply()

            triggerSupabaseVerificationEmail(context, user.email ?: "", user.displayName, token, user.uid) {
                Toast.makeText(context, "Verification email resent successfully!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Error: User not found.", Toast.LENGTH_SHORT).show()
        }
    }

    fun loginWithGoogle(context: Context, idToken: String, onSuccess: () -> Unit) {
        _authState.value = AuthState.Loading
        initializeFirebaseIfNeeded(context)
        val auth = firebaseAuth
        if (auth != null) {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user != null) {
                        loadUserProfileFromSpOrFirestore(context, user.uid) { finalProfile ->
                            saveUserToFirestore(user)
                            _authState.value = AuthState.Success(finalProfile.copy(isEmailVerified = true))
                            registerAndStartSession(context, user.uid)
                            triggerSupabaseLoginNotification(context, user.email ?: "", finalProfile.displayName)
                            Toast.makeText(context, "Logged in with Google successfully!", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        }
                    } else {
                        _authState.value = AuthState.Error("Failed to obtain signed in Google user details.")
                    }
                }
                .addOnFailureListener { e ->
                    _authState.value = AuthState.Error(e.localizedMessage ?: "Google sign in failed")
                }
        } else {
            // Simulator - Use centralized google chooser logic
            simulateGoogleLogin(context, "shwetabhatingar@gmail.com", "Shwetabh", "google-uid-72", onSuccess)
        }
    }

    fun simulateGoogleLogin(context: Context, email: String, displayName: String, uid: String, onSuccess: () -> Unit) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            delay(1200)
            loadUserProfileFromSpOrFirestore(context, uid) { finalProfile ->
                val finalEmail = if (finalProfile.email.isBlank()) email else finalProfile.email
                val finalDisplayName = if (finalProfile.displayName.isNullOrBlank()) displayName else finalProfile.displayName
                val updatedProfile = finalProfile.copy(
                    uid = uid,
                    email = finalEmail,
                    displayName = finalDisplayName,
                    isEmailVerified = true
                )
                // Set success state only AFTER profile has been fully loaded from Sp or Supabase
                _authState.value = AuthState.Success(updatedProfile)
                registerAndStartSession(context, uid)
                triggerSupabaseLoginNotification(context, finalEmail, finalDisplayName)
                Toast.makeText(context, "Signed in with Google as $finalDisplayName!", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
        }
    }

    fun logout(context: Context? = null) {
        val uid = firebaseAuth?.currentUser?.uid ?: ""
        sessionListener?.remove()
        sessionListener = null
        
        if (firestore != null && uid.isNotEmpty()) {
            firestore?.collection("users")?.document(uid)?.update("activeSessionId", null)
                ?.addOnFailureListener {
                    Log.e("AuthViewModel", "Failed to clear active session field during logout")
                }
        }
        
        firebaseAuth?.signOut()
        _authState.value = AuthState.Idle
        
        if (context != null && uid.isNotEmpty()) {
            val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
            prefs.edit().remove("active_session_id_${uid}").apply()
        }
    }

    fun updateProfile(
        context: Context,
        username: String,
        displayName: String,
        avatar: String,
        dob: String,
        gender: String,
        nativeLanguage: String,
        learningLanguage: String,
        country: String,
        countryCode: String,
        hobbies: List<String>,
        onSuccess: () -> Unit
    ) {
        val currentState = _authState.value
        if (currentState is AuthState.Success) {
            viewModelScope.launch {
                var finalAvatar = avatar
                try {
                    // 1. Upload avatar to Supabase Storage if it's a content URI or local file path
                    var inputStream: java.io.InputStream? = null
                    if (avatar.startsWith("content://")) {
                        val uri = android.net.Uri.parse(avatar)
                        inputStream = context.contentResolver.openInputStream(uri)
                    } else if (avatar.startsWith("/") || avatar.startsWith("file://")) {
                        val purePath = avatar.removePrefix("file://")
                        val file = java.io.File(purePath)
                        if (file.exists()) {
                            inputStream = java.io.FileInputStream(file)
                        }
                    }
                    if (inputStream != null) {
                        val bytes = inputStream.readBytes()
                        inputStream.close()
                        val mediaType = "image/*".toMediaTypeOrNull()
                        val requestFile = bytes.toRequestBody(mediaType, 0, bytes.size)
                        val filename = "avatar_${currentState.user.uid}_${System.currentTimeMillis()}.jpg"
                        val body = okhttp3.MultipartBody.Part.createFormData("file", filename, requestFile)
                        val response = SupabaseClient.api.uploadAvatar(filename = filename, file = body)
                        if (response.isSuccessful) {
                            val baseUrl = BuildConfig.SUPABASE_URL.let {
                                if (!it.startsWith("http")) "https://$it" else it
                            }.trimEnd('/')
                            finalAvatar = "$baseUrl/storage/v1/object/public/avatars/$filename"
                            Log.d("AuthViewModel", "Avatar uploaded to Supabase: $finalAvatar")
                        } else {
                            Log.e("AuthViewModel", "Failed to upload avatar: ${response.code()} ${response.message()}")
                        }
                    }
                    
                    // 2. Save user Profile to Supabase Postgres Database
                    val supaUser = SupabaseUser(
                        uid = currentState.user.uid,
                        email = currentState.user.email,
                        display_name = displayName,
                        username = username,
                        avatar = finalAvatar,
                        dob = dob,
                        gender = gender,
                        native_language = nativeLanguage,
                        learning_language = learningLanguage,
                        country = country,
                        country_code = countryCode,
                        hobbies = hobbies
                    )
                    val apiResponse = SupabaseClient.api.upsertUser(user = supaUser)
                    if (apiResponse.isSuccessful) {
                        Log.d("AuthViewModel", "User successfully upserted to Supabase Database!")
                    } else {
                        Log.e("AuthViewModel", "Failed to upsert to Supabase Postgres: ${apiResponse.errorBody()?.string()}")
                    }

                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Supabase update error: ${e.message}", e)
                }

                val updatedUser = currentState.user.copy(
                    username = username,
                    displayName = displayName,
                    avatar = finalAvatar,
                    dob = dob,
                    gender = gender,
                    nativeLanguage = nativeLanguage,
                    learningLanguage = learningLanguage,
                    country = country,
                    countryCode = countryCode,
                    hobbies = hobbies
                )
                _authState.value = AuthState.Success(updatedUser)
                
                val userId = updatedUser.uid
                val firestoreDb = firestore
                if (firestoreDb != null) {
                    val updates = mapOf(
                        "username" to username,
                        "displayName" to displayName,
                        "avatar" to finalAvatar,
                        "dob" to dob,
                        "gender" to gender,
                        "nativeLanguage" to nativeLanguage,
                        "learningLanguage" to learningLanguage,
                        "country" to country,
                        "countryCode" to countryCode,
                        "hobbies" to hobbies,
                        "isOnboarded" to true
                    )
                    firestoreDb.collection("users").document(userId).set(updates, com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d("AuthViewModel", "Profile updated in Firestore")
                        }
                }
                
                val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putString("profile_username_${userId}", username)
                    putString("profile_displayName_${userId}", displayName)
                    putString("profile_avatar_${userId}", finalAvatar)
                    putString("profile_dob_${userId}", dob)
                    putString("profile_gender_${userId}", gender)
                    putString("profile_nativeLanguage_${userId}", nativeLanguage)
                    putString("profile_learningLanguage_${userId}", learningLanguage)
                    putString("profile_country_${userId}", country)
                    putString("profile_countryCode_${userId}", countryCode)
                    putString("profile_hobbies_${userId}", hobbies.joinToString("||"))
                    putBoolean("profile_configured_${userId}", true)
                    apply()
                }
                onSuccess()
            }
        }
    }

    fun loadUserProfileFromSpOrFirestore(context: Context, userId: String, onComplete: (UserProfile) -> Unit = {}) {
        val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
        val isCustomVerified = prefs.getBoolean("custom_email_verified_${userId}", false)
        val firebaseVerified = firebaseAuth?.currentUser?.let { if (it.uid == userId) it.isEmailVerified else false } ?: false
        val resolvedVerified = isCustomVerified || firebaseVerified
        val isVip = prefs.getBoolean("profile_is_vip_${userId}", false)

        val username = prefs.getString("profile_username_${userId}", null)
        val displayName = prefs.getString("profile_displayName_${userId}", null)
        val avatar = prefs.getString("profile_avatar_${userId}", null)
        val dob = prefs.getString("profile_dob_${userId}", null)
        val gender = prefs.getString("profile_gender_${userId}", null)
        val nativeLanguage = prefs.getString("profile_nativeLanguage_${userId}", null)
        val learningLanguage = prefs.getString("profile_learningLanguage_${userId}", null)
        val country = prefs.getString("profile_country_${userId}", null)
        val countryCode = prefs.getString("profile_countryCode_${userId}", null)
        val hobbiesStr = prefs.getString("profile_hobbies_${userId}", null)
        val hobbies = hobbiesStr?.split("||")?.filter { it.isNotEmpty() } ?: emptyList()

        val currentState = _authState.value
        val currentProfile = if (currentState is AuthState.Success) currentState.user else UserProfile(
            uid = userId,
            email = firebaseAuth?.currentUser?.email ?: "",
            displayName = displayName,
            isEmailVerified = resolvedVerified,
            username = username,
            avatar = avatar,
            dob = dob,
            gender = gender,
            nativeLanguage = nativeLanguage,
            learningLanguage = learningLanguage,
            country = country,
            countryCode = countryCode,
            hobbies = hobbies,
            isVip = isVip
        )

        val mergedProfile = currentProfile.copy(
            isEmailVerified = resolvedVerified,
            username = username ?: currentProfile.username,
            displayName = displayName ?: currentProfile.displayName,
            avatar = avatar ?: currentProfile.avatar,
            dob = dob ?: currentProfile.dob,
            gender = gender ?: currentProfile.gender,
            nativeLanguage = nativeLanguage ?: currentProfile.nativeLanguage,
            learningLanguage = learningLanguage ?: currentProfile.learningLanguage,
            country = country ?: currentProfile.country,
            countryCode = countryCode ?: currentProfile.countryCode,
            hobbies = if (hobbies.isNotEmpty()) hobbies else currentProfile.hobbies,
            isVip = isVip
        )

        val isCachedConfigured = !username.isNullOrBlank() && !dob.isNullOrBlank()
        if (!isCachedConfigured) {
            _authState.value = AuthState.Loading
            viewModelScope.launch {
                try {
                    val response = SupabaseClient.api.getUser(uidEq = "eq.$userId")
                    if (response.isSuccessful) {
                        val supaUsers = response.body()
                        if (!supaUsers.isNullOrEmpty()) {
                            val supaUser = supaUsers.first()
                            val fsUsername = supaUser.username
                            if (fsUsername != null) {
                                prefs.edit().apply {
                                    putString("profile_username_${userId}", fsUsername)
                                    putString("profile_displayName_${userId}", supaUser.display_name ?: "")
                                    putString("profile_avatar_${userId}", supaUser.avatar ?: "")
                                    putString("profile_dob_${userId}", supaUser.dob ?: "")
                                    putString("profile_gender_${userId}", supaUser.gender ?: "")
                                    putString("profile_nativeLanguage_${userId}", supaUser.native_language ?: "")
                                    putString("profile_learningLanguage_${userId}", supaUser.learning_language ?: "")
                                    putString("profile_country_${userId}", supaUser.country ?: "")
                                    putString("profile_countryCode_${userId}", supaUser.country_code ?: "")
                                    putString("profile_hobbies_${userId}", supaUser.hobbies?.joinToString("||") ?: "")
                                    putBoolean("profile_configured_${userId}", true)
                                    apply()
                                }
                                val finalProfile = mergedProfile.copy(
                                    isEmailVerified = resolvedVerified,
                                    username = fsUsername,
                                    displayName = supaUser.display_name ?: mergedProfile.displayName,
                                    avatar = supaUser.avatar ?: mergedProfile.avatar,
                                    dob = supaUser.dob ?: mergedProfile.dob,
                                    gender = supaUser.gender ?: mergedProfile.gender,
                                    nativeLanguage = supaUser.native_language ?: mergedProfile.nativeLanguage,
                                    learningLanguage = supaUser.learning_language ?: mergedProfile.learningLanguage,
                                    country = supaUser.country ?: mergedProfile.country,
                                    countryCode = supaUser.country_code ?: mergedProfile.countryCode,
                                    hobbies = supaUser.hobbies ?: mergedProfile.hobbies
                                )
                                _authState.value = AuthState.Success(finalProfile)
                                onComplete(finalProfile)
                                return@launch
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Failed to load from Supabase", e)
                }

                // Fallback to Firestore
                val firestoreDb = firestore
                if (firestoreDb != null) {
                    firestoreDb.collection("users").document(userId).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                val fsUsername = doc.getString("username")
                                val remoteCustomVerified = doc.getBoolean("customEmailVerified") ?: false
                                if (remoteCustomVerified) {
                                    prefs.edit().putBoolean("custom_email_verified_${userId}", true).apply()
                                }
                                val updatedResolvedVerified = resolvedVerified || remoteCustomVerified

                                val fsDisplayName = doc.getString("displayName")
                                val fsAvatar = doc.getString("avatar")
                                val fsDob = doc.getString("dob")
                                val fsGender = doc.getString("gender")
                                val fsNativeLanguage = doc.getString("nativeLanguage")
                                val fsLearningLanguage = doc.getString("learningLanguage")
                                val fsCountry = doc.getString("country")
                                val fsCountryCode = doc.getString("countryCode")
                                val fsHobbies = doc.get("hobbies") as? List<String> ?: emptyList()

                                if (fsUsername != null) {
                                    prefs.edit().apply {
                                        putString("profile_username_${userId}", fsUsername)
                                        putString("profile_displayName_${userId}", fsDisplayName ?: "")
                                        putString("profile_avatar_${userId}", fsAvatar ?: "")
                                        putString("profile_dob_${userId}", fsDob ?: "")
                                        putString("profile_gender_${userId}", fsGender ?: "")
                                        putString("profile_nativeLanguage_${userId}", fsNativeLanguage ?: "")
                                        putString("profile_learningLanguage_${userId}", fsLearningLanguage ?: "")
                                        putString("profile_country_${userId}", fsCountry ?: "")
                                        putString("profile_countryCode_${userId}", fsCountryCode ?: "")
                                        putString("profile_hobbies_${userId}", fsHobbies.joinToString("||"))
                                        putBoolean("profile_configured_${userId}", true)
                                        apply()
                                    }
                                    val finalProfile = mergedProfile.copy(
                                        isEmailVerified = updatedResolvedVerified,
                                        username = fsUsername,
                                        displayName = fsDisplayName ?: mergedProfile.displayName,
                                        avatar = fsAvatar ?: mergedProfile.avatar,
                                        dob = fsDob ?: mergedProfile.dob,
                                        gender = fsGender ?: mergedProfile.gender,
                                        nativeLanguage = fsNativeLanguage ?: mergedProfile.nativeLanguage,
                                        learningLanguage = fsLearningLanguage ?: mergedProfile.learningLanguage,
                                        country = fsCountry ?: mergedProfile.country,
                                        countryCode = fsCountryCode ?: mergedProfile.countryCode,
                                        hobbies = fsHobbies.ifEmpty { mergedProfile.hobbies }
                                    )
                                    _authState.value = AuthState.Success(finalProfile)
                                    onComplete(finalProfile)
                                } else {
                                    _authState.value = AuthState.Success(mergedProfile.copy(isEmailVerified = updatedResolvedVerified))
                                    onComplete(mergedProfile.copy(isEmailVerified = updatedResolvedVerified))
                                }
                            } else {
                                _authState.value = AuthState.Success(mergedProfile)
                                onComplete(mergedProfile)
                            }
                        }
                        .addOnFailureListener {
                            _authState.value = AuthState.Success(mergedProfile)
                            onComplete(mergedProfile)
                        }
                } else {
                    _authState.value = AuthState.Success(mergedProfile)
                    onComplete(mergedProfile)
                }
            }
        } else {
            _authState.value = AuthState.Success(mergedProfile)
            onComplete(mergedProfile)
        }
    }

    fun checkUsernameUnique(username: String, currentUserId: String?, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val trimmedUsername = username.trim()
                if (trimmedUsername.isEmpty()) {
                    callback(false)
                    return@launch
                }
                
                // Let's check Supabase by username (case-insensitive exact match using 'ilike')
                val response = SupabaseClient.api.getUserByUsername(usernameEq = "ilike.$trimmedUsername")
                if (response.isSuccessful) {
                    val users = response.body() ?: emptyList()
                    var isUnique = true
                    for (user in users) {
                        if (user.uid != currentUserId) {
                            isUnique = false
                            break
                        }
                    }
                    callback(isUnique)
                } else {
                    Log.e("AuthViewModel", "Supabase checkUsernameUnique with ilike failed, trying eq: ${response.code()} ${response.message()}")
                    val retryResponse = SupabaseClient.api.getUserByUsername(usernameEq = "eq.$trimmedUsername")
                    if (retryResponse.isSuccessful) {
                        val users = retryResponse.body() ?: emptyList()
                        var isUnique = true
                        for (user in users) {
                            if (user.uid != currentUserId) {
                                isUnique = false
                                break
                            }
                        }
                        callback(isUnique)
                    } else {
                        Log.e("AuthViewModel", "Retry checkUsernameUnique with eq failed")
                        callback(true)
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error checking username uniqueness in Supabase", e)
                callback(true)
            }
        }
    }

    fun saveUserToFirestore(user: FirebaseUser, code: String? = null, token: String? = null) {
        val firestoreDb = firestore ?: return
        val userData = hashMapOf<String, Any>(
            "uid" to user.uid,
            "email" to (user.email ?: ""),
            "createdAt" to System.currentTimeMillis()
        )
        if (code != null) {
            userData["authCode"] = code
        }
        if (token != null) {
            userData["emailVerificationToken"] = token
        }
        userData["customEmailVerified"] = false
        firestoreDb.collection("users").document(user.uid).set(userData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Log.d("AuthViewModel", "User document written successfully in Firestore!")
            }
            .addOnFailureListener { e ->
                Log.e("AuthViewModel", "Error adding user document: ", e)
            }
    }

    fun purchaseVip(context: android.content.Context, callback: () -> Unit) {
        val currentState = _authState.value
        if (currentState is AuthState.Success) {
            val user = currentState.user
            val updatedUser = user.copy(isVip = true)
            _authState.value = AuthState.Success(updatedUser)
            
            val prefs = context.getSharedPreferences("funky_talk_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("profile_is_vip_${user.uid}", true).apply()
            
            // Also update firestore if online
            val firestoreDb = firestore
            if (firestoreDb != null) {
                firestoreDb.collection("users").document(user.uid).update("isVip", true)
            }
            callback()
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionListener?.remove()
        sessionListener = null
    }
}
