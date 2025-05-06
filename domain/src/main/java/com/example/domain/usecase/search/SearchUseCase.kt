package com.example.domain.usecase.search

import com.example.domain.model.SearchResultItem
import com.example.domain.model.SearchScope
import com.example.domain.repository.SearchRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 검색 기능을 제공하는 UseCase
 * 
 * @property searchRepository 검색 관련 기능을 제공하는 Repository
 */
class SearchUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    /**
     * 주어진 검색어와 범위에 따라 검색을 수행합니다.
     *
     * @param query 검색어
     * @param scope 검색 범위
     * @return 성공 시 검색 결과 목록이 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(query: String, scope: SearchScope): Result<List<SearchResultItem>> {
        return searchRepository.search(query, scope)
    }
} 