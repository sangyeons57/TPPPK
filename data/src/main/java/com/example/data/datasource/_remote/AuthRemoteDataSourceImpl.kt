
package com.example.data.datasource._remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRemoteDataSource {

    override fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(authStateListener)

        awaitClose { auth.removeAuthStateListener(authStateListener) }
    }

    override fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    override suspend fun signUp(email: String, password: String): Result<String> =
        withContext(Dispatchers.IO) {
            resultTry {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                authResult.user?.uid ?: throw Exception("Failed to create user: UID is null.")
            }
        }

    override suspend fun signIn(email: String, password: String): Result<String> =
        withContext(Dispatchers.IO) {
            resultTry {
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                authResult.user?.uid ?: throw Exception("Failed to sign in: UID is null.")
            }
        }

    override suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            auth.signOut()
            Unit
        }
    }

    private inline fun <T> resultTry(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Throwable) {
            if (e is java.util.concurrent.CancellationException) throw e
            Result.failure(e)
        }
    }
}

