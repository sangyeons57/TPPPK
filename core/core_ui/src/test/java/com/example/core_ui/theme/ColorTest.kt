package com.example.core_ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Color 테스트 클래스
 *
 * 테마의 주요 색상값이 올바르게 정의되어 있는지 확인하는 테스트입니다.
 */
class ColorTest {

    @Test
    fun `verify primary colors`() {
        // Primary Colors 정의 확인
        assertEquals(Color(0xFFB4C3DC), AppPrimary)
        assertEquals(Color(0xFF1E3246), AppOnPrimary)
        assertEquals(Color(0xFF284664), AppPrimaryContainer)
        assertEquals(Color(0xFFDAE1F0), AppOnPrimaryContainer)
    }

    @Test
    fun `verify secondary colors`() {
        // Secondary Colors 정의 확인
        assertEquals(Color(0xFFA1CED6), AppSecondary)
        assertEquals(Color(0xFF1A343A), AppOnSecondary)
        assertEquals(Color(0xFF2E4A50), AppSecondaryContainer)
        assertEquals(Color(0xFFBDEAF1), AppOnSecondaryContainer)
    }

    @Test
    fun `verify background and surface colors`() {
        // Background & Surface Colors 정의 확인
        assertEquals(Color(0xFF141A24), AppBackground)
        assertEquals(Color(0xFFE1E2E9), AppOnBackground)
        assertEquals(Color(0xFF1C232F), AppSurface)
        assertEquals(Color(0xFFE1E2E9), AppOnSurface)
        assertEquals(Color(0xFF404853), AppSurfaceVariant)
        assertEquals(Color(0xFFC0C7D2), AppOnSurfaceVariant)
    }

    @Test
    fun `verify error colors`() {
        // Error Colors 정의 확인
        assertEquals(Color(0xFFF2B8B5), AppError)
        assertEquals(Color(0xFF601410), AppOnError)
        assertEquals(Color(0xFF8C1D18), AppErrorContainer)
        assertEquals(Color(0xFFF9DEDC), AppOnErrorContainer)
    }

    @Test
    fun `verify schedule colors`() {
        // 일정 색상 확인
        assertEquals(Color(0xFFFF7675), ScheduleColor1)
        assertEquals(Color(0xFF74B9FF), ScheduleColor2)
        assertEquals(Color(0xFF55EFC4), ScheduleColor3)
        assertEquals(Color(0xFFFECE61), ScheduleColor4)
        assertEquals(Color(0xFFA29BFE), ScheduleColor5)
        assertEquals(Color(0xFFE84393), ScheduleColor6)
        assertEquals(Color(0xFFE17055), ScheduleColor7)
    }

    @Test
    fun `verify high contrast schedule colors`() {
        // 고대비 일정 색상 확인
        assertEquals(Color(0xFFFF0000), ScheduleHighContrastColor1)
        assertEquals(Color(0xFF0000FF), ScheduleHighContrastColor2)
        assertEquals(Color(0xFF00FF00), ScheduleHighContrastColor3)
        assertEquals(Color(0xFFFFFF00), ScheduleHighContrastColor4)
        assertEquals(Color(0xFFFFFFFF), ScheduleHighContrastColor5)
        assertEquals(Color(0xFFFF00FF), ScheduleHighContrastColor6)
        assertEquals(Color(0xFFFF8000), ScheduleHighContrastColor7)
    }
    
    @Test
    fun `verify all app colors are defined`() {
        // 필수 테마 색상이 null이 아닌지 확인
        assertNotNull(AppPrimary, "AppPrimary should be defined")
        assertNotNull(AppOnPrimary, "AppOnPrimary should be defined")
        assertNotNull(AppPrimaryContainer, "AppPrimaryContainer should be defined")
        assertNotNull(AppOnPrimaryContainer, "AppOnPrimaryContainer should be defined")
        assertNotNull(AppSecondary, "AppSecondary should be defined")
        assertNotNull(AppOnSecondary, "AppOnSecondary should be defined")
        assertNotNull(AppSecondaryContainer, "AppSecondaryContainer should be defined")
        assertNotNull(AppOnSecondaryContainer, "AppOnSecondaryContainer should be defined")
        assertNotNull(AppBackground, "AppBackground should be defined")
        assertNotNull(AppOnBackground, "AppOnBackground should be defined")
        assertNotNull(AppSurface, "AppSurface should be defined")
        assertNotNull(AppOnSurface, "AppOnSurface should be defined")
        assertNotNull(AppError, "AppError should be defined")
        assertNotNull(AppOnError, "AppOnError should be defined")
    }
    
    // 헬퍼 메서드
    private fun assertNotNull(color: Color, message: String) {
        // Color 객체는 null이 될 수 없지만 논리적으로 정의되었는지 확인
        assert(color != Color.Unspecified) { message }
    }
} 