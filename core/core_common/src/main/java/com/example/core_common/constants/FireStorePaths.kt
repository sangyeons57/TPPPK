package com.example.core_common.constants

import com.example.core_common.constants.FirestoreConstants // 실제 패키지 경로로 수정하세요.

object FirestorePaths {

    // 최상위 컬렉션 경로
    fun usersCol(): String = FirestoreConstants.Collections.USERS
    fun userDoc(userId: String): String = "${usersCol()}/$userId"

    fun dmChannelsCol(): String = FirestoreConstants.Collections.DM_CHANNELS
    fun dmChannelDoc(dmChannelId: String): String = "${dmChannelsCol()}/$dmChannelId"

    fun projectsCol(): String = FirestoreConstants.Collections.PROJECTS
    fun projectDoc(projectId: String): String = "${projectsCol()}/$projectId"

    fun schedulesCol(): String = FirestoreConstants.Collections.SCHEDULES
    fun scheduleDoc(scheduleId: String): String = "${schedulesCol()}/$scheduleId"

    // Users 하위 컬렉션 경로
    fun friendsCol(userId: String): String =
        "${userDoc(userId)}/${FirestoreConstants.Users.Friends.COLLECTION_NAME}"
    fun friendDoc(userId: String, friendId: String): String =
        "${friendsCol(userId)}/$friendId"

    fun dmWrappersCol(userId: String): String =
        "${userDoc(userId)}/${FirestoreConstants.Users.DMWrappers.COLLECTION_NAME}"
    fun dmWrapperDoc(userId: String, dmChannelId: String): String =
        "${dmWrappersCol(userId)}/$dmChannelId"

    fun projectsWrappersCol(userId: String): String =
        "${userDoc(userId)}/${FirestoreConstants.Users.ProjectsWrappers.COLLECTION_NAME}"
    fun projectsWrapperDoc(userId: String, projectId: String): String =
        "${projectsWrappersCol(userId)}/$projectId"

    // DMChannel 하위 컬렉션 경로
    fun dmMessagesCol(dmChannelId: String): String =
        "${dmChannelDoc(dmChannelId)}/${FirestoreConstants.MessageFields.COLLECTION_NAME}" // DMChannel.Messages.COLLECTION_NAME도 가능
    fun dmMessageDoc(dmChannelId: String, messageId: String): String =
        "${dmMessagesCol(dmChannelId)}/$messageId"

    // Project 하위 컬렉션 경로
    fun projectMembersCol(projectId: String): String =
        "${projectDoc(projectId)}/${FirestoreConstants.Project.Members.COLLECTION_NAME}"
    fun projectMemberDoc(projectId: String, userId: String): String =
        "${projectMembersCol(projectId)}/$userId"

    fun projectRolesCol(projectId: String): String =
        "${projectDoc(projectId)}/${FirestoreConstants.Project.Roles.COLLECTION_NAME}"
    fun projectRoleDoc(projectId: String, roleId: String): String =
        "${projectRolesCol(projectId)}/$roleId"

    fun projectRolePermissionsCol(projectId: String, roleId: String): String =
        "${projectRoleDoc(projectId, roleId)}/${FirestoreConstants.Project.Permissions.COLLECTION_NAME}"
    fun projectRolePermissionDoc(projectId: String, roleId: String, permissionId: String): String =
        "${projectRolePermissionsCol(projectId, roleId)}/$permissionId"

    fun projectInvitesCol(projectId: String): String =
        "${projectDoc(projectId)}/${FirestoreConstants.Project.Invites.COLLECTION_NAME}"
    fun projectInviteDoc(projectId: String, inviteId: String): String =
        "${projectInvitesCol(projectId)}/$inviteId"

    fun projectCategoriesCol(projectId: String): String =
        "${projectDoc(projectId)}/${FirestoreConstants.Project.Categories.COLLECTION_NAME}"
    fun projectCategoryDoc(projectId: String, categoryId: String): String =
        "${projectCategoriesCol(projectId)}/$categoryId"

    fun projectChannelsCol(projectId: String, categoryId: String): String =
        "${projectCategoryDoc(projectId, categoryId)}/${FirestoreConstants.Project.Channels.COLLECTION_NAME}"
    fun projectChannelDoc(projectId: String, categoryId: String, projectChannelId: String): String =
        "${projectChannelsCol(projectId, categoryId)}/$projectChannelId"

    // ProjectChannel 하위 메시지 컬렉션 경로
    fun projectChannelMessagesCol(projectId: String, categoryId: String, projectChannelId: String): String =
        "${projectChannelDoc(projectId, categoryId, projectChannelId)}/${FirestoreConstants.MessageFields.COLLECTION_NAME}"
    fun projectChannelMessageDoc(projectId: String, categoryId: String, projectChannelId: String, messageId: String): String =
        "${projectChannelMessagesCol(projectId, categoryId, projectChannelId)}/$messageId"

    // Message 하위 첨부파일 컬렉션 경로 (DM 또는 ProjectChannel 메시지 공통)
    // 이 함수들은 부모 메시지 경로를 인자로 받는 것이 더 일반적일 수 있습니다.
    // 예: fun messageAttachmentsCol(messagePath: String) = "$messagePath/${FirestoreConstants.MessageFields.Attachments.COLLECTION_NAME}"
    // 아래는 projectChannel의 메시지 첨부파일을 예시로 듭니다.
    fun projectChannelMessageAttachmentsCol(projectId: String, categoryId: String, projectChannelId: String, messageId: String): String =
        "${projectChannelMessageDoc(projectId, categoryId, projectChannelId, messageId)}/${FirestoreConstants.MessageFields.Attachments.COLLECTION_NAME}"
    fun projectChannelMessageAttachmentDoc(projectId: String, categoryId: String, projectChannelId: String, messageId: String, attachmentId: String): String =
        "${projectChannelMessageAttachmentsCol(projectId, categoryId, projectChannelId, messageId)}/$attachmentId"

    // DM 메시지 첨부파일 경로
     fun dmMessageAttachmentsCol(dmChannelId: String, messageId: String): String =
        "${dmMessageDoc(dmChannelId, messageId)}/${FirestoreConstants.MessageFields.Attachments.COLLECTION_NAME}"
    fun dmMessageAttachmentDoc(dmChannelId: String, messageId: String, attachmentId: String): String =
        "${dmMessageAttachmentsCol(dmChannelId, messageId)}/$attachmentId"
}