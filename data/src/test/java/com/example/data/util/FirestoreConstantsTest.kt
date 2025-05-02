package com.example.data.util

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
     * 최상위 컬렉션 이름 테스트
     */
    @Test
    fun testTopLevelCollectionNames() {
        // 최상위 컬렉션 이름 확인
        assertEquals("users", FirestoreConstants.Collections.USERS)
        assertEquals("friendRequests", FirestoreConstants.Collections.FRIEND_REQUESTS)
        assertEquals("projects", FirestoreConstants.Collections.PROJECTS)
        assertEquals("schedules", FirestoreConstants.Collections.SCHEDULES)
        assertEquals("dmChannels", FirestoreConstants.Collections.DM_CHANNELS)
    }
    
    /**
     * 스토리지 경로 테스트
     */
    @Test
    fun testStoragePaths() {
        // 스토리지 경로 확인
        assertEquals("profileImages", FirestoreConstants.StoragePaths.PROFILE_IMAGES)
    }
    
    /**
     * Users 컬렉션 관련 상수 테스트
     */
    @Test
    fun testUsersConstants() {
        // Users 컬렉션 이름
        assertEquals("users", FirestoreConstants.Users.NAME)
        
        // Users 필드 이름
        assertEquals("userId", FirestoreConstants.Users.Fields.USER_ID)
        assertEquals("name", FirestoreConstants.Users.Fields.NAME)
        assertEquals("email", FirestoreConstants.Users.Fields.EMAIL)
        assertEquals("profileImageUrl", FirestoreConstants.Users.Fields.PROFILE_IMAGE_URL)
        assertEquals("status", FirestoreConstants.Users.Fields.STATUS)
        assertEquals("statusMessage", FirestoreConstants.Users.Fields.STATUS_MESSAGE)
        
        // 상태 값
        assertEquals("online", FirestoreConstants.Users.StatusValues.ONLINE)
        assertEquals("offline", FirestoreConstants.Users.StatusValues.OFFLINE)
        
        // Friends 서브컬렉션
        assertEquals("friends", FirestoreConstants.Users.FriendsSubcollection.NAME)
        assertEquals("addedAt", FirestoreConstants.Users.FriendsSubcollection.Fields.ADDED_AT)
    }
    
    /**
     * 친구 요청 컬렉션 관련 상수 테스트
     */
    @Test
    fun testFriendRequestsConstants() {
        // FriendRequests 컬렉션 이름
        assertEquals("friendRequests", FirestoreConstants.FriendRequests.NAME)
        
        // FriendRequests 필드 이름
        assertEquals("senderUid", FirestoreConstants.FriendRequests.Fields.SENDER_UID)
        assertEquals("receiverUid", FirestoreConstants.FriendRequests.Fields.RECEIVER_UID)
        assertEquals("status", FirestoreConstants.FriendRequests.Fields.STATUS)
        assertEquals("createdAt", FirestoreConstants.FriendRequests.Fields.CREATED_AT)
        
        // 상태 값
        assertEquals("pending", FirestoreConstants.FriendRequests.StatusValues.PENDING)
        assertEquals("accepted", FirestoreConstants.FriendRequests.StatusValues.ACCEPTED)
    }
    
    /**
     * 프로젝트 컬렉션 관련 상수 테스트
     */
    @Test
    fun testProjectsConstants() {
        // Projects 컬렉션 이름
        assertEquals("projects", FirestoreConstants.Projects.NAME)
        
        // Projects 필드 이름
        assertEquals("id", FirestoreConstants.Projects.Fields.ID)
        assertEquals("name", FirestoreConstants.Projects.Fields.NAME)
        assertEquals("description", FirestoreConstants.Projects.Fields.DESCRIPTION)
        assertEquals("imageUrl", FirestoreConstants.Projects.Fields.IMAGE_URL)
        assertEquals("memberCount", FirestoreConstants.Projects.Fields.MEMBER_COUNT)
        assertEquals("isPublic", FirestoreConstants.Projects.Fields.IS_PUBLIC)
        assertEquals("ownerId", FirestoreConstants.Projects.Fields.OWNER_ID)
        
        // Members 서브컬렉션
        assertEquals("members", FirestoreConstants.Projects.MembersSubcollection.NAME)
        assertEquals("userId", FirestoreConstants.Projects.MembersSubcollection.Fields.USER_ID)
        assertEquals("roleIds", FirestoreConstants.Projects.MembersSubcollection.Fields.ROLE_IDS)
        
        // Roles 서브컬렉션
        assertEquals("roles", FirestoreConstants.Projects.RolesSubcollection.NAME)
        assertEquals("name", FirestoreConstants.Projects.RolesSubcollection.Fields.ROLE_NAME)
        assertEquals("permissions", FirestoreConstants.Projects.RolesSubcollection.Fields.PERMISSIONS)
        
        // Categories 서브컬렉션
        assertEquals("categories", FirestoreConstants.Projects.CategoriesSubcollection.NAME)
        assertEquals("name", FirestoreConstants.Projects.CategoriesSubcollection.Fields.CATEGORY_NAME)
        assertEquals("order", FirestoreConstants.Projects.CategoriesSubcollection.Fields.ORDER)
        
        // Channels 서브컬렉션
        assertEquals("channels", FirestoreConstants.Projects.ChannelsSubcollection.NAME)
        assertEquals("categoryId", FirestoreConstants.Projects.ChannelsSubcollection.Fields.CATEGORY_ID)
        assertEquals("name", FirestoreConstants.Projects.ChannelsSubcollection.Fields.CHANNEL_NAME)
        assertEquals("type", FirestoreConstants.Projects.ChannelsSubcollection.Fields.TYPE)
        assertEquals("order", FirestoreConstants.Projects.ChannelsSubcollection.Fields.ORDER)
        
        // Channel 타입 값
        assertEquals("TEXT", FirestoreConstants.Projects.ChannelsSubcollection.TypeValues.TEXT)
        assertEquals("VOICE", FirestoreConstants.Projects.ChannelsSubcollection.TypeValues.VOICE)
        
        // Schedules 서브컬렉션
        assertEquals("schedules", FirestoreConstants.Projects.SchedulesSubcollection.NAME)
    }
    
    /**
     * 일정 컬렉션 관련 상수 테스트
     */
    @Test
    fun testSchedulesConstants() {
        // Schedules 컬렉션 이름
        assertEquals("schedules", FirestoreConstants.Schedules.NAME)
        
        // Schedules 필드 이름
        assertEquals("id", FirestoreConstants.Schedules.Fields.ID)
        assertEquals("projectId", FirestoreConstants.Schedules.Fields.PROJECT_ID)
        assertEquals("title", FirestoreConstants.Schedules.Fields.TITLE)
        assertEquals("content", FirestoreConstants.Schedules.Fields.CONTENT)
        assertEquals("startTime", FirestoreConstants.Schedules.Fields.START_TIME)
        assertEquals("endTime", FirestoreConstants.Schedules.Fields.END_TIME)
        assertEquals("attendees", FirestoreConstants.Schedules.Fields.ATTENDEES)
        assertEquals("isAllDay", FirestoreConstants.Schedules.Fields.IS_ALL_DAY)
    }
    
    /**
     * 채팅 메시지 관련 상수 테스트
     */
    @Test
    fun testChatMessagesConstants() {
        // 메시지 서브컬렉션 이름
        assertEquals("messages", FirestoreConstants.ChatMessages.SUBCOLLECTION_NAME)
        
        // 메시지 필드 이름
        assertEquals("chatId", FirestoreConstants.ChatMessages.Fields.CHAT_ID)
        assertEquals("channelId", FirestoreConstants.ChatMessages.Fields.CHANNEL_ID)
        assertEquals("userId", FirestoreConstants.ChatMessages.Fields.USER_ID)
        assertEquals("userName", FirestoreConstants.ChatMessages.Fields.USER_NAME)
        assertEquals("userProfileUrl", FirestoreConstants.ChatMessages.Fields.USER_PROFILE_URL)
        assertEquals("message", FirestoreConstants.ChatMessages.Fields.MESSAGE)
        assertEquals("sentAt", FirestoreConstants.ChatMessages.Fields.SENT_AT)
        assertEquals("isModified", FirestoreConstants.ChatMessages.Fields.IS_MODIFIED)
        assertEquals("attachmentImageUrls", FirestoreConstants.ChatMessages.Fields.ATTACHMENT_IMAGE_URLS)
    }
} 