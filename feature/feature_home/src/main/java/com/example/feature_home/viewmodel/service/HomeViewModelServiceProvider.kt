package com.example.feature_home.viewmodel.service

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
    private val loadUserDataService: LoadUserDataService,
    private val loadProjectsService: LoadProjectsService,
    private val loadDmsService: LoadDmsService,
    private val projectSelectionService: ProjectSelectionService,
    private val categoryManagementService: CategoryManagementService,
    private val navigationService: NavigationService,
    private val dialogManagementService: DialogManagementService
) {
    
    /**
     * HomeViewModel에서 사용할 Service들을 묶어서 제공하는 데이터 클래스
     */
    data class HomeViewModelServices(
        val loadUserDataService: LoadUserDataService,
        val loadProjectsService: LoadProjectsService,
        val loadDmsService: LoadDmsService,
        val projectSelectionService: ProjectSelectionService,
        val categoryManagementService: CategoryManagementService,
        val navigationService: NavigationService,
        val dialogManagementService: DialogManagementService
    )
    
    /**
     * HomeViewModel에서 사용할 Service들을 생성하여 반환
     */
    fun create(): HomeViewModelServices {
        return HomeViewModelServices(
            loadUserDataService = loadUserDataService,
            loadProjectsService = loadProjectsService,
            loadDmsService = loadDmsService,
            projectSelectionService = projectSelectionService,
            categoryManagementService = categoryManagementService,
            navigationService = navigationService,
            dialogManagementService = dialogManagementService
        )
    }
}