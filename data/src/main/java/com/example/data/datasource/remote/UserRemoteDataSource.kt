package com.example.data.datasource.remote


import com.example.core_common.result.CustomResult
import com.example.core_common.util.DateTimeUtil
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.example.data.model.remote.DMWrapperDTO
import com.example.data.model.remote.UserDTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.model.enum.UserStatus
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.Source
import javax.inject.Inject
import javax.inject.Singleton

interface UserRemoteDataSource : DefaultDatasource {

    /**
     * 주어진 이름(닉네임)과 정확히 일치하는 사용자 정보를 실시간 스트림으로 반환합니다.
     */
    fun findByNameStream(name: String): Flow<CustomResult<UserDTO, Exception>>

    /**
     * 주어진 이름(닉네임)을 포함하는 사용자 목록을 실시간 스트림으로 반환합니다.
     */
    fun findAllByNameStream(name: String, limit: Int = 10): Flow<CustomResult<List<UserDTO>, Exception>>


    /**
     * 특정 userId를 가진 사용자의 프로젝트 요약 정보(ProjectsWrapper)를 실시간으로 관찰합니다.
     * @param userId 관찰할 사용자의 ID
     * @return ProjectsWrapperDTO 목록을 담은 Flow
     */
    /**
     * 정확한 이름으로 단일 사용자를 실시간으로 가져옵니다.
     * Firestore에서 'name' 필드가 정확히 일치하는 사용자를 찾습니다.
     *
     * @param name 정확히 일치하는 사용자 이름
     * @return UserDTO를 담은 Flow, 실패 시 Exception (사용자를 찾지 못하거나 파싱 오류 발생 시 Failure)
     */
    fun getUserByExactNameStream(name: String): Flow<CustomResult<UserDTO, Exception>>

    /**
     * 특정 userId를 가진 사용자의 DM 요약 정보(DMWrapper)를 실시간으로 관찰합니다.
     * @param userId 관찰할 사용자의 ID
     * @return DMWrapperDTO 목록을 담은 Flow
     */
    fun getDmWrappersStream(userId: String): Flow<CustomResult<List<DMWrapperDTO>, Exception>>

    /**
     * 특정 사용자의 updatedAt 필드를 실시간으로 감지합니다.
     * @param userId 감지할 사용자의 ID
     * @return updatedAt 타임스탬프 값을 담은 Flow (updatedAt이 변경될 때마다 emit)
     */
    fun observeUserUpdatedAt(userId: String): Flow<CustomResult<Long, Exception>>

}

@Singleton
class UserRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : DefaultDatasourceImpl<UserDTO>(firestore, UserDTO::class.java), UserRemoteDataSource {

    /**
     * Creates a default UserDTO instance when standard deserialization fails.
     * Uses safe timestamp conversion to handle HashMap timestamp issues.
     */
    override fun createDefaultDto(documentId: String?, data: Map<String, Any?>?): UserDTO? {
        return try {
            if (data == null) return null
            
            UserDTO(
                id = documentId ?: "",
                email = data["email"] as? String ?: "",
                name = data["name"] as? String ?: "",
                consentTimeStamp = convertTimestampSafely(data["consentTimeStamp"]),
                memo = data["memo"] as? String,
                status = (data["status"] as? String)?.let { 
                    try { UserStatus.valueOf(it) } catch (e: Exception) { UserStatus.OFFLINE }
                } ?: UserStatus.OFFLINE,
                createdAt = convertTimestampSafely(data["createdAt"]) 
                    ?: java.util.Date(),
                updatedAt = convertTimestampSafely(data["updatedAt"]) 
                    ?: java.util.Date(),
                fcmToken = data["fcmToken"] as? String,
                accountStatus = (data["accountStatus"] as? String)?.let {
                    try { UserAccountStatus.valueOf(it) } catch (e: Exception) { UserAccountStatus.ACTIVE }
                } ?: UserAccountStatus.ACTIVE
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Safely converts Any value to Date, handling both Timestamp, Date, and HashMap cases.
     */
    private fun convertTimestampSafely(value: Any?): java.util.Date? {
        return try {
            when (value) {
                is java.util.Date -> value
                is Timestamp -> value.toDate()
                is HashMap<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    DateTimeUtil.hashMapToFirebaseTimestamp(value as HashMap<String, Any>)?.toDate()
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
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
        val query = collection
            .whereEqualTo(UserDTO.NAME, name)
            .limit(1)

        val listenerRegistration = query.addSnapshotListener { snapshots, error ->
            if (error != null) {
                trySend(CustomResult.Failure(error))
                return@addSnapshotListener
            }

            if (snapshots != null && !snapshots.isEmpty) {
                val document = snapshots.documents[0] // Get the first document due to limit(1)
                val userDTO = document.toDtoSafely()
                if (userDTO != null) {
                    // If UserDTO has @DocumentId annotation on a field for uid,
                    // toDtoSafely() might already populate it.
                    // If not, and UserDTO has a 'uid' field, explicitly set it.
                    // Assuming UserDTO is a data class or has a copy method and a 'uid' property.
                    val finalUserDTO = if (userDTO.id.isEmpty() && document.id.isNotEmpty())
                        userDTO.copy(id = document.id) else userDTO
                    trySend(CustomResult.Success(finalUserDTO))
                } else {
                    // This case (userDTO is null after toDtoSafely on an existing document) 
                    // means parsing failed for some reason.
                    trySend(CustomResult.Failure(Exception("Failed to parse user data for document: ${document.id}")))
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
        val query = collection
            .orderBy(UserDTO.NAME) // Order by the field you are querying
            .whereGreaterThanOrEqualTo(UserDTO.NAME, name) // Start at 'name'
            .whereLessThanOrEqualTo(UserDTO.NAME, name + "\uf8ff") // End at 'name' followed by a high Unicode character
            .limit(limit.toLong())

        val listenerRegistration = query.addSnapshotListener { snapshots, error ->
            if (error != null) {
                trySend(CustomResult.Failure(error)); return@addSnapshotListener
            }
            if (snapshots != null) {
                val list = snapshots.documents.mapNotNull { it.toDtoSafely() }
                trySend(CustomResult.Success(list))
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    override fun getUserByExactNameStream(name: String): Flow<CustomResult<UserDTO, Exception>> = callbackFlow {
        trySend(CustomResult.Loading) // Send initial loading state

        val query = collection
            .whereEqualTo(UserDTO.NAME, name)
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
                val userDto = document.toDtoSafely()
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

        val wrappersCollection = collection.document(userId)
            .collection(DMWrapperDTO.COLLECTION_NAME)

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

    override fun observeUserUpdatedAt(userId: String): Flow<CustomResult<Long, Exception>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(CustomResult.Failure(Exception("User ID cannot be empty")))
            awaitClose { }
            return@callbackFlow
        }

        val userDocRef = collection.document(userId)
        
        val listenerRegistration = userDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(CustomResult.Failure(error))
                return@addSnapshotListener
            }
            
            if (snapshot != null && snapshot.exists()) {
                val updatedAt = snapshot.get(AggregateRoot.KEY_UPDATED_AT)
                val timestamp = when (updatedAt) {
                    is com.google.firebase.Timestamp -> updatedAt.toDate().time
                    is java.util.Date -> updatedAt.time
                    else -> System.currentTimeMillis()
                }
                trySend(CustomResult.Success(timestamp))
            } else {
                trySend(CustomResult.Failure(Exception("User document not found: $userId")))
            }
        }
        
        awaitClose { listenerRegistration.remove() }
    }

}
