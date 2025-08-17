import { onCall } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { 
  validateAuth, 
  handleError, 
  createLogger, 
  getOrCreateRequestId,
  withTracing
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

          // 받은 친구 요청 조회
          const receivedRequestsQuery = await firestore
            .collection("friends")
            .where("friendId", "==", userId)
            .where("status", "==", "pending")
            .orderBy("createdAt", "desc")
            .get();

          // 보낸 친구 요청 조회
          const sentRequestsQuery = await firestore
            .collection("friends")
            .where("userId", "==", userId)
            .where("status", "==", "pending")
            .orderBy("createdAt", "desc")
            .get();

          // 받은 요청 처리
          const receivedRequests = await Promise.all(
            receivedRequestsQuery.docs.map(async (doc) => {
              try {
                const requestData = doc.data();
                const requesterDoc = await firestore
                  .collection("users")
                  .doc(requestData.userId)
                  .get();

                const requesterData = requesterDoc.exists ? requesterDoc.data()! : {};

                return {
                  id: doc.id,
                  requesterId: requestData.userId,
                  requesterName: requesterData.name || "Unknown User",
                  requesterProfileImageUrl: requesterData.profileImageUrl,
                  createdAt: requestData.createdAt?.toDate()?.toISOString() || new Date().toISOString(),
                };
              } catch (error) {
                logger.warn("Failed to process received friend request", { docId: doc.id, error });
                return null;
              }
            })
          );

          // 보낸 요청 처리
          const sentRequests = await Promise.all(
            sentRequestsQuery.docs.map(async (doc) => {
              try {
                const requestData = doc.data();
                const receiverDoc = await firestore
                  .collection("users")
                  .doc(requestData.friendId)
                  .get();

                const receiverData = receiverDoc.exists ? receiverDoc.data()! : {};

                return {
                  id: doc.id,
                  receiverId: requestData.friendId,
                  receiverName: receiverData.name || "Unknown User",
                  receiverProfileImageUrl: receiverData.profileImageUrl,
                  createdAt: requestData.createdAt?.toDate()?.toISOString() || new Date().toISOString(),
                };
              } catch (error) {
                logger.warn("Failed to process sent friend request", { docId: doc.id, error });
                return null;
              }
            })
          );

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