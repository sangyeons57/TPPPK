package com.example.domain.repository.base

import com.example.domain.model.data.search.SearchResult
import com.example.domain.model.ui.search.SearchScope
import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.Repository

/**
 * 검색 관련 기능을 제공하는 Repository 인터페이스
 * 다양한 범위(메시지, 사용자 등)에서 검색 기능을 수행합니다.
 */
interface SearchRepository : Repository {
    /**
     * 주어진 검색어와 범위에 따라 검색을 수행합니다.
     *
     * @param query 검색어
     * @param scope 검색 범위
     * @return 검색 결과 목록
     */
    suspend fun search(query: String, scope: SearchScope): Result<List<SearchResult>>
}
