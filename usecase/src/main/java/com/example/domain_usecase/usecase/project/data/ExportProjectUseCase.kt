package com.example.domain_usecase.usecase.project.data

import android.content.Context
import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * 프로젝트 데이터를 내보내는 UseCase
 * Firebase Function을 통해 프로젝트 전체 데이터를 가져와서 로컬에 파일로 저장합니다.
 */
class ExportProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val authRepository: AuthRepository,
    private val context: Context
) {

    /**
     * 프로젝트를 내보냅니다.
     *
     * @param projectId 내보낼 프로젝트 ID
     * @param includeMessages 메시지 포함 여부 (기본값: false)
     * @param format 내보내기 형식 ('json' 또는 'csv', 기본값: 'json')
     * @return 내보내기 진행 상황을 나타내는 Flow.
     *         Loading -> Success(파일 URI) 또는 Failure(exception) 순서로 발행
     */
    operator fun invoke(
        projectId: DocumentId,
        includeMessages: Boolean = false,
        format: ExportFormat = ExportFormat.JSON
    ): Flow<CustomResult<ExportResult, Exception>> = flow {
        emit(CustomResult.Loading)

        if (projectId.isBlank()) {
            emit(CustomResult.Failure(IllegalArgumentException("Project ID cannot be blank")))
            return@flow
        }

        // 1. 사용자 인증 확인
        val sessionResult = authRepository.getCurrentUserSession()
        if (sessionResult !is CustomResult.Success) {
            emit(CustomResult.Failure(Exception("User not authenticated")))
            return@flow
        }

        try {
            // 2. Firebase Function 호출하여 데이터 가져오기
            val exportDataResult = projectRepository.exportProject(
                projectId = projectId.value,
                includeMessages = includeMessages,
                format = format.value
            )

            when (exportDataResult) {
                is CustomResult.Success -> {
                    val exportData = exportDataResult.data
                    val success = exportData["success"] as? Boolean ?: false

                    if (!success) {
                        val errorMessage = exportData["message"] as? String ?: "Export failed"
                        emit(CustomResult.Failure(Exception(errorMessage)))
                        return@flow
                    }

                    // 3. 데이터를 로컬 파일로 저장
                    val projectData = exportData["data"] as? Map<String, Any>
                    if (projectData == null) {
                        emit(CustomResult.Failure(Exception("No data received from export")))
                        return@flow
                    }

                    val fileResult = saveDataToFile(projectData, projectId, format)
                    when (fileResult) {
                        is CustomResult.Success -> {
                            emit(CustomResult.Success(fileResult.data))
                        }

                        is CustomResult.Failure -> {
                            emit(CustomResult.Failure(fileResult.error))
                        }

                        else -> {
                            emit(CustomResult.Failure(Exception("Unexpected result from file save")))
                        }
                    }
                }

                is CustomResult.Failure -> {
                    emit(CustomResult.Failure(exportDataResult.error))
                }

                else -> {
                    emit(CustomResult.Failure(Exception("Unexpected result type from export")))
                }
            }
        } catch (e: Exception) {
            emit(CustomResult.Failure(Exception("Export failed: ${e.message}", e)))
        }
    }.catch { e ->
        emit(
            CustomResult.Failure(
                Exception(
                    "An unexpected error occurred during export: ${e.message}",
                    e
                )
            )
        )
    }

    /**
     * 내보낸 데이터를 로컬 파일로 저장합니다.
     */
    private suspend fun saveDataToFile(
        data: Map<String, Any>,
        projectId: DocumentId,
        format: ExportFormat
    ): CustomResult<ExportResult, Exception> {
        return try {
            val projectInfo = data["projectInfo"] as? Map<String, Any>
            val projectName = projectInfo?.get("name") as? String ?: "Unknown_Project"

            // 파일명 생성 (안전한 파일명으로 변환)
            val safeProjectName = projectName.replace(Regex("[^a-zA-Z0-9가-힣\\s-_]"), "")
                .replace(Regex("\\s+"), "_")
                .take(50) // 파일명 길이 제한

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                .format(Date())

            val fileName = "${safeProjectName}_${timestamp}.${format.extension}"

            // 외부 저장소의 Downloads 폴더에 저장
            val downloadsDir = File(context.getExternalFilesDir(null), "Downloads")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val file = File(downloadsDir, fileName)

            when (format) {
                ExportFormat.JSON -> {
                    saveAsJson(file, data)
                }

                ExportFormat.CSV -> {
                    saveAsCsv(file, data)
                }
            }

            CustomResult.Success(
                ExportResult(
                    fileUri = Uri.fromFile(file),
                    fileName = fileName,
                    filePath = file.absolutePath,
                    fileSize = file.length(),
                    format = format,
                    exportedAt = Date()
                )
            )
        } catch (e: Exception) {
            CustomResult.Failure(Exception("Failed to save file: ${e.message}", e))
        }
    }

    /**
     * JSON 형식으로 저장
     */
    private fun saveAsJson(file: File, data: Map<String, Any>) {
        // 간단한 JSON 형태로 수동 변환 (라이브러리 의존성 없이)
        FileWriter(file).use { writer ->
            writer.write(convertToJsonString(data, 0))
        }
    }

    /**
     * 맵을 JSON 문자열로 변환하는 헬퍼 함수
     */
    private fun convertToJsonString(obj: Any?, indent: Int = 0): String {
        val indentStr = "  ".repeat(indent)
        val nextIndentStr = "  ".repeat(indent + 1)

        return when (obj) {
            null -> "null"
            is String -> "\"${obj.replace("\"", "\\\"").replace("\n", "\\n")}\""
            is Number -> obj.toString()
            is Boolean -> obj.toString()
            is Map<*, *> -> {
                if (obj.isEmpty()) {
                    "{}"
                } else {
                    val entries = obj.entries.joinToString(",\n$nextIndentStr") { (key, value) ->
                        "\"$key\": ${convertToJsonString(value, indent + 1)}"
                    }
                    "{\n$nextIndentStr$entries\n$indentStr}"
                }
            }

            is List<*> -> {
                if (obj.isEmpty()) {
                    "[]"
                } else {
                    val items = obj.joinToString(",\n$nextIndentStr") { item ->
                        convertToJsonString(item, indent + 1)
                    }
                    "[\n$nextIndentStr$items\n$indentStr]"
                }
            }

            else -> "\"$obj\""
        }
    }

    /**
     * CSV 형식으로 저장 (기본적인 테이블 데이터만)
     */
    private fun saveAsCsv(file: File, data: Map<String, Any>) {
        FileWriter(file).use { writer ->
            // 프로젝트 정보
            writer.append("=== PROJECT INFO ===\n")
            val projectInfo = data["projectInfo"] as? Map<String, Any> ?: emptyMap()
            projectInfo.forEach { (key, value) ->
                writer.append("$key,$value\n")
            }

            writer.append("\n=== CATEGORIES ===\n")
            writer.append("ID,Name,Position,Created At,Updated At\n")
            val categories = data["categories"] as? List<Map<String, Any>> ?: emptyList()
            categories.forEach { category ->
                writer.append("${category["id"]},${category["name"]},${category["position"]},${category["createdAt"]},${category["updatedAt"]}\n")
            }

            writer.append("\n=== CHANNELS ===\n")
            writer.append("ID,Category ID,Name,Type,Description,Position,Created At,Updated At\n")
            val channels = data["channels"] as? List<Map<String, Any>> ?: emptyList()
            channels.forEach { channel ->
                writer.append("${channel["id"]},${channel["categoryId"]},${channel["name"]},${channel["type"]},${channel["description"]},${channel["position"]},${channel["createdAt"]},${channel["updatedAt"]}\n")
            }

            writer.append("\n=== MEMBERS ===\n")
            writer.append("User ID,Role IDs,Joined At,Status\n")
            val members = data["members"] as? List<Map<String, Any>> ?: emptyList()
            members.forEach { member ->
                val roleIds = (member["roleIds"] as? List<String>)?.joinToString(";") ?: ""
                writer.append("${member["userId"]},$roleIds,${member["joinedAt"]},${member["status"]}\n")
            }

            writer.append("\n=== ROLES ===\n")
            writer.append("ID,Name,Permissions,Color,Position,Created At,Updated At\n")
            val roles = data["roles"] as? List<Map<String, Any>> ?: emptyList()
            roles.forEach { role ->
                val permissions = (role["permissions"] as? List<String>)?.joinToString(";") ?: ""
                writer.append("${role["id"]},${role["name"]},$permissions,${role["color"]},${role["position"]},${role["createdAt"]},${role["updatedAt"]}\n")
            }

            // 메시지가 있는 경우 포함
            val messages = data["messages"] as? List<Map<String, Any>>
            if (!messages.isNullOrEmpty()) {
                writer.append("\n=== MESSAGES ===\n")
                writer.append("ID,Channel ID,Category ID,Content,Author ID,Timestamp,Edited,Edited At\n")
                messages.forEach { message ->
                    // CSV에서 콤마와 개행 문자 처리
                    val content = (message["content"] as? String ?: "")
                        .replace("\"", "\"\"")
                        .replace("\n", "\\n")
                    writer.append("${message["id"]},${message["channelId"]},${message["categoryId"]},\"$content\",${message["authorId"]},${message["timestamp"]},${message["edited"]},${message["editedAt"]}\n")
                }
            }
        }
    }
}

/**
 * 내보내기 형식
 */
enum class ExportFormat(val value: String, val extension: String) {
    JSON("json", "json"),
    CSV("csv", "csv")
}

/**
 * 내보내기 결과
 */
data class ExportResult(
    val fileUri: Uri,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val format: ExportFormat,
    val exportedAt: Date
) {
    /**
     * 파일 크기를 인간이 읽기 쉬운 형태로 반환
     */
    fun getReadableFileSize(): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = fileSize.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return String.format("%.1f %s", size, units[unitIndex])
    }
}