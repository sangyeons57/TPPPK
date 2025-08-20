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

interface JoinProjectRequest {
  projectId: string;
}

interface JoinProjectResponse {
  success: boolean;
  projectId: string;
  memberId: string;
  role: string;
}

// Self-join to a project using projectId only.
// Assumes the caller has the right to join (flow/UX controls this).
export const joinProject = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: FUNCTION_MEMORY.SMALL as any,
    timeoutSeconds: 30,
  },
  async (request): Promise<JoinProjectResponse> => {
    const requestId = getOrCreateRequestId(request);
    const logger = createLogger({
      requestId,
      domain: "members",
      operation: "joinProject",
      startTime: Date.now(),
    });

    try {
      return await withTracing(
        {
          requestId,
          domain: "members",
          operation: "joinProject",
          startTime: Date.now(),
        },
        async () => {
          const userId = validateAuth(request.auth);
          const { projectId } = request.data as JoinProjectRequest;

          validateRequired(projectId, "projectId");
          validateProjectId(projectId);

          const firestore = admin.firestore();

          // Project existence
          const projectRef = firestore.collection(COLLECTIONS.PROJECTS).doc(projectId);
          const projectSnap = await projectRef.get();
          if (!projectSnap.exists) {
            throw new AppError("not-found", "Project not found");
          }
          const project = projectSnap.data() as any;

          // Check if user is blocked/banned or already a member
          const existingMemberRef = projectRef.collection(COLLECTIONS.MEMBERS).doc(userId);
          const existingMemberSnap = await existingMemberRef.get();
          if (existingMemberSnap.exists) {
            const memberData = existingMemberSnap.data() as any;
            const memberStatus = memberData.status || 'active';
            
            if (memberStatus === 'banned') {
              throw new AppError("permission-denied", "User is permanently banned from this project");
            }
            
            if (memberStatus === 'blocked') {
              throw new AppError("permission-denied", "User is blocked from this project");
            }
            
            // If status is 'active', user is already a member
            if (memberStatus === 'active') {
              logger.info("User already a project member", { projectId, userId });
              return {
                success: true,
                projectId,
                memberId: existingMemberSnap.id,
                role: "member",
              };
            }
          }

          // Create member at /projects/{projectId}/members/{userId}
          const role = "member";
          const memberRef = projectRef.collection(COLLECTIONS.MEMBERS).doc(userId);
          await memberRef.set({
            roleIds: [],
            status: 'active', // Set default status for new members
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          });
          logger.info("Created project member", { projectId, userId });

          // Create project wrapper under /users/{uid}/projects_wrapper/{projectId} (best-effort)
          try {
            const wrappersCol = firestore
              .collection(COLLECTIONS.USERS)
              .doc(userId)
              .collection(COLLECTIONS.PROJECT_WRAPPERS)
            // compute next order = max(order)+1
            const lastOrderSnap = await wrappersCol.orderBy("order", "desc").limit(1).get();
            const nextOrder = !lastOrderSnap.empty
              ? ((lastOrderSnap.docs[0].data() as any).order ?? 0) + 1
              : 1;

            const wrapperRef = wrappersCol.doc(projectId);
            await wrapperRef.set({
              order: nextOrder,
              projectName: project?.name?.value || project?.name || "",
              createdAt: admin.firestore.FieldValue.serverTimestamp(),
              updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            });
            logger.info("Created project wrapper for user", { userId, projectId, path: `users/${userId}/projects_wrapper/${projectId}` });
          } catch (wrapperError) {
            logger.warn("Failed to create project wrapper", { error: wrapperError, userId, projectId });
          }

          logger.info("Member joined project", { projectId, userId });

          return {
            success: true,
            projectId,
            memberId: memberRef.id,
            role,
          };
        }
      );
    } catch (error) {
      logger.error("Failed to join project", error);
      throw handleError(error, "joinProject");
    }
  }
);
