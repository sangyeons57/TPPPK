package com.example.core_ui.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * 뷰모델에서 공통으로 사용할 수 있는 로딩 처리 헬퍼.
 *
 * - UI 상태의 `isLoading` 필드를 true/false로 토글합니다.
 * - 호출부는 상태 타입의 `copy(isLoading = ...)`만 지정하면 됩니다.
 */
suspend inline fun <State> MutableStateFlow<State>.withLoading(
    crossinline setLoading: (State, Boolean) -> State,
    crossinline block: suspend () -> Unit
) {
    update { state -> setLoading(state, true) }
    try {
        block()
    } finally {
        update { state -> setLoading(state, false) }
    }
}


