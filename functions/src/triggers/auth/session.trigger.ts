import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { LoginUserUseCase } from '../../application/auth/loginUser.usecase';
import { FirestoreSessionDataSource } from '../../data/firestore/session.datasource';
import { FirestoreUserProfileDataSource } from '../../data/firestore/userProfile.datasource';
import { RUNTIME_CONFIG } from '../../core/constants';

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
      const { email, password, deviceInfo } = request.data as LoginRequest;
      const ipAddress = request.rawRequest.ip;

      if (!email || !password) {
        throw new HttpsError('invalid-argument', 'Email and password are required');
      }

      const sessionRepository = new FirestoreSessionDataSource();
      const userProfileRepository = new FirestoreUserProfileDataSource();
      const loginUseCase = new LoginUserUseCase(sessionRepository, userProfileRepository);

      const result = await loginUseCase.execute({
        email,
        password,
        deviceInfo,
        ipAddress
      });

      if (!result.success) {
        if (result.error.code === 'UNAUTHORIZED') {
          throw new HttpsError('unauthenticated', result.error.message);
        }
        throw new HttpsError('internal', result.error.message);
      }

      return {
        sessionToken: result.data.sessionToken,
        refreshToken: result.data.refreshToken,
        expiresAt: result.data.expiresAt.toISOString(),
        userProfile: result.data.userProfile
      };
    } catch (error) {
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError('internal', `Login failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
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
      const { sessionToken } = request.data as LogoutRequest;

      if (!sessionToken) {
        throw new HttpsError('invalid-argument', 'Session token is required');
      }

      const sessionRepository = new FirestoreSessionDataSource();
      // TODO: Implement LogoutUserUseCase
      
      return { success: true };
    } catch (error) {
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError('internal', `Logout failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  }
);