import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { RegisterUserUseCase } from '../../application/auth/registerUser.usecase';
import { FirestoreUserProfileDataSource } from '../../data/firestore/userProfile.datasource';
import { RUNTIME_CONFIG } from '../../core/constants';

interface RegisterRequest {
  email: string;
  username: string;
  password: string;
  displayName?: string;
  deviceInfo?: string;
}

interface RegisterResponse {
  userProfile: any;
  message: string;
}

export const registerUserFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request): Promise<RegisterResponse> => {
    try {
      const { email, username, password, displayName, deviceInfo } = request.data as RegisterRequest;
      const ipAddress = request.rawRequest.ip;

      if (!email || !username || !password) {
        throw new HttpsError('invalid-argument', 'Email, username, and password are required');
      }

      const userProfileRepository = new FirestoreUserProfileDataSource();
      const registerUseCase = new RegisterUserUseCase(userProfileRepository);

      const result = await registerUseCase.execute({
        email,
        username,
        password,
        displayName,
        deviceInfo,
        ipAddress
      });

      if (!result.success) {
        if (result.error.code === 'CONFLICT') {
          throw new HttpsError('already-exists', result.error.message);
        }
        if (result.error.code === 'VALIDATION_ERROR') {
          throw new HttpsError('invalid-argument', result.error.message);
        }
        throw new HttpsError('internal', result.error.message);
      }

      return {
        userProfile: result.data.userProfile,
        message: result.data.message
      };
    } catch (error) {
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError('internal', `Registration failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  }
);