import {CustomResult, Result} from "../../../core/types";
import {ValidationError, ConflictError, NotFoundError} from "../../../core/errors";
import {FriendRepository} from "../../../domain/friend/repositories/friend.repository";
import {UserProfileRepository} from "../../../domain/user/repositories/userProfile.repository";
import {UserId, FriendId, FriendStatus} from "../../../domain/friend/entities/friend.entity";

export interface RejectFriendRequestRequest {
  friendRequestId: string;
  userId: string; // 거절하는 사용자 (수신자)
}

export interface RejectFriendRequestResponse {
  friendRequestId: string;
  status: string;
  rejectedAt: string;
}

export class RejectFriendRequestUseCase {
  constructor(
    private readonly friendRepository: FriendRepository,
    private readonly userRepository: UserProfileRepository
  ) {}

  async execute(request: RejectFriendRequestRequest): Promise<CustomResult<RejectFriendRequestResponse>> {
    try {
      // 입력 검증
      if (!request.friendRequestId || !request.userId) {
        return Result.failure(
          new ValidationError("request", "Friend request ID and user ID are required")
        );
      }

      const friendId = new FriendId(request.friendRequestId);
      const userId = new UserId(request.userId);

      // 친구 요청 조회
      const friendRequestResult = await this.friendRepository.findById(friendId);
      if (!friendRequestResult.success) {
        return Result.failure(friendRequestResult.error);
      }
      if (!friendRequestResult.data) {
        return Result.failure(
          new NotFoundError("Friend request not found", "friendRequestResult")
        );
      }

      const friendRequest = friendRequestResult.data;

      // 요청 수신자가 맞는지 확인
      if (!friendRequest.isReceiver(userId)) {
        return Result.failure(
          new ValidationError("userId", "Only the request receiver can reject this friend request")
        );
      }

      // 요청 상태 확인
      if (friendRequest.status !== FriendStatus.REQUESTED) {
        return Result.failure(
          new ConflictError(
            `Cannot reject friend request. Current status: ${friendRequest.status}`,
            "friendRequest.status",
            "friendRequestResult"
          )
        );
      }

      // 사용자 존재 확인
      const userResult = await this.userRepository.findByUserId(request.userId);
      if (!userResult.success) {
        return Result.failure(userResult.error);
      }
      if (!userResult.data) {
        return Result.failure(new NotFoundError("User not found", "userResult"));
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
        friendRequestId: rejectedFriend.id.value,
        status: rejectedFriend.status,
        rejectedAt: rejectedFriend.respondedAt!.toISOString(),
      });
    } catch (error) {
      return Result.failure(
        error instanceof Error ? error : new Error("Failed to reject friend request")
      );
    }
  }
}
