package com.example.data.datasource.remote.project

import com.example.core_common.constants.FirestoreConstants.Collections
import com.example.core_common.constants.FirestoreConstants.ProjectFields
import com.example.data.model.remote.project.ProjectDto
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ProjectRemoteDataSource 인터페이스의 Firestore 구현체입니다.
 */
@Singleton
class ProjectRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ProjectRemoteDataSource {

    private val projectCollection = firestore.collection(Collections.PROJECTS)

    // --- ProjectRemoteDataSource 인터페이스 함수 구현 --- 

    /**
     * Firestore에서 사용자가 참여하고 있는 프로젝트 목록을 가져옵니다.
     *
     * @param userId 사용자 ID.
     * @return kotlin.Result 객체. 성공 시 List<ProjectDto>, 실패 시 Exception 포함.
     */
    override suspend fun getParticipatingProjects(userId: String): Result<List<ProjectDto>> = runCatching {
        val querySnapshot = projectCollection
            .whereArrayContains(ProjectFields.PARTICIPATING_MEMBERS, userId)
            .get()
            .await()
        querySnapshot.documents.mapNotNull { it.toObject(ProjectDto::class.java) }
    }

    /**
     * Firestore에서 프로젝트 상세 정보를 가져옵니다.
     *
     * @param projectId 프로젝트 ID.
     * @return kotlin.Result 객체. 성공 시 ProjectDto, 실패 시 Exception 포함.
     */
    override suspend fun getProjectDetails(projectId: String): Result<ProjectDto> = runCatching {
        val documentSnapshot = projectCollection.document(projectId).get().await()
        documentSnapshot.toObject(ProjectDto::class.java)
            ?: throw NoSuchElementException("Project document with id $projectId not found or could not be deserialized.")
    }

    /**
     * Firestore에 새 프로젝트를 생성합니다.
     *
     * @param projectDto 생성할 프로젝트 정보 DTO.
     * @return kotlin.Result 객체. 성공 시 생성된 프로젝트 ID(String), 실패 시 Exception 포함.
     */
    override suspend fun createProject(projectDto: ProjectDto): Result<String> = runCatching {
        val documentReference = projectCollection.add(projectDto).await()
        documentReference.id
    }

    /*
    override suspend fun getParticipatingProjects(userId: String): List<ProjectDto> {
        // Firestore에서 참여 중인 프로젝트 목록 조회 로직 구현
        // 예: 'participants' 배열 필드에 userId가 포함된 문서 쿼리
        val querySnapshot = projectCollection.whereArrayContains("participants", userId).get().await()
        return querySnapshot.documents.mapNotNull { it.toObject(ProjectDto::class.java) }
    }
    */

    /*
    override suspend fun getProjectDetails(projectId: String): ProjectDto {
        // Firestore에서 프로젝트 상세 정보 조회 로직 구현
        val documentSnapshot = projectCollection.document(projectId).get().await()
        return documentSnapshot.toObject(ProjectDto::class.java)
            ?: throw NoSuchElementException("Project document with id $projectId not found.")
    }
    */

    /*
    override suspend fun createProject(projectDto: ProjectDto): String {
        // Firestore에 새 프로젝트 생성 로직 구현
        val documentReference = projectCollection.add(projectDto).await()
        return documentReference.id
    }
    */

    // ... 다른 함수들의 실제 구현 추가 ...

} 