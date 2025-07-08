
package com.example.data.datasource.remote

import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.example.data.model.remote.ProjectDTO
import com.example.domain.model.vo.project.ProjectStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface ProjectRemoteDataSource : DefaultDatasource {
    suspend fun findActiveProjects(source: Source = Source.DEFAULT): CustomResult<List<ProjectDTO>, Exception>
    fun observeActiveProjects(): Flow<CustomResult<List<ProjectDTO>, Exception>>
}

@Singleton
class ProjectRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DefaultDatasourceImpl<ProjectDTO>(firestore, ProjectDTO::class.java),
    ProjectRemoteDataSource {

    override suspend fun findActiveProjects(source: Source): CustomResult<List<ProjectDTO>, Exception> = withContext(Dispatchers.IO) {
        checkCollectionInitialized("findActiveProjects")
        resultTry {
            val snapshot = collection
                .whereNotEqualTo(ProjectDTO.STATUS, ProjectStatus.DELETED.value)
                .get(source)
                .await()
            snapshot.documents.mapNotNull { it.toDtoSafely() }
        }
    }

    override fun observeActiveProjects(): Flow<CustomResult<List<ProjectDTO>, Exception>> = callbackFlow {
        checkCollectionInitialized("observeActiveProjects")
        val listener = collection
            .whereNotEqualTo(ProjectDTO.STATUS, ProjectStatus.DELETED.value)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(CustomResult.Failure(error))
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val dtos = snapshot.documents.mapNotNull { it.toDtoSafely() }
                    trySend(CustomResult.Success(dtos))
                }
            }
        awaitClose { listener.remove() }
    }
}

