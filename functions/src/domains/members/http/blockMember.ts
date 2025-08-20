import { onCall } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import {
  validateAuth,
  validateRequired,
  validateProjectId,
  handleError,
  createLogger,
  getOrCreateRequestId,
  withTracing,
  AppError,
  COLLECTIONS,
  RUNTIME_CONFIG,
  FUNCTION_MEMORY,
} from "../../../shared";

interface BlockMemberRequest {
  projectId: string;
  targetUserId: string;
  blockType: 'blocked' | 'banned'; // blocked = temporary, banned = permanent
}

interface BlockMemberResponse {
  success: boolean;
  message: string;
}

export const blockMember = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: FUNCTION_MEMORY.SMALL as any,
    timeoutSeconds: 30,
  },
  async (request): Promise<BlockMemberResponse> => {
    const requestId = getOrCreateRequestId(request);
    const logger = createLogger({
      requestId,
      domain: "members",
      operation: "blockMember",
      startTime: Date.now(),
    });

    try {
      return await withTracing(
        {
          requestId,
          domain: "members",
          operation: "blockMember",
          startTime: Date.now(),
        },
        async () => {
          const userId = validateAuth(request.auth);
          const { projectId, targetUserId, blockType } = request.data as BlockMemberRequest;

          validateRequired(projectId, "projectId");
          validateRequired(targetUserId, "targetUserId");
          validateRequired(blockType, "blockType");
          validateProjectId(projectId);

          // Validate blockType
          if (!['blocked', 'banned'].includes(blockType)) {
            throw new AppError("invalid-argument", "blockType must be 'blocked' or 'banned'");
          }

          const firestore = admin.firestore();
          const projectRef = firestore.collection(COLLECTIONS.PROJECTS).doc(projectId);
          const projectSnap = await projectRef.get();
          
          if (!projectSnap.exists) {
            throw new AppError("not-found", "Project not found");
          }

          const projectData = projectSnap.data() as any;

          // Verify caller is project owner
          if (projectData.ownerId !== userId) {
            throw new AppError("permission-denied", "Only project owner can block members");
          }

          // Prevent owner from blocking themselves
          if (targetUserId === projectData.ownerId) {
            throw new AppError("invalid-argument", "Owner cannot block themselves");
          }

          // Verify target user is a member
          const memberRef = projectRef.collection(COLLECTIONS.MEMBERS).doc(targetUserId);
          const memberSnap = await memberRef.get();
          
          if (!memberSnap.exists) {
            throw new AppError("not-found", "Target user is not a member of this project");
          }

          // Update member status instead of deleting
          await memberRef.update({
            status: blockType,
            blockedAt: admin.firestore.FieldValue.serverTimestamp(),
            blockedBy: userId,
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
          });

          // Remove user wrapper to prevent access to the project
          const wrapperRef = firestore
            .collection(COLLECTIONS.USERS)
            .doc(targetUserId)
            .collection(COLLECTIONS.PROJECT_WRAPPERS)
            .doc(projectId);
          await wrapperRef.delete();
          logger.info("Deleted project wrapper for blocked user", { projectId, userId: targetUserId });

          logger.info("Member blocked successfully", { 
            projectId, 
            targetUserId, 
            blockType, 
            blockedBy: userId
          });

          const actionMessage = blockType === 'banned' ? 'banned' : 'blocked';
          return { 
            success: true, 
            message: `Member ${actionMessage} successfully`
          };
        }
      );
    } catch (error) {
      logger.error("Failed to block member", error);
      throw handleError(error, "blockMember");
    }
  }
);