package com.example.data.repository

/**
 * 테스트에서 Android URI를 대체하기 위한 테스트용 클래스
 * Android 의존성 없이 단위 테스트에서 사용 가능
 */
class TestUri(private val uriString: String) {
    // URI 구성 요소 추출
    val scheme: String = uriString.substringBefore("://", "")
    val path: String = uriString.substringAfter("://", "")
    val lastPathSegment: String = path.substringAfterLast("/", "")
    
    override fun toString(): String = uriString
}

/**
 * 테스트에서 Firebase User를 대체하기 위한 테스트용 클래스
 * Firebase 의존성 없이 단위 테스트에서 사용 가능
 */
class TestFirebaseUser(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val isEmailVerified: Boolean = true
) {
    fun getIdToken(forceRefresh: Boolean): TestTask<TestIdTokenResult> {
        return TestTask(TestIdTokenResult(email))
    }
}

/**
 * Firebase Task를 대체하기 위한 테스트용 클래스
 */
class TestTask<T>(private val result: T) {
    
    fun addOnCompleteListener(listener: (TestTask<T>) -> Unit): TestTask<T> {
        listener(this)
        return this
    }
    
    fun isSuccessful(): Boolean = true
    
    fun getResult(): T = result
    
    fun addOnSuccessListener(listener: (T) -> Unit): TestTask<T> {
        listener(result)
        return this
    }
    
    fun addOnFailureListener(listener: (Exception) -> Unit): TestTask<T> {
        // 항상 성공하는 것으로 가정, 필요시 실패 시뮬레이션 추가 가능
        return this
    }
}

/**
 * Firebase IdTokenResult을 대체하기 위한 테스트용 클래스
 */
class TestIdTokenResult(
    val token: String
) 