package com.example.data.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.domain.util.NetworkConnectivityMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NetworkConnectivityMonitor 인터페이스의 구현체
 * Android ConnectivityManager를 사용하여 네트워크 연결 상태를 모니터링합니다.
 *
 * @param context 애플리케이션 컨텍스트
 */
@Singleton
class NetworkConnectivityMonitorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkConnectivityMonitor {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * 현재 네트워크 연결 가능 여부를 확인합니다.
     *
     * @return 네트워크 연결 가능 여부를 나타내는 Flow
     */
    override val isNetworkAvailable: Flow<Boolean> = callbackFlow {
        // 초기 상태 전송
        trySend(isCurrentlyConnected())

        // 네트워크 콜백 생성
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(isCurrentlyConnected())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val isConnected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                trySend(isConnected)
            }
        }

        // 네트워크 요청 설정
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        // 네트워크 콜백 등록
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Flow 구독이 취소되면 콜백 해제
        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }.distinctUntilChanged() // 연속된 동일 값 필터링

    /**
     * 현재 네트워크 연결 상태를 확인합니다.
     *
     * @return 현재 네트워크 연결 여부
     */
    private fun isCurrentlyConnected(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
} 