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
        
        // Project invitations stored as root collection for global accessibility
        fun projectInvitations(): CollectionPath = 
            CollectionPath(ProjectInvitation.COLLECTION_NAME)
        fun projectInvitation(inviteId: String): CollectionPath = 
            CollectionPath("${ProjectInvitation.COLLECTION_NAME}/$inviteId")
        
        fun projectCategories(projectId: String): CollectionPath = 
            CollectionPath("${project(projectId).value}/${Category.COLLECTION_NAME}")
        fun projectCategory(projectId: String, categoryId: String): CollectionPath = 
            CollectionPath("${projectCategories(projectId).value}/$categoryId")
        
        fun projectChannels(projectId: String): CollectionPath = 
            CollectionPath("${project(projectId).value}/${ProjectChannel.COLLECTION_NAME}")
        fun projectChannel(projectId: String, channelId: String): CollectionPath = 
            CollectionPath("${projectChannels(projectId).value}/$channelId")
        
        fun projectChannelMessages(projectId: String, channelId: String): CollectionPath =
            CollectionPath("${projectChannel(projectId, channelId).value}/${Message.COLLECTION_NAME}")
        fun projectChannelMessage(projectId: String, channelId: String, messageId: String): CollectionPath =
            CollectionPath("${projectChannelMessages(projectId, channelId).value}/$messageId")
        
        fun projectMessageAttachments(projectId: String, channelId: String, messageId: String): CollectionPath =
            CollectionPath("${projectChannelMessage(projectId, channelId, messageId).value}/${MessageAttachment.COLLECTION_NAME}")
        fun projectMessageAttachment(
            projectId: String,
            channelId: String,
            messageId: String,
            attachmentId: String
        ): CollectionPath = CollectionPath("${projectMessageAttachments(projectId, channelId, messageId).value}/$attachmentId")
        
        /* -------------------- Task Paths -------------------- */
        fun task(projectId: String, channelId: String, taskId: String): CollectionPath =
            CollectionPath("${projectChannel(projectId, channelId).value}/${Task.COLLECTION_NAME}/$taskId")
        
        /* -------------------- Schedule Paths -------------------- */
        fun schedule(scheduleId: String): CollectionPath = 
            CollectionPath("${Schedule.COLLECTION_NAME}/$scheduleId")
    }
}
