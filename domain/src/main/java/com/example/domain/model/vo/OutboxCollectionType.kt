package com.example.domain.model.vo

import com.example.domain.model.base.Category
import com.example.domain.model.base.DMChannel
import com.example.domain.model.base.DMWrapper
import com.example.domain.model.base.Friend
import com.example.domain.model.base.Member
import com.example.domain.model.base.Message
import com.example.domain.model.base.MessageAttachment
import com.example.domain.model.base.Permission
import com.example.domain.model.base.Project
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.model.base.Reaction
import com.example.domain.model.base.Role
import com.example.domain.model.base.Schedule
import com.example.domain.model.base.Task
import com.example.domain.model.base.User

/**
 * Outbox Collection Type Enum
 * 각 도메인 모델의 COLLECTION_NAME을 활용한 타입 안전한 컬렉션 정의
 *
 * 🎯 역할:
 * - 타입 안전한 컬렉션 이름 제공
 * - Firestore 경로 생성을 위한 CollectionPath 변환
 * - SQL 테이블명과 Firestore 경로 분리
 *
 * 📋 사용법:
 * - collectionName: 도메인 모델의 COLLECTION_NAME 사용
 * - toCollectionPath(): Firestore 경로 변환
 */
enum class OutboxCollectionType(val collectionName: String) {
    USERS(User.COLLECTION_NAME),
    DM_CHANNELS(DMChannel.COLLECTION_NAME),
    PROJECTS(Project.COLLECTION_NAME),
    SCHEDULES(Schedule.COLLECTION_NAME),
    FRIENDS(Friend.COLLECTION_NAME),
    DM_WRAPPERS(DMWrapper.COLLECTION_NAME),
    PROJECT_WRAPPERS(ProjectsWrapper.COLLECTION_NAME),
    PROJECT_MEMBERS(Member.COLLECTION_NAME),
    PROJECT_ROLES(Role.COLLECTION_NAME),
    PROJECT_PERMISSIONS(Permission.COLLECTION_NAME),
    PROJECT_INVITATIONS(ProjectInvitation.COLLECTION_NAME),
    PROJECT_CATEGORIES(Category.COLLECTION_NAME),
    PROJECT_CHANNELS(ProjectChannel.COLLECTION_NAME),
    MESSAGES(Message.COLLECTION_NAME),
    MESSAGE_ATTACHMENTS(MessageAttachment.COLLECTION_NAME),
    TASKS(Task.COLLECTION_NAME),
    REACTIONS(Reaction.COLLECTION_NAME);

    /**
     * CollectionPath로 변환
     * Firestore 저장시 실제 경로 생성용
     *
     * @param contextIds 경로 생성에 필요한 컨텍스트 ID들 (userId, projectId 등)
     * @return CollectionPath 인스턴스
     */
    fun toCollectionPath(vararg contextIds: String): CollectionPath {
        return when (this) {
            // Root Collections
            USERS -> CollectionPath.users
            DM_CHANNELS -> CollectionPath.dmChannels
            PROJECTS -> CollectionPath.projects
            SCHEDULES -> CollectionPath.schedules
            PROJECT_INVITATIONS -> CollectionPath.projectInvitations()

            // User Sub-collections (contextIds[0] = userId)
            FRIENDS -> {
                require(contextIds.isNotEmpty()) { "FRIENDS requires userId" }
                CollectionPath.userFriends(contextIds[0])
            }

            DM_WRAPPERS -> {
                require(contextIds.isNotEmpty()) { "DM_WRAPPERS requires userId" }
                CollectionPath.userDmWrappers(contextIds[0])
            }

            PROJECT_WRAPPERS -> {
                require(contextIds.isNotEmpty()) { "PROJECT_WRAPPERS requires userId" }
                CollectionPath.userProjectWrappers(contextIds[0])
            }

            // Project Sub-collections (contextIds[0] = projectId)
            PROJECT_MEMBERS -> {
                require(contextIds.isNotEmpty()) { "PROJECT_MEMBERS requires projectId" }
                CollectionPath.projectMembers(contextIds[0])
            }

            PROJECT_ROLES -> {
                require(contextIds.isNotEmpty()) { "PROJECT_ROLES requires projectId" }
                CollectionPath.projectRoles(contextIds[0])
            }

            PROJECT_CATEGORIES -> {
                require(contextIds.isNotEmpty()) { "PROJECT_CATEGORIES requires projectId" }
                CollectionPath.projectCategories(contextIds[0])
            }

            PROJECT_CHANNELS -> {
                require(contextIds.isNotEmpty()) { "PROJECT_CHANNELS requires projectId" }
                CollectionPath.projectChannels(contextIds[0])
            }

            // Role Sub-collections (contextIds[0] = projectId, contextIds[1] = roleId)
            PROJECT_PERMISSIONS -> {
                require(contextIds.size >= 2) { "PROJECT_PERMISSIONS requires projectId and roleId" }
                CollectionPath.projectRolePermissions(contextIds[0], contextIds[1])
            }

            // Channel Sub-collections (contextIds varies by context)
            MESSAGES -> {
                when (contextIds.size) {
                    1 -> CollectionPath.dmChannelMessages(contextIds[0]) // DM 메시지
                    2 -> CollectionPath.projectChannelMessages(
                        contextIds[0],
                        contextIds[1]
                    ) // 프로젝트 메시지
                    else -> throw IllegalArgumentException("MESSAGES requires 1 (dmChannelId) or 2 (projectId, channelId) parameters")
                }
            }

            MESSAGE_ATTACHMENTS -> {
                when (contextIds.size) {
                    2 -> CollectionPath.dmMessageAttachments(
                        contextIds[0],
                        contextIds[1]
                    ) // DM 첨부파일
                    3 -> CollectionPath.projectMessageAttachments(
                        contextIds[0],
                        contextIds[1],
                        contextIds[2]
                    ) // 프로젝트 첨부파일
                    else -> throw IllegalArgumentException("MESSAGE_ATTACHMENTS requires 2 (dmChannelId, messageId) or 3 (projectId, channelId, messageId) parameters")
                }
            }

            TASKS -> {
                require(contextIds.size >= 2) { "TASKS requires projectId and channelId" }
                CollectionPath.tasks(contextIds[0], contextIds[1])
            }

            // 기본값: 컬렉션 이름을 그대로 사용 (단순 루트 컬렉션)
            REACTIONS -> CollectionPath(collectionName)
        }
    }

    companion object {
        /**
         * 컬렉션 이름으로 enum 찾기
         * @param name 컬렉션 이름
         * @return 해당하는 enum 값
         */
        fun fromCollectionName(name: String): OutboxCollectionType? {
            return values().find { it.collectionName == name }
        }

        /**
         * 도메인 모델 타입으로 enum 찾기
         * @param domainClass 도메인 모델 클래스
         * @return 해당하는 enum 값
         */
        inline fun <reified T> fromDomainType(): OutboxCollectionType? {
            return when (T::class) {
                User::class -> USERS
                DMChannel::class -> DM_CHANNELS
                Project::class -> PROJECTS
                Schedule::class -> SCHEDULES
                Friend::class -> FRIENDS
                DMWrapper::class -> DM_WRAPPERS
                ProjectsWrapper::class -> PROJECT_WRAPPERS
                Member::class -> PROJECT_MEMBERS
                Role::class -> PROJECT_ROLES
                Permission::class -> PROJECT_PERMISSIONS
                ProjectInvitation::class -> PROJECT_INVITATIONS
                Category::class -> PROJECT_CATEGORIES
                ProjectChannel::class -> PROJECT_CHANNELS
                Message::class -> MESSAGES
                MessageAttachment::class -> MESSAGE_ATTACHMENTS
                Task::class -> TASKS
                Reaction::class -> REACTIONS
                else -> null
            }
        }
    }
}