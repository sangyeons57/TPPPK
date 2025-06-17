package com.example.data.datasource.remote

import android.net.Uri
import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.model.remote.UserDTO
import com.example.data.model.remote.ProjectsWrapperDTO
import com.example.data.model.remote.DMWrapperDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : UserRemoteDataSource {


    private val usersCollection = firestore.collection(FirestoreConstants.Collections.USERS)
    
    private fun getCurrentUserIdOrThrow(): String {
        return auth.currentUser?.uid ?: throw Exception("User not logged in.")
    }


    override fun observeUser(userId: String): Flow<CustomResult<UserDTO, Exception>> = callbackFlow {
        val listenerRegistration = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(CustomResult.Failure(error))
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(UserDTO::class.java)
                    if (user != null) {
                        trySend(CustomResult.Success(user))
                    } else {
                        trySend(CustomResult.Failure(Exception("Failed to parse user data.")))
                    }
                } else {
                     trySend(CustomResult.Failure(Exception("User document does not exist.")))
                }
            }
        awaitClose { listenerRegistration.remove() }
    }


    /**
     * Searches for a single user by their exact name and returns a Flow that emits the UserDTO.
     * The Flow will emit updates if the user's data changes in Firestore.
     * Emits [com.example.data.util.CustomResult.Success] with [com.example.data.dto.UserDTO] if a user is found.
     * Emits [com.example.data.util.CustomResult.Error] with [java.util.NoSuchElementException] if no user is found.
     * Emits [com.example.data.util.CustomResult.Error] with the underlying exception if a Firestore error occurs.
     *
     * @param name The exact name of the user to search for.
     * @return A Flow emitting the search result.
     */
    override fun findByNameStream(name: String): Flow<CustomResult<UserDTO, Exception>> = callbackFlow {
        trySend(CustomResult.Loading)
        val query = usersCollection
            .whereEqualTo(FirestoreConstants.Users.NAME, name)
            .limit(1)

        val listenerRegistration = query.addSnapshotListener { snapshots, error ->
            if (error != null) {
                trySend(CustomResult.Failure(error))
                return@addSnapshotListener
            }

            if (snapshots != null && !snapshots.isEmpty) {
                val document = snapshots.documents[0] // Get the first document due to limit(1)
                try {
                    val userDTO = document.toObject(UserDTO::class.java)
                    if (userDTO != null) {
                        // If UserDTO has @DocumentId annotation on a field for uid,
                        // toObject() might already populate it.
                        // If not, and UserDTO has a 'uid' field, explicitly set it.
                        // Assuming UserDTO is a data class or has a copy method and a 'uid' property.
                        val finalUserDTO = if (userDTO.uid.isEmpty() && document.id.isNotEmpty()) userDTO.copy(uid = document.id) else userDTO
                        trySend(CustomResult.Success(finalUserDTO))
                    } else {
                        // This case (userDTO is null after toObject on an existing document) is less common
                        // but good to handle. It means parsing failed for some reason.
                        trySend(CustomResult.Failure(Exception("Failed to parse user data for document: ${document.id}")))
                    }
                } catch (parseException: Exception) {
                    // This catches errors during document.toObject()
                    trySend(CustomResult.Failure(parseException))
                }
            } else {
                // Snapshots is null or empty, meaning no user found with that exact name.
                trySend(CustomResult.Failure(NoSuchElementException("User not found with name: $name")))
            }
        }
        // This is crucial: unregister the listener when the Flow is no longer collected.
        awaitClose { listenerRegistration.remove() }
    }

    // Returns multiple users containing name substring
    override fun findAllByNameStream(name: String, limit: Int): Flow<CustomResult<List<UserDTO>, Exception>> = callbackFlow {
        trySend(CustomResult.Loading)
        // Firestore does not support case-insensitive searches directly or partial text search with 'contains' like SQL LIKE.
        // The common workaround is to query for a range, which works for 'starts-with' type queries.
        // For 'contains', you'd typically need a more advanced search solution (e.g., Algolia, Elasticsearch) or client-side filtering.
        // This implementation provides a 'starts-with' search.
        val query = usersCollection
            .orderBy(FirestoreConstants.Users.NAME) // Order by the field you are querying
            .whereGreaterThanOrEqualTo(FirestoreConstants.Users.NAME, name) // Start at 'name'
            .whereLessThanOrEqualTo(FirestoreConstants.Users.NAME, name + "\uf8ff") // End at 'name' followed by a high Unicode character
            .limit(limit.toLong())

        val listenerRegistration = query.addSnapshotListener { snapshots, error ->
            if (error != null) {
                trySend(CustomResult.Failure(error)); return@addSnapshotListener
            }
            if (snapshots != null) {
                val list = snapshots.documents.mapNotNull { it.toObject(UserDTO::class.java) }
                trySend(CustomResult.Success(list))
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    // Upserts user document (set with merge=true)
    override suspend fun updateUser(userDto: UserDTO): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = userDto.uid
            usersCollection.document(uid).set(userDto, SetOptions.merge()).await()
            Unit
        }
    }

    // Deletes user document
    override suspend fun deleteUser(uid: String): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            if (uid.isEmpty()) throw IllegalArgumentException("User ID cannot be empty.")
            usersCollection.document(uid).delete().await()
            Unit
        }
    }

    override fun getUserByExactNameStream(name: String): Flow<CustomResult<UserDTO, Exception>> = callbackFlow {
        trySend(CustomResult.Loading) // Send initial loading state

        val query = usersCollection
            .whereEqualTo(FirestoreConstants.Users.NAME, name)
            .limit(1)

        val listenerRegistration = query.addSnapshotListener { snapshots, error ->
            if (error != null) {
                trySend(CustomResult.Failure(error))
                close(error) // Close the flow on error
                return@addSnapshotListener
            }

            if (snapshots != null && !snapshots.isEmpty) {
                // Even with limit(1), snapshots.documents is a list. Get the first.
                val document = snapshots.documents[0]
                val userDto = document.toObject(UserDTO::class.java)
                if (userDto != null) {
                    trySend(CustomResult.Success(userDto))
                } else {
                    // Document exists but couldn't be parsed
                    trySend(CustomResult.Failure(Exception("Failed to parse user data for name: $name")))
                }
            } else {
                // No document found with that exact name
                trySend(CustomResult.Failure(Exception("User not found with name: $name")))
            }
        }
        awaitClose { listenerRegistration.remove() }
    }


    override fun getDmWrappersStream(userId: String): Flow<CustomResult<List<DMWrapperDTO>, Exception>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(CustomResult.Success(emptyList()))
            awaitClose { }
            return@callbackFlow
        }

        val wrappersCollection = usersCollection.document(userId)
            .collection(FirestoreConstants.Users.DMWrappers.COLLECTION_NAME)

        val listenerRegistration = wrappersCollection.addSnapshotListener { snapshots, error ->
            if (error != null) {
                trySend(CustomResult.Failure(error))
                close(error)
                return@addSnapshotListener
            }
            if (snapshots != null) {
                val wrappers = snapshots.toObjects(DMWrapperDTO::class.java)
                trySend(CustomResult.Success(wrappers))
            } else {
                trySend(CustomResult.Failure(Exception("DM wrappers snapshot was null.")))
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun fetchUserByIdServer(userId: String): CustomResult<UserDTO, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val document = usersCollection.document(userId).get(Source.SERVER).await()
            document.toObject(UserDTO::class.java) ?: throw Exception("User data could not be parsed.")
        }
    }

}
