package com.example.data.datasource.remote

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.example.data.model.remote.TaskContainerDTO
import com.example.domain.model.vo.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 태스크 컨테이너 정보에 접근하기 위한 인터페이스입니다.
 * 통합된 task_container collection에서 container 타입 문서만 필터링하여 처리합니다.
 */
interface TaskContainerRemoteDataSource : DefaultDatasource

@Singleton
class TaskContainerRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DefaultDatasourceImpl<TaskContainerDTO>(firestore, TaskContainerDTO::class.java), TaskContainerRemoteDataSource {
    
    /**
     * 고정된 container ID로 단일 TaskContainer 문서를 관찰합니다.
     */
    fun observeContainer(): Flow<TaskContainerDTO?> {
        return observeById(TaskContainerDTO.FIXED_CONTAINER_ID)
    }
    
    /**
     * 모든 container 타입 문서를 관찰합니다 (실제로는 하나만 있음).
     */
    override fun observeAll(): Flow<List<TaskContainerDTO>> {
        return super.observeAll().map { documents ->
            documents.filter { it.type == TaskContainerDTO.TYPE_CONTAINER }
        }
    }
    
    /**
     * Container 정의 문서를 생성합니다. ID는 항상 고정값을 사용합니다.
     */
    suspend fun createContainer(dto: TaskContainerDTO): CustomResult<DocumentId, Exception> {
        val containerDto = dto.copy(
            id = TaskContainerDTO.FIXED_CONTAINER_ID,
            type = TaskContainerDTO.TYPE_CONTAINER
        )
        return create(containerDto)
    }
    
    /**
     * Container 정의 문서를 업데이트합니다.
     */
    suspend fun updateContainer(fields: Map<String, Any?>): CustomResult<DocumentId, Exception> {
        return update(TaskContainerDTO.FIXED_CONTAINER_ID, fields)
    }
    
    /**
     * Container 정의 문서를 삭제합니다.
     */
    suspend fun deleteContainer(): CustomResult<DocumentId, Exception> {
        return delete(TaskContainerDTO.FIXED_CONTAINER_ID)
    }
}