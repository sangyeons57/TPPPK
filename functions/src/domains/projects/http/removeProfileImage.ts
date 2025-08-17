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
  FUNCTION_MEMORY
} from "../../../shared";

interface RemoveProjectProfileImageRequest {
  projectId: string;
}

interface RemoveProjectProfileImageResponse {
  success: boolean;
  message: string;
}

export const removeProjectProfileImage = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: FUNCTION_MEMORY.SMALL as any,
    timeoutSeconds: 60,
  },
  async (request): Promise<RemoveProjectProfileImageResponse> => {
    const requestId = getOrCreateRequestId(request);
    const logger = createLogger({
      requestId,
      domain: "projects",
      operation: "removeProfileImage",
      startTime: Date.now(),
    });

    try {
      return await withTracing(
        {
          requestId,
          domain: "projects",
          operation: "removeProfileImage",
          startTime: Date.now(),
        },
        async () => {
          const userId = validateAuth(request.auth);
          const { projectId } = request.data as RemoveProjectProfileImageRequest;

          validateRequired(projectId, "projectId");
          validateProjectId(projectId);

          const firestore = admin.firestore();
          const storage = admin.storage();

          // 프로젝트 존재 및 권한 확인
          const projectRef = firestore.collection(COLLECTIONS.PROJECTS).doc(projectId);
          const projectDoc = await projectRef.get();

          if (!projectDoc.exists) {
            throw new AppError("not-found", "Project not found");
          }

          // 멤버 권한 확인 (OWNER 또는 ADMIN)
          const memberQuery = await firestore
            .collection(COLLECTIONS.MEMBERS)
            .where("projectId", "==", projectId)
            .where("userId", "==", userId)
            .where("role", "in", ["owner", "admin"])
            .limit(1)
            .get();

          if (memberQuery.empty) {
            throw new AppError("permission-denied", "Only project owners and admins can remove profile image");
          }

          const projectData = projectDoc.data()!;
          const currentImageUrl = projectData.profileImageUrl;

          if (!currentImageUrl) {
            logger.info("No profile image to remove", { projectId });
            return {
              success: true,
              message: "No profile image to remove"
            };
          }

          // Storage에서 기존 이미지 삭제
          try {
            const urlPattern = /\/o\/(.+?)\?/;
            const match = currentImageUrl.match(urlPattern);
            
            if (match) {
              const filePath = decodeURIComponent(match[1]);
              await storage.bucket().file(filePath).delete();
              logger.info("Profile image deleted from storage", { projectId, filePath });
            }
          } catch (storageError) {
            logger.warn("Failed to delete image from storage", storageError);
          }

          // Firestore에서 프로필 이미지 URL 제거
          await projectRef.update({
            profileImageUrl: admin.firestore.FieldValue.delete(),
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          });

          logger.info("Project profile image URL removed", { projectId, userId });

          return {
            success: true,
            message: "Project profile image removed successfully"
          };
        }
      );
    } catch (error) {
      logger.error("Failed to remove project profile image", error);
      throw handleError(error, "removeProjectProfileImage");
    }
  }
);