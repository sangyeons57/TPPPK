package com.example.teamnovapersonalprojectprojectingkotlin

import android.app.Application
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.teamnovapersonalprojectprojectingkotlin.initializer.FirebaseInitializer
import com.example.teamnovapersonalprojectprojectingkotlin.initializer.NotificationInitializer
import com.example.websocket.GlobalWebSocketService
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
 * 각 기능별 초기화는 별도의 Initializer 클래스에서 담당합니다.
 */
@HiltAndroidApp
class MyApp : Application() {

    private val TAG = "MyApp"
    
    @Inject
    lateinit var firebaseInitializer: FirebaseInitializer
    
    @Inject
    lateinit var notificationInitializer: NotificationInitializer
    
    @Inject
    lateinit var globalWebSocketService: GlobalWebSocketService
    
    private val applicationScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "🚀 Starting application initialization")

        // Initialize Firebase
        firebaseInitializer.initialize(this, applicationScope)
        
        // Initialize notification channels
        notificationInitializer.initializeNotificationChannels()

        // Register GlobalWebSocketService as lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(globalWebSocketService)

        // Initialize services asynchronously
        applicationScope.launch {
            // Initialize WebSocket service
            globalWebSocketService.initialize()

            // Initialize FCM token management
            notificationInitializer.initializeFcmTokenManager()
        }

        Log.d(TAG, "✅ Application initialization completed")
    }
    
    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "Application terminating - cleaning up resources")
        
        globalWebSocketService.stopService()
        notificationInitializer.cleanup()
    }
}