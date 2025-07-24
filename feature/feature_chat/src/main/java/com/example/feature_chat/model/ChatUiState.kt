package com.example.feature_chat.model

import android.net.Uri
import com.example.core_common.websocket.WebSocketConnectionState
import com.example.domain.model.vo.MentionType

/**
 * ChatViewModel의 UI 상태를 관리하는 데이터 클래스
 */
data class ChatUiState(
    val channelId: String = "", // 생성자에서 초기화되므로 non-null
    val channelName: String = "채팅방",
    val channelPath: String = "",
    val messages: List<ChatMessageUiModel> = emptyList(), // ★ UI 모델 사용
    val messageInput: String = "",
    val isAttachmentAreaVisible: Boolean = false,
    val galleryImages: List<GalleryImageUiModel> = emptyList(), // ★ UI 모델 사용
    val selectedImages: Set<Uri> = emptySet(),
    val isLoadingHistory: Boolean = false, // ★ 이름 명확화: 과거 메시지 로딩
    val isSendingMessage: Boolean = false, // ★ 이름 명확화: 메시지 전송 중
    val isEditing: Boolean = false,
    val editingMessageId: String? = null, // Fixed: Message IDs are String-based DocumentIds
    val myUserId: String = "", // 실제로는 외부에서 주입 또는 설정 필요
    val currentUserId: String? = null, // Current authenticated user ID
    val myUserNameDisplay: String? = null, // For optimistic UI updates
    val myUserProfileUrl: String? = null, // For optimistic UI updates
    val isLastPage: Boolean = false,
    val error: String? = null,
    // Added for ChatViewModel refactor
    val pendingMessageText: String = "", 
    val selectedAttachmentUris: List<Uri> = emptyList(),
    val isLoadingGallery: Boolean = false,
    
    // WebSocket connection state
    val connectionState: WebSocketConnectionState = WebSocketConnectionState.Disconnected,
    val queuedMessagesCount: Int = 0,
    val showConnectionError: Boolean = false,
    
    // Pagination state
    val hasMoreMessages: Boolean = true,
    val isLoadingMoreMessages: Boolean = false,
    val lastMessageTimestamp: java.time.Instant? = null,
    
    // Profile loading state
    val loadingProfileUserIds: Set<String> = emptySet(),
    
    // Mention suggestion state
    val mentionSuggestions: List<MentionSuggestion> = emptyList(),
    val isMentionSuggestionVisible: Boolean = false,
    val mentionQueryText: String = "",
    val mentionQueryStartPosition: Int = -1,
    
    // Channel participants (for DM channels)
    val participants: List<ChatParticipant> = emptyList(),
    val isLoadingParticipants: Boolean = false,
    
    // Project members and roles (for Project channels)
    val projectMembers: List<ProjectMember> = emptyList(),
    val projectRoles: List<ProjectRole> = emptyList(),
    val isLoadingProjectData: Boolean = false
)

/**
 * Represents a mention suggestion item
 */
data class MentionSuggestion(
    val type: MentionType,
    val id: String,
    val displayName: String,
    val profileUrl: String? = null,
    val subtitle: String? = null // e.g., role description, user status
)

/**
 * Represents a participant in a DM channel
 */
data class ChatParticipant(
    val userId: String,
    val displayName: String,
    val profileUrl: String? = null,
    val isOnline: Boolean = false
)

/**
 * Represents a project member
 */
data class ProjectMember(
    val userId: String,
    val displayName: String,
    val profileUrl: String? = null,
    val roleId: String? = null,
    val roleName: String? = null
)

/**
 * Represents a project role
 */
data class ProjectRole(
    val roleId: String,
    val roleName: String,
    val memberCount: Int = 0,
    val color: String? = null
)