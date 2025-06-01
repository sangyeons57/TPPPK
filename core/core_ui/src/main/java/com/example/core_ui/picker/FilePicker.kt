package com.example.core_ui.picker

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.core_common.util.MediaUtil

/**
 * 파일 선택 기능을 제공하는 유틸리티 클래스입니다.
 * 
 * 이 클래스는 다양한 유형의 파일(이미지, 문서 등)을 선택할 수 있는 기능을 제공합니다.
 * ActivityResultLauncher를 사용하여 파일 선택 결과를 처리합니다.
 */
class FilePicker {
    
    /**
     * 파일 선택 결과 콜백 인터페이스
     */
    interface FilePickerCallback {
        /**
         * 파일 선택 성공 시 호출됩니다.
         * 
         * @param uri 선택된 파일의 URI
         * @param mimeType 선택된 파일의 MIME 타입
         */
        fun onFileSelected(uri: Uri, mimeType: String?)
        
        /**
         * 파일 선택 취소 시 호출됩니다.
         */
        fun onFileSelectionCancelled()
    }
    
    companion object {
        /**
         * 이미지 MIME 타입 필터
         */
        val IMAGE_MIME_TYPES = arrayOf(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/heic",
            "image/heif"
        )
        
        /**
         * 문서 MIME 타입 필터
         */
        val DOCUMENT_MIME_TYPES = arrayOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain"
        )
        
        /**
         * 모든 파일 MIME 타입 필터
         */
        val ALL_MIME_TYPES = arrayOf("*/*")
    }
    
    /**
     * Compose에서 사용할 수 있는 파일 선택기를 생성합니다.
     * 
     * @param mimeTypes 선택 가능한 파일의 MIME 타입 배열
     * @param callback 파일 선택 결과 콜백
     * @return 파일 선택을 시작하는 함수
     */
    @Composable
    fun createFilePicker(
        mimeTypes: Array<String> = ALL_MIME_TYPES,
        callback: FilePickerCallback
    ): () -> Unit {
        val context = LocalContext.current
        
        // GetContent 계약을 사용하여 파일 선택기 생성
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                val mimeType = MediaUtil.getMimeType(context, uri)
                callback.onFileSelected(uri, mimeType)
            } else {
                callback.onFileSelectionCancelled()
            }
        }
        
        return remember {
            {
                launcher.launch(mimeTypes.firstOrNull() ?: "*/*")
            }
        }
    }
    
    /**
     * Compose에서 사용할 수 있는 다중 파일 선택기를 생성합니다.
     * 
     * @param mimeTypes 선택 가능한 파일의 MIME 타입 배열
     * @param callback 파일 선택 결과 콜백
     * @return 다중 파일 선택을 시작하는 함수
     */
    @Composable
    fun createMultipleFilePicker(
        mimeTypes: Array<String> = ALL_MIME_TYPES,
        callback: (List<Uri>) -> Unit
    ): () -> Unit {
        // GetMultipleContents 계약을 사용하여 다중 파일 선택기 생성
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetMultipleContents()
        ) { uris ->
            callback(uris)
        }
        
        return remember {
            {
                launcher.launch(mimeTypes.firstOrNull() ?: "*/*")
            }
        }
    }
}
