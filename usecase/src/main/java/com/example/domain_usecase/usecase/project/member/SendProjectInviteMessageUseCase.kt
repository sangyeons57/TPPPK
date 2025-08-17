package com.example.domain_usecase.usecase.project.member

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.vo.ChannelId
import com.example.domain.vo.DocumentId
import com.example.domain.vo.message.MessagePayload
import com.example.domain.vo.message.MessageType
import com.example.domain.vo.user.UserName
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.MessageRepository
import com.example.domain_repository.base.ProjectRepository
import com.example.domain_usecase.usecase.dm.AddDmChannelUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * 프로젝트 초대 메시지를 DM으로 전송하는 UseCase
 * 
 * 플로우:
 * 1. DM 채널 생성/확인
 * 2. WebSocket 채널 접속 
 * 3. 프로젝트 초대 특수 메시지 전송
 * 4. 자동 퇴장
 */
interface SendProjectInviteMessageUseCase {
    /**
     * 사용자에게 프로젝트 초대 메시지를 DM으로 전송
     * 
     * @param targetUserName 초대할 사용자 이름
     * @param projectId 프로젝트 ID
     * @return 전송 과정의 진행 상태를 나타내는 Flow
     */
    operator fun invoke(
        targetUserName: UserName,
        projectId: String
    ): Flow<CustomResult<Unit, Exception>>
}

/**
 * 프로젝트 초대 메시지 전송 UseCase 구현체
 */
class SendProjectInviteMessageUseCaseImpl @Inject constructor(
    private val addDmChannelUseCase: AddDmChannelUseCase,
    private val messageRepository: MessageRepository,
    private val projectRepository: ProjectRepository,
    private val authRepository: AuthRepository
) : SendProjectInviteMessageUseCase {

    private val TAG = "SendProjectInviteMessageUseCase"

    override operator fun invoke(
        targetUserName: UserName, 
        projectId: String
    ): Flow<CustomResult<Unit, Exception>> = flow {
        
        emit(CustomResult.Loading)

        try {
            Log.d(TAG, "프로젝트 초대 메시지 전송 시작: targetUser=${targetUserName.value}, projectId=$projectId")

            // 1. 현재 사용자 정보 가져오기
            val currentUserResult = authRepository.getCurrentUserSession()
            if (currentUserResult !is CustomResult.Success) {
                emit(CustomResult.Failure(Exception("사용자 인증이 필요합니다.")))
                return@flow
            }
            val currentUser = currentUserResult.data
            val inviterId = currentUser.userId
            
            // 2. 프로젝트 정보 가져오기
            val projectResult = projectRepository.findById(DocumentId(projectId))
            val project = when (projectResult) {
                is CustomResult.Success -> projectResult.data
                else -> {
                    emit(CustomResult.Failure(Exception("프로젝트 정보를 찾을 수 없습니다.")))
                    return@flow
                }
            }

            // 3. 별도 초대 문서 없이, projectId로만 참여하는 메시지 구성

            // 4. DM 채널 생성/확인
            Log.d(TAG, "DM 채널 생성/확인 중...")
            val dmChannelResult = addDmChannelUseCase(targetUserName).first()
            val dmChannelId = when (dmChannelResult) {
                is CustomResult.Success -> dmChannelResult.data.value
                is CustomResult.Failure -> {
                    emit(CustomResult.Failure(dmChannelResult.error))
                    return@flow
                }
                else -> {
                    emit(CustomResult.Failure(Exception("DM 채널 생성 과정에서 알 수 없는 오류가 발생했습니다.")))
                    return@flow
                }
            }

            // 5. 프로젝트 초대 메시지 생성 및 저장
            Log.d(TAG, "프로젝트 초대 메시지 생성 중...")
            val messageId = DocumentId.generate()
            val payload = MessagePayload.forProjectInvite(
                projectId = project.id.value,
                projectName = project.name.value,
                inviterName = currentUser.displayName?.value ?: currentUser.email?.value ?: "알 수 없음",
                invitationId = ""
            )

            // 6. 메시지를 Repository에 저장 (WebSocket 전송은 MessageRepository 내부에서 처리)
            val message = Message.create(
                id = messageId,
                senderId = inviterId,
                messageType = MessageType.PROJECT_INVITE,
                payload = payload,
                replyToMessageId = null,
                mentions = emptyList(),
                channelId = ChannelId(dmChannelId)
            )

            val messageSaveResult = messageRepository.save(message)
            when (messageSaveResult) {
                is CustomResult.Success -> {
                    Log.d(TAG, "프로젝트 초대 메시지 저장 및 전송 완료")
                }
                is CustomResult.Failure -> {
                    Log.e(TAG, "메시지 저장 실패", messageSaveResult.error)
                    emit(CustomResult.Failure(Exception("메시지 전송에 실패했습니다: ${messageSaveResult.error.message}")))
                    return@flow
                }
                else -> {
                    emit(CustomResult.Failure(Exception("메시지 전송 중 알 수 없는 오류가 발생했습니다.")))
                    return@flow
                }
            }

            Log.d(TAG, "프로젝트 초대 메시지 전송 완료")
            emit(CustomResult.Success(Unit))

        } catch (e: Exception) {
            Log.e(TAG, "프로젝트 초대 메시지 전송 중 오류 발생", e)
            emit(CustomResult.Failure(e))
        }
    }
}
