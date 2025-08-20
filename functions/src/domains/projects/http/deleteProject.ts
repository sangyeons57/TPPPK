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

interface DeleteProjectRequest {
  projectId: string;
}

interface DeleteProjectResponse {
  success: boolean;
  message: string;
}

export const deleteProject = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: FUNCTION_MEMORY.SMALL as any,
    timeoutSeconds: 30,
  },
  async (request): Promise<DeleteProjectResponse> => {
    const requestId = getOrCreateRequestId(request);
    const logger = createLogger({
      requestId,
      domain: "projects",
      operation: "deleteProject",
      startTime: Date.now(),
    });

    try {
      return await withTracing(
        {
          requestId,
          domain: "projects",
          operation: "deleteProject",
          startTime: Date.now(),
        },
        async () => {
          const userId = validateAuth(request.auth);
          const { projectId } = request.data as DeleteProjectRequest;

          validateRequired(projectId, "projectId");
          validateProjectId(projectId);

          const firestore = admin.firestore();
          const projectRef = firestore.collection(COLLECTIONS.PROJECTS).doc(projectId);
          const projectSnap = await projectRef.get();

          if (!projectSnap.exists) {
            throw new AppError("not-found", "Project not found");
          }

          const projectData = projectSnap.data() as any;

          // Verify caller is project owner
          if (projectData.ownerId !== userId) {
            throw new AppError("permission-denied", "Only project owner can delete project");
          }

          // Clean up project_wrappers for all members first
          // Get all project members to clean up their wrappers
          const membersQuery = await projectRef.collection(COLLECTIONS.MEMBERS).get();
          
          if (!membersQuery.empty) {
            const batch = firestore.batch();
            let wrapperDeleteCount = 0;

            for (const memberDoc of membersQuery.docs) {
              const memberId = memberDoc.id;
              const wrapperRef = firestore
                .collection(COLLECTIONS.USERS)
                .doc(memberId)
                .collection(COLLECTIONS.PROJECT_WRAPPERS)
                .doc(projectId);
              
              batch.delete(wrapperRef);
              wrapperDeleteCount++;
            }

            await batch.commit();
            logger.info("Cleaned up project wrappers", { 
              projectId, 
              deletedCount: wrapperDeleteCount 
            });
          }

          // Delete the project document
          // Note: Firestore subcollections are NOT automatically deleted
          await projectRef.delete();
          
          logger.info("Project deleted successfully", { 
            projectId, 
            ownerId: userId
          });

          return { 
            success: true, 
            message: "Project deleted successfully"
          };
        }
      );
    } catch (error) {
      logger.error("Failed to delete project", error);
      throw handleError(error, "deleteProject");
    }
  }
);