
package com.example.data.datasource.remote

import com.example.core_common.result.CustomResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRemoteDataSource {

    override fun observeAuthState(): Flow<CustomResult<FirebaseUser, Exception>> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) {
                trySend(CustomResult.Failure(Exception("No user is currently signed in")))
            } else {
                trySend(CustomResult.Success(firebaseAuth.currentUser!!))
            }
        }
        auth.addAuthStateListener(authStateListener)

        awaitClose { auth.removeAuthStateListener(authStateListener) }
    }

    override fun getCurrentUser(): CustomResult<FirebaseUser, Exception> {
        return auth.currentUser?.let { CustomResult.Success(it) } ?: CustomResult.Failure(Exception("No user is currently signed in"))
    }
    
    override suspend fun getCurrentUserId(): String? = withContext(Dispatchers.IO) {
        auth.currentUser?.uid
    }

    override suspend fun signUp(
        email: String,
        password: String
    ): CustomResult<String, Exception> =
        withContext(Dispatchers.IO) {
            resultTry {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                authResult.user?.uid ?: throw Exception("Failed to create user: UID is null.")
            }
        }

    override suspend fun signIn(email: String, password: String): CustomResult<String, Exception> =
        withContext(Dispatchers.IO) {
            resultTry {
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                authResult.user?.uid ?: throw Exception("Failed to sign in: UID is null.")
            }
        }

    override suspend fun signOut(): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            auth.signOut()
            Unit
        }
    }

    private inline fun <T> resultTry(block: () -> T): CustomResult<T, Exception> {
        return try {
            CustomResult.Success(block())
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }
    
    override suspend fun requestPasswordResetCode(email: String): CustomResult<Unit, Exception> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
    
    override suspend fun sendEmailVerification(): CustomResult<Unit, Exception> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                currentUser.sendEmailVerification().await()
                CustomResult.Success(Unit)
            } else {
                CustomResult.Failure(Exception("No user is currently signed in"))
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
    
    override suspend fun checkEmailVerification(): CustomResult<Boolean, Exception> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // Reload the user to get the latest email verification status
                currentUser.reload().await()
                CustomResult.Success(currentUser.isEmailVerified)
            } else {
                CustomResult.Failure(Exception("No user is currently signed in"))
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}

