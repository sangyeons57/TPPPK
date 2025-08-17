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
  IdempotencyManager
} from "../../../shared";

interface SendFriendRequestRequest {
  receiverUserId: string;
}

interface SendFriendRequestResponse {
  success: boolean;
  friendRequestId: string;
}

export const sendFriendRequest = onCall(
  {
    region: "asia-northeast3",
    memory: "256MiB",
    timeoutSeconds: 60,
  },
  async (request): Promise<SendFriendRequestResponse> => {
    const requestId = getOrCreateRequestId(request);
    const logger = createLogger({
      requestId,
      domain: "friends",
      operation: "sendRequest",
      startTime: Date.now(),
    });

    try {
      return await withTracing(
        {
          requestId,
          domain: "friends",
          operation: "sendRequest",
          startTime: Date.now(),
        },
        async () => {
          // 인증 검증
          const requesterId = validateAuth(request.auth);
          const { receiverUserId } = request.data as SendFriendRequestRequest;

          // 입력 검증
          validateRequired(receiverUserId, "receiverUserId");
          validateUserId(receiverUserId);

          if (requesterId === receiverUserId) {
            throw new AppError("invalid-argument", "Cannot send friend request to yourself");
          }

          // 등성성 체크
          const idempotencyKey = IdempotencyManager.generateKey(
            requesterId, 
            "send-friend-request", 
            receiverUserId
          );
          
          const canProceed = await IdempotencyManager.checkAndMark(
            idempotencyKey, 
            "send-friend-request",
            10 // 10분 TTL
          );
          
          if (!canProceed) {
            throw new AppError("already-exists", "Friend request already sent or being processed");
          }

          try {
            const firestore = admin.firestore();

            // 수신자 존재 확인 및 사용자 정보 획득
            const receiverDoc = await firestore.collection("users").doc(receiverUserId).get();
            if (!receiverDoc.exists) {
              throw new AppError("not-found", "Receiver user not found");
            }
            
            const requesterDoc = await firestore.collection("users").doc(requesterId).get();
            if (!requesterDoc.exists) {
              throw new AppError("not-found", "Requester user not found");
            }

            const receiverData = receiverDoc.data()!;
            const requesterData = requesterDoc.data()!;

            // Subcollection에서 이미 친구인지 확인 (요청자 측)
            const existingFriendDoc = await firestore
              .collection(`users/${requesterId}/friends`)
              .doc(receiverUserId)
              .get();

            if (existingFriendDoc.exists) {
              const friendData = existingFriendDoc.data()!;
              if (friendData.status === "ACCEPTED") {
                throw new AppError("already-exists", "Users are already friends");
              }
              if (friendData.status === "REQUESTED" || friendData.status === "PENDING") {
                throw new AppError("already-exists", "Friend request already sent");
              }
            }

            // 수신자 측에서도 확인 (반대 방향 요청이 있는지)
            const existingReverseDoc = await firestore
              .collection(`users/${receiverUserId}/friends`)
              .doc(requesterId)
              .get();

            if (existingReverseDoc.exists) {
              const reverseData = existingReverseDoc.data()!;
              if (reverseData.status === "REQUESTED") {
                throw new AppError("already-exists", "Friend request already received from this user");
              }
            }

            // 양방향 친구 요청 생성 (Subcollection 방식)
            const batch = firestore.batch();

            // 요청자 측: REQUESTED 상태로 생성
            const requesterFriendRef = firestore
              .collection(`users/${requesterId}/friends`)
              .doc(receiverUserId);
            
            const requesterFriendData = {
              name: receiverData.name || receiverData.username || "Unknown User",
              profileImageUrl: receiverData.profileImageUrl || null,
              status: "REQUESTED",
              requestedAt: admin.firestore.FieldValue.serverTimestamp(),
              acceptedAt: null,
              createdAt: admin.firestore.FieldValue.serverTimestamp(),
              updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            };

            // 수신자 측: PENDING 상태로 생성
            const receiverFriendRef = firestore
              .collection(`users/${receiverUserId}/friends`)
              .doc(requesterId);
            
            const receiverFriendData = {
              name: requesterData.name || requesterData.username || "Unknown User",
              profileImageUrl: requesterData.profileImageUrl || null,
              status: "PENDING",
              requestedAt: admin.firestore.FieldValue.serverTimestamp(),
              acceptedAt: null,
              createdAt: admin.firestore.FieldValue.serverTimestamp(),
              updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            };

            batch.set(requesterFriendRef, requesterFriendData);
            batch.set(receiverFriendRef, receiverFriendData);

            await batch.commit();

            // 등성성 완료 마킹
            await IdempotencyManager.markCompleted(idempotencyKey);

            logger.info("Friend request sent successfully", { 
              requesterId, 
              receiverUserId, 
              requesterFriendId: requesterFriendRef.id,
              receiverFriendId: receiverFriendRef.id
            });

            return {
              success: true,
              friendRequestId: receiverUserId, // 수신자의 ID를 반환 (Android에서 사용)
            };
          } catch (error) {
            await IdempotencyManager.markFailed(idempotencyKey, error);
            throw error;
          }
        }
      );
    } catch (error) {
      logger.error("Failed to send friend request", error);
      throw handleError(error, "sendFriendRequest");
    }
  }
);