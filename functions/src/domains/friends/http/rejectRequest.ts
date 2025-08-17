import { onCall } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { 
  validateAuth, 
  validateRequired,
  handleError, 
  createLogger, 
  getOrCreateRequestId,
  withTracing,
  AppError
} from "../../../shared";

interface RejectFriendRequestRequest {
  friendRequestId: string;
}

interface RejectFriendRequestResponse {
  success: boolean;
  message: string;
}

export const rejectFriendRequest = onCall(
  {
    region: "asia-northeast3",
    memory: "256MiB",
    timeoutSeconds: 30,
  },
  async (request): Promise<RejectFriendRequestResponse> => {
    const requestId = getOrCreateRequestId(request);
    const logger = createLogger({
      requestId,
      domain: "friends",
      operation: "rejectRequest",
      startTime: Date.now(),
    });

    try {
      return await withTracing(
        {
          requestId,
          domain: "friends",
          operation: "rejectRequest",
          startTime: Date.now(),
        },
        async () => {
          const userId = validateAuth(request.auth);
          const { friendRequestId } = request.data as RejectFriendRequestRequest;

          validateRequired(friendRequestId, "friendRequestId");

          const firestore = admin.firestore();
          const friendRequestRef = firestore.collection("friends").doc(friendRequestId);
          const friendRequestDoc = await friendRequestRef.get();

          if (!friendRequestDoc.exists) {
            throw new AppError("not-found", "Friend request not found");
          }

          const friendRequestData = friendRequestDoc.data()!;

          if (friendRequestData.friendId !== userId) {
            throw new AppError("permission-denied", "You can only reject friend requests sent to you");
          }

          if (friendRequestData.status !== "pending") {
            throw new AppError("invalid-argument", "Friend request is not pending");
          }

          await friendRequestRef.update({
            status: "rejected",
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          });

          logger.info("Friend request rejected successfully", { userId, friendRequestId });

          return {
            success: true,
            message: "Friend request rejected successfully",
          };
        }
      );
    } catch (error) {
      logger.error("Failed to reject friend request", error);
      throw handleError(error, "rejectFriendRequest");
    }
  }
);