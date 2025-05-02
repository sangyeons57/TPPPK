package com.example.teamnovapersonalprojectprojectingkotlin

import android.app.Application
import android.content.Context
import com.example.core_logging.SentryUtil
import dagger.hilt.android.HiltAndroidApp

/**
 * 애플리케이션 초기화 클래스
 * 
 * 앱이 처음 시작될 때 필요한 초기화 작업을 수행합니다.
 */
@HiltAndroidApp
class MyApp: Application() {
    
    companion object {
        private var isSentryInitialized = false
    }
    
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        
        // 가장 먼저 Sentry 초기화 (attachBaseContext에서 실행하여 최대한 빨리 초기화)
        if (!isSentryInitialized) {
            initSentry()
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 만약 attachBaseContext에서 초기화하지 못했다면 여기서 초기화
        if (!isSentryInitialized) {
            initSentry()
        }
        
        // 앱 시작 로깅
        SentryUtil.logInfo("앱 시작됨", mapOf("startup_type" to "normal"))
        
        // 다른 초기화 코드들...
    }
    
    /**
     * Sentry 초기화 함수
     * 중복 초기화 방지를 위해 별도 함수로 분리
     */
    private fun initSentry() {
        try {
            SentryUtil.SentryInit(this)
            isSentryInitialized = true
        } catch (e: Exception) {
            // 초기화 실패 시 로깅 (파일 로그 등)
            println("Sentry 초기화 실패: ${e.message}")
        }
    }
}