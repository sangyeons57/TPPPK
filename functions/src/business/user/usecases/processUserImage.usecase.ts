import { ImageProcessingService, ProcessedImageEntity } from '../../../core/services/imageProcessing.service';
import { UserProfileRepository } from '../../../domain/user/repositories/userProfile.repository';
import { ImageType } from '../../../core/types/image.types';
import { CustomResult, Result } from '../../../core/types';
import { NotFoundError, ValidationError } from '../../../core/errors';

export interface ProcessUserImageRequest {
  userId: string;
  imageBuffer: Buffer;
  contentType: string;
  replaceExisting?: boolean;
}

export interface ProcessUserImageResponse {
  imageUrl: string;
  thumbnailUrl?: string;
  userProfile: any;
}

export class ProcessUserImageUseCase {
  constructor(
    private readonly imageProcessingService: ImageProcessingService,
    private readonly userProfileRepository: UserProfileRepository
  ) {}

  async execute(request: ProcessUserImageRequest): Promise<CustomResult<ProcessUserImageResponse>> {
    try {
      const userProfileResult = await this.userProfileRepository.findByUserId(request.userId);
      if (!userProfileResult.success) {
        return Result.failure(userProfileResult.error);
      }

      const userProfile = userProfileResult.data;
      if (!userProfile) {
        return Result.failure(new NotFoundError('UserProfile', request.userId));
      }

      const processResult = await this.imageProcessingService.processAndUploadImage(
        request.imageBuffer,
        request.contentType,
        request.userId,
        ImageType.USER_PROFILE
      );

      if (!processResult.success) {
        return Result.failure(processResult.error);
      }

      const processedImage = processResult.data;

      const updatedProfile = userProfile.updateProfile({
        profileImage: processedImage.originalUrl
      });

      const updateResult = await this.userProfileRepository.update(updatedProfile);
      if (!updateResult.success) {
        await this.imageProcessingService.deleteImage(processedImage.id);
        return Result.failure(updateResult.error);
      }

      return Result.success({
        imageUrl: processedImage.originalUrl.value,
        thumbnailUrl: processedImage.thumbnailUrl?.value,
        userProfile: updateResult.data
      });
    } catch (error) {
      return Result.failure(new Error(`Failed to process user image: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }
}