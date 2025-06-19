package com.example.data.datasource.remote.special

import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.domain.event.AggregateRoot
import com.example.domain.model.vo.DocumentId
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

interface DefaultDatasource<T: Any> {
    fun setCollection(vararg ids: String) : DefaultDatasource<T>

    fun observe(id: DocumentId): Flow<CustomResult<T, Exception>>
    suspend fun findById(
        id: DocumentId,
        source: Source = Source.DEFAULT,
    ): CustomResult<T, Exception>

    suspend fun create(dto: Any): CustomResult<DocumentId, Exception>
    suspend fun update(aggregateRoot: AggregateRoot): CustomResult<DocumentId, Exception>
    suspend fun delete(id: DocumentId): CustomResult<Unit, Exception>
}


/**
 * Generic base implementation of [DefaultDatasource].
 *
 * 1. Holds a late-initialized Firestore [collection] reference.
 * 2. Provides default CRUD + observe implementations with unified [CustomResult] wrapping.
 * 3. Concrete datasources are responsible for calling [setCollection] (or overriding it) **before** using
 *    any of the CRUD methods.
 *
 * @param firestore Firestore instance injected from DI container.
 */
abstract class  DefaultDatasourceImpl<T: Any>(
    private val firestore: FirebaseFirestore,
    private val clazz: Class<T>
) : DefaultDatasource<T> {

    /** Firestore collection reference – must be set via [setCollection] */
    lateinit var collection: CollectionReference
        private set

    /**
     * Default implementation: treat the passed segments as a single collection path.
     * Most concrete datasources will **override** this to build nested paths.
     */
    override fun setCollection(vararg ids: String): DefaultDatasource<T> {
        FirestoreConstants
        require(ids.isNotEmpty()) { "At least one path segment must be provided to setCollection()." }
        collection = firestore.collection(ids.joinToString("/"))
        return this
    }

    // region —— Safety helper ——

    protected fun checkCollectionInitialized(methodName: String) {
        if (!this::collection.isInitialized) {
            throw IllegalStateException("$methodName was called before setCollection(). Make sure to set Firestore collection context first.")
        }
    }

    // endregion

    // region —— CRUD & observe implementation ——

    override fun observe(id: DocumentId): Flow<CustomResult<T, Exception>> = callbackFlow{
        checkCollectionInitialized("observe")
        val listener = collection.document(id.value).addSnapshotListener{ snapshot, error ->
            if(error != null) {
                trySend(CustomResult.Failure(error))
                close(error)
                return@addSnapshotListener
            }
            if(snapshot != null && snapshot.exists()) {
                val dto = snapshot.toObject(clazz)
                if(dto != null) {
                    trySend(CustomResult.Success(dto))
                } else {
                    trySend(CustomResult.Failure(Exception("${clazz::javaClass.name} not found")))
                }
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun findById(
        id: DocumentId,
        source: Source,
    ): CustomResult<T, Exception> = withContext(Dispatchers.IO) {
        checkCollectionInitialized("findById")
        resultTry {
            val snapshot = collection.document(id.value).get(source).await()
            snapshot.toObject(clazz) ?: throw Exception("${clazz::javaClass.name} not found")
        }
    }

    override suspend fun create(dto: Any): CustomResult<DocumentId, Exception> = withContext(Dispatchers.IO) {
        checkCollectionInitialized("create")
        resultTry {
            val ref = collection.add(dto).await()
            DocumentId(ref.id)
        }
    }

    override suspend fun update(aggregateRoot: AggregateRoot): CustomResult<DocumentId, Exception> = withContext(
        Dispatchers.IO) {
        checkCollectionInitialized("update")
        resultTry {
            if (!aggregateRoot.id.isAssigned()) throw IllegalArgumentException("ID cannot be empty when updating")
            collection.document(aggregateRoot.id.value).update(aggregateRoot.getChangedFields()).await()
            aggregateRoot.id
        }
    }

    override suspend fun delete(id: DocumentId): CustomResult<Unit, Exception> = withContext(
        Dispatchers.IO) {
        checkCollectionInitialized("delete")
        resultTry {
            if (!id.isAssigned()) throw IllegalArgumentException("ID cannot be empty when deleting")
            collection.document(id.value).delete().await()
            Unit
        }
    }

}
