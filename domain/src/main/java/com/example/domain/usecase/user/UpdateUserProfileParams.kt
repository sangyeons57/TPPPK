package com.example.domain.usecase.user

import android.net.Uri

/**
 * 사용자 프로필 업데이트에 필요한 매개변수 클래스
 *
 * @property name 사용자 이름
 * @property profileImageUrl 프로필 이미지 URL (null인 경우 이미지 삭제, 빈 문자열인 경우 변경 없음)
 * @property memo 사용자 소개글 (null인 경우 변경 없음)
 * @property localImageUri 로컬 이미지 URI (null인 경우 원격 URL 사용)
 */
data class UpdateUserProfileParams(
    val name: String,
    val profileImageUrl: String? = null,
    val memo: String? = null,
    val localImageUri: Uri? = null
)
