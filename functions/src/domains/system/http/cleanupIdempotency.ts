import { onCall } from "firebase-functions/v2/https";
import { 
  validateAuth, 
  handleError, 
  createLogger, 
  getOrCreateRequestId,
  withTracing,
  IdempotencyManager
} from "../../../shared";

interface CleanupIdempotencyRequest {
  userId?: string;  // 특정 사용자의 실패한 친구 요청만 클린업 (선택적)
  cleanupAll?: boolean;  // 모든 만료된 키 클린업
}

interface CleanupIdempotencyResponse {
  success: boolean;
  message: string;
  cleanedCount?: number;
}

export const cleanupIdempotency = onCall(
  {
    region: "asia-northeast3",
    memory: "256MiB",
    timeoutSeconds: 30,
  },
  async (request): Promise<CleanupIdempotencyResponse> => {
    const requestId = getOrCreateRequestId(request);
    const logger = createLogger({
      requestId,
      domain: "system",
      operation: "cleanupIdempotency",
      startTime: Date.now(),
    });

    try {
      return await withTracing(
        {
          requestId,
          domain: "system",
          operation: "cleanupIdempotency",
          startTime: Date.now(),
        },
        async () => {
          const userId = validateAuth(request.auth);
          const { userId: targetUserId, cleanupAll } = request.data as CleanupIdempotencyRequest;

          if (cleanupAll) {
            // 모든 만료된 키 클린업
            await IdempotencyManager.cleanupExpired();
            logger.info("Cleaned up all expired idempotency keys");
            
            return {
              success: true,
              message: "All expired idempotency keys cleaned up successfully",
            };
          } else {
            // 특정 사용자 또는 현재 사용자의 실패한 친구 요청 클린업
            const userToCleanup = targetUserId || userId;
            await IdempotencyManager.cleanupFailedFriendRequests(userToCleanup);
            
            logger.info(`Cleaned up failed friend requests for user: ${userToCleanup}`);
            
            return {
              success: true,
              message: `Failed friend request keys cleaned up successfully for user: ${userToCleanup}`,
            };
          }
        }
      );
    } catch (error) {
      logger.error("Failed to cleanup idempotency keys", error);
      throw handleError(error, "cleanupIdempotency");
    }
  }
);