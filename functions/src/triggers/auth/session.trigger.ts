import {onCall, HttpsError} from "firebase-functions/v2/https";
import {RUNTIME_CONFIG} from "../../core/constants";
import {Providers} from "../../config/dependencies";

interface LoginRequest {
  email: string;
  password: string;
  deviceInfo?: string;
}

interface LoginResponse {
  sessionToken: string;
  refreshToken: string;
  expiresAt: string;
  userProfile: any;
}

export const loginUserFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request): Promise<LoginResponse> => {
    try {
      const {email, password, deviceInfo} = request.data as LoginRequest;
      const ipAddress = request.rawRequest.ip;

      if (!email || !password) {
        throw new HttpsError("invalid-argument", "Email and password are required");
      }

      const authUseCases = Providers.getAuthSessionProvider().create();

      const result = await authUseCases.loginUserUseCase.execute({
        email,
        password,
        deviceInfo,
        ipAddress,
      });

      if (!result.success) {
        if (result.error.name === "Unauthorized") {
          throw new HttpsError("unauthenticated", result.error.message);
        }
        throw new HttpsError("internal", result.error.message);
      }

      return {
        sessionToken: result.data.sessionToken,
        refreshToken: result.data.refreshToken,
        expiresAt: result.data.expiresAt.toISOString(),
        userProfile: result.data.userProfile,
      };
    } catch (error) {
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", `Login failed: ${error instanceof Error ? error.message : "Unknown error"}`);
    }
  }
);

interface LogoutRequest {
  sessionToken: string;
}

export const logoutUserFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request): Promise<{ success: boolean }> => {
    try {
      const {sessionToken} = request.data as LogoutRequest;

      if (!sessionToken) {
        throw new HttpsError("invalid-argument", "Session token is required");
      }

      const authUseCases = Providers.getAuthSessionProvider().create();
      // TODO: Implement LogoutUserUseCase and use authUseCases.logoutUserUseCase

      return {success: true};
    } catch (error) {
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", `Logout failed: ${error instanceof Error ? error.message : "Unknown error"}`);
    }
  }
);
