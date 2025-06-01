package com.example.core_ui.picker

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.core_common.util.MediaUtil

/**
 * 이미지 선택 기능을 제공하는 유틸리티 클래스입니다.
 * 
 * 이 클래스는 최신 Android PhotoPicker API를 사용하여 이미지를 선택할 수 있는 기능을 제공합니다.
 * ActivityResultLauncher와 PickVisualMedia를 사용하여 이미지 선택 결과를 처리합니다.
 */
class ImagePicker {
    
    /**
     * 이미지 선택 결과 콜백 인터페이스
     */
    interface ImagePickerCallback {
        /**
         * 이미지 선택 성공 시 호출됩니다.
         * 
         * @param uri 선택된 이미지의 URI
         * @param mimeType 선택된 이미지의 MIME 타입
         */
        fun onImageSelected(uri: Uri, mimeType: String?)
        
        /**
         * 이미지 선택 취소 시 호출됩니다.
         */
        fun onImageSelectionCancelled()
    }
    
    /**
     * Compose에서 사용할 수 있는 이미지 선택기를 생성합니다.
     * 
     * @param callback 이미지 선택 결과 콜백
     * @return 이미지 선택을 시작하는 함수
     */
    @Composable
    fun createImagePicker(callback: ImagePickerCallback): () -> Unit {
        val context = LocalContext.current
        
        // PhotoPicker API를 사용하여 이미지 선택기 생성
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                val mimeType = MediaUtil.getMimeType(context, uri)
                callback.onImageSelected(uri, mimeType)
            } else {
                callback.onImageSelectionCancelled()
            }
        }
        
        return remember {
            {
                launcher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        }
    }
    
    /**
     * Compose에서 사용할 수 있는 다중 이미지 선택기를 생성합니다.
     * 
     * @param maxItems 선택 가능한 최대 이미지 수 (기본값: 10)
     * @param callback 다중 이미지 선택 결과 콜백
     * @return 다중 이미지 선택을 시작하는 함수
     */
    @Composable
    fun createMultipleImagePicker(
        maxItems: Int = 10,
        callback: (List<Uri>) -> Unit
    ): () -> Unit {
        // PhotoPicker API를 사용하여 다중 이미지 선택기 생성
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems)
        ) { uris ->
            callback(uris)
        }
        
        return remember {
            {
                launcher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        }
    }
}
