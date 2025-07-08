import {CustomResult, Result} from "../../../core/types";
import {ValidationError, ConflictError, NotFoundError} from "../../../core/errors";
import {UserRepository} from "../../../domain/user/repositories/user.repository";
import {DMChannelRepository} from "../../../domain/dmchannel/repositories/dmchannel.repository";
import {DMWrapperRepository} from "../../../domain/dmwrapper/repositories/dmwrapper.repository";
import {DMChannelEntity} from "../../../domain/dmchannel/entities/dmchannel.entity";
import {DMWrapperEntity} from "../../../domain/dmwrapper/entities/dmwrapper.entity";

export interface CreateDMChannelRequest {
  currentUserId: string;
  targetUserName: string;
}

export interface CreateDMChannelResponse {
  channelId: string;
  otherUserId: string;
  otherUserName: string;
  otherUserImageUrl: string;
  createdAt: string;
}

export class CreateDMChannelUseCase {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly dmChannelRepository: DMChannelRepository,
    private readonly dmWrapperRepository: DMWrapperRepository
  ) {}

  async execute(request: CreateDMChannelRequest): Promise<CustomResult<CreateDMChannelResponse>> {
    try {
      // 입력 검증
      if (!request.currentUserId || !request.targetUserName) {
        return Result.failure(new ValidationError("request", "Current user ID and target user name are required"));
      }

      const currentUserId = request.currentUserId;
      const targetUserName = request.targetUserName;

      // 현재 사용자 정보 조회
      const currentUserResult = await this.userRepository.findByUserId(currentUserId);
      if (!currentUserResult.success || !currentUserResult.data) {
        return Result.failure(new NotFoundError("Current user not found", currentUserId));
      }
      const currentUser = currentUserResult.data;

      // 대상 사용자 정보 조회 (이름으로 검색)
      const targetUserResult = await this.userRepository.findByName(targetUserName);
      if (!targetUserResult.success || !targetUserResult.data) {
        return Result.failure(new NotFoundError("Target user not found", targetUserName));
      }
      const targetUser = targetUserResult.data;

      // 자기 자신과 DM 생성 방지
      if (currentUserId === targetUser.id) {
        return Result.failure(new ValidationError("targetUser", "Cannot create DM with yourself"));
      }

      // 기존 DM Channel이 있는지 확인
      const existingChannelResult = await this.dmChannelRepository.findByParticipants(currentUserId, targetUser.id);
      if (existingChannelResult.success && existingChannelResult.data) {
        return Result.failure(new ConflictError("dmChannel", "exists", "DM channel already exists between these users"));
      }

      // DM Channel 생성
      const channelResult = await this.createDMChannelForUsers(currentUserId, targetUser.id, currentUser, targetUser);
      if (!channelResult.success) {
        return Result.failure(channelResult.error);
      }

      return Result.success({
        channelId: channelResult.data.channelId,
        otherUserId: targetUser.id,
        otherUserName: targetUser.name,
        otherUserImageUrl: targetUser.profileImageUrl || "",
        createdAt: new Date().toISOString(),
      });
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error("Failed to create DM channel"));
    }
  }

  private async createDMChannelForUsers(
    userId1: string,
    userId2: string,
    user1: any,
    user2: any
  ): Promise<CustomResult<{channelId: string}>> {
    try {
      // DM Channel ID 생성 (두 사용자 ID를 정렬하여 고유한 ID 생성)
      const sortedUserIds = [userId1, userId2].sort();
      const channelId = `dm_${sortedUserIds[0]}_${sortedUserIds[1]}`;

      // DM Channel 생성
      const dmChannel = DMChannelEntity.createForUsers(channelId, userId1, userId2);
      const saveChannelResult = await this.dmChannelRepository.save(dmChannel);
      if (!saveChannelResult.success) {
        return Result.failure(saveChannelResult.error);
      }

      // 첫 번째 사용자의 DM Wrapper 생성 (상대방을 가리킴)
      const dmWrapper1 = DMWrapperEntity.createForUsers(
        userId2, // wrapperId는 상대방 ID
        userId2, // otherUserId
        user2.name, // otherUserName
        user2.profileImageUrl || undefined // otherUserImageUrl (null-safe)
      );

      const saveWrapper1Result = await this.dmWrapperRepository.save(userId1, dmWrapper1);
      if (!saveWrapper1Result.success) {
        return Result.failure(saveWrapper1Result.error);
      }

      // 두 번째 사용자의 DM Wrapper 생성 (상대방을 가리킴)
      const dmWrapper2 = DMWrapperEntity.createForUsers(
        userId1, // wrapperId는 상대방 ID
        userId1, // otherUserId
        user1.name, // otherUserName
        user1.profileImageUrl || undefined // otherUserImageUrl (null-safe)
      );

      const saveWrapper2Result = await this.dmWrapperRepository.save(userId2, dmWrapper2);
      if (!saveWrapper2Result.success) {
        return Result.failure(saveWrapper2Result.error);
      }

      return Result.success({channelId});
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error("Failed to create DM channel and wrappers"));
    }
  }
}