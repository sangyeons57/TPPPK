package com.example.teamnovapersonalprojectprojectingkotlin.initializer

import android.content.Context
import android.util.Log
import com.example.teamnovapersonalprojectprojectingkotlin.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase 관련 초기화를 담당하는 클래스
 */
@Singleton
class FirebaseInitializer @Inject constructor() {

    private val TAG = "FirebaseInitializer"

    /**
     * Firebase를 초기화합니다.
     */
    fun initialize(context: Context, applicationScope: CoroutineScope) {
        // Firebase App 초기화
        FirebaseApp.initializeApp(context)

        // App Check 초기화
        initializeAppCheck(applicationScope)
    }

    /**
     * Firebase App Check을 초기화합니다.
     * 2024-2025 Firebase 요구사항에 맞게 구성되었습니다.
     */
    private fun initializeAppCheck(applicationScope: CoroutineScope) {
        try {
            Log.d(TAG, "Starting Firebase App Check initialization...")

            val firebaseAppCheck = FirebaseAppCheck.getInstance()

            // Debug 빌드에서만 Debug Provider 사용
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Installing DebugAppCheckProviderFactory for debug build")
                Log.w(TAG, "=== FIREBASE APP CHECK DEBUG MODE ===")
                Log.w(TAG, "앱을 실행한 후 로그에서 다음과 같은 메시지를 찾으세요:")
                Log.w(TAG, "'Enter this debug secret into the allow list in the Firebase Console'")
                Log.w(TAG, "해당 토큰을 Firebase Console > App Check > Manage debug tokens에 등록하세요")
                Log.w(TAG, "=====================================")

                firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG, "DebugAppCheckProviderFactory installed successfully")

                // Firebase 서비스 호출을 통해 디버그 토큰 생성 유도
                triggerDebugTokenGeneration(firebaseAppCheck, applicationScope)
            } else {
                Log.d(TAG, "Release build detected - Debug provider NOT installed")
                // TODO: Production에서는 Play Integrity Provider 사용
                // firebaseAppCheck.installAppCheckProviderFactory(
                //     PlayIntegrityAppCheckProviderFactory.getInstance()
                // )
            }

        } catch (e: IllegalStateException) {
            Log.e(
                TAG, "FirebaseApp is not initialized. Ensure google-services.json is correct " +
                        "or call FirebaseApp.initializeApp() explicitly if needed.", e
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase App Check", e)
        }
    }

    private fun triggerDebugTokenGeneration(
        firebaseAppCheck: FirebaseAppCheck,
        applicationScope: CoroutineScope
    ) {
        // 이렇게 하면 로그에 실제 디버그 토큰이 출력됩니다
        applicationScope.launch {
            try {
                Log.d(TAG, "Triggering App Check token generation...")
                firebaseAppCheck.getAppCheckToken(false).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "App Check token generated successfully")
                    } else {
                        Log.w(
                            TAG,
                            "Failed to get App Check token - this will trigger debug token logging",
                            task.exception
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error triggering App Check token generation", e)
            }
        }
    }
}