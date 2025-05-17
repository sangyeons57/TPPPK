package com.example.core_fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * FCM 메시지를 처리하는 서비스
 * 앱이 포그라운드에 있을 때 FCM 메시지를 받거나 데이터 메시지를 처리하는 역할을 합니다.
 */
class FCMService : FirebaseMessagingService() {

    /**
     * FCM 메시지가 수신되었을 때 호출되는 메서드
     * 메시지 데이터를 로깅하고 필요에 따라 처리합니다.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // 데이터 페이로드 확인
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            
            // 장시간 실행 작업이 필요한지 확인
            if (needsToBeScheduled()) {
                // 10초 이상 실행되는 작업의 경우 WorkManager 사용
                scheduleJob()
            } else {
                // 10초 이내에 처리 가능한 작업
                handleNow(remoteMessage.data)
            }
        }

        // 알림 페이로드 확인
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(it.title, it.body)
        }
    }

    /**
     * 토큰이 새로 생성되거나 업데이트될 때 호출되는 메서드
     * 새 토큰을 로깅하고 서버에 전송합니다.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    /**
     * 장시간 실행 작업이 필요한지 확인하는 메서드
     */
    private fun needsToBeScheduled(): Boolean {
        // 구현 필요: 작업의 복잡성에 따라 결정
        return false
    }

    /**
     * WorkManager를 사용하여 장기 실행 작업을 예약하는 메서드
     */
    private fun scheduleJob() {
        // TODO: WorkManager를 사용하여 장기 실행 작업 구현
        Log.d(TAG, "Long running job scheduled")
    }

    /**
     * 즉시 처리가 가능한 작업을 처리하는 메서드
     */
    private fun handleNow(data: Map<String, String>) {
        // TODO: 메시지 데이터에 따른 즉각적인 처리 구현
        Log.d(TAG, "Short lived task is done.")
    }

    /**
     * 새 토큰을 앱 서버에 전송하는 메서드
     */
    private fun sendRegistrationToServer(token: String) {
        // TODO: 앱 서버에 토큰 전송 로직 구현
    }

    /**
     * 사용자에게 알림을 표시하는 메서드
     */
    private fun sendNotification(title: String?, messageBody: String?) {
        // 메인 액티비티를 시작하는 인텐트 생성 (앱 구조에 맞게 수정 필요)
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title ?: "알림")
            .setContentText(messageBody ?: "")
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android Oreo 이상에서는 채널 생성 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "FCMService"
    }
} 