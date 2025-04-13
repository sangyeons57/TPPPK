package com.example.teamnovapersonalprojectprojectingkotlin.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 앱의 기본 컬러 스킴
private val defaultColorScheme = lightColorScheme(
    // Primary Colors (주요 브랜드 색상)
    primary = AppPrimary,                    // 앱의 주요 브랜드 색상, 주요 버튼과 액션에 사용
    onPrimary = AppOnPrimary,               // primary 색상 위의 텍스트/아이콘 색상
    primaryContainer = AppPrimaryContainer,   // primary 색상의 컨테이너 배경색 (카드, 칩 등)
    onPrimaryContainer = AppOnPrimaryContainer, // primaryContainer 위의 텍스트/아이콘 색상
    
    // Secondary Colors (보조 색상)
    secondary = AppSecondary,                // 보조 강조 색상, 부가적인 액션이나 정보에 사용
    onSecondary = AppOnSecondary,           // secondary 색상 위의 텍스트/아이콘 색상
    secondaryContainer = AppSecondaryContainer, // secondary 색상의 컨테이너 배경색
    onSecondaryContainer = AppOnSecondaryContainer, // secondaryContainer 위의 텍스트/아이콘 색상

    // Tertiary Colors
    tertiary = AppTertiary,                  // 세 번째 강조 색상, 균형과 시각적 관심을 위해 사용
    onTertiary = AppOnTertiary,             // tertiary 색상 위의 텍스트/아이콘 색상
    tertiaryContainer = AppTertiaryContainer, // tertiary 색상의 컨테이너 배경색
    onTertiaryContainer = AppOnTertiaryContainer, // tertiaryContainer 위의 텍스트/아이콘 색상

    // Background & Surface Colors (배경 및 표면 색상)
    background = AppBackground,              // 앱의 기본 배경색
    onBackground = AppOnBackground,          // background 위의 텍스트/아이콘 색상
    
    // Surface Colors
    surface = AppSurface,                    // 카드, 시트, 메뉴 등의 표면 색상
    onSurface = AppOnSurface,               // surface 위의 텍스트/아이콘 색상
    surfaceVariant = AppSurfaceVariant,     // surface의 변형 색상, 구분이 필요한 표면에 사용
    onSurfaceVariant = AppOnSurfaceVariant, // surfaceVariant 위의 텍스트/아이콘 색상
    surfaceTint = AppSurfaceTint,           // surface에 적용되는 틴트 색상
    
    // Surface Container Colors
    surfaceContainer = AppSurfaceContainer,  // 기본 컨테이너의 배경색
    surfaceContainerHigh = AppSurfaceContainerHigh, // 높은 강조도의 컨테이너 배경색
    surfaceContainerHighest = AppSurfaceContainerHighest, // 최고 강조도의 컨테이너 배경색
    surfaceContainerLow = AppSurfaceContainerLow, // 낮은 강조도의 컨테이너 배경색
    surfaceContainerLowest = AppSurfaceContainerLowest, // 최저 강조도의 컨테이너 배경색
    
    // Outline & Error Colors (외곽선 및 오류 색상)
    outline = AppOutline,                    // 테두리와 구분선에 사용되는 색상
    outlineVariant = AppOutlineVariant,     // 덜 강조된 테두리에 사용되는 색상
    error = AppError,                        // 오류 상태를 나타내는 색상
    onError = AppOnError,                    // error 색상 위의 텍스트/아이콘 색상
    errorContainer = AppErrorContainer,      // error 관련 컨테이너의 배경색
    onErrorContainer = AppOnErrorContainer,  // errorContainer 위의 텍스트/아이콘 색상
    
    // Inverse Colors
    inversePrimary = AppInversePrimary,     // 반전된 테마에서의 주요 색상
    inverseSurface = AppInverseSurface,     // 반전된 표면 색상
    inverseOnSurface = AppInverseOnSurface, // 반전된 표면 위의 텍스트/아이콘 색상
    
    // Additional Colors
    scrim = AppScrim,                       // 백드롭이나 모달의 반투명 오버레이 색상
)

@Composable
fun TeamnovaPersonalProjectProjectingKotlinTheme ( // 앱 이름에 맞게 함수명 변경 (예: MyAwesomeAppTheme)
    darkTheme: Boolean = isSystemInDarkTheme(), // 시스템 설정 감지
    // 동적 색상 (Android 12+): 필요하다면 true로 설정
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit // 앱의 실제 UI 콘텐츠
) {
    val colorScheme = when {
        // 동적 색상 사용 및 가능한 경우
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // 동적 색상 미사용 또는 불가능한 경우, 직접 정의한 팔레트 사용
        darkTheme -> defaultColorScheme
        else -> defaultColorScheme
    }

    // MaterialTheme 컴포저블로 앱 콘텐츠를 감싸고, 정의한 값들 전달
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // 3단계에서 정의한 Typography
        shapes = Shapes,         // 4단계에서 정의한 Shapes
        content = content        // 앱 UI 콘텐츠 렌더링
    )
}