package com.example.data.di

import com.example.core_common.constants.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.LocalCacheSettings
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.tasks.await
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
        val firestore = FirebaseFirestore.getInstance(Constants.DB_NAME)

        // 캐시 완전 비활성화
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false)
            .build()
        
        firestore.firestoreSettings = settings

        return firestore
    }

    /**
     * Firebase Functions 서비스를 제공합니다.
     * 프로젝트에서 정의된 REGION을 사용하여 설정합니다.
     *
     * @return FirebaseFunctions 인스턴스
     */
    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions {
        val functions = FirebaseFunctions.getInstance(Constants.REGION)

        // 개발 환경에서는 에뮬레이터 사용 (필요시 주석 처리)
        // functions.useFunctionsEmulator("localhost", 5001)

        return functions
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