package com.example.core_common.constants

/**
 * Firebase Firestore 컬렉션 및 필드 이름을 상수로 관리하는 객체입니다.
 * 문자열 하드코딩을 방지하고 일관된 참조를 보장합니다.
 */
object FirestoreConstants {
    /**
     * Firestore 컬렉션 이름
     */
    object Collections {
        const val USERS = "users"
        const val PROJECTS = "projects"
        const val MEMBERS = "members"
        const val ROLES = "roles"
        const val CATEGORIES = "categories"
        const val CHANNELS = "channels"
        const val MESSAGES = "messages"
        const val DMS = "dms"
        const val INVITES = "invites"
        const val SCHEDULES = "schedules"
        const val FRIENDS = "friends"
        const val CHAT_CHANNELS = "chatChannels"
    }

    /**
     * 사용자 관련 필드
     */
    object UserFields {
        const val ID = "id"
        const val NICKNAME = "nickname"
        const val EMAIL = "email"
        const val PROFILE_IMAGE_URL = "profileImageUrl"
        const val PARTICIPATING_PROJECT_IDS = "participatingProjectIds"
        const val CREATED_AT = "createdAt"
        const val LAST_ACTIVE_AT = "lastActiveAt"
        const val ACTIVE_DM_IDS = "activeDmIds"
    }

    /**
     * 프로젝트 관련 필드
     */
    object ProjectFields {
        const val ID = "id"
        const val NAME = "name"
        const val DESCRIPTION = "description"
        const val OWNER_ID = "ownerId"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
        const val PARTICIPATING_MEMBERS = "participatingMembers"
    }

    /**
     * 멤버 관련 필드
     */
    object MemberFields {
        const val ROLE_IDS = "roleIds"
        const val JOINED_AT = "joinedAt"
        const val ADDED_AT = "addedAt"
        const val ADDED_BY = "addedBy"
        const val UPDATED_AT = "updatedAt"
        const val UPDATED_BY = "updatedBy"
    }

    /**
     * 역할 관련 필드
     */
    object RoleFields {
        const val NAME = "name"
        const val COLOR = "color"
        const val PERMISSIONS = "permissions"
        const val IS_DEFAULT = "isDefault"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
        const val CREATED_BY = "createdBy"
        const val UPDATED_BY = "updatedBy"
    }

    /**
     * 카테고리 관련 필드
     */
    object CategoryFields {
        const val NAME = "name"
        const val ORDER = "order"
        const val CREATED_AT = "createdAt"
        const val CREATED_BY = "createdBy"
        const val UPDATED_AT = "updatedAt"
        const val UPDATED_BY = "updatedBy"
    }

    /**
     * 채널 관련 필드
     */
    object ChannelFields {
        const val NAME = "name"
        const val TYPE = "type"
        const val ORDER = "order"
        const val CREATED_AT = "createdAt"
        const val CREATED_BY = "createdBy"
        const val UPDATED_AT = "updatedAt"
        const val UPDATED_BY = "updatedBy"
    }

    /**
     * 초대 관련 필드
     */
    object InviteFields {
        const val TYPE = "type"
        const val INVITER_ID = "inviterId"
        const val PROJECT_ID = "projectId"
        const val CREATED_AT = "createdAt"
        const val EXPIRES_AT = "expiresAt"
    }

    /**
     * 친구 관련 필드
     */
    object FriendFields {
        const val STATUS = "status"
        const val TIMESTAMP = "timestamp"
        const val ACCEPTED_AT = "acceptedAt"
    }

    /**
     * 메시지 관련 필드
     */
    object MessageFields {
        const val SENDER_ID = "senderId"
        const val CONTENT = "content"
        const val TIMESTAMP = "sentAt"
        const val READ_BY = "readBy"
        const val CHAT_ID = "chatId"
        const val IS_MODIFIED = "isModified"
    }

    /**
     * DM 관련 필드
     */
    object DmFields {
        const val PARTICIPANTS = "participants"
        const val LAST_MESSAGE = "lastMessage"
        const val LAST_MESSAGE_TIMESTAMP = "lastMessageTimestamp"
        const val CREATED_AT = "createdAt"
    }

    /**
     * 일정 관련 필드
     */
    object ScheduleFields {
        const val TITLE = "title"
        const val CONTENT = "content"
        const val START_TIME = "startTime"
        const val END_TIME = "endTime"
        const val PARTICIPANTS = "participants"
        const val PROJECT_ID = "projectId"
        const val IS_ALL_DAY = "isAllDay"
        const val CREATED_BY = "createdBy"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
    }

    /**
     * 공통 필드
     */
    object CommonFields {
        const val ID = "id"
        const val NAME = "name"
        const val DESCRIPTION = "description"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
    }

    /**
     * Firestore 상태값
     */
    object Status {
        // 친구 관계 상태
        const val ACCEPTED = "accepted"
        const val PENDING_SENT = "pending_sent"
        const val PENDING_RECEIVED = "pending_received"
        
        // 초대 유형
        const val PROJECT_INVITE = "project_invite"
    }
} 