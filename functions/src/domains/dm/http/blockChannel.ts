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

interface BlockDMChannelRequest {
  channelId: string;
}

interface BlockDMChannelResponse {
  success: boolean;
  message: string;
}

export const blockDMChannel = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: FUNCTION_MEMORY.SMALL as any,
    timeoutSeconds: 30,
  },
  async (request): Promise<BlockDMChannelResponse> => {
    const requestId = getOrCreateRequestId(request);
    const logger = createLogger({
      requestId,
      domain: "dm",
      operation: "blockChannel",
      startTime: Date.now(),
    });

    try {
      return await withTracing(
        {
          requestId,
          domain: "dm",
          operation: "blockChannel",
          startTime: Date.now(),
        },
        async () => {
          const currentUserId = validateAuth(request.auth);
          const { channelId } = request.data as BlockDMChannelRequest;

          validateRequired(channelId, "channelId");

          const firestore = admin.firestore();

          // DM Wrapper 조회 (사용자 서브컬렉션에서)
          const wrapperRef = firestore
            .collection(COLLECTIONS.USERS)
            .doc(currentUserId)
            .collection(COLLECTIONS.DM_WRAPPERS)
            .doc(channelId);
          
          const wrapperDoc = await wrapperRef.get();

          if (!wrapperDoc.exists) {
            throw new AppError("not-found", "DM channel not found");
          }

          // 이미 차단된 경우
          const wrapperData = wrapperDoc.data();
          if (wrapperData?.isBlocked) {
            logger.info("DM channel already blocked", { channelId, currentUserId });
            return {
              success: true,
              message: "DM channel is already blocked"
            };
          }

          // 차단 상태로 업데이트
          await wrapperRef.update({
            isBlocked: true,
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          });

          logger.info("DM channel blocked successfully", { channelId, currentUserId });

          return {
            success: true,
            message: "DM channel blocked successfully"
          };
        }
      );
    } catch (error) {
      logger.error("Failed to block DM channel", error);
      throw handleError(error, "blockDMChannel");
    }
  }
);