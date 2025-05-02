package com.example.core_ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Dimens 테스트 클래스
 *
 * 앱에서 사용되는 치수 상수들이 올바르게 정의되어 있는지 확인하는 테스트입니다.
 */
class DimensTest {

    @Test
    fun `padding values should be correctly defined`() {
        // 일반 간격 검증
        assertEquals(2.dp, Dimens.paddingExtraSmall)
        assertEquals(4.dp, Dimens.paddingSmall)
        assertEquals(8.dp, Dimens.paddingMedium)
        assertEquals(12.dp, Dimens.paddingLarge)
        assertEquals(16.dp, Dimens.paddingXLarge)
        assertEquals(24.dp, Dimens.paddingXXLarge)
        assertEquals(32.dp, Dimens.paddingXXXLarge)
    }

    @Test
    fun `component height values should be correctly defined`() {
        // 컴포넌트 높이 검증
        assertEquals(32.dp, Dimens.buttonHeightSmall)
        assertEquals(36.dp, Dimens.buttonHeightMedium)
        assertEquals(48.dp, Dimens.buttonHeightLarge)
        assertEquals(48.dp, Dimens.listItemHeightSmall)
        assertEquals(56.dp, Dimens.listItemHeightMedium)
        assertEquals(72.dp, Dimens.listItemHeightLarge)
    }

    @Test
    fun `icon size values should be correctly defined`() {
        // 아이콘 크기 검증
        assertEquals(12.dp, Dimens.iconSizeExtraSmall)
        assertEquals(16.dp, Dimens.iconSizeSmall)
        assertEquals(24.dp, Dimens.iconSizeMedium)
        assertEquals(32.dp, Dimens.iconSizeLarge)
        assertEquals(40.dp, Dimens.iconSizeXLarge)
        assertEquals(48.dp, Dimens.iconSizeXXLarge)
    }

    @Test
    fun `corner radius values should be correctly defined`() {
        // 둥근 모서리 검증
        assertEquals(4.dp, Dimens.cornerRadiusSmall)
        assertEquals(8.dp, Dimens.cornerRadiusMedium)
        assertEquals(12.dp, Dimens.cornerRadiusLarge)
        assertEquals(16.dp, Dimens.cornerRadiusXLarge)
    }

    @Test
    fun `line thickness values should be correctly defined`() {
        // 선 두께 검증
        assertEquals(1.dp, Dimens.borderWidth)
        assertEquals(1.dp, Dimens.dividerHeight)
        assertEquals(2.dp, Dimens.strokeWidth)
        assertEquals(3.dp, Dimens.progressStrokeWidth)
    }

    @Test
    fun `elevation values should be correctly defined`() {
        // 엘리베이션 검증
        assertEquals(2.dp, Dimens.elevationSmall)
        assertEquals(4.dp, Dimens.elevationMedium)
        assertEquals(8.dp, Dimens.elevationLarge)
    }

    @Test
    fun `section size values should be correctly defined`() {
        // 섹션 크기 검증
        assertEquals(100.dp, Dimens.sectionMinHeight)
        assertEquals(200.dp, Dimens.sectionMediumHeight)
        assertEquals(300.dp, Dimens.sectionLargeHeight)
    }

    @Test
    fun `marker and indicator values should be correctly defined`() {
        // 마커 및 인디케이터 검증
        assertEquals(6.dp, Dimens.indicatorSize)
        assertEquals(4.dp, Dimens.markerSizeSmall)
        assertEquals(8.dp, Dimens.markerSizeMedium)
        assertEquals(12.dp, Dimens.markerSizeLarge)
    }
    
    @Test
    fun `padding values should follow logical progression`() {
        // 간격 값들이 논리적 순서를 따르는지 확인
        assert(Dimens.paddingExtraSmall < Dimens.paddingSmall)
        assert(Dimens.paddingSmall < Dimens.paddingMedium)
        assert(Dimens.paddingMedium < Dimens.paddingLarge)
        assert(Dimens.paddingLarge < Dimens.paddingXLarge)
        assert(Dimens.paddingXLarge < Dimens.paddingXXLarge)
        assert(Dimens.paddingXXLarge < Dimens.paddingXXXLarge)
    }
    
    @Test
    fun `button heights should follow logical progression`() {
        // 버튼 높이 값들이 논리적 순서를 따르는지 확인
        assert(Dimens.buttonHeightSmall < Dimens.buttonHeightMedium)
        assert(Dimens.buttonHeightMedium < Dimens.buttonHeightLarge)
    }
    
    @Test
    fun `icon sizes should follow logical progression`() {
        // 아이콘 크기 값들이 논리적 순서를 따르는지 확인
        assert(Dimens.iconSizeExtraSmall < Dimens.iconSizeSmall)
        assert(Dimens.iconSizeSmall < Dimens.iconSizeMedium)
        assert(Dimens.iconSizeMedium < Dimens.iconSizeLarge)
        assert(Dimens.iconSizeLarge < Dimens.iconSizeXLarge)
        assert(Dimens.iconSizeXLarge < Dimens.iconSizeXXLarge)
    }
} 