package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.base.Project
import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.ProjectRepositoryFactoryContext
import kotlinx.coroutines.flow.Flow

/**
 * 프로젝트 생성, 조회, 관리 등 관련 데이터 처리를 위한 인터페이스입니다.
 */
interface ProjectRepository : DefaultRepository {
    override val factoryContext: ProjectRepositoryFactoryContext

}
