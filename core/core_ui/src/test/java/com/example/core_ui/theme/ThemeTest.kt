package com.example.core_ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Theme 테스트 클래스
 *
 * 테마가 MaterialTheme에 올바르게 적용되었는지 확인하는 테스트입니다.
 */
class ThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `default color scheme should have correct colors`() {
        // Given: MaterialTheme을 사용하는 내부 컴포저블
        @Composable
        fun TestContent() {
            TeamnovaPersonalProjectProjectingKotlinTheme {
                // 컴포저블 내에서 테마 컬러를 캡처
                capturedColorScheme = MaterialTheme.colorScheme
            }
        }

        // Capture용 변수
        var capturedColorScheme = MaterialTheme.colorScheme

        // When: 컴포저블 실행
        composeTestRule.setContent {
            TestContent()
        }

        // Then: 테마 컬러가 올바르게 적용되었는지 확인
        with(capturedColorScheme) {
            // Primary Colors 확인
            assertEquals(AppPrimary, primary)
            assertEquals(AppOnPrimary, onPrimary)
            assertEquals(AppPrimaryContainer, primaryContainer)
            assertEquals(AppOnPrimaryContainer, onPrimaryContainer)

            // Secondary Colors 확인
            assertEquals(AppSecondary, secondary)
            assertEquals(AppOnSecondary, onSecondary)
            assertEquals(AppSecondaryContainer, secondaryContainer)
            assertEquals(AppOnSecondaryContainer, onSecondaryContainer)

            // Background & Surface Colors 확인
            assertEquals(AppBackground, background)
            assertEquals(AppOnBackground, onBackground)
            assertEquals(AppSurface, surface)
            assertEquals(AppOnSurface, onSurface)
            assertEquals(AppSurfaceVariant, surfaceVariant)
            assertEquals(AppOnSurfaceVariant, onSurfaceVariant)

            // Error Colors 확인
            assertEquals(AppError, error)
            assertEquals(AppOnError, onError)
            assertEquals(AppErrorContainer, errorContainer)
            assertEquals(AppOnErrorContainer, onErrorContainer)
        }
    }

    @Test
    fun `theme should use correct typography`() {
        // Given: MaterialTheme을 사용하는 내부 컴포저블
        @Composable
        fun TestContent() {
            TeamnovaPersonalProjectProjectingKotlinTheme {
                // 컴포저블 내에서 테마 타이포그래피를 캡처
                capturedTypography = MaterialTheme.typography
            }
        }

        // Capture용 변수
        var capturedTypography = MaterialTheme.typography

        // When: 컴포저블 실행
        composeTestRule.setContent {
            TestContent()
        }

        // Then: 테마 타이포그래피가 올바르게 적용되었는지 확인
        assertEquals(Typography, capturedTypography)
    }

    @Test
    fun `theme should use correct shapes`() {
        // Given: MaterialTheme을 사용하는 내부 컴포저블
        @Composable
        fun TestContent() {
            TeamnovaPersonalProjectProjectingKotlinTheme {
                // 컴포저블 내에서 테마 모양을 캡처
                capturedShapes = MaterialTheme.shapes
            }
        }

        // Capture용 변수
        var capturedShapes = MaterialTheme.shapes

        // When: 컴포저블 실행
        composeTestRule.setContent {
            TestContent()
        }

        // Then: 테마 모양이 올바르게 적용되었는지 확인
        assertEquals(Shapes, capturedShapes)
    }
} 