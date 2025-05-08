package com.example.data.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Firebase 서비스를 제공하는 Hilt 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    /**
     * Firebase Authentication 서비스를 제공합니다.
     *
     * @return FirebaseAuth 인스턴스
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    /**
     * Firestore 서비스를 제공합니다.
     *
     * @return FirebaseFirestore 인스턴스
     */
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance("default")
    }

    /**
     * Firebase Storage 서비스를 제공합니다.
     *
     * @return FirebaseStorage 인스턴스
     */
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }
} 