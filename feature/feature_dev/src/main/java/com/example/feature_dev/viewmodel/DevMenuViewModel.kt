package com.example.feature_dev.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class DevMenuViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

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
     * Firestore 로컬 캐시를 삭제합니다.
     * terminate() 이후 clearPersistence() 순서로 실행해야 합니다.
     */
    fun clearFirestoreCache() {
        viewModelScope.launch {
            _isCacheClearing.value = true
            _cacheClearResult.value = "캐시 삭제 중..."

            try {
                // 현재 Firestore 인스턴스 종료
                firestore.terminate().await()
                firestore.clearPersistence().await()
                FirebaseFirestore.getInstance().clearPersistence()

                _cacheClearResult.value = "성공: Firestore 캐시가 삭제되었습니다."
            } catch (e: Exception) {
                _cacheClearResult.value = "실패: ${e.message}"
            }

            _isCacheClearing.value = false
        }
    }
}