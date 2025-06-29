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
import com.example.domain.usecase.user.SearchUserByNameUseCaseImpl
import com.example.domain.usecase.user.SearchUsersByNameUseCase
import com.example.domain.usecase.user.SearchUsersByNameUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 검색 관련 UseCase들을 제공하는 Provider
 * 
 * 사용자 검색, 전체 검색 등의 기능을 담당합니다.
 */
@Singleton
class SearchUseCaseProvider @Inject constructor(
    private val searchRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<SearchRepositoryFactoryContext, SearchRepository>,
    private val userRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<UserRepositoryFactoryContext, UserRepository>,
    private val authRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>
) {

    /**
     * 검색 관련 UseCase들을 생성합니다.
     * 
     * @return 검색 관련 UseCase 그룹
     */
    fun create(): SearchUseCases {
        val searchRepository = searchRepositoryFactory.create(
            SearchRepositoryFactoryContext()
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
                searchRepository = searchRepository
            ),
            
            searchUserByNameUseCase = SearchUserByNameUseCaseImpl(
                userRepository = userRepository
            ),
            
            searchUsersByNameUseCase = SearchUsersByNameUseCaseImpl(
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
            SearchRepositoryFactoryContext()
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
                searchRepository = searchRepository
            ),
            
            searchUserByNameUseCase = SearchUserByNameUseCaseImpl(
                userRepository = userRepository
            ),
            
            searchUsersByNameUseCase = SearchUsersByNameUseCaseImpl(
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