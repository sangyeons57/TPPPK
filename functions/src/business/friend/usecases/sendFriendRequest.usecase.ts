import {CustomResult, Result} from "../../../core/types";
import {ValidationError, ConflictError, NotFoundError} from "../../../core/errors";
import {FriendRepository} from "../../../domain/friend/repositories/friend.repository";
import {UserRepository} from "../../../domain/user/repositories/user.repository";
import {FriendEntity, FriendStatus} from "../../../domain/friend/entities/friend.entity";

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
    private readonly userRepository: UserRepository
  ) {}

  async execute(request: SendFriendRequestRequest): Promise<CustomResult<SendFriendRequestResponse>> {
    try {
      // 입력 검증
      if (!request.requesterId || !request.receiverUserId) {
        return Result.failure(new ValidationError("userId", "Both requester and receiver IDs are required"));
      }

      if (request.requesterId === request.receiverUserId) {
        return Result.failure(new ValidationError("receiverUserId", "Cannot send friend request to yourself"));
      }

      const requesterId = request.requesterId;
      const receiverId = request.receiverUserId;

      // 요청자 존재 확인
      const requesterResult = await this.userRepository.findByUserId(request.requesterId);
      if (!requesterResult.success) {
        return Result.failure(requesterResult.error);
      }
      if (!requesterResult.data) {
        return Result.failure(new NotFoundError("Requester not found", "requesterResult"));
      }

      // 수신자 존재 확인
      const receiverResult = await this.userRepository.findByUserId(request.receiverUserId);
      if (!receiverResult.success) {
        return Result.failure(receiverResult.error);
      }
      if (!receiverResult.data) {
        return Result.failure(
          new NotFoundError("Receiver not found", "receiverResult")
        );
      }

      // 수신자가 친구 요청을 받을 수 있는지 확인
      if (!receiverResult.data.canReceiveRequests()) {
        return Result.failure(
          new ConflictError("User is not accepting friend requests", "canReceiveRequests", "receiverResult")
        );
      }

      // 이미 친구인지 확인
      const areFriendsResult = await this.friendRepository.areUsersFriends(requesterId, receiverId);
      if (!areFriendsResult.success) {
        return Result.failure(areFriendsResult.error);
      }
      if (areFriendsResult.data) {
        return Result.failure(
          new ConflictError("Users are already friends", "areUsersFriends", "areFriendsResult")
        );
      }

      // 기존 친구 요청 확인
      const existingRequestResult = await this.friendRepository.friendRequestExists(requesterId, receiverId);
      if (!existingRequestResult.success) {
        return Result.failure(existingRequestResult.error);
      }
      if (existingRequestResult.data) {
        return Result.failure(
          new ConflictError("Friend request already exists", "friendRequestExists", "existingRequestResult")
        );
      }

      // Android Friend.kt 구조에 따라 양방향으로 친구 요청 생성
      const now = new Date();

      // 요청자 관점: 상대방을 REQUESTED 상태로 추가 (요청자의 subcollection에 저장)
      // receiverId를 document ID로 사용
      const requesterFriend = FriendEntity.newRequest(
        receiverId,
        receiverResult.data.name,
        undefined, // Fixed path system: user_profiles/{userId}/profile.webp
        now
      );

      // 수신자 관점: 상대방을 PENDING 상태로 추가 (수신자의 subcollection에 저장)
      // requesterId를 document ID로 사용
      const receiverFriend = FriendEntity.receivedRequest(
        requesterId,
        requesterResult.data.name,
        undefined, // Fixed path system: user_profiles/{userId}/profile.webp
        now
      );

      // 요청자의 friends subcollection에 저장
      const saveRequesterResult = await this.friendRepository.save(requesterId, requesterFriend);
      if (!saveRequesterResult.success) {
        return Result.failure(saveRequesterResult.error);
      }

      // 수신자의 friends subcollection에 저장
      const saveReceiverResult = await this.friendRepository.save(receiverId, receiverFriend);
      if (!saveReceiverResult.success) {
        // 롤백을 위해 요청자 쪽 데이터 삭제 시도
        await this.friendRepository.deleteByUserIds(requesterId, receiverId);
        return Result.failure(saveReceiverResult.error);
      }

      return Result.success({
        friendRequestId: receiverId, // Document ID is the other user's ID
        status: FriendStatus.REQUESTED,
        requestedAt: now.toISOString(),
      });
    } catch (error) {
      return Result.failure(
        error instanceof Error ? error : new Error("Failed to send friend request")
      );
    }
  }
}
