package com.example.core_navigation.destination

import androidx.navigation.NavType
import androidx.navigation.navArgument

object AppRoutes {

    object Auth {
        private const val ROOT = "auth"
        object Graph { // For the nested navigation graph
            const val path = ROOT_GRAPH
            const val suggestedStartPath = Splash.path
        }
        private const val ROOT_GRAPH = "auth_graph"


        object Splash {
            const val path = "$ROOT/splash"
        }
        object Login {
            const val path = "$ROOT/login"
        }
        object SignUp {
            const val path = "$ROOT/signup"
        }
        object FindPassword {
            const val path = "$ROOT/find_password"
        }
        object TermsOfService {
            const val path = "$ROOT/terms_of_service"
        }
        object PrivacyPolicy {
            const val path = "$ROOT/privacy_policy"
        }
    }

    object Main {
        const val ROOT = "main_host" // Path for MainScreen which hosts bottom nav and its NavHost

        object Home {
            const val GRAPH_ROOT = "main_home_graph" // Navigating to this graph shows HomeContent
            const val ROOT_CONTENT = "main_home_content" // Actual content screen for home
        }
        object Calendar {
            const val GRAPH_ROOT = "main_calendar_graph"
            const val ROOT_CONTENT = "main_calendar_content"
            // ARG_SCHEDULE_ID is used by EditSchedule, ScheduleDetail
            const val ARG_SCHEDULE_ID = "scheduleId"
            // ARG_YEAR, ARG_MONTH, ARG_DAY are used by Calendar24Hour, AddSchedule
            const val ARG_YEAR = "year"
            const val ARG_MONTH = "month"
            const val ARG_DAY = "day"

            fun addSchedule(year: Int, month: Int, day: Int) = "schedule/add/$year/$month/$day"
            fun addScheduleRoute() = "schedule/add/{$ARG_YEAR}/{$ARG_MONTH}/{$ARG_DAY}"
            val addScheduleArguments = listOf(
                navArgument(ARG_YEAR) { type = NavType.Companion.IntType },
                navArgument(ARG_MONTH) { type = NavType.Companion.IntType },
                navArgument(ARG_DAY) { type = NavType.Companion.IntType }
            )

            fun scheduleDetail(scheduleId: String) = "schedule/detail/$scheduleId"
            fun scheduleDetailRoute() = "schedule/detail/{$ARG_SCHEDULE_ID}"
            val scheduleDetailArguments = listOf(navArgument(ARG_SCHEDULE_ID) {
                type = NavType.Companion.StringType
            })

            fun editSchedule(scheduleId: String) = "schedule/edit/$scheduleId"
            fun editScheduleRoute() = "schedule/edit/{$ARG_SCHEDULE_ID}"
            val editScheduleArguments = listOf(navArgument(ARG_SCHEDULE_ID) {
                type = NavType.Companion.StringType
            })

            fun calendar24Hour(year: Int, month: Int, day: Int) = "schedule/24hour/$year/$month/$day"
            fun calendar24HourRoute() = "schedule/24hour/{$ARG_YEAR}/{$ARG_MONTH}/{$ARG_DAY}"
            val calendar24HourArguments = listOf(
                navArgument(ARG_YEAR) { type = NavType.Companion.IntType },
                navArgument(ARG_MONTH) { type = NavType.Companion.IntType },
                navArgument(ARG_DAY) { type = NavType.Companion.IntType }
            )

            // 캘린더 그래프 정의
            object Graph {
                const val path = "calendar_graph"
            }
        }
        object Profile {
            const val GRAPH_ROOT = "main_profile_graph"
            const val ROOT_CONTENT = "main_profile_content"
        }
        object Graph { // For the nested navigation graph for MainScreen itself
            const val path = ROOT
        }
    }

    object Project {
        private const val ROOT = "project"
        const val ARG_PROJECT_ID = "projectId"
        const val ARG_CATEGORY_ID = "categoryId"
        const val ARG_CHANNEL_ID = "channelId"
        const val ARG_USER_ID = "userId" // For member editing
        const val ARG_ROLE_ID = "roleId" // For role editing (optional query param)

        // 프로젝트 그래프 정의
        object Graph {
            const val path = "project_graph"
            const val suggestedStartPath = ADD
        }
        
        // Project Creation / Joining
        const val ADD = "$ROOT/add"
        const val JOIN = "$ROOT/join"
        const val SET_NAME = "$ROOT/set_name" // After ADD
        const val SELECT_TYPE = "$ROOT/select_type" // 프로젝트 타입 선택 화면

        // Project Detail
        fun detail(projectId: String) = "$ROOT/$projectId"
        fun detailRoute() = "$ROOT/{$ARG_PROJECT_ID}"
        val detailArguments = listOf(navArgument(ARG_PROJECT_ID) {
            type = NavType.Companion.StringType
        })

        // Project Settings (Root)
        fun settings(projectId: String) = "$ROOT/$projectId/settings"
        fun settingsRoute() = "$ROOT/{$ARG_PROJECT_ID}/settings"
        val settingsArguments = listOf(navArgument(ARG_PROJECT_ID) {
            type = NavType.Companion.StringType
        })

        // Project Structure: Categories & Channels
        fun createCategory(projectId: String) = "$ROOT/$projectId/category/create"
        fun createCategoryRoute() = "$ROOT/{$ARG_PROJECT_ID}/category/create"
        val createCategoryArguments = listOf(navArgument(ARG_PROJECT_ID) {
            type = NavType.Companion.StringType
        })

        fun editCategory(projectId: String, categoryId: String) = "$ROOT/$projectId/category/edit/$categoryId"
        fun editCategoryRoute() = "$ROOT/{$ARG_PROJECT_ID}/category/edit/{$ARG_CATEGORY_ID}"
        val editCategoryArguments = listOf(
            navArgument(ARG_PROJECT_ID) { type = NavType.Companion.StringType },
            navArgument(ARG_CATEGORY_ID) { type = NavType.Companion.StringType }
        )

        fun createChannel(projectId: String, categoryId: String) = "$ROOT/$projectId/category/$categoryId/channel/create"
        fun createChannelRoute() = "$ROOT/{$ARG_PROJECT_ID}/category/{$ARG_CATEGORY_ID}/channel/create"
        val createChannelArguments = listOf(
            navArgument(ARG_PROJECT_ID) { type = NavType.Companion.StringType },
            navArgument(ARG_CATEGORY_ID) { type = NavType.Companion.StringType }
        )

        fun editChannel(projectId: String, categoryId: String, channelId: String) = "$ROOT/$projectId/category/$categoryId/channel/edit/$channelId"
        fun editChannelRoute() = "$ROOT/{$ARG_PROJECT_ID}/category/{$ARG_CATEGORY_ID}/channel/edit/{$ARG_CHANNEL_ID}"
        val editChannelArguments = listOf(
            navArgument(ARG_PROJECT_ID) { type = NavType.Companion.StringType },
            navArgument(ARG_CATEGORY_ID) { type = NavType.Companion.StringType },
            navArgument(ARG_CHANNEL_ID) { type = NavType.Companion.StringType }
        )

        // Project Members
        fun memberList(projectId: String) = "$ROOT/$projectId/members"
        fun memberListRoute() = "$ROOT/{$ARG_PROJECT_ID}/members"
        val memberListArguments = listOf(navArgument(ARG_PROJECT_ID) {
            type = NavType.Companion.StringType
        })

        fun editMember(projectId: String, userId: String) = "$ROOT/$projectId/members/edit/$userId"
        fun editMemberRoute() = "$ROOT/{$ARG_PROJECT_ID}/members/edit/{$ARG_USER_ID}"
        val editMemberArguments = listOf(
            navArgument(ARG_PROJECT_ID) { type = NavType.Companion.StringType },
            navArgument(ARG_USER_ID) { type = NavType.Companion.StringType }
        )

        // Project Roles
        fun roleList(projectId: String) = "$ROOT/$projectId/roles"
        fun roleListRoute() = "$ROOT/{$ARG_PROJECT_ID}/roles"
        val roleListArguments = listOf(navArgument(ARG_PROJECT_ID) {
            type = NavType.Companion.StringType
        })

        // EditRole might take roleId as an optional query parameter or part of the path
        // For simplicity, keeping it similar to EditMember for now if it's a detail screen
        // If roleId is optional (for "add role" vs "edit role" on the same screen):
        // fun editRole(projectId: String, roleId: String? = null): String {
        //     return if (roleId != null) "$ROOT/$projectId/roles/edit/$roleId"
        //            else "$ROOT/$projectId/roles/add"
        // }
        // fun editRoleRoute() = "$ROOT/{$ARG_PROJECT_ID}/roles/edit/{$ARG_ROLE_ID}" // Path for specific role
        // fun addRoleRoute() = "$ROOT/{$ARG_PROJECT_ID}/roles/add" // Path for adding new role
        // For now, assuming one route for edit, and add might be implicit or a separate simple route if needed.
        fun editRole(projectId: String, roleId: String) = "$ROOT/$projectId/roles/edit/$roleId" // If roleId is mandatory for edit screen
        fun editRoleRoute() = "$ROOT/{$ARG_PROJECT_ID}/roles/edit/{$ARG_ROLE_ID}"
         val editRoleArguments = listOf(
             navArgument(ARG_PROJECT_ID) { type = NavType.Companion.StringType },
             navArgument(ARG_ROLE_ID) {
                 type = NavType.Companion.StringType
             } // Assuming roleId is part of path for edit
        )
        // If "add role" is a separate screen or uses this screen without roleId:
        fun addRole(projectId: String) = "$ROOT/$projectId/roles/add"
        fun addRoleRoute() = "$ROOT/{$ARG_PROJECT_ID}/roles/add"
        // addRoleArguments would just be projectId if roleId is not applicable for add.
    }

    object User { // For user-specific screens not tied to a project directly or main tabs
        private const val ROOT = "user"
        const val ARG_USER_ID = "userId"

        fun profile(userId: String) = "$ROOT/$userId/profile"
        fun profileRoute() = "$ROOT/{$ARG_USER_ID}/profile"
        val profileArguments = listOf(navArgument(ARG_USER_ID) {
            type = NavType.Companion.StringType
        })
    }

    object Settings { // App-level settings
        private const val ROOT = "settings"
        const val APP_SETTINGS = "$ROOT/application" // General app settings

        // User profile settings (editing own profile) - distinct from viewing another user's profile
        const val EDIT_MY_PROFILE = "$ROOT/profile/edit"
        const val CHANGE_MY_PASSWORD = "$ROOT/password/change"
    }

    object Friends {
        const val ROOT = "friends"

        const val LIST = "$ROOT/list"
        const val ACCEPT_REQUESTS = "$ROOT/accept"
        // Add friend might be a dialog or a simple screen, not requiring complex routing here initially
    }

    object Chat {
        const val ROOT = "chat"
        const val ARG_CHANNEL_ID = "channelId"
        const val ARG_MESSAGE_ID = "messageId" // Optional for scrolling to a message
        // const val ARG_PROJECT_ID = "projectId" // REMOVED: Not needed for unified chat route

        // 채팅 그래프 정의
        object Graph {
            const val path = "chat_graph"
        }
        
        // 통합 채널 라우트: chat/{channelId}?messageId={messageId}
        /**
         * 지정된 채널 ID와 선택적 메시지 ID를 사용하여 채팅 화면으로 이동하는 경로를 생성합니다.
         * 예: "chat/someChannelId" 또는 "chat/someChannelId?messageId=someMessageId"
         *
         * @param channelId 대상 채널의 ID.
         * @param messageId (선택 사항) 특정 메시지로 스크롤하기 위한 메시지 ID.
         * @return 구성된 네비게이션 경로 문자열.
         */
        fun screen(channelId: String, messageId: String? = null): String {
            var route = "$ROOT/$channelId"
            messageId?.let { route += "?$ARG_MESSAGE_ID=$it" }
            return route
        }

        /**
         * 채팅 화면의 기본 라우트 패턴입니다.
         * 예: "chat/{channelId}"
         * messageId는 선택적 쿼리 파라미터로 처리됩니다.
         */
        val route = "$ROOT/{$ARG_CHANNEL_ID}"

        /**
         * 채팅 화면 라우트에 필요한 네비게이션 인자 목록입니다.
         * channelId는 필수 경로 파라미터입니다.
         */
        val arguments = listOf(
            navArgument(ARG_CHANNEL_ID) { type = NavType.StringType }
            // messageId는 선택적 쿼리 파라미터이므로, NavHostController.navigate 호출 시 URL에 직접 추가됩니다.
            // NavArgument로 정의할 필요는 일반적으로 없습니다 (딥 링크 처리 시에는 고려할 수 있음).
        )

        // REMOVED: dm function
        // REMOVED: dmRoute
        // REMOVED: dmArguments

        // REMOVED: projectChannel function
        // REMOVED: projectChannelRoute
        // REMOVED: projectChannelArguments
    }

    object Search {
        private const val ROOT = "search"
        const val GLOBAL = "$ROOT/global"
    }

    object Dev {
        private const val ROOT = "dev"
        const val MENU = "$ROOT/menu"
    }

    /**
     * FCM 관련 경로
     */
    object FCM {
        // FCM 테스트 화면
        const val TEST = "fcm/test"
    }
}