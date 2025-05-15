package com.example.core_common.constants

/**
 * Firebase Firestore 컬렉션 및 필드 이름을 상수로 관리하는 객체입니다.
 * 문자열 하드코딩을 방지하고 일관된 참조를 보장합니다.
 */
object FirestoreConstants {
    /**
     * Firestore 컬렉션 이름
     */
    const val DB_NAME = "default"
    object Collections {
        const val USERS = "users"
        const val PROJECTS = "projects"
        const val MEMBERS = "members" // Project subcollection
        const val ROLES = "roles" // Project subcollection
        const val CATEGORIES = "categories" // Project subcollection
        const val CHANNELS = "channels" // Global collection
        const val MESSAGES = "messages" // Channel subcollection
        const val INVITES = "invites"
        const val SCHEDULES = "schedules"
        const val FRIENDS = "friends" // User subcollection
        const val PERMISSIONS = "permissions" // Channel subcollection for permissions
        const val PASSWORD_RESET_TOKENS = "passwordResetTokens"
        // const val PARTICIPANTS = "participants" // Removed: Channel access is now managed via members collection
        // const val DMS = "dms" // DEPRECATED: DMs are now managed via global channels collection
    }

    /**
     * 사용자 관련 필드 (`users/{userId}`)
     */
    object UserFields {
        const val USER_ID = "userId"
        const val NAME = "name" // 사용자 이름 (닉네임) - Schema: name
        const val EMAIL = "email" // 이메일 주소 - Schema: email
        const val PROFILE_IMAGE_URL = "profileImageUrl" // 프로필 이미지 URL - Schema: profileImageUrl
        const val STATUS_MESSAGE = "statusMessage" // 상태 메시지 - Schema: statusMessage
        const val MEMO = "memo" // 개인 메모 또는 소개 - Schema: memo
        const val STATUS = "status" // 현재 접속 상태 (ONLINE, OFFLINE 등) - Schema: status
        const val CREATED_AT = "createdAt" // 계정 생성 시간 - Schema: createdAt
        const val UPDATED_AT = "updatedAt" // 계정 마지막 수정 시간 - Schema: updatedAt
        const val FCM_TOKEN = "fcmToken" // FCM 토큰 - Schema: fcmToken
        const val ACCOUNT_STATUS = "accountStatus" // 계정 상태 (ACTIVE, SUSPENDED, DELETED 등) - Schema: accountStatus
        const val IS_EMAIL_VERIFIED = "isEmailVerified" // 이메일 인증 여부 - Schema: isEmailVerified
        const val PARTICIPATING_PROJECT_IDS = "participatingProjectIds" // 참여중인 프로젝트 ID 목록 - Schema: participatingProjectIds
        const val ACTIVE_DM_IDS = "activeDmIds" // 활성화된 DM 채널 ID 목록 (references /channels/{channelId}) - Schema: activeDmIds
        // const val ID = "id" // Removed: User document ID is the Firebase Auth UID, 'id' is not a field within the document per schema.
        // const val LAST_ACTIVE_AT = "lastActiveAt" // Removed: Not in schema
    }

    /**
     * 프로젝트 관련 필드 (`projects/{projectId}`)
     */
    object ProjectFields {
        const val NAME = "name" // 프로젝트 이름 - Schema: name
        const val DESCRIPTION = "description" // 프로젝트 설명 - Schema: description
        const val IMAGE_URL = "imageUrl" // 프로젝트 이미지 URL - Schema: imageUrl
        const val OWNER_ID = "ownerId" // 프로젝트 소유자 ID - Schema: ownerId
        const val MEMBER_IDS = "memberIds" // 프로젝트 멤버 ID 목록 - Schema: memberIds
        const val CREATED_AT = "createdAt" // 프로젝트 생성 시간 - Schema: createdAt
        const val UPDATED_AT = "updatedAt" // 프로젝트 마지막 수정 시간 - Schema: updatedAt
        const val IS_PUBLIC = "isPublic" // 프로젝트 공개 여부 - Schema: isPublic
        const val CATEGORY_ID = "categoryId" // 프로젝트가 속한 카테고리 ID (선택 사항)
        // const val ID = "id" // Removed: Project document ID is auto-generated, 'id' is not a field within the document per schema.
        // const val PARTICIPATING_MEMBERS = "participatingMembers" // Removed: Renamed to MEMBER_IDS based on schema
    }

    /**
     * 프로젝트 멤버 관련 필드 (`projects/{projectId}/members/{userId}`)
     */
    object MemberFields {
        const val ROLE_IDS = "roleIds" // 역할 ID 목록 - Schema: roleIds
        const val JOINED_AT = "joinedAt" // 참여 시간 - Schema: joinedAt
        const val CHANNEL_IDS = "channelIds" // 접근 가능한 채널 ID 목록 - Schema: channelIds
        // const val ADDED_AT = "addedAt" // Removed: Not in schema
        // const val ADDED_BY = "addedBy" // Removed: Not in schema
        // const val UPDATED_AT = "updatedAt" // Removed: Not in schema
        // const val UPDATED_BY = "updatedBy" // Removed: Not in schema
    }

    /**
     * 프로젝트 역할 관련 필드 (`projects/{projectId}/roles/{roleId}`)
     */
    object RoleFields {
        const val NAME = "name" // 역할 이름 - Schema: name
        const val PERMISSIONS = "permissions" // 권한 맵 - Schema: permissions
        const val IS_DEFAULT = "isDefault" // 기본 역할 여부 - Schema: isDefault
        const val CREATED_AT = "createdAt" // 역할 생성 시간
        const val UPDATED_AT = "updatedAt" // 역할 마지막 수정 시간
        // const val COLOR = "color" // Removed: Not in schema
        // const val CREATED_BY = "createdBy" // Removed: Not in schema
        // const val UPDATED_BY = "updatedBy" // Removed: Not in schema
    }

    /**
     * 프로젝트 카테고리 관련 필드 (`projects/{projectId}/categories/{categoryId}`)
     */
    object CategoryFields {
        const val NAME = "name" // 카테고리 이름 - Schema: name
        const val ORDER = "order" // 카테고리 순서 - Schema: order
        // 스키마에는 없지만 데이터 무결성을 위해 사용될 수 있는 필드들
        const val CREATED_AT = "createdAt" 
        const val CREATED_BY = "createdBy"
        const val UPDATED_AT = "updatedAt"
        const val UPDATED_BY = "updatedBy"
    }

    /**
     * 채널 관련 필드 (`channels/{channelId}`) - Global collection
     */
    object ChannelFields {
        const val ID = "id" // 채널 ID (문서 ID와 동일) - Schema: id
        const val NAME = "name" // 채널 이름 - Schema: name
        const val DESCRIPTION = "description" // 채널 설명 - Schema: description
        const val CHANNEL_TYPE = "type" // 채널 타입 (DM, PROJECT, CATEGORY) - Schema: type
        const val LAST_MESSAGE_PREVIEW = "lastMessagePreview" // 마지막 메시지 미리보기 - Schema: lastMessagePreview
        const val LAST_MESSAGE_TIMESTAMP = "lastMessageTimestamp" // 마지막 메시지 시간 - Schema: lastMessageTimestamp
        const val CREATED_AT = "createdAt" // 채널 생성 시간 - Schema: createdAt
        const val CREATED_BY = "createdBy" // 채널 생성자 ID - Schema: createdBy
        const val UPDATED_AT = "updatedAt" // 채널 마지막 수정 시간 - Schema: updatedAt
        
        // 새로운 타입별 특화 데이터 필드
        const val PROJECT_SPECIFIC_DATA = "projectSpecificData" // 프로젝트/카테고리 채널 특화 데이터
        const val DM_SPECIFIC_DATA = "dmSpecificData" // DM 채널 특화 데이터
        
        // 제거될 필드 (기존 필드 주석 처리)
        // const val ORDER = "order" // 제거: projectSpecificData.order로 이동
        // const val PARTICIPANT_IDS = "participantIds" // 제거: dmSpecificData.participantIds로 이동
        // const val METADATA = "metadata" // 제거: 타입별 특화 데이터로 대체
        // const val CHANNEL_MODE = "channelMode" // 제거: projectSpecificData.channelMode로 이동
    }

    /**
     * 프로젝트 채널 특화 데이터 필드 (`channels/{channelId}.projectSpecificData`)
     */
    object ChannelProjectDataFields {
        const val PROJECT_ID = "projectId" // 프로젝트 ID
        const val CATEGORY_ID = "categoryId" // 카테고리 ID (null인 경우 프로젝트 직속 채널)
        const val ORDER = "order" // 채널 표시 순서
        const val CHANNEL_MODE = "channelMode" // 채널 모드 (TEXT, VOICE 등) - Schema: projectSpecificData.channelMode
    }

    /**
     * DM 채널 특화 데이터 필드 (`channels/{channelId}.dmSpecificData`)
     */
    object ChannelDmDataFields {
        const val PARTICIPANT_IDS = "participantIds" // 참여자 ID 목록
    }

    /**
     * 초대 관련 필드 (`invites/{inviteToken}`)
     */
    object InviteFields {
        const val TYPE = "type" // 초대 유형 (e.g., project_invite) - Schema: type
        const val INVITER_ID = "inviterId" // 초대한 사용자 ID - Schema: inviterId
        const val INVITER_NAME = "inviterName" // 초대한 사용자 이름 - Schema: inviterName
        const val PROJECT_ID = "projectId" // 대상 프로젝트 ID - Schema: projectId
        const val PROJECT_NAME = "projectName" // 대상 프로젝트 이름 - Schema: projectName
        const val CREATED_AT = "createdAt" // 초대 생성 시간 - Schema: createdAt
        const val EXPIRES_AT = "expiresAt" // 초대 만료 시간 - Schema: expiresAt
    }

    /**
     * 친구 관련 필드 (`users/{userId}/friends/{friendId}`)
     */
    object FriendFields {
        const val STATUS = "status" // 친구 관계 상태 (accepted, pending_sent, pending_received) - Schema: status
        const val TIMESTAMP = "timestamp" // 관계 생성/변경 시간 - Schema: Instant
        const val ACCEPTED_AT = "acceptedAt" // 수락된 시간 - Schema: acceptedAt
    }

    /**
     * 메시지 관련 필드 (`channels/{channelId}/messages/{messageId}`)
     */
    object MessageFields {
        const val ID = "id" // 메시지 ID (문서 ID와 동일) - Schema: id
        const val CHANNEL_ID = "channelId" // 메시지가 속한 채널 ID - Added for ChatMessageDto
        const val SENDER_ID = "senderId" // 보낸 사람 ID - Schema: senderId
        const val SENDER_NAME = "senderName" // 보낸 사람 이름 - Corresponds to ChatMessageDto.senderName, schema's senderName
        const val SENDER_PROFILE_URL = "senderProfileUrl" // 보낸 사람 프로필 URL - Corresponds to ChatMessageDto.senderProfileUrl, schema's senderProfileUrl
        const val MESSAGE = "text" // 메시지 내용 - Schema: text (formerly MESSAGE = "message")
        const val SENT_AT = "sentAt" // 메시지 생성 시간 (Firestore Timestamp) - Schema: timestamp (formerly SENT_AT = "sentAt")
        const val UPDATED_AT = "updatedAt" // 메시지 마지막 수정 시간 - Schema: updatedAt
        const val ATTACHMENTS = "attachments" // 첨부 파일 목록 - Schema: attachments
        const val REPLY_TO_MESSAGE_ID = "replyToMessageId" // 답장할 메시지의 ID - Schema: replyToMessageId
        const val REACTIONS = "reactions" // 메시지 반응 - Schema: reactions
        const val METADATA = "metadata" // 추가 메타데이터 - Schema: metadata
        const val IS_EDITED = "isEdited" // 메시지 수정 여부 - Schema: isEdited
        const val IS_DELETED = "isDeleted" // 메시지 삭제 여부 (논리적 삭제 플래그) - Schema: isDeleted
        // const val IS_MODIFIED = "isModified" // Removed: Replaced by IS_EDITED and schema update

        /**
         * 메시지 첨부 파일(attachments) 내부 Map에서 사용되는 키
         */
        object AttachmentMapKeys {
            const val ID = "id" // 첨부파일 자체의 ID (선택적, URL이 고유 식별자 역할을 할 수도 있음)
            const val TYPE = "type" // 파일 유형 (예: "image", "video", "file")
            const val URL = "url" // 파일 접근 URL
            const val FILE_NAME = "fileName" // 원본 파일 이름 (선택적)
            const val SIZE = "size" // 파일 크기 (Long, 문자열로 저장될 수도 있음) (선택적)
            const val MIME_TYPE = "mimeType" // 파일 MIME 타입 (선택적)
            const val THUMBNAIL_URL = "thumbnailUrl" // 썸네일 이미지 URL (이미지/비디오의 경우, 선택적)
        }

        // Fields based on older ChatMessageDto or specific local needs, review if still needed vs schema fields above
        // const val USER_NAME = "userName" // Deprecated: Use SENDER_NAME
        // const val USER_PROFILE_URL = "userProfileUrl" // Deprecated: Use SENDER_PROFILE_URL
        // const val MESSAGE = "message" // Deprecated: Use TEXT
        // const val SENT_AT = "sentAt" // Deprecated: Use TIMESTAMP
        // const val ATTACHMENT_IMAGE_URLS = "attachmentImageUrls" // Review: Schema uses a generic 'attachments' array of maps
    }


    /**
     * 일정 관련 필드 (`schedules/{scheduleId}`)
     */
    object ScheduleFields {
        const val TITLE = "title" // 일정 제목 - Schema: title
        const val CONTENT = "content" // 일정 설명 - Schema: description (formerly CONTENT)
        const val START_TIME = "startTime" // 시작 시간 - Schema: startTime
        const val END_TIME = "endTime" // 종료 시간 - Schema: endTime
        const val PROJECT_ID = "projectId" // 관련 프로젝트 ID - Schema: projectId
        const val CHANNEL_ID = "channelId" // 관련 채널 ID (references /channels/{channelId}) - Schema: channelId
        const val CREATOR_ID = "creatorId" // 생성자 ID - Schema: creatorId (formerly CREATED_BY)
        const val PARTICIPANT_IDS = "participantIds" // 참여자 ID 목록 - Schema: participantIds (formerly PARTICIPANTS)
        const val STATUS = "status" // 일정 상태 (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED) - Schema: status
        const val COLOR = "color" // 표시 색상 - Schema: color
        const val CREATED_AT = "createdAt" // 일정 생성 시간 - Schema: createdAt
        const val UPDATED_AT = "updatedAt" // 일정 마지막 수정 시간 - Schema: updatedAt
    }

    /**
     * 채널 참조 관련 필드 (`projects/{projectId}/categories/{categoryId}/channelReferences/{channelId}` 또는 
     * `projects/{projectId}/projectChannelReferences/{channelId}`)
     * 채널의 프로젝트 내 메타데이터와 표시 순서 등을 정의합니다.
     */
    object ChannelReferenceFields {
        const val ORDER = "order" // 채널 표시 순서
    }

    /**
     * 채널 내 권한 재정의 컬렉션 및 필드
     */
    const val PERMISSION_OVERRIDES = "permission_overrides"
    object ChannelPermissionOverrideFields {
        const val PERMISSIONS = "permissions" // Map<String, Boolean>
        const val USER_ID = "userId" // String
        const val UPDATED_AT = "updatedAt" // Timestamp
    }
} 