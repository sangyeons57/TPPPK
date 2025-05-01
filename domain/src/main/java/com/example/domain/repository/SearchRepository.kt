// 경로: domain/repository/SearchRepository.kt (신규 생성)
package com.example.teamnovapersonalprojectprojectingkotlin.domain.repository

import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.SearchScope
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.SearchResultItem
import kotlin.Result

interface SearchRepository {
    /**
     * 지정된 범위 내에서 검색어를 사용하여 검색 수행
     * @param query 검색어
     * @param scope 검색 범위
     * @return 검색 결과 리스트 (성공/실패 포함)
     */
    suspend fun search(query: String, scope: SearchScope): Result<List<SearchResultItem>>
}