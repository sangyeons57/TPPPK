package com.example.data.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data.datasource.local.LocalProjectsWrapperDataSource
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.model.vo.projectwrapper.ProjectWrapperOrder
import com.example.domain.repository.local.LocalProjectsWrapperRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Projects Wrapper Repository Implementation (SSOT)
 * Room Database 전용 구현체 - UI에 직접 데이터 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지
 *
 * ✅ 역할:
 * - LocalDataSource를 통한 Room DB 접근
 * - Flow로 UI에 실시간 데이터 제공
 * - 로컬 CRUD 작업 처리
 * - Outbox 관리 (동기화 대상 저장)
 */
@Singleton
class LocalProjectsWrapperRepositoryImpl @Inject constructor(
    private val localProjectsWrapperDataSource: LocalProjectsWrapperDataSource
) : LocalProjectsWrapperRepository {

    companion object {
        private const val TAG = "LocalProjectsWrapperRepository"
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeWrapperById(wrapperId: String): Flow<ProjectsWrapper?> {
        Log.d(TAG, "observeWrapperById: $wrapperId")
        return localProjectsWrapperDataSource.observeWrapperById(wrapperId)
    }

    override fun observeWrappersByUser(userId: String): Flow<List<ProjectsWrapper>> {
        Log.d(TAG, "observeWrappersByUser: $userId")
        return localProjectsWrapperDataSource.observeWrappersByUser(userId)
    }

    override fun observeWrapperByUserAndProject(
        userId: String,
        projectId: String
    ): Flow<ProjectsWrapper?> {
        Log.d(TAG, "observeWrapperByUserAndProject: userId=$userId, projectId=$projectId")
        return localProjectsWrapperDataSource.observeWrapperByUserAndProject(userId, projectId)
    }

    override fun observeWrappersByProjectName(projectName: ProjectName): Flow<List<ProjectsWrapper>> {
        Log.d(TAG, "observeWrappersByProjectName: ${projectName.value}")
        return localProjectsWrapperDataSource.observeWrappersByProjectName(projectName)
    }

    override fun observeWrappersByNameContaining(
        name: String,
        limit: Int
    ): Flow<List<ProjectsWrapper>> {
        Log.d(TAG, "observeWrappersByNameContaining: name='$name', limit=$limit")
        return localProjectsWrapperDataSource.observeWrappersByNameContaining(name, limit)
    }

    override fun observeWrappers(wrapperIds: List<String>): Flow<List<ProjectsWrapper>> {
        Log.d(TAG, "observeWrappers: ${wrapperIds.size} wrappers")
        return localProjectsWrapperDataSource.observeWrappers(wrapperIds)
    }

    override fun observeWrapperUpdatedAt(wrapperId: String): Flow<Long?> {
        Log.d(TAG, "observeWrapperUpdatedAt: $wrapperId")
        return localProjectsWrapperDataSource.observeWrapperUpdatedAt(wrapperId)
    }

    override fun observeAllWrappers(): Flow<List<ProjectsWrapper>> {
        Log.d(TAG, "observeAllWrappers")
        return localProjectsWrapperDataSource.observeAllWrappers()
    }

    override fun observeWrappersByOrderRange(
        minOrder: Int,
        maxOrder: Int
    ): Flow<List<ProjectsWrapper>> {
        Log.d(TAG, "observeWrappersByOrderRange: minOrder=$minOrder, maxOrder=$maxOrder")
        return localProjectsWrapperDataSource.observeWrappersByOrderRange(minOrder, maxOrder)
    }

    // === 단순 읽기 작업 ===

    override suspend fun getWrapperById(wrapperId: String): ProjectsWrapper? {
        Log.d(TAG, "getWrapperById: $wrapperId")
        return try {
            localProjectsWrapperDataSource.getWrapperById(wrapperId)
        } catch (e: Exception) {
            Log.e(TAG, "getWrapperById failed", e)
            null
        }
    }

    override suspend fun getWrappersByUser(userId: String): List<ProjectsWrapper> {
        Log.d(TAG, "getWrappersByUser: $userId")
        return try {
            localProjectsWrapperDataSource.getWrappersByUser(userId)
        } catch (e: Exception) {
            Log.e(TAG, "getWrappersByUser failed", e)
            emptyList()
        }
    }

    override suspend fun getWrapperByUserAndProject(
        userId: String,
        projectId: String
    ): ProjectsWrapper? {
        Log.d(TAG, "getWrapperByUserAndProject: userId=$userId, projectId=$projectId")
        return try {
            localProjectsWrapperDataSource.getWrapperByUserAndProject(userId, projectId)
        } catch (e: Exception) {
            Log.e(TAG, "getWrapperByUserAndProject failed", e)
            null
        }
    }

    override suspend fun getWrappersByProjectName(projectName: ProjectName): List<ProjectsWrapper> {
        Log.d(TAG, "getWrappersByProjectName: ${projectName.value}")
        return try {
            localProjectsWrapperDataSource.getWrappersByProjectName(projectName)
        } catch (e: Exception) {
            Log.e(TAG, "getWrappersByProjectName failed", e)
            emptyList()
        }
    }

    override suspend fun searchWrappersByName(name: String, limit: Int): List<ProjectsWrapper> {
        Log.d(TAG, "searchWrappersByName: name='$name', limit=$limit")
        return try {
            localProjectsWrapperDataSource.searchWrappersByName(name, limit)
        } catch (e: Exception) {
            Log.e(TAG, "searchWrappersByName failed", e)
            emptyList()
        }
    }

    override suspend fun getWrappersByIds(wrapperIds: List<String>): List<ProjectsWrapper> {
        Log.d(TAG, "getWrappersByIds: ${wrapperIds.size} wrappers")
        return try {
            localProjectsWrapperDataSource.getWrappersByIds(wrapperIds)
        } catch (e: Exception) {
            Log.e(TAG, "getWrappersByIds failed", e)
            emptyList()
        }
    }

    override suspend fun getAllWrappers(limit: Int?): List<ProjectsWrapper> {
        Log.d(TAG, "getAllWrappers: limit=$limit")
        return try {
            localProjectsWrapperDataSource.getAllWrappers(limit)
        } catch (e: Exception) {
            Log.e(TAG, "getAllWrappers failed", e)
            emptyList()
        }
    }

    override suspend fun getWrappersByOrderRange(
        minOrder: Int,
        maxOrder: Int
    ): List<ProjectsWrapper> {
        Log.d(TAG, "getWrappersByOrderRange: minOrder=$minOrder, maxOrder=$maxOrder")
        return try {
            localProjectsWrapperDataSource.getWrappersByOrderRange(minOrder, maxOrder)
        } catch (e: Exception) {
            Log.e(TAG, "getWrappersByOrderRange failed", e)
            emptyList()
        }
    }

    // === 쓰기 작업 (Outbox 포함) ===

    override suspend fun saveWrapper(wrapper: ProjectsWrapper): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveWrapper: ${wrapper.id}")

            // 1. Room DB에 저장
            localProjectsWrapperDataSource.saveWrapper(wrapper)

            // 2. Outbox에 동기화 작업 추가
            val operation = if (wrapper.isNew) "CREATE" else "UPDATE"
            localProjectsWrapperDataSource.addToOutbox(
                wrapperId = wrapper.id.value,
                operation = operation,
                payload = null // 필요시 JSON 직렬화된 변경사항
            )

            Log.d(TAG, "Wrapper saved and added to outbox: ${wrapper.id}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveWrapper failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun saveWrappers(wrappers: List<ProjectsWrapper>): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveWrappers: ${wrappers.size} wrappers")

            if (wrappers.isEmpty()) {
                return CustomResult.Success(Unit)
            }

            // 대량 저장 (동기화용 - Outbox 추가 안 함)
            localProjectsWrapperDataSource.saveWrappers(wrappers)

            Log.d(TAG, "Bulk wrappers saved: ${wrappers.size}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveWrappers failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteWrapper(wrapperId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteWrapper: $wrapperId")

            // 1. Room DB에서 삭제 (실제로는 soft delete)
            localProjectsWrapperDataSource.deleteWrapper(wrapperId)

            // 2. Outbox에 삭제 작업 추가
            localProjectsWrapperDataSource.addToOutbox(
                wrapperId = wrapperId,
                operation = "DELETE",
                payload = null
            )

            Log.d(TAG, "Wrapper deleted and added to outbox: $wrapperId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteWrapper failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateWrapper(
        wrapperId: String,
        projectName: ProjectName?,
        projectImageUrl: ImageUrl?,
        order: ProjectWrapperOrder?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(
                TAG,
                "updateWrapper: wrapperId=$wrapperId, projectName=$projectName, order=$order"
            )

            // 1. 현재 래퍼 조회
            val currentWrapper = localProjectsWrapperDataSource.getWrapperById(wrapperId)
                ?: return CustomResult.Failure(IllegalArgumentException("Wrapper not found: $wrapperId"))

            // 2. 업데이트된 래퍼 생성 (필요한 필드만 수정)
            var updatedWrapper = currentWrapper

            projectName?.let {
                updatedWrapper = updatedWrapper.copy(projectName = it)
            }
            projectImageUrl?.let {
                updatedWrapper = updatedWrapper.copy(projectImageUrl = it)
            }
            order?.let {
                updatedWrapper = updatedWrapper.copy(order = it)
            }

            // 3. 저장 (Outbox 포함)
            saveWrapper(updatedWrapper)

            Log.d(TAG, "Wrapper updated: $wrapperId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "updateWrapper failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun reorderWrappers(wrapperOrderMap: Map<String, Int>): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "reorderWrappers: ${wrapperOrderMap.size} wrappers")

            // 1. 각 래퍼 업데이트
            wrapperOrderMap.forEach { (wrapperId, newOrder) ->
                val wrapper = localProjectsWrapperDataSource.getWrapperById(wrapperId)
                if (wrapper != null) {
                    val updatedWrapper = wrapper.copy(order = ProjectWrapperOrder(newOrder))
                    localProjectsWrapperDataSource.saveWrapper(updatedWrapper)

                    // Outbox에 추가
                    localProjectsWrapperDataSource.addToOutbox(
                        wrapperId = wrapperId,
                        operation = "UPDATE",
                        payload = null
                    )
                }
            }

            Log.d(TAG, "Wrappers reordered: ${wrapperOrderMap.size}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "reorderWrappers failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun reorderUserWrappers(
        userId: String,
        wrapperOrderMap: Map<String, Int>
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "reorderUserWrappers: userId=$userId, ${wrapperOrderMap.size} wrappers")

            // 1. 사용자의 래퍼들 중에서만 재정렬
            val userWrappers = localProjectsWrapperDataSource.getWrappersByUser(userId)
            val userWrapperIds = userWrappers.map { it.id.value }.toSet()

            // 2. 유효한 래퍼들만 필터링
            val validOrderMap = wrapperOrderMap.filterKeys { it in userWrapperIds }

            // 3. 재정렬 수행
            reorderWrappers(validOrderMap)

            Log.d(TAG, "User wrappers reordered: userId=$userId, count=${validOrderMap.size}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "reorderUserWrappers failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 유틸리티 ===

    override suspend fun wrapperExists(wrapperId: String): Boolean {
        return try {
            localProjectsWrapperDataSource.wrapperExists(wrapperId)
        } catch (e: Exception) {
            Log.e(TAG, "wrapperExists failed", e)
            false
        }
    }

    override suspend fun userHasProjectWrapper(userId: String, projectId: String): Boolean {
        return try {
            localProjectsWrapperDataSource.userHasProjectWrapper(userId, projectId)
        } catch (e: Exception) {
            Log.e(TAG, "userHasProjectWrapper failed", e)
            false
        }
    }

    override suspend fun projectNameExistsForUser(
        userId: String,
        projectName: ProjectName,
        excludeWrapperId: String?
    ): Boolean {
        return try {
            localProjectsWrapperDataSource.projectNameExistsForUser(
                userId,
                projectName,
                excludeWrapperId
            )
        } catch (e: Exception) {
            Log.e(TAG, "projectNameExistsForUser failed", e)
            false
        }
    }

    override suspend fun getTotalWrapperCount(): Int {
        return try {
            localProjectsWrapperDataSource.getTotalWrapperCount()
        } catch (e: Exception) {
            Log.e(TAG, "getTotalWrapperCount failed", e)
            0
        }
    }

    override suspend fun getWrapperCountByUser(userId: String): Int {
        return try {
            localProjectsWrapperDataSource.getWrapperCountByUser(userId)
        } catch (e: Exception) {
            Log.e(TAG, "getWrapperCountByUser failed", e)
            0
        }
    }

    override suspend fun getNextWrapperOrder(userId: String): Int {
        return try {
            localProjectsWrapperDataSource.getNextWrapperOrder(userId)
        } catch (e: Exception) {
            Log.e(TAG, "getNextWrapperOrder failed", e)
            0
        }
    }

    override suspend fun clearAllWrappers(): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "clearAllWrappers")

            localProjectsWrapperDataSource.clearAllWrappers()

            Log.d(TAG, "All wrappers cleared")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "clearAllWrappers failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun clearUserWrappers(userId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "clearUserWrappers: $userId")

            localProjectsWrapperDataSource.clearUserWrappers(userId)

            Log.d(TAG, "User wrappers cleared: $userId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "clearUserWrappers failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 동기화 지원 ===

    override suspend fun getWrappersUpdatedAfter(timestamp: Instant): List<ProjectsWrapper> {
        return try {
            localProjectsWrapperDataSource.getWrappersUpdatedAfter(timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "getWrappersUpdatedAfter failed", e)
            emptyList()
        }
    }

    override suspend fun addToOutbox(
        wrapperId: String,
        operation: String,
        payload: String?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "addToOutbox: wrapperId=$wrapperId, operation=$operation")

            localProjectsWrapperDataSource.addToOutbox(wrapperId, operation, payload)

            Log.d(TAG, "Added to outbox: $wrapperId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "addToOutbox failed", e)
            CustomResult.Failure(e)
        }
    }
}