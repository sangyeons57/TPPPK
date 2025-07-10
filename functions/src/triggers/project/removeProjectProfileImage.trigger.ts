import {onCall, HttpsError} from "firebase-functions/v2/https";
import {RUNTIME_CONFIG} from "../../core/constants";
import {Providers} from "../../config/dependencies";

interface RemoveProjectProfileImageRequest {
  projectId: string;
}

interface RemoveProjectProfileImageResponse {
  success: boolean;
  message: string;
}

export const removeProjectProfileImageFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request): Promise<RemoveProjectProfileImageResponse> => {
    try {
      const {
        projectId,
      } = request.data as RemoveProjectProfileImageRequest;

      if (!projectId) {
        throw new HttpsError("invalid-argument", "Project ID is required");
      }

      // 인증된 사용자만 프로젝트 이미지를 삭제할 수 있음
      if (!request.auth) {
        throw new HttpsError("unauthenticated", "Authentication required");
      }

      const userId = request.auth.uid;
      const projectUseCases = Providers.getProjectProvider().create();

      const result = await projectUseCases.removeProjectProfileImageUseCase.execute({
        projectId,
        userId,
      });

      if (!result.success) {
        if (result.error.name === "NOT_FOUND") {
          throw new HttpsError("not-found", result.error.message);
        }
        if (result.error.name === "VALIDATION_ERROR") {
          throw new HttpsError("permission-denied", result.error.message);
        }
        throw new HttpsError("internal", result.error.message);
      }

      return {
        success: result.data.success,
        message: result.data.message,
      };
    } catch (error) {
      console.error("Error in removeProjectProfileImage:", error);
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", "Internal server error");
    }
  }
);