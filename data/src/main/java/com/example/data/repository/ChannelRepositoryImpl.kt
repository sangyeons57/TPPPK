package com.example.data.repository

import com.example.core_common.result.resultTry
import com.example.data.datasource.remote.DMChannelRemoteDataSource
import com.example.data.datasource.remote.MemberRemoteDataSource
import com.example.data.datasource.remote.MessageRemoteDataSource
import com.example.data.datasource.remote.ProjectChannelRemoteDataSource
import com.example.data.model._remote.ProjectChannelDTO
import com.example.data.model.mapper.toDomain
import com.example.domain.model.Channel
import com.example.domain.model.ChannelType
import com.example.domain.model.ChatMessage
import com.example.domain.repository.ChannelRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.Result

class ChannelRepositoryImpl @Inject constructor(
    private val projectChannelRemoteDataSource: ProjectChannelRemoteDataSource,
    private val dmChannelRemoteDataSource: DMChannelRemoteDataSource,
    private val messageRemoteDataSource: MessageRemoteDataSource, // 마지막 메시지, 안읽은 개수용
    private val memberRemoteDataSource: MemberRemoteDataSource // 멤버 관리용 (필요시)
    // private val channelMapper: ChannelMapper // 개별 매퍼 사용시
) : ChannelRepository {

    override fun getProjectChannelsStream(projectId: String): Flow<Result<List<Channel>>> {
        return projectChannelRemoteDataSource.getProjectChannelsStream(projectId).map { result ->
            result.mapCatching { dtoList ->
                dtoList.map { it.toDomain() } // ProjectChannelDTO -> Channel 매핑
            }
        }
    }

    override fun getDmChannelsStream(userId: String): Flow<Result<List<Channel>>> {
        return dmChannelRemoteDataSource.getDmChannelsStream(userId).map { result ->
            result.mapCatching { dtoList ->
                dtoList.map { it.toDomain() } // DMChannelDTO -> Channel 매핑
            }
        }
    }

    override suspend fun getChannelDetails(channelId: String): Result<Channel> = resultTry {
        // 채널 ID만으로는 DM인지 프로젝트 채널인지 구분하기 어려울 수 있음.
        // DataSource에서 먼저 시도해보고, 실패하면 다른 DataSource에서 시도하는 방식 또는
        // 채널 ID 자체에 타입 정보가 포함되어 있다면 더 용이.
        // 여기서는 우선 ProjectChannel로 시도, 실패 시 DMChannel로 시도하는 예시 (간단화된 로직)
        projectChannelRemoteDataSource.getChannel(channelId).fold(
            onSuccess = { it.toDomain() },
            onFailure = {
                dmChannelRemoteDataSource.getChannel(channelId).getOrThrow().toDomain()
            }
        )
    }

    override suspend fun createProjectChannel(
        name: String,
        projectId: String,
        categoryId: String?,
        channelType: ChannelType,
        isPrivate: Boolean,
        memberIds: List<String>
    ): Result<String> = resultTry {
        val dto = ProjectChannelDTO(
            // id는 Firestore에서 자동 생성
            name = name,
            projectId = projectId,
            categoryId = categoryId,
            type = channelType.name, // Enum을 String으로
            isPrivate = isPrivate,
            memberIds = memberIds,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
            // ownerId는 DataSource에서 현재 사용자로 설정하거나, UseCase에서 주입
        )
        projectChannelRemoteDataSource.createChannel(dto).getOrThrow() // ID 반환 가정
    }

    override suspend fun getOrCreateDmChannel(userId1: String, userId2: String): Result<String> = resultTry {
        // DataSource에 getOrCreateDmChannel(userId1, userId2)와 같은 기능이 있어야 함.
        // 이 함수는 내부적으로 두 사용자 간의 DM 채널이 이미 있는지 확인하고,
        // 없으면 생성하며, 있으면 기존 채널 ID를 반환합니다.
        // 현재 DMChannelRemoteDataSource에는 해당 기능이 명시적으로 없을 수 있습니다. (2.1 항목 참고)
        // throw NotImplementedError(\
getOrCreateDmChannel
is
not
fully
defined
in
DataSource
yet.\)
        // 임시로 DataSource에 해당 기능이 있다고 가정
        dmChannelRemoteDataSource.getOrCreateDmChannel(userId1, userId2).getOrThrow()
    }

    override suspend fun updateChannelInfo(channelId: String, name: String, description: String?): Result<Unit> = resultTry {
        // 이것도 Project 채널인지 DM 채널인지에 따라 다른 DataSource 호출 필요
        // getChannelDetails를 통해 타입을 먼저 알아내거나, 두 DataSource 모두 시도
        // 여기서는 ProjectChannelDTO 업데이트를 예시로
        val currentProjectChannel = projectChannelRemoteDataSource.getChannel(channelId).getOrThrow()
        val updatedDto = currentProjectChannel.copy(
            name = name,
            description = description ?: currentProjectChannel.description, // null이면 기존 값 유지
            updatedAt = Timestamp.now()
        )
        projectChannelRemoteDataSource.updateChannel(updatedDto).getOrThrow()
        // TODO: DMChannel의 경우도 처리
    }

    override suspend fun deleteChannel(channelId: String): Result<Unit> = resultTry {
        // Project 채널인지 DM 채널인지에 따라 다른 DataSource 호출 필요
        // 여기서는 ProjectChannel 삭제를 예시로
        projectChannelRemoteDataSource.deleteChannel(channelId).getOrThrow()
        // TODO: DMChannel의 경우도 처리
    }

    override suspend fun addMembersToChannel(channelId: String, userIds: List<String>): Result<Unit> = resultTry {
        // Project 채널에만 해당될 가능성이 높음. DM은 보통 2명.
        // MemberRemoteDataSource 또는 ProjectChannelRemoteDataSource를 통해 멤버 추가
        // 여기서는 ProjectChannelRemoteDataSource에 addMembers 기능이 있다고 가정
        // projectChannelRemoteDataSource.addMembers(channelId, userIds).getOrThrow()
        throw NotImplementedError(\addMembersToChannel
in
DataSource
needs
to
be
defined.\)
    }

    override suspend fun removeMemberFromChannel(channelId: String, userId: String): Result<Unit> = resultTry {
        // Project 채널에만 해당될 가능성이 높음.
        // projectChannelRemoteDataSource.removeMember(channelId, userId).getOrThrow()
        throw NotImplementedError(\removeMemberFromChannel
in
DataSource
needs
to
be
defined.\)
    }

    override fun getLastMessageStream(channelId: String): Flow<Result<ChatMessage?>> {
        return messageRemoteDataSource.getLastMessageStream(channelId).map { result ->
            result.mapCatching { dto -> dto?.toDomain() }
        }
    }

    override fun getUnreadMessageCountStream(channelId: String, userId: String): Flow<Result<Int>> {
        // 이 기능은 복잡하며, MessageRemoteDataSource 또는 별도 로직 필요
        // 예를 들어, 채널의 lastMessageTimestamp와 사용자의 lastReadTimestamp를 비교하여 계산
        // 여기서는 MessageRemoteDataSource에 해당 기능이 있다고 가정 (단순화)
        // return messageRemoteDataSource.getUnreadCountStream(channelId, userId)
        return flow { emit(Result.success(0)) } // 임시 구현
    }
    
    override suspend fun markMessagesAsRead(channelId: String, userId: String): Result<Unit> = resultTry {
        // 사용자의 채널별 마지막 읽은 메시지 정보를 업데이트하는 로직
        // 예를 들어 UserChannelMetadata 같은 별도 컬렉션에 저장하거나,
        // Device-specific하게 로컬에 저장 후 서버와 동기화 할 수 있습니다.
        // MessageRemoteDataSource 또는 UserRepository 등에 기능이 필요할 수 있습니다.
        // messageRemoteDataSource.markAsRead(channelId, userId).getOrThrow()
        throw NotImplementedError(\markMessagesAsRead
in
DataSource/Repository
needs
to
be
defined.\)
    }
}
