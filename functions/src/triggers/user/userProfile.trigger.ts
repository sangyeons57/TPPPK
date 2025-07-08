import {onCall, HttpsError} from "firebase-functions/v2/https";
import {RUNTIME_CONFIG} from "../../core/constants";
import {Providers} from "../../config/dependencies";

interface UpdateUserProfileRequest {
  userId: string;
  name?: string;
  memo?: string;
}

interface UpdateUserProfileResponse {
  userProfile: any;
}

export const updateUserProfileFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request): Promise<UpdateUserProfileResponse> => {
    try {
      const {
        userId,
        name,
        memo,
      } = request.data as UpdateUserProfileRequest;

      if (!userId) {
        throw new HttpsError("invalid-argument", "User ID is required");
      }

      const userUseCases = Providers.getUserProvider().create();

      const result = await userUseCases.updateUserProfileUseCase.execute({
        userId,
        name,
        memo,
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
        userProfile: result.data.userProfile,
      };
    } catch (error) {
      console.error("Error in updateUserProfile:", error);
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", "Internal server error");
    }
  }
);
