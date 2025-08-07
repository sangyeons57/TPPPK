package com.example.feature_chat.ui.components.system

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Start
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.domain.vo.message.MessagePayload

/**
 * 날짜 표시 시스템 메시지 컴포넌트 (기존 메시지 기반)
 */
@Composable
fun DateSystemMessage(
    payload: String,
    modifier: Modifier = Modifier
) {
    val messagePayload = MessagePayload(payload)
    val displayText = messagePayload.getValue("displayText") ?: "날짜"

    DateSeparator(displayText = displayText, modifier = modifier)
}

/**
 * UI-based 날짜 구분선 컴포넌트 (실제 메시지가 아닌 UI 요소)
 */
@Composable
fun DateSeparator(
    displayText: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 채팅 시작 시스템 메시지 컴포넌트
 */
@Composable
fun ChatStartSystemMessage(
    payload: String,
    modifier: Modifier = Modifier
) {
    val messagePayload = MessagePayload(payload)
    val channelName = messagePayload.getValue("channelName") ?: "채널"
    val welcomeText = messagePayload.getValue("welcomeText") ?: "채팅이 시작되었습니다"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Start,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$channelName 채널",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = welcomeText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 프로젝트 참여 시스템 메시지 컴포넌트
 */
@Composable
fun ProjectJoinSystemMessage(
    payload: String,
    onJoinProject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val messagePayload = MessagePayload(payload)
    val projectId = messagePayload.getValue("projectId") ?: ""
    val projectName = messagePayload.getValue("projectName") ?: "프로젝트"
    val actionText = messagePayload.getValue("actionText") ?: "참여하기"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.width(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "프로젝트 초대",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = projectName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onJoinProject(projectId) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 프로젝트 멤버 초대 시스템 메시지 컴포넌트
 */
@Composable
fun MemberInvitationSystemMessage(
    payload: String,
    onAddMember: (String, String) -> Unit, // (projectId, targetUserId) -> Unit
    modifier: Modifier = Modifier
) {
    val messagePayload = MessagePayload(payload)
    val projectId = messagePayload.getValue("projectId") ?: ""
    val projectName = messagePayload.getValue("projectName") ?: "프로젝트"
    val inviterName = messagePayload.getValue("inviterName") ?: "사용자"
    val targetUserId = messagePayload.getValue("targetUserId") ?: ""
    val actionText = messagePayload.getValue("actionText") ?: "멤버로 추가"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.width(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "멤버 초대",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${inviterName}님이 '$projectName' 프로젝트에 초대했습니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onAddMember(projectId, targetUserId) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = actionText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = { /* 거절 로직 */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "거절",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * 기본 시스템 메시지 컴포넌트 (알 수 없는 타입용)
 */
@Composable
fun DefaultSystemMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

// parsePayload 함수 제거 - MessagePayload.getValue() 사용

// Preview Composables
@Preview(showBackground = true)
@Composable
private fun DateSystemMessagePreview() {
    DateSystemMessage(
        payload = """{"date": "2024-01-01", "displayText": "2024년 1월 1일"}"""
    )
}

@Preview(showBackground = true)
@Composable
private fun ChatStartSystemMessagePreview() {
    ChatStartSystemMessage(
        payload = """{"channelName": "일반", "welcomeText": "채팅이 시작되었습니다"}"""
    )
}

@Preview(showBackground = true)
@Composable
private fun ProjectJoinSystemMessagePreview() {
    ProjectJoinSystemMessage(
        payload = """{"projectId": "project123", "projectName": "새로운 프로젝트", "actionText": "참여하기"}""",
        onJoinProject = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun MemberInvitationSystemMessagePreview() {
    MemberInvitationSystemMessage(
        payload = """{"projectId": "project123", "projectName": "새로운 프로젝트", "inviterName": "김철수", "targetUserId": "user456", "actionText": "멤버로 추가"}""",
        onAddMember = { _, _ -> }
    )
}

@Preview(showBackground = true)
@Composable
private fun DefaultSystemMessagePreview() {
    DefaultSystemMessage(
        message = "알 수 없는 시스템 메시지입니다"
    )
}