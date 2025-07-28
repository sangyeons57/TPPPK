package com.example.domain.provider

import com.example.domain.repository.local.TaskLocalRepository
import com.example.domain.usecase.local.task.CreateTaskLocalUseCase
import com.example.domain.usecase.local.task.CreateTaskLocalUseCaseImpl
import com.example.domain.usecase.local.task.DeleteTaskLocalUseCase
import com.example.domain.usecase.local.task.DeleteTaskLocalUseCaseImpl
import com.example.domain.usecase.local.task.GetTasksLocalUseCase
import com.example.domain.usecase.local.task.GetTasksLocalUseCaseImpl
import com.example.domain.usecase.local.task.ObserveTasksLocalUseCase
import com.example.domain.usecase.local.task.ObserveTasksLocalUseCaseImpl
import com.example.domain.usecase.local.task.ReorderTaskLocalUseCase
import com.example.domain.usecase.local.task.ReorderTaskLocalUseCaseImpl
import com.example.domain.usecase.local.task.ToggleTaskCheckLocalUseCase
import com.example.domain.usecase.local.task.ToggleTaskCheckLocalUseCaseImpl
import com.example.domain.usecase.local.task.UpdateTaskLocalUseCase
import com.example.domain.usecase.local.task.UpdateTaskLocalUseCaseImpl
import com.example.domain.usecase.local.task.UpdateTaskStatusLocalUseCase
import com.example.domain.usecase.local.task.UpdateTaskStatusLocalUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 태스크 관리 관련 Local UseCase들을 제공하는 Provider
 * 
 * 로컬 저장소를 기반으로 한 태스크 생성, 수정, 삭제, 상태 변경 등의 기능을 담당합니다.
 */
@Singleton
class TaskUseCaseProvider @Inject constructor(
    private val taskLocalRepository: TaskLocalRepository
) {

    /**
     * 태스크 기본 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 태스크 기본 관리 UseCase 그룹
     */
    fun createBasicUseCases(): TaskLocalBasicUseCases {
        return TaskLocalBasicUseCases(
            // 태스크 생성
            createTaskLocalUseCase = CreateTaskLocalUseCaseImpl(
                taskLocalRepository = taskLocalRepository
            ),
            
            // 태스크 조회
            getTasksLocalUseCase = GetTasksLocalUseCaseImpl(
                taskLocalRepository = taskLocalRepository
            ),
            
            observeTasksLocalUseCase = ObserveTasksLocalUseCaseImpl(
                taskLocalRepository = taskLocalRepository
            ),
            
            // 태스크 삭제
            deleteTaskLocalUseCase = DeleteTaskLocalUseCaseImpl(
                taskLocalRepository = taskLocalRepository
            ),
            
            taskLocalRepository = taskLocalRepository
        )
    }

    /**
     * 태스크 상태 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 태스크 상태 관리 UseCase 그룹
     */
    fun createStatusUseCases(): TaskLocalStatusUseCases {
        return TaskLocalStatusUseCases(
            // 태스크 수정
            updateTaskLocalUseCase = UpdateTaskLocalUseCaseImpl(
                taskLocalRepository = taskLocalRepository
            ),
            
            updateTaskStatusLocalUseCase = UpdateTaskStatusLocalUseCaseImpl(
                taskLocalRepository = taskLocalRepository
            ),
            
            // 태스크 체크 토글
            toggleTaskCheckLocalUseCase = ToggleTaskCheckLocalUseCaseImpl(
                taskLocalRepository = taskLocalRepository
            ),
            
            // 태스크 순서 변경
            reorderTaskLocalUseCase = ReorderTaskLocalUseCaseImpl(
                taskLocalRepository = taskLocalRepository
            ),
            
            taskLocalRepository = taskLocalRepository
        )
    }
}

/**
 * 태스크 기본 관리 Local UseCase 그룹
 */
data class TaskLocalBasicUseCases(
    // 태스크 생성
    val createTaskLocalUseCase: CreateTaskLocalUseCase,
    
    // 태스크 조회
    val getTasksLocalUseCase: GetTasksLocalUseCase,
    val observeTasksLocalUseCase: ObserveTasksLocalUseCase,
    
    // 태스크 삭제
    val deleteTaskLocalUseCase: DeleteTaskLocalUseCase,
    
    val taskLocalRepository: TaskLocalRepository
)

/**
 * 태스크 상태 관리 Local UseCase 그룹
 */
data class TaskLocalStatusUseCases(
    // 태스크 수정
    val updateTaskLocalUseCase: UpdateTaskLocalUseCase,
    val updateTaskStatusLocalUseCase: UpdateTaskStatusLocalUseCase,
    
    // 태스크 체크 토글
    val toggleTaskCheckLocalUseCase: ToggleTaskCheckLocalUseCase,
    
    // 태스크 순서 변경
    val reorderTaskLocalUseCase: ReorderTaskLocalUseCase,
    
    val taskLocalRepository: TaskLocalRepository
) 