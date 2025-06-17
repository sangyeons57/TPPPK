package com.example.feature_profile.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_ui.picker.FilePicker
import com.example.core_ui.picker.ImagePicker
import com.example.domain.model.base.User
import com.example.domain.usecase.user.UploadProfileImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 프로필 이미지 관리를 위한 ViewModel
 * 
 * 이 ViewModel은 프로필 이미지 선택 및 업로드 기능을 제공합니다.
 * ImagePicker 또는 FilePicker를 사용하여 이미지를 선택하고
 * UploadProfileImageUseCase를 통해 서버에 업로드합니다.
 */
@HiltViewModel
class ProfileImageViewModel @Inject constructor(
    private val uploadProfileImageUseCase: UploadProfileImageUseCase
) : ViewModel() {

    // UI 상태를 나타내는 데이터 클래스
    data class ProfileImageUiState(
        val isLoading: Boolean = false,
        val profileImageUrl: String? = null,
        val error: String? = null
    )

    // UI 상태를 관리하는 StateFlow
    private val _uiState = MutableStateFlow(ProfileImageUiState())
    val uiState: StateFlow<ProfileImageUiState> = _uiState.asStateFlow()

    /**
     * 이미지 선택 콜백 구현
     */
    val imagePickerCallback = object : ImagePicker.ImagePickerCallback {
        override fun onImageSelected(uri: Uri, mimeType: String?) {
            uploadProfileImage(uri)
        }

        override fun onImageSelectionCancelled() {
            // 사용자가 이미지 선택을 취소한 경우 처리
        }
    }

    /**
     * 파일 선택 콜백 구현
     */
    val filePickerCallback = object : FilePicker.FilePickerCallback {
        override fun onFileSelected(uri: Uri, mimeType: String?) {
            // 이미지 파일인 경우에만 처리
            if (mimeType?.startsWith("image/") == true) {
                uploadProfileImage(uri)
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "이미지 파일만 선택할 수 있습니다."
                )
            }
        }

        override fun onFileSelectionCancelled() {
            // 사용자가 파일 선택을 취소한 경우 처리
        }
    }

    /**
     * 선택한 이미지를 서버에 업로드합니다.
     * 
     * @param imageUri 업로드할 이미지의 URI
     */
    private fun uploadProfileImage(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val result = uploadProfileImageUseCase(imageUri)
                
                when (result) {
                    is CustomResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            profileImageUrl = result.data.profileImageUrl?.value
                        )
                    }
                    is CustomResult.Failure -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.error.message ?: "이미지 업로드에 실패했습니다."
                        )
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error ="알수 없는 문제"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "이미지 업로드 중 오류가 발생했습니다."
                )
            }
        }
    }
}
