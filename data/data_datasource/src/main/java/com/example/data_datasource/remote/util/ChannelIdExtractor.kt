package com.example.data_datasource.remote.util

import android.util.Log

/**
 * Firestore 경로에서 channelId를 추출하는 유틸리티
 */
object ChannelIdExtractor {

    private const val TAG = "ChannelIdExtractor"

    /**
     * Firestore 문서 경로에서 channelId를 추출
     *
     * @param documentPath Firestore 문서 경로
     * @return 추출된 channelId (실패 시 null)
     */
    fun extractChannelIdFromPath(documentPath: String): String? {
        return try {
            when {
                // DM 채널: dm_channels/{channelId}/messages/{messageId}
                documentPath.contains("/dm_channels/") -> {
                    val parts = documentPath.split("/")
                    val channelIndex = parts.indexOf("dm_channels")
                    if (channelIndex >= 0 && channelIndex + 1 < parts.size) {
                        parts[channelIndex + 1]
                    } else null
                }

                // 프로젝트 채널: projects/{projectId}/project_channels/{channelId}/messages/{messageId}
                documentPath.contains("/project_channels/") -> {
                    val parts = documentPath.split("/")
                    val channelIndex = parts.indexOf("project_channels")
                    if (channelIndex >= 0 && channelIndex + 1 < parts.size) {
                        parts[channelIndex + 1]
                    } else null
                }

                else -> {
                    Log.w(TAG, "Unknown path format: $documentPath")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract channelId from path: $documentPath", e)
            null
        }
    }

    /**
     * 메시지 컬렉션 경로에서 channelId를 추출
     *
     * @param collectionPath 메시지 컬렉션 경로
     * @return 추출된 channelId (실패 시 null)
     */
    fun extractChannelIdFromCollectionPath(collectionPath: String): String? {
        return try {
            when {
                // DM 채널: dm_channels/{channelId}/messages
                collectionPath.contains("/dm_channels/") -> {
                    val parts = collectionPath.split("/")
                    val channelIndex = parts.indexOf("dm_channels")
                    if (channelIndex >= 0 && channelIndex + 1 < parts.size) {
                        parts[channelIndex + 1]
                    } else null
                }

                // 프로젝트 채널: projects/{projectId}/project_channels/{channelId}/messages
                collectionPath.contains("/project_channels/") -> {
                    val parts = collectionPath.split("/")
                    val channelIndex = parts.indexOf("project_channels")
                    if (channelIndex >= 0 && channelIndex + 1 < parts.size) {
                        parts[channelIndex + 1]
                    } else null
                }

                else -> {
                    Log.w(TAG, "Unknown collection path format: $collectionPath")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract channelId from collection path: $collectionPath", e)
            null
        }
    }

    /**
     * 채널 타입과 ID로 Firestore 경로 생성
     *
     * @param channelType 채널 타입 ("DM" 또는 "PROJECT")
     * @param channelId 채널 ID
     * @param projectId 프로젝트 ID (PROJECT 타입일 때만)
     * @return Firestore 컬렉션 경로
     */
    fun buildCollectionPath(
        channelType: String,
        channelId: String,
        projectId: String? = null
    ): String {
        return when (channelType) {
            "DM" -> "dm_channels/$channelId/messages"
            "PROJECT" -> {
                requireNotNull(projectId) { "ProjectId is required for PROJECT channel type" }
                "projects/$projectId/project_channels/$channelId/messages"
            }

            else -> throw IllegalArgumentException("Unknown channel type: $channelType")
        }
    }
} 