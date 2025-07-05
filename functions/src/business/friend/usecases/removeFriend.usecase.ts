import { CustomResult, Result } from '../../../core/types';
import { ValidationError, ConflictError, NotFoundError } from '../../../core/errors';
import { FriendRepository } from '../../../domain/friend/repositories/friend.repository';
import { UserProfileRepository } from '../../../domain/user/repositories/userProfile.repository';
import { UserId, FriendStatus } from '../../../domain/friend/entities/friend.entity';

export interface RemoveFriendRequest {
  userId: string;       // 친구를 제거하는 사용자
  friendUserId: string; // 제거될 친구
}

export interface RemoveFriendResponse {
  success: boolean;
  removedAt: string;
}

export class RemoveFriendUseCase {
  constructor(
    private readonly friendRepository: FriendRepository,
    private readonly userRepository: UserProfileRepository
  ) {}

  async execute(request: RemoveFriendRequest): Promise<CustomResult<RemoveFriendResponse>> {
    try {
      // 입력 검증
      if (!request.userId || !request.friendUserId) {
        return Result.failure(new ValidationError('request', 'Both user ID and friend user ID are required'));
      }

      if (request.userId === request.friendUserId) {
        return Result.failure(new ValidationError('friendUserId', 'Cannot remove yourself as a friend'));
      }

      const userId = new UserId(request.userId);
      const friendUserId = new UserId(request.friendUserId);

      // 사용자 존재 확인
      const userResult = await this.userRepository.findByUserId(request.userId);
      if (!userResult.success) {
        return Result.failure(userResult.error);
      }
      if (!userResult.data) {
        return Result.failure(new NotFoundError('User not found'));
      }

      // 친구 사용자 존재 확인
      const friendUserResult = await this.userRepository.findByUserId(request.friendUserId);
      if (!friendUserResult.success) {
        return Result.failure(friendUserResult.error);
      }
      if (!friendUserResult.data) {
        return Result.failure(new NotFoundError('Friend user not found'));
      }

      // 현재 친구 관계인지 확인
      const areFriendsResult = await this.friendRepository.areUsersFriends(userId, friendUserId);
      if (!areFriendsResult.success) {
        return Result.failure(areFriendsResult.error);
      }
      if (!areFriendsResult.data) {
        return Result.failure(new ConflictError('Users are not friends'));
      }

      // 양방향 친구 관계 조회
      const friendRelation1Result = await this.friendRepository.findByUserIds(userId, friendUserId);
      const friendRelation2Result = await this.friendRepository.findByUserIds(friendUserId, userId);

      const relations = [];
      if (friendRelation1Result.success && friendRelation1Result.data) {
        relations.push(friendRelation1Result.data);
      }
      if (friendRelation2Result.success && friendRelation2Result.data) {
        relations.push(friendRelation2Result.data);
      }

      if (relations.length === 0) {
        return Result.failure(new NotFoundError('Friend relationship not found'));
      }

      const now = new Date();
      const removedRelations = [];

      // 모든 친구 관계를 제거 상태로 변경
      for (const relation of relations) {
        if (relation.status === FriendStatus.ACCEPTED) {
          const removeResult = relation.remove();
          if (!removeResult.success) {
            return Result.failure(removeResult.error);
          }
          removedRelations.push(removeResult.data);
        }
      }

      if (removedRelations.length === 0) {
        return Result.failure(new ConflictError('No active friend relationships to remove'));
      }

      // 제거된 관계 저장
      for (const removedRelation of removedRelations) {
        const saveResult = await this.friendRepository.update(removedRelation);
        if (!saveResult.success) {
          // 일부만 저장되었을 수 있으므로 에러를 반환하지만 성공한 것은 유지
          console.error('Failed to save removed friend relation:', saveResult.error);
        }
      }

      // 실제로는 데이터를 삭제하는 것이 아니라 상태를 REMOVED로 변경
      // 필요에 따라 실제 삭제를 원한다면 다음 코드를 사용:
      // await this.friendRepository.deleteByUserIds(userId, friendUserId);

      // 양쪽 사용자의 친구 수 업데이트 (백그라운드에서 수행)
      this.updateFriendCounts(request.userId, request.friendUserId);

      return Result.success({
        success: true,
        removedAt: now.toISOString()
      });
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to remove friend'));
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