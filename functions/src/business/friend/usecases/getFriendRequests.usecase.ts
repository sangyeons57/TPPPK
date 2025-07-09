import {CustomResult, Result} from "../../../core/types";
import {ValidationError, NotFoundError} from "../../../core/errors";
import {FriendRepository} from "../../../domain/friend/repositories/friend.repository";
import {UserRepository} from "../../../domain/user/repositories/user.repository";
import {UserEntity} from "../../../domain/user/entities/user.entity";

export interface GetFriendRequestsRequest {
  userId: string;
  type: "received" | "sent";
  limit?: number;
  offset?: number;
}

export interface FriendRequestInfo {
  requestId: string;
  requesterUserId: string;
  receiverUserId: string;
  requester?: Partial<UserEntity>;
  receiver?: Partial<UserEntity>;
  status: string;
  requestedAt: string;
  respondedAt?: string;
}

export interface GetFriendRequestsResponse {
  requests: FriendRequestInfo[];
  totalCount: number;
  hasMore: boolean;
}

export class GetFriendRequestsUseCase {
  constructor(
    private readonly friendRepository: FriendRepository,
    private readonly userRepository: UserRepository
  ) {}

  async execute(request: GetFriendRequestsRequest): Promise<CustomResult<GetFriendRequestsResponse>> {
    try {
      // 입력 검증
      if (!request.userId) {
        return Result.failure(new ValidationError("userId", "User ID is required"));
      }

      if (!["received", "sent"].includes(request.type)) {
        return Result.failure(new ValidationError("type", "Type must be either 'received' or 'sent'"));
      }

      const userId = request.userId;
      const limit = request.limit || 50;
      const offset = request.offset || 0;

      // 사용자 존재 확인
      const userResult = await this.userRepository.findByUserId(userId);
      if (!userResult.success) {
        return Result.failure(userResult.error);
      }
      if (!userResult.data) {
        return Result.failure(new NotFoundError("User not found", userId));
      }

      // 친구 요청 조회
      let requestsResult;
      if (request.type === "received") {
        requestsResult = await this.friendRepository.findReceivedFriendRequests(userId);
      } else {
        requestsResult = await this.friendRepository.findSentFriendRequests(userId);
      }

      if (!requestsResult.success) {
        return Result.failure(requestsResult.error);
      }

      const friendRequests = requestsResult.data;

      // 페이징 적용
      const paginatedRequests = friendRequests.slice(offset, offset + limit);
      const hasMore = friendRequests.length > offset + limit;

      // 친구 요청 정보 구성
      const requestInfos: FriendRequestInfo[] = [];

      for (const friendRequest of paginatedRequests) {
        // 필수 필드들이 존재하는지 확인
        if (!friendRequest.requestedAt) {
          continue; // 불완전한 데이터는 건너뛰기
        }

        const requestInfo: FriendRequestInfo = {
          requestId: friendRequest.id,
          requesterUserId: request.type === "received" ? friendRequest.id : request.userId, // Friend ID가 상대방 ID
          receiverUserId: request.type === "received" ? request.userId : friendRequest.id,
          status: friendRequest.status,
          requestedAt: friendRequest.requestedAt.toISOString(),
          respondedAt: friendRequest.acceptedAt?.toISOString(), // acceptedAt을 respondedAt으로 사용
        };

        // 상대방 정보 조회 (Friend ID가 상대방의 userId)
        const otherUserId = friendRequest.id;
        const otherUserResult = await this.userRepository.findByUserId(otherUserId);
        if (otherUserResult.success && otherUserResult.data) {
          const otherUserInfo = {
            id: otherUserResult.data.id,
            name: otherUserResult.data.name,
            profileImageUrl: undefined, // Fixed path system: user_profiles/{userId}/profile.webp
            userStatus: otherUserResult.data.userStatus,
          };

          if (request.type === "received") {
            requestInfo.requester = otherUserInfo; // 상대방이 요청자
          } else {
            requestInfo.receiver = otherUserInfo; // 상대방이 수신자
          }
        }

        // 현재 사용자 정보 조회
        const currentUserResult = await this.userRepository.findByUserId(request.userId);
        if (currentUserResult.success && currentUserResult.data) {
          const currentUserInfo = {
            id: currentUserResult.data.id,
            name: currentUserResult.data.name,
            profileImageUrl: undefined, // Fixed path system: user_profiles/{userId}/profile.webp
            userStatus: currentUserResult.data.userStatus,
          };

          if (request.type === "received") {
            requestInfo.receiver = currentUserInfo; // 현재 사용자가 수신자
          } else {
            requestInfo.requester = currentUserInfo; // 현재 사용자가 요청자
          }
        }

        requestInfos.push(requestInfo);
      }

      return Result.success({
        requests: requestInfos,
        totalCount: friendRequests.length,
        hasMore,
      });
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error("Failed to get friend requests"));
    }
  }
}
