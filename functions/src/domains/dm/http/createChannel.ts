import { onCall } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { 
  validateAuth, 
  validateRequired,
  validateUsername,
  handleError, 
  createLogger, 
  getOrCreateRequestId,
  withTracing,
  AppError,
  COLLECTIONS,
  RUNTIME_CONFIG,
  FUNCTION_MEMORY,
  IdempotencyManager
} from "../../../shared";

interface CreateDMChannelRequest {
  targetUserName: string;
}

interface CreateDMChannelResponse {
  success: boolean;
  channelId: string;
  dmChannelWrapper: {
    id: string;
    channelId: string;
    targetUserId: string;
    targetUserName: string;
    isBlocked: boolean;
  };
}

export const createDMChannel = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: FUNCTION_MEMORY.SMALL as any,
    timeoutSeconds: 60,
  },
  async (request): Promise<CreateDMChannelResponse> => {
    const requestId = getOrCreateRequestId(request);
    const logger = createLogger({
      requestId,
      domain: "dm",
      operation: "createChannel",
      startTime: Date.now(),
    });

    try {
      return await withTracing(
        {
          requestId,
          domain: "dm",
          operation: "createChannel",
          startTime: Date.now(),
        },
        async () => {
          const currentUserId = validateAuth(request.auth);
          const { targetUserName } = request.data as CreateDMChannelRequest;

          validateRequired(targetUserName, "targetUserName");
          validateUsername(targetUserName);

          // 등성성 체크
          const idempotencyKey = IdempotencyManager.generateKey(
            currentUserId, 
            "create-dm-channel", 
            targetUserName
          );
          
          const canProceed = await IdempotencyManager.checkAndMark(
            idempotencyKey, 
            "create-dm-channel",
            10
          );
          
          if (!canProceed) {
            throw new AppError("already-exists", "DM channel creation already in progress");
          }

          try {
            const firestore = admin.firestore();

            // 대상 사용자 조회
            const targetUserQuery = await firestore
              .collection(COLLECTIONS.USERS)
              .where("name", "==", targetUserName)
              .limit(1)
              .get();

            if (targetUserQuery.empty) {
              throw new AppError("not-found", "Target user not found");
            }

            const targetUserDoc = targetUserQuery.docs[0];
            const targetUserId = targetUserDoc.id;
            const targetUserData = targetUserDoc.data();

            if (currentUserId === targetUserId) {
              throw new AppError("invalid-argument", "Cannot create DM with yourself");
            }

            // 기존 DM 채널 확인
            const existingChannelQuery = await firestore
              .collection(COLLECTIONS.DM_CHANNELS)
              .where("participants", "array-contains", currentUserId)
              .get();

            let existingChannelId: string | null = null;

            for (const doc of existingChannelQuery.docs) {
              const data = doc.data();
              if (data.participants.includes(targetUserId)) {
                existingChannelId = doc.id;
                break;
              }
            }

            let channelId: string;

            if (existingChannelId) {
              channelId = existingChannelId;
              logger.info("Using existing DM channel", { channelId, currentUserId, targetUserId });
            } else {
              // 새 DM 채널 생성
              const channelRef = firestore.collection(COLLECTIONS.DM_CHANNELS).doc();
              await channelRef.set({
                id: channelRef.id,
                participants: [currentUserId, targetUserId],
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                updatedAt: admin.firestore.FieldValue.serverTimestamp(),
              });

              channelId = channelRef.id;
              logger.info("Created new DM channel", { channelId, currentUserId, targetUserId });
            }

            // DM Wrapper 생성 (현재 사용자용)
            const wrapperRef = firestore.collection(COLLECTIONS.DM_WRAPPERS).doc();
            const wrapperData = {
              id: wrapperRef.id,
              userId: currentUserId,
              channelId: channelId,
              targetUserId: targetUserId,
              targetUserName: targetUserData.name,
              isBlocked: false,
              createdAt: admin.firestore.FieldValue.serverTimestamp(),
              updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            };

            await wrapperRef.set(wrapperData);

            await IdempotencyManager.markCompleted(idempotencyKey);

            logger.info("DM channel created successfully", { 
              channelId, 
              wrapperId: wrapperRef.id,
              currentUserId, 
              targetUserId 
            });

            return {
              success: true,
              channelId,
              dmChannelWrapper: {
                id: wrapperRef.id,
                channelId,
                targetUserId,
                targetUserName: targetUserData.name,
                isBlocked: false,
              },
            };
          } catch (error) {
            await IdempotencyManager.markFailed(idempotencyKey, error);
            throw error;
          }
        }
      );
    } catch (error) {
      logger.error("Failed to create DM channel", error);
      throw handleError(error, "createDMChannel");
    }
  }
);