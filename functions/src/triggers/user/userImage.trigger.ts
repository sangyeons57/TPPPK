import { onObjectFinalized } from 'firebase-functions/v2/storage';
import { UpdateUserImageUseCase } from '../../business/user/usecases/updateUserImage.usecase';
import { RUNTIME_CONFIG } from '../../core/constants';
import { STORAGE_BUCKETS } from '../../core/constants';
import { Providers } from '../../config/dependencies';

/**
 * Simplified Storage trigger for user profile images
 * Updates User entity's profileImageUrl when image is uploaded
 */
export const onUserProfileImageUpload = onObjectFinalized(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
    bucket: STORAGE_BUCKETS.USER_PROFILES,
  },
  async (event) => {
    try {
      const { bucket, name, contentType } = event.data;
      
      if (!name || !contentType) {
        console.log('Missing file name or content type');
        return;
      }

      // Only process image files
      if (!contentType.startsWith('image/')) {
        console.log(`Skipping non-image file: ${contentType}`);
        return;
      }

      // Extract userId from file path (e.g., "users/{userId}/profile.jpg")
      const pathParts = name.split('/');
      if (pathParts.length < 2 || pathParts[0] !== 'users') {
        console.log(`Invalid file path structure: ${name}`);
        return;
      }

      const userId = pathParts[1];
      if (!userId) {
        console.log('Could not extract userId from file path');
        return;
      }

      // Generate public URL
      const publicUrl = `https://storage.googleapis.com/${bucket}/${name}`;

      // Get use case and update user
      const userUseCases = Providers.getUserProvider().create();
      const updateImageUseCase = new UpdateUserImageUseCase(userUseCases.userRepository);

      const result = await updateImageUseCase.execute({
        userId: userId,
        imageUrl: publicUrl
      });

      if (result.success) {
        console.log(`Successfully updated user ${userId} profile image: ${publicUrl}`);
      } else {
        console.error(`Failed to update user ${userId} profile image:`, result.error);
      }

    } catch (error) {
      console.error('Error processing user profile image upload:', error);
    }
  }
);