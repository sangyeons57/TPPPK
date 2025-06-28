package com.example.feature_dev

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.domain.provider.functions.FunctionsUseCaseProvider
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * 개발 메뉴 화면의 ViewModel
 * Firebase Functions 테스트 기능을 포함합니다.
 */
@HiltViewModel
class DevMenuViewModel @Inject constructor(
    private val functionsUseCaseProvider: FunctionsUseCaseProvider,
    private val firestore: FirebaseFirestore
) : ViewModel() {
    
    private val functionsUseCases = functionsUseCaseProvider.create()
    
    private val _helloWorldResult = MutableStateFlow<String>("")
    val helloWorldResult: StateFlow<String> = _helloWorldResult.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Firestore 캐시 삭제 결과 메시지
    private val _cacheClearResult = MutableStateFlow("")
    val cacheClearResult: StateFlow<String> = _cacheClearResult.asStateFlow()
    
    // 캐시 삭제 진행 상태
    private val _isCacheClearing = MutableStateFlow(false)
    val isCacheClearing: StateFlow<Boolean> = _isCacheClearing.asStateFlow()
    
    /**
     * Firebase Functions의 helloWorld 함수를 호출합니다.
     */
    fun callHelloWorld() {
        viewModelScope.launch {
            _isLoading.value = true
            _helloWorldResult.value = "호출 중..."
            
            when (val result = functionsUseCases.helloWorldUseCase()) {
                is CustomResult.Success -> {
                    _helloWorldResult.value = "성공: ${result.data}"
                }
                is CustomResult.Failure -> {
                    _helloWorldResult.value = "실패: ${result.error.message}"
                }
                else -> {
                    _helloWorldResult.value = "알 수 없는 상태: $result"
                }
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * 결과를 초기화합니다.
     */
    fun clearResult() {
        _helloWorldResult.value = ""
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
                // 로컬 캐시 삭제
                FirebaseFirestore.clearPersistence().await()

                _cacheClearResult.value = "성공: Firestore 캐시가 삭제되었습니다."
            } catch (e: Exception) {
                _cacheClearResult.value = "실패: ${e.message}"
            }

            _isCacheClearing.value = false
        }
    }
}