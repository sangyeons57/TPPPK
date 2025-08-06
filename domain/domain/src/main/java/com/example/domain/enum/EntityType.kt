package com.example.domain.enum

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
import com.example.domain.model.base.Reaction
import com.example.domain.model.base.Role
import com.example.domain.model.base.Schedule
import com.example.domain.model.base.Task
import com.example.domain.model.base.User

/**
 * 모든 Domain Model Entity 타입을 정의하는 enum
 * Domain Model의 COLLECTION_NAME 상수를 사용하여 Clean Architecture 원칙 준수
 */
enum class EntityType(val collectionName: String) {
    // Core Communication
    MESSAGE(Message.COLLECTION_NAME),
    REACTION(Reaction.COLLECTION_NAME),

    // User Management
    USER(User.COLLECTION_NAME),
    FRIEND(Friend.COLLECTION_NAME),

    // Project Management
    PROJECT(Project.COLLECTION_NAME),
    PROJECT_CHANNEL(ProjectChannel.COLLECTION_NAME),
    PROJECT_INVITATION(ProjectInvitation.COLLECTION_NAME),
    PROJECTS_WRAPPER(ProjectsWrapper.COLLECTION_NAME),

    // Organization Structure
    CATEGORY(Category.COLLECTION_NAME),
    MEMBER(Member.COLLECTION_NAME),
    ROLE(Role.COLLECTION_NAME),
    PERMISSION(Permission.COLLECTION_NAME),

    // Direct Message
    DM_CHANNEL(DMChannel.COLLECTION_NAME),
    DM_WRAPPER(DMWrapper.COLLECTION_NAME),

    // Task & Schedule Management  
    TASK(Task.COLLECTION_NAME),
    SCHEDULE(Schedule.COLLECTION_NAME);

    companion object {
        /**
         * Collection name으로 EntityType을 찾는 헬퍼 함수
         */
        fun fromCollectionName(collectionName: String): EntityType? {
            return entries.find { it.collectionName == collectionName }
        }

        /**
         * Domain Model Class로 EntityType을 찾는 헬퍼 함수
         */
        inline fun <reified T> fromDomainClass(): EntityType? {
            return when (T::class) {
                Message::class -> MESSAGE
                Reaction::class -> REACTION
                User::class -> USER
                Friend::class -> FRIEND
                Project::class -> PROJECT
                ProjectChannel::class -> PROJECT_CHANNEL
                ProjectInvitation::class -> PROJECT_INVITATION
                ProjectsWrapper::class -> PROJECTS_WRAPPER
                Category::class -> CATEGORY
                Member::class -> MEMBER
                Role::class -> ROLE
                Permission::class -> PERMISSION
                DMChannel::class -> DM_CHANNEL
                DMWrapper::class -> DM_WRAPPER
                Task::class -> TASK
                Schedule::class -> SCHEDULE
                else -> null
            }
        }

        /**
         * 모든 EntityType 목록
         */
        fun getAllTypes(): List<EntityType> = entries.toList()

        /**
         * Entity 존재 여부 확인 (Message, Metadata, OutBox만 Entity 있음)
         */
        fun hasEntity(entityType: EntityType): Boolean {
            return when (entityType) {
                MESSAGE -> true  // MessageEntity 있음
                // 추후 다른 Entity들 추가 시 여기에 추가
                else -> false
            }
        }
    }
}