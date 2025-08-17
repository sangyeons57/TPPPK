package com.example.domain_usecase.usecase.project.assets

import com.example.core_common.result.CustomResult
import com.example.domain.vo.DocumentId

/**
 * 프로젝트 프로필 이미지 존재 여부를 확인하는 UseCase
 */
interface CheckProjectProfileImageExistsUseCase {
    /**
     * 고정 경로(project_profile/{projectId}/profile.webp)에 프로젝트 이미지가 존재하는지 확인합니다.
     *
     * @param projectId 프로젝트 ID
     * @return 이미지 존재 여부 (true: 존재, false: 존재하지 않음)
     */
    suspend operator fun invoke(projectId: DocumentId): CustomResult<Boolean, Exception>
}