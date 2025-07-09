import {CustomResult, Result} from "../../../core/types";
import {ValidationError, NotFoundError, ConflictError} from "../../../core/errors";
import {DMChannelRepository} from "../../../domain/dmchannel/repositories/dmchannel.repository";
import {DMWrapperRepository} from "../../../domain/dmwrapper/repositories/dmwrapper.repository";
import {UserRepository} from "../../../domain/user/repositories/user.repository";
import {DMWrapperEntity} from "../../../domain/dmwrapper/entities/dmwrapper.entity";

export interface UnblockDMChannelRequest {
  currentUserId: string;
  channelId: string;
}

export interface UnblockDMChannelByUserNameRequest {
  currentUserId: string;
  targetUserName: string;
}

export interface UnblockDMChannelResponse {
  channelId: string;
  success: boolean;
  message: string;
  isFullyUnblocked: boolean; // 완전히 해제되었는지 여부
}

export class UnblockDMChannelUseCase {
  constructor(
    private readonly dmChannelRepository: DMChannelRepository,
    private readonly dmWrapperRepository: DMWrapperRepository,
    private readonly userRepository: UserRepository
  ) {}

  async execute(request: UnblockDMChannelRequest): Promise<CustomResult<UnblockDMChannelResponse>> {
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

      // 현재 사용자가 차단을 했는지 확인
      if (!dmChannel.isBlockedByUser(currentUserId)) {
        return Result.failure(new ConflictError("dmChannel", "not_blocked", "You have not blocked this DM channel"));
      }

      // 채널 차단 해제 처리
      const unblockedChannel = dmChannel.unblockByUser(currentUserId);
      const updateResult = await this.dmChannelRepository.save(unblockedChannel);
      if (!updateResult.success) {
        return Result.failure(updateResult.error);
      }

      // 현재 사용자의 DMWrapper 재생성
      const otherUserId = dmChannel.getOtherParticipant(currentUserId);
      if (otherUserId) {
        const createWrapperResult = await this.createDMWrapperForUser(currentUserId, otherUserId, channelId);
        if (!createWrapperResult.success) {
          // DMWrapper 생성 실패 시 경고하지만 차단 해제는 성공으로 처리
          console.warn(`Failed to create DM wrapper for user ${currentUserId}:`, createWrapperResult.error);
        }
      }

      const isFullyUnblocked = unblockedChannel.isActive();

      return Result.success({
        channelId,
        success: true,
        message: isFullyUnblocked ? "DM channel has been fully unblocked and is now active" : "You have unblocked this DM channel, but it remains blocked by the other user",
        isFullyUnblocked,
      });
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error("Failed to unblock DM channel"));
    }
  }

  async executeByUserName(request: UnblockDMChannelByUserNameRequest): Promise<CustomResult<UnblockDMChannelResponse>> {
    try {
      // 입력 검증
      if (!request.currentUserId || !request.targetUserName) {
        return Result.failure(new ValidationError("request", "Current user ID and target user name are required"));
      }

      const currentUserId = request.currentUserId;
      const targetUserName = request.targetUserName;

      // 대상 사용자 조회
      const targetUserResult = await this.userRepository.findByName(targetUserName);
      if (!targetUserResult.success || !targetUserResult.data) {
        return Result.failure(new NotFoundError("Target user not found", targetUserName));
      }

      const targetUser = targetUserResult.data;

      // 기존 DM 채널 조회
      const channelResult = await this.dmChannelRepository.findByParticipants(currentUserId, targetUser.id);
      if (!channelResult.success || !channelResult.data) {
        return Result.failure(new NotFoundError("DM channel not found between users", `${currentUserId} and ${targetUser.id}`));
      }

      const dmChannel = channelResult.data;

      // 현재 사용자가 차단을 했는지 확인
      if (!dmChannel.isBlockedByUser(currentUserId)) {
        return Result.failure(new ConflictError("dmChannel", "not_blocked", "You have not blocked this DM channel"));
      }

      // 채널 차단 해제 처리
      const unblockedChannel = dmChannel.unblockByUser(currentUserId);
      const updateResult = await this.dmChannelRepository.save(unblockedChannel);
      if (!updateResult.success) {
        return Result.failure(updateResult.error);
      }

      // 현재 사용자의 DMWrapper 재생성
      const createWrapperResult = await this.createDMWrapperForUser(currentUserId, targetUser.id, dmChannel.id);
      if (!createWrapperResult.success) {
        // DMWrapper 생성 실패 시 경고하지만 차단 해제는 성공으로 처리
        console.warn(`Failed to create DM wrapper for user ${currentUserId}:`, createWrapperResult.error);
      }

      const isFullyUnblocked = unblockedChannel.isActive();

      return Result.success({
        channelId: dmChannel.id,
        success: true,
        message: isFullyUnblocked ? "DM channel has been fully unblocked and is now active" : "You have unblocked this DM channel, but it remains blocked by the other user",
        isFullyUnblocked,
      });
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error("Failed to unblock DM channel by user name"));
    }
  }

  private async createDMWrapperForUser(
    userId: string,
    otherUserId: string,
    channelId: string
  ): Promise<CustomResult<void>> {
    try {
      // 상대방 사용자 정보 조회
      const otherUserResult = await this.userRepository.findByUserId(otherUserId);
      if (!otherUserResult.success || !otherUserResult.data) {
        return Result.failure(new NotFoundError("Other user not found", otherUserId));
      }

      const otherUser = otherUserResult.data;

      // DMWrapper 생성
      const dmWrapper = DMWrapperEntity.createForUsers(
        channelId,
        otherUserId,
        otherUser.name,
        otherUser.profileImageUrl || undefined
      );

      const saveResult = await this.dmWrapperRepository.save(userId, dmWrapper);
      if (!saveResult.success) {
        return Result.failure(saveResult.error);
      }

      return Result.success(undefined);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error("Failed to create DM wrapper"));
    }
  }
}
