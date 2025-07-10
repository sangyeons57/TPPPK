package com.example.feature_dev.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DevMenuViewModel @Inject constructor() : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Firestore 캐시 삭제 결과 메시지
    private val _cacheClearResult = MutableStateFlow("")
    val cacheClearResult: StateFlow<String> = _cacheClearResult.asStateFlow()

    // 캐시 삭제 진행 상태
    private val _isCacheClearing = MutableStateFlow(false)
    val isCacheClearing: StateFlow<Boolean> = _isCacheClearing.asStateFlow()

    /**
     * 결과를 초기화합니다.
     */
    fun clearResult() {
        _cacheClearResult.value = ""
    }

    /**
     * Firestore 캐시 정보를 표시합니다.
     * 실제 캐시 삭제는 Firestore의 네이티브 캐싱에 의해 관리됩니다.
     */
    fun clearFirestoreCache() {
        viewModelScope.launch {
            _isCacheClearing.value = true
            _cacheClearResult.value = "캐시 정보 확인 중..."

            // Firestore는 네이티브 캐싱을 사용하므로 수동 캐시 삭제 불필요
            Log.d("DevMenuViewModel", "Firestore uses native caching - manual cache clearing not required")
            _cacheClearResult.value = "정보: Firestore는 네이티브 캐싱을 사용합니다. 수동 캐시 삭제가 필요하지 않습니다."

            _isCacheClearing.value = false
        }
    }
}