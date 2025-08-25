package com.example.teamnovapersonalprojectprojectingkotlin.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.core_common.result.CustomResult
import com.example.domain_usecase.provider.user.UserUseCaseProvider
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Firebase Cloud Messaging 서비스 구현체
 * FCM 알림 처리 및 토큰 관리를 담당합니다
 */
@AndroidEntryPoint
class AppFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var userUseCaseProvider: UserUseCaseProvider

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received: $token")

        // 서버에 토큰 업데이트
        updateTokenOnServer(token)

        // FcmTokenManager에게 토큰 업데이트 알림
        broadcastTokenUpdate(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Handle data payload
        val data = remoteMessage.data
        val notificationType = data["type"]

        Log.d(TAG, "Message data: $data")
        Log.d(TAG, "Notification type: $notificationType")

        when (notificationType) {
            "mention" -> handleMentionNotification(remoteMessage)
            else -> handleDefaultNotification(remoteMessage)
        }
    }

    /**
     * FCM 토큰을 서버에 업데이트
     */
    private fun updateTokenOnServer(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Updating FCM token on server...")

                val userUseCases = userUseCaseProvider.createForUser()
                when (val result = userUseCases.updateFcmTokenUseCase()) {
                    is CustomResult.Success -> {
                        Log.d(TAG, "FCM token successfully updated on server")

                        // SharedPreferences에서 pending 토큰 제거
                        val prefs = getSharedPreferences("fcm_prefs", MODE_PRIVATE)
                        prefs.edit().remove("pending_fcm_token").apply()
                    }

                    is CustomResult.Failure -> {
                        Log.e(TAG, "Failed to update FCM token on server", result.error)

                        // 실패 시 SharedPreferences에 저장해두어 나중에 재시도
                        val prefs = getSharedPreferences("fcm_prefs", MODE_PRIVATE)
                        prefs.edit().putString("pending_fcm_token", token).apply()
                    }

                    else -> {
                        Log.d(TAG, "FCM token update in progress...")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while updating FCM token", e)

                // 예외 발생 시 SharedPreferences에 저장
                val prefs = getSharedPreferences("fcm_prefs", MODE_PRIVATE)
                prefs.edit().putString("pending_fcm_token", token).apply()
            }
        }
    }

    /**
     * Handle mention notification specifically
     */
    private fun handleMentionNotification(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Handling mention notification")

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        if (notification == null) {
            Log.w(TAG, "Mention notification has no notification payload")
            return
        }

        // Extract mention-specific data
        val messageId = data["messageId"] ?: ""
        data["senderId"] ?: ""
        val senderName = data["senderName"] ?: "Someone"
        val channelId = data["channelId"] ?: ""
        val projectId = data["projectId"] ?: ""
        val mentionType = data["mentionType"] ?: "USER"
        val mentionId = data["mentionId"] ?: ""
        val roleName = data["roleName"]

        // Log important mention payload fields for diagnostics
        Log.i(
            TAG,
            "FCM_RECEIVED type=mention messageId=${messageId} channelId=${channelId} projectId=${projectId} sender=${senderName} mentionType=${mentionType}"
        )

        // Create notification title and body
        val title = notification.title ?: when (mentionType) {
            "USER" -> "${senderName}님이 회원님을 멘션했습니다"
            "ROLE" -> "${senderName}님이 @${roleName ?: mentionId}을 멘션했습니다"
            else -> "${senderName}님이 회원님을 멘션했습니다"
        }

        val body = notification.body ?: "새로운 멘션이 있습니다"

        // Create pending intent for navigation
        val intent = createMentionNavigationIntent(
            messageId = messageId,
            channelId = channelId,
            projectId = projectId
        )

        val pendingIntent = PendingIntent.getActivity(
            this,
            messageId.hashCode(), // Use messageId as unique request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Show notification with unique ID
        val uniqueNotificationId = (messageId + System.currentTimeMillis()).hashCode()
        showNotification(
            title = title,
            body = body,
            pendingIntent = pendingIntent,
            notificationId = uniqueNotificationId,
            channelId = MENTION_CHANNEL_ID,
            channelName = "멘션 알림"
        )

        Log.i(TAG, "FCM_HANDLED mention notificationId=${messageId.hashCode()} title='${title}'")
    }

    /**
     * Handle default notification
     */
    private fun handleDefaultNotification(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Handling default notification")

        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: "알림"
            val body = notification.body ?: ""

            showNotification(
                title = title,
                body = body,
                pendingIntent = null,
                notificationId = System.currentTimeMillis().toInt(),
                channelId = DEFAULT_CHANNEL_ID,
                channelName = "일반 알림"
            )
        }
    }

    /**
     * Create navigation intent for mention notification
     */
    private fun createMentionNavigationIntent(
        messageId: String,
        channelId: String,
        projectId: String
    ): Intent {
        // Create intent to navigate to the specific chat
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: Intent()

        // Add extra data for navigation
        intent.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", "mention")
            putExtra("message_id", messageId)
            putExtra("channel_id", channelId)
            if (projectId.isNotEmpty()) {
                putExtra("project_id", projectId)
            }
        }

        return intent
    }

    /**
     * Show notification with given parameters
     */
    private fun showNotification(
        title: String,
        body: String,
        pendingIntent: PendingIntent?,
        notificationId: Int,
        channelId: String,
        channelName: String
    ) {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "${channelName} 채널"
                enableVibration(true)
                setShowBadge(true)
                enableLights(true)
                lightColor = 0xFF2196F3.toInt() // 파란색 LED
                setBypassDnd(false) // 방해금지 모드 우회하지 않음
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                canShowBadge() // 배지 표시 가능
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Build notification with enhanced visibility settings
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(com.example.core_ui.R.drawable.ic_stat_ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setGroup("MENTION_NOTIFICATIONS") // 그룹 설정
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_ALL) // 모든 알림 표시
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 잠금화면에서도 표시
            .setCategory(NotificationCompat.CATEGORY_MESSAGE) // 메시지 카테고리
            .setOnlyAlertOnce(false) // 매번 소리/진동

        // Add pending intent if provided
        pendingIntent?.let { intent ->
            notificationBuilder.setContentIntent(intent)
        }

        // Show notification
        notificationManager.notify(notificationId, notificationBuilder.build())
        Log.d(TAG, "Notification shown with ID: $notificationId")
    }

    /**
     * FCM 토큰 업데이트를 FcmTokenManager에게 브로드캐스트
     */
    private fun broadcastTokenUpdate(token: String) {
        try {
            val intent = Intent("com.example.FCM_TOKEN_UPDATED")
            intent.putExtra("token", token)
            sendBroadcast(intent)
            Log.d(TAG, "FCM token update broadcasted: ${token.take(20)}...")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to broadcast FCM token update", e)
        }
    }

    companion object {
        private const val TAG = "AppFirebaseMessagingService"
        private const val MENTION_CHANNEL_ID = "mention_notifications"
        private const val DEFAULT_CHANNEL_ID = "default_notifications"
    }
}
