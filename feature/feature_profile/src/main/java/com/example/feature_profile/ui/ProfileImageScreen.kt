package com.example.feature_profile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core_ui.picker.FilePicker
import com.example.core_ui.picker.ImagePicker
import com.example.feature_profile.viewmodel.ProfileImageViewModel

/**
 * 프로필 이미지 관리 화면
 * 
 * 이 화면은 사용자가 프로필 이미지를 선택하고 업로드할 수 있는 UI를 제공합니다.
 * ImagePicker와 FilePicker를 사용하여 이미지를 선택할 수 있습니다.
 */
@Composable
fun ProfileImageScreen(
    viewModel: ProfileImageViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // 이미지 선택기 초기화
    val imagePicker = remember { ImagePicker() }
    val filePicker = remember { FilePicker() }
    
    // 이미지 선택 시작 함수
    val pickImage = imagePicker.createImagePicker(viewModel.imagePickerCallback)
    
    // 파일 선택 시작 함수 (이미지 파일만 필터링)
    val pickFile = filePicker.createFilePicker(
        mimeTypes = FilePicker.IMAGE_MIME_TYPES,
        callback = viewModel.filePickerCallback
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 상단 제목
        Text(
            text = "프로필 이미지 설정",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        // 프로필 이미지 표시
        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color.LightGray.copy(alpha = 0.3f))
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable { pickImage() },
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                // 로딩 중 표시
                CircularProgressIndicator(
                    modifier = Modifier.size(50.dp)
                )
            } else if (uiState.profileImageUrl != null) {
                // 프로필 이미지 표시
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(uiState.profileImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "프로필 이미지",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // 기본 이미지 또는 안내 메시지 표시
                Text(
                    text = "이미지를 선택하려면\n클릭하세요",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 이미지 선택 버튼
        Button(
            onClick = { pickImage() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Text("사진 앱에서 이미지 선택")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 파일 선택 버튼
        OutlinedButton(
            onClick = { pickFile() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Text("파일에서 이미지 선택")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 오류 메시지 표시
        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
