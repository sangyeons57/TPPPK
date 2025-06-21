package com.example.data.datasource.remote

import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.example.data.model.remote.MemberDTO
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로젝트 멤버 정보에 접근하기 위한 인터페이스입니다.
 * DefaultDatasource를 확장하여 특정 프로젝트의 멤버 문서에 대한 CRUD 및 관찰 기능을 제공합니다.
 * 멤버 데이터는 `projects/{projectId}/members/{userId}` 경로에 저장되며, `userId`가 문서 ID가 됩니다.
 * 모든 작업 전에 `setCollection(projectId)`를 호출하여 프로젝트 컨텍스트를 설정해야 합니다.
 */
interface MemberRemoteDataSource : DefaultDatasource {

}

@Singleton
class MemberRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DefaultDatasourceImpl<MemberDTO>(firestore, MemberDTO::class.java), MemberRemoteDataSource {
}
