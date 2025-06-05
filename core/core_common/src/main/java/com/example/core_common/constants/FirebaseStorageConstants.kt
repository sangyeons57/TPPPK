package com.example.core_common.constants

object FirebaseStorageConstants {

    // Base Paths
    private const val USER_PROFILE_IMAGES = "user_profile_images"
    private const val PROJECT_PROFILE_IMAGES = "project_profile_images"
    private const val DM_CHANNEL_FILES = "dm_channel_files"
    private const val PROJECT_CHANNEL_FILES = "project_channel_files"

    // Path Builder Functions

    /**
     * Generates the Firebase Storage path for a user's profile image.
     * Example: user_profile_images/{userId}/{fileName}
     */
    fun getUserProfileImagePath(userId: String, fileName: String): String {
        return "$USER_PROFILE_IMAGES/$userId/$fileName"
    }

    /**
     * Generates the Firebase Storage path for a project's profile image.
     * Example: project_profile_images/{projectId}/{fileName}
     */
    fun getProjectProfileImagePath(projectId: String, fileName: String): String {
        return "$PROJECT_PROFILE_IMAGES/$projectId/$fileName"
    }

    /**
     * Generates the Firebase Storage path for a file in a DM channel.
     * Example: dm_channel_files/{dmChannelId}/{messageId}/{fileType}/{fileName}
     * @param fileType Type of the file, e.g., "images", "videos", "documents".
     */
    fun getDmChannelFilePath(dmChannelId: String, messageId: String, fileType: String, fileName: String): String {
        return "$DM_CHANNEL_FILES/$dmChannelId/$messageId/$fileType/$fileName"
    }

    /**
     * Generates the Firebase Storage path for a file in a project channel.
     * Example: project_channel_files/{projectChannelId}/{messageId}/{fileType}/{fileName}
     * @param fileType Type of the file, e.g., "images", "videos", "documents".
     */
    fun getProjectChannelFilePath(projectChannelId: String, messageId: String, fileType: String, fileName: String): String {
        return "$PROJECT_CHANNEL_FILES/$projectChannelId/$messageId/$fileType/$fileName"
    }
}
