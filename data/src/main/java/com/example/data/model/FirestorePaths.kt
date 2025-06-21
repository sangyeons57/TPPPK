package com.example.data.model

import com.example.data.model.remote.*

/**
 * Centralised helpers for building Firestore document / collection paths.
 * Each helper directly references the `COLLECTION_NAME` constant declared in the
 * corresponding DTO, so there is a single source of truth for collection names.
 */
object FirestorePaths {

    /* --------------------  User -------------------- */
    fun user(userId: String) = "${UserDTO.COLLECTION_NAME}/$userId"

    fun userFriends(userId: String) = "${user(userId)}/${FriendDTO.COLLECTION_NAME}"
    fun userFriend(userId: String, friendId: String) = "${userFriends(userId)}/$friendId"

    fun userDmWrappers(userId: String) = "${user(userId)}/${DMWrapperDTO.COLLECTION_NAME}"
    fun userDmWrapper(userId: String, dmChannelId: String) = "${userDmWrappers(userId)}/$dmChannelId"

    fun userProjectWrappers(userId: String) = "${user(userId)}/${ProjectsWrapperDTO.COLLECTION_NAME}"
    fun userProjectWrapper(userId: String, projectId: String) = "${userProjectWrappers(userId)}/$projectId"

    /* --------------------  DM Channel -------------------- */
    fun dmChannel(dmChannelId: String) = "${DMChannelDTO.COLLECTION_NAME}/$dmChannelId"

    fun dmChannelMessages(dmChannelId: String) = "${dmChannel(dmChannelId)}/${MessageDTO.COLLECTION_NAME}"
    fun dmChannelMessage(dmChannelId: String, messageId: String) = "${dmChannelMessages(dmChannelId)}/$messageId"

    fun dmMessageAttachments(dmChannelId: String, messageId: String) =
        "${dmChannelMessage(dmChannelId, messageId)}/${MessageAttachmentDTO.COLLECTION_NAME}"
    fun dmMessageAttachment(dmChannelId: String, messageId: String, attachmentId: String) =
        "${dmMessageAttachments(dmChannelId, messageId)}/$attachmentId"

    /* --------------------  Project -------------------- */
    fun project(projectId: String) = "${ProjectDTO.COLLECTION_NAME}/$projectId"

    fun projectMembers(projectId: String) = "${project(projectId)}/${MemberDTO.COLLECTION_NAME}"
    fun projectMember(projectId: String, memberUserId: String) = "${projectMembers(projectId)}/$memberUserId"

    fun projectRoles(projectId: String) = "${project(projectId)}/${RoleDTO.COLLECTION_NAME}"
    fun projectRole(projectId: String, roleId: String) = "${projectRoles(projectId)}/$roleId"

    fun projectRolePermissions(projectId: String, roleId: String) =
        "${projectRole(projectId, roleId)}/${PermissionDTO.COLLECTION_NAME}"
    fun projectRolePermission(projectId: String, roleId: String, permissionName: String) =
        "${projectRolePermissions(projectId, roleId)}/$permissionName"

    fun projectInvites(projectId: String) = "${project(projectId)}/${InviteDTO.COLLECTION_NAME}"
    fun projectInvite(projectId: String, inviteId: String) = "${projectInvites(projectId)}/$inviteId"

    fun projectCategories(projectId: String) = "${project(projectId)}/${CategoryDTO.COLLECTION_NAME}"
    fun projectCategory(projectId: String, categoryId: String) = "${projectCategories(projectId)}/$categoryId"

    fun projectCategoryChannels(projectId: String, categoryId: String) =
        "${projectCategory(projectId, categoryId)}/${ProjectChannelDTO.COLLECTION_NAME}"
    fun projectCategoryChannel(projectId: String, categoryId: String, channelId: String) =
        "${projectCategoryChannels(projectId, categoryId)}/$channelId"

    fun projectChannelMessages(projectId: String, categoryId: String, channelId: String) =
        "${projectCategoryChannel(projectId, categoryId, channelId)}/${MessageDTO.COLLECTION_NAME}"
    fun projectChannelMessage(projectId: String, categoryId: String, channelId: String, messageId: String) =
        "${projectChannelMessages(projectId, categoryId, channelId)}/$messageId"

    fun projectMessageAttachments(projectId: String, categoryId: String, channelId: String, messageId: String) =
        "${projectChannelMessage(projectId, categoryId, channelId, messageId)}/${MessageAttachmentDTO.COLLECTION_NAME}"
    fun projectMessageAttachment(
        projectId: String,
        categoryId: String,
        channelId: String,
        messageId: String,
        attachmentId: String
    ) = "${projectMessageAttachments(projectId, categoryId, channelId, messageId)}/$attachmentId"

    /* --------------------  Schedule -------------------- */
    fun schedule(scheduleId: String) = "${ScheduleDTO.COLLECTION_NAME}/$scheduleId"
}



import com.example.data.model.remote.UserDTO
import com.example.data.model.remote.DMChannelDTO
import com.example.data.model.remote.ProjectDTO
import com.example.data.model.remote.ScheduleDTO
import com.example.data.model.remote.CategoryDTO
import com.example.data.model.remote.FriendDTO
import com.example.data.model.remote.DMWrapperDTO
import com.example.data.model.remote.ProjectsWrapperDTO
import com.example.data.model.remote.MessageDTO
import com.example.data.model.remote.MessageAttachmentDTO
import com.example.data.model.remote.MemberDTO
import com.example.data.model.remote.RoleDTO
import com.example.data.model.remote.PermissionDTO
import com.example.data.model.remote.InviteDTO
import com.example.data.model.remote.ProjectChannelDTO

/**
 * Helper object to generate Firestore paths.
 * This ensures consistency in accessing Firestore collections and documents
 * by referencing COLLECTION_NAME constants from DTOs where applicable.
 */
object FirestorePaths {

    /* =====================
       User paths
       ===================== */
    fun user(userId: String) = "${UserDTO.COLLECTION_NAME}/$userId"

    fun userFriends(userId: String) = "${user(userId)}/${FriendDTO.COLLECTION_NAME}"
    fun userFriend(userId: String, friendId: String) = "${userFriends(userId)}/$friendId"

    fun userDmWrappers(userId: String) = "${user(userId)}/${DMWrapperDTO.COLLECTION_NAME}"
    fun userDmWrapper(userId: String, dmChannelId: String) = "${userDmWrappers(userId)}/$dmChannelId"

    fun userProjectWrappers(userId: String) = "${user(userId)}/${ProjectsWrapperDTO.COLLECTION_NAME}"
    fun userProjectWrapper(userId: String, projectId: String) = "${userProjectWrappers(userId)}/$projectId"

    /* =====================
       DM-channel paths
       ===================== */
    fun dmChannel(dmChannelId: String) = "${DMChannelDTO.COLLECTION_NAME}/$dmChannelId"

    fun dmChannelMessages(dmChannelId: String) = "${dmChannel(dmChannelId)}/${MessageDTO.COLLECTION_NAME}"
    fun dmChannelMessage(dmChannelId: String, messageId: String) = "${dmChannelMessages(dmChannelId)}/$messageId"

    fun dmMessageAttachments(dmChannelId: String, messageId: String) =
        "${dmChannelMessage(dmChannelId, messageId)}/${MessageAttachmentDTO.COLLECTION_NAME}"
    fun dmMessageAttachment(dmChannelId: String, messageId: String, attachmentId: String) =
        "${dmMessageAttachments(dmChannelId, messageId)}/$attachmentId"

    /* =====================
       Project paths
       ===================== */
    fun project(projectId: String) = "${ProjectDTO.COLLECTION_NAME}/$projectId"

    fun projectMembers(projectId: String) = "${project(projectId)}/${MemberDTO.COLLECTION_NAME}"
    fun projectMember(projectId: String, memberUserId: String) = "${projectMembers(projectId)}/$memberUserId"

    fun projectRoles(projectId: String) = "${project(projectId)}/${RoleDTO.COLLECTION_NAME}"
    fun projectRole(projectId: String, roleId: String) = "${projectRoles(projectId)}/$roleId"

    fun projectRolePermissions(projectId: String, roleId: String) =
        "${projectRole(projectId, roleId)}/${PermissionDTO.COLLECTION_NAME}"
    fun projectRolePermission(projectId: String, roleId: String, permissionName: String) =
        "${projectRolePermissions(projectId, roleId)}/$permissionName"

    fun projectInvites(projectId: String) = "${project(projectId)}/${InviteDTO.COLLECTION_NAME}"
    fun projectInvite(projectId: String, inviteId: String) = "${projectInvites(projectId)}/$inviteId"

    fun projectCategories(projectId: String) = "${project(projectId)}/${CategoryDTO.COLLECTION_NAME}"
    fun projectCategory(projectId: String, categoryId: String) = "${projectCategories(projectId)}/$categoryId"

    fun projectCategoryChannels(projectId: String, categoryId: String) =
        "${projectCategory(projectId, categoryId)}/${ProjectChannelDTO.COLLECTION_NAME}"
    fun projectCategoryChannel(projectId: String, categoryId: String, channelId: String) =
        "${projectCategoryChannels(projectId, categoryId)}/$channelId"

    fun projectChannelMessages(projectId: String, categoryId: String, channelId: String) =
        "${projectCategoryChannel(projectId, categoryId, channelId)}/${MessageDTO.COLLECTION_NAME}"
    fun projectChannelMessage(projectId: String, categoryId: String, channelId: String, messageId: String) =
        "${projectChannelMessages(projectId, categoryId, channelId)}/$messageId"

    fun projectMessageAttachments(projectId: String, categoryId: String, channelId: String, messageId: String) =
        "${projectChannelMessage(projectId, categoryId, channelId, messageId)}/${MessageAttachmentDTO.COLLECTION_NAME}"
    fun projectMessageAttachment(
        projectId: String,
        categoryId: String,
        channelId: String,
        messageId: String,
        attachmentId: String
    ) = "${projectMessageAttachments(projectId, categoryId, channelId, messageId)}/$attachmentId"

    /* =====================
       Schedule paths
       ===================== */
    fun schedule(scheduleId: String) = "${ScheduleDTO.COLLECTION_NAME}/$scheduleId"
}
