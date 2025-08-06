package com.example.data_converter

import com.example.domain.AggregateRoot
import com.example.domain.model.base.User
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.model.enum.UserStatus
import com.example.domain.vo.DocumentId
import com.example.domain.vo.user.UserEmail
import com.example.domain.vo.user.UserFcmToken
import com.example.domain.vo.user.UserMemo
import com.example.domain.vo.user.UserName
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User Domain Model과 JSON 간의 변환을 담당하는 Converter
 */
@Singleton
class UserJsonConverter @Inject constructor(
    private val gson: Gson
) : JsonConverter<User> {

    override fun toJson(data: User): String {
        return try {
            val userData = mapOf(
                AggregateRoot.KEY_ID to data.id.value,
                User.KEY_EMAIL to data.email.value,
                User.KEY_NAME to data.name.value,
                User.KEY_CONSENT_TIMESTAMP to data.consentTimeStamp.toEpochMilli(),
                User.KEY_MEMO to data.memo?.value,
                User.KEY_USER_STATUS to data.userStatus.name,
                User.KEY_FCM_TOKEN to data.fcmToken?.value,
                User.KEY_ACCOUNT_STATUS to data.accountStatus.name,
                AggregateRoot.KEY_CREATED_AT to data.createdAt?.toEpochMilli(),
                AggregateRoot.KEY_UPDATED_AT to data.updatedAt?.toEpochMilli()
            )
            gson.toJson(userData)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert User to JSON: ${e.message}", e)
        }
    }

    override fun fromJson(json: String): User {
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val userData: Map<String, Any?> = gson.fromJson(json, type)

            User.fromDataSource(
                id = DocumentId(userData[AggregateRoot.KEY_ID] as String),
                email = UserEmail(userData[User.KEY_EMAIL] as String),
                name = UserName(userData[User.KEY_NAME] as String),
                consentTimeStamp = Instant.ofEpochMilli((userData[User.KEY_CONSENT_TIMESTAMP] as Double).toLong()),
                memo = (userData[User.KEY_MEMO] as? String)?.let { UserMemo(it) },
                userStatus = UserStatus.valueOf(userData[User.KEY_USER_STATUS] as String),
                fcmToken = (userData[User.KEY_FCM_TOKEN] as? String)?.let { UserFcmToken(it) },
                accountStatus = UserAccountStatus.valueOf(userData[User.KEY_ACCOUNT_STATUS] as String),
                createdAt = (userData[AggregateRoot.KEY_CREATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) },
                updatedAt = (userData[AggregateRoot.KEY_UPDATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) }
            )
        } catch (e: JsonSyntaxException) {
            throw JsonConversionException("Invalid JSON format for User: ${e.message}", e)
        } catch (e: ClassCastException) {
            throw JsonConversionException("JSON structure mismatch for User: ${e.message}", e)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert JSON to User: ${e.message}", e)
        }
    }
}