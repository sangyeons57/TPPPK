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
    
    // Sentry는 AndroidManifest.xml을 통해 자동 초기화되므로 수동 초기화 불필요
    /*
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
    */
    
    override fun onCreate() {
        super.onCreate()
        
        // Sentry는 AndroidManifest.xml을 통해 자동 초기화됩니다.
        // 만약 attachBaseContext에서 초기화하지 못했다면 여기서 초기화
        /*
        if (!isSentryInitialized) {
            initSentry()
        }
        */
        
        // 앱 시작 로깅 (자동 초기화 후 호출되도록 여기에 유지)
        // 자동 초기화가 완료되었는지 확인하는 로직이 있다면 더 좋지만, 일단 이렇게 둡니다.
        SentryUtil.logInfo("앱 시작됨", mapOf("startup_type" to "auto_init")) // 태그 변경
        
        // 다른 초기화 코드들...
    }
    
    // Sentry는 AndroidManifest.xml을 통해 자동 초기화되므로 수동 초기화 불필요
    /*
    private fun initSentry() {
        try {
            SentryUtil.SentryInit(this)
            isSentryInitialized = true
        } catch (e: Exception) {
            // 초기화 실패 시 로깅 (파일 로그 등)
            println("Sentry 초기화 실패: ${e.message}")
        }
    }
    */
}