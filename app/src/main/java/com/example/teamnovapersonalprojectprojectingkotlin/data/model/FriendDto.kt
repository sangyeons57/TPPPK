// data/model/FriendDto.kt (네트워크 응답용)
package com.example.teamnovapersonalprojectprojectingkotlin.data.model
import com.google.gson.annotations.SerializedName // 예시: Gson 사용 시

data class FriendDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("user_name") val userName: String,
    @SerializedName("current_status") val status: String?,
    @SerializedName("profile_img") val profileImageUrl: String?
)

