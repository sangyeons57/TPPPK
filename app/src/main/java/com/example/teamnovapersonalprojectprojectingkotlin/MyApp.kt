package com.example.teamnovapersonalprojectprojectingkotlin

import android.app.Application
import com.example.core_logging.SentryUtil
import com.google.firebase.FirebaseApp
import com.google.firebase.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory


/**
 * 애플리케이션 초기화 클래스
 * 
 * 앱이 처음 시작될 때 필요한 초기화 작업을 수행합니다.
 */
@HiltAndroidApp
class MyApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Firebase 초기화
        initializeFirebase()
        
        // Sentry 초기화
        SentryUtil.SentryInit(this)


        SentryUtil.logInfo("앱 시작됨", mapOf("startup_type" to "manual_init"))
    }
    
    /**
     * FirebaseApp을 명시적으로 초기화합니다.
     */
    private fun initializeFirebase() {
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = com.google.firebase.appcheck.FirebaseAppCheck.getInstance()
        if (false) {
            firebaseAppCheck.installAppCheckProviderFactory(
                com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }
    
}