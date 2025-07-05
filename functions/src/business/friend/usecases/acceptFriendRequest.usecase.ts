import { CustomResult, Result } from '../../../core/types';
import { ValidationError, ConflictError, NotFoundError } from '../../../core/errors';
import { FriendRepository } from '../../../domain/friend/repositories/friend.repository';
import { UserProfileRepository } from '../../../domain/user/repositories/userProfile.repository';
import { FriendEntity, UserId, FriendId, FriendStatus } from '../../../domain/friend/entities/friend.entity';

export interface AcceptFriendRequestRequest {
  friendRequestId: string;
  userId: string; // 수락하는 사용자 (수신자)
}

export interface AcceptFriendRequestResponse {
  friendRequestId: string;
  status: string;
  acceptedAt: string;
  reciprocalFriendId: string; // 상대방에게 생성된 친구 관계 ID
}

export class AcceptFriendRequestUseCase {
  constructor(
    private readonly friendRepository: FriendRepository,
    private readonly userRepository: UserProfileRepository
  ) {}

  async execute(request: AcceptFriendRequestRequest): Promise<CustomResult<AcceptFriendRequestResponse>> {
    try {
      // 입력 검증
      if (!request.friendRequestId || !request.userId) {
        return Result.failure(new ValidationError('request', 'Friend request ID and user ID are required'));
      }

      const friendId = new FriendId(request.friendRequestId);
      const userId = new UserId(request.userId);

      // 친구 요청 조회
      const friendRequestResult = await this.friendRepository.findById(friendId);
      if (!friendRequestResult.success) {
        return Result.failure(friendRequestResult.error);
      }
      if (!friendRequestResult.data) {
        return Result.failure(new NotFoundError('Friend request not found'));
      }

      const friendRequest = friendRequestResult.data;

      // 요청 수신자가 맞는지 확인
      if (!friendRequest.isReceiver(userId)) {
        return Result.failure(new ValidationError('userId', 'Only the request receiver can accept this friend request'));
      }

      // 요청 상태 확인
      if (friendRequest.status !== FriendStatus.REQUESTED) {
        return Result.failure(new ConflictError(`Cannot accept friend request. Current status: ${friendRequest.status}`));
      }

      // 사용자 존재 확인
      const userResult = await this.userRepository.findByUserId(request.userId);
      if (!userResult.success) {
        return Result.failure(userResult.error);
      }
      if (!userResult.data) {
        return Result.failure(new NotFoundError('User not found'));
      }

      // 친구 요청 수락
      const acceptResult = friendRequest.accept();
      if (!acceptResult.success) {
        return Result.failure(acceptResult.error);
      }

      const acceptedFriend = acceptResult.data;

      // 수락된 친구 요청 저장
      const saveResult = await this.friendRepository.update(acceptedFriend);
      if (!saveResult.success) {
        return Result.failure(saveResult.error);
      }

      // 상대방을 위한 친구 관계 생성 (양방향 관계)
      const reciprocalFriendResult = FriendEntity.createFriendRequest(
        friendRequest.friendUserId, // 원래 수신자
        friendRequest.userId        // 원래 요청자
      );

      if (!reciprocalFriendResult.success) {
        // 원본 친구 관계는 이미 저장되었으므로 롤백하지 않고 에러만 반환
        return Result.failure(new Error('Failed to create reciprocal friend relationship'));
      }

      // 상대방 친구 관계를 즉시 수락 상태로 변경
      const reciprocalFriend = reciprocalFriendResult.data;
      const acceptReciprocalResult = reciprocalFriend.accept();
      if (!acceptReciprocalResult.success) {
        return Result.failure(new Error('Failed to accept reciprocal friend relationship'));
      }

      // 상대방 친구 관계 저장
      const saveReciprocalResult = await this.friendRepository.save(acceptReciprocalResult.data);
      if (!saveReciprocalResult.success) {
        // 이 경우 데이터 일관성이 깨질 수 있으므로 에러 반환
        return Result.failure(new Error('Failed to save reciprocal friend relationship'));
      }

      // 양쪽 사용자의 친구 수 업데이트 (백그라운드에서 수행)
      this.updateFriendCounts(friendRequest.userId.value, friendRequest.friendUserId.value);

      return Result.success({
        friendRequestId: acceptedFriend.id.value,
        status: acceptedFriend.status,
        acceptedAt: acceptedFriend.respondedAt!.toISOString(),
        reciprocalFriendId: saveReciprocalResult.data.id.value
      });
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to accept friend request'));
    }
  }

  /**
   * 사용자들의 친구 수를 업데이트합니다 (백그라운드 작업)
   */
  private async updateFriendCounts(userId1: string, userId2: string): Promise<void> {
    try {
      // 사용자 1의 친구 수 업데이트
      const user1FriendsResult = await this.friendRepository.countFriendsByUserId(new UserId(userId1));
      if (user1FriendsResult.success) {
        const user1Result = await this.userRepository.findByUserId(userId1);
        if (user1Result.success && user1Result.data) {
          const updatedUser1 = user1Result.data.updateFriendCount(user1FriendsResult.data);
          await this.userRepository.update(updatedUser1);
        }
      }

      // 사용자 2의 친구 수 업데이트
      const user2FriendsResult = await this.friendRepository.countFriendsByUserId(new UserId(userId2));
      if (user2FriendsResult.success) {
        const user2Result = await this.userRepository.findByUserId(userId2);
        if (user2Result.success && user2Result.data) {
          const updatedUser2 = user2Result.data.updateFriendCount(user2FriendsResult.data);
          await this.userRepository.update(updatedUser2);
        }
      }
    } catch (error) {
      // 친구 수 업데이트 실패는 주요 기능에 영향을 주지 않으므로 로그만 남김
      console.error('Failed to update friend counts:', error);
    }
  }
}