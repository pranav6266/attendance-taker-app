package com.pranav.attendencetaker.data

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // CHECK IF USER IS LOGGED IN
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun getUserId(): String? = auth.currentUser?.uid

    // CONFIGURE GOOGLE SIGN IN
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("205098330268-j2qafeggfcpr6h23n2kgddtomee2d32j.apps.googleusercontent.com") // <--- REPLACE THIS WITH YOUR WEB CLIENT ID FROM FIREBASE CONSOLE
            .requestEmail()
            .build()

        return GoogleSignIn.getClient(context, gso)
    }

    // SIGN IN WITH FIREBASE USING GOOGLE TOKEN
    suspend fun signInWithGoogle(intent: Intent): Boolean {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            val account = task.await()
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun signOut(context: Context) {
        auth.signOut()
        getGoogleSignInClient(context).signOut()
    }
}