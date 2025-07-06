import {onCall, HttpsError} from "firebase-functions/v2/https";
import {RUNTIME_CONFIG} from "../../core/constants";
import {Providers} from "../../config/dependencies";

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

      const result = await friendUseCases.sendFriendRequestUseCase.execute({requesterId, receiverUserId});

      if (!result.success) {
        if (result.error.message.includes("not found")) {
          throw new HttpsError("not-found", result.error.message);
        }
        if (result.error.message.includes("already")) {
          throw new HttpsError("already-exists", result.error.message);
        }
        if (result.error.message.includes("cannot") || result.error.message.includes("not accepting")) {
          throw new HttpsError("failed-precondition", result.error.message);
        }
        throw new HttpsError("internal", result.error.message);
      }

      return result.data;
    } catch (error) {
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", `Failed to send friend request: ${error instanceof Error ? error.message : "Unknown error"}`);
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

      const result = await friendUseCases.acceptFriendRequestUseCase.execute({
        requesterId: friendRequestId,
        receiverId: userId,
      });

      if (!result.success) {
        if (result.error.message.includes("not found")) {
          throw new HttpsError("not-found", result.error.message);
        }
        if (result.error.message.includes("cannot") || result.error.message.includes("Only")) {
          throw new HttpsError("permission-denied", result.error.message);
        }
        if (result.error.message.includes("Current status")) {
          throw new HttpsError("failed-precondition", result.error.message);
        }
        throw new HttpsError("internal", result.error.message);
      }

      return result.data;
    } catch (error) {
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", `Failed to accept friend request: ${error instanceof Error ? error.message : "Unknown error"}`);
    }
  }
);

// Reject Friend Request Function
interface RejectFriendRequestRequest {
  requesterId: string;
  receiverId: string;
}

export const rejectFriendRequestFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request) => {
    try {
      const {requesterId, receiverId} = request.data as RejectFriendRequestRequest;

      if (!requesterId || !receiverId) {
        throw new HttpsError("invalid-argument", "Requester ID and receiver ID are required");
      }

      const friendUseCases = Providers.getFriendProvider().create();

      const result = await friendUseCases.rejectFriendRequestUseCase.execute({
        requesterId: requesterId,
        receiverId: receiverId,
      });

      if (!result.success) {
        if (result.error.message.includes("not found")) {
          throw new HttpsError("not-found", result.error.message);
        }
        if (result.error.message.includes("cannot") || result.error.message.includes("Only")) {
          throw new HttpsError("permission-denied", result.error.message);
        }
        if (result.error.message.includes("Current status")) {
          throw new HttpsError("failed-precondition", result.error.message);
        }
        throw new HttpsError("internal", result.error.message);
      }

      return result.data;
    } catch (error) {
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", `Failed to reject friend request: ${error instanceof Error ? error.message : "Unknown error"}`);
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

      const result = await friendUseCases.removeFriendUseCase.execute({userId, friendUserId});

      if (!result.success) {
        if (result.error.message.includes("not found")) {
          throw new HttpsError("not-found", result.error.message);
        }
        if (result.error.message.includes("not friends")) {
          throw new HttpsError("failed-precondition", result.error.message);
        }
        throw new HttpsError("internal", result.error.message);
      }

      return result.data;
    } catch (error) {
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", `Failed to remove friend: ${error instanceof Error ? error.message : "Unknown error"}`);
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
      const {userId, status, limit, offset} = request.data as GetFriendsRequest;

      if (!userId) {
        throw new HttpsError("invalid-argument", "User ID is required");
      }

      const friendUseCases = Providers.getFriendProvider().create();

      const result = await friendUseCases.getFriendsUseCase.execute({
        userId,
        status: status as any,
        limit,
        offset,
      });

      if (!result.success) {
        if (result.error.message.includes("not found")) {
          throw new HttpsError("not-found", result.error.message);
        }
        throw new HttpsError("internal", result.error.message);
      }

      return result.data;
    } catch (error) {
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", `Failed to get friends: ${error instanceof Error ? error.message : "Unknown error"}`);
    }
  }
);

// Get Friend Requests Function
interface GetFriendRequestsRequest {
  userId: string;
  type: "received" | "sent";
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
      const {userId, type, limit, offset} = request.data as GetFriendRequestsRequest;

      if (!userId || !type) {
        throw new HttpsError("invalid-argument", "User ID and type are required");
      }

      if (!["received", "sent"].includes(type)) {
        throw new HttpsError("invalid-argument", "Type must be either 'received' or 'sent'");
      }

      const friendUseCases = Providers.getFriendProvider().create();

      const result = await friendUseCases.getFriendRequestsUseCase.execute({userId, type, limit, offset});

      if (!result.success) {
        if (result.error.message.includes("not found")) {
          throw new HttpsError("not-found", result.error.message);
        }
        throw new HttpsError("internal", result.error.message);
      }

      return result.data;
    } catch (error) {
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", `Failed to get friend requests: ${error instanceof Error ? error.message : "Unknown error"}`);
    }
  }
);
