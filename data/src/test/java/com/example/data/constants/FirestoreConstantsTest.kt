package com.example.data.constants

import com.example.core_common.constants.FirestoreConstants
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * FirestoreConstants의 유효성과 일관성을 테스트합니다.
 * 상수 값이 예상대로 올바르게 정의되어 있는지 확인합니다.
 */
class FirestoreConstantsTest {

    @Test
    fun `collection names are correctly defined`() {
        // 주요 컬렉션 이름이 올바르게 정의되어 있는지 검증
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
    }

    @Test
    fun `user fields are correctly defined`() {
        // 사용자 필드 정의 검증
        assertEquals("id", FirestoreConstants.UserFields.ID)
        assertEquals("email", FirestoreConstants.UserFields.EMAIL)
        assertEquals("displayName", FirestoreConstants.UserFields.DISPLAY_NAME)
        assertEquals("profileImageUrl", FirestoreConstants.UserFields.PROFILE_IMAGE_URL)
        assertEquals("participatingProjectIds", FirestoreConstants.UserFields.PARTICIPATING_PROJECT_IDS)
        assertEquals("createdAt", FirestoreConstants.UserFields.CREATED_AT)
        assertEquals("lastActiveAt", FirestoreConstants.UserFields.LAST_ACTIVE_AT)
        assertEquals("activeDmIds", FirestoreConstants.UserFields.PARTICIPATING_DM_IDS)
    }

    @Test
    fun `project fields are correctly defined`() {
        // 프로젝트 필드 정의 검증
        assertEquals("name", FirestoreConstants.ProjectFields.NAME)
        assertEquals("description", FirestoreConstants.ProjectFields.DESCRIPTION)
        assertEquals("ownerId", FirestoreConstants.ProjectFields.OWNER_ID)
        assertEquals("createdAt", FirestoreConstants.ProjectFields.CREATED_AT)
        assertEquals("updatedAt", FirestoreConstants.ProjectFields.UPDATED_AT)
    }

    @Test
    fun `message fields are correctly defined`() {
        // 메시지 필드 정의 검증
        assertEquals("senderId", FirestoreConstants.MessageFields.SENDER_ID)
        assertEquals("content", FirestoreConstants.MessageFields.CONTENT)
        assertEquals("sentAt", FirestoreConstants.MessageFields.SENT_AT)
        assertEquals("chatId", FirestoreConstants.MessageFields.CHAT_ID)
        assertEquals("isModified", FirestoreConstants.MessageFields.IS_EDITED)
    }

    @Test
    fun `category fields are correctly defined`() {
        // 카테고리 필드 정의 검증
        assertEquals("name", FirestoreConstants.CategoryFields.NAME)
        assertEquals("order", FirestoreConstants.CategoryFields.ORDER)
        assertEquals("createdAt", FirestoreConstants.CategoryFields.CREATED_AT)
        assertEquals("createdBy", FirestoreConstants.CategoryFields.CREATED_BY)
    }

    @Test
    fun `channel fields are correctly defined`() {
        // 채널 필드 정의 검증
        assertEquals("name", FirestoreConstants.ChannelFields.NAME)
        assertEquals("type", FirestoreConstants.ChannelFields.CHANNEL_TYPE)
        assertEquals("order", FirestoreConstants.ChannelFields.ORDER)
        assertEquals("createdAt", FirestoreConstants.ChannelFields.CREATED_AT)
        assertEquals("createdBy", FirestoreConstants.ChannelFields.CREATED_BY)
    }

    @Test
    fun `status values are correctly defined`() {
        // 상태 상수 정의 검증
        assertEquals("accepted", FirestoreConstants.Status.ACCEPTED)
        assertEquals("pending_sent", FirestoreConstants.Status.PENDING_SENT)
        assertEquals("pending_received", FirestoreConstants.Status.PENDING_RECEIVED)
        assertEquals("project_invite", FirestoreConstants.Status.PROJECT_INVITE)
    }
} 