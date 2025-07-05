import { CustomResult, Result } from '../../core/types';
import { ValidationError, NotFoundError } from '../../core/errors';
import { FriendRepository } from '../../domain/friend/friend.repository';
import { UserProfileRepository } from '../../domain/user/userProfile.repository';
import { UserId } from '../../domain/friend/friend.entity';
import { UserSearchProfile } from '../../domain/user/user.entity';

export interface GetFriendRequestsRequest {
  userId: string;
  type: 'received' | 'sent';
  limit?: number;
  offset?: number;
}

export interface FriendRequestInfo {
  requestId: string;
  requesterUserId: string;
  receiverUserId: string;
  requester?: UserSearchProfile;
  receiver?: UserSearchProfile;
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
    private readonly userRepository: UserProfileRepository
  ) {}

  async execute(request: GetFriendRequestsRequest): Promise<CustomResult<GetFriendRequestsResponse>> {
    try {
      // 입력 검증
      if (!request.userId) {
        return Result.failure(new ValidationError('userId', 'User ID is required'));
      }

      if (!['received', 'sent'].includes(request.type)) {
        return Result.failure(new ValidationError('type', 'Type must be either "received" or "sent"'));
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
        return Result.failure(new NotFoundError('User not found'));
      }

      // 친구 요청 조회
      let requestsResult;
      if (request.type === 'received') {
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
        const requestInfo: FriendRequestInfo = {
          requestId: friendRequest.id.value,
          requesterUserId: friendRequest.userId.value,
          receiverUserId: friendRequest.friendUserId.value,
          status: friendRequest.status,
          requestedAt: friendRequest.requestedAt.toISOString(),
          respondedAt: friendRequest.respondedAt?.toISOString()
        };

        // 요청자 정보 조회 (received 타입일 때 또는 sent 타입에서 상대방 정보)
        if (request.type === 'received' || request.type === 'sent') {
          const requesterResult = await this.userRepository.findByUserId(friendRequest.userId.value);
          if (requesterResult.success && requesterResult.data) {
            requestInfo.requester = requesterResult.data.toSearchProfile();
          }

          const receiverResult = await this.userRepository.findByUserId(friendRequest.friendUserId.value);
          if (receiverResult.success && receiverResult.data) {
            requestInfo.receiver = receiverResult.data.toSearchProfile();
          }
        }

        requestInfos.push(requestInfo);
      }

      return Result.success({
        requests: requestInfos,
        totalCount: friendRequests.length,
        hasMore
      });
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to get friend requests'));
    }
  }
}