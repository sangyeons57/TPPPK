package com.example.core_ui.components.file

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core_ui.components.bottom_sheet_dialog.BottomSheetDialog
import com.example.core_ui.components.bottom_sheet_dialog.BottomSheetDialogItem
import com.example.core_ui.picker.FilePicker
import com.example.core_ui.picker.ImagePicker

/**
 * 파일 선택 옵션
 */
sealed class FileSelectionOption(val title: String) {
    object Gallery : FileSelectionOption("갤러리")
    object Camera : FileSelectionOption("카메라")
    object Document : FileSelectionOption("문서")
    object Video : FileSelectionOption("비디오")
    object Audio : FileSelectionOption("오디오")
}

/**
 * 파일 첨부 선택기 컴포넌트
 * 갤러리, 카메라, 파일 선택 등의 옵션을 제공합니다.
 */
@Composable
fun FileAttachmentSelector(
    onFileSelected: (Uri) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    options: List<FileSelectionOption> = listOf(
        FileSelectionOption.Gallery,
        FileSelectionOption.Camera,
        FileSelectionOption.Document,
        FileSelectionOption.Video
    )
) {
    val context = LocalContext.current
    var showBottomSheet by remember { mutableStateOf(false) }

    // 권한 요청 런처들
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 카메라 촬영 시작
            // TODO: 카메라 촬영 구현
        } else {
            onError("카메라 권한이 필요합니다.")
        }
    }

    // 이미지 선택기
    val imagePicker = ImagePicker().createImagePicker(
        object : ImagePicker.ImagePickerCallback {
            override fun onImageSelected(uri: Uri, mimeType: String?) {
                onFileSelected(uri)
            }
            override fun onImageSelectionCancelled() {}
        }
    )

    // 파일 선택기들
    val documentPicker = FilePicker().createFilePicker(
        mimeTypes = FilePicker.DOCUMENT_MIME_TYPES,
        callback = object : FilePicker.FilePickerCallback {
            override fun onFileSelected(uri: Uri, mimeType: String?) {
                onFileSelected(uri)
            }
            override fun onFileSelectionCancelled() {}
        }
    )

    val videoPicker = FilePicker().createFilePicker(
        mimeTypes = arrayOf("video/*"),
        callback = object : FilePicker.FilePickerCallback {
            override fun onFileSelected(uri: Uri, mimeType: String?) {
                onFileSelected(uri)
            }
            override fun onFileSelectionCancelled() {}
        }
    )

    val audioPicker = FilePicker().createFilePicker(
        mimeTypes = arrayOf("audio/*"),
        callback = object : FilePicker.FilePickerCallback {
            override fun onFileSelected(uri: Uri, mimeType: String?) {
                onFileSelected(uri)
            }
            override fun onFileSelectionCancelled() {}
        }
    )

    // 메인 버튼
    IconButton(
        onClick = { showBottomSheet = true },
        enabled = enabled,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.AttachFile,
            contentDescription = "파일 첨부"
        )
    }

    // 선택 옵션 바텀시트
    if (showBottomSheet) {
        val bottomSheetItems = options.map { option ->
            when (option) {
                is FileSelectionOption.Gallery -> BottomSheetDialogItem.Button(
                    label = option.title,
                    icon = Icons.Default.PhotoLibrary,
                    onClick = {
                        imagePicker()
                        showBottomSheet = false
                    }
                )
                is FileSelectionOption.Camera -> BottomSheetDialogItem.Button(
                    label = option.title,
                    icon = Icons.Default.CameraAlt,
                    onClick = {
                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                                // TODO: 카메라 촬영 구현
                                showBottomSheet = false
                            }
                            else -> {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                showBottomSheet = false
                            }
                        }
                    }
                )
                is FileSelectionOption.Document -> BottomSheetDialogItem.Button(
                    label = option.title,
                    icon = Icons.Default.AttachFile,
                    onClick = {
                        documentPicker()
                        showBottomSheet = false
                    }
                )
                is FileSelectionOption.Video -> BottomSheetDialogItem.Button(
                    label = option.title,
                    icon = Icons.Default.Videocam,
                    onClick = {
                        videoPicker()
                        showBottomSheet = false
                    }
                )
                is FileSelectionOption.Audio -> BottomSheetDialogItem.Button(
                    label = "오디오",
                    icon = Icons.Default.AttachFile, // TODO: 적절한 아이콘으로 변경
                    onClick = {
                        audioPicker()
                        showBottomSheet = false
                    }
                )
            }
        }

        BottomSheetDialog(
            items = bottomSheetItems,
            onDismiss = { showBottomSheet = false }
        )
    }
}

/**
 * 빠른 파일 선택 버튼들을 가로로 나열한 컴포넌트
 */
@Composable
fun QuickFileSelectionBar(
    onFileSelected: (Uri) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val imagePicker = ImagePicker().createImagePicker(
        object : ImagePicker.ImagePickerCallback {
            override fun onImageSelected(uri: Uri, mimeType: String?) {
                onFileSelected(uri)
            }
            override fun onImageSelectionCancelled() {}
        }
    )

    val documentPicker = FilePicker().createFilePicker(
        mimeTypes = FilePicker.DOCUMENT_MIME_TYPES,
        callback = object : FilePicker.FilePickerCallback {
            override fun onFileSelected(uri: Uri, mimeType: String?) {
                onFileSelected(uri)
            }
            override fun onFileSelectionCancelled() {}
        }
    )

    LazyRow(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            listOf(
                Triple("갤러리", Icons.Default.PhotoLibrary) { imagePicker() },
                Triple("카메라", Icons.Default.CameraAlt) { 
                    // TODO: 카메라 촬영 구현
                },
                Triple("문서", Icons.Default.AttachFile) { documentPicker() }
            )
        ) { (title, icon, action) ->
            Surface(
                onClick = action,
                enabled = enabled,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}