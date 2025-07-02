package com.example.domain.model.vo

import com.example.domain.model.base.*

/**
 * Centralised helpers for building Firestore document / collection paths.
 * Each helper directly references the `COLLECTION_NAME` constant declared in the
 * corresponding domain model, providing a single source of truth for collection names.
 */
@JvmInline
value class CollectionPath(val value: String) {
    companion object {
        
        /* -------------------- Static Root Collections -------------------- */
        val users: CollectionPath get() = CollectionPath(User.COLLECTION_NAME)
        val dmChannels: CollectionPath get() = CollectionPath(DMChannel.COLLECTION_NAME)
        val projects: CollectionPath get() = CollectionPath(Project.COLLECTION_NAME)
        val schedules: CollectionPath get() = CollectionPath(Schedule.COLLECTION_NAME)
        
        /* -------------------- User Paths -------------------- */
        fun user(userId: String): CollectionPath = CollectionPath("${User.COLLECTION_NAME}/$userId")
        
        fun userFriends(userId: String): CollectionPath = 
            CollectionPath("${user(userId).value}/${Friend.COLLECTION_NAME}")
        fun userFriend(userId: String, friendId: String): CollectionPath = 
            CollectionPath("${userFriends(userId).value}/$friendId")
        
        fun userDmWrappers(userId: String): CollectionPath = 
            CollectionPath("${user(userId).value}/${DMWrapper.COLLECTION_NAME}")
        fun userDmWrapper(userId: String, dmChannelId: String): CollectionPath = 
            CollectionPath("${userDmWrappers(userId).value}/$dmChannelId")
        
        fun userProjectWrappers(userId: String): CollectionPath = 
            CollectionPath("${user(userId).value}/${ProjectsWrapper.COLLECTION_NAME}")
        fun userProjectWrapper(userId: String, projectId: String): CollectionPath = 
            CollectionPath("${userProjectWrappers(userId).value}/$projectId")

        fun userSchedules(userId: String): CollectionPath =
            CollectionPath("${user(userId).value}/${Schedule.COLLECTION_NAME}")
        
        /* -------------------- DM Channel Paths -------------------- */
        fun dmChannel(dmChannelId: String): CollectionPath = 
            CollectionPath("${DMChannel.COLLECTION_NAME}/$dmChannelId")
        
        fun dmChannelMessages(dmChannelId: String): CollectionPath = 
            CollectionPath("${dmChannel(dmChannelId).value}/${Message.COLLECTION_NAME}")
        fun dmChannelMessage(dmChannelId: String, messageId: String): CollectionPath = 
            CollectionPath("${dmChannelMessages(dmChannelId).value}/$messageId")
        
        fun dmMessageAttachments(dmChannelId: String, messageId: String): CollectionPath =
            CollectionPath("${dmChannelMessage(dmChannelId, messageId).value}/${MessageAttachment.COLLECTION_NAME}")
        fun dmMessageAttachment(dmChannelId: String, messageId: String, attachmentId: String): CollectionPath =
            CollectionPath("${dmMessageAttachments(dmChannelId, messageId).value}/$attachmentId")
        
        /* -------------------- Project Paths -------------------- */
        fun project(projectId: String): CollectionPath = 
            CollectionPath("${Project.COLLECTION_NAME}/$projectId")
        
        fun projectMembers(projectId: String): CollectionPath = 
            CollectionPath("${project(projectId).value}/${Member.COLLECTION_NAME}")
        fun projectMember(projectId: String, memberUserId: String): CollectionPath = 
            CollectionPath("${projectMembers(projectId).value}/$memberUserId")
        
        fun projectRoles(projectId: String): CollectionPath = 
            CollectionPath("${project(projectId).value}/${Role.COLLECTION_NAME}")
        fun projectRole(projectId: String, roleId: String): CollectionPath = 
            CollectionPath("${projectRoles(projectId).value}/$roleId")
        
        fun projectRolePermissions(projectId: String, roleId: String): CollectionPath =
            CollectionPath("${projectRole(projectId, roleId).value}/${Permission.COLLECTION_NAME}")
        fun projectRolePermission(projectId: String, roleId: String, permissionName: String): CollectionPath =
            CollectionPath("${projectRolePermissions(projectId, roleId).value}/$permissionName")
        
        fun projectInvites(projectId: String): CollectionPath = 
            CollectionPath("${project(projectId).value}/${Invite.COLLECTION_NAME}")
        fun projectInvite(projectId: String, inviteId: String): CollectionPath = 
            CollectionPath("${projectInvites(projectId).value}/$inviteId")
        
        fun projectCategories(projectId: String): CollectionPath = 
            CollectionPath("${project(projectId).value}/${Category.COLLECTION_NAME}")
        fun projectCategory(projectId: String, categoryId: String): CollectionPath = 
            CollectionPath("${projectCategories(projectId).value}/$categoryId")
        
        fun projectChannels(projectId: String): CollectionPath = 
            CollectionPath("${project(projectId).value}/${ProjectChannel.COLLECTION_NAME}")
        fun projectChannel(projectId: String, channelId: String): CollectionPath = 
            CollectionPath("${projectChannels(projectId).value}/$channelId")
        
        fun projectCategoryChannels(projectId: String, categoryId: String): CollectionPath =
            CollectionPath("${projectCategory(projectId, categoryId).value}/${ProjectChannel.COLLECTION_NAME}")
        fun projectCategoryChannel(projectId: String, categoryId: String, channelId: String): CollectionPath =
            CollectionPath("${projectCategoryChannels(projectId, categoryId).value}/$channelId")
        
        fun projectChannelMessages(projectId: String, categoryId: String, channelId: String): CollectionPath =
            CollectionPath("${projectCategoryChannel(projectId, categoryId, channelId).value}/${Message.COLLECTION_NAME}")
        fun projectChannelMessage(projectId: String, categoryId: String, channelId: String, messageId: String): CollectionPath =
            CollectionPath("${projectChannelMessages(projectId, categoryId, channelId).value}/$messageId")
        
        fun projectMessageAttachments(projectId: String, categoryId: String, channelId: String, messageId: String): CollectionPath =
            CollectionPath("${projectChannelMessage(projectId, categoryId, channelId, messageId).value}/${MessageAttachment.COLLECTION_NAME}")
        fun projectMessageAttachment(
            projectId: String,
            categoryId: String,
            channelId: String,
            messageId: String,
            attachmentId: String
        ): CollectionPath = CollectionPath("${projectMessageAttachments(projectId, categoryId, channelId, messageId).value}/$attachmentId")
        
        /* -------------------- Schedule Paths -------------------- */
        fun schedule(scheduleId: String): CollectionPath = 
            CollectionPath("${Schedule.COLLECTION_NAME}/$scheduleId")
    }
}
