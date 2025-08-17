import { onCall } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { 
  validateAuth, 
  handleError, 
  createLogger, 
  getOrCreateRequestId,
  withTracing,
  AppError
} from "../../../shared";

interface RemoveUserProfileImageResponse {
  success: boolean;
  message: string;
}

export const removeUserProfileImage = onCall(
  {
    region: "asia-northeast3",
    memory: "256MiB",
    timeoutSeconds: 60,
  },
  async (request): Promise<RemoveUserProfileImageResponse> => {
    const requestId = getOrCreateRequestId(request);
    const logger = createLogger({
      requestId,
      domain: "users",
      operation: "removeProfileImage",
      startTime: Date.now(),
    });

    try {
      return await withTracing(
        {
          requestId,
          domain: "users",
          operation: "removeProfileImage", 
          startTime: Date.now(),
        },
        async () => {
          // 인증 검증
          const userId = validateAuth(request.auth);
          logger.info("User authenticated", { userId });

          const firestore = admin.firestore();
          const storage = admin.storage();
          
          // 현재 사용자 조회
          const userRef = firestore.collection("users").doc(userId);
          const userDoc = await userRef.get();
          
          if (!userDoc.exists) {
            throw new AppError("not-found", "User not found");
          }

          const userData = userDoc.data()!;
          const currentImageUrl = userData.profileImageUrl;

          if (!currentImageUrl) {
            logger.info("No profile image to remove", { userId });
            return {
              success: true,
              message: "No profile image to remove"
            };
          }

          // Storage에서 기존 이미지 삭제 (URL에서 파일 경로 추출)
          try {
            // Firebase Storage URL에서 파일 경로 추출
            const urlPattern = /\/o\/(.+?)\?/;
            const match = currentImageUrl.match(urlPattern);
            
            if (match) {
              const filePath = decodeURIComponent(match[1]);
              await storage.bucket().file(filePath).delete();
              logger.info("Profile image deleted from storage", { userId, filePath });
            }
          } catch (storageError) {
            // Storage 삭제 실패는 로그만 남기고 계속 진행
            logger.warn("Failed to delete image from storage", storageError);
          }

          // Firestore에서 프로필 이미지 URL 제거
          await userRef.update({
            profileImageUrl: admin.firestore.FieldValue.delete(),
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          });

          logger.info("Profile image URL removed from user document", { userId });

          return {
            success: true,
            message: "Profile image removed successfully"
          };
        }
      );
    } catch (error) {
      logger.error("Failed to remove user profile image", error);
      throw handleError(error, "removeUserProfileImage");
    }
  }
);