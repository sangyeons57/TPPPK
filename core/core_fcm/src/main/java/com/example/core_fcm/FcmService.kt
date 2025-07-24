
package com.example.core_fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        
        // Send token to server using dependency injection
        sendTokenToServer(token)
    }
    
    /**
     * Send FCM token to server
     */
    private fun sendTokenToServer(token: String) {
        // This will be handled by the app module which has access to the use case
        // For now, store token in SharedPreferences for the app to pick up
        val prefs = getSharedPreferences("fcm_prefs", MODE_PRIVATE)
        prefs.edit().putString("pending_fcm_token", token).apply()
        
        Log.d(TAG, "FCM token stored for app to sync with server")
        
        // Send broadcast to notify app of new token
        val intent = android.content.Intent("com.example.FCM_TOKEN_UPDATED")
        intent.putExtra("token", token)
        sendBroadcast(intent)
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
        val senderId = data["senderId"] ?: ""
        val senderName = data["senderName"] ?: "Someone"
        val channelId = data["channelId"] ?: ""
        val projectId = data["projectId"] ?: ""
        val mentionType = data["mentionType"] ?: "USER"
        val mentionId = data["mentionId"] ?: ""
        val roleName = data["roleName"]

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

        // Show notification
        showNotification(
            title = title,
            body = body,
            pendingIntent = pendingIntent,
            notificationId = messageId.hashCode(),
            channelId = MENTION_CHANNEL_ID,
            channelName = "멘션 알림"
        )
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
        // This should match your app's navigation structure
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
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
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
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Build notification
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(com.example.core_ui.R.drawable.ic_stat_ic_notification) // Use custom notification icon
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        // Add pending intent if provided
        pendingIntent?.let { intent ->
            notificationBuilder.setContentIntent(intent)
        }

        // Show notification
        notificationManager.notify(notificationId, notificationBuilder.build())
        Log.d(TAG, "Notification shown with ID: $notificationId")
    }

    companion object {
        private const val TAG = "FcmService"
        private const val MENTION_CHANNEL_ID = "mention_notifications"
        private const val DEFAULT_CHANNEL_ID = "default_notifications"
    }
}
