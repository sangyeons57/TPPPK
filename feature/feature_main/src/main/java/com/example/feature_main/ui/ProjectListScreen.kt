package com.example.feature_main.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme

/**
 * 사용자 프로필 아이템을 표시하는 컴포저블
 * 프로젝트 목록 상단에 표시되며 클릭하면 DM 목록으로 전환됨
 */
@Composable
fun UserProfileItem(
    isSelected: Boolean,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    userInitial: String = "U",
    profileImageUrl: String? = null
) {
    val size = 48.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(size + 16.dp)
            .clickable(onClick = onProfileClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 선택 상태 표시 바
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (isSelected) size / 1.5f else size / 3f)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp)
                    )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 프로필 아이콘 (원형)
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary), // 프로젝트와 구분되는 색상
                contentAlignment = Alignment.Center
            ) {
                // TODO: profileImageUrl을 사용하여 Coil 등으로 이미지 로드 구현
                Text(
                    text = userInitial,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/**
 * 프로젝트 아이템을 표시하는 컴포저블 (Discord 서버 아이콘 스타일)
 */
@Composable
fun ProjectListItem(
    project: ProjectUiModel,
    isSelected: Boolean, // 선택 상태를 위한 파라미터 추가
    onProjectClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val size = 48.dp // Discord 서버 아이콘 크기와 유사
    
    // 로그 추가
    Log.d("ProjectListItem", "Displaying project: id=${project.id}, name=${project.name}")

    Box(
        modifier = modifier
            .fillMaxWidth() // 전체 너비를 차지하도록 해서 클릭 영역 확보
            .height(size + 16.dp) // 아이콘 크기 + 상하 패딩
            .clickable { onProjectClick(project.id) }
            .padding(vertical = 8.dp), // 아이템 간 상하 간격
        contentAlignment = Alignment.CenterStart // 아이콘을 왼쪽에 배치 (실제 아이콘은 Row 내부에서 정렬)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 선택 상태 표시 바 (Discord 스타일)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (isSelected) size / 1.5f else size / 3f) // 선택 시 더 길게, 비율 조정
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                        shape = RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp) // 약간 둥글게
                    )
            )

            Spacer(modifier = Modifier.width(8.dp)) // 선택 바와 아이콘 사이 간격

            // 프로젝트 아이콘 (원형)
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant), // 기본 배경색
                contentAlignment = Alignment.Center
            ) {
                // TODO: project.imageUrl을 사용하여 Coil 등으로 이미지 로드 구현
                // 현재는 이름 첫 글자만 표시
                Text(
                    text = project.name.firstOrNull()?.uppercase() ?: "P",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 프로젝트 목록을 표시하는 화면입니다. (Discord 서버 목록 스타일)
 * 최상단에는 사용자 프로필 표시, 그 아래로 프로젝트 목록 표시
 *
 * @param projects 표시할 프로젝트 목록
 * @param selectedProjectId 현재 선택된 프로젝트 ID
 * @param isDmSelected DM 섹션이 선택되었는지 여부
 * @param onProfileClick 프로필 아이템 클릭 시 호출될 콜백
 * @param onProjectClick 프로젝트 아이템 클릭 시 호출될 콜백 (Project ID 전달)
 * @param modifier Modifier
 */
@Composable
fun ProjectListScreen(
    modifier: Modifier = Modifier,
    projects: List<ProjectUiModel>,
    selectedProjectId: String?, // 현재 선택된 프로젝트 ID
    isDmSelected: Boolean = false, // DM 섹션이 선택되었는지 여부
    onProfileClick: () -> Unit, // 프로필 클릭 핸들러 추가
    onProjectClick: (projectId: String) -> Unit,
    userInitial: String = "U", // 사용자 이니셜
    profileImageUrl: String? = null // 프로필 이미지 URL
) {
    LazyColumn(
        modifier = modifier
            .fillMaxHeight() // 전체 높이
            .width(72.dp) // Discord 서버 목록 너비와 유사하게 고정
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)), // 어두운 배경
        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp), // 아이템 전체의 상하 패딩
        horizontalAlignment = Alignment.CenterHorizontally, // 내부 아이템들을 중앙 정렬 시도
        verticalArrangement = Arrangement.spacedBy(4.dp) // 아이템 간 간격
    ) {
        // 1. 사용자 프로필 아이템
        item(key = "user_profile") {
            UserProfileItem(
                isSelected = isDmSelected, // DM 섹션이 선택되었는지 여부
                onProfileClick = onProfileClick,
                userInitial = userInitial,
                profileImageUrl = profileImageUrl,
                modifier = Modifier.padding(horizontal = (72.dp - 48.dp - 4.dp - 8.dp) / 2)
            )
            
            // 구분선 추가
            HorizontalDivider(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        }

        // 2. 프로젝트 목록
        Log.d("ProjectListScreen", "project: $projects")
        items(projects, key = { it.id }) { project ->
            ProjectListItem(
                project = project,
                isSelected = project.id == selectedProjectId && !isDmSelected, // DM이 선택되었으면 어떤 프로젝트도 선택되지 않은 상태
                onProjectClick = onProjectClick,
                modifier = Modifier.padding(horizontal = (72.dp - 48.dp - 4.dp - 8.dp) / 2) // 아이콘이 중앙에 오도록 좌우 패딩 계산 (72(전체) - 48(아이콘) - 4(바) - 8(간격)) / 2
            )
        }
    }
}

// --- Previews Start ---

@Preview(showBackground = true, name = "UserProfileItem - Default")
@Composable
fun UserProfileItemPreview_Default() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Row(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))) {
            UserProfileItem(
                isSelected = false,
                onProfileClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "UserProfileItem - Selected")
@Composable
fun UserProfileItemPreview_Selected() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Row(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))) {
            UserProfileItem(
                isSelected = true,
                onProfileClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "ProjectListItem - Default")
@Composable
fun ProjectListItemPreview_Default() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Row(modifier=Modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))) {
             ProjectListItem(
                project = ProjectUiModel(id = "1", name = "Alpha", description = "A", imageUrl = null),
                isSelected = false,
                onProjectClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "ProjectListItem - Selected")
@Composable
fun ProjectListItemPreview_Selected() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Row(modifier=Modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))) {
            ProjectListItem(
                project = ProjectUiModel(id = "2", name = "Bravo", description = "B", imageUrl = null),
                isSelected = true,
                onProjectClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "ProjectListScreen - Empty")
@Composable
fun ProjectListScreenPreview_Empty() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ProjectListScreen(
            projects = emptyList(),
            selectedProjectId = null,
            isDmSelected = false,
            onProfileClick = {},
            onProjectClick = {}
        )
    }
}

@Preview(showBackground = true, name = "ProjectListScreen - With Items")
@Composable
fun ProjectListScreenPreview_WithItems() {
    val sampleProjects = listOf(
        ProjectUiModel(id = "1", name = "녹색 프로젝트", description = "환경 보호 캠페인", imageUrl = null),
        ProjectUiModel(id = "2", name = "Alpha App", description = "새로운 모바일 앱 출시 준비", imageUrl = null),
        ProjectUiModel(id = "3", name = "커뮤니티 정원", description = "지역 사회를 위한 정원 조성", imageUrl = null),
        ProjectUiModel(id = "4", name = "스터디 그룹", description = "React 스터디", imageUrl = null),
        ProjectUiModel(id = "5", name = "개인 작업실", description = "사이드 프로젝트 진행", imageUrl = null)
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ProjectListScreen(
            projects = sampleProjects,
            selectedProjectId = "2", // "Alpha App" 선택된 상태로
            isDmSelected = false,
            onProfileClick = {},
            onProjectClick = {}
        )
    }
}

@Preview(showBackground = true, name = "ProjectListScreen - DM Selected")
@Composable
fun ProjectListScreenPreview_DmSelected() {
    val sampleProjects = listOf(
        ProjectUiModel(id = "1", name = "녹색 프로젝트", description = "환경 보호 캠페인", imageUrl = null),
        ProjectUiModel(id = "2", name = "Alpha App", description = "새로운 모바일 앱 출시 준비", imageUrl = null),
        ProjectUiModel(id = "3", name = "커뮤니티 정원", description = "지역 사회를 위한 정원 조성", imageUrl = null)
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ProjectListScreen(
            projects = sampleProjects,
            selectedProjectId = "2",
            isDmSelected = true, // DM이 선택된 상태
            onProfileClick = {},
            onProjectClick = {}
        )
    }
}

// --- Previews End --- 