package com.example.domain.provider

import com.example.domain.repository.local.SearchLocalRepository
import com.example.domain.usecase.local.search.SearchLocalUseCase
import com.example.domain.usecase.local.search.SearchLocalUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 검색 기능 관련 Local UseCase들을 제공하는 Provider
 * 
 * 로컬 저장소를 기반으로 한 통합 검색 기능을 담당합니다.
 */
@Singleton
class SearchUseCaseProvider @Inject constructor(
    private val searchLocalRepository: SearchLocalRepository
) {

    /**
     * 검색 기능 관련 UseCase들을 생성합니다.
     * 
     * @return 검색 기능 UseCase 그룹
     */
    fun create(): SearchLocalUseCases {
        return SearchLocalUseCases(
            // 통합 검색
            searchLocalUseCase = SearchLocalUseCaseImpl(
                searchLocalRepository = searchLocalRepository
            ),
            
            searchLocalRepository = searchLocalRepository
        )
    }
}

/**
 * 검색 기능 Local UseCase 그룹
 */
data class SearchLocalUseCases(
    // 통합 검색
    val searchLocalUseCase: SearchLocalUseCase,
    
    val searchLocalRepository: SearchLocalRepository
) 