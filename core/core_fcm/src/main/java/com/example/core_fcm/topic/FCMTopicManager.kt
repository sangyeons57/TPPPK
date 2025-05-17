package com.example.core_fcm.topic

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FCM 토픽 구독 관리를 위한 클래스
 * 토픽 기반 알림을 위한 구독 및 구독 취소 기능을 제공합니다.
 */
@Singleton
class FCMTopicManager @Inject constructor() {
    
    private val tag = "FCMTopicManager"
    
    /**
     * FCM 토픽 구독
     * 
     * @param topic 구독할 토픽
     * @param callback 성공/실패 시 콜백
     */
    fun subscribeTopic(topic: String, callback: ((Boolean) -> Unit)? = null) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(tag, "Subscribed to topic: $topic")
                    callback?.invoke(true)
                } else {
                    Log.e(tag, "Failed to subscribe to topic: $topic", task.exception)
                    callback?.invoke(false)
                }
            }
    }
    
    /**
     * FCM 토픽 구독 취소
     * 
     * @param topic 구독 취소할 토픽
     * @param callback 성공/실패 시 콜백
     */
    fun unsubscribeTopic(topic: String, callback: ((Boolean) -> Unit)? = null) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(tag, "Unsubscribed from topic: $topic")
                    callback?.invoke(true)
                } else {
                    Log.e(tag, "Failed to unsubscribe from topic: $topic", task.exception)
                    callback?.invoke(false)
                }
            }
    }
    
    /**
     * 여러 토픽 구독 처리
     * 
     * @param topics 구독할 토픽 목록
     * @param onAllCompleted 모든 구독 완료 후 콜백 (성공 토픽 수)
     */
    fun subscribeToTopics(topics: List<String>, onAllCompleted: ((Int) -> Unit)? = null) {
        var successCount = 0
        var completedCount = 0
        
        topics.forEach { topic ->
            subscribeTopic(topic) { success ->
                if (success) successCount++
                completedCount++
                
                if (completedCount == topics.size) {
                    onAllCompleted?.invoke(successCount)
                }
            }
        }
    }
    
    /**
     * 여러 토픽 구독 취소 처리
     * 
     * @param topics 구독 취소할 토픽 목록
     * @param onAllCompleted 모든 구독 취소 완료 후 콜백 (성공 토픽 수)
     */
    fun unsubscribeFromTopics(topics: List<String>, onAllCompleted: ((Int) -> Unit)? = null) {
        var successCount = 0
        var completedCount = 0
        
        topics.forEach { topic ->
            unsubscribeTopic(topic) { success ->
                if (success) successCount++
                completedCount++
                
                if (completedCount == topics.size) {
                    onAllCompleted?.invoke(successCount)
                }
            }
        }
    }
    
    companion object {
        // 앱에서 사용할 주요 토픽 상수 정의
        const val TOPIC_GENERAL = "general"
        const val TOPIC_UPDATES = "app_updates" 
        const val TOPIC_MARKETING = "marketing"
    }
} 