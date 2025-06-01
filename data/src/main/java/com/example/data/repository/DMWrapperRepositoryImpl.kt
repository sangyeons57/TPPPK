package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.DMWrapperRemoteDataSource
import com.example.data.model.remote.toDto
import com.example.domain.model.base.DMWrapper
import com.example.domain.repository.DMWrapperRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DMWrapperRepositoryImpl @Inject constructor(
    private val dmWrapperRemoteDataSource: DMWrapperRemoteDataSource,
    private val auth: FirebaseAuth
) : DMWrapperRepository {

    override fun getDMWrappersStream(userId: String): Flow<CustomResult<List<DMWrapper>, Exception>> {
        // Use the parameterized version of observeDmWrappers method
        return dmWrapperRemoteDataSource.observeDmWrappers(userId)
            .map { dtoList ->
                try {
                    // Convert list of DTOs to domain models
                    val domainList = dtoList.map { it.toDomain() }
                    CustomResult.Success(domainList)
                } catch (e: Exception) {
                    CustomResult.Failure(e)
                }
            }
    }

    override fun getDMWrapperStream(dmChannelId: String): Flow<CustomResult<DMWrapper, Exception>> {
        // Since there's no direct method to get a single DM wrapper stream,
        // we'll filter the results from observeDmWrappers
        return dmWrapperRemoteDataSource.observeDmWrappers()
            .map { dtoList ->
                try {
                    // Find the specific DMWrapper by channel ID
                    val dto = dtoList.find { it.dmChannelId == dmChannelId }
                    if (dto != null) {
                        CustomResult.Success(dto.toDomain())
                    } else {
                        CustomResult.Failure(Exception("DM channel not found: $dmChannelId"))
                    }
                } catch (e: Exception) {
                    CustomResult.Failure(e)
                }
            }
    }

    override fun createDmChannel(otherUserId: String): CustomResult<String, Exception> {
        // This implementation depends on the project's requirements
        // and should use Firebase Auth for the current user ID
        return try {
            val currentUserId = auth.currentUser?.uid 
                ?: return CustomResult.Failure(Exception("User not logged in"))
                
            // For now, return a placeholder implementation
            // This should be replaced with actual Firebase implementation
            CustomResult.Failure(Exception("Implementation pending for createDmChannel"))
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    override fun findDmChannelWithUser(otherUserId: String): CustomResult<String, Exception> {
        // This implementation depends on the project's requirements
        // We need to search existing DM channels to find one with the specified user
        return try {
            val currentUserId = auth.currentUser?.uid 
                ?: return CustomResult.Failure(Exception("User not logged in"))
                
            // For now, return a placeholder implementation
            // This should be replaced with actual Firebase implementation
            CustomResult.Failure(Exception("Implementation pending for findDmChannelWithUser"))
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}
