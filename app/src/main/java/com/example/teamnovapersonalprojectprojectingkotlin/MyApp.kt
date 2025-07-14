package com.example.teamnovapersonalprojectprojectingkotlin

// import com.google.firebase.BuildConfig // Firebase 라이브러리의 BuildConfig가 아님
import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp


/**
 * 애플리케이션 초기화 클래스
 * 
 * 앱이 처음 시작될 때 필요한 초기화 작업을 수행합니다.
 */
@HiltAndroidApp
class MyApp : Application() {

    private val TAG = "MyApp"
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        appcheck()


    }
    
    /**
     * FirebaseApp을 명시적으로 초기화합니다.
     */
    private fun appcheck() {
        try {
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance(),
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")

        } catch (e: IllegalStateException) {
            Log.e(TAG, "FirebaseApp is not initialized. Ensure google-services.json is correct " +
                    "or call FirebaseApp.initializeApp() explicitly if needed.", e)
        }
    }
}