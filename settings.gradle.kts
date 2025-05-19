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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TeamnovaPersonalProjectProjectingKotlin"
include(":app")
include(":navigation")
include(":data")
include(":domain")

include(":core:core_common")
include(":core:core_logging")
include(":core:core_ui")
include(":core:core_navigation")
include(":core:core_fcm")

include(":feature:feature_chat")
include(":feature:feature_auth")
include(":feature:feature_dev")
include(":feature:feature_friends")
include(":feature:feature_main")
include(":feature:feature_profile")
include(":feature:feature_project")
include(":feature:feature_schedule")
include(":feature:feature_search")
include(":feature:feature_settings")
include(":app_api")
