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
import kotlinx.coroutines.tasks.await

class AuthHelper(
    private val context: Context,
    private val credentialManager: CredentialManager,
    private val firebaseAuth: FirebaseAuth
) {

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
                Result.success(authResult.user)
            } else {
                Result.failure(IllegalStateException("Tipo de credencial no soportado para Google Sign-In"))
            }
        } catch (e: GetCredentialException) {
            // Captura cualquier error o cancelación de Credential Manager de forma segura
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
