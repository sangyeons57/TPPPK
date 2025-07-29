package com.example.domain.usecase.local.media

import com.example.core_common.result.CustomResult
import com.example.domain.repository.local.MediaLocalRepository
import javax.inject.Inject

interface DeleteMediaLocalUseCase {
    suspend operator fun invoke(fileUrl: String): CustomResult<Unit, Exception>
}

class DeleteMediaLocalUseCaseImpl @Inject constructor(
    private val mediaLocalRepository: MediaLocalRepository
) : DeleteMediaLocalUseCase {

    override suspend operator fun invoke(fileUrl: String): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 지정된 URL의 미디어 파일을 삭제")
    }
} 