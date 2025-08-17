package com.example.teamnovapersonalprojectprojectingkotlin

import android.app.ActivityManager
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
 * ANR 방지를 위해 무거운 작업은 백그라운드에서 실행합니다.
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

        // 1. 필수적인 동기 초기화만 먼저 수행
        initializeEssentialServices()

        // 2. 무거운 초기화는 백그라운드에서 지연 실행
        initializeHeavyServicesAsync()
    }

    /**
     * 필수적인 동기 초기화 작업
     * 메인 스레드에서 빠르게 완료되어야 하는 작업들
     */
    private fun initializeEssentialServices() {
        try {
            // Firebase 기본 초기화
            FirebaseApp.initializeApp(this)

            // 라이프사이클 옵저버 등록
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)

            // 알림 채널 초기화 (빠른 작업)
            initializeNotificationChannels()

            Log.d(TAG, "✅ Essential services initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize essential services", e)
        }
    }

    /**
     * 무거운 초기화 작업을 백그라운드에서 지연 실행
     * ANR 방지를 위해 비동기로 처리
     */
    private fun initializeHeavyServicesAsync() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "🚀 Starting heavy services initialization in background...")

                // 메모리 상태 로깅
                logMemoryUsage("Before heavy services initialization")

                // Firebase App Check 초기화
                initializeFirebaseAppCheck()

                // 잠시 대기 후 WebSocket 서비스 초기화
                kotlinx.coroutines.delay(1000)
                initializeGlobalWebSocketService()

                // FCM 토큰 관리 초기화
                kotlinx.coroutines.delay(500)
                initializeFcmTokenManager()

                // 메모리 상태 재확인
                logMemoryUsage("After heavy services initialization")

                Log.d(TAG, "✅ All heavy services initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to initialize heavy services", e)
            }
        }
    }

    /**
     * 메모리 사용량을 로그로 출력
     */
    private fun logMemoryUsage(context: String) {
        try {
            val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)

            val availableMemory = memoryInfo.availMem / (1024 * 1024) // MB
            val totalMemory = memoryInfo.totalMem / (1024 * 1024) // MB
            val usedMemory = totalMemory - availableMemory
            val memoryUsagePercent = (usedMemory.toFloat() / totalMemory * 100).toInt()

            Log.i(TAG, "📊 Memory Usage [$context]:")
            Log.i(TAG, "   Used: ${usedMemory}MB / ${totalMemory}MB (${memoryUsagePercent}%)")
            Log.i(TAG, "   Available: ${availableMemory}MB")
            Log.i(TAG, "   Low memory: ${memoryInfo.lowMemory}")

            // 메모리 압박이 높으면 경고
            if (memoryUsagePercent > 80) {
                Log.w(TAG, "⚠️ HIGH MEMORY USAGE: ${memoryUsagePercent}% - Consider optimizing")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get memory info", e)
        }
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
    private fun initializeFirebaseAppCheck() {
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

                // local.properties에 appCheckDebugSecret을 설정한 경우, 해당 값을 로그로 안내
                // app/build.gradle.kts에서 BuildConfig.APP_CHECK_DEBUG_SECRET을 debug에만 주입함
                val configuredSecret = BuildConfig.APP_CHECK_DEBUG_SECRET
                if (!configuredSecret.isNullOrBlank() && configuredSecret.lowercase() != "null") {
                    Log.w(
                        TAG,
                        "[AppCheck] Configured debug secret detected in BuildConfig.APP_CHECK_DEBUG_SECRET"
                    )
                    Log.w(
                        TAG,
                        "[AppCheck] Use this secret in Firebase Console (Manage debug tokens): $configuredSecret"
                    )
                }
                
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
