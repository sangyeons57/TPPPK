package com.example.data.di

import com.example.core_common.constants.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.functions.FirebaseFunctions
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
        // Firestore 인스턴스 가져오기
        val firestore = FirebaseFirestore.getInstance(Constants.DB_NAME) // "default"는 기본 FirebaseApp 인스턴스를 사용함을 의미

        val persistentCacheSettings = PersistentCacheSettings.newBuilder()
            .setSizeBytes(100 * 1024 * 1024)
            .build()

        // Firestore 설정 객체 생성 및 디스크 지속성 활성화
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(persistentCacheSettings)
            .build()
        
        firestore.firestoreSettings = settings
        
        return firestore
    }

    /**
     * Firebase Functions 서비스를 제공합니다.
     *
     * @return FirebaseFunctions 인스턴스
     */
    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions {
        return FirebaseFunctions.getInstance()
    }
} 