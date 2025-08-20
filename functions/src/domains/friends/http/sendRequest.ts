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
  IdempotencyManager,
  FRIEND_SUBCOLLECTION_STATUS
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

          // 먼저 실제 데이터 기준으로 Subcollection에서 중복 확인 (요청자 측)
          const existingFriendDoc = await firestore
            .collection(`users/${requesterId}/friends`)
            .doc(receiverUserId)
            .get();

          if (existingFriendDoc.exists) {
            const friendData = existingFriendDoc.data()!;
            if (friendData.status === FRIEND_SUBCOLLECTION_STATUS.ACCEPTED) {
              throw new AppError("already-exists", "Users are already friends");
            }
            if (friendData.status === FRIEND_SUBCOLLECTION_STATUS.REQUESTED || friendData.status === FRIEND_SUBCOLLECTION_STATUS.PENDING) {
              throw new AppError("already-exists", "Friend request already exists in subcollection");
            }
          }

          // 수신자 측에서도 확인 (반대 방향 요청이 있는지)
          const existingReverseDoc = await firestore
            .collection(`users/${receiverUserId}/friends`)
            .doc(requesterId)
            .get();

          if (existingReverseDoc.exists) {
            const reverseData = existingReverseDoc.data()!;
            if (reverseData.status === FRIEND_SUBCOLLECTION_STATUS.REQUESTED) {
              throw new AppError("already-exists", "Friend request already received from this user");
            }
          }

          // 실제 데이터 확인 후 Idempotency 체크 (동시성 제어용)
          const idempotencyKey = IdempotencyManager.generateKey(
            requesterId, 
            "send-friend-request", 
            receiverUserId
          );
          
          const canProceed = await IdempotencyManager.checkAndMark(
            idempotencyKey, 
            "send-friend-request",
            2 // 2분 TTL (친구 요청은 빠른 재시도 필요)
          );
          
          if (!canProceed) {
            throw new AppError("already-exists", "Friend request is currently being processed (idempotency check)");
          }

          try {

            // 양방향 친구 요청 생성 (Subcollection 방식)
            const batch = firestore.batch();

            // 요청자 측: REQUESTED 상태로 생성
            const requesterFriendRef = firestore
              .collection(`users/${requesterId}/friends`)
              .doc(receiverUserId);
            
            const requesterFriendData = {
              name: receiverData.name || receiverData.username || "Unknown User",
              profileImageUrl: receiverData.profileImageUrl || null,
              status: FRIEND_SUBCOLLECTION_STATUS.REQUESTED,
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
              status: FRIEND_SUBCOLLECTION_STATUS.PENDING,
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