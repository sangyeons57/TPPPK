package com.example.domain.usecase.search

import com.example.domain.model.data.search.SearchResult
import com.example.domain.model.ui.search.MessageResult
import com.example.domain.model.ui.search.SearchResultItem
import com.example.domain.model.ui.search.SearchScope
import com.example.domain.model.ui.search.UserResult
import com.example.domain.repository.SearchRepository
import javax.inject.Inject

/**
 * 검색 기능을 수행하는 UseCase
 * 사용자가 입력한 검색어와 검색 범위에 따라 적절한 검색 결과를 반환합니다.
 * 데이터 모델에서 UI 모델로의 변환을 담당합니다.
 *
 * @property searchRepository 검색 기능을 제공하는 Repository
 */
class SearchUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    /**
     * 검색어와 검색 범위를 기반으로 검색을 수행합니다.
     * Repository에서 반환한 검색 결과를 UI에서 사용하는 모델로 변환합니다.
     *
     * @param query 검색어
     * @param scope 검색 범위
     * @return 변환된 UI 검색 결과 아이템 목록을 포함한 Result
     */
    suspend operator fun invoke(query: String, scope: SearchScope): Result<List<SearchResultItem>> {
        if (query.isBlank()) {
            return Result.success(emptyList())
        }

        return try {
            // SearchRepository를 통해 검색 수행
            val searchResultResponse = searchRepository.search(query, scope)
            
            searchResultResponse.fold(
                onSuccess = { results ->
                    // 데이터 모델에서 UI 모델로 변환
                    val uiResults = mapDataToUiModels(results, scope)
                    Result.success(uiResults)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 데이터 모델을 UI 모델로 변환하는 그룹 방법
     * 검색 범위에 따라 필터링도 수행
     * 
     * @param dataResults 데이터 모델 검색 결과
     * @param scope 검색 범위
     * @return UI 모델 검색 결과 목록
     */
    private fun mapDataToUiModels(dataResults: List<SearchResult>, scope: SearchScope): List<SearchResultItem> {
        // 검색 범위가 ALL이 아닐 경우 해당 타입에 맞는 결과만 보여줌
        val filteredResults = when (scope) {
            SearchScope.ALL -> dataResults
            SearchScope.MESSAGES -> dataResults.filterIsInstance<SearchResult.Message>()
            SearchScope.USERS -> dataResults.filterIsInstance<SearchResult.User>()
            SearchScope.PROJECTS -> dataResults.filterIsInstance<SearchResult.Project>()
        }
        
        // 필터링된 결과를 UI 모델로 변환
        return filteredResults.map { dataResult ->
            when (dataResult) {
                is SearchResult.Message -> MessageResult.fromDataModel(dataResult)
                is SearchResult.User -> UserResult.fromDataModel(dataResult)
                is SearchResult.Project -> throw NotImplementedError("Project results not yet implemented")
                // 프로젝트 검색 결과는 추후 구현
            }
        }
    }
}
