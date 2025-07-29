package com.example.domain.usecase.local.file

import com.example.domain.repository.remote.FileRepository
import javax.inject.Inject

interface CheckFileExistenceLocalUseCase {
    suspend operator fun invoke(path: String): Boolean
}

class CheckFileExistenceLocalUseCaseImpl @Inject constructor(
    private val fileLocalRepository: FileRepository
) : CheckFileExistenceLocalUseCase {

    override suspend operator fun invoke(path: String): Boolean {
        return TODO("로컬 저장소에서 파일 존재 여부 확인")
    }
} 