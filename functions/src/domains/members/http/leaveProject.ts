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

/**
 * 내부 함수: 실제 프로젝트 탈퇴 로직 실행 (재사용 가능)
 */
export async function executeLeaveProject(
  projectId: string,
  userToRemove: string,
  removedBy: string,
  logger: any
): Promise<void> {
  const firestore = admin.firestore();
  const projectRef = firestore.collection(COLLECTIONS.PROJECTS).doc(projectId);

  // Update member status to LEAVE instead of deleting
  const memberRef = projectRef.collection(COLLECTIONS.MEMBERS).doc(userToRemove);
  await memberRef.delete(); // Hard delete for simplification
  logger.info("Deleted member from project", { projectId, userId: userToRemove });

  // Remove user wrapper
  const wrapperRef = firestore
    .collection(COLLECTIONS.USERS)
    .doc(userToRemove)
    .collection(COLLECTIONS.PROJECT_WRAPPERS)
    .doc(projectId);
  await wrapperRef.delete();
  logger.info("Deleted project wrapper for user", { projectId, userId: userToRemove });
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

          // If removing another user, verify permissions
          if (targetUserId && targetUserId !== userId) {
            // 🚨 오너 보호: 대상이 오너인 경우 내보내기 금지
            if (targetUserId === projectData.ownerId) {
              throw new AppError("permission-denied", "Cannot remove the project owner");
            }

            // 권한 확인: 오너이거나 MEMBER_MANAGE 권한이 있어야 함
            const isOwner = projectData.ownerId === userId;
            
            if (!isOwner) {
              // 일반 멤버인 경우 MEMBER_MANAGE 권한 확인
              const memberRef = projectRef.collection(COLLECTIONS.MEMBERS).doc(userId);
              const memberSnap = await memberRef.get();
              
              if (!memberSnap.exists) {
                throw new AppError("permission-denied", "User is not a member of this project");
              }

              const memberData = memberSnap.data() as any;
              const roleIds = memberData.roleIds || [];
              
              // 멤버의 역할들을 확인하여 MEMBER_MANAGE 권한이 있는지 검사
              let hasMemberManagePermission = false;
              for (const roleId of roleIds) {
                const roleRef = firestore.collection(COLLECTIONS.PROJECTS).doc(projectId)
                  .collection(COLLECTIONS.ROLES).doc(roleId);
                const roleSnap = await roleRef.get();
                
                if (roleSnap.exists) {
                  const roleData = roleSnap.data() as any;
                  const permissions = roleData.permissions || {};
                  if (permissions.MEMBER_MANAGE === true) {
                    hasMemberManagePermission = true;
                    break;
                  }
                }
              }
              
              if (!hasMemberManagePermission) {
                throw new AppError("permission-denied", "User does not have permission to manage members");
              }
            }
            
            logger.info("User removing member", { projectId, userId, targetUserId, isOwner });
          } else {
            logger.info("Member leaving project", { projectId, userId });
          }

          // Execute leave project logic
          await executeLeaveProject(projectId, userToRemove, userId, logger);

          return { success: true };
        }
      );
    } catch (error) {
      logger.error("Failed to leave project", error);
      throw handleError(error, "leaveProject");
    }
  }
);

