import { onCall } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { 
  validateAuth, 
  validateRequired,
  validateUserId,
  handleError, 
  createLogger, 
  getOrCreateRequestId,
  withTracing,
  AppError
} from "../../../shared";

interface RemoveFriendRequest {
  friendId: string;
}

interface RemoveFriendResponse {
  success: boolean;
  message: string;
}

export const removeFriend = onCall(
  {
    region: "asia-northeast3",
    memory: "256MiB",
    timeoutSeconds: 30,
  },
  async (request): Promise<RemoveFriendResponse> => {
    const requestId = getOrCreateRequestId(request);
    const logger = createLogger({
      requestId,
      domain: "friends",
      operation: "removeFriend",
      startTime: Date.now(),
    });

    try {
      return await withTracing(
        {
          requestId,
          domain: "friends",
          operation: "removeFriend",
          startTime: Date.now(),
        },
        async () => {
          const userId = validateAuth(request.auth);
          const { friendId } = request.data as RemoveFriendRequest;

          validateRequired(friendId, "friendId");
          validateUserId(friendId);

          if (userId === friendId) {
            throw new AppError("invalid-argument", "Cannot remove yourself as friend");
          }

          const firestore = admin.firestore();

          // 양방향 친구 관계 조회 및 삭제
          const friendshipsQuery = await firestore
            .collection("friends")
            .where("status", "==", "accepted")
            .get();

          const friendshipsToDelete: admin.firestore.DocumentReference[] = [];

          friendshipsQuery.docs.forEach(doc => {
            const data = doc.data();
            if (
              (data.userId === userId && data.friendId === friendId) ||
              (data.userId === friendId && data.friendId === userId)
            ) {
              friendshipsToDelete.push(doc.ref);
            }
          });

          if (friendshipsToDelete.length === 0) {
            throw new AppError("not-found", "Friendship not found");
          }

          // 배치로 양방향 친구 관계 삭제
          const batch = firestore.batch();
          friendshipsToDelete.forEach(ref => {
            batch.delete(ref);
          });

          await batch.commit();

          logger.info("Friend removed successfully", { userId, friendId, deletedCount: friendshipsToDelete.length });

          return {
            success: true,
            message: "Friend removed successfully",
          };
        }
      );
    } catch (error) {
      logger.error("Failed to remove friend", error);
      throw handleError(error, "removeFriend");
    }
  }
);