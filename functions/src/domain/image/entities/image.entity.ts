import { BaseEntity, ValueObject } from '../../../core/types';
import { ValidationError } from '../../../core/errors';
import { IMAGE_PROCESSING } from '../../../core/constants';

export class ImageUrl implements ValueObject<string> {
  constructor(public readonly value: string) {
    if (!value.startsWith('https://')) {
      throw new ValidationError('imageUrl', 'Image URL must be a valid HTTPS URL');
    }
  }

  equals(other: ImageUrl): boolean {
    return this.value === other.value;
  }
}

export class ImageSize implements ValueObject<number> {
  constructor(public readonly value: number) {
    if (value <= 0) {
      throw new ValidationError('imageSize', 'Image size must be positive');
    }
    if (value > IMAGE_PROCESSING.MAX_SIZE) {
      throw new ValidationError('imageSize', `Image size must be less than ${IMAGE_PROCESSING.MAX_SIZE} bytes`);
    }
  }

  equals(other: ImageSize): boolean {
    return this.value === other.value;
  }
}

export class ImageFormat implements ValueObject<string> {
  constructor(public readonly value: string) {
    if (!IMAGE_PROCESSING.SUPPORTED_FORMATS.includes(value.toLowerCase())) {
      throw new ValidationError('imageFormat', `Image format must be one of: ${IMAGE_PROCESSING.SUPPORTED_FORMATS.join(', ')}`);
    }
  }

  equals(other: ImageFormat): boolean {
    return this.value.toLowerCase() === other.value.toLowerCase();
  }
}

export enum ImageType {
  USER_PROFILE = 'user_profile',
  PROJECT_IMAGE = 'project_image'
}

export interface ProcessedImage extends BaseEntity {
  originalUrl: ImageUrl;
  thumbnailUrl?: ImageUrl;
  size: ImageSize;
  format: ImageFormat;
  type: ImageType;
  ownerId: string;
  width?: number;
  height?: number;
}

export class ProcessedImageEntity implements ProcessedImage {
  constructor(
    public readonly id: string,
    public readonly originalUrl: ImageUrl,
    public readonly size: ImageSize,
    public readonly format: ImageFormat,
    public readonly type: ImageType,
    public readonly ownerId: string,
    public readonly createdAt: Date,
    public readonly updatedAt: Date,
    public readonly thumbnailUrl?: ImageUrl,
    public readonly width?: number,
    public readonly height?: number
  ) {}

  addThumbnail(thumbnailUrl: ImageUrl): ProcessedImageEntity {
    return new ProcessedImageEntity(
      this.id,
      this.originalUrl,
      this.size,
      this.format,
      this.type,
      this.ownerId,
      this.createdAt,
      new Date(),
      thumbnailUrl,
      this.width,
      this.height
    );
  }

  updateDimensions(width: number, height: number): ProcessedImageEntity {
    if (width <= 0 || height <= 0) {
      throw new ValidationError('imageDimensions', 'Image dimensions must be positive');
    }

    return new ProcessedImageEntity(
      this.id,
      this.originalUrl,
      this.size,
      this.format,
      this.type,
      this.ownerId,
      this.createdAt,
      new Date(),
      this.thumbnailUrl,
      width,
      height
    );
  }
}