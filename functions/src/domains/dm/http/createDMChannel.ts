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
  FUNCTION_MEMORY,
  IdempotencyManager,
} from "../../../shared";

interface CreateDMChannelRequest {
  targetUserId: string;
}

interface CreateDMChannelResponse {
  success: boolean;
  channelId: string;
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
      operation: "createDMChannel",
      startTime: Date.now(),
    });

    try {
      return await withTracing(
        { requestId, domain: "dm", operation: "createDMChannel", startTime: Date.now() },
        async () => {
          const currentUserId = validateAuth(request.auth);
          const { targetUserId } = request.data as CreateDMChannelRequest;

          validateRequired(targetUserId, "targetUserId");
          
          // Enhanced targetUserId validation
          if (typeof targetUserId !== 'string') {
            throw new AppError("invalid-argument", `targetUserId must be a string, got: ${typeof targetUserId}`);
          }
          
          if (targetUserId.trim() === '') {
            throw new AppError("invalid-argument", "targetUserId cannot be empty or whitespace");
          }
          
          logger.info("DM channel creation request", { 
            currentUserId, 
            targetUserId,
            targetUserIdLength: targetUserId.length,
            targetUserIdTrimmed: targetUserId.trim()
          });

          if (currentUserId === targetUserId) {
            throw new AppError("invalid-argument", "Cannot create DM with yourself");
          }

          const idempotencyKey = IdempotencyManager.generateKey(
            currentUserId,
            "create-dm-channel",
            targetUserId
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

            // Load both users (to populate wrapper names)
            logger.info("Loading user documents", { currentUserId, targetUserId });
            const [currentUserSnap, targetUserSnap] = await Promise.all([
              firestore.collection(COLLECTIONS.USERS).doc(currentUserId).get(),
              firestore.collection(COLLECTIONS.USERS).doc(targetUserId).get(),
            ]);

            logger.info("User documents loaded", { 
              currentUserExists: currentUserSnap.exists, 
              targetUserExists: targetUserSnap.exists,
              targetUserId,
              targetUserIdLength: targetUserId.length,
              targetUserIdType: typeof targetUserId
            });

            if (!currentUserSnap.exists) {
              logger.error("Current user document not found", { currentUserId });
              throw new AppError("not-found", `Current user not found: ${currentUserId}`);
            }

            if (!targetUserSnap.exists) {
              logger.error("Target user document not found", { 
                targetUserId,
                targetUserIdLength: targetUserId.length,
                targetUserIdType: typeof targetUserId,
                isEmptyString: targetUserId === "",
                isBlank: targetUserId.trim() === "",
                collection: COLLECTIONS.USERS
              });
              throw new AppError("not-found", `Target user not found: ${targetUserId}. Please verify the user ID is correct.`);
            }

            const currentUserData = currentUserSnap.data() || {};
            const targetUserData = targetUserSnap.data()!;

            // Deterministic channel id
            const [u1, u2] = [currentUserId, targetUserId].sort();
            const channelId = `dm_${u1}_${u2}`;

            const channelRef = firestore.collection(COLLECTIONS.DM_CHANNELS).doc(channelId);
            const currentWrapperRef = firestore
              .collection(COLLECTIONS.USERS)
              .doc(currentUserId)
              .collection(COLLECTIONS.DM_WRAPPERS)
              .doc(channelId);
            const targetWrapperRef = firestore
              .collection(COLLECTIONS.USERS)
              .doc(targetUserId)
              .collection(COLLECTIONS.DM_WRAPPERS)
              .doc(channelId);

            const channelSnap = await channelRef.get();

            const batch = firestore.batch();

            if (!channelSnap.exists) {
              batch.set(channelRef, {
                participants: [currentUserId, targetUserId],
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                updatedAt: admin.firestore.FieldValue.serverTimestamp(),
              } as any);
              logger.info("Created new DM channel (batched)", { channelId, currentUserId, targetUserId });
            } else {
              logger.info("Using existing DM channel", { channelId, currentUserId, targetUserId });
            }

            const nowTs = admin.firestore.FieldValue.serverTimestamp();

            // Wrapper for current user
            batch.set(
              currentWrapperRef,
              {
                channelId,
                otherUserId: targetUserId,
                otherUserName: targetUserData.name ?? null,
                isBlocked: false,
                createdAt: nowTs,
                updatedAt: nowTs,
              } as any,
              { merge: true } as any
            );

            // Wrapper for target user
            batch.set(
              targetWrapperRef,
              {
                channelId,
                otherUserId: currentUserId,
                otherUserName: currentUserData.name ?? null,
                isBlocked: false,
                createdAt: nowTs,
                updatedAt: nowTs,
              } as any,
              { merge: true } as any
            );

            logger.info("Committing batch transaction", {
              channelId,
              currentUserId,
              targetUserId,
              isNewChannel: !channelSnap.exists
            });

            await batch.commit();
            logger.info("Batch transaction committed successfully");

            await IdempotencyManager.markCompleted(idempotencyKey);
            logger.info("Idempotency key marked as completed");

            logger.info("DM channel and wrappers created/updated successfully", {
              channelId,
              currentUserId,
              targetUserId,
              isNewChannel: !channelSnap.exists
            });

            return { success: true, channelId };
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

