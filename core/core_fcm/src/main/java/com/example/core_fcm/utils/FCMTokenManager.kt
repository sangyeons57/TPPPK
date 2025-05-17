package com.example.core_fcm.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.core_fcm.FCMInitializer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FCM 토큰 관리를 담당하는 유틸리티 클래스
 * 토큰을 SharedPreferences에 저장하고 검색합니다.
 */
@Singleton
class FCMTokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * FCM 토큰 저장
     * 
     * @param token 저장할 FCM 토큰
     */
    fun saveToken(token: String) {
        sharedPreferences.edit().putString(FCMInitializer.KEY_FCM_TOKEN, token).apply()
    }
    
    /**
     * 저장된 FCM 토큰 가져오기
     * 
     * @return 저장된 토큰 또는 null(저장된 토큰이 없는 경우)
     */
    fun getToken(): String? {
        return sharedPreferences.getString(FCMInitializer.KEY_FCM_TOKEN, null)
    }
    
    /**
     * 저장된 FCM 토큰 삭제
     */
    fun clearToken() {
        sharedPreferences.edit().remove(FCMInitializer.KEY_FCM_TOKEN).apply()
    }
    
    /**
     * FCM 토큰이 저장되어 있는지 확인
     * 
     * @return 토큰 존재 여부
     */
    fun hasToken(): Boolean {
        return getToken() != null
    }
    
    companion object {
        private const val PREF_NAME = "fcm_prefs"
    }
} 