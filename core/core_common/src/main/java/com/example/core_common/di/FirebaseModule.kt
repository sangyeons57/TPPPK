package com.example.core_common.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.persistentCacheSettings
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Firebase 관련 의존성을 제공하는 Hilt 모듈입니다.
 * 공통 모듈에 위치하여 다른 모듈에서도 접근 가능합니다.
 */
@Module
@InstallIn(SingletonComponent::class) // 애플리케이션 전역 범위에서 의존성 제공
object FirebaseModule {

    /**
     * FirebaseFirestore 인스턴스를 싱글톤으로 제공합니다.
     * 명시적으로 "default" 데이터베이스를 사용하고 오프라인 지속성을 활성화합니다.
     */
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance("default")
        // 오프라인 지속성 활성화 설정
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                persistentCacheSettings {
                    setSizeBytes(100 * 1024 * 1024)
                }
            )
            .build()
        firestore.firestoreSettings = settings
        return firestore
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return Firebase.auth
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return Firebase.storage
    }
} 