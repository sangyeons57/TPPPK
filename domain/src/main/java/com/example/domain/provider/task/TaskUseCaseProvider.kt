package com.example.domain.provider.task

import com.example.domain.model.vo.CollectionPath
import com.example.domain.repository.remote.AuthRepository
import com.example.domain.repository.remote.DefaultRepository
import com.example.domain.model.base.Task
import com.example.domain.usecase.task.CreateTaskUseCase
import com.example.domain.usecase.task.CreateTaskUseCaseImpl
import com.example.domain.usecase.task.DeleteTaskUseCase
import com.example.domain.usecase.task.DeleteTaskUseCaseImpl
import com.example.domain.usecase.task.GetTasksUseCase
import com.example.domain.usecase.task.GetTasksUseCaseImpl
import com.example.domain.usecase.task.ObserveTasksUseCase
import com.example.domain.usecase.task.ObserveTasksUseCaseImpl
import com.example.domain.usecase.task.ToggleTaskCheckUseCase
import com.example.domain.usecase.task.ToggleTaskCheckUseCaseImpl
import com.example.domain.usecase.task.UpdateTaskStatusUseCase
import com.example.domain.usecase.task.UpdateTaskStatusUseCaseImpl
import com.example.domain.usecase.task.UpdateTaskUseCase
import com.example.domain.usecase.task.UpdateTaskUseCaseImpl
import com.example.domain.usecase.task.ReorderTaskUseCase
import com.example.domain.usecase.task.ReorderTaskUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 태스크 관리 관련 UseCase들을 제공하는 Provider
 * 
 * 태스크 컨테이너 및 태스크 생성, 조회, 수정, 삭제 등의 기능을 담당합니다.
 */
@Singleton
class TaskUseCaseProvider @Inject constructor(
    private val taskRepository: DefaultRepository<Task>,
    private val authRepository: AuthRepository,
) {


    /**
     * 특정 프로젝트 채널의 태스크 관련 UseCase들을 생성합니다.
     * 통합된 task_container collection을 사용합니다.
     * containerId 파라미터는 하위 호환성을 위해 유지하지만 실제로는 사용하지 않습니다.
     * 
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @param containerId 컨테이너 ID (사용되지 않음, 하위 호환성용)
     * @return 태스크 관련 UseCase 그룹
     */
    fun createForTasks(projectId: String, channelId: String, containerId: String): TaskUseCases {
        taskRepository.setCollection(CollectionPath.tasks(projectId, channelId))

        return TaskUseCases(
            createTaskUseCase = CreateTaskUseCaseImpl(
                taskRepository = taskRepository
            ),
            
            deleteTaskUseCase = DeleteTaskUseCaseImpl(
                taskRepository = taskRepository
            ),
            
            updateTaskUseCase = UpdateTaskUseCaseImpl(
                taskRepository = taskRepository
            ),
            
            updateTaskStatusUseCase = UpdateTaskStatusUseCaseImpl(
                taskRepository = taskRepository
            ),
            
            toggleTaskCheckUseCase = ToggleTaskCheckUseCaseImpl(
                taskRepository = taskRepository,
                authRepository = authRepository,
            ),
            
            getTasksUseCase = GetTasksUseCaseImpl(
                taskRepository = taskRepository
            ),
            
            observeTasksUseCase = ObserveTasksUseCaseImpl(
                taskRepository = taskRepository
            ),
            
            reorderTaskUseCase = ReorderTaskUseCaseImpl(
                taskRepository = taskRepository
            ),
            
            taskRepository= taskRepository,
            authRepository= authRepository
        )
    }
}
    

/**
 * 태스크 관련 UseCase 그룹
 */
data class TaskUseCases(
    val createTaskUseCase: CreateTaskUseCase,
    val deleteTaskUseCase: DeleteTaskUseCase,
    val updateTaskUseCase: UpdateTaskUseCase,
    val updateTaskStatusUseCase: UpdateTaskStatusUseCase,
    val toggleTaskCheckUseCase: ToggleTaskCheckUseCase,
    val getTasksUseCase: GetTasksUseCase,
    val observeTasksUseCase: ObserveTasksUseCase,
    val reorderTaskUseCase: ReorderTaskUseCase,
    val taskRepository: DefaultRepository<Task>,
    val authRepository: AuthRepository
)