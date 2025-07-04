import { ProcessedImageEntity, ImageType, ImageUrl, ImageSize, ImageFormat } from './image.entity';
import { ImageRepository, ImageStorageService } from './image.repository';
import { CustomResult, Result } from '../../core/types';
import { ValidationError, InternalError } from '../../core/errors';
import { IMAGE_PROCESSING } from '../../core/constants';

export class ImageProcessingService {
  constructor(
    private readonly imageRepository: ImageRepository,
    private readonly storageService: ImageStorageService
  ) {}

  async processAndUploadImage(
    imageBuffer: Buffer,
    contentType: string,
    ownerId: string,
    type: ImageType
  ): Promise<CustomResult<ProcessedImageEntity>> {
    try {
      const formatValidation = await this.storageService.validateImageFormat(contentType);
      if (!formatValidation.success) {
        return Result.failure(new ValidationError('contentType', 'Invalid image format'));
      }

      if (imageBuffer.length > IMAGE_PROCESSING.MAX_SIZE) {
        return Result.failure(new ValidationError('imageSize', 'Image size exceeds maximum limit'));
      }

      const metadataResult = await this.storageService.getImageMetadata(imageBuffer);
      if (!metadataResult.success) {
        return Result.failure(new InternalError('Failed to get image metadata'));
      }

      const { width, height, format } = metadataResult.data;

      const path = this.generateImagePath(ownerId, type);
      const uploadResult = await this.storageService.uploadImage(imageBuffer, path, contentType);
      if (!uploadResult.success) {
        return Result.failure(new InternalError('Failed to upload image'));
      }

      const imageEntity = new ProcessedImageEntity(
        this.generateId(),
        new ImageUrl(uploadResult.data.value),
        new ImageSize(imageBuffer.length),
        new ImageFormat(format),
        type,
        ownerId,
        new Date(),
        new Date(),
        undefined,
        width,
        height
      );

      const thumbnailResult = await this.generateAndUploadThumbnail(
        imageBuffer,
        ownerId,
        type,
        contentType
      );

      if (thumbnailResult.success) {
        const entityWithThumbnail = imageEntity.addThumbnail(thumbnailResult.data);
        return await this.imageRepository.save(entityWithThumbnail);
      }

      return await this.imageRepository.save(imageEntity);
    } catch (error) {
      return Result.failure(new InternalError(`Image processing failed: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async deleteImage(imageId: string): Promise<CustomResult<void>> {
    try {
      const imageResult = await this.imageRepository.findById(imageId);
      if (!imageResult.success) {
        return Result.failure(imageResult.error);
      }

      const image = imageResult.data;
      if (!image) {
        return Result.success(undefined);
      }

      await this.storageService.deleteImage(image.originalUrl);
      
      if (image.thumbnailUrl) {
        await this.storageService.deleteImage(image.thumbnailUrl);
      }

      return await this.imageRepository.delete(imageId);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to delete image: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async replaceImage(
    oldImageId: string,
    newImageBuffer: Buffer,
    contentType: string,
    ownerId: string,
    type: ImageType
  ): Promise<CustomResult<ProcessedImageEntity>> {
    try {
      const deleteResult = await this.deleteImage(oldImageId);
      if (!deleteResult.success) {
        return Result.failure(deleteResult.error);
      }

      return await this.processAndUploadImage(newImageBuffer, contentType, ownerId, type);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to replace image: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  private async generateAndUploadThumbnail(
    imageBuffer: Buffer,
    ownerId: string,
    type: ImageType,
    contentType: string
  ): Promise<CustomResult<ImageUrl>> {
    try {
      const thumbnailBuffer = await this.storageService.generateThumbnail(
        imageBuffer,
        IMAGE_PROCESSING.THUMBNAIL_SIZE,
        IMAGE_PROCESSING.THUMBNAIL_SIZE
      );

      if (!thumbnailBuffer.success) {
        return Result.failure(thumbnailBuffer.error);
      }

      const thumbnailPath = this.generateImagePath(ownerId, type, 'thumbnail');
      return await this.storageService.uploadImage(thumbnailBuffer.data, thumbnailPath, contentType);
    } catch (error) {
      return Result.failure(new InternalError(`Thumbnail generation failed: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  private generateImagePath(ownerId: string, type: ImageType, variant?: string): string {
    const timestamp = Date.now();
    const variantSuffix = variant ? `_${variant}` : '';
    return `${type}/${ownerId}/${timestamp}${variantSuffix}`;
  }

  private generateId(): string {
    return `img_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }
}