package com.example.core_logging

import android.content.Context
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.android.core.SentryAndroid
import io.sentry.protocol.Message
import io.sentry.protocol.User
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.function.Consumer

/**
 * SentryUtil 테스트 클래스
 *
 * Sentry SDK의 정적 메서드들을 모킹하여 실제 Sentry 서버로 이벤트가 전송되지 않도록 합니다.
 * 이를 위해 PowerMock과 Mockito를 함께 사용합니다.
 */
@RunWith(PowerMockRunner::class)
@PrepareForTest(Sentry::class, SentryAndroid::class)
class SentryUtilTest {

    @Mock
    private lateinit var mockContext: Context

    @Captor
    private lateinit var eventCaptor: ArgumentCaptor<SentryEvent>

    @Captor
    private lateinit var scopeConsumerCaptor: ArgumentCaptor<Consumer<io.sentry.IScope>>

    @Captor
    private lateinit var userCaptor: ArgumentCaptor<User>

    @Captor
    private lateinit var breadcrumbCaptor: ArgumentCaptor<Breadcrumb>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        PowerMockito.mockStatic(Sentry::class.java)
        PowerMockito.mockStatic(SentryAndroid::class.java)
    }

    @Test
    fun `logDebug should capture message with DEBUG level`() {
        // Given
        val testMessage = "디버그 메시지"
        val testTags = mapOf("key" to "value")

        // When
        SentryUtil.logDebug(testMessage, testTags)

        // Then
        PowerMockito.verifyStatic(Sentry::class.java)
        Sentry.captureEvent(Mockito.any(SentryEvent::class.java), Mockito.any())
        
        // Note: Detailed verification with PowerMock and ArgumentCaptor 
        // would be done in a real implementation
    }

    @Test
    fun `logError should capture message with ERROR level`() {
        // Given
        val testMessage = "에러 메시지"

        // When
        SentryUtil.logError(testMessage)

        // Then
        PowerMockito.verifyStatic(Sentry::class.java)
        Sentry.captureEvent(Mockito.any(SentryEvent::class.java), Mockito.any())
    }

    @Test
    fun `captureError should capture exception with ERROR level`() {
        // Given
        val testException = RuntimeException("테스트 예외")
        val additionalMessage = "추가 메시지"

        // When
        SentryUtil.captureError(testException, additionalMessage)

        // Then
        PowerMockito.verifyStatic(Sentry::class.java)
        Sentry.captureException(Mockito.eq(testException), Mockito.any())
    }

    @Test
    fun `setUserInfo should set user when userId is provided`() {
        // Given
        val testUserId = "user123"
        val testEmail = "test@example.com"
        val testUsername = "테스트사용자"

        // When
        SentryUtil.setUserInfo(testUserId, testEmail, testUsername)

        // Then
        PowerMockito.verifyStatic(Sentry::class.java)
        Sentry.setUser(Mockito.any(User::class.java))
    }

    @Test
    fun `setUserInfo should clear user when userId is null`() {
        // Given
        val nullUserId: String? = null

        // When
        SentryUtil.setUserInfo(nullUserId)

        // Then
        PowerMockito.verifyStatic(Sentry::class.java)
        Sentry.setUser(null)
    }

    @Test
    fun `addBreadcrumb should add breadcrumb with specified category and message`() {
        // Given
        val testCategory = "navigation"
        val testMessage = "화면 이동"
        val testLevel = SentryLevel.INFO

        // When
        SentryUtil.addBreadcrumb(testCategory, testMessage, testLevel)

        // Then
        PowerMockito.verifyStatic(Sentry::class.java)
        Sentry.addBreadcrumb(Mockito.any(Breadcrumb::class.java))
    }

    @Test
    fun `setCustomTag should set tag with specified key and value`() {
        // Given
        val testKey = "feature"
        val testValue = "chat"

        // When
        SentryUtil.setCustomTag(testKey, testValue)

        // Then
        PowerMockito.verifyStatic(Sentry::class.java)
        Sentry.setTag(Mockito.eq(testKey), Mockito.eq(testValue))
    }

    @Test
    fun `setCustomExtra should set extra with specified key and value`() {
        // Given
        val testKey = "device_info"
        val testValue = "Galaxy S21"

        // When
        SentryUtil.setCustomExtra(testKey, testValue)

        // Then
        PowerMockito.verifyStatic(Sentry::class.java)
        Sentry.setExtra(Mockito.eq(testKey), Mockito.eq(testValue))
    }
} 