package com.example.core_ui.components.fab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * 확장 가능한 FAB 메뉴 아이템 데이터 클래스
 * 
 * @param icon 메뉴 아이템 아이콘
 * @param text 메뉴 아이템 텍스트
 * @param contentDescription 접근성을 위한 설명
 * @param onClick 클릭 이벤트 핸들러
 */
data class FabMenuItem(
    val icon: ImageVector,
    val text: String,
    val contentDescription: String,
    val onClick: () -> Unit
)

/**
 * 확장 가능한 FloatingActionButton 메뉴 컴포넌트
 * 
 * @param menuItems 메뉴 아이템 리스트
 * @param isExpanded 메뉴가 확장되었는지 여부
 * @param onExpandedChange 메뉴 확장 상태 변경 콜백
 * @param modifier Modifier
 * @param mainFabIcon 메인 FAB 아이콘 (기본값: Add)
 * @param labelStyle 라벨 스타일 (SURFACE 또는 CARD)
 */
@Composable
fun ExtendableFab(
    menuItems: List<FabMenuItem>,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    mainFabIcon: ImageVector = Icons.Default.Add,
    labelStyle: FabLabelStyle = FabLabelStyle.SURFACE
) {
    // 메인 FAB 회전 애니메이션
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "FAB Rotation Animation"
    )
    
    // LocalDensity를 미리 가져와서 변수로 저장
    val density = LocalDensity.current
    // 애니메이션에 사용될 픽셀 값을 미리 계산
    val offsetPx = with(density) { 100.dp.roundToPx() }
        
    // 메뉴 아이템 및 메인 FAB
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom,
        modifier = modifier
    ) {
        // 메뉴 아이템들 (확장 시에만 표시)
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(300)) + slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                initialOffsetY = { offsetPx }
            ),
            exit = fadeOut(tween(300)) + slideOutVertically(
                animationSpec = tween(300),
                targetOffsetY = { offsetPx }
            )
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                menuItems.forEach { item ->
                    FabMenuItemRow(
                        item = item,
                        labelStyle = labelStyle,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        // 메인 FAB
        FloatingActionButton(
            onClick = { onExpandedChange(!isExpanded) },
            modifier = Modifier.semantics {
                contentDescription = if (isExpanded) "메뉴 닫기" else "메뉴 열기"
            }
        ) {
            // 메뉴 확장 상태에 따라 아이콘 회전
            Icon(
                imageVector = mainFabIcon,
                contentDescription = null,
                modifier = Modifier
                    .padding(4.dp)
                    .alpha(1f)
                    .rotate(rotationAngle)
            )
        }
    }
}

/**
 * 메뉴 아이템 행 (아이콘 + 텍스트)
 * 
 * @param item 메뉴 아이템 데이터
 * @param labelStyle 라벨 스타일
 * @param modifier Modifier
 */
@Composable
fun FabMenuItemRow(
    item: FabMenuItem,
    labelStyle: FabLabelStyle = FabLabelStyle.SURFACE,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = modifier
    ) {
        // 아이템 텍스트 (레이블)
        when (labelStyle) {
            FabLabelStyle.SURFACE -> {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .semantics { contentDescription = item.contentDescription }
                        .clickable(onClick = item.onClick)
                ) {
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            FabLabelStyle.CARD -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .semantics { contentDescription = item.contentDescription }
                        .clickable(onClick = item.onClick)
                ) {
                    Text(
                        text = item.text,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // 아이템 아이콘 (미니 FAB)
        SmallFloatingActionButton(
            onClick = item.onClick,
            modifier = Modifier.semantics { contentDescription = item.contentDescription }
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * FAB 라벨 스타일 enum
 */
enum class FabLabelStyle {
    SURFACE,  // Material 3 Surface 사용 (feature_home 스타일)
    CARD      // Material 3 Card 사용 (feature_tasks 스타일)
}