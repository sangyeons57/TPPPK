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
  AppError,
  FRIEND_SUBCOLLECTION_STATUS
} from "../../../shared";

interface RemoveFriendRequest {
  friendUserId: string; // 제거할 친구의 사용자 ID
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
          const { friendUserId } = request.data as RemoveFriendRequest;

          validateRequired(friendUserId, "friendUserId");
          validateUserId(friendUserId);

          if (userId === friendUserId) {
            throw new AppError("invalid-argument", "Cannot remove yourself as friend");
          }

          const firestore = admin.firestore();

          // Subcollection에서 친구 관계 확인 (현재 사용자 측)
          const userFriendRef = firestore
            .collection(`users/${userId}/friends`)
            .doc(friendUserId);
          
          const userFriendDoc = await userFriendRef.get();

          if (!userFriendDoc.exists) {
            throw new AppError("not-found", "Friendship not found");
          }

          const userFriendData = userFriendDoc.data()!;

          if (userFriendData.status !== FRIEND_SUBCOLLECTION_STATUS.ACCEPTED) {
            throw new AppError("invalid-argument", "Users are not friends");
          }

          // 친구 측 문서도 확인
          const friendUserRef = firestore
            .collection(`users/${friendUserId}/friends`)
            .doc(userId);

          const friendUserDoc = await friendUserRef.get();

          if (!friendUserDoc.exists) {
            throw new AppError("not-found", "Corresponding friendship not found");
          }

          // 양방향 친구 관계 삭제
          const batch = firestore.batch();
          batch.delete(userFriendRef);
          batch.delete(friendUserRef);

          await batch.commit();

          logger.info("Friend removed successfully", { userId, friendUserId });

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