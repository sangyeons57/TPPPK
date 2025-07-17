package com.example.feature_home.component

import android.annotation.SuppressLint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Group
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.core_ui.components.fab.ExtendableFab
import com.example.core_ui.components.fab.FabMenuItem
import com.example.core_ui.components.fab.FabLabelStyle
import com.example.feature_home.viewmodel.TopSection

@Composable
fun MainHomeFloatingButton(
    currentSection: TopSection,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAddProject: () -> Unit,
    onAddDm: () -> Unit,
    onAddProjectElement: () -> Unit,
    modifier: Modifier = Modifier
) {

    // 현재 선택된 섹션에 따라 메뉴 아이템 목록 생성
    val menuItems = remember(currentSection) {
        buildList {
            // 프로젝트 추가 항목 (공통)
            add(
                FabMenuItem(
                icon = Icons.Default.Group,
                text = "프로젝트 추가",
                contentDescription = "새 프로젝트 추가",
                onClick = {
                    onExpandedChange(false)
                    onAddProject()
                }
            ))

            // DM 추가 항목 (공통)
            add(
                FabMenuItem(
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
                add(
                    FabMenuItem(
                    icon = Icons.Default.Edit,
                    text = "프로젝트 요소 추가",
                    contentDescription = "프로젝트 요소 추가",
                    onClick = {
                        onExpandedChange(false)
                        onAddProjectElement()
                    }
                ))
            }
        }
    }

    ExtendableFab(
        menuItems = menuItems,
        isExpanded = isExpanded,
        onExpandedChange = onExpandedChange,
        labelStyle = FabLabelStyle.SURFACE,
        modifier = modifier
    )
}