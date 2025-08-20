import { onCall } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { 
  validateAuth, 
  handleError, 
  createLogger, 
  getOrCreateRequestId,
  withTracing,
  FRIEND_SUBCOLLECTION_STATUS
} from "../../../shared";

interface GetFriendRequestsResponse {
  received: Array<{
    id: string;
    requesterId: string;
    requesterName: string;
    requesterProfileImageUrl?: string;
    createdAt: string;
  }>;
  sent: Array<{
    id: string;
    receiverId: string;
    receiverName: string;
    receiverProfileImageUrl?: string;
    createdAt: string;
  }>;
}

export const getFriendRequests = onCall(
  {
    region: "asia-northeast3",
    memory: "256MiB",
    timeoutSeconds: 30,
  },
  async (request): Promise<GetFriendRequestsResponse> => {
    const requestId = getOrCreateRequestId(request);
    const logger = createLogger({
      requestId,
      domain: "friends",
      operation: "getFriendRequests",
      startTime: Date.now(),
    });

    try {
      return await withTracing(
        {
          requestId,
          domain: "friends",
          operation: "getFriendRequests",
          startTime: Date.now(),
        },
        async () => {
          const userId = validateAuth(request.auth);
          const firestore = admin.firestore();

          // Subcollection에서 받은 친구 요청 조회 (PENDING 상태)
          const receivedRequestsQuery = await firestore
            .collection(`users/${userId}/friends`)
            .where("status", "==", FRIEND_SUBCOLLECTION_STATUS.PENDING)
            .orderBy("createdAt", "desc")
            .get();

          // Subcollection에서 보낸 친구 요청 조회 (REQUESTED 상태)
          const sentRequestsQuery = await firestore
            .collection(`users/${userId}/friends`)
            .where("status", "==", FRIEND_SUBCOLLECTION_STATUS.REQUESTED)
            .orderBy("createdAt", "desc")
            .get();

          // 받은 요청 처리 (이미 친구 문서에 필요한 정보가 포함됨)
          const receivedRequests = receivedRequestsQuery.docs.map((doc) => {
            try {
              const requestData = doc.data();
              return {
                id: doc.id, // 요청자의 userId
                requesterId: doc.id,
                requesterName: requestData.name || "Unknown User",
                requesterProfileImageUrl: requestData.profileImageUrl,
                createdAt: requestData.createdAt?.toDate()?.toISOString() || new Date().toISOString(),
              };
            } catch (error) {
              logger.warn("Failed to process received friend request", { docId: doc.id, error });
              return null;
            }
          });

          // 보낸 요청 처리 (이미 친구 문서에 필요한 정보가 포함됨)
          const sentRequests = sentRequestsQuery.docs.map((doc) => {
            try {
              const requestData = doc.data();
              return {
                id: doc.id, // 수신자의 userId
                receiverId: doc.id,
                receiverName: requestData.name || "Unknown User",
                receiverProfileImageUrl: requestData.profileImageUrl,
                createdAt: requestData.createdAt?.toDate()?.toISOString() || new Date().toISOString(),
              };
            } catch (error) {
              logger.warn("Failed to process sent friend request", { docId: doc.id, error });
              return null;
            }
          });

          // null 값 필터링 및 타입 보장
          const validReceivedRequests = receivedRequests.filter((req): req is NonNullable<typeof req> => req !== null);
          const validSentRequests = sentRequests.filter((req): req is NonNullable<typeof req> => req !== null);

          logger.info("Friend requests retrieved successfully", {
            userId,
            receivedCount: validReceivedRequests.length,
            sentCount: validSentRequests.length,
          });

          return {
            received: validReceivedRequests,
            sent: validSentRequests,
          };
        }
      );
    } catch (error) {
      logger.error("Failed to get friend requests", error);
      throw handleError(error, "getFriendRequests");
    }
  }
);