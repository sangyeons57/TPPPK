package com.example.domain.provider.search

import com.example.domain.model.vo.CollectionPath
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.SearchRepository
import com.example.domain.repository.base.UserRepository
import com.example.domain.repository.factory.context.AuthRepositoryFactoryContext
import com.example.domain.repository.factory.context.SearchRepositoryFactoryContext
import com.example.domain.repository.factory.context.UserRepositoryFactoryContext
import com.example.domain.usecase.search.SearchUseCase
import com.example.domain.usecase.user.SearchUserByNameUseCase
import com.example.domain.usecase.user.SearchUsersByNameUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 검색 관련 UseCase들을 제공하는 Provider
 * 
 * 사용자 검색, 전체 검색 등의 기능을 담당합니다.
 */
@Singleton
class SearchUseCaseProvider @Inject constructor(
    private val searchRepositoryFactory: RepositoryFactory<SearchRepositoryFactoryContext, SearchRepository>,
    private val userRepositoryFactory: RepositoryFactory<UserRepositoryFactoryContext, UserRepository>,
    private val authRepositoryFactory: RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>
) {

    /**
     * 검색 관련 UseCase들을 생성합니다.
     * 
     * @return 검색 관련 UseCase 그룹
     */
    fun create(): SearchUseCases {
        val searchRepository = searchRepositoryFactory.create(
            SearchRepositoryFactoryContext(
                collectionPath = CollectionPath.search
            )
        )

        val userRepository = userRepositoryFactory.create(
            UserRepositoryFactoryContext(
                collectionPath = CollectionPath.users
            )
        )

        val authRepository = authRepositoryFactory.create(
            AuthRepositoryFactoryContext()
        )

        return SearchUseCases(
            searchUseCase = SearchUseCase(
                searchRepository = searchRepository,
                authRepository = authRepository
            ),
            
            searchUserByNameUseCase = SearchUserByNameUseCase(
                userRepository = userRepository
            ),
            
            searchUsersByNameUseCase = SearchUsersByNameUseCase(
                userRepository = userRepository
            ),
            
            // 공통 Repository
            authRepository = authRepository,
            searchRepository = searchRepository,
            userRepository = userRepository
        )
    }

    /**
     * 특정 컨텍스트에서의 검색 UseCase들을 생성합니다.
     * 
     * @param searchContext 검색 컨텍스트 (예: 프로젝트 내 검색, 전체 검색 등)
     * @return 검색 관련 UseCase 그룹
     */
    fun createForContext(searchContext: String): SearchUseCases {
        val searchRepository = searchRepositoryFactory.create(
            SearchRepositoryFactoryContext(
                collectionPath = CollectionPath.search // 컨텍스트별로 다른 경로 사용 가능
            )
        )

        val userRepository = userRepositoryFactory.create(
            UserRepositoryFactoryContext(
                collectionPath = CollectionPath.users
            )
        )

        val authRepository = authRepositoryFactory.create(
            AuthRepositoryFactoryContext()
        )

        return SearchUseCases(
            searchUseCase = SearchUseCase(
                searchRepository = searchRepository,
                authRepository = authRepository
            ),
            
            searchUserByNameUseCase = SearchUserByNameUseCase(
                userRepository = userRepository
            ),
            
            searchUsersByNameUseCase = SearchUsersByNameUseCase(
                userRepository = userRepository
            ),
            
            // 공통 Repository
            authRepository = authRepository,
            searchRepository = searchRepository,
            userRepository = userRepository
        )
    }
}

/**
 * 검색 관련 UseCase 그룹
 */
data class SearchUseCases(
    val searchUseCase: SearchUseCase,
    val searchUserByNameUseCase: SearchUserByNameUseCase,
    val searchUsersByNameUseCase: SearchUsersByNameUseCase,
    
    // 공통 Repository
    val authRepository: AuthRepository,
    val searchRepository: SearchRepository,
    val userRepository: UserRepository
)