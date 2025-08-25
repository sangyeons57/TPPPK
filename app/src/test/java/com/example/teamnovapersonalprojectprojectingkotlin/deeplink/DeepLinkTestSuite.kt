package com.example.teamnovapersonalprojectprojectingkotlin.deeplink

import android.content.Intent
import android.net.Uri
import com.example.teamnovapersonalprojectprojectingkotlin.deeplink.util.DeepLinkExtractor
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

/**
 * 딥링크 시스템 통합 테스트 스위트
 *
 * 다양한 딥링크 시나리오를 검증하여 안정적인 딥링크 처리를 보장합니다.
 *
 * 테스트 범위:
 * - 초대 코드 추출 (HTTPS, 커스텀 스킴)
 * - 채널 ID 추출 (앱 스킴, 개발용 스킴)
 * - FCM 알림 데이터 추출 및 검증
 * - 잘못된 URI 처리
 * - 딥링크 생성 및 검증 유틸리티
 */
class DeepLinkTestSuite {

    // ================================
    // 초대 코드 추출 테스트
    // ================================

    @Test
    fun `extractInviteCode should extract from HTTPS deep link`() {
        val uri = Uri.parse("https://tpppk.app/invite/ABC123")
        val result = DeepLinkExtractor.extractInviteCode(uri)
        assertEquals("ABC123", result)
    }

    @Test
    fun `extractInviteCode should extract from custom scheme with path`() {
        val uri = Uri.parse("tpppk://invite/XYZ789")
        val result = DeepLinkExtractor.extractInviteCode(uri)
        assertEquals("XYZ789", result)
    }

    @Test
    fun `extractInviteCode should extract from custom scheme with query parameter`() {
        val uri = Uri.parse("tpppk://invite?code=QWE456")
        val result = DeepLinkExtractor.extractInviteCode(uri)
        assertEquals("QWE456", result)
    }

    @Test
    fun `extractInviteCode should return null for unsupported scheme`() {
        val uri = Uri.parse("http://example.com/invite/ABC123")
        val result = DeepLinkExtractor.extractInviteCode(uri)
        assertNull(result)
    }

    @Test
    fun `extractInviteCode should return null for wrong host`() {
        val uri = Uri.parse("https://wrong.com/invite/ABC123")
        val result = DeepLinkExtractor.extractInviteCode(uri)
        assertNull(result)
    }

    @Test
    fun `extractInviteCode should return null for wrong path`() {
        val uri = Uri.parse("https://tpppk.app/wrongpath/ABC123")
        val result = DeepLinkExtractor.extractInviteCode(uri)
        assertNull(result)
    }

    // ================================
    // 채널 ID 추출 테스트
    // ================================

    @Test
    fun `extractChannelId should extract from app scheme`() {
        val uri = Uri.parse("app://channel/channel123")
        val result = DeepLinkExtractor.extractChannelId(uri)
        assertEquals("channel123", result)
    }

    @Test
    fun `extractChannelId should extract from custom scheme`() {
        val uri = Uri.parse("tpppk://channel/channel456")
        val result = DeepLinkExtractor.extractChannelId(uri)
        assertEquals("channel456", result)
    }

    @Test
    fun `extractChannelId should extract from HTTPS scheme`() {
        val uri = Uri.parse("https://tpppk.app/channel/channel789")
        val result = DeepLinkExtractor.extractChannelId(uri)
        assertEquals("channel789", result)
    }

    @Test
    fun `extractChannelId should return null for unsupported scheme`() {
        val uri = Uri.parse("http://example.com/channel/channel123")
        val result = DeepLinkExtractor.extractChannelId(uri)
        assertNull(result)
    }

    @Test
    fun `extractChannelId should return null for wrong host`() {
        val uri = Uri.parse("app://wronghost/channel123")
        val result = DeepLinkExtractor.extractChannelId(uri)
        assertNull(result)
    }

    // ================================
    // FCM 알림 데이터 추출 테스트
    // ================================

    @Test
    fun `extractFcmNotificationData should extract valid data`() {
        val intent = mockk<Intent>()
        every { intent.getStringExtra("notification_type") } returns "mention"
        every { intent.getStringExtra("message_id") } returns "msg123"
        every { intent.getStringExtra("channel_id") } returns "channel456"
        every { intent.getStringExtra("project_id") } returns "project789"

        val result = DeepLinkExtractor.extractFcmNotificationData(intent)

        assertNotNull(result)
        assertEquals("mention", result?.notificationType)
        assertEquals("msg123", result?.messageId)
        assertEquals("channel456", result?.channelId)
        assertEquals("project789", result?.projectId)
    }

    @Test
    fun `extractFcmNotificationData should handle null project_id`() {
        val intent = mockk<Intent>()
        every { intent.getStringExtra("notification_type") } returns "mention"
        every { intent.getStringExtra("message_id") } returns "msg123"
        every { intent.getStringExtra("channel_id") } returns "channel456"
        every { intent.getStringExtra("project_id") } returns null

        val result = DeepLinkExtractor.extractFcmNotificationData(intent)

        assertNotNull(result)
        assertEquals("mention", result?.notificationType)
        assertEquals("msg123", result?.messageId)
        assertEquals("channel456", result?.channelId)
        assertNull(result?.projectId)
    }

    @Test
    fun `extractFcmNotificationData should return null for missing notification_type`() {
        val intent = mockk<Intent>()
        every { intent.getStringExtra("notification_type") } returns null
        every { intent.getStringExtra("message_id") } returns "msg123"
        every { intent.getStringExtra("channel_id") } returns "channel456"
        every { intent.getStringExtra("project_id") } returns "project789"

        val result = DeepLinkExtractor.extractFcmNotificationData(intent)
        assertNull(result)
    }

    @Test
    fun `extractFcmNotificationData should return null for blank notification_type`() {
        val intent = mockk<Intent>()
        every { intent.getStringExtra("notification_type") } returns "   "
        every { intent.getStringExtra("message_id") } returns "msg123"
        every { intent.getStringExtra("channel_id") } returns "channel456"
        every { intent.getStringExtra("project_id") } returns "project789"

        val result = DeepLinkExtractor.extractFcmNotificationData(intent)
        assertNull(result)
    }

    @Test
    fun `extractFcmNotificationData should return null for missing message_id`() {
        val intent = mockk<Intent>()
        every { intent.getStringExtra("notification_type") } returns "mention"
        every { intent.getStringExtra("message_id") } returns null
        every { intent.getStringExtra("channel_id") } returns "channel456"
        every { intent.getStringExtra("project_id") } returns "project789"

        val result = DeepLinkExtractor.extractFcmNotificationData(intent)
        assertNull(result)
    }

    @Test
    fun `extractFcmNotificationData should return null for missing channel_id`() {
        val intent = mockk<Intent>()
        every { intent.getStringExtra("notification_type") } returns "mention"
        every { intent.getStringExtra("message_id") } returns "msg123"
        every { intent.getStringExtra("channel_id") } returns null
        every { intent.getStringExtra("project_id") } returns "project789"

        val result = DeepLinkExtractor.extractFcmNotificationData(intent)
        assertNull(result)
    }

    // ================================
    // 결과 포함 추출 테스트
    // ================================

    @Test
    fun `extractInviteCodeWithResult should return Success for valid URI`() {
        val uri = Uri.parse("https://tpppk.app/invite/ABC123")
        val result = DeepLinkExtractor.extractInviteCodeWithResult(uri)

        assertTrue(result is DeepLinkExtractor.ExtractionResult.Success)
        assertEquals("ABC123", (result as DeepLinkExtractor.ExtractionResult.Success).data)
    }

    @Test
    fun `extractInviteCodeWithResult should return Failure for invalid URI`() {
        val uri = Uri.parse("http://example.com/invalid")
        val result = DeepLinkExtractor.extractInviteCodeWithResult(uri)

        assertTrue(result is DeepLinkExtractor.ExtractionResult.Failure)
        assertTrue((result as DeepLinkExtractor.ExtractionResult.Failure).reason.contains("Unsupported URI format"))
    }

    @Test
    fun `extractChannelIdWithResult should return Success for valid URI`() {
        val uri = Uri.parse("app://channel/channel123")
        val result = DeepLinkExtractor.extractChannelIdWithResult(uri)

        assertTrue(result is DeepLinkExtractor.ExtractionResult.Success)
        assertEquals("channel123", (result as DeepLinkExtractor.ExtractionResult.Success).data)
    }

    @Test
    fun `extractChannelIdWithResult should return Failure for invalid URI`() {
        val uri = Uri.parse("http://example.com/invalid")
        val result = DeepLinkExtractor.extractChannelIdWithResult(uri)

        assertTrue(result is DeepLinkExtractor.ExtractionResult.Failure)
        assertTrue((result as DeepLinkExtractor.ExtractionResult.Failure).reason.contains("No channel ID found"))
    }

    // ================================
    // 딥링크 생성 테스트
    // ================================

    @Test
    fun `createChannelDeepLink should create app scheme URI`() {
        val uri = DeepLinkExtractor.createChannelDeepLink("channel123")
        assertEquals("app://channel/channel123", uri.toString())
    }

    @Test
    fun `createChannelDeepLink should create custom scheme URI`() {
        val uri = DeepLinkExtractor.createChannelDeepLink("channel123", "tpppk")
        assertEquals("tpppk://channel/channel123", uri.toString())
    }

    @Test
    fun `createInviteDeepLink should create custom scheme URI by default`() {
        val uri = DeepLinkExtractor.createInviteDeepLink("ABC123")
        assertEquals("tpppk://invite/ABC123", uri.toString())
    }

    @Test
    fun `createInviteDeepLink should create HTTPS URI when requested`() {
        val uri = DeepLinkExtractor.createInviteDeepLink("ABC123", useHttps = true)
        assertEquals("https://tpppk.app/invite/ABC123", uri.toString())
    }

    // ================================
    // 검증 유틸리티 테스트
    // ================================

    @Test
    fun `isValidDeepLink should return true for valid invite link`() {
        val uri = Uri.parse("https://tpppk.app/invite/ABC123")
        assertTrue(DeepLinkExtractor.isValidDeepLink(uri))
    }

    @Test
    fun `isValidDeepLink should return true for valid channel link`() {
        val uri = Uri.parse("app://channel/channel123")
        assertTrue(DeepLinkExtractor.isValidDeepLink(uri))
    }

    @Test
    fun `isValidDeepLink should return false for invalid link`() {
        val uri = Uri.parse("http://example.com/invalid")
        assertFalse(DeepLinkExtractor.isValidDeepLink(uri))
    }

    @Test
    fun `getDeepLinkType should return INVITE for invite link`() {
        val uri = Uri.parse("https://tpppk.app/invite/ABC123")
        assertEquals(DeepLinkExtractor.DeepLinkType.INVITE, DeepLinkExtractor.getDeepLinkType(uri))
    }

    @Test
    fun `getDeepLinkType should return CHANNEL for channel link`() {
        val uri = Uri.parse("app://channel/channel123")
        assertEquals(DeepLinkExtractor.DeepLinkType.CHANNEL, DeepLinkExtractor.getDeepLinkType(uri))
    }

    @Test
    fun `getDeepLinkType should return UNSUPPORTED for invalid link`() {
        val uri = Uri.parse("http://example.com/invalid")
        assertEquals(
            DeepLinkExtractor.DeepLinkType.UNSUPPORTED,
            DeepLinkExtractor.getDeepLinkType(uri)
        )
    }

    // ================================
    // 에지 케이스 테스트
    // ================================

    @Test
    fun `extractInviteCode should handle empty path segments`() {
        val uri = Uri.parse("https://tpppk.app/invite/")
        val result = DeepLinkExtractor.extractInviteCode(uri)
        assertNull(result)
    }

    @Test
    fun `extractChannelId should handle empty channel ID`() {
        val uri = Uri.parse("app://channel/")
        val result = DeepLinkExtractor.extractChannelId(uri)
        assertNull(result)
    }

    @Test
    fun `extractChannelIdWithResult should handle blank channel ID`() {
        val uri = Uri.parse("app://channel/   ")
        val result = DeepLinkExtractor.extractChannelIdWithResult(uri)

        assertTrue(result is DeepLinkExtractor.ExtractionResult.Failure)
    }

    @Test
    fun `extractFcmNotificationDataWithResult should provide detailed failure reasons`() {
        val intent = mockk<Intent>()
        every { intent.getStringExtra("notification_type") } returns null
        every { intent.getStringExtra("message_id") } returns "msg123"
        every { intent.getStringExtra("channel_id") } returns "channel456"
        every { intent.getStringExtra("project_id") } returns "project789"

        val result = DeepLinkExtractor.extractFcmNotificationDataWithResult(intent)

        assertTrue(result is DeepLinkExtractor.ExtractionResult.Failure)
        assertEquals(
            "notification_type is null or blank",
            (result as DeepLinkExtractor.ExtractionResult.Failure).reason
        )
    }

    @Test
    fun `extractFcmNotificationDataWithResult should handle exception`() {
        val intent = mockk<Intent>()
        every { intent.getStringExtra(any()) } throws RuntimeException("Mock exception")

        val result = DeepLinkExtractor.extractFcmNotificationDataWithResult(intent)

        assertTrue(result is DeepLinkExtractor.ExtractionResult.Failure)
        assertTrue((result as DeepLinkExtractor.ExtractionResult.Failure).reason.contains("Exception during extraction"))
    }
}