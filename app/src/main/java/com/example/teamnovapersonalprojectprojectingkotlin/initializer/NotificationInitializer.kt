package com.example.teamnovapersonalprojectprojectingkotlin.initializer

import android.util.Log
import com.example.teamnovapersonalprojectprojectingkotlin.fcm.FcmTokenManager
import com.example.teamnovapersonalprojectprojectingkotlin.notification.NotificationChannelManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FCM 토큰 관리 및 알림 채널 초기화를 담당하는 클래스
 */
@Singleton
class NotificationInitializer @Inject constructor(
    private val fcmTokenManager: FcmTokenManager,
    private val notificationChannelManager: NotificationChannelManager
) {

    private val TAG = "NotificationInitializer"

    /**
     * 알림 채널을 초기화합니다.
     */
    fun initializeNotificationChannels() {
        try {
            Log.d(TAG, "Initializing notification channels")
            notificationChannelManager.initializeChannels()
            Log.d(TAG, "Notification channels initialization completed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize notification channels", e)
        }
    }

    /**
     * FCM 토큰 관리를 초기화합니다.
     */
    suspend fun initializeFcmTokenManager() {
        try {
            Log.d(TAG, "Initializing FCM token management")
            fcmTokenManager.initialize()
            Log.d(TAG, "FCM token management initialization completed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FCM token management", e)
        }
    }

    /**
     * FCM 관련 리소스를 정리합니다.
     */
    fun cleanup() {
        try {
            Log.d(TAG, "Cleaning up FCM token manager")
            fcmTokenManager.cleanup()
            Log.d(TAG, "FCM cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during FCM cleanup", e)
        }
    }
}