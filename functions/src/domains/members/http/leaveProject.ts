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
          const { projectId } = request.data as LeaveProjectRequest;

          validateRequired(projectId, "projectId");
          validateProjectId(projectId);

          const firestore = admin.firestore();
          const projectRef = firestore.collection(COLLECTIONS.PROJECTS).doc(projectId);
          const projectSnap = await projectRef.get();
          if (!projectSnap.exists) {
            throw new AppError("not-found", "Project not found");
          }

          // Remove membership
          const memberRef = projectRef.collection(COLLECTIONS.MEMBERS).doc(userId);
          await memberRef.delete();
          logger.info("Deleted project member", { projectId, userId });

          // Remove user wrapper
          const wrapperRef = firestore
            .collection(COLLECTIONS.USERS)
            .doc(userId)
            .collection(COLLECTIONS.PROJECT_WRAPPERS)
            .doc(projectId);
          await wrapperRef.delete();
          logger.info("Deleted project wrapper for user", { projectId, userId });

          return { success: true };
        }
      );
    } catch (error) {
      logger.error("Failed to leave project", error);
      throw handleError(error, "leaveProject");
    }
  }
);

