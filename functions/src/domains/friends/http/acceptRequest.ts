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
  IdempotencyManager,
  publish,
  FRIEND_SUBCOLLECTION_STATUS
} from "../../../shared";

interface AcceptFriendRequestRequest {
  friendUserId: string; // 친구 요청을 보낸 사용자의 ID
}

interface AcceptFriendRequestResponse {
  success: boolean;
  message: string;
}

export const acceptFriendRequest = onCall(
  {
    region: "asia-northeast3",
    memory: "256MiB",
    timeoutSeconds: 60,
  },
  async (request): Promise<AcceptFriendRequestResponse> => {
    const requestId = getOrCreateRequestId(request);
    const logger = createLogger({
      requestId,
      domain: "friends",
      operation: "acceptRequest",
      startTime: Date.now(),
    });

    try {
      return await withTracing(
        {
          requestId,
          domain: "friends",
          operation: "acceptRequest",
          startTime: Date.now(),
        },
        async () => {
          // 인증 검증
          const userId = validateAuth(request.auth);
          const { friendUserId } = request.data as AcceptFriendRequestRequest;

          // 입력 검증
          validateRequired(friendUserId, "friendUserId");

          // 등성성 체크
          const idempotencyKey = IdempotencyManager.generateKey(
            userId, 
            "accept-friend-request", 
            friendUserId
          );
          
          const canProceed = await IdempotencyManager.checkAndMark(
            idempotencyKey, 
            "accept-friend-request",
            10 // 10분 TTL
          );
          
          if (!canProceed) {
            throw new AppError("already-exists", "Friend request already processed");
          }

          try {
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

            // 요청 상태 확인
            if (pendingFriendData.status !== FRIEND_SUBCOLLECTION_STATUS.PENDING) {
              throw new AppError("invalid-argument", "Friend request is not pending");
            }

            // 요청자 측 문서도 확인
            const requesterFriendRef = firestore
              .collection(`users/${friendUserId}/friends`)
              .doc(userId);
            
            const requesterFriendDoc = await requesterFriendRef.get();

            if (!requesterFriendDoc.exists) {
              throw new AppError("not-found", "Corresponding friend request not found");
            }

            const requesterFriendData = requesterFriendDoc.data()!;

            if (requesterFriendData.status !== FRIEND_SUBCOLLECTION_STATUS.REQUESTED) {
              throw new AppError("invalid-argument", "Friend request status mismatch");
            }

            // 양방향 상태를 ACCEPTED로 업데이트
            const batch = firestore.batch();

            // 수신자 측: PENDING → ACCEPTED
            batch.update(pendingFriendRef, {
              status: FRIEND_SUBCOLLECTION_STATUS.ACCEPTED,
              acceptedAt: admin.firestore.FieldValue.serverTimestamp(),
              updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            });

            // 요청자 측: REQUESTED → ACCEPTED  
            batch.update(requesterFriendRef, {
              status: FRIEND_SUBCOLLECTION_STATUS.ACCEPTED,
              acceptedAt: admin.firestore.FieldValue.serverTimestamp(),
              updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            });

            await batch.commit();

            // DM 채널 생성을 위한 이벤트 발행 (비동기 처리)
            try {
              await publish("friends.accepted", {
                requesterId: friendUserId,
                receiverId: userId,
                friendshipId: `${userId}_${friendUserId}`,
              });
              logger.info("Published friend accepted event for DM creation", { friendUserId, userId });
            } catch (publishError) {
              logger.warn("Failed to publish friend accepted event", publishError);
              // 이벤트 발행 실패해도 친구 수락은 성공으로 처리
            }

            // 등성성 완료 마킹
            await IdempotencyManager.markCompleted(idempotencyKey);

            logger.info("Friend request accepted successfully", { 
              userId, 
              friendUserId,
              friendship: `${userId} <-> ${friendUserId}`
            });

            return {
              success: true,
              message: "Friend request accepted successfully",
            };
          } catch (error) {
            await IdempotencyManager.markFailed(idempotencyKey, error);
            throw error;
          }
        }
      );
    } catch (error) {
      logger.error("Failed to accept friend request", error);
      throw handleError(error, "acceptFriendRequest");
    }
  }
);