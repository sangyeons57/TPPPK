package com.example.core_fcm.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * 알림 권한 관련 유틸리티 클래스
 * Android 13(API 33) 이상에서 알림 권한 처리를 위한 기능을 제공합니다.
 */
object PermissionUtils {
    
    /**
     * 알림 권한이 있는지 확인합니다.
     * 
     * @param context 컨텍스트
     * @return 권한 존재 여부
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 13 미만에서는 권한이 필요 없음
            true
        }
    }
    
    /**
     * 알림 권한 설명이 필요한지 확인합니다.
     * 
     * @param activity 활동
     * @return 권한 설명 필요 여부
     */
    fun shouldShowNotificationPermissionRationale(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            false
        }
    }
    
    /**
     * 활동에서 알림 권한 요청을 위한 확장 함수
     * 
     * @param onGranted 권한 승인 시 콜백
     * @param onDenied 권한 거부 시 콜백
     */
    fun AppCompatActivity.requestNotificationPermission(
        onGranted: () -> Unit = {},
        onDenied: () -> Unit = {}
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    onGranted()
                } else {
                    onDenied()
                }
            }
            
            // 권한 상태 확인
            when {
                // 이미 권한이 있는 경우
                hasNotificationPermission(this) -> onGranted()
                
                // 권한 설명이 필요한 경우 (사용자가 이전에 거부한 경우)
                shouldShowNotificationPermissionRationale(this) -> {
                    // 여기서 사용자에게 알림이 필요한 이유 설명 UI 표시 후 요청
                    // 실제 구현에서는 이 부분을 적절히 구현해야 함
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                
                // 처음 요청하는 경우
                else -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // Android 13 미만에서는 권한이 필요 없으므로 바로 승인된 것으로 처리
            onGranted()
        }
    }
} 