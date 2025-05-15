package com.example.data.repository

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.Channel
import com.example.domain.model.channel.ProjectChannelRef
import com.example.domain.repository.ChannelRepository
import com.example.domain.repository.ProjectChannelRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import java.time.Instant

/**
 * 프로젝트 채널 참조를 관리하는 리포지토리 구현체입니다.
 */
class ProjectChannelRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val channelRepository: ChannelRepository
) : ProjectChannelRepository {

    companion object {
        private const val PROJECTS_COLLECTION = "projects"
        private const val CATEGORIES_COLLECTION = "categories"
        private const val CHANNEL_REFS_COLLECTION = "channelRefs"
        private const val DIRECT_CHANNEL_REFS_COLLECTION = "directChannelRefs"
    }

    /**
     * 프로젝트에 채널 참조를 추가합니다.
     * @param projectChannelRef 프로젝트 채널 참조 정보
     * @return 성공 시 생성된 참조, 실패 시 예외
     */
    override suspend fun addChannelToProject(projectChannelRef: ProjectChannelRef): Result<ProjectChannelRef> {
        return try {
            if (projectChannelRef.categoryId != null) {
                // 카테고리에 추가
                return addChannelToCategory(
                    projectId = projectChannelRef.projectId,
                    categoryId = projectChannelRef.categoryId.toString(),
                    channelId = projectChannelRef.channelId,
                    order = projectChannelRef.order
                )
            } else {
                // 직속 채널로 추가
                return addDirectChannelToProject(
                    projectId = projectChannelRef.projectId,
                    channelId = projectChannelRef.channelId,
                    order = projectChannelRef.order
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 카테고리에 채널 참조를 추가합니다.
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param channelId 채널 ID
     * @param order 순서 (기본값은 마지막 위치)
     * @return 성공 시 생성된 참조, 실패 시 예외
     */
    override suspend fun addChannelToCategory(
        projectId: String,
        categoryId: String,
        channelId: String,
        order: Int
    ): Result<ProjectChannelRef> {
        return try {
            // 먼저 카테고리가 존재하는지 확인
            val categoryDoc = firestore.collection(PROJECTS_COLLECTION)
                .document(projectId)
                .collection(CATEGORIES_COLLECTION)
                .document(categoryId)
                .get()
                .await()
            
            if (!categoryDoc.exists()) {
                return Result.failure(NoSuchElementException("Category not found: $categoryId in project: $projectId"))
            }
            
            // 채널 참조 생성
            val refDocRef = firestore.collection(PROJECTS_COLLECTION)
                .document(projectId)
                .collection(CATEGORIES_COLLECTION)
                .document(categoryId)
                .collection(CHANNEL_REFS_COLLECTION)
                .document()
            
            // 순서 설정
            val finalOrder = if (order >= 0) order else {
                // 마지막 순서 계산
                val lastRefQuery = firestore.collection(PROJECTS_COLLECTION)
                    .document(projectId)
                    .collection(CATEGORIES_COLLECTION)
                    .document(categoryId)
                    .collection(CHANNEL_REFS_COLLECTION)
                    .orderBy("order", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .await()
                
                if (lastRefQuery.isEmpty) 0 else {
                    (lastRefQuery.documents.first().getLong("order") ?: 0) + 1
                }
            }
            
            // 프로젝트 채널 참조 객체 생성
            val projectChannelRef = ProjectChannelRef(
                id = refDocRef.id,
                projectId = projectId,
                categoryId = categoryId,
                channelId = channelId,
                order = finalOrder.toInt(),
                createdAt = DateTimeUtil.nowInstant()
            )
            
            // Firestore에 저장
            refDocRef.set(projectChannelRef.toFirestore()).await()
            
            Result.success(projectChannelRef)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 프로젝트에 직속 채널 참조를 추가합니다.
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @param order 순서 (기본값은 마지막 위치)
     * @return 성공 시 생성된 참조, 실패 시 예외
     */
    override suspend fun addDirectChannelToProject(
        projectId: String,
        channelId: String,
        order: Int
    ): Result<ProjectChannelRef> {
        return try {
            // 프로젝트가 존재하는지 확인
            val projectDoc = firestore.collection(PROJECTS_COLLECTION)
                .document(projectId)
                .get()
                .await()
            
            if (!projectDoc.exists()) {
                return Result.failure(NoSuchElementException("Project not found: $projectId"))
            }
            
            // 채널 참조 생성
            val refDocRef = firestore.collection(PROJECTS_COLLECTION)
                .document(projectId)
                .collection(DIRECT_CHANNEL_REFS_COLLECTION)
                .document()
            
            // 순서 설정
            val finalOrder = if (order >= 0) order else {
                // 마지막 순서 계산
                val lastRefQuery = firestore.collection(PROJECTS_COLLECTION)
                    .document(projectId)
                    .collection(DIRECT_CHANNEL_REFS_COLLECTION)
                    .orderBy("order", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .await()
                
                if (lastRefQuery.isEmpty) 0 else {
                    (lastRefQuery.documents.first().getLong("order") ?: 0) + 1
                }
            }
            
            // 프로젝트 채널 참조 객체 생성
            val projectChannelRef = ProjectChannelRef(
                id = refDocRef.id,
                projectId = projectId,
                categoryId = null, // 직속 채널은 카테고리가 없음
                channelId = channelId,
                order = finalOrder.toInt(),
                createdAt = DateTimeUtil.nowInstant()
            )
            
            // Firestore에 저장
            refDocRef.set(projectChannelRef.toFirestore()).await()
            
            Result.success(projectChannelRef)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 프로젝트 채널 참조를 조회합니다.
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @return 성공 시 참조 정보, 실패 시 예외
     */
    override suspend fun getProjectChannelRef(projectId: String, channelId: String): Result<ProjectChannelRef> {
        return try {
            // 먼저 직속 채널에서 찾기
            val directQuery = firestore.collection(PROJECTS_COLLECTION)
                .document(projectId)
                .collection(DIRECT_CHANNEL_REFS_COLLECTION)
                .whereEqualTo("channelId", channelId)
                .get()
                .await()
            
            if (!directQuery.isEmpty) {
                val ref = directQuery.documents.first().toProjectChannelRef()
                return Result.success(ref)
            }
            
            // 카테고리 내 채널에서 찾기
            val categoriesQuery = firestore.collection(PROJECTS_COLLECTION)
                .document(projectId)
                .collection(CATEGORIES_COLLECTION)
                .get()
                .await()
            
            for (categoryDoc in categoriesQuery.documents) {
                val categoryId = categoryDoc.id
                
                val channelRefQuery = firestore.collection(PROJECTS_COLLECTION)
                    .document(projectId)
                    .collection(CATEGORIES_COLLECTION)
                    .document(categoryId)
                    .collection(CHANNEL_REFS_COLLECTION)
                    .whereEqualTo("channelId", channelId)
                    .get()
                    .await()
                
                if (!channelRefQuery.isEmpty) {
                    val ref = channelRefQuery.documents.first().toProjectChannelRef()
                    return Result.success(ref)
                }
            }
            
            Result.failure(NoSuchElementException("Channel reference not found for channel: $channelId in project: $projectId"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 프로젝트의 모든 채널 참조를 조회합니다.
     * @param projectId 프로젝트 ID
     * @return 성공 시 참조 목록, 실패 시 예외
     */
    override suspend fun getProjectChannelRefs(projectId: String): Result<List<ProjectChannelRef>> {
        return try {
            val allRefs = mutableListOf<ProjectChannelRef>()
            
            // 직속 채널 참조 가져오기
            val directResult = getDirectChannelRefs(projectId)
            if (directResult.isSuccess) {
                allRefs.addAll(directResult.getOrNull() ?: emptyList())
            }
            
            // 카테고리 별 채널 참조 가져오기
            val categoriesQuery = firestore.collection(PROJECTS_COLLECTION)
                .document(projectId)
                .collection(CATEGORIES_COLLECTION)
                .get()
                .await()
            
            for (categoryDoc in categoriesQuery.documents) {
                val categoryId = categoryDoc.id
                
                val categoryResult = getCategoryChannelRefs(projectId, categoryId)
                if (categoryResult.isSuccess) {
                    allRefs.addAll(categoryResult.getOrNull() ?: emptyList())
                }
            }
            
            Result.success(allRefs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 프로젝트의 모든 채널 참조를 실시간으로 구독합니다.
     * @param projectId 프로젝트 ID
     * @return 참조 목록 Flow
     */
    override fun getProjectChannelRefsStream(projectId: String): Flow<List<ProjectChannelRef>> {
        return callbackFlow {
            val refs = mutableListOf<ProjectChannelRef>()
            val listeners = mutableListOf<() -> Unit>()
            
            // 직속 채널 리스너
            val directListener = firestore.collection(PROJECTS_COLLECTION)
                .document(projectId)
                .collection(DIRECT_CHANNEL_REFS_COLLECTION)
                .orderBy("order")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // 하나의 에러가 전체 Flow를 취소하지 않도록 처리
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        // 기존 직속 채널 제거
                        refs.removeAll { it.categoryId == null }
                        
                        // 새 직속 채널 추가
                        val directRefs = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toProjectChannelRef()
                            } catch (e: Exception) {
                                null
                            }
                        }
                        refs.addAll(directRefs)
                        
                        // 전체 목록 전송
                        trySend(refs.toList())
                    }
                }
            
            listeners.add { directListener.remove() }
            
            // 카테고리 리스너
            val categoriesListener = firestore.collection(PROJECTS_COLLECTION)
                .document(projectId)
                .collection(CATEGORIES_COLLECTION)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    
                    // 현재 리스너 해제 (카테고리 변경 시 채널 리스너도 다시 설정)
                    listeners.filter { it != directListener::remove }.forEach { it() }
                    listeners.clear()
                    listeners.add { directListener.remove() }
                    
                    // 기존 카테고리 채널 제거
                    refs.removeAll { it.categoryId != null }
                    
                    // 각 카테고리별 리스너 설정
                    for (categoryDoc in snapshot.documents) {
                        val categoryId = categoryDoc.id
                        
                        val channelListener = firestore.collection(PROJECTS_COLLECTION)
                            .document(projectId)
                            .collection(CATEGORIES_COLLECTION)
                            .document(categoryId)
                            .collection(CHANNEL_REFS_COLLECTION)
                            .orderBy("order")
                            .addSnapshotListener { channelSnapshot, channelError ->
                                if (channelError != null || channelSnapshot == null) return@addSnapshotListener
                                
                                // 해당 카테고리의 채널 제거
                                refs.removeAll { it.categoryId == categoryId }
                                
                                // 새 채널 추가
                                val categoryRefs = channelSnapshot.documents.mapNotNull { doc ->
                                    try {
                                        doc.toProjectChannelRef()
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                refs.addAll(categoryRefs)
                                
                                // 전체 목록 전송
                                trySend(refs.toList())
                            }
                            
                        listeners.add { channelListener.remove() }
                    }
                }
            
            listeners.add { categoriesListener.remove() }
            
            awaitClose { listeners.forEach { it() } }
        }
    }

    /**
     * 카테고리의 채널 참조를 조회합니다.
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @return 성공 시 참조 목록, 실패 시 예외
     */
    override suspend fun getCategoryChannelRefs(projectId: String, categoryId: String): Result<List<ProjectChannelRef>> = try {
        val querySnapshot = firestore.collection(PROJECTS_COLLECTION)
            .document(projectId)
            .collection(CATEGORIES_COLLECTION)
            .document(categoryId)
            .collection(CHANNEL_REFS_COLLECTION)
            .orderBy("order")
            .get()
            .await()
            
        val refs = querySnapshot.documents.mapNotNull { doc ->
            try {
                doc.toProjectChannelRef()
            } catch (e: Exception) {
                null // 변환 실패한 항목은 무시
            }
        }
        
        Result.success(refs)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 카테고리의 채널 참조를 실시간으로 구독합니다.
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @return 참조 목록 Flow
     */
    override fun getCategoryChannelRefsStream(projectId: String, categoryId: String): Flow<List<ProjectChannelRef>> = callbackFlow {
        val listener = firestore.collection(PROJECTS_COLLECTION)
            .document(projectId)
            .collection(CATEGORIES_COLLECTION)
            .document(categoryId)
            .collection(CHANNEL_REFS_COLLECTION)
            .orderBy("order")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    try {
                        val refs = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toProjectChannelRef()
                            } catch (e: Exception) {
                                null // 변환 실패한 항목은 무시
                            }
                        }
                        trySend(refs)
                    } catch (e: Exception) {
                        close(e)
                    }
                }
            }
            
        awaitClose { listener.remove() }
    }

    /**
     * 프로젝트의 직속 채널 참조를 조회합니다.
     * @param projectId 프로젝트 ID
     * @return 성공 시 참조 목록, 실패 시 예외
     */
    override suspend fun getDirectChannelRefs(projectId: String): Result<List<ProjectChannelRef>> = try {
        val querySnapshot = firestore.collection(PROJECTS_COLLECTION)
            .document(projectId)
            .collection(DIRECT_CHANNEL_REFS_COLLECTION)
            .orderBy("order")
            .get()
            .await()
            
        val refs = querySnapshot.documents.mapNotNull { doc ->
            try {
                doc.toProjectChannelRef()
            } catch (e: Exception) {
                null // 변환 실패한 항목은 무시
            }
        }
        
        Result.success(refs)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 프로젝트의 직속 채널 참조를 실시간으로 구독합니다.
     * @param projectId 프로젝트 ID
     * @return 참조 목록 Flow
     */
    override fun getDirectChannelRefsStream(projectId: String): Flow<List<ProjectChannelRef>> = callbackFlow {
        val listener = firestore.collection(PROJECTS_COLLECTION)
            .document(projectId)
            .collection(DIRECT_CHANNEL_REFS_COLLECTION)
            .orderBy("order")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    try {
                        val refs = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toProjectChannelRef()
                            } catch (e: Exception) {
                                null // 변환 실패한 항목은 무시
                            }
                        }
                        trySend(refs)
                    } catch (e: Exception) {
                        close(e)
                    }
                }
            }
            
        awaitClose { listener.remove() }
    }

    /**
     * 프로젝트 채널 참조를 업데이트합니다 (순서 변경 등).
     * @param projectChannelRef 업데이트할 참조 정보
     * @return 성공 시 Unit, 실패 시 예외
     */
    override suspend fun updateProjectChannelRef(projectChannelRef: ProjectChannelRef): Result<Unit> {
        return try {
            if (projectChannelRef.categoryId != null) {
                // 카테고리 내 채널 참조 업데이트
                firestore.collection(PROJECTS_COLLECTION)
                    .document(projectChannelRef.projectId)
                    .collection(CATEGORIES_COLLECTION)
                    .document(projectChannelRef.categoryId.toString())
                    .collection(CHANNEL_REFS_COLLECTION)
                    .document(projectChannelRef.id)
                    .set(projectChannelRef.toFirestore())
                    .await()
            } else {
                // 직속 채널 참조 업데이트
                firestore.collection(PROJECTS_COLLECTION)
                    .document(projectChannelRef.projectId)
                    .collection(DIRECT_CHANNEL_REFS_COLLECTION)
                    .document(projectChannelRef.id)
                    .set(projectChannelRef.toFirestore())
                    .await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 프로젝트에서 채널 참조를 제거합니다.
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @return 성공 시 Unit, 실패 시 예외
     */
    override suspend fun removeChannelFromProject(projectId: String, channelId: String): Result<Unit> {
        return try {
            // 먼저 채널 참조 찾기
            val refResult = getProjectChannelRef(projectId, channelId)

            if (refResult.isFailure) {
                return Result.failure(refResult.exceptionOrNull() ?: Exception("Failed to find channel reference"))
            }

            val ref = refResult.getOrNull()!!

            if (ref.categoryId != null) {
                // 카테고리 내 채널 참조 삭제
                firestore.collection(PROJECTS_COLLECTION)
                    .document(projectId)
                    .collection(CATEGORIES_COLLECTION)
                    .document(ref.categoryId.toString())
                    .collection(CHANNEL_REFS_COLLECTION)
                    .document(ref.id)
                    .delete()
                    .await()
            } else {
                // 직속 채널 참조 삭제
                firestore.collection(PROJECTS_COLLECTION)
                    .document(projectId)
                    .collection(DIRECT_CHANNEL_REFS_COLLECTION)
                    .document(ref.id)
                    .delete()
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 채널 참조의 순서를 변경합니다.
     * @param projectChannelRef 참조 정보
     * @param newOrder 새 순서
     * @return 성공 시 Unit, 실패 시 예외
     */
    override suspend fun reorderChannelRef(projectChannelRef: ProjectChannelRef, newOrder: Int): Result<Unit> {
        return try {
            val updatedRef = projectChannelRef.copy(order = newOrder)
            updateProjectChannelRef(updatedRef)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 프로젝트의 채널 목록을 조회합니다 (참조가 아닌 실제 채널 객체).
     * @param projectId 프로젝트 ID
     * @return 성공 시 채널 목록, 실패 시 예외
     */
    override suspend fun getProjectChannels(projectId: String): Result<List<Channel>> {
        return try {
            // 프로젝트의 모든 채널 참조 가져오기
            val refsResult = getProjectChannelRefs(projectId)

            if (refsResult.isFailure) {
                return Result.failure(refsResult.exceptionOrNull() ?: Exception("Failed to get channel references"))
            }

            val refs = refsResult.getOrNull()!!

            // 각 참조에 해당하는 실제 채널 정보 가져오기
            val channels = mutableListOf<Channel>()

            for (ref in refs) {
                val channelResult = channelRepository.getChannel(ref.channelId)
                if (channelResult.isSuccess) {
                    channelResult.getOrNull()?.let { channels.add(it) }
                }
            }

            Result.success(channels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 프로젝트의 채널 목록을 실시간으로 구독합니다 (참조가 아닌 실제 채널 객체).
     * @param projectId 프로젝트 ID
     * @return 채널 목록 Flow
     */
    override fun getProjectChannelsStream(projectId: String): Flow<List<Channel>> {
        return getProjectChannelRefsStream(projectId).map { refs ->
            val channels = mutableListOf<Channel>()
            
            // 각 참조 ID에 해당하는 채널 정보 가져오기
            refs.forEach { ref ->
                try {
                    val channelResult = channelRepository.getChannel(ref.channelId)
                    if (channelResult.isSuccess) {
                        channelResult.getOrNull()?.let { channels.add(it) }
                    }
                } catch (e: Exception) {
                    // 개별 채널 로드 실패는 무시하고 계속 진행
                }
            }
            
            channels
        }
    }
    
    /**
     * 채널 ID가 이미 프로젝트에 존재하는지 확인합니다.
     * 직속 채널 또는 카테고리 내 채널을 모두 검사합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @return 성공 시 존재 여부, 실패 시 예외
     */
    override suspend fun isChannelInProject(projectId: String, channelId: String): Result<Boolean> {
        return try {
            val refResult = getProjectChannelRef(projectId, channelId)
            Result.success(refResult.isSuccess)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 채널 참조가 속한 카테고리 ID를 조회합니다.
     * 직속 채널인 경우 null을 반환합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @return 성공 시 카테고리 ID(또는 null), 실패 시 예외
     */
    override suspend fun getChannelCategory(projectId: String, channelId: String): Result<String?> {
        return try {
            val refResult = getProjectChannelRef(projectId, channelId)
            
            if (refResult.isFailure) {
                return Result.failure(refResult.exceptionOrNull() ?: Exception("Failed to find channel reference"))
            }
            
            val ref = refResult.getOrNull()!!
            Result.success(ref.categoryId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 채널을 한 카테고리에서 다른 카테고리로 이동합니다.
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @param targetCategoryId 대상 카테고리 ID (null인 경우 직속 채널로 이동)
     * @param order 새 순서 (-1인 경우 마지막 위치)
     * @return 성공 시 업데이트된 참조, 실패 시 예외
     */
    override suspend fun moveChannelToCategory(
        projectId: String, 
        channelId: String, 
        targetCategoryId: String?, 
        order: Int
    ): Result<ProjectChannelRef> {
        return try {
            // 기존 참조 정보 가져오기
            val refResult = getProjectChannelRef(projectId, channelId)
            
            if (refResult.isFailure) {
                return Result.failure(refResult.exceptionOrNull() ?: Exception("Failed to find channel reference"))
            }
            
            val oldRef = refResult.getOrNull()!!
            
            // 같은 위치로 이동하려고 하면 바로 반환
            if (oldRef.categoryId == targetCategoryId) {
                return if (order >= 0 && oldRef.order != order) {
                    // 순서만 변경
                    reorderChannelRef(oldRef, order)
                    val updatedRef = oldRef.copy(order = order)
                    Result.success(updatedRef)
                } else {
                    // 변경 없음
                    Result.success(oldRef)
                }
            }
            
            // 기존 참조 삭제
            val removeResult = removeChannelFromProject(projectId, channelId)
            
            if (removeResult.isFailure) {
                return Result.failure(removeResult.exceptionOrNull() ?: Exception("Failed to remove existing reference"))
            }
            
            // 새 위치에 참조 추가
            val addResult = if (targetCategoryId != null) {
                addChannelToCategory(projectId, targetCategoryId, channelId, order)
            } else {
                addDirectChannelToProject(projectId, channelId, order)
            }
            
            addResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 특정 채널 ID에 해당하는 모든 프로젝트 참조를 찾습니다.
     * 
     * @param channelId 채널 ID
     * @return 성공 시 해당 채널을 포함하는 모든 프로젝트 참조 목록, 실패 시 예외
     */
    override suspend fun findProjectsContainingChannel(channelId: String): Result<List<ProjectChannelRef>> {
        return try {
            val allRefs = mutableListOf<ProjectChannelRef>()
            
            // 1. 모든 프로젝트의 직속 채널 참조 검색
            val directRefsQuery = firestore.collectionGroup(DIRECT_CHANNEL_REFS_COLLECTION)
                .whereEqualTo("channelId", channelId)
                .get()
                .await()
            
            for (doc in directRefsQuery.documents) {
                try {
                    allRefs.add(doc.toProjectChannelRef())
                } catch (e: Exception) {
                    // 개별 문서 변환 오류는 무시하고 계속 진행
                }
            }
            
            // 2. 모든 카테고리의 채널 참조 검색
            val categoryRefsQuery = firestore.collectionGroup(CHANNEL_REFS_COLLECTION)
                .whereEqualTo("channelId", channelId)
                .get()
                .await()
            
            for (doc in categoryRefsQuery.documents) {
                try {
                    allRefs.add(doc.toProjectChannelRef())
                } catch (e: Exception) {
                    // 개별 문서 변환 오류는 무시하고 계속 진행
                }
            }
            
            Result.success(allRefs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 여러 채널 참조의 순서를 일괄 업데이트합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID (null인 경우 직속 채널)
     * @param reorderedRefs 새로운 순서가 적용된 참조 목록
     * @return 성공 시 Unit, 실패 시 예외
     */
    override suspend fun batchReorderChannelRefs(
        projectId: String,
        categoryId: String?,
        reorderedRefs: List<ProjectChannelRef>
    ): Result<Unit> {
        return try {
            val batch = firestore.batch()
            
            if (categoryId != null) {
                // 카테고리 내 채널 참조 일괄 업데이트
                for (ref in reorderedRefs) {
                    val refDocRef = firestore.collection(PROJECTS_COLLECTION)
                        .document(projectId)
                        .collection(CATEGORIES_COLLECTION)
                        .document(categoryId)
                        .collection(CHANNEL_REFS_COLLECTION)
                        .document(ref.id)
                    
                    // 순서만 업데이트
                    batch.update(refDocRef, "order", ref.order)
                }
            } else {
                // 직속 채널 참조 일괄 업데이트
                for (ref in reorderedRefs) {
                    val refDocRef = firestore.collection(PROJECTS_COLLECTION)
                        .document(projectId)
                        .collection(DIRECT_CHANNEL_REFS_COLLECTION)
                        .document(ref.id)
                    
                    // 순서만 업데이트
                    batch.update(refDocRef, "order", ref.order)
                }
            }
            
            // 배치 커밋
            batch.commit().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 확장 함수: Document를 ProjectChannelRef 모델로 변환
    private fun com.google.firebase.firestore.DocumentSnapshot.toProjectChannelRef(): ProjectChannelRef {
        val id = getString("id") ?: id
        val projectId = getString("projectId") ?: throw IllegalStateException("Project ID is required")
        val categoryId = getString("categoryId")
        val channelId = getString("channelId") ?: throw IllegalStateException("Channel ID is required")
        val order = getLong("order")?.toInt() ?: 0
        val createdAt = DateTimeUtil.firebaseTimestampToInstant(getTimestamp("createdAt")) ?: DateTimeUtil.nowInstant()
        
        return ProjectChannelRef(
            id = id,
            projectId = projectId,
            categoryId = categoryId,
            channelId = channelId,
            order = order,
            createdAt = createdAt
        )
    }
    
    // 확장 함수: ProjectChannelRef 모델을 Firestore Map으로 변환
    private fun ProjectChannelRef.toFirestore(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        result["id"] = id
        result["projectId"] = projectId
        categoryId?.let { result["categoryId"] = it }
        result["channelId"] = channelId
        result["order"] = order
        result["createdAt"] = DateTimeUtil.instantToFirebaseTimestamp(createdAt)!!
        return result
    }
} 