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

interface LeaveProjectRequest {
  projectId: string;
  targetUserId?: string; // If provided, remove this user (owner-only)
}

interface LeaveProjectResponse {
  success: boolean;
}

export const leaveProject = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: FUNCTION_MEMORY.SMALL as any,
    timeoutSeconds: 30,
  },
  async (request): Promise<LeaveProjectResponse> => {
    const requestId = getOrCreateRequestId(request);
    const logger = createLogger({
      requestId,
      domain: "members",
      operation: "leaveProject",
      startTime: Date.now(),
    });

    try {
      return await withTracing(
        {
          requestId,
          domain: "members",
          operation: "leaveProject",
          startTime: Date.now(),
        },
        async () => {
          const userId = validateAuth(request.auth);
          const { projectId, targetUserId } = request.data as LeaveProjectRequest;

          validateRequired(projectId, "projectId");
          validateProjectId(projectId);

          const firestore = admin.firestore();
          const projectRef = firestore.collection(COLLECTIONS.PROJECTS).doc(projectId);
          const projectSnap = await projectRef.get();
          if (!projectSnap.exists) {
            throw new AppError("not-found", "Project not found");
          }

          const projectData = projectSnap.data() as any;
          const userToRemove = targetUserId || userId;

          // If removing another user, verify caller is project owner
          if (targetUserId && targetUserId !== userId) {
            if (projectData.ownerId !== userId) {
              throw new AppError("permission-denied", "Only project owner can remove other members");
            }
            
            // Prevent owner from removing themselves via targetUserId
            if (targetUserId === projectData.ownerId) {
              throw new AppError("invalid-argument", "Owner cannot be removed from project");
            }
            
            logger.info("Owner removing member", { projectId, ownerId: userId, targetUserId });
          } else {
            logger.info("Member leaving project", { projectId, userId });
          }

          // Remove membership
          const memberRef = projectRef.collection(COLLECTIONS.MEMBERS).doc(userToRemove);
          await memberRef.delete();
          logger.info("Deleted project member", { projectId, userId: userToRemove });

          // Remove user wrapper
          const wrapperRef = firestore
            .collection(COLLECTIONS.USERS)
            .doc(userToRemove)
            .collection(COLLECTIONS.PROJECT_WRAPPERS)
            .doc(projectId);
          await wrapperRef.delete();
          logger.info("Deleted project wrapper for user", { projectId, userId: userToRemove });

          return { success: true };
        }
      );
    } catch (error) {
      logger.error("Failed to leave project", error);
      throw handleError(error, "leaveProject");
    }
  }
);

