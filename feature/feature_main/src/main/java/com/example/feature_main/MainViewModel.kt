package com.example.feature_main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * MainViewModel: 메인 화면 (하단 탭 호스트) 전체에 필요한 공통 로직 관리
 */
@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {
    // 현재는 MainScreen의 상태 관리가 단순하므로 필요한 기능만 구현
    // 향후 전역적인 상태 관리가 필요하면 추가 구현
}