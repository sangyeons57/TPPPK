import {onCall, HttpsError} from "firebase-functions/v2/https";
import {RUNTIME_CONFIG} from "../../core/constants";
import {Providers} from "../../config/dependencies";

interface RemoveUserProfileImageRequest {
  userId: string;
}

interface RemoveUserProfileImageResponse {
  success: boolean;
  message: string;
}

export const removeUserProfileImageFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request): Promise<RemoveUserProfileImageResponse> => {
    try {
      const {
        userId,
      } = request.data as RemoveUserProfileImageRequest;

      if (!userId) {
        throw new HttpsError("invalid-argument", "User ID is required");
      }

      // 인증된 사용자만 자신의 프로필 이미지를 삭제할 수 있음
      if (!request.auth) {
        throw new HttpsError("unauthenticated", "Authentication required");
      }

      if (request.auth.uid !== userId) {
        throw new HttpsError("permission-denied", "Can only remove your own profile image");
      }

      const userUseCases = Providers.getUserProvider().create();

      const result = await userUseCases.removeUserProfileImageUseCase.execute({
        userId,
      });

      if (!result.success) {
        if (result.error.name === "NOT_FOUND") {
          throw new HttpsError("not-found", result.error.message);
        }
        if (result.error.name === "VALIDATION_ERROR") {
          throw new HttpsError("invalid-argument", result.error.message);
        }
        throw new HttpsError("internal", result.error.message);
      }

      return {
        success: result.data.success,
        message: result.data.message,
      };
    } catch (error) {
      console.error("Error in removeUserProfileImage:", error);
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", "Internal server error");
    }
  }
);
