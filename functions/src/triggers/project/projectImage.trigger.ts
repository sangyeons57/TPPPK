import { onObjectFinalized } from 'firebase-functions/v2/storage';
import { UpdateProjectImageUseCase } from '../../application/project/updateProjectImage.usecase';
import { ImageProcessingService } from '../../domain/image/imageProcessing.service';
import { FirebaseStorageService } from '../../data/firestore/image.datasource';
import { RUNTIME_CONFIG } from '../../core/constants';
import { STORAGE_BUCKETS } from '../../core/constants';
import { Providers } from '../../config/dependencies';

export const onProjectImageUpload = onObjectFinalized(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
    bucket: STORAGE_BUCKETS.PROJECT_IMAGES,
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

      const { projectId, userId } = extractInfoFromPath(name);
      if (!projectId || !userId) {
        console.log('Cannot extract project ID or user ID from file path');
        return;
      }

      const { getStorage } = await import('firebase-admin/storage');
      const storage = getStorage();
      const file = storage.bucket(bucket).file(name);
      
      const [fileBuffer] = await file.download();

      const projectUseCases = Providers.getProjectProvider().create();
      const storageService = new FirebaseStorageService();
      const imageProcessingService = new ImageProcessingService(projectUseCases.imageRepository, storageService);

      const updateUseCase = new UpdateProjectImageUseCase(
        imageProcessingService,
        projectUseCases.projectRepository
      );

      const result = await updateUseCase.execute({
        projectId,
        userId,
        imageBuffer: fileBuffer,
        contentType
      });

      if (!result.success) {
        console.error('Failed to update project image:', result.error);
        return;
      }

      console.log('Project image updated successfully:', result.data);
    } catch (error) {
      console.error('Error processing project image:', error);
    }
  }
);

function extractInfoFromPath(filePath: string): { projectId: string | null; userId: string | null } {
  const parts = filePath.split('/');
  
  if (parts.length < 2) {
    return { projectId: null, userId: null };
  }
  
  return {
    projectId: parts[0],
    userId: parts[1]
  };
}