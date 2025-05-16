package com.example.data.datasource.remote.projectstructure

import com.example.core_common.constants.FirestoreConstants.Collections
import com.example.core_common.constants.FirestoreConstants.CategoryFields
import com.example.core_common.constants.FirestoreConstants.MemberFields
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.Category
import com.example.domain.model.Channel
import com.example.domain.model.ChannelType
import com.example.domain.model.ProjectStructure
import com.example.domain.repository.ChannelRepository
import com.example.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.withContext
import kotlin.Result
import com.example.data.model.mapper.CategoryMapper
import com.example.data.model.mapper.ChannelMapper
import com.example.domain.model.ChannelMode
import com.example.core_common.dispatcher.DispatcherProvider
import com.example.domain.model.channel.ProjectSpecificData
import com.google.firebase.firestore.FieldValue
import java.util.UUID

/**
 * 프로젝트 구조(카테고리, 채널) 관련 원격 데이터 소스 구현체
 * Firebase Firestore를 사용하여 프로젝트 구조 관련 기능을 구현합니다.
 * 
 * @param firestore Firebase Firestore 인스턴스
 * @param auth Firebase Auth 인스턴스
 * @param channelRepository ChannelRepository 인스턴스
 * @param userRepository UserRepository 인스턴스
 */
@Singleton
class ProjectStructureRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val channelRepository: ChannelRepository,
    private val userRepository: UserRepository,
    private val categoryMapper: CategoryMapper,
    private val channelMapper: ChannelMapper,
    private val dispatcherProvider: DispatcherProvider
) : ProjectStructureRemoteDataSource {

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("사용자가 로그인되어 있지 않습니다.")
    
    // --- Project Structure Fetching ---
    
    /**
     * 프로젝트 구조(카테고리 및 직속 채널 포함)를 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 프로젝트 구조 결과
     */
    override suspend fun getProjectStructure(projectId: String): Result<ProjectStructure> = withContext(dispatcherProvider.io) {
        try {
            val categoriesCollection = firestore.collection(Collections.PROJECTS)
                .document(projectId)
                .collection(Collections.CATEGORIES)
                .orderBy(CategoryFields.ORDER)
            val categoriesSnapshot = categoriesCollection.get().await()
            val categories = categoriesSnapshot.documents.mapNotNull { categoryDoc ->
                Category(
                    id = categoryDoc.id,
                    projectId = projectId,
                    name = categoryDoc.getString(CategoryFields.NAME) ?: "Category",
                    order = categoryDoc.getLong(CategoryFields.ORDER)?.toInt() ?: 0,
                    channels = emptyList(),
                    createdAt = DateTimeUtil.firebaseTimestampToInstant(categoryDoc.getTimestamp(CategoryFields.CREATED_AT)),
                    updatedAt = DateTimeUtil.firebaseTimestampToInstant(categoryDoc.getTimestamp(CategoryFields.UPDATED_AT)),
                    createdBy = categoryDoc.getString(CategoryFields.CREATED_BY),
                    updatedBy = categoryDoc.getString(CategoryFields.UPDATED_BY)
                )
            }.sortedBy { it.order }

            val currentUserMemberDoc = firestore.collection(Collections.PROJECTS)
                .document(projectId)
                .collection(Collections.MEMBERS)
                .document(currentUserId)
                .get()
                .await()
                
            val accessibleChannelIds = if (currentUserMemberDoc.exists()) {
                (currentUserMemberDoc.get(MemberFields.CHANNEL_IDS) as? List<String>) ?: emptyList()
            } else { emptyList() }

            val channels = accessibleChannelIds.mapNotNull { channelId ->
                channelRepository.getChannel(channelId).getOrNull()
            }

            val categoriesWithChannels = categories.map { category ->
                val categoryChannels = channels.filter { channel ->
                    val projData = channel.projectSpecificData
                    projData?.projectId == projectId && projData.categoryId == category.id
                }.sortedBy { it.projectSpecificData?.order ?: 0 }
                category.copy(channels = categoryChannels)
            }
            
            val directChannels = channels.filter { channel ->
                val projData = channel.projectSpecificData
                projData?.projectId == projectId && projData.categoryId == null
            }.sortedBy { it.projectSpecificData?.order ?: 0 }
            
            Result.success(ProjectStructure(categories = categoriesWithChannels, directChannels = directChannels))
        } catch (e: Exception) {
            Log.e("ProjStructRemoteDS", "Error getting project structure for $projectId", e)
            Result.failure(e)
        }
    }

    /**
     * 프로젝트 구조(카테고리 및 직속 채널 포함) 실시간 스트림을 가져옵니다.
     * TODO: This implementation needs significant work to combine streams from categories, 
     *       channels within categories, and direct channels efficiently.
     * @param projectId 프로젝트 ID
     * @return 프로젝트 구조 Flow
     */
    override fun getProjectStructureStream(projectId: String): Flow<ProjectStructure> {
        return callbackFlow {
            Log.w("ProjStructRemoteDS", "getProjectStructureStream for $projectId is simplified.")
            val job = CoroutineScope(dispatcherProvider.io).launch {
                getProjectStructure(projectId).onSuccess { send(it) }.onFailure { close(it) }
            }
            awaitClose { job.cancel() }
        }
    }
    
    // --- Category Management ---
    
    /**
     * 새 카테고리를 생성합니다.
     * @param projectId 프로젝트 ID
     * @param name 카테고리 이름
     * @return 생성된 카테고리 결과
     */
    override suspend fun createCategory(projectId: String, name: String): Result<Category> = withContext(dispatcherProvider.io) {
        try {
            val categoriesColRef = firestore.collection(Collections.PROJECTS).document(projectId)
                .collection(Collections.CATEGORIES)
            
            val categoriesQuery = categoriesColRef.orderBy(CategoryFields.ORDER, com.google.firebase.firestore.Query.Direction.DESCENDING).limit(1)
            val categoriesSnapshot = categoriesQuery.get().await()
            val nextOrder = if (categoriesSnapshot.isEmpty) 0 else {
                (categoriesSnapshot.documents.firstOrNull()?.getLong(CategoryFields.ORDER) ?: -1) + 1
            }
            
            val nowTimestamp = DateTimeUtil.instantToFirebaseTimestamp(DateTimeUtil.nowInstant())
            val categoryData = mapOf(
                CategoryFields.NAME to name,
                CategoryFields.ORDER to nextOrder,
                CategoryFields.CREATED_AT to nowTimestamp,
                CategoryFields.CREATED_BY to currentUserId,
                CategoryFields.UPDATED_AT to nowTimestamp,
                CategoryFields.UPDATED_BY to currentUserId
            )
            
            val newCategoryRef = categoriesColRef.document()
            newCategoryRef.set(categoryData).await()
            
            Result.success(Category(
                id = newCategoryRef.id,
                projectId = projectId,
                name = name,
                order = nextOrder.toInt(),
                channels = emptyList(),
                createdAt = DateTimeUtil.firebaseTimestampToInstant(nowTimestamp),
                updatedAt = DateTimeUtil.firebaseTimestampToInstant(nowTimestamp),
                createdBy = currentUserId,
                updatedBy = currentUserId
            ))
        } catch (e: Exception) {
            Log.e("ProjStructRemoteDS", "Error creating category $name in project $projectId", e)
            Result.failure(e)
        }
    }

    /**
     * 카테고리 정보를 수정합니다.
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param newName 새 카테고리 이름 (nullable)
     * @param newOrder 새 카테고리 순서 (nullable)
     * @return 작업 성공 여부
     */
    override suspend fun updateCategory(
        projectId: String, 
        categoryId: String, 
        newName: String?, 
        newOrder: Int?
    ): Result<Unit> = withContext(dispatcherProvider.io) {
        try {
            val categoryRef = firestore.collection(Collections.PROJECTS).document(projectId)
                .collection(Collections.CATEGORIES).document(categoryId)
            
            if (!categoryRef.get().await().exists()) {
                return@withContext Result.failure(NoSuchElementException("Category with ID $categoryId not found in project $projectId"))
            }
            
            val updateData = mutableMapOf<String, Any>(
                CategoryFields.UPDATED_AT to FieldValue.serverTimestamp(),
                CategoryFields.UPDATED_BY to currentUserId
            )
            newName?.let { updateData[CategoryFields.NAME] = it }
            newOrder?.let { updateData[CategoryFields.ORDER] = it }

            categoryRef.update(updateData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProjStructRemoteDS", "Error updating category $categoryId in project $projectId", e)
            Result.failure(e)
        }
    }
    
    /**
     * 카테고리를 삭제합니다.
     * TODO: Should also delete nested channels within the category.
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @return 작업 성공 여부
     */
    override suspend fun deleteCategory(projectId: String, categoryId: String): Result<Unit> = withContext(dispatcherProvider.io) {
        try {
            val categoryRef = firestore.collection(Collections.PROJECTS).document(projectId)
                .collection(Collections.CATEGORIES).document(categoryId)
            
            if (!categoryRef.get().await().exists()) {
                 return@withContext Result.failure(NoSuchElementException("Category with ID $categoryId not found in project $projectId for deletion"))
            }
            categoryRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProjStructRemoteDS", "Error deleting category $categoryId in project $projectId", e)
            Result.failure(e)
        }
    }
    
    // --- Category Channel Management ---
    
    /**
     * 카테고리 내 새 채널을 생성합니다.
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param name 채널 이름
     * @param channelMode 채널 모드
     * @param order 채널 순서
     * @return 생성된 채널 결과
     */
    override suspend fun createCategoryChannel(
        projectId: String,
        categoryId: String,
        name: String,
        channelMode: ChannelMode,
        order: Int?
    ): Result<Channel> = withContext(dispatcherProvider.io) {
        try {
            val currentUserId = this@ProjectStructureRemoteDataSourceImpl.currentUserId

            // Basic validation (optional)
            val projectDocRef = firestore.collection(Collections.PROJECTS).document(projectId)
            if (!projectDocRef.get().await().exists()) {
                return@withContext Result.failure(IllegalArgumentException("Project $projectId does not exist"))
            }
            val categoryDocRef = projectDocRef.collection(Collections.CATEGORIES).document(categoryId)
            if (!categoryDocRef.get().await().exists()) {
                return@withContext Result.failure(IllegalArgumentException("Category $categoryId does not exist"))
            }
            
            // Construct the Channel object
            val newChannelId = UUID.randomUUID().toString()
            val now = DateTimeUtil.nowInstant()
            val channel = Channel(
                id = newChannelId,
                name = name,
                description = null,
                type = ChannelType.CATEGORY,
                projectSpecificData = ProjectSpecificData(
                projectId = projectId,
                    categoryId = categoryId,
                    order = order ?: 0,
                    channelMode = channelMode
                ),
                dmSpecificData = null,
                lastMessagePreview = null,
                lastMessageTimestamp = null,
                createdAt = now,
                createdBy = currentUserId,
                updatedAt = now
            )

            // Use the standard createChannel repository method
            val result = channelRepository.createChannel(channel)
            
            if (result.isFailure) {
                return@withContext Result.failure(
                    result.exceptionOrNull() ?: IllegalStateException("Failed to create category channel using repository")
                )
            }
            
            // Add channel ID to member's list (keep this logic)
            val memberDocRef = projectDocRef.collection(Collections.MEMBERS).document(currentUserId)
            kotlin.runCatching { memberDocRef.update(MemberFields.CHANNEL_IDS, FieldValue.arrayUnion(newChannelId)).await() }

            // Return the created channel from the repository result
            Result.success(result.getOrThrow())

        } catch (e: Exception) {
            Log.e("ProjStructRemoteDS", "Error creating category channel in project $projectId category $categoryId", e)
            Result.failure(e)
        }
    }

    // --- Project-Level Channel Management ---

    /**
     * 프로젝트 직속 채널을 생성합니다.
     * @param projectId 프로젝트 ID
     * @param name 채널 이름
     * @param channelMode 채널 모드
     * @param order 채널 순서
     * @return 생성된 채널 결과
     */
    override suspend fun createProjectChannel(
        projectId: String,
        name: String,
        channelMode: ChannelMode,
        order: Int?
    ): Result<Channel> = withContext(dispatcherProvider.io) {
        try {
            val currentUserId = this@ProjectStructureRemoteDataSourceImpl.currentUserId
                
            val projectDocRef = firestore.collection(Collections.PROJECTS).document(projectId)
            if (!projectDocRef.get().await().exists()) {
                return@withContext Result.failure(IllegalArgumentException("Project $projectId does not exist"))
            }

            // Construct the Channel object
            val newChannelId = UUID.randomUUID().toString()
            val now = DateTimeUtil.nowInstant()
            val channel = Channel(
                id = newChannelId,
                name = name,
                description = null,
                type = ChannelType.PROJECT,
                projectSpecificData = ProjectSpecificData(
                    projectId = projectId,
                    categoryId = null,
                    order = order ?: 0,
                    channelMode = channelMode
                ),
                dmSpecificData = null,
                lastMessagePreview = null,
                lastMessageTimestamp = null,
                createdAt = now,
                createdBy = currentUserId,
                updatedAt = now
            )

            // Use the standard createChannel repository method
            val result = channelRepository.createChannel(channel)

            if (result.isFailure) {
                return@withContext Result.failure(
                    result.exceptionOrNull() ?: IllegalStateException("Failed to create project channel using repository")
                )
            }

            // Add channel ID to member's list (keep this logic)
            val memberDocRef = projectDocRef.collection(Collections.MEMBERS).document(currentUserId)
            kotlin.runCatching { memberDocRef.update(MemberFields.CHANNEL_IDS, FieldValue.arrayUnion(newChannelId)).await() }

            Result.success(result.getOrThrow())

        } catch (e: Exception) {
            Log.e("ProjStructRemoteDS", "Error creating project channel in project $projectId", e)
            Result.failure(e)
        }
    }

    override suspend fun getProjectChannelDetails(projectId: String, channelId: String): Result<Channel> =
            channelRepository.getChannel(channelId)

    override suspend fun deleteProjectChannel(projectId: String, channelId: String): Result<Unit> =
        channelRepository.deleteChannel(channelId)

    /**
     * 카테고리 내 채널 정보를 수정합니다.
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param channelId 채널 ID
     * @param newName 새 채널 이름
     * @param newChannelMode 새 채널 모드
     * @return 작업 성공 여부
     */
    override suspend fun updateCategoryChannel(
        projectId: String, 
        categoryId: String, 
        channelId: String, 
        newName: String, 
        newChannelMode: ChannelMode
    ): Result<Unit> = withContext(dispatcherProvider.io) {
        try {
            val channelResult = channelRepository.getChannel(channelId)
            if (channelResult.isFailure) {
                return@withContext Result.failure(IllegalArgumentException("존재하지 않는 채널입니다."))
            }
            
            val channel = channelResult.getOrThrow()
            
            val updatedProjectSpecificData = channel.projectSpecificData?.copy(
                categoryId = categoryId,
                channelMode = newChannelMode
            ) ?: ProjectSpecificData(
                projectId = projectId,
                categoryId = categoryId,
                order = channel.projectSpecificData?.order ?: 0,
                channelMode = newChannelMode
            )
            
            val updatedChannel = channel.copy(
                name = newName,
                type = ChannelType.CATEGORY,
                projectSpecificData = updatedProjectSpecificData,
                updatedAt = DateTimeUtil.nowInstant()
            )
            
            channelRepository.updateChannel(updatedChannel)
        } catch (e: Exception) {
            Log.e("ProjStructRemoteDS", "Error updating category channel", e)
            Result.failure(e)
        }
    }
    
    /**
     * 프로젝트 직속 채널 정보를 수정합니다.
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @param newName 새 채널 이름
     * @param newChannelMode 새 채널 모드
     * @return 작업 성공 여부
     */
    override suspend fun updateProjectChannel(
        projectId: String, 
        channelId: String, 
        newName: String, 
        newChannelMode: ChannelMode
    ): Result<Unit> = withContext(dispatcherProvider.io) {
        try {
            val channelResult = channelRepository.getChannel(channelId)
            if (channelResult.isFailure) {
                return@withContext Result.failure(IllegalArgumentException("존재하지 않는 채널입니다."))
            }
            
            val channel = channelResult.getOrThrow()
            
            val updatedProjectSpecificData = channel.projectSpecificData?.copy(
                categoryId = null,
                channelMode = newChannelMode
            ) ?: ProjectSpecificData(
                projectId = projectId,
                categoryId = null,
                order = channel.projectSpecificData?.order ?: 0,
                channelMode = newChannelMode
            )
            
            val updatedChannel = channel.copy(
                name = newName,
                type = ChannelType.PROJECT,
                projectSpecificData = updatedProjectSpecificData,
                updatedAt = DateTimeUtil.nowInstant()
            )
            
            channelRepository.updateChannel(updatedChannel)
        } catch (e: Exception) {
            Log.e("ProjStructRemoteDS", "Error updating project channel", e)
            Result.failure(e)
        }
    }

    // TODO: 실제 구현 필요
    override suspend fun getCategoryDetails(projectId: String, categoryId: String): Result<Category> {
        return Result.failure(NotImplementedError("getCategoryDetails not implemented"))
    }

    // TODO: 실제 구현 필요
    override suspend fun getCategoryChannelDetails(projectId: String, categoryId: String, channelId: String): Result<Channel> {
        return Result.failure(NotImplementedError("getCategoryChannelDetails not implemented"))
    }

    // TODO: 실제 구현 필요
    override suspend fun deleteCategoryChannel(projectId: String, categoryId: String, channelId: String): Result<Unit> {
        return Result.failure(NotImplementedError("deleteCategoryChannel not implemented"))
    }
} 