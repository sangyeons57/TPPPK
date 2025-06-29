package com.example.data.datasource.remote.special

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.model.DTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.vo.DocumentId
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

interface Datasource {

}

interface DefaultDatasource : Datasource {
    fun setCollection(vararg ids: String) : DefaultDatasource

    fun observe(id: DocumentId): Flow<CustomResult<DTO, Exception>>
    fun observeAll(): Flow<CustomResult<List<DTO>, Exception>>
    suspend fun findById(
        id: DocumentId,
        source: Source = Source.DEFAULT,
    ): CustomResult<DTO, Exception>
    suspend fun findAll(source: Source = Source.DEFAULT): CustomResult<List<DTO>, Exception>

    suspend fun create(dto: DTO): CustomResult<DocumentId, Exception>
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
abstract class DefaultDatasourceImpl <Dto> (
    private val firestore: FirebaseFirestore,
    val clazz: Class<Dto>
) : DefaultDatasource where Dto: DTO {

    /** Firestore collection reference – must be set via [setCollection] */
    lateinit var collection: CollectionReference
        private set

    /**
     * Default implementation: treat the passed segments as a single collection path.
     * Most concrete datasources will **override** this to build nested paths.
     */
    override fun setCollection(vararg ids: String): DefaultDatasource {
        require(ids.isNotEmpty()) { "At least one path segment must be provided to setCollection()." }
        collection = firestore.collection(ids.joinToString("/"))
        Log.d("DefaultDatasourceImpl", collection.path)
        return this
    }

    // region —— Safety helper ——

    protected fun checkCollectionInitialized(methodName: String) {
        if (!this::collection.isInitialized) {
            throw IllegalStateException("$methodName was called before setCollection(). Make sure to set Firestore collection context first.")
        }
    }

    // endregion

    // region —— Safe deserialization support ——

    /**
     * Creates a default DTO instance when deserialization fails.
     * Concrete datasources should override this to provide appropriate default values.
     * 
     * @param documentId The document ID (if available)
     * @param data The raw Firestore data map (if available)
     * @return A default DTO instance or null if no default can be created
     */
    protected open fun createDefaultDto(documentId: String? = null, data: Map<String, Any?>? = null): Dto? {
        return null // Base implementation returns null - subclasses should override
    }

    /**
     * Safely converts DocumentSnapshot to DTO with fallback to default creation.
     * Uses standard toObject() first, then falls back to createDefaultDto() if that fails.
     * 
     * @param snapshot The DocumentSnapshot to convert
     * @return Converted DTO or null if both standard conversion and fallback fail
     */
    protected fun DocumentSnapshot.toDtoSafely(): Dto? {
        return try {
            // First try standard Firestore toObject conversion
            this.toObject(clazz)
        } catch (e: Exception) {
            // If standard conversion fails, try fallback with default creation
            try {
                createDefaultDto(this.id, this.data)
            } catch (fallbackError: Exception) {
                // If everything fails, return null
                null
            }
        }
    }

    // endregion

    // region —— CRUD & observe implementation ——

    override fun observe(id: DocumentId): Flow<CustomResult<DTO, Exception>> = callbackFlow{
        checkCollectionInitialized("observe")
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
            }
        }
        awaitClose { listener.remove() }
    }

    override fun observeAll(): Flow<CustomResult<List<DTO>, Exception>> = callbackFlow{
        checkCollectionInitialized("observeAll")
        val listener = collection.addSnapshotListener{ snapshot, error ->
            if(error != null) {
                trySend(CustomResult.Failure(error))
                close(error)
                return@addSnapshotListener
            }
            if(snapshot != null) {
                val dtos = snapshot.documents.mapNotNull { it.toDtoSafely() }
                trySend(CustomResult.Success(dtos))
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun findById(
        id: DocumentId,
        source: Source,
    ): CustomResult<DTO, Exception> = withContext(Dispatchers.IO) {
        checkCollectionInitialized("findById")
        resultTry {
            val snapshot = collection.document(id.value).get(source).await()
            snapshot.toDtoSafely() ?: throw Exception("${clazz.simpleName} not found or failed to deserialize")
        }
    }

    override suspend fun findAll(source: Source): CustomResult<List<DTO>, Exception> = withContext(Dispatchers.IO) {
        checkCollectionInitialized("findAll")
        resultTry {
            val snapshot = collection.get(source).await()
            snapshot.documents.mapNotNull { it.toDtoSafely() }
        }
    }

    override suspend fun create(dto: DTO): CustomResult<DocumentId, Exception> = withContext(Dispatchers.IO) {
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
            // Automatically add updatedAt timestamp to all updates
            val dataWithTimestamp = data.toMutableMap().apply {
                put(AggregateRoot.KEY_UPDATED_AT, FieldValue.serverTimestamp())
            }
            collection.document(id.value).update(dataWithTimestamp).await()
            id
        }
    }

    override suspend fun delete(id: DocumentId): CustomResult<Unit, Exception> = withContext(
        Dispatchers.IO) {
        checkCollectionInitialized("delete")
        resultTry {
            if (id.isNotAssigned()) throw IllegalArgumentException("ID cannot be empty when deleting")
            collection.document(id.value).delete().await()
            Unit
        }
    }

}
