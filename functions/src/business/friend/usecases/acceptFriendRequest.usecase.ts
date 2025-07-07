import {CustomResult, Result} from "../../../core/types";
import {ValidationError, ConflictError, NotFoundError} from "../../../core/errors";
import {FriendRepository} from "../../../domain/friend/repositories/friend.repository";
import {UserRepository} from "../../../domain/user/repositories/user.repository";
import {FriendEntity, FriendStatus} from "../../../domain/friend/entities/friend.entity";
import {CreateDMChannelUseCase} from "../../dm/usecases/createDMChannel.usecase";

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
    private readonly userRepository: UserRepository,
    private readonly createDMChannelUseCase?: CreateDMChannelUseCase
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

      // DM Channel 및 Wrapper 생성 (백그라운드에서 수행, 실패해도 친구 수락은 성공)
      this.createDMChannelForFriends(request.requesterId, request.receiverId, requesterResult.data, receiverResult.data);

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

  private async createDMChannelForFriends(userId1: string, userId2: string, user1: any, user2: any): Promise<void> {
    try {
      // CreateDMChannelUseCase가 주입되지 않은 경우 생략
      if (!this.createDMChannelUseCase) {
        console.log("CreateDMChannelUseCase not available, skipping DM channel creation");
        return;
      }

      // CreateDMChannelUseCase를 사용하여 DM 채널 생성
      // 참고: CreateDMChannelUseCase는 사용자 이름으로 검색하지만, 
      // 친구 수락의 경우 이미 사용자 정보를 가지고 있으므로 직접 생성 로직을 호출할 수 있음
      // 하지만 일관성을 위해 UseCase를 통해 처리
      
      // 첫 번째 사용자 관점에서 두 번째 사용자와 DM 생성
      const result1 = await this.createDMChannelUseCase.execute({
        currentUserId: userId1,
        targetUserName: user2.name
      });

      if (result1.success) {
        console.log(`Successfully created DM channel for user ${userId1} with ${user2.name}`);
      } else {
        // 이미 존재하는 DM 채널인 경우는 정상적인 상황
        if (result1.error.message.includes("already exists")) {
          console.log(`DM channel already exists between ${userId1} and ${userId2}`);
        } else {
          console.error(`Failed to create DM channel for user ${userId1}:`, result1.error);
        }
      }

    } catch (error) {
      // DM 생성 실패는 주요 기능에 영향을 주지 않으므로 로그만 남김
      console.error("Failed to create DM channel for friends:", error);
    }
  }
}
