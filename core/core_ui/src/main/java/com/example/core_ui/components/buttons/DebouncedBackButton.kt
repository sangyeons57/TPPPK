package com.example.core_ui.components.buttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.core_ui.R
import com.example.core_common.constants.Constants

/**
 * 중복 클릭 방지 기능이 있는 뒤로 가기 아이콘 버튼입니다.
 * 지정된 시간(기본값: 500ms) 내에 연속적인 클릭을 무시합니다.
 *
 * @param onClick 버튼 클릭 시 호출될 콜백 함수입니다.
 * @param modifier 이 컴포저블에 적용할 [Modifier]입니다.
 * @param enabled 버튼의 활성화 상태를 제어합니다. 비활성화되면 클릭 이벤트가 발생하지 않습니다.
 * @param debounceMillis 중복 클릭을 방지하기 위한 시간 간격(밀리초)입니다.
 * @param contentDescription 접근성을 위한 아이콘 설명입니다. 기본값은 "뒤로 가기"입니다.
 */
@Composable
fun DebouncedBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    debounceMillis: Long = Constants.Navigation.DEBOUNCE_TIMEOUT_MS,
    contentDescription: String = stringResource(id = R.string.action_back)
) {
    var lastClickTime by remember { mutableLongStateOf(0L) }

    IconButton(
        onClick = {
            val currentTime = System.currentTimeMillis()
            if (enabled && (currentTime - lastClickTime) >= debounceMillis) {
                lastClickTime = currentTime
                onClick()
            }
        },
        modifier = modifier,
        enabled = enabled
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 중복 클릭 방지 기능이 있는 뒤로 가기 아이콘 버튼의 미리보기입니다.
 * (실제 앱에서는 사용되지 않습니다.)
 */
@Composable
fun DebouncedBackButtonPreview() {
    MaterialTheme {
        DebouncedBackButton(onClick = { println("Back button clicked") })
    }
} 