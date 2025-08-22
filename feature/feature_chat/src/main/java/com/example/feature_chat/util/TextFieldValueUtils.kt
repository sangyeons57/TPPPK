package com.example.feature_chat.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * TextFieldValue 커서 제어 유틸리티 함수들
 */

/**
 * 커서를 지정된 위치로 이동
 * @param position 이동할 커서 위치 (경계 보정 자동 적용)
 */
fun TextFieldValue.moveCursorTo(position: Int) = copy(
    selection = TextRange(position.coerceIn(0, text.length))
)

/**
 * 커서를 텍스트 끝으로 이동
 */
fun TextFieldValue.moveCursorToEnd() = copy(
    selection = TextRange(text.length)
)

/**
 * 커서를 텍스트 시작으로 이동
 */
fun TextFieldValue.moveCursorToStart() = copy(
    selection = TextRange(0)
)

/**
 * 현재 커서 위치에 텍스트 삽입
 * @param insertText 삽입할 텍스트
 */
fun TextFieldValue.insertTextAtCursor(insertText: String) = copy(
    text = text.replaceRange(selection.start, selection.end, insertText),
    selection = TextRange(selection.start + insertText.length)
)

/**
 * 현재 커서 위치에 텍스트 삽입 후 커서를 지정된 위치로 이동
 * @param insertText 삽입할 텍스트
 * @param newCursorPosition 삽입 후 커서 위치
 */
fun TextFieldValue.insertTextAndMoveCursor(insertText: String, newCursorPosition: Int) = copy(
    text = text.replaceRange(selection.start, selection.end, insertText),
    selection = TextRange(newCursorPosition.coerceIn(0, text.length + insertText.length))
)

/**
 * 지정된 범위의 텍스트를 새 텍스트로 교체하고 커서를 지정된 위치로 이동
 * @param startIndex 교체 시작 위치
 * @param endIndex 교체 끝 위치
 * @param newText 새로운 텍스트
 * @param newCursorPosition 교체 후 커서 위치
 */
fun TextFieldValue.replaceTextAndMoveCursor(
    startIndex: Int,
    endIndex: Int,
    newText: String,
    newCursorPosition: Int
): TextFieldValue {
    val safeStartIndex = startIndex.coerceIn(0, text.length)
    val safeEndIndex = endIndex.coerceIn(safeStartIndex, text.length)
    val resultText = text.replaceRange(safeStartIndex, safeEndIndex, newText)

    return copy(
        text = resultText,
        selection = TextRange(newCursorPosition.coerceIn(0, resultText.length))
    )
}

/**
 * 현재 커서 위치 반환
 */
val TextFieldValue.cursorPosition: Int
    get() = selection.end