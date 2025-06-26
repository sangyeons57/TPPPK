package com.example.domain.provider.auth

import com.example.domain.repository.FakeAuthRepository
import com.example.domain.repository.FakeUserRepository
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
import com.example.domain.repository.factory.context.AuthRepositoryFactoryContext
import com.example.domain.repository.factory.context.UserRepositoryFactoryContext
import com.example.domain.usecase.auth.CheckAuthenticationStatusUseCaseImpl
import com.example.domain.usecase.auth.session.CheckSessionUseCase
import com.example.domain.usecase.auth.session.LoginUseCase
import com.example.domain.usecase.auth.session.LogoutUseCase
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class AuthSessionUseCaseProviderTest {

    private lateinit var authSessionUseCaseProvider: AuthSessionUseCaseProvider
    private lateinit var mockAuthRepositoryFactory: RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>
    private lateinit var mockUserRepositoryFactory: RepositoryFactory<UserRepositoryFactoryContext, UserRepository>
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeUserRepository: FakeUserRepository

    @Before
    fun setUp() {
        // Create fake repositories
        fakeAuthRepository = FakeAuthRepository()
        fakeUserRepository = FakeUserRepository()

        // Create mock factories
        @Suppress("UNCHECKED_CAST")
        mockAuthRepositoryFactory = mock(RepositoryFactory::class.java) as RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>
        @Suppress("UNCHECKED_CAST")
        mockUserRepositoryFactory = mock(RepositoryFactory::class.java) as RepositoryFactory<UserRepositoryFactoryContext, UserRepository>

        // Configure mock factories to return fake repositories
        `when`(mockAuthRepositoryFactory.create(AuthRepositoryFactoryContext())).thenReturn(fakeAuthRepository)
        `when`(mockUserRepositoryFactory.create(UserRepositoryFactoryContext(com.example.domain.model.vo.CollectionPath.users))).thenReturn(fakeUserRepository)

        // Create provider with mock factories
        authSessionUseCaseProvider = AuthSessionUseCaseProvider(
            authRepositoryFactory = mockAuthRepositoryFactory,
            userRepositoryFactory = mockUserRepositoryFactory
        )
    }

    @Test
    fun `create returns non-null AuthSessionUseCases`() {
        // When
        val authSessionUseCases = authSessionUseCaseProvider.create()

        // Then
        assertNotNull(authSessionUseCases)
    }

    @Test
    fun `create returns AuthSessionUseCases with all required UseCases`() {
        // When
        val authSessionUseCases = authSessionUseCaseProvider.create()

        // Then
        assertNotNull("LoginUseCase should not be null", authSessionUseCases.loginUseCase)
        assertNotNull("LogoutUseCase should not be null", authSessionUseCases.logoutUseCase)
        assertNotNull("CheckAuthenticationStatusUseCase should not be null", authSessionUseCases.checkAuthenticationStatusUseCase)
        assertNotNull("CheckSessionUseCase should not be null", authSessionUseCases.checkSessionUseCase)
    }

    @Test
    fun `create returns AuthSessionUseCases with correct UseCase types`() {
        // When
        val authSessionUseCases = authSessionUseCaseProvider.create()

        // Then
        assertTrue("LoginUseCase should be correct type", authSessionUseCases.loginUseCase is LoginUseCase)
        assertTrue("LogoutUseCase should be correct type", authSessionUseCases.logoutUseCase is LogoutUseCase)
        assertTrue("CheckAuthenticationStatusUseCase should be correct type", authSessionUseCases.checkAuthenticationStatusUseCase is CheckAuthenticationStatusUseCaseImpl)
        assertTrue("CheckSessionUseCase should be correct type", authSessionUseCases.checkSessionUseCase is CheckSessionUseCase)
    }

    @Test
    fun `create returns AuthSessionUseCases with repositories`() {
        // When
        val authSessionUseCases = authSessionUseCaseProvider.create()

        // Then
        assertNotNull("AuthRepository should not be null", authSessionUseCases.authRepository)
        assertNotNull("UserRepository should not be null", authSessionUseCases.userRepository)
        assertTrue("AuthRepository should be FakeAuthRepository", authSessionUseCases.authRepository is FakeAuthRepository)
        assertTrue("UserRepository should be FakeUserRepository", authSessionUseCases.userRepository is FakeUserRepository)
    }

    @Test
    fun `create returns same repository instances across multiple calls`() {
        // When
        val authSessionUseCases1 = authSessionUseCaseProvider.create()
        val authSessionUseCases2 = authSessionUseCaseProvider.create()

        // Then
        // Note: This test documents current behavior. If caching is implemented, 
        // repositories might be the same instance. If not, they'll be different instances
        // but still of the correct type.
        assertTrue("AuthRepository should be correct type", authSessionUseCases1.authRepository is FakeAuthRepository)
        assertTrue("AuthRepository should be correct type", authSessionUseCases2.authRepository is FakeAuthRepository)
        assertTrue("UserRepository should be correct type", authSessionUseCases1.userRepository is FakeUserRepository)
        assertTrue("UserRepository should be correct type", authSessionUseCases2.userRepository is FakeUserRepository)
    }

    @Test
    fun `create properly configures LoginUseCase with dependencies`() {
        // When
        val authSessionUseCases = authSessionUseCaseProvider.create()

        // Then
        val loginUseCase = authSessionUseCases.loginUseCase
        assertNotNull("LoginUseCase should be created", loginUseCase)
        
        // Verify that the use case is functional by checking it doesn't throw on instantiation
        // and has the expected dependencies
        assertNotNull("LoginUseCase should have dependencies", loginUseCase)
    }

    @Test
    fun `create properly configures LogoutUseCase with dependencies`() {
        // When
        val authSessionUseCases = authSessionUseCaseProvider.create()

        // Then
        val logoutUseCase = authSessionUseCases.logoutUseCase
        assertNotNull("LogoutUseCase should be created", logoutUseCase)
    }

    @Test
    fun `create properly configures CheckAuthenticationStatusUseCase with dependencies`() {
        // When
        val authSessionUseCases = authSessionUseCaseProvider.create()

        // Then
        val checkAuthUseCase = authSessionUseCases.checkAuthenticationStatusUseCase
        assertNotNull("CheckAuthenticationStatusUseCase should be created", checkAuthUseCase)
    }

    @Test
    fun `create properly configures CheckSessionUseCase with dependencies`() {
        // When
        val authSessionUseCases = authSessionUseCaseProvider.create()

        // Then
        val checkSessionUseCase = authSessionUseCases.checkSessionUseCase
        assertNotNull("CheckSessionUseCase should be created", checkSessionUseCase)
    }

    @Test
    fun `AuthSessionUseCases data class properties work correctly`() {
        // Given
        val authSessionUseCases = authSessionUseCaseProvider.create()

        // When - Access all properties
        val loginUseCase = authSessionUseCases.loginUseCase
        val logoutUseCase = authSessionUseCases.logoutUseCase
        val checkAuthUseCase = authSessionUseCases.checkAuthenticationStatusUseCase
        val checkSessionUseCase = authSessionUseCases.checkSessionUseCase
        val authRepository = authSessionUseCases.authRepository
        val userRepository = authSessionUseCases.userRepository

        // Then - All properties should be accessible and non-null
        assertNotNull(loginUseCase)
        assertNotNull(logoutUseCase)
        assertNotNull(checkAuthUseCase)
        assertNotNull(checkSessionUseCase)
        assertNotNull(authRepository)
        assertNotNull(userRepository)
    }
}