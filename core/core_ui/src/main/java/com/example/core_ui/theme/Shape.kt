package com.example.core_ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp), // 예: 버튼, 카드 등에 사용될 중간 크기 모서리
    large = RoundedCornerShape(16.dp) // 예: BottomSheet 등에 사용될 큰 크기 모서리
)