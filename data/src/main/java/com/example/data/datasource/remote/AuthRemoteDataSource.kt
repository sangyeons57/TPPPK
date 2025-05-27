
package com.example.data.datasource.remote

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRemoteDataSource {

    /**
     * 현재 로그인 상태(사용자)의 변경을 실시간으로 관찰합니다.
     * @return 로그인 시 FirebaseUser, 로그아웃 시 null을 방출하는 Flow
     */
    fun observeAuthState(): Flow<FirebaseUser?>

    /**
     * 현재 로그인된 FirebaseUser 객체를 즉시 반환합니다. 동기적인 세션 확인에 사용됩니다.
     * @return 로그인 상태이면 FirebaseUser, 아니면 null
     */
    fun getCurrentUser(): FirebaseUser?

    /**
     * 이메일과 비밀번호로 회원가입을 시도합니다.
     * @param email 가입할 이메일
     * @param password 사용할 비밀번호
     * @return 성공 시 생성된 사용자의 UID를 포함한 Result 객체
     */
    suspend fun signUp(email: String, password: String): Result<String>

    /**
     * 이메일과 비밀번호로 로그인을 시도합니다.
     * @param email 로그인할 이메일
     * @param password 비밀번호
     * @return 성공 시 로그인된 사용자의 UID를 포함한 Result 객체
     */
    suspend fun signIn(email: String, password: String): Result<String>

    /**
     * 현재 로그인된 사용자를 로그아웃합니다.
     */
    suspend fun signOut(): Result<Unit>
}

