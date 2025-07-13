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
        
        /* -------------------- Unified Task Container Paths -------------------- */
        /**
         * 통합된 task_container collection 경로
         * 하나의 collection에 TaskContainer 정의 문서와 Task 문서들이 함께 저장됩니다.
         */
        fun projectChannelUnifiedTaskContainer(projectId: String, channelId: String): CollectionPath =
            CollectionPath("${projectChannel(projectId, channelId).value}/task_container")
        
        /**
         * 중첩된 TaskContainer collection 경로들
         * TaskContainer는 자기 자신의 subcollection이 될 수 있습니다.
         */
        
        /**
         * 1단계 중첩: project/channel/task_container/{container_id}/task_container
         */
        fun nestedTaskContainer1(projectId: String, channelId: String, containerId: String): CollectionPath =
            CollectionPath("${projectChannelUnifiedTaskContainer(projectId, channelId).value}/$containerId/task_container")
        
        /**
         * 2단계 중첩: project/channel/task_container/{id1}/task_container/{id2}/task_container
         */
        fun nestedTaskContainer2(
            projectId: String, 
            channelId: String, 
            containerId1: String, 
            containerId2: String
        ): CollectionPath =
            CollectionPath("${nestedTaskContainer1(projectId, channelId, containerId1).value}/$containerId2/task_container")
        
        /**
         * 3단계 중첩: project/channel/task_container/{id1}/task_container/{id2}/task_container/{id3}/task_container
         */
        fun nestedTaskContainer3(
            projectId: String, 
            channelId: String, 
            containerId1: String, 
            containerId2: String,
            containerId3: String
        ): CollectionPath =
            CollectionPath("${nestedTaskContainer2(projectId, channelId, containerId1, containerId2).value}/$containerId3/task_container")
        
        /**
         * 일반적인 중첩 TaskContainer 경로 생성 함수
         * @param projectId 프로젝트 ID
         * @param channelId 채널 ID
         * @param containerPath 중첩된 container ID들의 배열 (예: ["container1", "container2", "container3"])
         * @return 중첩된 TaskContainer collection 경로
         */
        fun nestedTaskContainer(
            projectId: String, 
            channelId: String, 
            containerPath: List<String>
        ): CollectionPath {
            if (containerPath.isEmpty()) {
                return projectChannelUnifiedTaskContainer(projectId, channelId)
            }
            
            var path = projectChannelUnifiedTaskContainer(projectId, channelId).value
            containerPath.forEach { containerId ->
                path += "/$containerId/task_container"
            }
            return CollectionPath(path)
        }
        
        /* -------------------- Legacy Task Container Paths (Deprecated) -------------------- */
        @Deprecated("Use projectChannelUnifiedTaskContainer instead")
        fun projectChannelTaskContainers(projectId: String, channelId: String): CollectionPath =
            CollectionPath("${projectChannel(projectId, channelId).value}/${TaskContainer.COLLECTION_NAME}")
        @Deprecated("Use projectChannelUnifiedTaskContainer instead")
        fun projectChannelTaskContainer(projectId: String, channelId: String, containerId: String): CollectionPath =
            CollectionPath("${projectChannelTaskContainers(projectId, channelId).value}/$containerId")
        
        /* -------------------- Legacy Task Paths (Deprecated) -------------------- */
        @Deprecated("Use projectChannelUnifiedTaskContainer instead")
        fun taskContainerTasks(projectId: String, channelId: String, containerId: String): CollectionPath =
            CollectionPath("${projectChannelTaskContainer(projectId, channelId, containerId).value}/${Task.COLLECTION_NAME}")
        @Deprecated("Use projectChannelUnifiedTaskContainer instead")
        fun taskContainerTask(projectId: String, channelId: String, containerId: String, taskId: String): CollectionPath =
            CollectionPath("${taskContainerTasks(projectId, channelId, containerId).value}/$taskId")
        
        /* -------------------- Schedule Paths -------------------- */
        fun schedule(scheduleId: String): CollectionPath = 
            CollectionPath("${Schedule.COLLECTION_NAME}/$scheduleId")
    }
}
