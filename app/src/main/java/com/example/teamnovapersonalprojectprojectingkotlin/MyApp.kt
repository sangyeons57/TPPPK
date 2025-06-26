package com.example.teamnovapersonalprojectprojectingkotlin

// import com.google.firebase.BuildConfig // Firebase 라이브러리의 BuildConfig가 아님
import android.app.Application
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp


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
        
        // Sentry 초기화 :: 잠시 꺼두기
        //SentryUtil.SentryInit(this)
        //SentryUtil.logInfo("앱 시작됨", mapOf("startup_type" to "manual_init"))
    }
    
    /**
     * FirebaseApp을 명시적으로 초기화합니다.
     */
    private fun initializeFirebase() {
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = com.google.firebase.appcheck.FirebaseAppCheck.getInstance()
        // 앱 모듈의 BuildConfig.DEBUG를 사용해야 합니다.
        if (BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }
    
}