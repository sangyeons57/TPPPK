package com.example.feature_dev

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.domain.provider.functions.FunctionsUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 개발 메뉴 화면의 ViewModel
 * Firebase Functions 테스트 기능을 포함합니다.
 */
@HiltViewModel
class DevMenuViewModel @Inject constructor(
    private val functionsUseCaseProvider: FunctionsUseCaseProvider
) : ViewModel() {
    
    private val functionsUseCases = functionsUseCaseProvider.create()
    
    private val _helloWorldResult = MutableStateFlow<String>("")
    val helloWorldResult: StateFlow<String> = _helloWorldResult.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
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
    }
}