package com.example.domain.di

import com.example.domain.repository.AuthRepository
import com.example.domain.repository.UserRepository
import com.example.domain.usecase.user.WithdrawUserUseCase
import com.example.domain.usecase.user.WithdrawUserUseCaseImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideWithdrawUserUseCase(
        authRepository: AuthRepository,
        userRepository: UserRepository
    ): WithdrawUserUseCase {
        return WithdrawUserUseCaseImpl(authRepository, userRepository)
    }
}
