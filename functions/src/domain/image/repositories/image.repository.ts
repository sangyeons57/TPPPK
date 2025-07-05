import { ProcessedImageEntity, ImageType, ImageUrl } from '../entities/image.entity';
import { CustomResult } from '../../../core/types';

export interface ImageRepository {
  findById(id: string): Promise<CustomResult<ProcessedImageEntity | null>>;
  findByOwnerId(ownerId: string): Promise<CustomResult<ProcessedImageEntity[]>>;
  findByType(type: ImageType): Promise<CustomResult<ProcessedImageEntity[]>>;
  findByOwnerIdAndType(ownerId: string, type: ImageType): Promise<CustomResult<ProcessedImageEntity[]>>;
  save(image: ProcessedImageEntity): Promise<CustomResult<ProcessedImageEntity>>;
  update(image: ProcessedImageEntity): Promise<CustomResult<ProcessedImageEntity>>;
  delete(id: string): Promise<CustomResult<void>>;
  exists(id: string): Promise<CustomResult<boolean>>;
  deleteByUrl(url: ImageUrl): Promise<CustomResult<void>>;
}

export interface ImageStorageService {
  uploadImage(file: Buffer, path: string, contentType: string): Promise<CustomResult<ImageUrl>>;
  deleteImage(url: ImageUrl): Promise<CustomResult<void>>;
  generateThumbnail(imageBuffer: Buffer, width: number, height: number): Promise<CustomResult<Buffer>>;
  validateImageFormat(contentType: string): Promise<CustomResult<boolean>>;
  getImageMetadata(imageBuffer: Buffer): Promise<CustomResult<{ width: number; height: number; format: string }>>;
}