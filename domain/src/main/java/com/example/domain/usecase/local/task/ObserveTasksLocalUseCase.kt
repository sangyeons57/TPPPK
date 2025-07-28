package com.example.domain.usecase.local.task

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Task
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.TaskLocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface ObserveTasksLocalUseCase {
    operator fun invoke(projectId: DocumentId): Flow<CustomResult<List<Task>, Exception>>
}

class ObserveTasksLocalUseCaseImpl @Inject constructor(
    private val taskLocalRepository: TaskLocalRepository
) : ObserveTasksLocalUseCase {

    override operator fun invoke(projectId: DocumentId): Flow<CustomResult<List<Task>, Exception>> {
        return TODO("로컬 저장소에서 특정 프로젝트의 태스크 목록을 실시간으로 관찰")
    }
} 