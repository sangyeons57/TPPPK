
package com.example.data.datasource.remote

import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.example.data.model.remote.ProjectsWrapperDTO
import com.example.domain.model.vo.CollectionPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface ProjectsWrapperRemoteDataSource : DefaultDatasource {
    fun observeProjectsWrappers(userId: String): Flow<CustomResult<List<String>, Exception>>
    suspend fun addProjectToUser(userId: String, projectId: String, dto: ProjectsWrapperDTO): CustomResult<Unit, Exception>
    suspend fun removeProjectFromUser(userId: String, projectId: String): CustomResult<Unit, Exception>
}

@Singleton
class ProjectsWrapperRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DefaultDatasourceImpl<ProjectsWrapperDTO>(firestore, ProjectsWrapperDTO::class.java),
    ProjectsWrapperRemoteDataSource {

    override fun observeProjectsWrappers(userId: String): Flow<CustomResult<List<String>, Exception>> {
        setCollection(CollectionPath.userProjectWrappers(userId))
        return callbackFlow {
            val listener = collection.addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(CustomResult.Failure(error)); close(error); return@addSnapshotListener }
                val ids = snapshot?.documents?.map { it.id } ?: emptyList()
                trySend(CustomResult.Success(ids))
            }
            awaitClose { listener.remove() }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun addProjectToUser(userId: String, projectId: String, dto: ProjectsWrapperDTO): CustomResult<Unit, Exception> {
        setCollection(CollectionPath.userProjectWrappers(userId))
        return withContext(Dispatchers.IO) {
            resultTry {
                collection.document(projectId).set(dto).await(); Unit
            }
        }
    }

    override suspend fun removeProjectFromUser(userId: String, projectId: String): CustomResult<Unit, Exception> {
        setCollection(CollectionPath.userProjectWrappers(userId))
        return withContext(Dispatchers.IO) {
            resultTry {
                collection.document(projectId).delete().await(); Unit
            }
        }
    }
}

