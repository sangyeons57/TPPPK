package com.example.core_common.constants

/**
 * Firestore 컬렉션 및 문서 필드 이름을 정의하는 상수 객체입니다.
 * (새로운 erDiagram.mmd 기반)
 */
object FirestoreConstants {

    const val DB_NAME = "default"

    object Collections {
        const val USERS = "users"
        const val DM_CHANNELS = "dm_channels"
        const val PROJECTS = "projects"
        const val SCHEDULES = "schedules"
        // Sub-collections will be accessed via parent paths + sub-collection name
    }

    object Users { // users/{userId}
        const val EMAIL = "email"
        const val NAME = "name"
        const val CONSENT_TIMESTAMP = "consentTimeStamp"
        const val PROFILE_IMAGE_URL = "profileImageUrl"
        const val MEMO = "memo"
        const val STATUS = "status" // User's online/offline status
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
        const val FCM_TOKEN = "fcmToken" // Note: ERD shows singular, DTO might have List
        const val ACCOUNT_STATUS = "accountStatus"

        object Friends { // users/{userId}/friends/{friendId}
            const val COLLECTION_NAME = "friends"
            const val FRIEND_NAME = "friendName" // Denormalized from User
            const val FRIEND_PROFILE_IMAGE_URL = "friendProfileImageUrl" // Denormalized
            const val STATUS = "status" // "PENDING_SENT", "PENDING_RECEIVED", "ACCEPTED", "DECLINED", "BLOCKED"
            const val REQUESTED_AT = "requestedAt"
            const val ACCEPTED_AT = "acceptedAt"
        }

        object DMWrappers { // users/{userId}/dm_wrapper/{dmChannelId}
            const val COLLECTION_NAME = "dm_wrapper"
            const val OTHER_USER_ID = "otherUserId"
            const val OTHER_USER_NAME = "otherUserName"
            const val OTHER_USER_PROFILE_IMAGE_URL = "otherUserProfileImageUrl"
            const val LAST_MESSAGE_PREVIEW = "lastMessagePreview"
            const val LAST_MESSAGE_TIMESTAMP = "lastMessageTimestamp"
        }

        object ProjectsWrappers { // users/{userId}/projects_wrapper/{projectId}
            const val COLLECTION_NAME = "projects_wrapper"
            const val PROJECT_NAME = "projectName"
            const val PROJECT_IMAGE_URL = "projectImageUrl"
        }
    }

    object DMChannel { // dm_channels/{dmChannelId}
        const val USER_ID_1 = "userId1"
        const val USER_ID_2 = "userId2"
        const val LAST_MESSAGE_PREVIEW = "lastMessagePreview"
        const val LAST_MESSAGE_TIMESTAMP = "lastMessageTimestamp"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"

        object Messages { // dm_channels/{dmChannelId}/messages/{messageId}
            const val COLLECTION_NAME = "messages"
            // Common MessageFields defined below
        }
    }

    object Project { // projects/{projectId}
        const val NAME = "name"
        const val IMAGE_URL = "imageUrl"
        const val OWNER_ID = "ownerId"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
        const val IS_PUBLIC = "isPublic"

        object Members { // projects/{projectId}/members/{userId}
            const val COLLECTION_NAME = "members"
            const val JOINED_AT = "joinedAt"
            const val ROLE_ID = "roleIds" // List<String>
        }

        object Roles { // projects/{projectId}/roles/{roleId}
            const val COLLECTION_NAME = "roles"
            const val NAME = "name"
            const val IS_DEFAULT = "isDefault"
        }

        object Permissions { // projects/{projectId}/roles/{roleId}/permissions/{permissionId}
            const val COLLECTION_NAME = "permissions" // Sub-collection of Role
            const val NAME = "name"
            const val DESCRIPTION = "description"
        }

        object Invites { // projects/{projectId}/invites/{inviteId}
            const val COLLECTION_NAME = "invites"
            const val INVITE_LINK = "inviteLink"
            const val STATUS = "status" // "ACTIVE", "INACTIVE", "EXPIRED"
            const val CREATED_BY = "createdBy"
            const val CREATED_AT = "createdAt"
            const val EXPIRES_AT = "expiresAt"
        }

        object Categories { // projects/{projectId}/categories/{categoryId}
            const val COLLECTION_NAME = "categories"
            const val NAME = "name"
            const val ORDER = "order"
            const val CREATED_BY = "createdBy"
            const val CREATED_AT = "createdAt"
            const val UPDATED_AT = "updatedAt"

            const val _DIRECT_CHANNELS_CATEGORY = "directChannels"
        }

        object Channels { // projects/{projectId}/categories/{categoryId}/project_channels/{projectChannelId}
            const val COLLECTION_NAME = "project_channels"
            const val CHANNEL_NAME = "channelName"
            const val ORDER = "order"
            const val CHANNEL_TYPE = "channelType" // MESSAGES, TASKS etc.
            const val CREATED_AT = "createdAt"
            const val UPDATED_AT = "updatedAt"
        }
    }

    // Common fields for Message (sub-collection of DMChannel or ProjectChannel)
    object MessageFields {
        const val COLLECTION_NAME = "messages"
        const val SENDER_ID = "senderId"
        const val SENDER_NAME = "senderName"
        const val SENDER_PROFILE_IMAGE_URL = "senderProfileImageUrl"
        const val SEND_MESSAGE = "sendMessage" // Text content of the message
        const val SENT_AT = "sentAt"
        const val UPDATED_AT = "updatedAt"
        const val REPLY_TO_MESSAGE_ID = "replyToMessageId"
        const val IS_DELETED = "isDeleted"

        object Attachments { // .../messages/{messageId}/message_attachments/{attachmentId}
            const val COLLECTION_NAME = "message_attachments"
            const val ATTACHMENT_TYPE = "attachmentType"
            const val ATTACHMENT_URL = "attachmentUrl"
        }
    }

    object Schedule { // schedules/{scheduleId}
        const val TITLE = "title"
        const val CONTENT = "content"
        const val START_TIME = "startTime"
        const val END_TIME = "endTime"
        const val PROJECT_ID = "projectId"
        const val CREATOR_ID = "creatorId"
        const val STATUS = "status"
        const val COLOR = "color"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
    }
}