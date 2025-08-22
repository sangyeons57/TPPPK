package com.example.feature_chat.ui.components.message

import android.net.Uri
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import com.example.feature_chat.ui.components.mention.ParsedMention
import com.example.feature_chat.ui.components.mention.ProcessedText

/**
 * ChatMessageText: 링크와 멘션을 지원하는 채팅 메시지 텍스트 컴포넌트
 * LinkAnnotation을 사용하여 타입 안전하고 깔끔한 링크 처리
 */
@Composable
fun ChatMessageText(
    processedText: ProcessedText,
    onMentionClick: (type: String, id: String) -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val annotatedString = buildAnnotatedString {
        var currentIndex = 0

        // 멘션과 URL을 시작 위치로 정렬
        val allAnnotations = mutableListOf<AnnotationInfo>()

        // 멘션 정보 추가
        processedText.mentions.forEach { mention ->
            allAnnotations.add(
                AnnotationInfo(
                    start = mention.start,
                    end = mention.end,
                    type = AnnotationType.MENTION,
                    data = mention
                )
            )
        }

        // URL 정보 추가 (기존 URL 파싱 로직 사용)
        val urlRegex = """(https?://\S+)""".toRegex()
        urlRegex.findAll(processedText.text).forEach { matchResult ->
            allAnnotations.add(
                AnnotationInfo(
                    start = matchResult.range.first,
                    end = matchResult.range.last + 1,
                    type = AnnotationType.URL,
                    data = matchResult.value
                )
            )
        }

        // 시작 위치로 정렬
        allAnnotations.sortBy { it.start }

        // 텍스트를 순차적으로 처리
        allAnnotations.forEach { annotation ->
            // 현재 위치에서 annotation 시작까지 일반 텍스트 추가
            if (currentIndex < annotation.start) {
                append(processedText.text.substring(currentIndex, annotation.start))
            }

            when (annotation.type) {
                AnnotationType.MENTION -> {
                    val mention = annotation.data as ParsedMention
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = "MENTION:${mention.type}:${mention.id}",
                            styles = TextLinkStyles(
                                style = when (mention.type) {
                                    "user" -> SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        background = MaterialTheme.colorScheme.primaryContainer.copy(
                                            alpha = 0.7f
                                        ),
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    "role" -> {
                                        // Special styling for @everyone vs regular roles
                                        if (mention.id == "everyone") {
                                            SpanStyle(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                background = MaterialTheme.colorScheme.surfaceVariant.copy(
                                                    alpha = 0.7f
                                                ),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        } else {
                                            SpanStyle(
                                                color = MaterialTheme.colorScheme.tertiary,
                                                background = MaterialTheme.colorScheme.tertiaryContainer.copy(
                                                    alpha = 0.7f
                                                ),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    else -> SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        background = MaterialTheme.colorScheme.primaryContainer.copy(
                                            alpha = 0.7f
                                        ),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            ),
                            linkInteractionListener = { link ->
                                val tag = (link as LinkAnnotation.Clickable).tag
                                val parts = tag.split(":")
                                if (parts.size == 3 && parts[0] == "MENTION") {
                                    onMentionClick(parts[1], parts[2])
                                }
                            }
                        )
                    ) {
                        append("@${mention.displayName}")
                    }
                }

                AnnotationType.URL -> {
                    val url = annotation.data as String
                    withLink(
                        LinkAnnotation.Url(
                            url = url,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.tertiary,
                                    textDecoration = TextDecoration.Underline
                                )
                            ),
                            linkInteractionListener = { link ->
                                val urlToOpen = (link as LinkAnnotation.Url).url
                                try {
                                    uriHandler.openUri(urlToOpen)
                                } catch (e: Exception) {
                                    // uriHandler가 실패하면 Intent로 시도
                                    try {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            Uri.parse(urlToOpen)
                                        )
                                        context.startActivity(intent)
                                    } catch (e2: Exception) {
                                        // URL 열기 실패 처리
                                    }
                                }
                            }
                        )
                    ) {
                        append(url)
                    }
                }
            }

            currentIndex = annotation.end
        }

        // 마지막 annotation 이후의 텍스트 추가
        if (currentIndex < processedText.text.length) {
            append(processedText.text.substring(currentIndex))
        }
    }

    BasicText(
        text = annotatedString,
        style = LocalTextStyle.current.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier
    )
}

/**
 * 어노테이션 타입을 구분하는 enum
 */
private enum class AnnotationType {
    MENTION,
    URL
}

/**
 * 어노테이션 정보를 담는 데이터 클래스
 */
private data class AnnotationInfo(
    val start: Int,
    val end: Int,
    val type: AnnotationType,
    val data: Any
) 