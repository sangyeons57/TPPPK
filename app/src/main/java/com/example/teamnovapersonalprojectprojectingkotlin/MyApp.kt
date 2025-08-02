package com.example.teamnovapersonalprojectprojectingkotlin

import android.app.Application
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.domain_usecase.provider.auth.AuthSessionUseCaseProvider
import com.example.teamnovapersonalprojectprojectingkotlin.fcm.FcmTokenManager
import com.example.teamnovapersonalprojectprojectingkotlin.notification.NotificationChannelManager
import com.example.websocket.core.WebSocketManager
import com.example.websocket.service.GlobalWebSocketService
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.take
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

    @Inject
    lateinit var fcmTokenManager: FcmTokenManager

    @Inject
    lateinit var notificationChannelManager: NotificationChannelManager
    
    private val applicationScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        appcheck()
        
        // Register lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Initialize notification channels
        initializeNotificationChannels()
        
        // Initialize global WebSocket service after DI is ready
        initializeGlobalWebSocketService()

        // Initialize FCM token management
        initializeFcmTokenManager()
    }

    private fun initializeNotificationChannels() {
        try {
            Log.d(TAG, "Initializing notification channels")
            notificationChannelManager.initializeChannels()
            Log.d(TAG, "Notification channels initialization completed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize notification channels", e)
        }
    }
    
    private fun initializeGlobalWebSocketService() {
        applicationScope.launch {
            try {
                Log.i(TAG, "🚀 === Starting GlobalWebSocketService Initialization ===")

                // Configure WebSocket server URL (서버 중심 저장 아키텍처)
                Log.d(TAG, "🌐 Configuring WebSocket server: ${WebSocketManager.SERVER_URL}")
                globalWebSocketService.configure()
                
                // Set up authentication monitoring
                Log.d(TAG, "🔑 Setting up authentication monitoring")
                val authUseCases = authSessionUseCaseProvider.create()
                val authStream = authUseCases.getCurrentUserSessionStreamUseCase()

                Log.d(TAG, "🔍 Initializing auth stream monitoring")
                globalWebSocketService.initializeWithAuth(authStream)
                
                // Start the service
                Log.d(TAG, "▶️ Starting GlobalWebSocketService")
                globalWebSocketService.startService()

                Log.i(TAG, "✅ GlobalWebSocketService initialization completed successfully")

                // Test auth stream immediately
                Log.d(TAG, "🧪 Testing initial auth state...")
                launch {
                    authStream.take(1).collect { authResult ->
                        Log.d(
                            TAG,
                            "📲 Initial auth stream emitted: ${authResult.javaClass.simpleName}"
                        )
                        when (authResult) {
                            is com.example.core_common.result.CustomResult.Success -> {
                                Log.i(TAG, "✅ Initial auth success detected - user is logged in")
                            }

                            is com.example.core_common.result.CustomResult.Failure -> {
                                Log.w(TAG, "⚠️ Initial auth failure detected - user needs to login")
                            }

                            else -> {
                                Log.d(
                                    TAG,
                                    "🔄 Initial auth state: ${authResult.javaClass.simpleName}"
                                )
                            }
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to initialize GlobalWebSocketService", e)
                Log.e(TAG, "Error details: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun initializeFcmTokenManager() {
        applicationScope.launch {
            try {
                Log.d(TAG, "Initializing FCM token management")
                fcmTokenManager.initialize()
                Log.d(TAG, "FCM token management initialization completed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize FCM token management", e)
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
        fcmTokenManager.cleanup()
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