package com.example.domain_usecase.provider.task

import com.example.domain.vo.CollectionPath
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.TaskRepository
import com.example.domain_usecase.usecase.task.CreateTaskUseCase
import com.example.domain_usecase.usecase.task.CreateTaskUseCaseImpl
import com.example.domain_usecase.usecase.task.DeleteTaskUseCase
import com.example.domain_usecase.usecase.task.DeleteTaskUseCaseImpl
import com.example.domain_usecase.usecase.task.ObserveChannelTasksUseCase
import com.example.domain_usecase.usecase.task.ObserveChannelTasksUseCaseImpl
import com.example.domain_usecase.usecase.task.ToggleTaskCheckUseCase
import com.example.domain_usecase.usecase.task.ToggleTaskCheckUseCaseImpl
import com.example.domain_usecase.usecase.task.UpdateTaskStatusUseCase
import com.example.domain_usecase.usecase.task.UpdateTaskStatusUseCaseImpl
import com.example.domain_usecase.usecase.task.UpdateTaskUseCase
import com.example.domain_usecase.usecase.task.UpdateTaskUseCaseImpl
import com.example.domain_usecase.usecase.task.MoveTaskUseCase
import com.example.domain_usecase.usecase.task.MoveTaskUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 태스크 관리 관련 UseCase들을 제공하는 Provider
 * 
 * 태스크 컨테이너 및 태스크 생성, 조회, 수정, 삭제 등의 기능을 담당합니다.
 */
@Singleton
class TaskUseCaseProvider @Inject constructor(
    private val taskRepository: TaskRepository,
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
                taskRepository = this.taskRepository
            ),
            
            deleteTaskUseCase = DeleteTaskUseCaseImpl(
                taskRepository = this.taskRepository
            ),
            
            updateTaskUseCase = UpdateTaskUseCaseImpl(
                taskRepository = this.taskRepository
            ),
            
            updateTaskStatusUseCase = UpdateTaskStatusUseCaseImpl(
                taskRepository = this.taskRepository
            ),
            
            toggleTaskCheckUseCase = ToggleTaskCheckUseCaseImpl(
                taskRepository = this.taskRepository,
                authRepository = this.authRepository,
            ),

            observeChannelTasksUseCase = ObserveChannelTasksUseCaseImpl(
                taskRepository = this.taskRepository
            ),

            moveTaskUseCase = MoveTaskUseCaseImpl(
                taskRepository = this.taskRepository
            ),
            
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
    val observeChannelTasksUseCase: ObserveChannelTasksUseCase,
    val moveTaskUseCase: MoveTaskUseCase,

    )
