package com.example.data.datasource.remote.user

import com.example.core_common.constants.FirestoreConstants.Collections
import com.example.data.model.remote.user.UserDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.NoSuchElementException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UserRemoteDataSource 인터페이스의 Firestore 구현체입니다.
 */
@Singleton
class UserRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRemoteDataSource {

    private val userCollection = firestore.collection(Collections.USERS) // 'users' 컬렉션 참조 예시

    /**
     * Firestore에서 특정 사용자의 프로필 정보를 가져옵니다.
     *
     * @param userId 가져올 사용자의 ID (Firebase Auth UID).
     * @return kotlin.Result 객체. 성공 시 UserDto, 실패 시 Exception 포함.
     */
    override suspend fun getUserProfile(userId: String): Result<UserDto> = runCatching {
        val documentSnapshot = userCollection.document(userId).get().await()
        documentSnapshot.toObject(UserDto::class.java)
            ?: throw NoSuchElementException("User document with id $userId not found or could not be deserialized.")
    }

    /**
     * Firestore에서 특정 사용자의 프로필 정보를 업데이트합니다.
     * 문서가 존재하지 않으면 생성하고, 존재하면 제공된 필드만 병합합니다.
     *
     * @param userId 업데이트할 사용자의 ID (Firebase Auth UID).
     * @param userDto 업데이트할 사용자 정보 DTO.
     * @return kotlin.Result 객체. 성공 시 Unit, 실패 시 Exception 포함.
     */
    override suspend fun updateUserProfile(userId: String, userDto: UserDto): Result<Unit> = runCatching {
        userCollection.document(userId).set(userDto, SetOptions.merge()).await()
        // Success is implied if no exception is thrown, runCatching handles this.
        // The return type of await() for set is Void, so we don't explicitly return Unit.
        // runCatching wraps the implicit Unit success value.
    }

    // ... 다른 함수들의 실제 구현 추가 ...

} 