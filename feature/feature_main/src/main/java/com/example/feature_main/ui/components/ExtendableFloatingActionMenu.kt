package com.example.feature_main.ui.components

import android.annotation.SuppressLint
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.feature_main.viewmodel.TopSection

/**
 * 확장 가능한 FloatingActionMenu 아이템 데이터 클래스
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
 * 확장 가능한 FloatingActionMenu 컴포넌트 (오버레이 제외)
 * 
 * @param currentSection 현재 선택된 탭 (PROJECTS 또는 DMS)
 * @param isExpanded 메뉴가 확장되었는지 여부
 * @param onExpandedChange 메뉴 확장 상태 변경 콜백
 * @param onAddProject 프로젝트 추가 버튼 클릭 핸들러
 * @param onAddDm DM 추가 버튼 클릭 핸들러
 * @param onEditProjectStructure 프로젝트 구조 편집 버튼 클릭 핸들러 (프로젝트 탭에서만 표시)
 * @param modifier Modifier
 */
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ExtendableFloatingActionMenu(
    currentSection: TopSection,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAddProject: () -> Unit,
    onAddDm: () -> Unit,
    onEditProjectStructure: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 현재 선택된 섹션에 따라 메뉴 아이템 목록 생성
    val menuItems = remember(currentSection) {
        buildList {
            // 프로젝트 추가 항목 (공통)
            add(FabMenuItem(
                icon = Icons.Default.Group,
                text = "프로젝트 추가",
                contentDescription = "새 프로젝트 추가",
                onClick = {
                    onExpandedChange(false)
                    onAddProject()
                }
            ))
            
            // DM 추가 항목 (공통)
            add(FabMenuItem(
                icon = Icons.Default.Person,
                text = "DM 추가",
                contentDescription = "새 DM 대화 추가",
                onClick = {
                    onExpandedChange(false)
                    onAddDm()
                }
            ))
            
            // 프로젝트 구조 편집 항목 (프로젝트 탭에서만 표시)
            if (currentSection == TopSection.PROJECTS) {
                add(FabMenuItem(
                    icon = Icons.Default.Edit,
                    text = "프로젝트 구조 편집",
                    contentDescription = "프로젝트 구조 편집",
                    onClick = {
                        onExpandedChange(false)
                        onEditProjectStructure()
                    }
                ))
            }
        }
    }
    
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
                // 메뉴 확장 상태에 따라 + 아이콘 회전
                Icon(
                    imageVector = Icons.Default.Add,
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
 */
@Composable
fun FabMenuItemRow(
    item: FabMenuItem,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = modifier
    ) {
        // 아이템 텍스트 (레이블)
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