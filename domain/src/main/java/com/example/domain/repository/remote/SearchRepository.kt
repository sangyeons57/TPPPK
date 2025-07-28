package com.example.domain.repository.remote

import com.example.domain.model.data.search.SearchResult
import com.example.domain.model.ui.search.SearchScope
import com.example.domain.repository.remote.Repository

/**
 * Search Repository Interface (Firebase Functions Only)
 * Firebase Functions를 통한 검색 서비스 전용
 */
interface SearchRepository : Repository {
    /**
     * 주어진 검색어와 범위에 따라 검색을 수행합니다.
     */
    suspend fun search(query: String, scope: SearchScope): Result<List<SearchResult>>
}
