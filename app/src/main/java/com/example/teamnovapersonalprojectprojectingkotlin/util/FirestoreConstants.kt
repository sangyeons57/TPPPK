package com.example.teamnovapersonalprojectprojectingkotlin.util // 또는 data.constants 등 적절한 위치

/**
 * Firestore 컬렉션, 필드, 값 등의 이름을 상수로 관리하는 객체
 */
object FirestoreConstants {

    /** 최상위 컬렉션 이름 */
    object Collections {
        const val USERS = "users"
        const val FRIEND_REQUESTS = "friendRequests"
        const val PROJECTS = "projects" // 추정
        const val SCHEDULES = "schedules" // 추정 (개인/프로젝트 일정 통합 또는 분리 가능)
        const val DM_CHANNELS = "dmChannels" // 추정 (또는 다른 구조일 수 있음)
        // 다른 최상위 컬렉션 필요 시 추가...
    }

    object StoragePaths {
        const val PROFILE_IMAGES = "profileImages" // 프로필 이미지 저장 경로
    }

    /** 'users' 컬렉션 관련 상수 */
    object Users {
        const val NAME = Collections.USERS
        // 필드 이름
        object Fields {
            const val USER_ID = "userId"
            const val NAME = "name"
            const val EMAIL = "email"
            const val PROFILE_IMAGE_URL = "profileImageUrl"
            const val STATUS = "status"
            const val STATUS_MESSAGE = "statusMessage"
            // User 모델에 추가 필드 정의 시 여기에 상수 추가...
        }

        object StatusValues{
            const val ONLINE = "online"
            const val OFFLINE = "offline"
        }

        // 'friends' 서브컬렉션 관련 상수
        object FriendsSubcollection {
            const val NAME = "friends" // 서브컬렉션 이름

            // 서브컬렉션 문서 내 필드
            object Fields {
                const val ADDED_AT = "addedAt"
            }
        }
        // 다른 서브컬렉션 필요 시 추가... (예: userSchedules 등)
    }

    /** 'friendRequests' 컬렉션 관련 상수 */
    object FriendRequests {
        const val NAME = Collections.FRIEND_REQUESTS
        // 필드 이름
        object Fields {
            const val SENDER_UID = "senderUid"
            const val RECEIVER_UID = "receiverUid"
            const val STATUS = "status"
            const val CREATED_AT = "createdAt"
        }

        // 'status' 필드의 값
        object StatusValues {
            const val PENDING = "pending"
            const val ACCEPTED = "accepted"
            // const val DENIED = "denied" // 필요 시 추가
        }
    }

    /** 'projects' 컬렉션 관련 상수 (추정) */
    object Projects {
        const val NAME = Collections.PROJECTS
        // 필드 이름 (Project 모델 기반 추정)
        object Fields {
            const val ID = "id" // Document ID와 별개로 필드 저장 시
            const val NAME = "name"
            const val DESCRIPTION = "description"
            const val IMAGE_URL = "imageUrl"
            const val MEMBER_COUNT = "memberCount"
            const val IS_PUBLIC = "isPublic"
            const val OWNER_ID = "ownerId"
            // 필요 시 추가... (예: createdAt)
        }

        // 'members' 서브컬렉션 관련 상수 (추정)
        object MembersSubcollection {
            const val NAME = "members"

            // 필드 이름 (ProjectMember 모델 기반 추정)
            object Fields {
                const val USER_ID = "userId" // 문서 ID와 중복될 수 있지만 명시적으로
                const val ROLE_IDS = "roleIds" // 역할 ID 리스트
                // 필요 시 추가... (예: joinedAt)
            }
        }

        // 'roles' 서브컬렉션 관련 상수 (추정)
        object RolesSubcollection {
            const val NAME = "roles"

            // 필드 이름 (Role 모델 기반 추정)
            object Fields {
                const val ROLE_NAME = "name" // 역할 이름
                const val PERMISSIONS = "permissions" // 권한 맵 (Map<String, Boolean> 형태 예상)
                // 필요 시 추가... (예: color, order)
            }
        }

        // 'categories' 서브컬렉션 관련 상수 (추정)
        object CategoriesSubcollection {
            const val NAME = "categories"

            // 필드 이름 (Category 모델 기반 추정)
            object Fields {
                const val CATEGORY_NAME = "name" // 카테고리 이름
                const val ORDER = "order"
                // 필요 시 추가...
            }
        }

        // 'channels' 서브컬렉션 관련 상수 (추정) - 카테고리 하위가 아니라 프로젝트 하위로 둘 수도 있음
        object ChannelsSubcollection {
            const val NAME = "channels"

            // 필드 이름 (Channel 모델 기반 추정)
            object Fields {
                const val CATEGORY_ID = "categoryId" // 어떤 카테고리 소속인지
                const val CHANNEL_NAME = "name"
                const val TYPE = "type" // "TEXT" 또는 "VOICE" 문자열 저장 예상
                const val ORDER = "order"
                // 필요 시 추가...
            }

            object TypeValues { // 채널 타입 값
                const val TEXT = "TEXT"
                const val VOICE = "VOICE"
            }
        }

        // 'schedules' 서브컬렉션 관련 상수 (추정)
        object SchedulesSubcollection {
            const val NAME = "schedules"
            // 필드 이름은 아래 Schedules 객체 필드와 동일할 것으로 예상
        }
        // 다른 서브컬렉션 필요 시 추가...
    }

    /** 'schedules' 컬렉션 관련 상수 (추정) - 개인 일정 포함 가능성 */
    object Schedules {
        const val NAME = Collections.SCHEDULES
        // 필드 이름 (Schedule 모델 기반 추정)
        object Fields {
            const val ID = "id" // 문서 ID와 별개로 저장 시
            const val PROJECT_ID = "projectId" // 어떤 프로젝트 소속인지 (null이면 개인 일정)
            const val TITLE = "title"
            const val CONTENT = "content"
            const val START_TIME = "startTime" // Timestamp
            const val END_TIME = "endTime" // Timestamp
            const val ATTENDEES = "attendees" // List<String> (userId 목록)
            const val IS_ALL_DAY = "isAllDay" // Boolean
            // 필요 시 추가... (예: createdAt, createdBy)
        }
    }

    /** 채팅 메시지 관련 상수 (컬렉션 구조는 유동적) */
    object ChatMessages {
        // 예시: 채널별 서브컬렉션 사용 시
        const val SUBCOLLECTION_NAME = "messages"

        // 필드 이름 (ChatMessage 모델 기반 추정)
        object Fields {
            const val CHAT_ID = "chatId" // 서버 ID (auto-increment?)
            const val CHANNEL_ID = "channelId" // 어떤 채널/DM 소속인지
            const val USER_ID = "userId"
            const val USER_NAME = "userName"
            const val USER_PROFILE_URL = "userProfileUrl"
            const val MESSAGE = "message"
            const val SENT_AT = "sentAt" // Timestamp
            const val IS_MODIFIED = "isModified" // Boolean
            const val ATTACHMENT_IMAGE_URLS = "attachmentImageUrls" // List<String>
            // 필요 시 추가... (예: readBy)
        }
    }

    // --- 다른 컬렉션/필요한 상수들 추가 ---

}