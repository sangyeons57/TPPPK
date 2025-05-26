package com.example.data.util

import com.example.core_common.constants.FirestoreConstants
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Firestore 상수 테스트
 * 
 * 이 테스트는 FirestoreConstants 클래스의 상수값이 예상대로 정의되어 있는지 확인합니다.
 * 이를 통해 상수 이름이나 값이 변경될 경우 테스트가 실패하여 코드 일관성을 유지할 수 있습니다.
 */
class FirestoreConstantsTest {

    /**
     * 컬렉션 이름 테스트
     */
    @Test
    fun testCollectionNames() {
        assertEquals("users", FirestoreConstants.Collections.USERS)
        assertEquals("projects", FirestoreConstants.Collections.PROJECTS)
        assertEquals("members", FirestoreConstants.Collections.MEMBERS)
        assertEquals("roles", FirestoreConstants.Collections.ROLES)
        assertEquals("categories", FirestoreConstants.Collections.CATEGORIES)
        assertEquals("channels", FirestoreConstants.Collections.CHANNELS)
        assertEquals("messages", FirestoreConstants.Collections.MESSAGES)
        assertEquals("invites", FirestoreConstants.Collections.INVITES)
        assertEquals("schedules", FirestoreConstants.Collections.SCHEDULES)
        assertEquals("friends", FirestoreConstants.Collections.FRIENDS)
        assertEquals("participants", FirestoreConstants.Collections.PARTICIPANTS)
    }

    /**
     * 사용자 필드 테스트
     */
    @Test
    fun testUserFields() {
        assertEquals("name", FirestoreConstants.UserFields.NAME)
        assertEquals("email", FirestoreConstants.UserFields.EMAIL)
        assertEquals("profileImageUrl", FirestoreConstants.UserFields.PROFILE_IMAGE_URL)
        assertEquals("statusMessage", FirestoreConstants.UserFields.STATUS_MESSAGE)
        assertEquals("memo", FirestoreConstants.UserFields.MEMO)
        assertEquals("status", FirestoreConstants.UserFields.STATUS)
        assertEquals("participatingProjectIds", FirestoreConstants.UserFields.PARTICIPATING_PROJECT_IDS)
        assertEquals("createdAt", FirestoreConstants.UserFields.CREATED_AT)
        assertEquals("activeDmIds", FirestoreConstants.UserFields.PARTICIPATING_DM_IDS)
        assertEquals("fcmToken", FirestoreConstants.UserFields.FCM_TOKEN)
        assertEquals("accountStatus", FirestoreConstants.UserFields.ACCOUNT_STATUS)
        assertEquals("isEmailVerified", FirestoreConstants.UserFields.IS_EMAIL_VERIFIED)
    }

    /**
     * 프로젝트 필드 테스트
     */
    @Test
    fun testProjectFields() {
        assertEquals("name", FirestoreConstants.ProjectFields.NAME)
        assertEquals("description", FirestoreConstants.ProjectFields.DESCRIPTION)
        assertEquals("imageUrl", FirestoreConstants.ProjectFields.IMAGE_URL)
        assertEquals("ownerId", FirestoreConstants.ProjectFields.OWNER_ID)
        assertEquals("memberIds", FirestoreConstants.ProjectFields.MEMBER_IDS)
        assertEquals("createdAt", FirestoreConstants.ProjectFields.CREATED_AT)
        assertEquals("updatedAt", FirestoreConstants.ProjectFields.UPDATED_AT)
        assertEquals("isPublic", FirestoreConstants.ProjectFields.IS_PUBLIC)
    }

    /**
     * 멤버 필드 테스트
     */
    @Test
    fun testMemberFields() {
        assertEquals("roleIds", FirestoreConstants.MemberFields.ROLE_IDS)
        assertEquals("joinedAt", FirestoreConstants.MemberFields.JOINED_AT)
    }

    /**
     * 역할 필드 테스트
     */
    @Test
    fun testRoleFields() {
        assertEquals("name", FirestoreConstants.RoleFields.NAME)
        assertEquals("permissions", FirestoreConstants.RoleFields.PERMISSIONS)
        assertEquals("isDefault", FirestoreConstants.RoleFields.IS_DEFAULT)
    }

    /**
     * 카테고리 필드 테스트
     */
    @Test
    fun testCategoryFields() {
        assertEquals("name", FirestoreConstants.CategoryFields.NAME)
        assertEquals("order", FirestoreConstants.CategoryFields.ORDER)
    }

    /**
     * 채널 필드 테스트
     */
    @Test
    fun testChannelFields() {
        assertEquals("id", FirestoreConstants.ChannelFields.ID)
        assertEquals("name", FirestoreConstants.ChannelFields.NAME)
        assertEquals("description", FirestoreConstants.ChannelFields.DESCRIPTION)
        assertEquals("participantIds", FirestoreConstants.ChannelFields.PARTICIPANT_IDS)
        assertEquals("lastMessagePreview", FirestoreConstants.ChannelFields.LAST_MESSAGE_PREVIEW)
        assertEquals("lastMessageTimestamp", FirestoreConstants.ChannelFields.LAST_MESSAGE_TIMESTAMP)
        assertEquals("metadata", FirestoreConstants.ChannelFields.METADATA)
        assertEquals("createdAt", FirestoreConstants.ChannelFields.CREATED_AT)
        assertEquals("createdBy", FirestoreConstants.ChannelFields.CREATED_BY)
        assertEquals("updatedAt", FirestoreConstants.ChannelFields.UPDATED_AT)
    }

    /**
     * 채널 참조 필드 테스트
     */
    @Test
    fun testChannelReferenceFields() {
        assertEquals("order", FirestoreConstants.ChannelReferenceFields.ORDER)
    }

    /**
     * 채널 메타데이터 키 테스트
     */
    @Test
    fun testChannelMetadataKeys() {
        assertEquals("source", FirestoreConstants.ChannelMetadataKeys.SOURCE)
        assertEquals("dmUsers", FirestoreConstants.ChannelMetadataKeys.DM_USERS)
        assertEquals("projectId", FirestoreConstants.ChannelMetadataKeys.PROJECT_ID)
        assertEquals("categoryId", FirestoreConstants.ChannelMetadataKeys.CATEGORY_ID)
        assertEquals("type", FirestoreConstants.ChannelMetadataKeys.TYPE)
    }

    /**
     * 채널 메타데이터 소스 값 테스트
     */
    @Test
    fun testChannelMetadataSourceValues() {
        assertEquals("dm", FirestoreConstants.ChannelMetadataSourceValues.DM)
        assertEquals("project", FirestoreConstants.ChannelMetadataSourceValues.PROJECT)
        assertEquals("schedule", FirestoreConstants.ChannelMetadataSourceValues.SCHEDULE)
    }

    /**
     * 초대 필드 테스트
     */
    @Test
    fun testInviteFields() {
        assertEquals("type", FirestoreConstants.InviteFields.TYPE)
        assertEquals("inviterId", FirestoreConstants.InviteFields.INVITER_ID)
        assertEquals("inviterName", FirestoreConstants.InviteFields.INVITER_NAME)
        assertEquals("projectId", FirestoreConstants.InviteFields.PROJECT_ID)
        assertEquals("projectName", FirestoreConstants.InviteFields.PROJECT_NAME)
        assertEquals("createdAt", FirestoreConstants.InviteFields.CREATED_AT)
        assertEquals("expiresAt", FirestoreConstants.InviteFields.EXPIRES_AT)
    }

    /**
     * 친구 필드 테스트
     */
    @Test
    fun testFriendFields() {
        assertEquals("status", FirestoreConstants.FriendFields.STATUS)
        assertEquals("timestamp", FirestoreConstants.FriendFields.TIMESTAMP)
        assertEquals("acceptedAt", FirestoreConstants.FriendFields.ACCEPTED_AT)
    }

    /**
     * 메시지 필드 테스트
     */
    @Test
    fun testMessageFields() {
        assertEquals("id", FirestoreConstants.MessageFields.ID)
        assertEquals("senderId", FirestoreConstants.MessageFields.SENDER_ID)
        assertEquals("text", FirestoreConstants.MessageFields.MESSAGE)
        assertEquals("timestamp", FirestoreConstants.MessageFields.SENT_AT)
        assertEquals("reactions", FirestoreConstants.MessageFields.REACTIONS)
        assertEquals("attachments", FirestoreConstants.MessageFields.ATTACHMENTS)
        assertEquals("metadata", FirestoreConstants.MessageFields.METADATA)
        assertEquals("replyToMessageId", FirestoreConstants.MessageFields.REPLY_TO_MESSAGE_ID)
        assertEquals("isEdited", FirestoreConstants.MessageFields.IS_EDITED)
        assertEquals("isDeleted", FirestoreConstants.MessageFields.IS_DELETED)
        assertEquals("isModified", FirestoreConstants.MessageFields.IS_EDITED)
    }

    /**
     * 일정 필드 테스트
     */
    @Test
    fun testScheduleFields() {
        assertEquals("title", FirestoreConstants.ScheduleFields.TITLE)
        assertEquals("description", FirestoreConstants.ScheduleFields.CONTENT)
        assertEquals("startTime", FirestoreConstants.ScheduleFields.START_TIME)
        assertEquals("endTime", FirestoreConstants.ScheduleFields.END_TIME)
        assertEquals("location", FirestoreConstants.ScheduleFields.LOCATION)
        assertEquals("projectId", FirestoreConstants.ScheduleFields.PROJECT_ID)
        assertEquals("channelId", FirestoreConstants.ScheduleFields.CHANNEL_ID)
        assertEquals("creatorId", FirestoreConstants.ScheduleFields.CREATOR_ID)
        assertEquals("participantIds", FirestoreConstants.ScheduleFields.PARTICIPANT_IDS)
        assertEquals("isAllDay", FirestoreConstants.ScheduleFields.IS_ALL_DAY)
        assertEquals("priority", FirestoreConstants.ScheduleFields.PRIORITY)
        assertEquals("status", FirestoreConstants.ScheduleFields.STATUS)
        assertEquals("reminderTime", FirestoreConstants.ScheduleFields.REMINDER_TIME)
        assertEquals("color", FirestoreConstants.ScheduleFields.COLOR)
        assertEquals("tags", FirestoreConstants.ScheduleFields.TAGS)
        assertEquals("recurrenceRule", FirestoreConstants.ScheduleFields.RECURRENCE_RULE)
        assertEquals("createdAt", FirestoreConstants.ScheduleFields.CREATED_AT)
        assertEquals("updatedAt", FirestoreConstants.ScheduleFields.UPDATED_AT)
    }

    /**
     * 상태 상수 테스트
     */
    @Test
    fun testStatusValues() {
        assertEquals("accepted", FirestoreConstants.Status.ACCEPTED)
        assertEquals("pending_sent", FirestoreConstants.Status.PENDING_SENT)
        assertEquals("pending_received", FirestoreConstants.Status.PENDING_RECEIVED)
        assertEquals("project_invite", FirestoreConstants.Status.PROJECT_INVITE)
    }
} 