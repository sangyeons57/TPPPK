package com.example.core_fcm.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 알림 채널을 관리하고 알림 설정을 처리하는 도우미 클래스
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    
    /**
     * 앱의 기본 알림 채널을 생성하고 등록합니다.
     * Android O(API 26) 이상에서만 채널 생성이 필요합니다.
     */
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createDefaultChannel()
            createHighPriorityChannel()
            // 필요한 경우 추가 채널 생성
        }
    }
    
    /**
     * 기본 우선순위 알림 채널 생성
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createDefaultChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID_DEFAULT,
            CHANNEL_NAME_DEFAULT,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "일반 알림을 위한 채널입니다"
            setShowBadge(true)
        }
        
        notificationManager.createNotificationChannel(channel)
    }
    
    /**
     * 높은 우선순위 알림 채널 생성
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createHighPriorityChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID_HIGH_PRIORITY,
            CHANNEL_NAME_HIGH_PRIORITY,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "중요 알림을 위한 채널입니다"
            setShowBadge(true)
        }
        
        notificationManager.createNotificationChannel(channel)
    }
    
    /**
     * 특정 채널의 알림 관리 설정으로 이동
     * 
     * @param channelId 채널 ID
     */
    fun openChannelSettings(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 알림 채널 설정 화면으로 이동하는 인텐트 생성 로직
            // 필요한 경우 구현
        }
    }
    
    /**
     * 모든 알림 채널 삭제 (테스트용 또는 재설정 목적)
     */
    fun deleteAllChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.notificationChannels.forEach { channel ->
                notificationManager.deleteNotificationChannel(channel.id)
            }
        }
    }
    
    companion object {
        // 기본 채널
        const val CHANNEL_ID_DEFAULT = "default_channel"
        const val CHANNEL_NAME_DEFAULT = "기본 알림"
        
        // 높은 우선순위 채널
        const val CHANNEL_ID_HIGH_PRIORITY = "high_priority_channel"
        const val CHANNEL_NAME_HIGH_PRIORITY = "중요 알림"
    }
} 