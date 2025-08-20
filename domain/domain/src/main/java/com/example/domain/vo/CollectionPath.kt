package com.example.domain.vo

import com.example.domain.model.base.Category
import com.example.domain.model.base.DMChannel
import com.example.domain.model.base.DMWrapper
import com.example.domain.model.base.Friend
import com.example.domain.model.base.Member
import com.example.domain.model.base.Message
import com.example.domain.model.base.Permission
import com.example.domain.model.base.Project
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.model.base.Role
import com.example.domain.model.base.Schedule
import com.example.domain.model.base.Task
import com.example.domain.model.base.User

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

        /**
         * Overload: Build messages path from ChannelId (supports composite "projectId:channelId").
         */
        fun messages(channelId: ChannelId): CollectionPath =
            if (channelId.hasDelimiter()) {
                val projectId = channelId.firstOrNull()
                    ?: throw IllegalArgumentException("Invalid composite channelId: ${channelId.value}")
                val leaf = channelId.last()
                projectChannelMessages(projectId, leaf)
            } else {
                dmChannelMessages(channelId.value)
            }
        fun projectChannelMessage(projectId: String, channelId: String, messageId: String): CollectionPath =
            CollectionPath("${projectChannelMessages(projectId, channelId).value}/$messageId")

        /* -------------------- Storage Paths (Firebase Storage) -------------------- */
        /**
         * 채널 메시지 첨부 파일의 저장 경로 (디렉터리)
         * 예: channels/{channelId}/messages/{messageId}/attachments
         */
        fun storageMessageAttachments(channelId: String, messageId: String): CollectionPath =
            CollectionPath("channels/$channelId/messages/$messageId/attachments")

        /**
         * 채널 메시지 첨부 파일의 개별 파일 경로
         * 예: channels/{channelId}/messages/{messageId}/attachments/{fileName}
         */
        fun storageMessageAttachment(
            channelId: String,
            messageId: String,
            fileName: String
        ): CollectionPath =
            CollectionPath("${storageMessageAttachments(channelId, messageId).value}/$fileName")
        
        /* -------------------- Task Paths -------------------- */
        fun tasks(projectId: String, projectChannelId: String): CollectionPath =
            CollectionPath(
                "${
                    projectChannel(
                        projectId,
                        projectChannelId
                    ).value
                }/${Task.COLLECTION_NAME}"
            )

        /**
         * Overload: Build task path from composite ChannelId (project channels only).
         */
        fun tasks(channelId: ChannelId): CollectionPath =
            if (channelId.hasDelimiter()) {
                val projectId = channelId.firstOrNull()
                    ?: throw IllegalArgumentException("Invalid composite channelId: ${channelId.value}")
                val leaf = channelId.last()
                tasks(projectId, leaf)
            } else {
                throw IllegalArgumentException("Tasks require project channel; got DM channelId=${channelId.value}")
            }

        fun task(projectId: String, projectChannelId: String, taskId: String): CollectionPath =
            CollectionPath("${tasks(projectId, projectChannelId).value}/$taskId")
        
        /* -------------------- Schedule Paths -------------------- */
        fun schedule(scheduleId: String): CollectionPath = 
            CollectionPath("${Schedule.COLLECTION_NAME}/$scheduleId")
    }
}
