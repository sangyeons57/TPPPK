package com.example.teamnovapersonalprojectprojectingkotlin.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 알림 채널을 관리하는 매니저 클래스
 * 앱 시작 시 필요한 알림 채널들을 미리 생성합니다.
 */
@Singleton
class NotificationChannelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * 모든 알림 채널을 초기화합니다.
     */
    fun initializeChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Initializing notification channels...")

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 멘션 알림 채널
            val mentionChannel = NotificationChannel(
                MENTION_CHANNEL_ID,
                "멘션 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "채팅에서 회원님을 멘션했을 때 알림"
                enableVibration(true)
                setShowBadge(true)
                enableLights(true)
            }

            // 일반 알림 채널
            val defaultChannel = NotificationChannel(
                DEFAULT_CHANNEL_ID,
                "일반 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "일반적인 앱 알림"
                enableVibration(true)
                setShowBadge(true)
            }

            // 채팅 메시지 알림 채널
            val chatChannel = NotificationChannel(
                CHAT_CHANNEL_ID,
                "채팅 메시지",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "새로운 채팅 메시지 알림"
                enableVibration(true)
                setShowBadge(true)
                enableLights(true)
            }

            // 시스템 알림 채널
            val systemChannel = NotificationChannel(
                SYSTEM_CHANNEL_ID,
                "시스템 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "시스템 관련 알림"
                enableVibration(false)
                setShowBadge(true)
            }

            // 채널들을 시스템에 등록
            notificationManager.createNotificationChannels(
                listOf(mentionChannel, defaultChannel, chatChannel, systemChannel)
            )

            Log.d(TAG, "Notification channels created successfully")

        } else {
            Log.d(TAG, "Notification channels not needed for API level < 26")
        }
    }

    /**
     * 알림 권한이 허용되어 있는지 확인합니다.
     */
    fun isNotificationEnabled(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    companion object {
        private const val TAG = "NotificationChannelManager"

        // 채널 ID들 - core_fcm의 FcmService와 동일하게 유지
        const val MENTION_CHANNEL_ID = "mention_notifications"
        const val DEFAULT_CHANNEL_ID = "default_notifications"
        const val CHAT_CHANNEL_ID = "chat_notifications"
        const val SYSTEM_CHANNEL_ID = "system_notifications"
    }
}