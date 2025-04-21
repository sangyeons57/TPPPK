// data/model/FriendEntity.kt (Room DB 테이블용)
package com.example.teamnovapersonalprojectprojectingkotlin.data.model
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val userId: String,
    val userName: String,
    val status: String?,
    val profileImageUrl: String?
)