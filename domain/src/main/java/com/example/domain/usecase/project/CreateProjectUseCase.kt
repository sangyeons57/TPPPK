package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.CategoryRepository // Added
import com.example.domain.repository.ProjectRepository
import com.example.domain.repository.ProjectsWrapperRepository
import com.example.domain.model.base.Category // Added
import com.example.core_common.constants.Constants // Added
import android.util.Log // Added for logging failure of default category creation
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.category.CategoryName
import com.example.domain.model.vo.category.CategoryOrder
import com.example.domain.model.vo.category.IsCategoryFlag
import javax.inject.Inject

/**
 * 새 프로젝트를 생성하고 초기화하는 유스케이스 클래스입니다.
 *
 * 이 유스케이스는 다음 주요 단계를 수행합니다:
 * 1. 사용자 세션 및 입력된 프로젝트 이름의 유효성을 검사합니다.
 * 2. 새 프로젝트를 생성합니다 ([ProjectRepository]).
 * 3. 생성된 프로젝트에 현재 사용자를 참여시킵니다 ([ProjectsWrapperRepository]).
 * 4. 새 프로젝트에 기본 "카테고리 없음" 카테고리를 자동으로 생성합니다 ([CategoryRepository]).
 *
 * @property projectRepository 프로젝트 생성 및 관리를 위한 저장소입니다.
 * @property projectsWrapperRepository 사용자와 프로젝트 간의 관계(참여 정보)를 관리하는 저장소입니다.
 * @property authRepository 사용자 인증 및 세션 정보를 제공하는 저장소입니다.
 * @property categoryRepository 카테고리 생성 및 관리를 위한 저장소입니다.
 */
class CreateProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val projectsWrapperRepository: ProjectsWrapperRepository,
    private val authRepository: AuthRepository,
    private val categoryRepository: CategoryRepository // Added
) {
    /**
     * 새 프로젝트를 생성하고 초기 설정을 수행합니다.
     *
     * 처리 과정:
     * 1. 입력된 프로젝트 이름의 유효성을 검사합니다 (공백 여부, 길이 2~30자).
     * 2. 현재 사용자 세션 정보를 가져옵니다. 로그인되지 않은 경우 실패 처리합니다.
     * 3. [projectRepository]를 통해 새 프로젝트를 생성합니다. 실패 시 오류를 반환합니다.
     * 4. 생성된 프로젝트 ID를 사용하여 [projectsWrapperRepository]를 통해 현재 사용자를 프로젝트에 참여시킵니다. 실패 시 오류를 반환합니다.
     * 5. [categoryRepository]를 통해 새 프로젝트에 기본 "카테고리 없음"([Constants.NO_CATEGORY_NAME]) 카테고리를 자동으로 생성합니다.
     *    - 이 단계에서 실패하더라도 프로젝트 생성 자체는 성공으로 간주하고 오류를 로그로만 기록합니다 (정책에 따라 변경 가능).
     *
     * @param name 생성할 프로젝트의 이름입니다. 앞뒤 공백은 제거되며, 유효성 검사를 거칩니다.
     * @return 작업 성공 시 생성된 프로젝트의 고유 ID를 포함하는 [CustomResult.Success]를 반환합니다.
     *         실패 (예: 유효성 검사 실패, 사용자 로그인 필요, 프로젝트 생성/참여 실패 등) 시 예외 정보를 포함하는 [CustomResult.Failure]를 반환합니다.
     */
    suspend operator fun invoke(name: String): CustomResult<String, Exception> {
        val trimmedName = name.trim()

        // 프로젝트 이름 유효성 검증
        if (trimmedName.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("프로젝트 이름은 필수 입력값입니다."))
        }
        
        // 프로젝트 이름 길이 검증 (2~30자)
        if (trimmedName.length < 2 || trimmedName.length > 30) {
            return CustomResult.Failure(IllegalArgumentException("프로젝트 이름은 2~30자 사이여야 합니다."))
        }


        Log.d("CreateProjectUseCase", "1");
        // 모든 검증 통과 시 프로젝트 생성
        when (val session = authRepository.getCurrentUserSession()) {
            is CustomResult.Success -> {
                Log.d("CreateProjectUseCase", "2");
                val projectIdResult = projectRepository.createProject(trimmedName, session.data.userId)
                if (projectIdResult is CustomResult.Failure) {
                    return CustomResult.Failure(projectIdResult.error)
                } else if (projectIdResult !is CustomResult.Success) {
                    return CustomResult.Failure(Exception("프로젝트 생성에 실패했습니다."))
                }
                Log.d("CreateProjectUseCase", "3");
                val projectId = projectIdResult.data

                // 생성된 프로젝트에 현재 사용자를 참여시킵니다.
                val addProjectToUserResult = projectsWrapperRepository.addProjectToUser(session.data.userId, projectId)

                if (addProjectToUserResult is CustomResult.Failure) {
                    // 프로젝트 생성은 성공했으나 사용자를 참여시키는 데 실패한 경우입니다.
                    // 필요에 따라 여기서 생성된 프로젝트를 삭제하는 등의 롤백 로직을 고려할 수 있으나,
                    // 현재는 에러를 반환합니다.
                    return CustomResult.Failure(addProjectToUserResult.error)
                }
                Log.d("CreateProjectUseCase", "4");

                // "카테고리 없음" 카테고리 자동 생성
                val noCategory = Category.create(
                    id = DocumentId(Constants.NO_CATEGORY_ID),
                    name = CategoryName.NO_CATEGORY_NAME,
                    order = CategoryOrder(Constants.NO_CATEGORY_ORDER),
                    createdBy = OwnerId(session.data.userId),
                    isCategory = IsCategoryFlag.FALSE,
                )
                // Firestore의 경우 projectId는 Category 객체에 포함되지 않고, 컬렉션 경로의 일부로 사용됨
                // 따라서 categoryRepository.addCategory(projectId, noCategory) 형태가 될 것임
                val addNoCategoryResult = categoryRepository.setDirectCategory(projectId, Constants.NO_CATEGORY_ID ,noCategory)
                if (addNoCategoryResult is CustomResult.Failure) {
                    // "카테고리 없음" 생성 실패 시 로그만 남기고 프로젝트 생성은 성공으로 간주
                    // 중요: 이 부분은 프로젝트 정책에 따라 롤백 처리 등을 고려해야 할 수 있음
                    Log.e("CreateProjectUseCase", "Failed to create default 'No Category' for project $projectId: ${addNoCategoryResult.error.message}")
                }
                Log.d("AddCategoryUseCaseImpl", "Category added with ID: ${noCategory}")

                Log.d("CreateProjectUseCase", "5");
                return CustomResult.Success(projectId)
            }
            else -> {
                return CustomResult.Failure(Exception("로그인이 필요합니다."))
            }
        }
    }
} 