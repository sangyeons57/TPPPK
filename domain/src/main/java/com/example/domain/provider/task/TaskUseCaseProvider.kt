package com.example.domain.provider.task

import com.example.domain.model.vo.CollectionPath
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.TaskContainerRepository
import com.example.domain.repository.base.TaskRepository
import com.example.domain.repository.factory.context.TaskContainerRepositoryFactoryContext
import com.example.domain.repository.factory.context.TaskRepositoryFactoryContext
import com.example.domain.repository.factory.context.NestedTaskContainerRepositoryFactoryContext
import com.example.domain.repository.factory.context.NestedTaskRepositoryFactoryContext
import com.example.domain.usecase.task.CreateTaskContainerUseCase
import com.example.domain.usecase.task.CreateTaskContainerUseCaseImpl
import com.example.domain.usecase.task.CreateTaskUseCase
import com.example.domain.usecase.task.CreateTaskUseCaseImpl
import com.example.domain.usecase.task.DeleteTaskContainerUseCase
import com.example.domain.usecase.task.DeleteTaskContainerUseCaseImpl
import com.example.domain.usecase.task.DeleteTaskUseCase
import com.example.domain.usecase.task.DeleteTaskUseCaseImpl
import com.example.domain.usecase.task.UpdateTaskStatusUseCase
import com.example.domain.usecase.task.UpdateTaskStatusUseCaseImpl
import com.example.domain.usecase.task.UpdateTaskUseCase
import com.example.domain.usecase.task.UpdateTaskUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 태스크 관리 관련 UseCase들을 제공하는 Provider
 * 
 * 태스크 컨테이너 및 태스크 생성, 조회, 수정, 삭제 등의 기능을 담당합니다.
 */
@Singleton
class TaskUseCaseProvider @Inject constructor(
    private val taskContainerRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<TaskContainerRepositoryFactoryContext, TaskContainerRepository>,
    private val taskRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<TaskRepositoryFactoryContext, TaskRepository>
) {

    /**
     * 특정 프로젝트 채널의 태스크 컨테이너 관련 UseCase들을 생성합니다.
     * 통합된 task_container collection을 사용합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @return 태스크 컨테이너 관련 UseCase 그룹
     */
    fun createForTaskContainers(projectId: String, channelId: String): TaskContainerUseCases {
        val taskContainerRepository = taskContainerRepositoryFactory.create(
            TaskContainerRepositoryFactoryContext(
                collectionPath = CollectionPath.projectChannelUnifiedTaskContainer(projectId, channelId)
            )
        )

        return TaskContainerUseCases(
            createTaskContainerUseCase = CreateTaskContainerUseCaseImpl(
                taskContainerRepository = taskContainerRepository
            ),
            
            deleteTaskContainerUseCase = DeleteTaskContainerUseCaseImpl(
                taskContainerRepository = taskContainerRepository
            ),
            
            // 공통 Repository
            taskContainerRepository = taskContainerRepository
        )
    }

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
        val taskRepository = taskRepositoryFactory.create(
            TaskRepositoryFactoryContext(
                collectionPath = CollectionPath.projectChannelUnifiedTaskContainer(projectId, channelId)
            )
        )

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
            
            // 공통 Repository
            taskRepository = taskRepository
        )
    }
    
    /**
     * 중첩된 TaskContainer용 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @param containerPath 중첩 경로 (루트부터 현재 레벨까지의 container ID들)
     * @return 중첩된 TaskContainer 관련 UseCase 그룹
     */
    fun createForNestedTaskContainers(
        projectId: String, 
        channelId: String, 
        containerPath: List<String>
    ): TaskContainerUseCases {
        val context = if (containerPath.isEmpty()) {
            NestedTaskContainerRepositoryFactoryContext.createRoot(projectId, channelId)
        } else {
            NestedTaskContainerRepositoryFactoryContext.createNested(projectId, channelId, containerPath)
        }
        
        val taskContainerRepository = taskContainerRepositoryFactory.create(
            TaskContainerRepositoryFactoryContext(collectionPath = context.collectionPath)
        )

        return TaskContainerUseCases(
            createTaskContainerUseCase = CreateTaskContainerUseCaseImpl(
                taskContainerRepository = taskContainerRepository
            ),
            
            deleteTaskContainerUseCase = DeleteTaskContainerUseCaseImpl(
                taskContainerRepository = taskContainerRepository
            ),
            
            // 공통 Repository
            taskContainerRepository = taskContainerRepository
        )
    }
    
    /**
     * 중첩된 TaskContainer 내부의 Task용 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @param containerPath 중첩 경로 (루트부터 현재 레벨까지의 container ID들)
     * @return 중첩된 Task 관련 UseCase 그룹
     */
    fun createForNestedTasks(
        projectId: String, 
        channelId: String, 
        containerPath: List<String>
    ): TaskUseCases {
        val context = if (containerPath.isEmpty()) {
            NestedTaskRepositoryFactoryContext.createRoot(projectId, channelId)
        } else {
            NestedTaskRepositoryFactoryContext.createNested(projectId, channelId, containerPath)
        }
        
        val taskRepository = taskRepositoryFactory.create(
            TaskRepositoryFactoryContext(collectionPath = context.collectionPath)
        )

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
            
            // 공통 Repository
            taskRepository = taskRepository
        )
    }
}

/**
 * 태스크 컨테이너 관련 UseCase 그룹
 */
data class TaskContainerUseCases(
    val createTaskContainerUseCase: CreateTaskContainerUseCase,
    val deleteTaskContainerUseCase: DeleteTaskContainerUseCase,
    
    // 공통 Repository
    val taskContainerRepository: TaskContainerRepository
)

/**
 * 태스크 관련 UseCase 그룹
 */
data class TaskUseCases(
    val createTaskUseCase: CreateTaskUseCase,
    val deleteTaskUseCase: DeleteTaskUseCase,
    val updateTaskUseCase: UpdateTaskUseCase,
    val updateTaskStatusUseCase: UpdateTaskStatusUseCase,
    
    // 공통 Repository
    val taskRepository: TaskRepository
)