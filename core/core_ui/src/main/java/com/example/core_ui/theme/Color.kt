package com.example.core_ui.theme

import androidx.compose.ui.graphics.Color

// 기본 앱 컬러
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// 추가 Material 컬러
val Purple200 = Color(0xFFBB86FC)
val Purple500 = Color(0xFF6200EE)
val Purple700 = Color(0xFF3700B3)
val Teal200 = Color(0xFF03DAC5)
val Teal700 = Color(0xFF018786)

// 추가 테마 컬러
val Grey100 = Color(0xFF262626)
val Grey200 = Color(0xFF363636)
val Grey250 = Color(0xFF4C4C4C)     // 기본 배경색 #4C4C4C
val Grey300 = Color(0xFF8A8A8A)
val ColorAccent = Teal200
val ColorPrimary = Purple500
val ColorPrimaryDark = Purple700
val ColorHint = Color(0xFFAAAAAA)
val ColorRed = Color(0xFFFF0000)
val ColorElementBackground = Grey200




// --- REVISED Dark Theme based on Desaturated Blue ---

// 앱 기본 컬러
val AppPrimary = Color(0xFFB4C3DC)                     // 앱의 주요 브랜드 색상 (차분한 밝은 회색빛 파란색), 주요 버튼과 액션에 사용
val AppOnPrimary = Color(0xFF1E3246)                   // primary 색상 위에 표시되는 텍스트/아이콘 색상 (어두운 회색빛 파란색)
val AppPrimaryContainer = Color(0xFF284664)            // primary 색상의 컨테이너 배경색 (중간톤 회색빛 파란색)
val AppOnPrimaryContainer = Color(0xFFDAE1F0)          // primaryContainer 위에 표시되는 텍스트/아이콘 색상 (매우 밝은 회색빛 파란색)
val AppSecondary = Color(0xFFA1CED6)                   // 보조 강조 색상 (차분한 하늘색/청록색), 부가적인 액션이나 정보에 사용
val AppOnSecondary = Color(0xFF1A343A)                 // secondary 색상 위에 표시되는 텍스트/아이콘 색상 (어두운 회색빛 청록색)
val AppBackground = Color(0xFF141A24)                  // 앱의 기본 배경색 (채도 낮은 매우 어두운 파란색/회색)
val AppOnBackground = Color(0xFFE1E2E9)                // 배경색 위에 표시되는 텍스트/아이콘 색상 (밝은 회색, 거의 흰색)
val AppSurface = Color(0xFF1C232F)                     // 카드, 시트, 메뉴 등의 표면 색상 (배경보다 약간 밝은 어두운 회색빛 파란색)
val AppOnSurface = Color(0xFFE1E2E9)                   // surface 위에 표시되는 텍스트/아이콘 색상 (밝은 회색, 거의 흰색)
val AppSurfaceVariant = Color(0xFF404853)              // surface의 변형 색상, 구분이 필요한 표면에 사용 (중간톤 어두운 회색)
val AppOnSurfaceVariant = Color(0xFFC0C7D2)            // surfaceVariant 위에 표시되는 텍스트/아이콘 색상 (밝은 회색)
val AppOutline = Color(0xFF8A919C)                     // 테두리와 구분선에 사용되는 색상 (중간톤 회색)
val AppOnOutline = Color(0xFFE1E2E9)                   // outline 위에 표시되는 텍스트/아이콘 색상 (비표준 역할, OnSurface 색상 사용 권장)
val AppScrim = Color(0xAA000000)                       // 백드롭이나 모달의 반투명 오버레이 색상 (반투명 검정)
val AppSecondaryContainer = Color(0xFF2E4A50)          // secondary 색상의 컨테이너 배경색 (어두운 회색빛 청록색)
val AppOnSecondaryContainer = Color(0xFFBDEAF1)       // secondaryContainer 위에 표시되는 텍스트/아이콘 색상 (차분한 밝은 하늘색)

// Tertiary Colors (제3 강조 색상) - 회색조에 가깝게 조정
val AppTertiary = Color(0xFFC6C6DC)                   // 세 번째 강조 색상 (밝은 회색빛 보라/파랑)
val AppOnTertiary = Color(0xFF2F2F45)                 // tertiary 위의 콘텐츠 색상 (어두운 회색빛 보라/파랑)
val AppTertiaryContainer = Color(0xFF46465C)           // tertiary 컨테이너 색상 (중간톤 어두운 회색빛 보라/파랑)
val AppOnTertiaryContainer = Color(0xFFE2E1F3)         // tertiaryContainer 위의 콘텐츠 색상 (매우 밝은 회색빛 보라/파랑)

// Surface Colors (표면 색상) - 채도 낮은 파란색/회색 계열
val AppSurfaceTint = Color(0xFFB4C3DC)                // 표면 틴트 색상 (Primary 색상과 동일하게 설정)
val AppSurfaceContainerLowest = Color(0xFF0F151F)     // 최저 강조도 컨테이너 (배경보다 어둡게)
val AppSurfaceContainerLow = Color(0xFF141A24)       // 낮은 강조도 컨테이너 (배경과 동일)
val AppSurfaceContainer = Color(0xFF1C232F)            // 기본 컨테이너 색상 (Surface와 동일)
val AppSurfaceContainerHigh = Color(0xFF262D3A)       // 높은 강조도 컨테이너 (Surface보다 밝게)
val AppSurfaceContainerHighest = Color(0xFF313845)   // 최고 강조도 컨테이너 (가장 밝은 표면)

// Error Colors (오류 색상) - 접근성을 위해 표준 색상 사용 강력 권장! (이전과 동일)
val AppError = Color(0xFFF2B8B5)                      // 오류 상태 색상 (표준 다크모드 연한 빨강 권장)
val AppOnError = Color(0xFF601410)                    // 오류 색상 위의 콘텐츠 (표준 어두운 빨강 권장)
val AppErrorContainer = Color(0xFF8C1D18)              // 오류 컨테이너 색상 (표준 중간톤 빨강 권장)
val AppOnErrorContainer = Color(0xFFF9DEDC)            // 오류 컨테이너 위의 콘텐츠 (표준 매우 연한 빨강 권장)

// Outline Colors (외곽선 색상)
val AppOutlineVariant = Color(0xFF404853)             // 보조 외곽선 색상 (SurfaceVariant와 유사)

// Inverse Colors (반전 색상 - 스낵바 등에 사용)
val AppInversePrimary = Color(0xFF5F8FCA)             // 반전된 주요 색상 (차분한 파란색)
val AppInverseSurface = Color(0xFFE1E2E9)             // 반전된 표면 색상 (밝은 회색)
val AppInverseOnSurface = Color(0xFF2A313C)           // 반전된 표면 위의 콘텐츠 색상 (어두운 회색빛 파란색)

// 일정 색상 - 캘린더 일정 표시용
val ScheduleColor1 = Color(0xFFFF7675) // 연한 빨강 (개인 일정)
val ScheduleColor2 = Color(0xFF74B9FF) // 연한 파랑 (업무 일정)
val ScheduleColor3 = Color(0xFF55EFC4) // 민트 (프로젝트 일정)
val ScheduleColor4 = Color(0xFFFECE61) // 노랑 (미팅 일정)
val ScheduleColor5 = Color(0xFFA29BFE) // 라벤더 (기타 일정)
val ScheduleColor6 = Color(0xFFE84393) // 핑크 (중요 일정)
val ScheduleColor7 = Color(0xFFE17055) // 주황 (마감 일정)

// 고대비 모드용 일정 색상
val ScheduleHighContrastColor1 = Color(0xFFFF0000) // 선명한 빨강 (개인 일정)
val ScheduleHighContrastColor2 = Color(0xFF0000FF) // 선명한 파랑 (업무 일정)
val ScheduleHighContrastColor3 = Color(0xFF00FF00) // 선명한 초록 (프로젝트 일정)
val ScheduleHighContrastColor4 = Color(0xFFFFFF00) // 선명한 노랑 (미팅 일정)
val ScheduleHighContrastColor5 = Color(0xFFFFFFFF) // 흰색 (기타 일정)
val ScheduleHighContrastColor6 = Color(0xFFFF00FF) // 선명한 마젠타 (중요 일정)
val ScheduleHighContrastColor7 = Color(0xFFFF8000) // 선명한 주황 (마감 일정)