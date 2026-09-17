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
    val age: Int? = null,
    val gender: String? = null,
    val nativeLanguage: String? = null,
    val learningLanguage: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val hobbies: List<String> = emptyList(),
    val onboardingComplete: Boolean = false
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

    private val _supabaseProfiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val supabaseProfiles: StateFlow<List<UserProfile>> = _supabaseProfiles

    private val _activeRoomId = MutableStateFlow<String?>(null)
    val activeRoomId: StateFlow<String?> = _activeRoomId

    private val _activeRoomTitle = MutableStateFlow<String?>(null)
    val activeRoomTitle: StateFlow<String?> = _activeRoomTitle

    fun joinRoom(roomId: String, title: String) {
        _activeRoomId.value = roomId
        _activeRoomTitle.value = title
        
        val uid = (authState.value as? AuthState.Success)?.user?.uid ?: return
        val db = firestore ?: return
        
        // 1. Add current user as speaker in rooms/{roomId}/userids/{userId}
        db.collection("rooms").document(roomId).collection("userids").document(uid).set(mapOf("action" to "speaker"))
            .addOnSuccessListener {
                Log.d("AuthViewModel", "User added to rooms/$roomId/userids/$uid")
            }
            .addOnFailureListener { e ->
                Log.e("AuthViewModel", "Error adding user to rooms/$roomId/userids/$uid", e)
            }
            
        // 2. Update users/{userId} status to "speaking", currentRoomId, isInVoiceRoom
        val userUpdate = mapOf(
            "status" to "speaking",
            "currentRoomId" to roomId,
            "isInVoiceRoom" to true
        )
        db.collection("users").document(uid).update(userUpdate)
            .addOnSuccessListener {
                Log.d("AuthViewModel", "User status updated to active room $roomId")
            }
    }

    fun leaveRoom() {
        val roomId = _activeRoomId.value
        val uid = (authState.value as? AuthState.Success)?.user?.uid
        val db = firestore
        
        if (roomId != null && uid != null && db != null) {
            // 1. Remove current user from rooms/{roomId}/userids/{userId}
            db.collection("rooms").document(roomId).collection("userids").document(uid).delete()
            
            // 2. Clear user's status inside users/{userId}
            val userUpdate = mapOf(
                "status" to "online",
                "currentRoomId" to null,
                "isInVoiceRoom" to false
            )
            db.collection("users").document(uid).update(userUpdate)
        }
        
        _activeRoomId.value = null
        _activeRoomTitle.value = null
    }

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
                val effectiveUid = SupabaseSessionManager.getUserId(context) ?: authUser.uid
                checkAndCleanupUnverifiedAccount(context, authUser) {
                    val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                    val isCustomVerified = prefs.getBoolean("custom_email_verified_${effectiveUid}", false)
                    loadUserProfileFromSpOrFirestore(context, effectiveUid) { finalProfile ->
                        val initialVerified = isCustomVerified
                        _authState.value = AuthState.Success(
                            finalProfile.copy(
                                isEmailVerified = initialVerified
                            )
                        )
                        
                        // Start real-time session tracking on boot for secure single active device enforcement
                        startSessionListener(context, effectiveUid)
                        
                        // Concurrent secure validation & auto-detection on boot
                        viewModelScope.launch {
                            val realVerify = performRemoteVerificationStatusCheck(context, effectiveUid, false)
                            if (realVerify != initialVerified) {
                                prefs.edit().putBoolean("custom_email_verified_${effectiveUid}", realVerify).apply()
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
                // If context is null, emit what we have.
                _authState.value = AuthState.Success(
                    UserProfile(
                        uid = authUser.uid,
                        email = authUser.email ?: "",
                        displayName = authUser.displayName,
                        isEmailVerified = false
                    )
                )
            }
        }
    }

    private suspend fun performRemoteVerificationStatusCheck(context: Context, userId: String, isFirebaseVerified: Boolean): Boolean {
        // 1. Check Supabase first
        try {
            val bearerAuth = SupabaseSessionManager.getBearerToken(context)
            val response = withContext(Dispatchers.IO) {
                SupabaseClient.api.getUser(
                    auth = bearerAuth,
                    uidEq = "eq.$userId"
                )
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
        val effectiveUid = SupabaseSessionManager.getUserId(context) ?: user.uid
        val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
        val isCustomVerified = prefs.getBoolean("custom_email_verified_${effectiveUid}", false)

        if (!isCustomVerified && creationTime > 0L && (System.currentTimeMillis() - creationTime) > 2L * 3600L * 1000L) {
            _authState.value = AuthState.Loading
            viewModelScope.launch {
                // Double-check remote verification state in Supabase
                val remoteVerified = performRemoteVerificationStatusCheck(context, effectiveUid, false)
                if (remoteVerified) {
                    prefs.edit().putBoolean("custom_email_verified_${effectiveUid}", true).apply()
                    loadUserProfileFromSpOrFirestore(context, effectiveUid) { profile ->
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
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Please enter your email and password.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _authState.value = AuthState.Error("Please enter a valid email address.")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters.")
            return
        }
        _authState.value = AuthState.Loading
        initializeFirebaseIfNeeded(context)

        val auth = firebaseAuth
        if (auth != null) {
            // STEP 1: Firebase.createUserWithEmailAndPassword(email, password)
            auth.createUserWithEmailAndPassword(cleanEmail, password)
                .addOnSuccessListener { result ->
                    val firebaseUser = result.user
                    if (firebaseUser != null) {
                        firebaseUser.getIdToken(true)
                            .addOnSuccessListener { tokenResult ->
                                val firebaseIdToken = tokenResult.token
                                if (!firebaseIdToken.isNullOrEmpty()) {
                                    viewModelScope.launch {
                                        // STEP 2: POST to verify-firebase-token edge function
                                        try {
                                            val verifyReq = VerifyFirebaseTokenRequest(
                                                firebase_token = firebaseIdToken,
                                                firebaseIdToken = firebaseIdToken
                                            )
                                            val verifyRes = withContext(Dispatchers.IO) {
                                                SupabaseClient.api.verifyFirebaseToken(request = verifyReq)
                                            }

                                            if (verifyRes.isSuccessful && verifyRes.body() != null) {
                                                val body = verifyRes.body()!!
                                                val accessToken = body.access_token
                                                val refreshToken = body.refresh_token
                                                val userId = body.user_id ?: body.user?.id ?: firebaseUser.uid

                                                if (!accessToken.isNullOrEmpty()) {
                                                    // STEP 3: supabase.auth.setSession(access_token, refresh_token)
                                                    SupabaseSessionManager.setSession(
                                                        context = context,
                                                        accessToken = accessToken,
                                                        refreshToken = refreshToken,
                                                        userId = userId
                                                    )

                                                    // STEP 4: Generate verification token & UPDATE public.users (auth.uid() = user_id)
                                                    val randomToken = java.util.UUID.randomUUID().toString()
                                                    val bearerAuth = "Bearer $accessToken"
                                                    val patchReq = UpdateEmailVerificationRequest(
                                                        email_verification_token = randomToken,
                                                        custom_email_verified = false
                                                    )

                                                    val patchRes = withContext(Dispatchers.IO) {
                                                        SupabaseClient.api.updateEmailVerification(
                                                            auth = bearerAuth,
                                                            uidEq = "eq.$userId",
                                                            request = patchReq
                                                        )
                                                    }

                                                    if (!patchRes.isSuccessful) {
                                                        Log.w("AuthViewModel", "PATCH users email_verification_token returned: ${patchRes.code()} ${patchRes.message()}")
                                                    }

                                                    // Also sync token locally
                                                    saveUserToFirestore(firebaseUser, token = randomToken)
                                                    val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                                                    prefs.edit()
                                                        .putString("auth_token_${userId}", randomToken)
                                                        .putBoolean("custom_email_verified_${userId}", false)
                                                        .putLong("last_verification_send_time_${userId}", System.currentTimeMillis())
                                                        .putInt("verification_resend_count_${userId}", 0)
                                                        .apply()

                                                    // STEP 5: Call send-verification-email with Authorization: Bearer <access_token>
                                                    val sendReq = SendVerificationRequest(
                                                        uid = userId,
                                                        email = cleanEmail,
                                                        displayName = firebaseUser.displayName,
                                                        token = randomToken
                                                    )

                                                    val sendRes = withContext(Dispatchers.IO) {
                                                        SupabaseClient.api.sendVerificationEmail(
                                                            auth = bearerAuth,
                                                            request = sendReq
                                                        )
                                                    }

                                                    if (sendRes.isSuccessful) {
                                                        Log.d("AuthViewModel", "Verification email dispatched successfully via Brevo/Supabase")
                                                    } else {
                                                        Log.w("AuthViewModel", "send-verification-email call status: ${sendRes.code()}")
                                                    }

                                                    _authState.value = AuthState.Success(
                                                        UserProfile(
                                                            uid = userId,
                                                            email = cleanEmail,
                                                            displayName = null,
                                                            isEmailVerified = false
                                                        )
                                                    )
                                                    onVerificationSent()
                                                } else {
                                                    _authState.value = AuthState.Error(body.error ?: "Unable to mint Supabase session tokens.")
                                                }
                                            } else {
                                                val err = verifyRes.errorBody()?.string() ?: verifyRes.message()
                                                Log.e("AuthViewModel", "verify-firebase-token failed: $err")
                                                _authState.value = AuthState.Error("Token verification failed: $err")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("AuthViewModel", "verify-firebase-token exception: ${e.message}", e)
                                            _authState.value = AuthState.Error(e.localizedMessage ?: "Network error connecting to auth service.")
                                        }
                                    }
                                } else {
                                    _authState.value = AuthState.Error("Failed to obtain Firebase ID token.")
                                }
                            }
                            .addOnFailureListener { e ->
                                _authState.value = AuthState.Error("Failed to fetch Firebase token: ${e.localizedMessage}")
                            }
                    } else {
                        _authState.value = AuthState.Error("Account creation error. Please try again.")
                    }
                }
                .addOnFailureListener { e ->
                    val errorCode = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
                    if (errorCode == "ERROR_EMAIL_ALREADY_IN_USE" || errorCode == "email-already-in-use" || e.message?.contains("already in use", ignoreCase = true) == true) {
                        Toast.makeText(context, "Account already exists! Logging you in...", Toast.LENGTH_LONG).show()
                        loginWithEmail(cleanEmail, password, context, onSignUpNeeded = onVerificationSent, onSuccess = onLoginNeeded)
                    } else if (errorCode == "ERROR_INVALID_EMAIL" || errorCode == "invalid-email") {
                        _authState.value = AuthState.Error("Please enter a valid email address.")
                    } else if (errorCode == "ERROR_WEAK_PASSWORD" || errorCode == "weak-password") {
                        _authState.value = AuthState.Error("Password is too weak. Please use a stronger password.")
                    } else {
                        _authState.value = AuthState.Error(e.localizedMessage ?: "Sign up failed. Please try again.")
                    }
                }
        } else {
            _authState.value = AuthState.Error("Auth service is not ready. Please try again in a moment.")
        }
    }

    fun loginWithEmail(
        email: String, 
        password: String, 
        context: Context, 
        onSignUpNeeded: () -> Unit,
        onSuccess: () -> Unit
    ) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Please enter your email and password.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _authState.value = AuthState.Error("Please enter a valid email address.")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters.")
            return
        }
        _authState.value = AuthState.Loading
        initializeFirebaseIfNeeded(context)

        val auth = firebaseAuth
        if (auth != null) {
            // STEP 1: Firebase.signInWithEmailAndPassword(email, password)
            auth.signInWithEmailAndPassword(cleanEmail, password)
                .addOnSuccessListener { result ->
                    val firebaseUser = result.user
                    if (firebaseUser != null) {
                        firebaseUser.getIdToken(true)
                            .addOnSuccessListener { tokenResult ->
                                val firebaseIdToken = tokenResult.token
                                if (!firebaseIdToken.isNullOrEmpty()) {
                                    viewModelScope.launch {
                                        // STEP 2: POST to verify-firebase-token edge function
                                        try {
                                            val verifyReq = VerifyFirebaseTokenRequest(
                                                firebase_token = firebaseIdToken,
                                                firebaseIdToken = firebaseIdToken
                                            )
                                            val verifyRes = withContext(Dispatchers.IO) {
                                                SupabaseClient.api.verifyFirebaseToken(request = verifyReq)
                                            }

                                            if (verifyRes.isSuccessful && verifyRes.body() != null) {
                                                val body = verifyRes.body()!!
                                                val accessToken = body.access_token
                                                val refreshToken = body.refresh_token
                                                val userId = body.user_id ?: body.user?.id ?: firebaseUser.uid

                                                if (!accessToken.isNullOrEmpty()) {
                                                    // STEP 3: supabase.auth.setSession(...)
                                                    SupabaseSessionManager.setSession(
                                                        context = context,
                                                        accessToken = accessToken,
                                                        refreshToken = refreshToken,
                                                        userId = userId
                                                    )

                                                    // STEP 4: Query public.users for custom_email_verified (Ignore Firebase isEmailVerified)
                                                    var isCustomVerified = false
                                                    val bearerAuth = "Bearer $accessToken"

                                                    try {
                                                        val userRes = withContext(Dispatchers.IO) {
                                                            SupabaseClient.api.getUser(
                                                                auth = bearerAuth,
                                                                uidEq = "eq.$userId"
                                                            )
                                                        }
                                                        if (userRes.isSuccessful) {
                                                            val supaUsers = userRes.body()
                                                            if (!supaUsers.isNullOrEmpty() && supaUsers.first().custom_email_verified == true) {
                                                                isCustomVerified = true
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e("AuthViewModel", "Error fetching user from Supabase: ${e.message}")
                                                    }

                                                    val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                                                    prefs.edit().putBoolean("custom_email_verified_${userId}", isCustomVerified).apply()

                                                    loadUserProfileFromSpOrFirestore(context, userId) { finalProfile ->
                                                        val finalProfileWithVerified = finalProfile.copy(isEmailVerified = isCustomVerified)
                                                        _authState.value = AuthState.Success(finalProfileWithVerified)

                                                        if (isCustomVerified) {
                                                            registerAndStartSession(context, userId)
                                                            // STEP 5 (Optional): send-login-notification
                                                            triggerSupabaseLoginNotification(context, firebaseUser.email ?: cleanEmail, finalProfile.displayName)
                                                            onSuccess()
                                                        } else {
                                                            // Redirect to "Please verify your email" screen
                                                            Toast.makeText(context, "Please verify your email before continuing.", Toast.LENGTH_LONG).show()
                                                            onSignUpNeeded()
                                                        }
                                                    }
                                                } else {
                                                    _authState.value = AuthState.Error(body.error ?: "Failed to authenticate session.")
                                                }
                                            } else {
                                                val err = verifyRes.errorBody()?.string() ?: verifyRes.message()
                                                Log.e("AuthViewModel", "verify-firebase-token failed: $err")
                                                _authState.value = AuthState.Error("Authentication failed: $err")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("AuthViewModel", "verify-firebase-token exception: ${e.message}", e)
                                            _authState.value = AuthState.Error(e.localizedMessage ?: "Network error during login.")
                                        }
                                    }
                                } else {
                                    _authState.value = AuthState.Error("Failed to obtain security credentials.")
                                }
                            }
                            .addOnFailureListener { e ->
                                _authState.value = AuthState.Error("Failed to fetch token: ${e.localizedMessage}")
                            }
                    } else {
                        _authState.value = AuthState.Error("Unable to find user account. Please try again.")
                    }
                }
                .addOnFailureListener { e ->
                    val errorCode = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
                    val isUserNotFound = errorCode == "ERROR_USER_NOT_FOUND" ||
                                         errorCode == "user-not-found" ||
                                         e.message?.contains("no user record", ignoreCase = true) == true
                    
                    val isWrongPassword = errorCode == "ERROR_WRONG_PASSWORD" ||
                                          errorCode == "wrong-password" ||
                                          e.message?.contains("password is invalid", ignoreCase = true) == true
                    
                    if (isUserNotFound) {
                        _authState.value = AuthState.Error("No account found with this email. Tap Sign Up below.")
                    } else if (isWrongPassword) {
                        _authState.value = AuthState.Error("Wrong password. Double check or tap 'Forgot password?'.")
                    } else if (errorCode == "invalid-credential" || errorCode == "INVALID_LOGIN_CREDENTIALS") {
                        _authState.value = AuthState.Error("Incorrect password or credentials. Please try again.")
                    } else if (errorCode == "ERROR_INVALID_EMAIL" || errorCode == "invalid-email") {
                        _authState.value = AuthState.Error("Please enter a valid email address.")
                    } else {
                        _authState.value = AuthState.Error(e.localizedMessage ?: "Login failed. Please try again.")
                    }
                }
        } else {
            _authState.value = AuthState.Error("Auth service is not ready. Please try again in a moment.")
        }
    }

    private var verificationJob: kotlinx.coroutines.Job? = null

    fun startAutoVerificationCheck(context: Context, onVerified: () -> Unit) {
        initializeFirebaseIfNeeded(context)
        verificationJob?.cancel()
        verificationJob = viewModelScope.launch {
            val auth = firebaseAuth
            if (auth != null) {
                while (true) {
                    delay(3000)
                    val supabaseUserId = SupabaseSessionManager.getUserId(context) ?: auth.currentUser?.uid
                    if (supabaseUserId != null) {
                        var isVerifiedRemote = false
                        val bearerToken = SupabaseSessionManager.getBearerToken(context)
                        
                        // 1. Check Supabase custom_email_verified
                        try {
                            val response = withContext(Dispatchers.IO) {
                                SupabaseClient.api.getUser(
                                    auth = bearerToken,
                                    uidEq = "eq.$supabaseUserId"
                                )
                            }
                            if (response.isSuccessful) {
                                val users = response.body()
                                if (!users.isNullOrEmpty() && users.first().custom_email_verified == true) {
                                    isVerifiedRemote = true
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Error polling verification in Supabase: ${e.message}")
                        }

                        if (isVerifiedRemote) {
                            val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("custom_email_verified_${supabaseUserId}", true).apply()
                            
                            loadUserProfileFromSpOrFirestore(context, supabaseUserId) { finalProfile ->
                                _authState.value = AuthState.Success(finalProfile.copy(isEmailVerified = true))
                                Toast.makeText(context, "Email verified successfully!", Toast.LENGTH_SHORT).show()
                                onVerified()
                            }
                            break
                        }
                    }
                }
            } else {
                Toast.makeText(context, "Error: Auth service not initialized.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun stopAutoVerificationCheck() {
        verificationJob?.cancel()
        verificationJob = null
    }

    fun checkAndRefreshVerification(context: Context, onVerified: () -> Unit) {
        val auth = firebaseAuth
        val supabaseUserId = SupabaseSessionManager.getUserId(context) ?: auth?.currentUser?.uid
        if (auth != null && supabaseUserId != null) {
            _authState.value = AuthState.Loading
            viewModelScope.launch {
                var isVerifiedRemote = false
                val bearerToken = SupabaseSessionManager.getBearerToken(context)
                
                // Check Supabase custom_email_verified
                try {
                    val response = withContext(Dispatchers.IO) {
                        SupabaseClient.api.getUser(
                            auth = bearerToken,
                            uidEq = "eq.$supabaseUserId"
                        )
                    }
                    if (response.isSuccessful) {
                        val users = response.body()
                        if (!users.isNullOrEmpty() && users.first().custom_email_verified == true) {
                            isVerifiedRemote = true
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Manual check error in Supabase: ${e.message}")
                }

                val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                if (isVerifiedRemote) {
                    prefs.edit().putBoolean("custom_email_verified_${supabaseUserId}", true).apply()
                }

                loadUserProfileFromSpOrFirestore(context, supabaseUserId) { finalProfile ->
                    _authState.value = AuthState.Success(finalProfile.copy(isEmailVerified = isVerifiedRemote))
                    if (isVerifiedRemote) {
                        Toast.makeText(context, "Email is verified!", Toast.LENGTH_SHORT).show()
                        onVerified()
                    } else {
                        Toast.makeText(context, "Email is still not verified. Please click the link in your inbox.", Toast.LENGTH_LONG).show()
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
        val initialUpdates = mapOf(
            "activeSessionId" to newSessionId,
            "status" to "online",
            "isInVoiceRoom" to false,
            "currentRoomId" to ""
        )
        userDoc.update(initialUpdates)
            .addOnSuccessListener {
                Log.d("AuthViewModel", "Successfully updated activeSessionId & online status in Firestore: $newSessionId")
                startSessionListener(context, uid)
            }
            .addOnFailureListener {
                userDoc.set(initialUpdates, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("AuthViewModel", "Successfully merged activeSessionId & online status in Firestore: $newSessionId")
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

    private fun hashTokenSha256(token: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
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
                val bearerAuth = SupabaseSessionManager.getBearerToken(context)

                // Update email_verification_token on existing record
                val patchReq = UpdateEmailVerificationRequest(
                    email_verification_token = token,
                    custom_email_verified = false
                )

                withContext(Dispatchers.IO) {
                    try {
                        SupabaseClient.api.updateEmailVerification(
                            auth = bearerAuth,
                            uidEq = "eq.$uid",
                            request = patchReq
                        )
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Error updating token in Supabase: ${e.message}")
                    }
                }

                val response = withContext(Dispatchers.IO) {
                    val req = SendVerificationRequest(
                        uid = uid,
                        email = email,
                        displayName = displayName ?: "FunkyTalk Learner",
                        token = token
                    )
                    SupabaseClient.api.sendVerificationEmail(
                        auth = bearerAuth,
                        request = req
                    )
                }

                if (response.isSuccessful) {
                    Toast.makeText(context, "Verification email sent securely!", Toast.LENGTH_LONG).show()
                    onComplete()
                } else {
                    val errMsg = response.errorBody()?.string() ?: response.message() ?: "Unknown error"
                    Log.e("AuthViewModel", "Supabase verification send failed: $errMsg")
                    Toast.makeText(context, "Please check your inbox or click resend.", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Supabase Edge call exception: ${e.message}")
                Toast.makeText(context, "Network issue sending verification email.", Toast.LENGTH_LONG).show()
                onComplete()
            }
        }
    }

    fun resendVerificationEmail(context: Context) {
        initializeFirebaseIfNeeded(context)
        val auth = firebaseAuth
        val user = auth?.currentUser
        val supabaseUserId = SupabaseSessionManager.getUserId(context) ?: user?.uid
        if (auth != null && user != null && supabaseUserId != null) {
            val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
            val lastSend = prefs.getLong("last_verification_send_time_${supabaseUserId}", 0L)
            val resendCount = prefs.getInt("verification_resend_count_${supabaseUserId}", 0)
            
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
                .putString("auth_token_${supabaseUserId}", token)
                .putLong("last_verification_send_time_${supabaseUserId}", currentTime)
                .putInt("verification_resend_count_${supabaseUserId}", resendCount + 1)
                .apply()

            triggerSupabaseVerificationEmail(context, user.email ?: "", user.displayName, token, supabaseUserId) {
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
            _authState.value = AuthState.Error("Google Sign-In failed. Please try again.")
        }
    }

    fun logout(context: Context? = null) {
        val uid = firebaseAuth?.currentUser?.uid ?: ""
        sessionListener?.remove()
        sessionListener = null
        
        if (firestore != null && uid.isNotEmpty()) {
            val offlineUpdates = mapOf(
                "activeSessionId" to null,
                "status" to "offline",
                "isInVoiceRoom" to false,
                "currentRoomId" to ""
            )
            firestore?.collection("users")?.document(uid)?.update(offlineUpdates)
                ?.addOnFailureListener {
                    Log.e("AuthViewModel", "Failed to clear active session and offline status during logout")
                }
        }
        
        firebaseAuth?.signOut()
        _authState.value = AuthState.Idle
        
        if (context != null) {
            SupabaseSessionManager.clearSession(context)
            if (uid.isNotEmpty()) {
                val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                prefs.edit().remove("active_session_id_${uid}").apply()
            }
        }
    }

    fun patchProfileStep(
        context: Context,
        updates: Map<String, Any?>,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val supaUid = SupabaseSessionManager.getUserId(context)
                val token = SupabaseSessionManager.getBearerToken(context)
                if (supaUid.isNullOrBlank()) {
                    Log.e("AuthViewModel", "User session not found for patchProfileStep")
                    onSuccess()
                    return@launch
                }

                val response = SupabaseClient.api.patchProfile(
                    auth = token,
                    idEq = "eq.$supaUid",
                    updates = updates
                )

                val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                val editor = prefs.edit()

                if (updates.containsKey("username")) {
                    editor.putString("profile_username_${supaUid}", updates["username"] as? String ?: "")
                }
                if (updates.containsKey("profile_name")) {
                    editor.putString("profile_displayName_${supaUid}", updates["profile_name"] as? String ?: "")
                }
                if (updates.containsKey("avatar_url")) {
                    editor.putString("profile_avatar_${supaUid}", updates["avatar_url"] as? String ?: "")
                }
                if (updates.containsKey("age")) {
                    val ageVal = (updates["age"] as? Number)?.toInt() ?: 0
                    editor.putInt("profile_age_${supaUid}", ageVal)
                }
                if (updates.containsKey("gender")) {
                    editor.putString("profile_gender_${supaUid}", updates["gender"] as? String ?: "")
                }
                if (updates.containsKey("country")) {
                    editor.putString("profile_country_${supaUid}", updates["country"] as? String ?: "")
                }
                if (updates.containsKey("native_languages")) {
                    val list = updates["native_languages"] as? List<*> ?: emptyList<Any>()
                    editor.putString("profile_nativeLanguage_${supaUid}", list.joinToString(", "))
                }
                if (updates.containsKey("learning_languages")) {
                    val list = updates["learning_languages"] as? List<*> ?: emptyList<Any>()
                    editor.putString("profile_learningLanguage_${supaUid}", list.joinToString(", "))
                }
                if (updates.containsKey("hobbies")) {
                    val list = updates["hobbies"] as? List<*> ?: emptyList<Any>()
                    editor.putString("profile_hobbies_${supaUid}", list.joinToString("||"))
                }
                if (updates.containsKey("onboarding_complete")) {
                    val complete = updates["onboarding_complete"] as? Boolean ?: false
                    editor.putBoolean("profile_onboarding_complete_${supaUid}", complete)
                }
                editor.apply()

                val current = _authState.value
                if (current is AuthState.Success) {
                    val u = current.user
                    val updatedUser = u.copy(
                        username = if (updates.containsKey("username")) updates["username"] as? String else u.username,
                        displayName = if (updates.containsKey("profile_name")) updates["profile_name"] as? String else u.displayName,
                        avatar = if (updates.containsKey("avatar_url")) updates["avatar_url"] as? String else u.avatar,
                        age = if (updates.containsKey("age")) (updates["age"] as? Number)?.toInt() else u.age,
                        gender = if (updates.containsKey("gender")) updates["gender"] as? String else u.gender,
                        country = if (updates.containsKey("country")) updates["country"] as? String else u.country,
                        nativeLanguage = if (updates.containsKey("native_languages")) (updates["native_languages"] as? List<*>)?.joinToString(", ") else u.nativeLanguage,
                        learningLanguage = if (updates.containsKey("learning_languages")) (updates["learning_languages"] as? List<*>)?.joinToString(", ") else u.learningLanguage,
                        hobbies = if (updates.containsKey("hobbies")) (updates["hobbies"] as? List<String>) ?: u.hobbies else u.hobbies,
                        onboardingComplete = if (updates.containsKey("onboarding_complete")) updates["onboarding_complete"] as? Boolean ?: u.onboardingComplete else u.onboardingComplete
                    )
                    _authState.value = AuthState.Success(updatedUser)
                }

                if (response.isSuccessful) {
                    Log.d("AuthViewModel", "Profile patched successfully: $updates")
                    onSuccess()
                } else {
                    Log.e("AuthViewModel", "Failed to patch profile: ${response.code()} ${response.errorBody()?.string()}")
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Exception patching profile", e)
                onSuccess()
            }
        }
    }

    fun patchStep1Profile(
        context: Context,
        username: String,
        displayName: String,
        avatar: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            var finalAvatar = avatar
            try {
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
                    val supaUid = SupabaseSessionManager.getUserId(context) ?: "anon"
                    val filename = "avatar_${supaUid}_${System.currentTimeMillis()}.jpg"
                    val body = okhttp3.MultipartBody.Part.createFormData("file", filename, requestFile)
                    val bearerAuth = SupabaseSessionManager.getBearerToken(context)
                    val response = SupabaseClient.api.uploadAvatar(
                        auth = bearerAuth,
                        filename = filename,
                        file = body
                    )
                    if (response.isSuccessful) {
                        val baseUrl = BuildConfig.SUPABASE_URL.let {
                            if (!it.startsWith("http")) "https://$it" else it
                        }.trimEnd('/')
                        finalAvatar = "$baseUrl/storage/v1/object/public/avatars/$filename"
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Avatar upload exception", e)
            }

            patchProfileStep(
                context = context,
                updates = mapOf(
                    "username" to username,
                    "profile_name" to displayName,
                    "avatar_url" to finalAvatar
                ),
                onSuccess = onSuccess,
                onError = onError
            )
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

                    val supaUid = SupabaseSessionManager.getUserId(context) ?: currentState.user.uid
                    val bearerAuth = SupabaseSessionManager.getBearerToken(context)
                    val nativeList = nativeLanguage.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val learningList = learningLanguage.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                    SupabaseClient.api.patchProfile(
                        auth = bearerAuth,
                        idEq = "eq.$supaUid",
                        updates = mapOf(
                            "username" to username,
                            "profile_name" to displayName,
                            "avatar_url" to finalAvatar,
                            "gender" to gender,
                            "country" to country,
                            "native_languages" to nativeList,
                            "learning_languages" to learningList,
                            "hobbies" to hobbies,
                            "onboarding_complete" to true
                        )
                    )
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Supabase update error: ${e.message}", e)
                }

                val userId = currentState.user.uid
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
                    putBoolean("profile_onboarding_complete_${userId}", true)
                    apply()
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
                    hobbies = hobbies,
                    onboardingComplete = true
                )
                _authState.value = AuthState.Success(updatedUser)
                onSuccess()
            }
        }
    }

    fun loadUserProfileFromSpOrFirestore(context: Context, userId: String, onComplete: (UserProfile) -> Unit = {}) {
        val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
        val isCustomVerified = prefs.getBoolean("custom_email_verified_${userId}", false)
        val resolvedVerified = isCustomVerified

        val username = prefs.getString("profile_username_${userId}", null)
        val displayName = prefs.getString("profile_displayName_${userId}", null)
        val avatar = prefs.getString("profile_avatar_${userId}", null)
        val ageVal = prefs.getInt("profile_age_${userId}", 0).takeIf { it > 0 }
        val dob = prefs.getString("profile_dob_${userId}", null)
        val gender = prefs.getString("profile_gender_${userId}", null)
        val nativeLanguage = prefs.getString("profile_nativeLanguage_${userId}", null)
        val learningLanguage = prefs.getString("profile_learningLanguage_${userId}", null)
        val country = prefs.getString("profile_country_${userId}", null)
        val countryCode = prefs.getString("profile_countryCode_${userId}", null)
        val hobbiesStr = prefs.getString("profile_hobbies_${userId}", null)
        val hobbies = hobbiesStr?.split("||")?.filter { it.isNotEmpty() } ?: emptyList()
        val isOnboardingComplete = prefs.getBoolean("profile_onboarding_complete_${userId}", false)

        val currentState = _authState.value
        val currentProfile = if (currentState is AuthState.Success) currentState.user else UserProfile(
            uid = userId,
            email = firebaseAuth?.currentUser?.email ?: "",
            displayName = displayName,
            isEmailVerified = resolvedVerified,
            username = username,
            avatar = avatar,
            age = ageVal,
            dob = dob,
            gender = gender,
            nativeLanguage = nativeLanguage,
            learningLanguage = learningLanguage,
            country = country,
            countryCode = countryCode,
            hobbies = hobbies,
            onboardingComplete = isOnboardingComplete
        )

        val mergedProfile = currentProfile.copy(
            isEmailVerified = resolvedVerified,
            username = username ?: currentProfile.username,
            displayName = displayName ?: currentProfile.displayName,
            avatar = avatar ?: currentProfile.avatar,
            age = ageVal ?: currentProfile.age,
            dob = dob ?: currentProfile.dob,
            gender = gender ?: currentProfile.gender,
            nativeLanguage = nativeLanguage ?: currentProfile.nativeLanguage,
            learningLanguage = learningLanguage ?: currentProfile.learningLanguage,
            country = country ?: currentProfile.country,
            countryCode = countryCode ?: currentProfile.countryCode,
            hobbies = if (hobbies.isNotEmpty()) hobbies else currentProfile.hobbies,
            onboardingComplete = isOnboardingComplete || currentProfile.onboardingComplete
        )

        viewModelScope.launch {
            try {
                val bearerToken = SupabaseSessionManager.getBearerToken(context)
                val response = SupabaseClient.api.getProfile(
                    auth = bearerToken,
                    idEq = "eq.$userId"
                )
                if (response.isSuccessful) {
                    val supaProfiles = response.body()
                    if (!supaProfiles.isNullOrEmpty()) {
                        val supaProfile = supaProfiles.first()
                        val supaComplete = supaProfile.onboarding_complete == true
                        val nativeStr = supaProfile.native_languages?.joinToString(", ")
                        val learningStr = supaProfile.learning_languages?.joinToString(", ")
                        val hobbiesList = supaProfile.hobbies ?: emptyList()

                        prefs.edit().apply {
                            putString("profile_username_${userId}", supaProfile.username ?: "")
                            putString("profile_displayName_${userId}", supaProfile.profile_name ?: "")
                            putString("profile_avatar_${userId}", supaProfile.avatar_url ?: "")
                            putInt("profile_age_${userId}", supaProfile.age ?: 0)
                            putString("profile_gender_${userId}", supaProfile.gender ?: "")
                            putString("profile_nativeLanguage_${userId}", nativeStr ?: "")
                            putString("profile_learningLanguage_${userId}", learningStr ?: "")
                            putString("profile_country_${userId}", supaProfile.country ?: "")
                            putString("profile_hobbies_${userId}", hobbiesList.joinToString("||"))
                            putBoolean("profile_onboarding_complete_${userId}", supaComplete)
                            apply()
                        }

                        val finalProfile = mergedProfile.copy(
                            isEmailVerified = resolvedVerified,
                            username = supaProfile.username ?: mergedProfile.username,
                            displayName = supaProfile.profile_name ?: mergedProfile.displayName,
                            avatar = supaProfile.avatar_url ?: mergedProfile.avatar,
                            age = supaProfile.age ?: mergedProfile.age,
                            gender = supaProfile.gender ?: mergedProfile.gender,
                            nativeLanguage = nativeStr ?: mergedProfile.nativeLanguage,
                            learningLanguage = learningStr ?: mergedProfile.learningLanguage,
                            country = supaProfile.country ?: mergedProfile.country,
                            hobbies = if (hobbiesList.isNotEmpty()) hobbiesList else mergedProfile.hobbies,
                            onboardingComplete = supaComplete
                        )
                        _authState.value = AuthState.Success(finalProfile)
                        onComplete(finalProfile)
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to load profile from Supabase: ${e.message}", e)
            }

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

    fun fetchSupabaseProfiles(context: Context) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    SupabaseClient.api.getAllUsers()
                }
                if (response.isSuccessful) {
                    val supaUsers = response.body() ?: emptyList()
                    val profiles = supaUsers.map { supaUser ->
                        UserProfile(
                            uid = supaUser.uid,
                            email = supaUser.email,
                            displayName = supaUser.display_name ?: "Learner",
                            isEmailVerified = supaUser.custom_email_verified ?: false,
                            username = supaUser.username ?: "learner_user",
                            avatar = supaUser.avatar ?: "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150",
                            dob = supaUser.dob ?: "2000-01-01",
                            gender = supaUser.gender ?: "Non-binary",
                            nativeLanguage = supaUser.native_language ?: "Hindi",
                            learningLanguage = supaUser.learning_language ?: "English",
                            country = supaUser.country ?: "India",
                            countryCode = supaUser.country_code ?: "in",
                            hobbies = supaUser.hobbies ?: emptyList()
                        )
                    }
                    _supabaseProfiles.value = profiles
                    Log.d("AuthViewModel", "Fetched ${profiles.size} profiles from Supabase successfully")
                } else {
                    Log.e("AuthViewModel", "Failed to fetch Supabase profiles: ${response.code()} ${response.message()}")
                    _supabaseProfiles.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error fetching Supabase profiles: ${e.message}", e)
                _supabaseProfiles.value = emptyList()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionListener?.remove()
        sessionListener = null
    }
}
