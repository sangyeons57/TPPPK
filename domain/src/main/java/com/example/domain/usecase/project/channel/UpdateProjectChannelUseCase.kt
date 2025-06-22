package com.example.domain.usecase.project.channel

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.category.CategoryName
import com.example.domain.model.vo.category.CategoryOrder
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.CategoryRepository
import javax.inject.Inject

/**
 * 프로젝트의 특정 카테고리 내에 채널을 생성하는 UseCase입니다.
 *
 */
class UpdateProjectChannelUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(name: String, order: Double): CustomResult<Unit, Exception> {
        TODO("Not yet implemented")
    }
} 