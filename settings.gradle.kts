pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TeamnovaPersonalProjectProjectingKotlin"
include(":app")
include(":app-api")
include(":navigation")
include(":data")
include(":domain")

include(":core:core_common")
include(":core:core_ui")
include(":core:core_navigation")
include(":core:core_fcm")

include(":feature:feature_chat")
include(":feature:feature_splash")
include(":feature:feature_dev")
include(":feature:feature_friends")
include(":feature:feature_main")
include(":feature:feature_profile")
include(":feature:feature_project_detail")
include(":feature:feature_add_schedule")
include(":feature:feature_search")
include(":feature:feature_settings")
include(":feature:feature_project_setting")
include(":feature:feature_model")
include(":feature:feature_find_password")
include(":feature:feature_change_password")
include(":feature:feature_calendar_24hour")
include(":feature:feature_edit_schedule")
include(":feature:feature_edit_profile")
include(":feature:feature_schedule_detail")
include(":feature:feature_calendar")
include(":feature:feature_home")
include(":feature:feature_login")
include(":feature:feature_signup")
include(":feature:feature_privacy_policy")
include(":feature:feature_terms_of_service")
include(":feature:feature_member_list")
include(":feature:feature_edit_member")
include(":feature:feature_role_list")
include(":feature:feature_edit_role")
include(":feature:feature_edit_category")
include(":feature:feature_add_project")
include(":feature:feature_edit_channel")
include(":feature:feature_join_project")
include(":feature:feature_set_project_name")
include(":feature:feature_accept_friend")
include(":feature:feature_add_role")
include(":feature:feature_tasks")
