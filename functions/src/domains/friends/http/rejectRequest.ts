import { onCall } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { 
  validateAuth, 
  validateRequired,
  handleError, 
  createLogger, 
  getOrCreateRequestId,
  withTracing,
  AppError,
  FRIEND_SUBCOLLECTION_STATUS
} from "../../../shared";

interface RejectFriendRequestRequest {
  friendUserId: string; // 친구 요청을 보낸 사용자의 ID
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
          const { friendUserId } = request.data as RejectFriendRequestRequest;

          validateRequired(friendUserId, "friendUserId");

          const firestore = admin.firestore();
          
          // Subcollection에서 친구 요청 조회 (수신자 관점)
          const pendingFriendRef = firestore
            .collection(`users/${userId}/friends`)
            .doc(friendUserId);
          
          const pendingFriendDoc = await pendingFriendRef.get();

          if (!pendingFriendDoc.exists) {
            throw new AppError("not-found", "Friend request not found");
          }

          const pendingFriendData = pendingFriendDoc.data()!;

          if (pendingFriendData.status !== FRIEND_SUBCOLLECTION_STATUS.PENDING) {
            throw new AppError("invalid-argument", "Friend request is not pending");
          }

          // 요청자 측 문서도 확인
          const requesterFriendRef = firestore
            .collection(`users/${friendUserId}/friends`)
            .doc(userId);

          // 양방향 문서 삭제 (거절 시 관계 완전 제거)
          const batch = firestore.batch();
          batch.delete(pendingFriendRef);
          batch.delete(requesterFriendRef);
          
          await batch.commit();

          logger.info("Friend request rejected successfully", { userId, friendUserId });

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