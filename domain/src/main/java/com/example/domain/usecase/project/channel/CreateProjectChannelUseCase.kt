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
class CreateProjectChannelUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(name: String, order: Double): CustomResult<Unit, Exception> {
        val session = when (val result = authRepository.getCurrentUserSession()){
            is CustomResult.Success -> result.data
            is CustomResult.Failure -> return CustomResult.Failure(result.error)
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(result.progress)
        }
        val category = Category.create(
            name = CategoryName(name),
            order = CategoryOrder(order),
            createdBy = OwnerId.from(session.userId),
        )

        return when ( val result = categoryRepository.save(category)) {
            is CustomResult.Success -> CustomResult.Success(Unit)
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }
} 