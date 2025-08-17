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
  COLLECTIONS,
  RUNTIME_CONFIG,
  FUNCTION_MEMORY
} from "../../../shared";

interface UnblockDMChannelRequest {
  channelId: string;
}

interface UnblockDMChannelResponse {
  success: boolean;
  message: string;
}

export const unblockDMChannel = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: FUNCTION_MEMORY.SMALL as any,
    timeoutSeconds: 30,
  },
  async (request): Promise<UnblockDMChannelResponse> => {
    const requestId = getOrCreateRequestId(request);
    const logger = createLogger({
      requestId,
      domain: "dm",
      operation: "unblockChannel",
      startTime: Date.now(),
    });

    try {
      return await withTracing(
        {
          requestId,
          domain: "dm",
          operation: "unblockChannel",
          startTime: Date.now(),
        },
        async () => {
          const currentUserId = validateAuth(request.auth);
          const { channelId } = request.data as UnblockDMChannelRequest;

          validateRequired(channelId, "channelId");

          const firestore = admin.firestore();

          // DM Wrapper 조회
          const wrapperQuery = await firestore
            .collection(COLLECTIONS.DM_WRAPPERS)
            .where("userId", "==", currentUserId)
            .where("channelId", "==", channelId)
            .limit(1)
            .get();

          if (wrapperQuery.empty) {
            throw new AppError("not-found", "DM channel not found");
          }

          const wrapperDoc = wrapperQuery.docs[0];

          // 이미 차단 해제된 경우
          if (!wrapperDoc.data().isBlocked) {
            logger.info("DM channel already unblocked", { channelId, currentUserId });
            return {
              success: true,
              message: "DM channel is already unblocked"
            };
          }

          // 차단 해제
          await wrapperDoc.ref.update({
            isBlocked: false,
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          });

          logger.info("DM channel unblocked successfully", { channelId, currentUserId });

          return {
            success: true,
            message: "DM channel unblocked successfully"
          };
        }
      );
    } catch (error) {
      logger.error("Failed to unblock DM channel", error);
      throw handleError(error, "unblockDMChannel");
    }
  }
);