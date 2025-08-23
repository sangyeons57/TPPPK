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

interface UnblockMemberRequest {
  projectId: string;
  targetUserId: string;
}

interface UnblockMemberResponse {
  success: boolean;
  message: string;
}

export const unblockMember = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: FUNCTION_MEMORY.SMALL as any,
    timeoutSeconds: 30,
  },
  async (request): Promise<UnblockMemberResponse> => {
    const requestId = getOrCreateRequestId(request);
    const logger = createLogger({
      requestId,
      domain: "members",
      operation: "unblockMember",
      startTime: Date.now(),
    });

    try {
      return await withTracing(
        {
          requestId,
          domain: "members",
          operation: "unblockMember",
          startTime: Date.now(),
        },
        async () => {
          const userId = validateAuth(request.auth);
          const { projectId, targetUserId } = request.data as UnblockMemberRequest;

          validateRequired(projectId, "projectId");
          validateRequired(targetUserId, "targetUserId");
          validateProjectId(projectId);

          const firestore = admin.firestore();
          const projectRef = firestore.collection(COLLECTIONS.PROJECTS).doc(projectId);
          const projectSnap = await projectRef.get();
          
          if (!projectSnap.exists) {
            throw new AppError("not-found", "Project not found");
          }

          const projectData = projectSnap.data() as any;

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

          // 🚨 오너 보호: 일반적으로 오너는 차단되지 않지만, 만약 차단된 경우 오너만 해제 가능
          if (targetUserId === projectData.ownerId && !isOwner) {
            throw new AppError("permission-denied", "Only the owner can unblock themselves");
          }

          // Verify target user exists and is currently blocked
          const memberRef = projectRef.collection(COLLECTIONS.MEMBERS).doc(targetUserId);
          const memberSnap = await memberRef.get();
          
          if (!memberSnap.exists) {
            throw new AppError("not-found", "Member not found in this project");
          }

          const memberData = memberSnap.data();
          const currentStatus = memberData?.status || 'active';

          // Check if user is actually blocked
          if (!['blocked', 'banned'].includes(currentStatus)) {
            logger.info("Member is not blocked", { projectId, targetUserId, currentStatus });
            return {
              success: true,
              message: "Member is already active"
            };
          }

          // Update member status to active
          await memberRef.update({
            status: 'active',
            unblockedAt: admin.firestore.FieldValue.serverTimestamp(),
            unblockedBy: userId,
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
          });
          logger.info("Updated member status to active", { projectId, targetUserId, previousStatus: currentStatus });

          // Optional: Remove from blocked_members collection if it exists
          const blockedMemberRef = projectRef.collection(COLLECTIONS.BLOCKED_MEMBERS).doc(targetUserId);
          const blockedMemberSnap = await blockedMemberRef.get();
          if (blockedMemberSnap.exists) {
            await blockedMemberRef.delete();
            logger.info("Removed blocked member record", { projectId, targetUserId });
          }

          // Recreate user wrapper to allow access to the project
          const wrapperRef = firestore
            .collection(COLLECTIONS.USERS)
            .doc(targetUserId)
            .collection(COLLECTIONS.PROJECT_WRAPPERS)
            .doc(projectId);
          
          // Get project data for wrapper recreation
          const wrappersCol = firestore
            .collection(COLLECTIONS.USERS)
            .doc(targetUserId)
            .collection(COLLECTIONS.PROJECT_WRAPPERS);
          const lastOrderSnap = await wrappersCol.orderBy("order", "desc").limit(1).get();
          const nextOrder = !lastOrderSnap.empty
            ? ((lastOrderSnap.docs[0].data() as any).order ?? 0) + 1
            : 1;

          await wrapperRef.set({
            order: nextOrder,
            projectName: projectData.name?.value || projectData.name || "Unknown Project",
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
          });
          logger.info("Recreated project wrapper for unblocked user", { projectId, targetUserId });

          logger.info("Member unblocked successfully", { 
            projectId, 
            targetUserId, 
            unblockedBy: userId,
            previousStatus: currentStatus
          });

          return {
            success: true,
            message: "Member unblocked successfully"
          };
        }
      );
    } catch (error) {
      logger.error("Failed to unblock member", error);
      throw handleError(error, "unblockMember");
    }
  }
);