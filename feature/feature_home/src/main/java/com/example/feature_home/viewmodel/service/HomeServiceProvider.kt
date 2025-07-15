package com.example.feature_home.viewmodel.service

import com.example.core_navigation.core.NavigationManger
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.provider.dm.DMUseCaseProvider
import com.example.domain.provider.project.CoreProjectUseCaseProvider
import com.example.domain.provider.project.ProjectStructureUseCaseProvider
import com.example.domain.provider.user.UserUseCaseProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HomeViewModel의 Service들을 제공하는 Provider 클래스
 * UseCaseProvider 패턴을 모방하여 Service들을 통합 관리합니다.
 */
@Singleton
class HomeServiceProvider @Inject constructor(
    private val userUseCaseProvider: UserUseCaseProvider,
    private val coreProjectUseCaseProvider: CoreProjectUseCaseProvider,
    private val projectStructureUseCaseProvider: ProjectStructureUseCaseProvider,
    private val dmUseCaseProvider: DMUseCaseProvider,
    private val navigationManager: NavigationManger
) {
    
    /**
     * 특정 프로젝트와 사용자를 위한 Service들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @return Home 화면에서 사용할 Service 그룹
     */
    fun create(projectId: DocumentId, userId: UserId): HomeServices {
        // 각 UseCaseProvider에서 UseCases 생성
        val userUseCases = userUseCaseProvider.createForUser()
        val coreProjectUseCases = coreProjectUseCaseProvider.createForProject(projectId, userId)
        val projectStructureUseCases = projectStructureUseCaseProvider.createForProject(projectId)
        val dmUseCases = dmUseCaseProvider.createForUser(userId)
        
        // 생성된 UseCases를 각 Service에 전달하여 생성
        return HomeServices(
            loadUserDataService = LoadUserDataService(userUseCases),
            loadProjectsService = LoadProjectsService(coreProjectUseCases),
            loadDmsService = LoadDmsService(dmUseCases),
            projectSelectionService = ProjectSelectionService(coreProjectUseCases, projectStructureUseCases),
            categoryManagementService = CategoryManagementService(projectStructureUseCases),
            navigationService = NavigationService(navigationManager),
            dialogManagementService = DialogManagementService()
        )
    }
    
    /**
     * 현재 사용자를 위한 Service들을 생성합니다. (프로젝트 없는 경우)
     * 
     * @return Home 화면에서 사용할 Service 그룹
     */
    fun createForCurrentUser(): HomeServices {
        val userUseCases = userUseCaseProvider.createForUser()
        val coreProjectUseCases = coreProjectUseCaseProvider.createForCurrentUser()
        // For DM, we need to use current user, but dmUseCaseProvider.createForUser needs a userId
        // Let's use the auth repository to get current user ID later in the service
        val dmUseCases = dmUseCaseProvider.createForUser(UserId.EMPTY)
        
        return HomeServices(
            loadUserDataService = LoadUserDataService(userUseCases),
            loadProjectsService = LoadProjectsService(coreProjectUseCases),
            loadDmsService = LoadDmsService(dmUseCases),
            projectSelectionService = ProjectSelectionService(coreProjectUseCases, null),
            categoryManagementService = CategoryManagementService(null),
            navigationService = NavigationService(navigationManager),
            dialogManagementService = DialogManagementService()
        )
    }
}

/**
 * Home 화면에서 사용할 Service 그룹
 */
data class HomeServices(
    val loadUserDataService: LoadUserDataService,
    val loadProjectsService: LoadProjectsService,
    val loadDmsService: LoadDmsService,
    val projectSelectionService: ProjectSelectionService,
    val categoryManagementService: CategoryManagementService,
    val navigationService: NavigationService,
    val dialogManagementService: DialogManagementService
)