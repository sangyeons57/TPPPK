package com.example.core_ui.dialogs.di

import com.example.core_ui.dialogs.adapter.ProjectStructureToDraggableItemsAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import com.example.domain.usecase.project.ConvertProjectStructureToDraggableItemsUseCase

/**
 * 다이얼로그 관련 의존성을 제공하는 Hilt 모듈
 * 
 * 이 모듈은 다이얼로그 관련 어댑터 및 서비스를 제공합니다.
 */
@Module
@InstallIn(ViewModelComponent::class)
object DialogsModule {
    
    /**
     * ProjectStructureToDraggableItemsAdapter를 제공합니다.
     * 
     * @param useCase 도메인 레이어의 ConvertProjectStructureToDraggableItemsUseCase
     * @return ProjectStructureToDraggableItemsAdapter 인스턴스
     */
    @Provides
    @ViewModelScoped
    fun provideProjectStructureToDraggableItemsAdapter(
        useCase: ConvertProjectStructureToDraggableItemsUseCase
    ): ProjectStructureToDraggableItemsAdapter {
        return ProjectStructureToDraggableItemsAdapter(useCase)
    }
}
