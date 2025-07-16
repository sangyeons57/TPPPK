package com.example.feature_home.viewmodel.service

import com.example.core_navigation.core.NavigationManger
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.provider.dm.DMUseCaseProvider
import com.example.domain.provider.project.CoreProjectUseCaseProvider
import com.example.domain.provider.project.ProjectStructureUseCaseProvider
import com.example.domain.provider.user.UserUseCaseProvider
import javax.inject.Inject

/**
 * HomeViewModel의 Service들을 제공하는 Provider 클래스
 * Hilt의 DI를 통해 필요한 Service들을 생성하여 제공합니다.
 */
class HomeViewModelServiceProvider @Inject constructor(
    private val userUseCaseProvider: UserUseCaseProvider,
    private val dmUseCaseProvider: DMUseCaseProvider,
    private val coreProjectUseCaseProvider: CoreProjectUseCaseProvider,
    private val projectStructureUseCaseProvider: ProjectStructureUseCaseProvider,
    private val navigationManger: NavigationManger
) {
    
    /**
     * HomeViewModel에서 사용할 Service들을 묶어서 제공하는 데이터 클래스
     */
    data class HomeViewModelServices(
        val loadUserDataService: LoadUserDataService,
        val loadProjectsService: LoadProjectsService,
        val loadDmsService: LoadDmsService,
        val navigationService: NavigationService,
        val dialogManagementService: DialogManagementService
    )

    data class ProjectServices(
        val projectSelectionService: ProjectSelectionService,
        val categoryManagementService: CategoryManagementService
    )
    
    /**
     * HomeViewModel에서 사용할 Service들을 생성하여 반환
     */
    fun createForUser(userId: UserId): HomeViewModelServices {
        val userUseCases = userUseCaseProvider.createForUser()
        val dmUseCases = dmUseCaseProvider.createForUser(userId)
        val coreProjectUseCases = coreProjectUseCaseProvider.createForCurrentUser()

        val loadUserDataService = LoadUserDataService(userUseCases)
        val loadProjectsService = LoadProjectsService(coreProjectUseCases)
        val loadDmsService = LoadDmsService(dmUseCases)
        val navigationService = NavigationService(navigationManger)
        val dialogManagementService = DialogManagementService()

        return HomeViewModelServices(
            loadUserDataService = loadUserDataService,
            loadProjectsService = loadProjectsService,
            loadDmsService = loadDmsService,
            navigationService = navigationService,
            dialogManagementService = dialogManagementService
        )
    }

    fun createProjectServices(projectId: DocumentId, userId: UserId): ProjectServices {
        val coreProjectUseCases = coreProjectUseCaseProvider.createForProject(projectId, userId)
        val structureUseCases = projectStructureUseCaseProvider.createForProject(projectId)

        val projectSelectionService = ProjectSelectionService(coreProjectUseCases, structureUseCases)
        val categoryManagementService = CategoryManagementService(structureUseCases)

        return ProjectServices(
            projectSelectionService = projectSelectionService,
            categoryManagementService = categoryManagementService
        )
    }
}