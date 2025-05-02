package com.example.data.repository

import com.example.domain.model.MessageResult
import com.example.domain.model.SearchResultItem
import com.example.domain.model.SearchScope
import com.example.domain.model.UserResult
import com.example.domain.repository.SearchRepository
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * SearchRepository의 가짜(Fake) 구현체
 * 
 * 이 클래스는 테스트 용도로 SearchRepository 인터페이스를 인메모리 방식으로 구현합니다.
 * 외부 의존성 없이 Repository 기능을 테스트할 수 있습니다.
 */
class FakeSearchRepository : SearchRepository {
    
    // 인메모리 검색 데이터 저장소
    private val messages = ConcurrentHashMap<String, MessageResult>()
    private val users = ConcurrentHashMap<String, UserResult>()
    
    // 에러 시뮬레이션을 위한 설정
    private var shouldSimulateError = false
    private var errorToSimulate: Exception = Exception("Simulated error")
    
    /**
     * 테스트를 위해 메시지 데이터 추가
     */
    fun addMessage(message: MessageResult) {
        messages[message.id] = message
    }
    
    /**
     * 테스트를 위해 사용자 데이터 추가
     */
    fun addUser(user: UserResult) {
        users[user.id] = user
    }
    
    /**
     * 테스트를 위해 모든 검색 데이터 초기화
     */
    fun clearData() {
        messages.clear()
        users.clear()
    }
    
    /**
     * 테스트를 위해 에러 시뮬레이션 설정
     */
    fun setShouldSimulateError(shouldError: Boolean, error: Exception = Exception("Simulated error")) {
        shouldSimulateError = shouldError
        errorToSimulate = error
    }
    
    /**
     * 모의 검색 데이터 추가
     */
    fun setupTestData() {
        // 테스트 메시지 데이터 추가
        addMessage(
            MessageResult(
                id = "msg-1",
                channelId = "channel-1",
                channelName = "일반",
                messageId = 1,
                senderName = "홍길동",
                messageContent = "안녕하세요 모두들",
                timestamp = LocalDateTime.now().minusDays(1)
            )
        )
        
        addMessage(
            MessageResult(
                id = "msg-2",
                channelId = "channel-1",
                channelName = "일반",
                messageId = 2,
                senderName = "이순신",
                messageContent = "프로젝트 진행상황 보고합니다",
                timestamp = LocalDateTime.now().minusHours(5)
            )
        )
        
        addMessage(
            MessageResult(
                id = "msg-3",
                channelId = "channel-2",
                channelName = "개발",
                messageId = 3,
                senderName = "김철수",
                messageContent = "테스트 코드 작성 중입니다",
                timestamp = LocalDateTime.now().minusHours(2)
            )
        )
        
        // 테스트 사용자 데이터 추가
        addUser(
            UserResult(
                id = "user-1",
                userId = "user-1",
                userName = "홍길동",
                profileImageUrl = null,
                status = "온라인"
            )
        )
        
        addUser(
            UserResult(
                id = "user-2",
                userId = "user-2",
                userName = "이순신",
                profileImageUrl = null,
                status = "오프라인"
            )
        )
        
        addUser(
            UserResult(
                id = "user-3",
                userId = "user-3",
                userName = "김철수",
                profileImageUrl = null,
                status = "자리비움"
            )
        )
    }

    override suspend fun search(query: String, scope: SearchScope): Result<List<SearchResultItem>> {
        // 에러 시뮬레이션 확인
        if (shouldSimulateError) {
            return Result.failure(errorToSimulate)
        }
        
        // 검색어가 비어있으면 빈 결과 반환
        if (query.isBlank()) {
            return Result.success(emptyList())
        }
        
        // 실제 검색 로직 구현
        val results = mutableListOf<SearchResultItem>()
        
        // 쿼리를 소문자로 변환하여 대소문자 구분 없이 검색
        val lowercaseQuery = query.lowercase()
        
        when (scope) {
            SearchScope.MESSAGES, SearchScope.ALL -> {
                // 메시지 내용이나 발신자 이름에서 검색
                val matchingMessages = messages.values.filter {
                    it.messageContent.lowercase().contains(lowercaseQuery) || 
                    it.senderName.lowercase().contains(lowercaseQuery)
                }
                results.addAll(matchingMessages)
            }
            
            SearchScope.USERS, SearchScope.ALL -> {
                // 사용자 이름에서 검색
                val matchingUsers = users.values.filter {
                    it.userName.lowercase().contains(lowercaseQuery)
                }
                results.addAll(matchingUsers)
            }
        }
        
        return Result.success(results)
    }
} 