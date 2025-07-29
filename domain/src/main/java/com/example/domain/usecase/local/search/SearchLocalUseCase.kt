package com.example.domain.usecase.local.search

import com.example.core_common.result.CustomResult
import com.example.domain.repository.local.LocalUserRepository
import com.example.domain.repository.local.MessageLocalRepository
import com.example.domain.repository.local.ProjectLocalRepository
import javax.inject.Inject

interface SearchLocalUseCase {
    suspend operator fun invoke(
        query: String,
        searchType: SearchType = SearchType.ALL
    ): CustomResult<SearchResult, Exception>
}

class SearchLocalUseCaseImpl @Inject constructor(
    private val localUserRepository: LocalUserRepository,
    private val projectLocalRepository: ProjectLocalRepository,
    private val messageLocalRepository: MessageLocalRepository
) : SearchLocalUseCase {

    override suspend operator fun invoke(
        query: String,
        searchType: SearchType
    ): CustomResult<SearchResult, Exception> {
        return TODO("로컬 저장소에서 사용자, 프로젝트, 메시지 등을 검색")
    }
} 