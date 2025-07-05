import { CustomResult, Result } from '../../../core/types';
import { ValidationError, ConflictError, NotFoundError } from '../../../core/errors';
import { FriendRepository } from '../../../domain/friend/repositories/friend.repository';
import { UserProfileRepository } from '../../../domain/user/repositories/userProfile.repository';
import { FriendEntity, UserId } from '../../../domain/friend/entities/friend.entity';

export interface SendFriendRequestRequest {
  requesterId: string;
  receiverUserId: string;
}

export interface SendFriendRequestResponse {
  friendRequestId: string;
  status: string;
  requestedAt: string;
}

export class SendFriendRequestUseCase {
  constructor(
    private readonly friendRepository: FriendRepository,
    private readonly userRepository: UserProfileRepository
  ) {}

  async execute(request: SendFriendRequestRequest): Promise<CustomResult<SendFriendRequestResponse>> {
    try {
      // 입력 검증
      if (!request.requesterId || !request.receiverUserId) {
        return Result.failure(new ValidationError('userId', 'Both requester and receiver IDs are required'));
      }

      if (request.requesterId === request.receiverUserId) {
        return Result.failure(new ValidationError('receiverUserId', 'Cannot send friend request to yourself'));
      }

      const requesterId = new UserId(request.requesterId);
      const receiverId = new UserId(request.receiverUserId);

      // 요청자 존재 확인
      const requesterResult = await this.userRepository.findByUserId(request.requesterId);
      if (!requesterResult.success) {
        return Result.failure(requesterResult.error);
      }
      if (!requesterResult.data) {
        return Result.failure(new NotFoundError('Requester not found'));
      }

      // 수신자 존재 확인
      const receiverResult = await this.userRepository.findByUserId(request.receiverUserId);
      if (!receiverResult.success) {
        return Result.failure(receiverResult.error);
      }
      if (!receiverResult.data) {
        return Result.failure(new NotFoundError('Receiver not found'));
      }

      // 수신자가 친구 요청을 받을 수 있는지 확인
      if (!receiverResult.data.canReceiveRequests()) {
        return Result.failure(new ConflictError('User is not accepting friend requests'));
      }

      // 이미 친구인지 확인
      const areFriendsResult = await this.friendRepository.areUsersFriends(requesterId, receiverId);
      if (!areFriendsResult.success) {
        return Result.failure(areFriendsResult.error);
      }
      if (areFriendsResult.data) {
        return Result.failure(new ConflictError('Users are already friends'));
      }

      // 기존 친구 요청 확인 (양방향)
      const existingRequestResult = await this.friendRepository.friendRequestExists(requesterId, receiverId);
      if (!existingRequestResult.success) {
        return Result.failure(existingRequestResult.error);
      }
      if (existingRequestResult.data) {
        return Result.failure(new ConflictError('Friend request already exists'));
      }

      const reverseRequestResult = await this.friendRepository.friendRequestExists(receiverId, requesterId);
      if (!reverseRequestResult.success) {
        return Result.failure(reverseRequestResult.error);
      }
      if (reverseRequestResult.data) {
        return Result.failure(new ConflictError('Friend request already exists from the other user'));
      }

      // 친구 요청 생성
      const friendRequestResult = FriendEntity.createFriendRequest(requesterId, receiverId);
      if (!friendRequestResult.success) {
        return Result.failure(friendRequestResult.error);
      }

      // 친구 요청 저장
      const saveResult = await this.friendRepository.save(friendRequestResult.data);
      if (!saveResult.success) {
        return Result.failure(saveResult.error);
      }

      const savedFriend = saveResult.data;

      return Result.success({
        friendRequestId: savedFriend.id.value,
        status: savedFriend.status,
        requestedAt: savedFriend.requestedAt.toISOString()
      });
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to send friend request'));
    }
  }
}