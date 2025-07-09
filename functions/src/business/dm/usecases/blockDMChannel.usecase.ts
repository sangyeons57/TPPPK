import {CustomResult, Result} from "../../../core/types";
import {ValidationError, NotFoundError, ConflictError} from "../../../core/errors";
import {DMChannelRepository} from "../../../domain/dmchannel/repositories/dmchannel.repository";
import {DMWrapperRepository} from "../../../domain/dmwrapper/repositories/dmwrapper.repository";

export interface BlockDMChannelRequest {
  currentUserId: string;
  channelId: string;
}

export interface BlockDMChannelResponse {
  channelId: string;
  success: boolean;
  message: string;
}

export class BlockDMChannelUseCase {
  constructor(
    private readonly dmChannelRepository: DMChannelRepository,
    private readonly dmWrapperRepository: DMWrapperRepository
  ) {}

  async execute(request: BlockDMChannelRequest): Promise<CustomResult<BlockDMChannelResponse>> {
    try {
      // 입력 검증
      if (!request.currentUserId || !request.channelId) {
        return Result.failure(new ValidationError("request", "Current user ID and channel ID are required"));
      }

      const currentUserId = request.currentUserId;
      const channelId = request.channelId;

      // DM 채널 조회
      const channelResult = await this.dmChannelRepository.findById(channelId);
      if (!channelResult.success || !channelResult.data) {
        return Result.failure(new NotFoundError("DM channel not found", channelId));
      }

      const dmChannel = channelResult.data;

      // 현재 사용자가 채널 참여자인지 확인
      if (!dmChannel.hasParticipant(currentUserId)) {
        return Result.failure(new ValidationError("authorization", "You are not a participant of this DM channel"));
      }

      // 이미 차단된 채널인지 확인
      if (dmChannel.isBlocked()) {
        return Result.failure(new ConflictError("dmChannel", "blocked", "DM channel is already blocked"));
      }

      // 채널 차단 처리
      const blockedChannel = dmChannel.block();
      const updateResult = await this.dmChannelRepository.save(blockedChannel);
      if (!updateResult.success) {
        return Result.failure(updateResult.error);
      }

      // 양쪽 사용자의 DMWrapper 제거
      const participants = dmChannel.participants;
      const removeWrapperResults = await Promise.all(
        participants.map(async (participantId) => {
          // DMWrapper는 channelId를 document ID로 사용
          return this.dmWrapperRepository.delete(participantId, channelId);
        })
      );

      // DMWrapper 제거 결과 확인
      const failedRemovals = removeWrapperResults.filter((result) => !result.success);
      if (failedRemovals.length > 0) {
        // 일부 DMWrapper 제거 실패 시 경고하지만 차단은 성공으로 처리
        console.warn(`Failed to remove some DM wrappers for channel ${channelId}:`, failedRemovals);
      }

      return Result.success({
        channelId,
        success: true,
        message: "DM channel has been blocked successfully",
      });
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error("Failed to block DM channel"));
    }
  }
}
