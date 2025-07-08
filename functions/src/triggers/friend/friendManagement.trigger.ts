import {onCall, HttpsError} from "firebase-functions/v2/https";
import {RUNTIME_CONFIG} from "../../core/constants";
import {Providers} from "../../config/dependencies";
import {FriendStatus} from "../../domain/friend/entities/friend.entity";

// Send Friend Request Function
interface SendFriendRequestRequest {
  requesterId: string;
  receiverUserId: string;
}

export const sendFriendRequestFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request) => {
    try {
      const {requesterId, receiverUserId} = request.data as SendFriendRequestRequest;

      if (!requesterId || !receiverUserId) {
        throw new HttpsError("invalid-argument", "Requester ID and receiver user ID are required");
      }

      const friendUseCases = Providers.getFriendProvider().create();

      const result = await friendUseCases.sendFriendRequestUseCase.execute({
        requesterId,
        receiverUserId,
      });

      if (!result.success) {
        throw new HttpsError("internal", result.error.message);
      }

      return {success: true, data: result.data};
    } catch (error) {
      console.error("Error in sendFriendRequest:", error);
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", "Internal server error");
    }
  }
);

// Accept Friend Request Function
interface AcceptFriendRequestRequest {
  friendRequestId: string;
  userId: string;
}

export const acceptFriendRequestFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request) => {
    try {
      const {friendRequestId, userId} = request.data as AcceptFriendRequestRequest;

      if (!friendRequestId || !userId) {
        throw new HttpsError("invalid-argument", "Friend request ID and user ID are required");
      }

      const friendUseCases = Providers.getFriendProvider().create();

      // 1. 친구 요청 수락
      const friendResult = await friendUseCases.acceptFriendRequestUseCase.execute({
        requesterId: friendRequestId,
        receiverId: userId,
      });

      if (!friendResult.success) {
        throw new HttpsError("internal", friendResult.error.message);
      }

      // 2. DM 채널 생성 (백그라운드 작업, 실패해도 친구 수락은 성공)
      try {
        const dmUseCases = Providers.getDmProvider().create();

        // 사용자 정보 조회
        const userUseCases = Providers.getUserProvider().create();
        const requesterResult = await userUseCases.userRepository.findByUserId(friendRequestId);
        const receiverResult = await userUseCases.userRepository.findByUserId(userId);

        if (requesterResult.success && receiverResult.success && requesterResult.data && receiverResult.data) {
          await dmUseCases.createDMChannelUseCase.execute({
            currentUserId: friendRequestId,
            targetUserName: receiverResult.data.name,
          });
        }
      } catch (dmError) {
        // DM 채널 생성 실패는 로그만 남기고 무시
        console.warn("Failed to create DM channel after friend acceptance:", dmError);
      }

      return {success: true, data: friendResult.data};
    } catch (error) {
      console.error("Error in acceptFriendRequest:", error);
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", "Internal server error");
    }
  }
);

// Reject Friend Request Function
interface RejectFriendRequestRequest {
  friendRequestId: string;
  userId: string;
}

export const rejectFriendRequestFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request) => {
    try {
      const {friendRequestId, userId} = request.data as RejectFriendRequestRequest;

      if (!friendRequestId || !userId) {
        throw new HttpsError("invalid-argument", "Friend request ID and user ID are required");
      }

      const friendUseCases = Providers.getFriendProvider().create();

      const result = await friendUseCases.rejectFriendRequestUseCase.execute({
        requesterId: friendRequestId,
        receiverId: userId,
      });

      if (!result.success) {
        throw new HttpsError("internal", result.error.message);
      }

      return {success: true, data: result.data};
    } catch (error) {
      console.error("Error in rejectFriendRequest:", error);
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", "Internal server error");
    }
  }
);

// Remove Friend Function
interface RemoveFriendRequest {
  userId: string;
  friendUserId: string;
}

export const removeFriendFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request) => {
    try {
      const {userId, friendUserId} = request.data as RemoveFriendRequest;

      if (!userId || !friendUserId) {
        throw new HttpsError("invalid-argument", "User ID and friend user ID are required");
      }

      const friendUseCases = Providers.getFriendProvider().create();

      const result = await friendUseCases.removeFriendUseCase.execute({
        userId,
        friendUserId,
      });

      if (!result.success) {
        throw new HttpsError("internal", result.error.message);
      }

      return {success: true, data: result.data};
    } catch (error) {
      console.error("Error in removeFriend:", error);
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", "Internal server error");
    }
  }
);

// Get Friends Function
interface GetFriendsRequest {
  userId: string;
  status?: string;
  limit?: number;
  offset?: number;
}

export const getFriendsFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request) => {
    try {
      const {
        userId,
        status,
        limit,
        offset,
      } = request.data as GetFriendsRequest;

      if (!userId) {
        throw new HttpsError("invalid-argument", "User ID is required");
      }

      const friendUseCases = Providers.getFriendProvider().create();

      const result = await friendUseCases.getFriendsUseCase.execute({
        userId,
        status: status as FriendStatus | undefined,
        limit,
        offset,
      });

      if (!result.success) {
        throw new HttpsError("internal", result.error.message);
      }

      return {success: true, data: result.data};
    } catch (error) {
      console.error("Error in getFriends:", error);
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", "Internal server error");
    }
  }
);

// Get Friend Requests Function
interface GetFriendRequestsRequest {
  userId: string;
  type: string;
  limit?: number;
  offset?: number;
}

export const getFriendRequestsFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request) => {
    try {
      const {
        userId,
        type,
        limit,
        offset,
      } = request.data as GetFriendRequestsRequest;

      if (!userId || !type) {
        throw new HttpsError("invalid-argument", "User ID and type are required");
      }

      const friendUseCases = Providers.getFriendProvider().create();

      const result = await friendUseCases.getFriendRequestsUseCase.execute({
        userId,
        type: type as "received" | "sent",
        limit,
        offset,
      });

      if (!result.success) {
        throw new HttpsError("internal", result.error.message);
      }

      return {success: true, data: result.data};
    } catch (error) {
      console.error("Error in getFriendRequests:", error);
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", "Internal server error");
    }
  }
);
