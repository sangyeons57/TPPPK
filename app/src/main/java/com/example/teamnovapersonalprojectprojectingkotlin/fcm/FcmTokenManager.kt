package com.example.teamnovapersonalprojectprojectingkotlin.fcm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.domain.provider.user.UserUseCaseProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.messaging.FirebaseMessaging
import com.example.core_common.result.CustomResult

/**
 * Manages FCM token synchronization with the server
 */
@Singleton
class FcmTokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userUseCaseProvider: UserUseCaseProvider
) {

    private val scope = CoroutineScope(SupervisorJob())
    private var isRegistered = false

    private val fcmTokenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.FCM_TOKEN_UPDATED") {
                val token = intent.getStringExtra("token")
                if (token != null) {
                    Log.d(TAG, "Received FCM token update broadcast: ${token.take(20)}...")
                    syncTokenWithServer(token)
                }
            }
        }
    }

    /**
     * Initialize FCM token management
     */
    fun initialize() {
        if (!isRegistered) {
            // Register broadcast receiver for token updates from FcmService
            val filter = IntentFilter("com.example.FCM_TOKEN_UPDATED")
            ContextCompat.registerReceiver(
                context, 
                fcmTokenReceiver, 
                filter, 
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            isRegistered = true
            Log.d(TAG, "FCM token manager initialized")
        }

        // Get current token and sync if available
        getCurrentTokenAndSync()
        
        // Check for any pending tokens from SharedPreferences
        checkPendingToken()
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        if (isRegistered) {
            context.unregisterReceiver(fcmTokenReceiver)
            isRegistered = false
            Log.d(TAG, "FCM token manager cleaned up")
        }
    }

    /**
     * Get current FCM token and sync with server
     */
    private fun getCurrentTokenAndSync() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d(TAG, "Current FCM token: ${token?.take(20)}...")
            
            if (token != null) {
                syncTokenWithServer(token)
            }
        }
    }

    /**
     * Check for pending tokens stored by FcmService
     */
    private fun checkPendingToken() {
        val prefs = context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
        val pendingToken = prefs.getString("pending_fcm_token", null)
        
        if (pendingToken != null) {
            Log.d(TAG, "Found pending FCM token, syncing...")
            syncTokenWithServer(pendingToken)
            
            // Clear pending token
            prefs.edit().remove("pending_fcm_token").apply()
        }
    }

    /**
     * Sync FCM token with server using domain use case
     */
    private fun syncTokenWithServer(token: String) {
        scope.launch {
            try {
                val userUseCases = userUseCaseProvider.createForUser()
                val result = userUseCases.updateFcmTokenUseCase(token)
                
                when (result) {
                    is CustomResult.Success -> {
                        Log.d(TAG, "FCM token successfully synced with server")
                    }
                    is CustomResult.Failure -> {
                        Log.e(TAG, "Failed to sync FCM token with server", result.error)
                        // Could implement retry logic here
                    }
                    else -> {
                        Log.w(TAG, "FCM token sync returned unexpected result: $result")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing FCM token with server", e)
            }
        }
    }

    /**
     * Manually refresh FCM token
     */
    fun refreshToken() {
        FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener { deleteTask ->
            if (deleteTask.isSuccessful) {
                Log.d(TAG, "FCM token deleted, getting new token...")
                getCurrentTokenAndSync()
            } else {
                Log.w(TAG, "Failed to delete FCM token", deleteTask.exception)
            }
        }
    }

    companion object {
        private const val TAG = "FcmTokenManager"
    }
}