package com.example.feature_chat.model

import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue
import com.example.domain.vo.MentionType
import com.example.domain.vo.message.MentionInfo
import com.example.websocket.core.WebSocketConnectionState

/**
 * ChatViewModel의 UI 상태를 관리하는 데이터 클래스
 */
data class ChatUiState(
    val channelId: String = "", // 생성자에서 초기화되므로 non-null
    val channelName: String = "채팅방",
    val channelPath: String = "",
    // Note: messages are now handled by Paging3 flow in ViewModel
    val isAttachmentAreaVisible: Boolean = false,
    val galleryImages: List<GalleryImageUiModel> = emptyList(), // ★ UI 모델 사용
    val selectedImages: Set<Uri> = emptySet(),
    // Note: loading states are now handled by Paging3 LoadState
    val isSendingMessage: Boolean = false, // ★ 이름 명확화: 메시지 전송 중
    val isLoadingHistory: Boolean = false, // 초기 메시지 로딩 상태 추가
    val isEditing: Boolean = false,
    val editingMessageId: String? = null, // Fixed: Message IDs are String-based DocumentIds
    val myUserId: String = "", // 실제로는 외부에서 주입 또는 설정 필요
    val currentUserId: String? = null, // Current authenticated user ID
    val myUserNameDisplay: String? = null, // For optimistic UI updates
    val myUserProfileUrl: String? = null, // For optimistic UI updates
    // Note: pagination state is now handled by Paging3
    val error: String? = null,
    // Added for ChatViewModel refactor
    val pendingMessageTextFieldValue: TextFieldValue = TextFieldValue(""),
    val selectedAttachmentUris: List<Uri> = emptyList(),
    val isLoadingGallery: Boolean = false,
    
    // WebSocket connection state
    val connectionState: WebSocketConnectionState = WebSocketConnectionState.Disconnected,
    val queuedMessagesCount: Int = 0,
    val failedMessagesCount: Int = 0, // 재시도 실패한 메시지 수
    val showConnectionError: Boolean = false,
    val isRetryingMessages: Boolean = false, // 메시지 재시도 진행 중

    // 업로드 상태
    val uploadingImagesCount: Int = 0, // 현재 업로드 중인 이미지 수
    val uploadProgress: Float = 0f, // 전체 업로드 진행률 (0.0 ~ 1.0)

    // Note: Pagination state is now handled by Paging3 LoadState
    
    // Profile loading state
    val loadingProfileUserIds: Set<String> = emptySet(),
    
    // Mention suggestion state
    val mentionSuggestions: List<MentionSuggestion> = emptyList(),
    val isMentionSuggestionVisible: Boolean = false,
    val mentionQueryText: String = "",
    val mentionQueryStartPosition: Int = -1,
    val selectedMentionIndex: Int = -1, // -1 means no selection
    val mentionSuggestionLimit: Int = 7,

    // Current message mentions (for composing message)
    val pendingMessageMentions: List<MentionInfo> = emptyList(),
    
    // Channel participants (for DM channels)
    val participants: List<ChatParticipant> = emptyList(),
    val isLoadingParticipants: Boolean = false,
    
    // Project members and roles (for Project channels)
    val projectMembers: List<ProjectMember> = emptyList(),
    val projectRoles: List<ProjectRole> = emptyList(),
    val isLoadingProjectData: Boolean = false,

    // Anchor jump state (for UI coordination)
    val isAnchorJumpInProgress: Boolean = false,

    // DM blocking state
    val isProjectChannel: Boolean = false, // true if projectId is not null
    val isDMBlocked: Boolean = false, // true if DM channel is blocked
    // Project channel cached permissions
    val canWrite: Boolean = true,
    val canInvite: Boolean = true
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
