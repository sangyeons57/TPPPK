package com.example.data.datasource.remote.projectstructure

import com.example.core_common.constants.FirestoreConstants.Collections
import com.example.core_common.constants.FirestoreConstants.CategoryFields
import com.example.core_common.constants.FirestoreConstants.ChannelFields
import com.example.core_common.constants.FirestoreConstants.CommonFields
import com.example.domain.model.Category
import com.example.domain.model.Channel
import com.example.domain.model.ChannelType
import com.example.domain.model.ProjectStructure
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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

/**
 * 프로젝트 구조(카테고리, 채널) 관련 원격 데이터 소스 구현체
 * Firebase Firestore를 사용하여 프로젝트 구조 관련 기능을 구현합니다.
 * 
 * @param firestore Firebase Firestore 인스턴스
 * @param auth Firebase Auth 인스턴스
 */
@Singleton
class ProjectStructureRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ProjectStructureRemoteDataSource {

    // 현재 사용자 ID를 가져오는 헬퍼 함수
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("사용자가 로그인되어 있지 않습니다.")
    
    /**
     * 프로젝트 구조를 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 구조(카테고리, 채널 목록) 결과
     */
    override suspend fun getProjectStructure(projectId: String): Result<ProjectStructure> = try {
        // 카테고리 목록 가져오기
        val categoriesCollection = firestore.collection(Collections.PROJECTS).document(projectId)
            .collection(Collections.CATEGORIES)
            .orderBy(CategoryFields.ORDER)
        
        val categoriesSnapshot = categoriesCollection.get().await()
        
        val categories = mutableListOf<Category>()
        val channels = mutableListOf<Channel>()
        
        for (categoryDoc in categoriesSnapshot.documents) {
            val categoryId = categoryDoc.id
            val categoryName = categoryDoc.getString(CommonFields.NAME) ?: "카테고리"
            val categoryOrder = categoryDoc.getLong(CategoryFields.ORDER)?.toInt() ?: 0
            
            // 카테고리 객체 생성
            val category = Category(
                id = categoryId,
                projectId = projectId,
                name = categoryName,
                order = categoryOrder
            )
            
            categories.add(category)
            
            // 각 카테고리의 채널 가져오기
            val channelsCollection = categoryDoc.reference.collection(Collections.CHANNELS)
                .orderBy(ChannelFields.ORDER)
            
            val channelsSnapshot = channelsCollection.get().await()
            
            for (channelDoc in channelsSnapshot.documents) {
                val channelId = channelDoc.id
                val channelName = channelDoc.getString(CommonFields.NAME) ?: "채널"
                val channelTypeStr = channelDoc.getString(ChannelFields.TYPE) ?: "TEXT"
                val channelOrder = channelDoc.getLong(ChannelFields.ORDER)?.toInt() ?: 0
                
                // 채널 타입 문자열을 enum으로 변환
                val channelType = when (channelTypeStr.uppercase()) {
                    "VOICE" -> ChannelType.VOICE
                    else -> ChannelType.TEXT
                }
                
                // 채널 객체 생성
                val channel = Channel(
                    id = channelId,
                    categoryId = categoryId,
                    projectId = projectId,
                    name = channelName,
                    type = channelType,
                    order = channelOrder
                )
                
                channels.add(channel)
            }
        }
        
        Result.success(ProjectStructure(categories, channels))
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * 프로젝트 구조(카테고리, 채널) 정보의 실시간 스트림을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 프로젝트 구조 Flow
     */
    override fun getProjectStructureStream(projectId: String): Flow<ProjectStructure> = callbackFlow {
        // 프로젝트의 카테고리 컬렉션 참조
        val categoriesCollection = firestore.collection(Collections.PROJECTS).document(projectId)
            .collection(Collections.CATEGORIES)
            .orderBy(CategoryFields.ORDER)
        
        // 카테고리 스냅샷 리스너 설정
        val subscription = categoriesCollection.addSnapshotListener { categoriesSnapshot, error ->
            if (error != null) {
                // 에러 발생 시 빈 구조 전송
                trySend(ProjectStructure(emptyList(), emptyList()))
                return@addSnapshotListener
            }
            
            if (categoriesSnapshot == null) {
                trySend(ProjectStructure(emptyList(), emptyList()))
                return@addSnapshotListener
            }
            
            // 카테고리를 처리하여 객체 생성
            val categories = categoriesSnapshot.documents.map { categoryDoc ->
                val categoryId = categoryDoc.id
                val categoryName = categoryDoc.getString(CommonFields.NAME) ?: "카테고리"
                val categoryOrder = categoryDoc.getLong(CategoryFields.ORDER)?.toInt() ?: 0
                
                Category(
                    id = categoryId,
                    projectId = projectId,
                    name = categoryName,
                    order = categoryOrder
                )
            }
            
            // 각 카테고리의 채널 정보를 따로 로드
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val channels = mutableListOf<Channel>()
                    
                    // 각 카테고리별로 채널 정보 가져오기
                    for (category in categories) {
                        val channelsCollection = firestore.collection(Collections.PROJECTS)
                            .document(projectId)
                            .collection(Collections.CATEGORIES)
                            .document(category.id)
                            .collection(Collections.CHANNELS)
                            .orderBy(ChannelFields.ORDER)
                        
                        val channelsSnapshot = channelsCollection.get().await()
                        
                        for (channelDoc in channelsSnapshot.documents) {
                            val channelId = channelDoc.id
                            val channelName = channelDoc.getString(CommonFields.NAME) ?: "채널"
                            val channelTypeStr = channelDoc.getString(ChannelFields.TYPE) ?: "TEXT"
                            val channelOrder = channelDoc.getLong(ChannelFields.ORDER)?.toInt() ?: 0
                            
                            // 채널 타입 문자열을 enum으로 변환
                            val channelType = when (channelTypeStr.uppercase()) {
                                "VOICE" -> ChannelType.VOICE
                                else -> ChannelType.TEXT
                            }
                            
                            // 채널 객체 생성
                            val channel = Channel(
                                id = channelId,
                                categoryId = category.id,
                                projectId = projectId,
                                name = channelName,
                                type = channelType,
                                order = channelOrder
                            )
                            
                            channels.add(channel)
                        }
                    }
                    
                    // 프로젝트 구조 객체 생성 및 전송
                    trySend(ProjectStructure(categories, channels))
                } catch (e: Exception) {
                    trySend(ProjectStructure(categories, emptyList()))
                }
            }
        }
        
        // Flow 취소 시 리스너 제거
        awaitClose { subscription.remove() }
    }
    
    /**
     * 새 카테고리를 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param name 카테고리 이름
     * @return 생성된 카테고리 결과
     */
    override suspend fun createCategory(projectId: String, name: String): Result<Category> {
        try {
            // 프로젝트 존재 확인
            val projectDoc = firestore.collection(Collections.PROJECTS).document(projectId)
                .get().await()
            
            if (!projectDoc.exists()) {
                return Result.failure(IllegalArgumentException("존재하지 않는 프로젝트입니다."))
            }
            
            // 마지막 카테고리 순서 확인
            val lastCategoryQuery = firestore.collection(Collections.PROJECTS).document(projectId)
                .collection(Collections.CATEGORIES)
                .orderBy(CategoryFields.ORDER, com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get().await()
            
            val nextOrder = if (lastCategoryQuery.isEmpty) 0 else {
                val lastOrder = lastCategoryQuery.documents.first().getLong(CategoryFields.ORDER)?.toInt() ?: -1
                lastOrder + 1
            }
            
            // 카테고리 데이터 생성
            val categoryData = hashMapOf(
                CommonFields.NAME to name,
                CategoryFields.ORDER to nextOrder,
                CommonFields.CREATED_AT to FieldValue.serverTimestamp(),
                CategoryFields.CREATED_BY to currentUserId
            )
            
            // 새 카테고리 문서 생성
            val newCategoryRef = firestore.collection(Collections.PROJECTS).document(projectId)
                .collection(Collections.CATEGORIES)
                .document()
            
            newCategoryRef.set(categoryData).await()
            
            // 생성된 카테고리 객체 반환
            val category = Category(
                id = newCategoryRef.id,
                projectId = projectId,
                name = name,
                order = nextOrder
            )
            
            return Result.success(category)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    /**
     * 새 채널을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param name 채널 이름
     * @param type 채널 타입
     * @return 생성된 채널 결과
     */
    override suspend fun createChannel(
        projectId: String,
        categoryId: String,
        name: String,
        type: ChannelType
    ): Result<Channel> {
        try {
            // 카테고리 존재 확인
            val categoryRef = firestore.collection(Collections.PROJECTS).document(projectId)
                .collection(Collections.CATEGORIES).document(categoryId)
            
            val categoryDoc = categoryRef.get().await()
            
            if (!categoryDoc.exists()) {
                return Result.failure(IllegalArgumentException("존재하지 않는 카테고리입니다."))
            }
            
            // 마지막 채널 순서 확인
            val lastChannelQuery = categoryRef.collection(Collections.CHANNELS)
                .orderBy(ChannelFields.ORDER, com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get().await()
            
            val nextOrder = if (lastChannelQuery.isEmpty) 0 else {
                val lastOrder = lastChannelQuery.documents.first().getLong(ChannelFields.ORDER)?.toInt() ?: -1
                lastOrder + 1
            }
            
            // 채널 타입을 문자열로 변환
            val typeString = type.name
            
            // 채널 데이터 생성
            val channelData = hashMapOf(
                CommonFields.NAME to name,
                ChannelFields.TYPE to typeString,
                ChannelFields.ORDER to nextOrder,
                CommonFields.CREATED_AT to FieldValue.serverTimestamp(),
                ChannelFields.CREATED_BY to currentUserId
            )
            
            // 새 채널 문서 생성
            val newChannelRef = categoryRef.collection(Collections.CHANNELS).document()
            
            newChannelRef.set(channelData).await()
            
            // 생성된 채널 객체 반환
            val channel = Channel(
                id = newChannelRef.id,
                categoryId = categoryId,
                projectId = projectId,
                name = name,
                type = type,
                order = nextOrder
            )
            
            return Result.success(channel)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    /**
     * 카테고리 상세 정보를 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @return 카테고리 상세 정보 결과
     */
    override suspend fun getCategoryDetails(
        projectId: String,
        categoryId: String
    ): Result<Category> = try {
        // 카테고리 문서 가져오기
        val categoryDoc = firestore.collection(Collections.PROJECTS).document(projectId)
            .collection(Collections.CATEGORIES).document(categoryId)
            .get().await()
        
        if (!categoryDoc.exists()) {
            Result.failure(IllegalArgumentException("존재하지 않는 카테고리입니다."))
        } else {
            val name = categoryDoc.getString(CommonFields.NAME) ?: "카테고리"
            val order = categoryDoc.getLong(CategoryFields.ORDER)?.toInt() ?: 0
            
            val category = Category(
                id = categoryId,
                projectId = projectId,
                name = name,
                order = order
            )
            
            Result.success(category)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * 카테고리 정보를 수정합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param newName 새 카테고리 이름
     * @return 작업 성공 여부
     */
    override suspend fun updateCategory(
        projectId: String,
        categoryId: String,
        newName: String
    ): Result<Unit> = try {
        // 카테고리 문서 참조
        val categoryRef = firestore.collection(Collections.PROJECTS).document(projectId)
            .collection(Collections.CATEGORIES).document(categoryId)
        
        // 카테고리 존재 확인
        val categoryDoc = categoryRef.get().await()
        
        if (!categoryDoc.exists()) {
            Result.failure(IllegalArgumentException("존재하지 않는 카테고리입니다."))
        } else {
            // 카테고리 이름 업데이트
            val updateData = hashMapOf(
                CommonFields.NAME to newName,
                CommonFields.UPDATED_AT to FieldValue.serverTimestamp(),
                CategoryFields.UPDATED_BY to currentUserId
            )
            
            categoryRef.update(updateData as Map<String, Any>).await()
            
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * 카테고리를 삭제합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @return 작업 성공 여부
     */
    override suspend fun deleteCategory(
        projectId: String,
        categoryId: String
    ): Result<Unit> = try {
        // 카테고리 문서 참조
        val categoryRef = firestore.collection(Collections.PROJECTS).document(projectId)
            .collection(Collections.CATEGORIES).document(categoryId)
        
        // 카테고리 존재 확인
        val categoryDoc = categoryRef.get().await()
        
        if (!categoryDoc.exists()) {
            Result.failure(IllegalArgumentException("존재하지 않는 카테고리입니다."))
        } else {
            // 카테고리 내의 모든 채널 가져오기
            val channelsSnapshot = categoryRef.collection(Collections.CHANNELS).get().await()
            
            // 트랜잭션으로 모든 채널과 카테고리 삭제
            firestore.runTransaction { transaction ->
                // 모든 채널 삭제
                for (channelDoc in channelsSnapshot.documents) {
                    transaction.delete(channelDoc.reference)
                }
                
                // 카테고리 삭제
                transaction.delete(categoryRef)
            }.await()
            
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * 채널 상세 정보를 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param channelId 채널 ID
     * @return 채널 상세 정보 결과
     */
    override suspend fun getChannelDetails(
        projectId: String,
        categoryId: String,
        channelId: String
    ): Result<Channel> = try {
        // 채널 문서 가져오기
        val channelDoc = firestore.collection(Collections.PROJECTS).document(projectId)
            .collection(Collections.CATEGORIES).document(categoryId)
            .collection(Collections.CHANNELS).document(channelId)
            .get().await()
        
        if (!channelDoc.exists()) {
            Result.failure(IllegalArgumentException("존재하지 않는 채널입니다."))
        } else {
            val name = channelDoc.getString(CommonFields.NAME) ?: "채널"
            val typeStr = channelDoc.getString(ChannelFields.TYPE) ?: "TEXT"
            val order = channelDoc.getLong(ChannelFields.ORDER)?.toInt() ?: 0
            
            // 채널 타입 문자열을 enum으로 변환
            val type = when (typeStr.uppercase()) {
                "VOICE" -> ChannelType.VOICE
                else -> ChannelType.TEXT
            }
            
            val channel = Channel(
                id = channelId,
                categoryId = categoryId,
                projectId = projectId,
                name = name,
                type = type,
                order = order
            )
            
            Result.success(channel)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * 채널 정보를 수정합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param channelId 채널 ID
     * @param newName 새 채널 이름
     * @param newType 새 채널 타입
     * @return 작업 성공 여부
     */
    override suspend fun updateChannel(
        projectId: String,
        categoryId: String,
        channelId: String,
        newName: String,
        newType: ChannelType
    ): Result<Unit> = try {
        // 채널 문서 참조
        val channelRef = firestore.collection(Collections.PROJECTS).document(projectId)
            .collection(Collections.CATEGORIES).document(categoryId)
            .collection(Collections.CHANNELS).document(channelId)
        
        // 채널 존재 확인
        val channelDoc = channelRef.get().await()
        
        if (!channelDoc.exists()) {
            Result.failure(IllegalArgumentException("존재하지 않는 채널입니다."))
        } else {
            // 채널 정보 업데이트
            val updateData = hashMapOf(
                CommonFields.NAME to newName,
                ChannelFields.TYPE to newType.name,
                CommonFields.UPDATED_AT to FieldValue.serverTimestamp(),
                ChannelFields.UPDATED_BY to currentUserId
            )
            
            channelRef.update(updateData as Map<String, Any>).await()
            
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * 채널을 삭제합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param channelId 채널 ID
     * @return 작업 성공 여부
     */
    override suspend fun deleteChannel(
        projectId: String,
        categoryId: String,
        channelId: String
    ): Result<Unit> = try {
        // 채널 문서 참조
        val channelRef = firestore.collection(Collections.PROJECTS).document(projectId)
            .collection(Collections.CATEGORIES).document(categoryId)
            .collection(Collections.CHANNELS).document(channelId)
        
        // 채널 존재 확인
        val channelDoc = channelRef.get().await()
        
        if (!channelDoc.exists()) {
            Result.failure(IllegalArgumentException("존재하지 않는 채널입니다."))
        } else {
            // 채널 삭제
            channelRef.delete().await()
            
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
} 