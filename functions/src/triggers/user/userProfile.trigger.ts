import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { UpdateUserProfileUseCase } from '../../application/user/updateUserProfile.usecase';
import { FirestoreUserProfileDataSource } from '../../data/firestore/userProfile.datasource';
import { RUNTIME_CONFIG } from '../../core/constants';

interface UpdateUserProfileRequest {
  userId: string;
  username?: string;
  profileImage?: string;
  bio?: string;
  displayName?: string;
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
      const { userId, username, profileImage, bio, displayName } = request.data as UpdateUserProfileRequest;

      if (!userId) {
        throw new HttpsError('invalid-argument', 'User ID is required');
      }

      const userProfileRepository = new FirestoreUserProfileDataSource();
      const updateUseCase = new UpdateUserProfileUseCase(userProfileRepository);

      const result = await updateUseCase.execute({
        userId,
        username,
        profileImage,
        bio,
        displayName
      });

      if (!result.success) {
        if (result.error.code === 'NOT_FOUND') {
          throw new HttpsError('not-found', result.error.message);
        }
        if (result.error.code === 'VALIDATION_ERROR') {
          throw new HttpsError('invalid-argument', result.error.message);
        }
        throw new HttpsError('internal', result.error.message);
      }

      return {
        userProfile: result.data.userProfile
      };
    } catch (error) {
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError('internal', `Update profile failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  }
);