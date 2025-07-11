import {CustomResult, Result} from "../../../core/types";
import {ValidationError, ConflictError, NotFoundError} from "../../../core/errors";
import {FriendRepository} from "../../../domain/friend/repositories/friend.repository";
import {UserRepository} from "../../../domain/user/repositories/user.repository";
import {FriendStatus} from "../../../domain/friend/entities/friend.entity";

export interface RemoveFriendRequest {
  userId: string;// 친구를 제거하는 사용자
  friendUserId: string; // 제거될 친구
}

export interface RemoveFriendResponse {
  success: boolean;
  removedAt: string;
}

export class RemoveFriendUseCase {
  constructor(
    private readonly friendRepository: FriendRepository,
    private readonly userRepository: UserRepository
  ) {}

  async execute(request: RemoveFriendRequest): Promise<CustomResult<RemoveFriendResponse>> {
    try {
      // 입력 검증
      if (!request.userId || !request.friendUserId) {
        return Result.failure(
          new ValidationError("request", "Both user ID and friend user ID are required")
        );
      }

      if (request.userId === request.friendUserId) {
        return Result.failure(
          new ValidationError("friendUserId", "Cannot remove yourself as a friend")
        );
      }

      const userId = request.userId;
      const friendUserId = request.friendUserId;

      // 사용자 존재 확인
      const userResult = await this.userRepository.findByUserId(request.userId);
      if (!userResult.success) {
        return Result.failure(userResult.error);
      }
      if (!userResult.data) {
        return Result.failure(new NotFoundError("User not found", "userResult"));
      }

      // 친구 사용자 존재 확인
      const friendUserResult = await this.userRepository.findByUserId(request.friendUserId);
      if (!friendUserResult.success) {
        return Result.failure(friendUserResult.error);
      }
      if (!friendUserResult.data) {
        return Result.failure(
          new NotFoundError("Friend user not found", "friendUserResult")
        );
      }

      // 현재 친구 관계인지 확인
      const areFriendsResult = await this.friendRepository.areUsersFriends(userId, friendUserId);
      if (!areFriendsResult.success) {
        return Result.failure(areFriendsResult.error);
      }
      if (!areFriendsResult.data) {
        return Result.failure(
          new ConflictError("Users are not friends", "areUsersFriends", "areFriendsResult")
        );
      }

      // 양방향 친구 관계 조회 및 처리
      const friendRelation1Result = await this.friendRepository.findByUserIds(userId, friendUserId);
      const friendRelation2Result = await this.friendRepository.findByUserIds(friendUserId, userId);

      const relationUpdates: Array<{userId: string, relation: any}> = [];

      // 첫 번째 사용자의 관계 (userId 컬렉션에서)
      if (friendRelation1Result.success && friendRelation1Result.data) {
        if (friendRelation1Result.data.status === FriendStatus.ACCEPTED) {
          const removeResult = friendRelation1Result.data.remove();
          if (!removeResult.success) {
            return Result.failure(removeResult.error);
          }
          relationUpdates.push({userId: userId, relation: removeResult.data});
        }
      }

      // 두 번째 사용자의 관계 (friendUserId 컬렉션에서)
      if (friendRelation2Result.success && friendRelation2Result.data) {
        if (friendRelation2Result.data.status === FriendStatus.ACCEPTED) {
          const removeResult = friendRelation2Result.data.remove();
          if (!removeResult.success) {
            return Result.failure(removeResult.error);
          }
          relationUpdates.push({userId: friendUserId, relation: removeResult.data});
        }
      }

      if (relationUpdates.length === 0) {
        return Result.failure(
          new ConflictError("No active friend relationships to remove", "relationUpdates.length", "relationUpdates")
        );
      }

      // 제거된 관계 저장 - 각각의 올바른 컬렉션에
      for (const update of relationUpdates) {
        const saveResult = await this.friendRepository.update(update.userId, update.relation);
        if (!saveResult.success) {
          // 일부만 저장되었을 수 있으므로 에러를 반환하지만 성공한 것은 유지
          console.error("Failed to save removed friend relation:", saveResult.error);
        }
      }

      // 실제로는 데이터를 삭제하는 것이 아니라 상태를 REMOVED로 변경
      // 필요에 따라 실제 삭제를 원한다면 다음 코드를 사용:
      // await this.friendRepository.deleteByUserIds(userId, friendUserId);

      // 양쪽 사용자의 친구 수 업데이트 (백그라운드에서 수행)
      this.updateFriendCounts(request.userId, request.friendUserId);

      const now = new Date();
      return Result.success({
        success: true,
        removedAt: now.toISOString(),
      });
    } catch (error) {
      return Result.failure(
        error instanceof Error ? error : new Error("Failed to remove friend")
      );
    }
  }

  private async updateFriendCounts(userId1: string, userId2: string): Promise<void> {
    try {
      // 사용자 1의 친구 수 업데이트
      const user1FriendsResult = await this.friendRepository.countFriendsByUserId(userId1);
      if (user1FriendsResult.success) {
        const user1Result = await this.userRepository.findByUserId(userId1);
        if (user1Result.success && user1Result.data) {
          const updatedUser1 = user1Result.data.updateFriendCount(user1FriendsResult.data);
          await this.userRepository.update(updatedUser1);
        }
      }

      // 사용자 2의 친구 수 업데이트
      const user2FriendsResult = await this.friendRepository.countFriendsByUserId(userId2);
      if (user2FriendsResult.success) {
        const user2Result = await this.userRepository.findByUserId(userId2);
        if (user2Result.success && user2Result.data) {
          const updatedUser2 = user2Result.data.updateFriendCount(user2FriendsResult.data);
          await this.userRepository.update(updatedUser2);
        }
      }
    } catch (error) {
      // 친구 수 업데이트 실패는 주요 기능에 영향을 주지 않으므로 로그만 남김
      console.error("Failed to update friend counts:", error);
    }
  }
}
