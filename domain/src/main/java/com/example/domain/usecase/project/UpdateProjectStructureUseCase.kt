package com.example.domain.usecase.project

import com.example.domain.model.ProjectStructure
import com.example.domain.repository.ProjectStructureRepository
import javax.inject.Inject

/**
 * 프로젝트 구조를 업데이트하는 유스케이스
 * 카테고리 및 채널의 추가/삭제/수정/순서 변경 등 전체 프로젝트 구조 변경 사항을 저장합니다.
 */
class UpdateProjectStructureUseCase @Inject constructor(
    private val projectStructureRepository: ProjectStructureRepository
) {
    /**
     * 프로젝트 구조를 업데이트합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param projectStructure 업데이트할 프로젝트 구조
     * @return 작업 결과
     */
    suspend operator fun invoke(projectId: String, projectStructure: ProjectStructure): Result<Unit> {
        return projectStructureRepository.updateProjectStructure(projectId, projectStructure)
    }
} 