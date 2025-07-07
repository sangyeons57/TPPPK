package com.example.data.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Repository 관련 의존성을 Hilt에 바인딩하는 모듈입니다.
 * 팩토리 패턴이 필요하지 않은 단순한 Repository 구현체들을 바인딩합니다.
 * 
 * 모든 Repository는 각 도메인별로 분산되어 구현되었으므로 
 * 현재는 공통적으로 바인딩할 Repository가 없습니다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    // 모든 Repository들이 도메인별로 분산되어 Factory 패턴을 사용하므로
    // 여기서는 바인딩할 Repository가 없습니다.
}