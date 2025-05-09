package com.example.data.repository

import com.example.domain.model.SearchResultItem
import com.example.domain.model.SearchScope
import com.example.domain.repository.SearchRepository
import javax.inject.Inject
import kotlin.Result

class SearchRepositoryImpl @Inject constructor(
    // TODO: SearchApiService 등 주입
) : SearchRepository {

    override suspend fun search(query: String, scope: SearchScope): Result<List<SearchResultItem>> {
        println("SearchRepositoryImpl: search called for '$query' in $scope (returning empty list)")
        return Result.success(emptyList())
    }
    // TODO: SearchRepository 인터페이스의 다른 함수들 구현
}