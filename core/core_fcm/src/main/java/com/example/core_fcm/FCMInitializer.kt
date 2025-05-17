package com.example.core_fcm

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FCM 초기화 및 토큰 관리를 담당하는 클래스
 * 앱 시작 시 Firebase를 초기화하고 FCM 토큰을 가져옵니다.
 */
@Singleton
class FCMInitializer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "FCMInitializer"
    
    /**
     * Firebase 및 FCM 초기화
     */
    fun initialize() {
        // Firebase 초기화
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
            Log.d(tag, "Firebase initialized")
        }
        
        // FCM 토큰 가져오기
        getToken()
    }
    
    /**
     * FCM 토큰을 가져오고 로깅하는 메서드
     * 토큰은 서버에 등록하거나 필요한 작업에 사용할 수 있습니다.
     * 
     * @return 토큰의 Task 객체, 콜백으로 처리 가능
     */
    fun getToken() = FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
        if (!task.isSuccessful) {
            Log.w(tag, "Fetching FCM registration token failed", task.exception)
            return@OnCompleteListener
        }

        // 새 FCM 등록 토큰 가져오기
        val token = task.result
        
        // 토큰 로깅
        Log.d(tag, "FCM Token: $token")
        
        // 서버에 토큰 등록 등의 추가 작업 수행 가능
    })
    
    /**
     * FCM 자동 초기화 활성화/비활성화
     * 
     * @param enabled 활성화 여부
     */
    fun setAutoInitEnabled(enabled: Boolean) {
        FirebaseMessaging.getInstance().isAutoInitEnabled = enabled
        Log.d(tag, "FCM auto init enabled: $enabled")
    }
    
    companion object {
        // 토큰을 로컬에 저장할 때 사용하는 키
        const val KEY_FCM_TOKEN = "fcm_token"
    }
} 