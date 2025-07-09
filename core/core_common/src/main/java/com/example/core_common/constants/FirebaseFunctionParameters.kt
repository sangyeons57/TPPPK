package com.example.core_common.constants

/**
 * Firebase Functions에서 사용되는 함수 이름과 파라미터 이름들을 관리합니다.
 * Functions 쪽 constants.ts의 FUNCTION_PARAMETERS와 동일한 값을 유지해야 합니다.
 */
object FirebaseFunctionParameters {
    
    /**
     * Firebase Function 이름들
     */
    object Functions {
        // Test
        const val HELLO_WORLD = "helloWorld"
        
        // User Management
        const val UPDATE_USER_PROFILE = "updateUserProfile"
        
        // Friend Management
        const val SEND_FRIEND_REQUEST = "sendFriendRequest"
        const val ACCEPT_FRIEND_REQUEST = "acceptFriendRequest"
        const val REJECT_FRIEND_REQUEST = "rejectFriendRequest"
        const val REMOVE_FRIEND = "removeFriend"
        const val GET_FRIENDS = "getFriends"
        const val GET_FRIEND_REQUESTS = "getFriendRequests"
        
        // DM Management
        const val CREATE_DM_CHANNEL = "createDMChannel"
        const val BLOCK_DM_CHANNEL = "blockDMChannel"
        const val UNBLOCK_DM_CHANNEL = "unblockDMChannel"
        const val UNBLOCK_DM_CHANNEL_BY_USER_NAME = "unblockDMChannelByUserName"
        
        // Project Management
        const val GENERATE_INVITE_LINK = "generateInviteLink"
        const val VALIDATE_INVITE_CODE = "validateInviteCode"
        const val JOIN_PROJECT_WITH_INVITE = "joinProjectWithInvite"
        const val DELETE_PROJECT = "deleteProject"
    }
    
    /**
     * 친구 관리 관련 파라미터
     */
    object Friend {
        const val REQUESTER_ID = "requesterId"
        const val RECEIVER_USER_ID = "receiverUserId"
        const val FRIEND_REQUEST_ID = "friendRequestId"
        const val USER_ID = "userId"
        const val FRIEND_USER_ID = "friendUserId"
        const val STATUS = "status"
        const val LIMIT = "limit"
        const val OFFSET = "offset"
        const val TYPE = "type"
    }
    
    /**
     * DM 관리 관련 파라미터
     */
    object DM {
        const val CURRENT_USER_ID = "currentUserId"
        const val TARGET_USER_NAME = "targetUserName"
        const val CHANNEL_ID = "channelId"
    }
    
    /**
     * 사용자 관리 관련 파라미터
     */
    object User {
        const val USER_ID = "userId"
        const val NAME = "name"
        const val IMAGE_URL = "imageUrl"
        const val PROFILE_IMAGE_URL = "profileImageUrl"
        const val MEMO = "memo"
    }
    
    /**
     * 프로젝트 관리 관련 파라미터
     */
    object Project {
        const val PROJECT_ID = "projectId"
        const val USER_ID = "userId"
        const val MEMBER_ID = "memberId"
        const val INVITE_CODE = "inviteCode"
        const val PROJECT_NAME = "projectName"
        const val INVITER_ID = "inviterId"
        const val EXPIRES_IN_HOURS = "expiresInHours"
        const val MAX_USES = "maxUses"
        const val DELETED_BY = "deletedBy"
    }
} 