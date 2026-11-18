package com.example.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthHelper(
    private val context: Context,
    private val credentialManager: CredentialManager,
    private val firebaseAuth: FirebaseAuth
) {

    private val _authStateFlow = MutableStateFlow<FirebaseUser?>(firebaseAuth.currentUser)
    val authStateFlow: StateFlow<FirebaseUser?> = _authStateFlow.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _authStateFlow.value = auth.currentUser
        }
    }

    suspend fun signInWithGoogle(request: GetCredentialRequest): Result<FirebaseUser?> {
        return try {
            val result = credentialManager.getCredential(
                context = context,
                request = request
            )
            val credential = result.credential
            
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                
                val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
                _authStateFlow.value = authResult.user
                Result.success(authResult.user)
            } else {
                Result.failure(IllegalStateException("Tipo de credencial no soportado para Google Sign-In"))
            }
        } catch (e: GetCredentialException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOutUser() {
        firebaseAuth.signOut()
        _authStateFlow.value = null
    }

    suspend fun attemptAutoSignIn() {
        // Validación de sesión existente
        _authStateFlow.value = firebaseAuth.currentUser
    }
}
