package com.example.core_common.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import java.util.UUID

/**
 * 미디어 관련 유틸리티 함수들을 제공하는 클래스입니다.
 * Firebase Storage에 파일을 업로드하고 관리하기 위한 유틸리티 메서드를 포함합니다.
 */
object MediaUtil {
    
    // Storage 경로 상수
    private const val STORAGE_ROOT = "gs://your-app.appspot.com"
    private const val USERS_PATH = "users"
    private const val CHATS_PATH = "chats"
    private const val PROJECTS_PATH = "projects"
    
    /**
     * 사용자 프로필 이미지 경로를 생성합니다.
     * 
     * @param userId 사용자 ID
     * @param fileName 파일명 (확장자 포함)
     * @return Storage 경로 문자열 (예: "users/abc123/xyz987.jpg")
     */
    fun getUserProfilePath(userId: String, fileName: String): String {
        return "$USERS_PATH/$userId/$fileName"
    }
    
    /**
     * 채팅 미디어 파일 경로를 생성합니다.
     * 
     * @param channelId 채널 ID
     * @param fileName 파일명 (확장자 포함)
     * @return Storage 경로 문자열 (예: "chats/chan123/xyz987.jpg")
     */
    fun getChatMediaPath(channelId: String, fileName: String): String {
        return "$CHATS_PATH/$channelId/$fileName"
    }
    
    /**
     * 프로젝트 파일 경로를 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param fileName 파일명 (확장자 포함)
     * @return Storage 경로 문자열 (예: "projects/proj123/xyz987.pdf")
     */
    fun getProjectFilePath(projectId: String, fileName: String): String {
        return "$PROJECTS_PATH/$projectId/$fileName"
    }
    
    /**
     * 고유한 파일명을 생성합니다.
     * 
     * @param originalFileName 원본 파일명 (확장자 포함)
     * @return UUID를 사용한 고유 파일명 (예: "xyz987.jpg" -> "a1b2c3d4.jpg")
     */
    fun generateUniqueFileName(originalFileName: String): String {
        val fileExtension = getFileExtension(originalFileName)
        val uuid = UUID.randomUUID().toString()
        return if (fileExtension != null) "$uuid.$fileExtension" else uuid
    }
    
    /**
     * URI에서 MIME 타입을 추출합니다.
     * 
     * @param context 컨텍스트
     * @param uri 파일 URI
     * @return MIME 타입 문자열 또는 null
     */
    fun getMimeType(context: Context, uri: Uri): String? {
        return if (uri.scheme == "content") {
            context.contentResolver.getType(uri)
        } else {
            val fileExtension = getFileExtension(uri.toString())
            fileExtension?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it.lowercase()) }
        }
    }
    
    /**
     * 파일명에서 확장자를 추출합니다.
     * 
     * @param fileName 파일명 또는 URI
     * @return 확장자 (예: "jpg", "pdf") 또는 null
     */
    fun getFileExtension(fileName: String): String? {
        return fileName.substringAfterLast('.', "").takeIf { it.isNotEmpty() }
    }
    
    /**
     * 파일명에서 UUID를 추출합니다.
     * 
     * @param fileName 파일명 (확장자 포함)
     * @return UUID 문자열 또는 null
     */
    fun extractUuidFromFileName(fileName: String): String? {
        val nameWithoutExt = fileName.substringBeforeLast('.')
        return if (nameWithoutExt.matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"))) {
            nameWithoutExt
        } else {
            null
        }
    }
    
    /**
     * 파일 크기를 사람이 읽기 쉬운 형식으로 변환합니다.
     * 
     * @param size 바이트 단위 파일 크기
     * @return 포맷된 문자열 (예: "2.5 MB")
     */
    @SuppressLint("DefaultLocale")
    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        
        return "%.1f %s".format(
            size / Math.pow(1024.0, digitGroups.toDouble()),
            units[digitGroups.coerceAtMost(units.size - 1)]
        )
    }
    
    /**
     * 파일 확장자로부터 MIME 타입을 가져옵니다.
     * 
     * @param extension 파일 확장자 (예: "jpg", "pdf")
     * @return MIME 타입 문자열 (예: "image/jpeg", "application/pdf")
     */
    fun getMimeTypeFromExtension(extension: String): String? {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
    }
    
    /**
     * URI에서 파일 확장자를 추출합니다.
     * 
     * @param uri 파일 URI
     * @return 파일 확장자 또는 null
     */
    fun getFileExtension(uri: Uri): String {
        Log.d("MediaUtil", "getFileExtension called with URI: $uri")
        return MimeTypeMap.getFileExtensionFromUrl(uri.toString())
    }
    
    /**
     * MIME 타입이 이미지인지 확인합니다.
     * 
     * @param mimeType MIME 타입
     * @return 이미지 MIME 타입이면 true, 아니면 false
     */
    fun isImageMimeType(mimeType: String?): Boolean {
        return mimeType?.startsWith("image/") == true
    }
    
    /**
     * MIME 타입이 문서인지 확인합니다.
     * 
     * @param mimeType MIME 타입
     * @return 문서 MIME 타입이면 true, 아니면 false
     */
    fun isDocumentMimeType(mimeType: String?): Boolean {
        val documentMimeTypes = listOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain"
        )
        return documentMimeTypes.contains(mimeType)
    }
    
    /**
     * URI에서 파일 크기를 가져옵니다.
     * 
     * @param context 컨텍스트
     * @param uri 파일 URI
     * @return 파일 크기 (바이트) 또는 -1 (실패 시)
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.available().toLong()
            } ?: -1L
        } catch (e: Exception) {
            -1L
        }
    }
}
