import { onObjectFinalized } from 'firebase-functions/v2/storage';
import { ProcessUserImageUseCase } from '../../application/user/processUserImage.usecase';
import { ImageProcessingService } from '../../domain/image/imageProcessing.service';
import { FirestoreUserProfileDataSource } from '../../data/firestore/userProfile.datasource';
import { FirestoreImageDataSource, FirebaseStorageService } from '../../data/firestore/image.datasource';
import { RUNTIME_CONFIG } from '../../core/constants';
import { STORAGE_BUCKETS } from '../../core/constants';

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

      if (!contentType.startsWith('image/')) {
        console.log('File is not an image');
        return;
      }

      const userId = extractUserIdFromPath(name);
      if (!userId) {
        console.log('Cannot extract user ID from file path');
        return;
      }

      const { getStorage } = await import('firebase-admin/storage');
      const storage = getStorage();
      const file = storage.bucket(bucket).file(name);
      
      const [fileBuffer] = await file.download();

      const imageRepository = new FirestoreImageDataSource();
      const storageService = new FirebaseStorageService();
      const imageProcessingService = new ImageProcessingService(imageRepository, storageService);
      const userProfileRepository = new FirestoreUserProfileDataSource();

      const processUseCase = new ProcessUserImageUseCase(
        imageProcessingService,
        userProfileRepository
      );

      const result = await processUseCase.execute({
        userId,
        imageBuffer: fileBuffer,
        contentType
      });

      if (!result.success) {
        console.error('Failed to process user image:', result.error);
        return;
      }

      console.log('User profile image processed successfully:', result.data);
    } catch (error) {
      console.error('Error processing user profile image:', error);
    }
  }
);

function extractUserIdFromPath(filePath: string): string | null {
  const parts = filePath.split('/');
  return parts.length > 1 ? parts[0] : null;
}