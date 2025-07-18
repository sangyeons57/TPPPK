package com.example.domain.usecase.task

import com.example.core_common.result.CustomResult

/**
 * Task 순서를 변경하는 UseCase
 */
interface ReorderTaskUseCase {
    /**
     * 특정 Task의 순서를 변경합니다.
     * 
     * @param taskId 순서를 변경할 Task의 ID
     * @param newOrder 새로운 순서 값
     * @return 성공 시 Unit, 실패 시 Exception
     */
    suspend operator fun invoke(
        taskId: String,
        newOrder: Int
    ): CustomResult<Unit, Exception>
}