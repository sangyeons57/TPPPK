import {CustomResult, Result} from "../../../core/types";
import {ValidationError, NotFoundError} from "../../../core/errors";
import {FriendRepository} from "../../../domain/friend/repositories/friend.repository";
import {UserRepository} from "../../../domain/user/repositories/user.repository";
import {UserId, FriendStatus} from "../../../domain/friend/entities/friend.entity";
import {UserEntity} from "../../../domain/user/entities/user.entity";

export interface GetFriendsRequest {
  userId: string;
  status?: FriendStatus;
  limit?: number;
  offset?: number;
}

export interface FriendInfo {
  friendId: string;
  userId: string;
  user: Partial<UserEntity>;
  status: string;
  friendsSince?: string;
  requestedAt: string;
}

export interface GetFriendsResponse {
  friends: FriendInfo[];
  totalCount: number;
  hasMore: boolean;
}

export class GetFriendsUseCase {
  constructor(
    private readonly friendRepository: FriendRepository,
    private readonly userRepository: UserRepository
  ) {}

  async execute(request: GetFriendsRequest): Promise<CustomResult<GetFriendsResponse>> {
    try {
      // 입력 검증
      if (!request.userId) {
        return Result.failure(new ValidationError("userId", "User ID is required"));
      }

      const userId = new UserId(request.userId);
      const limit = request.limit || 50;
      const offset = request.offset || 0;

      // 사용자 존재 확인
      const userResult = await this.userRepository.findByUserId(request.userId);
      if (!userResult.success) {
        return Result.failure(userResult.error);
      }
      if (!userResult.data) {
        return Result.failure(new NotFoundError("User not found", request.userId));
      }

      // 친구 관계 조회
      const friendsResult = await this.friendRepository.findFriendsByUserId(
        userId,
        request.status || FriendStatus.ACCEPTED
      );
      if (!friendsResult.success) {
        return Result.failure(friendsResult.error);
      }

      const friendRelations = friendsResult.data;

      // 페이징 적용
      const paginatedRelations = friendRelations.slice(offset, offset + limit);
      const hasMore = friendRelations.length > offset + limit;

      // 친구 사용자 정보 조회
      const friendInfos: FriendInfo[] = [];

      for (const relation of paginatedRelations) {
        // 친구의 UserId 결정 (현재 사용자가 아닌 상대방)
        const friendUserId = relation.isRequester(userId) ? relation.friendUserId.value : relation.userId.value;

        // 친구 사용자 정보 조회
        const friendUserResult = await this.userRepository.findByUserId(friendUserId);
        if (friendUserResult.success && friendUserResult.data) {
          const friendUser = friendUserResult.data;

          friendInfos.push({
            friendId: relation.id.value,
            userId: friendUserId,
            user: {
              id: friendUser.id,
              name: friendUser.name,
              profileImageUrl: friendUser.profileImageUrl,
              userStatus: friendUser.userStatus
            },
            status: relation.status,
            friendsSince:
              relation.status === FriendStatus.ACCEPTED &&
              relation.respondedAt ? relation.respondedAt.toISOString() : undefined,
            requestedAt: relation.requestedAt.toISOString(),
          });
        }
      }

      return Result.success({
        friends: friendInfos,
        totalCount: friendRelations.length,
        hasMore,
      });
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error("Failed to get friends"));
    }
  }
}
