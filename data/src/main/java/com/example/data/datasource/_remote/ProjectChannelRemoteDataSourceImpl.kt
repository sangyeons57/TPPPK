
package com.example.data.datasource._remote

import com.example.data.model._remote.ProjectChannelDTO
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.dataObjects
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectChannelRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ProjectChannelRemoteDataSource {

    companion object {
        private const val PROJECTS_COLLECTION = "projects"
        private const val CATEGORIES_COLLECTION = "categories"
        private const val PROJECT_CHANNELS_COLLECTION = "project_channels"
    }

    private fun getProjectChannelsCollection(projectId: String, categoryId: String) =
        firestore.collection(PROJECTS_COLLECTION).document(projectId)
            .collection(CATEGORIES_COLLECTION).document(categoryId)
            .collection(PROJECT_CHANNELS_COLLECTION)

    override fun observeProjectChannels(
        projectId: String,
        categoryId: String
    ): Flow<List<ProjectChannelDTO>> {
        return getProjectChannelsCollection(projectId, categoryId)
            .orderBy("channelName", Query.Direction.ASCENDING)
            .dataObjects()
    }

    override suspend fun addProjectChannel(
        projectId: String,
        categoryId: String,
        name: String,
        type: String
    ): Result<String> = withContext(Dispatchers.IO) {
        resultTry {
            val newChannel = ProjectChannelDTO(
                channelName = name,
                channelType = type
            )
            val docRef = getProjectChannelsCollection(projectId, categoryId).add(newChannel).await()
            docRef.id
        }
    }

    override suspend fun updateProjectChannel(
        projectId: String,
        categoryId: String,
        channelId: String,
        newName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            val updateData = mapOf(
                "channelName" to newName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            getProjectChannelsCollection(projectId, categoryId).document(channelId)
                .update(updateData).await()
            Unit
        }
    }

    override suspend fun deleteProjectChannel(
        projectId: String,
        categoryId: String,
        channelId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            // 경고: 이 작업은 채널 문서만 삭제합니다.
            // 이 채널에 속한 메시지, 태스크 등 하위 데이터는 자동으로 삭제되지 않으므로
            // Cloud Functions를 통한 연쇄 삭제(cascading delete) 구현이 권장됩니다.
            getProjectChannelsCollection(projectId, categoryId).document(channelId)
                .delete().await()
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

