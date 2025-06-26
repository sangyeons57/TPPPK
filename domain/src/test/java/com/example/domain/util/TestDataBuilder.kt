package com.example.domain.util

import com.example.domain.model.base.Friend
import com.example.domain.model.base.Project
import com.example.domain.model.base.Schedule
import com.example.domain.model.base.User
import com.example.domain.model.data.UserSession
import com.example.domain.model.enum.FriendStatus
import com.example.domain.model.enum.ScheduleStatus
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.ProjectId
import com.example.domain.model.vo.Token
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.model.vo.schedule.ScheduleContent
import com.example.domain.model.vo.schedule.ScheduleTitle
import com.example.domain.model.vo.user.UserEmail
import com.example.domain.model.vo.user.UserName
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Builder utility for creating test data consistently across all domain tests.
 * Provides default values that can be overridden as needed for specific test scenarios.
 */
object TestDataBuilder {

    /**
     * Creates a test User with sensible defaults
     */
    fun createTestUser(
        id: String = "test_user_id",
        email: String = "test@example.com",
        name: String = "Test User",
        accountStatus: UserAccountStatus = UserAccountStatus.ACTIVE,
        profileImageUrl: String? = null
    ): User {
        return User.create(
            id = DocumentId(id),
            email = UserEmail(email),
            name = UserName(name),
            consentTimeStamp = Instant.now()
        ).let { user ->
            if (accountStatus != UserAccountStatus.ACTIVE) {
                when (accountStatus) {
                    UserAccountStatus.WITHDRAWN -> user.apply { markAsWithdrawn() }
                    UserAccountStatus.SUSPENDED -> user.apply { suspendAccount() }
                    else -> user
                }
            } else {
                user
            }
        }.let { user ->
            profileImageUrl?.let { url ->
                user.copy(profileImageUrl = ImageUrl.toImageUrl(url))
            } ?: user
        }
    }

    /**
     * Creates a test UserSession
     */
    fun createTestUserSession(
        userId: String = "test_user_id",
        token: String = "test_token",
        email: String = "test@example.com"
    ): UserSession {
        return UserSession(
            userId = UserId(userId),
            token = Token(token),
            email = UserEmail(email)
        )
    }

    /**
     * Creates a test Project
     */
    fun createTestProject(
        id: String = "test_project_id",
        name: String = "Test Project",
        ownerId: String = "test_owner_id",
        imageUrl: String? = null
    ): Project {
        val now = Instant.now()
        return Project.fromDataSource(
            id = DocumentId(id),
            name = ProjectName(name),
            imageUrl = imageUrl?.let { ImageUrl.toImageUrl(it) },
            ownerId = OwnerId(ownerId),
            createdAt = now,
            updatedAt = now
        )
    }

    /**
     * Creates a test Schedule
     */
    fun createTestSchedule(
        id: String = "test_schedule_id",
        creatorId: String = "creator_id",
        title: String = "Test Schedule",
        content: String = "Test schedule content",
        startDateTime: LocalDateTime = LocalDateTime.now().plusDays(1),
        durationHours: Long = 1,
        projectId: String? = null
    ): Schedule {
        val start = startDateTime.atZone(ZoneId.systemDefault()).toInstant()
        val end = start.plusSeconds(durationHours * 3600)
        return Schedule.create(
            projectId = projectId?.let { ProjectId(it) },
            creatorId = OwnerId(creatorId),
            title = ScheduleTitle(title),
            content = ScheduleContent(content),
            startTime = start,
            endTime = end,
            status = ScheduleStatus.CONFIRMED
        )
    }

    /**
     * Creates a test Friend relationship
     */
    fun createTestFriend(
        id: String = "test_friend_id",
        requesterId: String = "test_requester_id",
        recipientId: String = "test_recipient_id",
        status: FriendStatus = FriendStatus.PENDING
    ): Friend {
        return Friend.createPendingRequest(
            id = DocumentId(id),
            requesterId = UserId(requesterId),
            recipientId = UserId(recipientId)
        ).let { friend ->
            when (status) {
                FriendStatus.ACCEPTED -> friend.copy(status = FriendStatus.ACCEPTED)
                FriendStatus.DECLINED -> friend.copy(status = FriendStatus.DECLINED)
                else -> friend
            }
        }
    }

    /**
     * Creates multiple test users for batch testing
     */
    fun createTestUsers(count: Int): List<User> {
        return (1..count).map { index ->
            createTestUser(
                id = "test_user_$index",
                email = "user$index@example.com",
                name = "Test User $index"
            )
        }
    }

    /**
     * Creates multiple test projects for batch testing
     */
    fun createTestProjects(count: Int, ownerId: String = "test_owner_id"): List<Project> {
        return (1..count).map { index ->
            createTestProject(
                id = "test_project_$index",
                name = "Test Project $index",
                ownerId = ownerId,
                imageUrl = "https://example.com/image_$index.jpg"
            )
        }
    }

    /**
     * Creates multiple test schedules for batch testing
     */
    fun createTestSchedules(count: Int, authorId: String = "test_author_id"): List<Schedule> {
        return (1..count).map { index ->
            createTestSchedule(
                id = "test_schedule_$index",
                authorId = authorId,
                title = "Test Schedule $index",
                content = "Test schedule $index content",
                scheduleDateTime = LocalDateTime.now().plusDays(index.toLong())
            )
        }
    }
}

/**
 * Extension functions for easy assertions in tests
 */
fun User.shouldBeActive() = assert(this.accountStatus == UserAccountStatus.ACTIVE) {
    "Expected user to be ACTIVE but was ${this.accountStatus}"
}

fun User.shouldBeWithdrawn() = assert(this.accountStatus == UserAccountStatus.WITHDRAWN) {
    "Expected user to be WITHDRAWN but was ${this.accountStatus}"
}

fun User.shouldBeSuspended() = assert(this.accountStatus == UserAccountStatus.SUSPENDED) {
    "Expected user to be SUSPENDED but was ${this.accountStatus}"
}

fun Friend.shouldBePending() = assert(this.status == FriendStatus.PENDING) {
    "Expected friendship to be PENDING but was ${this.status}"
}

fun Friend.shouldBeAccepted() = assert(this.status == FriendStatus.ACCEPTED) {
    "Expected friendship to be ACCEPTED but was ${this.status}"
}