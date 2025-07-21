package com.example.teamnovapersonalprojectprojectingkotlin

import android.app.Application
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.example.core_common.websocket.GlobalWebSocketService
import com.example.domain.provider.auth.AuthSessionUseCaseProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * 애플리케이션 초기화 클래스
 * 
 * 앱이 처음 시작될 때 필요한 초기화 작업을 수행합니다.
 */
@HiltAndroidApp
class MyApp : Application(), LifecycleObserver {

    private val TAG = "MyApp"
    
    @Inject
    lateinit var globalWebSocketService: GlobalWebSocketService
    
    @Inject
    lateinit var authSessionUseCaseProvider: AuthSessionUseCaseProvider
    
    private val applicationScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        appcheck()
        
        // Register lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        
        // Initialize global WebSocket service after DI is ready
        initializeGlobalWebSocketService()
    }
    
    private fun initializeGlobalWebSocketService() {
        applicationScope.launch {
            try {
                Log.d(TAG, "Initializing GlobalWebSocketService")
                
                // Configure WebSocket server URL
                val serverUrl = "wss://websocket-chat-wizwlraydq-du.a.run.app/chat"
                globalWebSocketService.configure(serverUrl)
                
                // Set up authentication monitoring
                val authUseCases = authSessionUseCaseProvider.create()
                globalWebSocketService.initializeWithAuth(
                    authUseCases.getCurrentUserSessionStreamUseCase()
                )
                
                // Start the service
                globalWebSocketService.startService()
                
                Log.d(TAG, "GlobalWebSocketService initialization completed")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize GlobalWebSocketService", e)
            }
        }
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        globalWebSocketService.onAppForegrounded()
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        globalWebSocketService.onAppBackgrounded()
    }
    
    override fun onTerminate() {
        super.onTerminate()
        globalWebSocketService.stopService()
    }
    
    /**
     * Firebase App Check을 초기화합니다.
     * 2024-2025 Firebase 요구사항에 맞게 구성되었습니다.
     */
    private fun appcheck() {
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
                // 이렇게 하면 로그에 실제 디버그 토큰이 출력됩니다
                applicationScope.launch {
                    try {
                        Log.d(TAG, "Triggering App Check token generation...")
                        firebaseAppCheck.getAppCheckToken(false).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d(TAG, "App Check token generated successfully")
                            } else {
                                Log.w(TAG, "Failed to get App Check token - this will trigger debug token logging", task.exception)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error triggering App Check token generation", e)
                    }
                }
            } else {
                Log.d(TAG, "Release build detected - Debug provider NOT installed")
                // TODO: Production에서는 Play Integrity Provider 사용
                // firebaseAppCheck.installAppCheckProviderFactory(
                //     PlayIntegrityAppCheckProviderFactory.getInstance()
                // )
            }

        } catch (e: IllegalStateException) {
            Log.e(TAG, "FirebaseApp is not initialized. Ensure google-services.json is correct " +
                    "or call FirebaseApp.initializeApp() explicitly if needed.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase App Check", e)
        }
    }
}