package com.example.data.datasource.remote

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.domain.model.AggregateRoot
import com.example.domain.model.DTO
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.Instant

interface Datasource

interface DefaultDatasource<D, E> : Datasource where D : AggregateRoot, E : DTO {
    fun setCollection(collectionPath: CollectionPath): DefaultDatasource<D, E>

    fun observe(id: DocumentId): Flow<CustomResult<E, Exception>>
    fun observeAll(): Flow<CustomResult<List<E>, Exception>>
    fun observeNByUpdatedAt(
        n: Long,
        updatedAt: Instant,
        direction: Query.Direction = Query.Direction.DESCENDING
    ): Flow<CustomResult<List<E>, Exception>>

    suspend fun findById(
        id: DocumentId,
        source: Source = Source.DEFAULT,
    ): CustomResult<E, Exception>

    suspend fun findAll(source: Source = Source.DEFAULT): CustomResult<List<E>, Exception>
    suspend fun findNByUpdatedAt(
        n: Long,
        updatedAt: Instant,
        direction: Query.Direction = Query.Direction.DESCENDING
    ): CustomResult<List<E>, Exception>

    suspend fun create(dto: E): CustomResult<DocumentId, Exception>
    suspend fun update(id: DocumentId, data: Map<String, Any?>): CustomResult<DocumentId, Exception>
    suspend fun delete(id: DocumentId): CustomResult<Unit, Exception>
}


/**
 * Generic base implementation of [DefaultDatasource].
 *
 * 1. Holds a late-initialized Firestore [collection] reference.
 * 2. Provides default CRUD + observe implementations with unified [CustomResult] wrapping.
 * 3. Concrete datasources are responsible for calling [setCollection] (or overriding it) **before** using
 *    any of the CRUD methods.
 * 4. Provides safe deserialization with fallback support for each concrete DTO type.
 *
 * @param firestore Firestore instance injected from DI container.
 */
abstract class DefaultDatasourceImpl<D, E>(
    private val firestore: FirebaseFirestore,
    val clazz: Class<E>
) : DefaultDatasource<D, E> where D : AggregateRoot, E : DTO {

    /** Firestore collection reference – must be set via [setCollection] */
    lateinit var collection: CollectionReference
        private set

    /**
     * Default implementation: treat the passed segments as a single collection path.
     * Most concrete datasources will **override** this to build nested paths.
     */
    override fun setCollection(collectionPath: CollectionPath): DefaultDatasource<D, E> {
        collection = firestore.collection(collectionPath.value)
        Log.d("DefaultDatasourceImpl", collection.path)
        return this
    }

    // region —— Safety helper ——

    protected fun checkCollectionInitialized(methodName: String) {
        if (!this::collection.isInitialized) {
            throw IllegalStateException("$methodName was called before setCollection(). Make sure to set Firestore collection context first.")
        }
    }

    /**
     * Safely converts DocumentSnapshot to DTO with fallback to default creation.
     * Uses standard toObject() first, then falls back to createDefaultDto() if that fails.
     * 
     * @param snapshot The DocumentSnapshot to convert
     * @return Converted DTO or null if both standard conversion and fallback fail
     */
    protected fun DocumentSnapshot.toDtoSafely(): E? {
        return try {
            // First try standard Firestore toObject conversion
            this.toObject(clazz)
        } catch (e: Exception) {
            null
        }
    }

    // endregion

    // region —— CRUD & observe implementation ——

    override fun observe(id: DocumentId): Flow<CustomResult<E, Exception>> = callbackFlow {
        trySend(CustomResult.Initial)
        checkCollectionInitialized("observe")
        trySend(CustomResult.Loading)
        val listener = collection.document(id.value).addSnapshotListener{ snapshot, error ->
            if(error != null) {
                trySend(CustomResult.Failure(error))
                close(error)
                return@addSnapshotListener
            }
            if(snapshot != null && snapshot.exists()) {
                val dto = snapshot.toDtoSafely()
                if(dto != null) {
                    trySend(CustomResult.Success(dto))
                } else {
                    trySend(CustomResult.Failure(Exception("${clazz.simpleName} not found or failed to deserialize")))
                }
            } else {
                trySend(CustomResult.Failure(Exception("${clazz.simpleName} not found or failed to deserialize")))
            }
        }
        awaitClose { listener.remove() }
    }

    override fun observeAll(): Flow<CustomResult<List<E>, Exception>> = callbackFlow {
        trySend(CustomResult.Initial)
        checkCollectionInitialized("observeAll")

        // 초기 로딩 상태 전송
        trySend(CustomResult.Loading)
        val listener = collection.addSnapshotListener{ snapshot, error ->
            if(error != null) {
                Log.e("DefaultDatasourceImpl", "observeAll listener error: ${error.message}", error)
                trySend(CustomResult.Failure(error))
                close(error)
                return@addSnapshotListener
            }
            if(snapshot != null) {
                trySend(resultTry {
                    snapshot.documents.mapNotNull { it.toDtoSafely() }
                })
            } else {
                Log.w("DefaultDatasourceImpl", "observeAll received null snapshot")
                trySend(CustomResult.Success(emptyList()))
            }
        }

        awaitClose {
            Log.d("DefaultDatasourceImpl", "observeAll listener closed for collection: ${collection.path}")
            listener.remove()
        }
    }

    override fun observeNByUpdatedAt(
        n: Long,
        updatedAt: Instant,
        direction: Query.Direction
    ): Flow<CustomResult<List<E>, Exception>> = callbackFlow {
        trySend(CustomResult.Initial)
        checkCollectionInitialized("observeNByUpdatedAt")

        trySend(CustomResult.Loading)
        val listener = collection
            .whereLessThan(AggregateRoot.KEY_UPDATED_AT, updatedAt)
            .orderBy(AggregateRoot.KEY_UPDATED_AT, Query.Direction.DESCENDING)
            .orderBy(AggregateRoot.KEY_CREATED_AT, direction)
            .limit(n)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(CustomResult.Failure(error))
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    trySend(resultTry {
                        snapshot.documents.mapNotNull { it.toDtoSafely() }
                    })
                } else {
                    trySend(CustomResult.Success(emptyList()))
                }
            }

        awaitClose {
            Log.d(
                "DefaultDatasourceImpl",
                "observeNByUpdatedAt listener closed for collection: ${collection.path}"
            )
            listener.remove()
        }
    }


    override suspend fun findById(
        id: DocumentId,
        source: Source,
    ): CustomResult<E, Exception> = withContext(Dispatchers.IO) {
        checkCollectionInitialized("findById")
        resultTry {
            val snapshot = collection.document(id.value).get(source).await()
            snapshot.toDtoSafely() ?: throw Exception("${clazz.simpleName} not found or failed to deserialize")
        }
    }

    override suspend fun findAll(source: Source): CustomResult<List<E>, Exception> =
        withContext(Dispatchers.IO) {
        checkCollectionInitialized("findAll")
        resultTry {
            val snapshot = collection.get(source).await()
            snapshot.documents.mapNotNull { it.toDtoSafely() }
        }
    }

    override suspend fun findNByUpdatedAt(
        n: Long,
        updatedAt: Instant,
        direction: Query.Direction
    ): CustomResult<List<E>, Exception> = resultTry {
        checkCollectionInitialized("findNByUpdatedAt")

        val snapshot = collection
            .whereLessThan(AggregateRoot.KEY_UPDATED_AT, updatedAt)
            .orderBy(AggregateRoot.KEY_UPDATED_AT, Query.Direction.DESCENDING)
            .orderBy(AggregateRoot.KEY_CREATED_AT, direction)
            .limit(n)
            .get()
            .await()

        snapshot.documents.mapNotNull { it.toDtoSafely() }
    }

    override suspend fun create(dto: E): CustomResult<DocumentId, Exception> =
        withContext(Dispatchers.IO) {
        checkCollectionInitialized("create")
        resultTry {
            if(DocumentId.isAssigned(dto.id)) {
                collection.document(dto.id).set(dto).await()
                DocumentId(dto.id)
            } else {
                val ref = collection.add(dto).await()
                DocumentId(ref.id)
            }
        }
    }

    override suspend fun update(id: DocumentId, data: Map<String, Any?>): CustomResult<DocumentId, Exception> = withContext(Dispatchers.IO) {
        checkCollectionInitialized("update")
        resultTry {
            if (id.isNotAssigned()) throw IllegalArgumentException("ID cannot be empty when updating")
            // Ensure createdAt is never modified during update
            val dataWithTimestamp = data.toMutableMap().apply {
                // Remove any accidental createdAt field in update map
                remove(AggregateRoot.KEY_CREATED_AT)

                // Always set updatedAt to server time
                put(AggregateRoot.KEY_UPDATED_AT, FieldValue.serverTimestamp())
            }
            collection.document(id.value).update(dataWithTimestamp).await()
            id
        }
    }

    override suspend fun delete(id: DocumentId): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        checkCollectionInitialized("delete")
        resultTry {
            if (id.isNotAssigned()) throw IllegalArgumentException("ID cannot be empty when deleting")
            collection.document(id.value).delete().await()
            Unit
        }
    }
}
