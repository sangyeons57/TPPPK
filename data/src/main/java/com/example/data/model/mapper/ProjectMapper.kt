package com.example.data.model.mapper

import com.example.core_common.util.DateTimeUtil
import com.example.data.model.remote.project.ProjectDto
import com.example.domain.model.Project
import com.google.firebase.Timestamp
import java.time.Instant
import javax.inject.Inject

/**
 * ProjectDto와 Project 도메인 모델 간의 변환을 담당합니다.
 * DateTimeUtil을 주입받아 시간 관련 필드를 처리합니다.
 */
class ProjectMapper @Inject constructor(
    private val dateTimeUtil: DateTimeUtil
) {
    /**
     * ProjectDto (Firestore 데이터)를 Project 도메인 모델로 변환합니다.
     * DTO의 projectId가 사용됩니다.
     */
    fun mapToDomain(dto: ProjectDto): Project {
        return dto.toDomainModelWithTime(dateTimeUtil)
    }

    /**
     * Project 도메인 모델을 Firestore에 저장하기 위한 ProjectDto로 변환합니다.
     */
    fun mapToDto(domain: Project): ProjectDto {
        return domain.toDtoWithTime(dateTimeUtil)
    }
}

/**
 * ProjectDto를 Project 도메인 모델로 변환합니다. DateTimeUtil을 사용하여 Timestamp 필드를 Instant로 변환합니다.
 * ProjectDto의 categoryId는 Project 도메인 모델에 없으므로 무시됩니다.
 */
fun ProjectDto.toDomainModelWithTime(dateTimeUtil: DateTimeUtil): Project {
    val basicDomain = this.toBasicDomainModel() // Uses projectId from DTO
    return basicDomain.copy(
        createdAt = this.createdAt?.let { dateTimeUtil.firebaseTimestampToInstant(it) } ?: Instant.EPOCH,
        updatedAt = this.updatedAt?.let { dateTimeUtil.firebaseTimestampToInstant(it) } ?: Instant.EPOCH
    )
}

/**
 * Project 도메인 모델을 ProjectDto로 변환합니다. DateTimeUtil을 사용하여 Instant 필드를 Timestamp로 변환합니다.
 * Project 도메인 모델에 없는 categoryId는 DTO에서 null로 설정됩니다.
 * (RemoteDataSource에서 프로젝트 생성/업데이트 시 필요에 따라 categoryId를 설정해야 합니다.)
 */
fun Project.toDtoWithTime(dateTimeUtil: DateTimeUtil): ProjectDto {
    val basicDto = ProjectDto.fromBasicDomainModel(this) // Sets categoryId to null
    return basicDto.copy(
        createdAt = dateTimeUtil.instantToFirebaseTimestamp(this.createdAt),
        updatedAt = dateTimeUtil.instantToFirebaseTimestamp(this.updatedAt)
        // categoryId remains null as set by fromBasicDomainModel,
        // consistent with Project domain model not having it.
        // If a specific operation needs to set categoryId (e.g. creating a project within a category),
        // the caller (e.g., RemoteDataSource) should modify the DTO accordingly.
    )
} 