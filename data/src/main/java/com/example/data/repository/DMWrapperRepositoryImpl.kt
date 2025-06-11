package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.DMWrapperRemoteDataSource
import com.example.data.model.remote.DMWrapperDTO // Assuming DMWrapperDTO is in this package
import com.example.domain.model.base.DMWrapper
import com.example.domain.repository.DMWrapperRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue // Import for serverTimestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
// import java.util.Date // For initial timestamp if needed, though serverTimestamp is preferred
import javax.inject.Inject

class DMWrapperRepositoryImpl @Inject constructor(
    private val dmWrapperRemoteDataSource: DMWrapperRemoteDataSource,
    private val auth: FirebaseAuth
) : DMWrapperRepository {

    override fun getDMWrappersStream(userId: String): Flow<CustomResult<List<DMWrapper>, Exception>> {
        return dmWrapperRemoteDataSource.observeDmWrappers(userId)
            .map { dtoList ->
                try {
                    val domainList = dtoList.map { it.toDomain() }
                    CustomResult.Success(domainList)
                } catch (e: Exception) {
                    CustomResult.Failure(e)
                }
            }
    }

    override fun getDMWrapperStream(currentUserId: String, dmChannelId: String): Flow<CustomResult<DMWrapper, Exception>> {
        return dmWrapperRemoteDataSource.observeDmWrappers(currentUserId)
            .map { dtoList ->
                try {
                    val dto = dtoList.find { it.dmChannelId == dmChannelId }
                    if (dto != null) {
                        CustomResult.Success(dto.toDomain())
                    } else {
                        CustomResult.Failure(Exception("DMWrapper for DM channel not found: $dmChannelId"))
                    }
                } catch (e: Exception) {
                    CustomResult.Failure(e)
                }
            }
    }

    override suspend fun createDMWrapper(
        userId: String,
        dmChannelId: String,
        otherUserId: String,
    ): CustomResult<String, Exception> {

        val newDmWrapperDto = DMWrapperDTO(
            dmChannelId = dmChannelId,
            otherUserId = otherUserId
            // Fields like otherUserId, otherUserName, otherUserProfileImageUrl
            // would typically be populated when creating the user-specific copies
            // or by a Cloud Function that enriches the global wrapper.
        )

        return dmWrapperRemoteDataSource.createDMWrapper(userId, newDmWrapperDto)
    }

    override suspend fun findDmChannelIdWithUser(currentUserId: String, otherUserId: String): CustomResult<String, Exception> {

        return when (val result = dmWrapperRemoteDataSource.findDMWrapperByExactParticipants(currentUserId, otherUserId)) {
            is CustomResult.Success -> CustomResult.Success(result.data.dmChannelId) // Found existing wrapper, return its channelId
            is CustomResult.Failure -> {
                CustomResult.Failure(result.error) // Propagate error
            }
            else -> CustomResult.Failure(Exception("Unknown error type from findDMWrapperByExactParticipants"))
        }
    }
}
