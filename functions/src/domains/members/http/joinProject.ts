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

/**
 * 내부 함수: 실제 프로젝트 가입 로직 실행 (재사용 가능)
 */
export async function executeJoinProject(
  projectId: string,
  userId: string,
  logger: any
): Promise<{ success: boolean; memberId: string; role: string }> {
  const firestore = admin.firestore();
  const projectRef = firestore.collection(COLLECTIONS.PROJECTS).doc(projectId);
  const projectSnap = await projectRef.get();
  
  if (!projectSnap.exists) {
    throw new AppError("not-found", "Project not found");
  }
  
  const project = projectSnap.data() as any;

  // Resolve default roles from /projects/{projectId}/roles where isDefault == true
  let defaultRoleIds: string[] = [];
  try {
    const rolesSnap = await projectRef
      .collection(COLLECTIONS.ROLES)
      .where("isDefault", "==", true)
      .get();
    defaultRoleIds = rolesSnap.docs
      .map((d) => d.id)
      .filter((id) => id !== "OWNER");
    logger.info("Resolved default roles for join", {
      projectId,
      userId,
      defaultRoleIds,
      count: defaultRoleIds.length,
    });
  } catch (e) {
    logger.warn("Failed to resolve default roles; proceeding with none", { error: e, projectId, userId });
    defaultRoleIds = [];
  }

  // Create member at /projects/{projectId}/members/{userId} with default roles
  const role = "member";
  const memberRef = projectRef.collection(COLLECTIONS.MEMBERS).doc(userId);
  await memberRef.set({
    roleIds: defaultRoleIds,
    status: 'active',
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  logger.info("Created project member", { projectId, userId, defaultRoleIds });

  // Create project wrapper under /users/{uid}/projects_wrapper/{projectId}
  try {
    const wrappersCol = firestore
      .collection(COLLECTIONS.USERS)
      .doc(userId)
      .collection(COLLECTIONS.PROJECT_WRAPPERS);
    
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
    logger.info("Created project wrapper for user", { userId, projectId });
  } catch (wrapperError) {
    logger.warn("Failed to create project wrapper", { error: wrapperError, userId, projectId });
  }

  return {
    success: true,
    memberId: memberRef.id,
    role,
  };
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

          // Check if user is blocked or already a member
          const existingMemberRef = projectRef.collection(COLLECTIONS.MEMBERS).doc(userId);
          const existingMemberSnap = await existingMemberRef.get();
          if (existingMemberSnap.exists) {
            const memberData = existingMemberSnap.data() as any;
            const memberStatus = memberData.status || 'active';
            
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
            
            // If status is 'leave', reactivate the member
            if (memberStatus === 'leave') {
              await existingMemberRef.update({
                status: 'active',
                updatedAt: admin.firestore.FieldValue.serverTimestamp()
              });
              logger.info("Reactivated member who had left", { projectId, userId });
              
              // Recreate project wrapper if needed
              try {
                const wrappersCol = firestore
                  .collection(COLLECTIONS.USERS)
                  .doc(userId)
                  .collection(COLLECTIONS.PROJECT_WRAPPERS);
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
                logger.info("Recreated project wrapper for rejoining user", { userId, projectId });
              } catch (wrapperError) {
                logger.warn("Failed to recreate project wrapper", { error: wrapperError, userId, projectId });
              }
              
              return {
                success: true,
                projectId,
                memberId: existingMemberSnap.id,
                role: "member",
              };
            }
          }

          // Execute join project logic
          const joinResult = await executeJoinProject(projectId, userId, logger);

          logger.info("Member joined project", { projectId, userId });

          return {
            success: joinResult.success,
            projectId,
            memberId: joinResult.memberId,
            role: joinResult.role,
          };
        }
      );
    } catch (error) {
      logger.error("Failed to join project", error);
      throw handleError(error, "joinProject");
    }
  }
);
