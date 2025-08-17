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
  publish
} from "../../../shared";

interface AcceptFriendRequestRequest {
  friendRequestId: string;
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
          const { friendRequestId } = request.data as AcceptFriendRequestRequest;

          // 입력 검증
          validateRequired(friendRequestId, "friendRequestId");

          // 등성성 체크
          const idempotencyKey = IdempotencyManager.generateKey(
            userId, 
            "accept-friend-request", 
            friendRequestId
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

            // 친구 요청 조회
            const friendRequestRef = firestore.collection("friends").doc(friendRequestId);
            const friendRequestDoc = await friendRequestRef.get();

            if (!friendRequestDoc.exists) {
              throw new AppError("not-found", "Friend request not found");
            }

            const friendRequestData = friendRequestDoc.data()!;

            // 요청 수신자 검증
            if (friendRequestData.friendId !== userId) {
              throw new AppError("permission-denied", "You can only accept friend requests sent to you");
            }

            // 요청 상태 확인
            if (friendRequestData.status !== "pending") {
              throw new AppError("invalid-argument", "Friend request is not pending");
            }

            const requesterId = friendRequestData.userId;

            // 배치 작업으로 양방향 친구 관계 생성
            const batch = firestore.batch();

            // 기존 요청 업데이트 (accepted로 변경)
            batch.update(friendRequestRef, {
              status: "accepted",
              updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            });

            // 반대 방향 친구 관계 생성 (수신자 -> 요청자)
            const reverseFriendRef = firestore.collection("friends").doc();
            batch.set(reverseFriendRef, {
              id: reverseFriendRef.id,
              userId: userId,
              friendId: requesterId,
              status: "accepted",
              createdAt: admin.firestore.FieldValue.serverTimestamp(),
              updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            });

            await batch.commit();

            // DM 채널 생성을 위한 이벤트 발행 (비동기 처리)
            try {
              await publish("friends.accepted", {
                requesterId,
                receiverId: userId,
                friendRequestId,
              });
              logger.info("Published friend accepted event for DM creation", { requesterId, userId });
            } catch (publishError) {
              logger.warn("Failed to publish friend accepted event", publishError);
              // 이벤트 발행 실패해도 친구 수락은 성공으로 처리
            }

            // 등성성 완료 마킹
            await IdempotencyManager.markCompleted(idempotencyKey);

            logger.info("Friend request accepted successfully", { 
              userId, 
              requesterId, 
              friendRequestId 
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