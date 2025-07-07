import {CustomResult, Result} from "../../../core/types";
import {ValidationError, ConflictError, NotFoundError} from "../../../core/errors";
import {FriendRepository} from "../../../domain/friend/repositories/friend.repository";
import {UserRepository} from "../../../domain/user/repositories/user.repository";
import {FriendEntity, FriendStatus} from "../../../domain/friend/entities/friend.entity";

export interface AcceptFriendRequestRequest {
  requesterId: string; // 요청자 ID (Friend ID로 사용됨)
  receiverId: string; // 수락하는 사용자 (수신자)
}

export interface AcceptFriendRequestResponse {
  friendId: string;
  status: string;
  acceptedAt: string;
}

export class AcceptFriendRequestUseCase {
  constructor(
    private readonly friendRepository: FriendRepository,
    private readonly userRepository: UserRepository
  ) {}

  async execute(request: AcceptFriendRequestRequest): Promise<CustomResult<AcceptFriendRequestResponse>> {
    try {
      // 입력 검증
      if (!request.requesterId || !request.receiverId) {
        return Result.failure(new ValidationError("request", "Requester ID and receiver ID are required"));
      }

      const requesterId = request.requesterId;
      const receiverId = request.receiverId;

      // 수신자의 friends subcollection에서 요청자의 Friend 조회
      const friendRequestResult = await this.friendRepository.findByUserIds(receiverId, requesterId);
      if (!friendRequestResult.success) {
        return Result.failure(friendRequestResult.error);
      }
      if (!friendRequestResult.data) {
        return Result.failure(new NotFoundError("Friend request not found", request.requesterId));
      }

      const friendRequest = friendRequestResult.data;

      // 요청 상태 확인 (PENDING 상태여야 함)
      if (friendRequest.status !== FriendStatus.PENDING) {
        return Result.failure(new ConflictError("friendRequest", "status", friendRequest.status));
      }

      // 사용자들 존재 확인
      const receiverResult = await this.userRepository.findByUserId(request.receiverId);
      if (!receiverResult.success || !receiverResult.data) {
        return Result.failure(new NotFoundError("Receiver not found", request.receiverId));
      }

      const requesterResult = await this.userRepository.findByUserId(request.requesterId);
      if (!requesterResult.success || !requesterResult.data) {
        return Result.failure(new NotFoundError("Requester not found", request.requesterId));
      }

      // 친구 요청 수락
      const acceptResult = friendRequest.accept();
      if (!acceptResult.success) {
        return Result.failure(acceptResult.error);
      }

      const acceptedFriend = acceptResult.data;

      // 수락된 친구 요청 저장 (수신자의 subcollection에)
      const saveResult = await this.friendRepository.update(request.receiverId, acceptedFriend);
      if (!saveResult.success) {
        return Result.failure(saveResult.error);
      }

      // 요청자의 기존 friend 문서를 ACCEPTED 상태로 업데이트
      const requesterFriendResult = await this.friendRepository.findByUserIds(request.requesterId, request.receiverId);
      if (requesterFriendResult.success && requesterFriendResult.data) {
        const requesterFriend = requesterFriendResult.data;
        const acceptResult = requesterFriend.accept();
        if (acceptResult.success) {
          const updateRequesterResult = await this.friendRepository.update(request.requesterId, acceptResult.data);
          if (!updateRequesterResult.success) {
            console.error("Failed to update requester friend status:", updateRequesterResult.error);
          }
        }
      } else {
        // 요청자에게 기존 friend 문서가 없으면 새로 생성
        const reciprocalFriend = FriendEntity.fromDataSource(
          request.receiverId, // Friend ID (수신자의 ID)
          receiverResult.data.name,
          receiverResult.data.profileImageUrl,
          FriendStatus.ACCEPTED,
          friendRequest.requestedAt,
          new Date(), // acceptedAt
          new Date(), // createdAt
          new Date() // updatedAt
        );

        // 요청자의 subcollection에 저장
        const saveReciprocalResult = await this.friendRepository.save(request.requesterId, reciprocalFriend);
        if (!saveReciprocalResult.success) {
          console.error("Failed to save reciprocal friend relationship:", saveReciprocalResult.error);
        }
      }

      // 양쪽 사용자의 친구 수 업데이트 (백그라운드에서 수행)
      this.updateFriendCounts(request.requesterId, request.receiverId);

      return Result.success({
        friendId: acceptedFriend.id,
        status: acceptedFriend.status,
        acceptedAt: acceptedFriend.acceptedAt!.toISOString(),
      });
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error("Failed to accept friend request"));
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
