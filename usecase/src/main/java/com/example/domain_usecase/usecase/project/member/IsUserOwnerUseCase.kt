package com.example.domain_usecase.usecase.project.member

import com.example.core_common.result.CustomResult
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.ProjectRepository
import javax.inject.Inject

/**
 * 현재 사용자가 프로젝트 오너인지 확인하는 UseCase
 */
interface IsCurrentUserOwnerUseCase {
    suspend operator fun invoke(projectId: DocumentId): CustomResult<Boolean, Exception>
}

/**
 * 대상 사용자가 프로젝트 오너인지 확인하는 UseCase
 */
interface IsTargetMemberOwnerUseCase {
    suspend fun invoke(
        projectId: DocumentId,
        targetUserId: UserId
    ): CustomResult<Boolean, Exception>
}


/**
 * 현재 사용자가 프로젝트 오너인지 확인하는 UseCase 구현체
 */
class IsCurrentUserOwnerUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository
) : IsCurrentUserOwnerUseCase {

    override suspend fun invoke(projectId: DocumentId): CustomResult<Boolean, Exception> {
        return try {
            // 1) Get current user ID from session
            val session = authRepository.getCurrentUserSession()
            val userId = when (session) {
                is CustomResult.Success -> session.data.userId
                is CustomResult.Failure -> return CustomResult.Failure(session.error)
                is CustomResult.Initial -> return CustomResult.Initial
                is CustomResult.Loading -> return CustomResult.Loading
                is CustomResult.Progress -> return CustomResult.Progress(session.progress)
            }

            // 2) Check if current user is owner using shared helper
            checkOwnership(projectId, userId)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    private suspend fun checkOwnership(
        projectId: DocumentId,
        userId: UserId
    ): CustomResult<Boolean, Exception> {
        return when (val projectResult = projectRepository.findById(projectId)) {
            is CustomResult.Success -> {
                val isOwner = projectResult.data.ownerId.value == userId.value
                CustomResult.Success(isOwner)
            }

            is CustomResult.Failure -> {
                CustomResult.Failure(
                    Exception("Failed to get project information: ${projectResult.error}")
                )
            }

            else -> {
                CustomResult.Failure(Exception("Unexpected result when getting project"))
            }
        }
    }
}

/**
 * 대상 사용자가 프로젝트 오너인지 확인하는 UseCase 구현체
 */
class IsTargetMemberOwnerUseCaseImpl @Inject constructor(
    private val projectRepository: ProjectRepository
) : IsTargetMemberOwnerUseCase {

    override suspend fun invoke(
        projectId: DocumentId,
        targetUserId: UserId
    ): CustomResult<Boolean, Exception> {
        return try {
            when (val projectResult = projectRepository.findById(projectId)) {
                is CustomResult.Success -> {
                    val isOwner = projectResult.data.ownerId.value == targetUserId.value
                    CustomResult.Success(isOwner)
                }

                is CustomResult.Failure -> {
                    CustomResult.Failure(
                        Exception("Failed to get project information: ${projectResult.error}")
                    )
                }

                else -> {
                    CustomResult.Failure(Exception("Unexpected result when getting project"))
                }
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}