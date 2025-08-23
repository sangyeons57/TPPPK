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
  blockType: 'blocked'; // blocked = temporary block
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
          if (blockType !== 'blocked') {
            throw new AppError("invalid-argument", "blockType must be 'blocked'");
          }

          const firestore = admin.firestore();
          const projectRef = firestore.collection(COLLECTIONS.PROJECTS).doc(projectId);
          const projectSnap = await projectRef.get();
          
          if (!projectSnap.exists) {
            throw new AppError("not-found", "Project not found");
          }

          const projectData = projectSnap.data() as any;

          // 🚨 오너 보호: 대상이 오너인 경우 차단 금지
          if (targetUserId === projectData.ownerId) {
            throw new AppError("permission-denied", "Cannot block the project owner");
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

          // Verify target user is a member
          const memberRef = projectRef.collection(COLLECTIONS.MEMBERS).doc(targetUserId);
          const memberSnap = await memberRef.get();
          
          if (!memberSnap.exists) {
            throw new AppError("not-found", "Target user is not a member of this project");
          }

          // Update member status to blocked (keep member record)
          await memberRef.update({
            status: blockType,
            blockedAt: admin.firestore.FieldValue.serverTimestamp(),
            blockedBy: userId,
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
          });
          logger.info("Updated member status to blocked", { projectId, targetUserId, blockedBy: userId });

          // Remove user wrapper to prevent access to the project (hard delete)
          const wrapperRef = firestore
            .collection(COLLECTIONS.USERS)
            .doc(targetUserId)
            .collection(COLLECTIONS.PROJECT_WRAPPERS)
            .doc(projectId);
          await wrapperRef.delete();
          logger.info("Deleted project wrapper for blocked user", { projectId, userId: targetUserId });

          // Optional: Add record to blocked_members collection for additional tracking
          const blockedMemberRef = projectRef.collection(COLLECTIONS.BLOCKED_MEMBERS).doc(targetUserId);
          await blockedMemberRef.set({
            blockedBy: userId,
            blockedAt: admin.firestore.FieldValue.serverTimestamp(),
            blockType: blockType,
            reason: "Manual block by admin",
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
          });
          logger.info("Added blocked member record", { projectId, targetUserId, blockedBy: userId });

          logger.info("Member blocked successfully", { 
            projectId, 
            targetUserId, 
            blockType, 
            blockedBy: userId
          });

          return { 
            success: true, 
            message: `Member blocked successfully`
          };
        }
      );
    } catch (error) {
      logger.error("Failed to block member", error);
      throw handleError(error, "blockMember");
    }
  }
);