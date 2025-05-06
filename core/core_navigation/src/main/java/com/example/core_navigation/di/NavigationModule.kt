package com.example.core_navigation.di

import android.content.Context
import com.example.core_navigation.core.ComposeNavigationHandler
import com.example.core_navigation.core.NavigationHandler
import com.example.core_navigation.core.NavigationManager
import com.example.core_navigation.core.NavigationResultListener
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 네비게이션 관련 의존성을 제공하는 Hilt 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NavigationModule {
    
    /**
     * 통합된 NavigationManager를 ComposeNavigationHandler로 바인딩
     */
    @Binds
    @Singleton
    abstract fun bindComposeNavigationHandler(
        manager: NavigationManager
    ): ComposeNavigationHandler
    
    /**
     * 통합된 NavigationManager를 NavigationHandler로 바인딩
     */
    @Binds
    @Singleton
    abstract fun bindNavigationHandler(
        manager: NavigationManager
    ): NavigationHandler
    
    /**
     * 통합된 NavigationManager를 NavigationResultListener로 바인딩
     */
    @Binds
    @Singleton
    abstract fun bindNavigationResultListener(
        manager: NavigationManager
    ): NavigationResultListener
    
    // NavigationManager 자체는 @Singleton @Inject constructor()를 가지므로
    // Hilt가 자동으로 인스턴스를 생성하고 관리합니다.
}

// companion object를 제거하거나 내부의 불필요한 @Provides 함수들을 제거합니다.
// @Module
// @InstallIn(SingletonComponent::class)
// object NavigationProviderModule { // 필요하다면 companion object 대신 별도 모듈 사용
// 
//     @Provides
//     @Singleton
//     fun provideNavigationManagerInstance( /* 필요한 의존성 */ ): NavigationManager {
//         // 만약 NavigationManager의 생성자 주입이 불가능한 경우 여기서 직접 생성
//         return NavigationManager( /* 의존성 주입 */ )
//     }
// } 