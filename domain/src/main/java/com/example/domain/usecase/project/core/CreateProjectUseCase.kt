package com.example.domain.usecase.project.core

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.CategoryRepository // Added
import com.example.domain.repository.base.ProjectRepository
import com.example.domain.repository.base.ProjectsWrapperRepository
import com.example.domain.model.base.Category // Added
import com.example.core_common.constants.Constants // Added
import com.example.core_common.result.resultTry
import com.example.domain.model.base.Member
import com.example.domain.model.base.Project
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.model.base.Role
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.category.CategoryName
import com.example.domain.model.vo.category.CategoryOrder
import com.example.domain.model.vo.category.IsCategoryFlag
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.repository.Repository
import com.example.domain.repository.base.MemberRepository
import com.example.domain.repository.base.ProjectRoleRepository
import com.example.domain.repository.factory.context.ProjectsWrapperRepositoryFactoryContext
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
    private val categoryRepository: CategoryRepository, // Added
    private val memberRepository: MemberRepository,
    private val roleRepository: ProjectRoleRepository
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
    suspend operator fun invoke(name: String): CustomResult<DocumentId, Exception> {
        val trimmedName = name.trim()

        // 프로젝트 이름 유효성 검증
        if (trimmedName.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("프로젝트 이름은 필수 입력값입니다."))
        }
        
        // 프로젝트 이름 길이 검증 (2~30자)
        if (trimmedName.length < 2 || trimmedName.length > 30) {
            return CustomResult.Failure(IllegalArgumentException("프로젝트 이름은 2~30자 사이여야 합니다."))
        }


        // 모든 검증 통과 시 프로젝트 생성
        when (val session = authRepository.getCurrentUserSession()) {
            is CustomResult.Success -> {
                val project = Project.create(
                    name = ProjectName(trimmedName),
                    imageUrl = null,
                    ownerId = OwnerId.from(session.data.userId)
                )
                val createdProjectId = when (val projectIdResult = projectRepository.save(project)){
                    is CustomResult.Success -> projectIdResult.data
                    is CustomResult.Failure -> return CustomResult.Failure(projectIdResult.error)
                    is CustomResult.Initial -> return CustomResult.Initial
                    is CustomResult.Loading -> return CustomResult.Loading
                    is CustomResult.Progress -> return CustomResult.Progress(projectIdResult.progress)
                }

                // 생성된 프로젝트에 현재 사용자를 참여시킵니다.
                Log.d("CreateProjectUseCase", "createdProjectId: ${createdProjectId.value}")
                val context = roleRepository.factoryContext.changeCollectionPath(createdProjectId.value)
                Log.d("CreateProjectUseCase", "context: ${context.collectionPath.value}")
                val roleId = when (val roleResult = roleRepository.save(Role.createOwner())){
                    is CustomResult.Success -> roleResult.data
                    is CustomResult.Failure -> return CustomResult.Failure(roleResult.error)
                    is CustomResult.Initial -> return CustomResult.Initial
                    is CustomResult.Loading -> return CustomResult.Loading
                    is CustomResult.Progress -> return CustomResult.Progress(roleResult.progress)
                }

                memberRepository.factoryContext.changeCollectionPath(createdProjectId.value)
                val owner : Member = Member.create(
                    id = DocumentId.from(session.data.userId),
                    roleIds = listOf(roleId)
                )
                Log.d("CreateProjectUseCase", "context: ${memberRepository.factoryContext.collectionPath.value}")
                when (val memberResult = memberRepository.save(owner)) {
                    is CustomResult.Success -> {
                        // Member created successfully: $memberResult
                    }
                    is CustomResult.Failure -> return CustomResult.Failure(memberResult.error)
                    is CustomResult.Initial -> return CustomResult.Initial
                    is CustomResult.Loading -> return CustomResult.Loading
                    is CustomResult.Progress -> return CustomResult.Progress(memberResult.progress)
                }

                projectsWrapperRepository.factoryContext.changeCollectionPath(session.data.userId)
                val wrapper = ProjectsWrapper.create(
                    id = createdProjectId,
                    projectName = ProjectName(trimmedName),
                )
                when (val wrapperResult = projectsWrapperRepository.save(wrapper)){
                    is CustomResult.Success -> {
                        // ProjectsWrapper created successfully: $wrapperResult
                    }
                    is CustomResult.Failure -> return CustomResult.Failure(wrapperResult.error)
                    is CustomResult.Initial -> return CustomResult.Initial
                    is CustomResult.Loading -> return CustomResult.Loading
                    is CustomResult.Progress -> return CustomResult.Progress(wrapperResult.progress)
                }


                categoryRepository.factoryContext.changeCollectionPath(createdProjectId.value)
                val noCategory = Category.createNoCategory(OwnerId.from(session.data.userId))
                // Firestore의 경우 projectId는 Category 객체에 포함되지 않고, 컬렉션 경로의 일부로 사용됨
                // 따라서 categoryRepository.addCategory(projectId, noCategory) 형태가 될 것임
                return when(val addNoCategoryResult = categoryRepository.save(noCategory)) {
                    is CustomResult.Success -> CustomResult.Success(addNoCategoryResult.data)
                    is CustomResult.Failure -> CustomResult.Failure(addNoCategoryResult.error)
                    is CustomResult.Initial -> CustomResult.Initial
                    is CustomResult.Loading -> CustomResult.Loading
                    is CustomResult.Progress -> CustomResult.Progress(addNoCategoryResult.progress)
                }
            }
            is CustomResult.Failure -> return CustomResult.Failure(session.error)
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(session.progress)
        }
    }
} 