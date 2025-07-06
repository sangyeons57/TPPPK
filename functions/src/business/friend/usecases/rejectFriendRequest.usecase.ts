import {CustomResult, Result} from "../../../core/types";
import {ValidationError, ConflictError, NotFoundError} from "../../../core/errors";
import {FriendRepository} from "../../../domain/friend/repositories/friend.repository";
import {UserRepository} from "../../../domain/user/repositories/user.repository";
import {FriendStatus} from "../../../domain/friend/entities/friend.entity";

export interface RejectFriendRequestRequest {
  requesterId: string; // 요청자 ID (Friend ID로 사용됨)
  receiverId: string; // 거절하는 사용자 (수신자)
}

export interface RejectFriendRequestResponse {
  friendId: string;
  status: string;
  rejectedAt: string;
}

export class RejectFriendRequestUseCase {
  constructor(
    private readonly friendRepository: FriendRepository,
    private readonly userRepository: UserRepository
  ) {}

  async execute(request: RejectFriendRequestRequest): Promise<CustomResult<RejectFriendRequestResponse>> {
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

      // 사용자 존재 확인
      const userResult = await this.userRepository.findByUserId(request.receiverId);
      if (!userResult.success) {
        return Result.failure(userResult.error);
      }
      if (!userResult.data) {
        return Result.failure(new NotFoundError("User not found", request.receiverId));
      }

      // 친구 요청 거절
      const rejectResult = friendRequest.reject();
      if (!rejectResult.success) {
        return Result.failure(rejectResult.error);
      }

      const rejectedFriend = rejectResult.data;

      // 거절된 친구 요청 저장
      const saveResult = await this.friendRepository.update(rejectedFriend);
      if (!saveResult.success) {
        return Result.failure(saveResult.error);
      }

      return Result.success({
        friendId: rejectedFriend.id,
        status: rejectedFriend.status,
        rejectedAt: rejectedFriend.updatedAt.toISOString(),
      });
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error("Failed to reject friend request"));
    }
  }
}
