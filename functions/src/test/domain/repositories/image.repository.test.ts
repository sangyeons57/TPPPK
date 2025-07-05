import { ImageRepository, ImageStorageService } from '../../../domain/image/image.repository';
import { ProcessedImageEntity, ImageType, ImageUrl, ImageSize, ImageFormat } from '../../../domain/image/image.entity';
import { TestUtils } from '../../helpers';

describe('ImageRepository Contract Tests', () => {
  let repository: ImageRepository;
  let testImage: ProcessedImageEntity;

  beforeEach(() => {
    testImage = new ProcessedImageEntity(
      'image_123',
      new ImageUrl('https://example.com/original.jpg'),
      new ImageSize(1024 * 1024),
      new ImageFormat('jpg'),
      ImageType.USER_PROFILE,
      'user_123',
      new Date('2023-01-01T00:00:00Z'),
      new Date('2023-01-01T00:00:00Z'),
      new ImageUrl('https://example.com/thumbnail.jpg'),
      800,
      600
    );

    // Mock repository will be provided by concrete implementation tests
    repository = {} as ImageRepository;
  });

  describe('findById', () => {
    it('should return image when found', async () => {
      const mockResult = TestUtils.createSuccessResult(testImage);
      repository.findById = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findById('image_123');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(testImage);
      }
      expect(repository.findById).toHaveBeenCalledWith('image_123');
    });

    it('should return null when image not found', async () => {
      const mockResult = TestUtils.createSuccessResult(null);
      repository.findById = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findById('nonexistent');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBeNull();
      }
    });

    it('should return error when repository fails', async () => {
      const error = new Error('Database connection failed');
      const mockResult = TestUtils.createFailureResult(error);
      repository.findById = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findById('image_123');
      
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(error);
      }
    });
  });

  describe('findByOwnerId', () => {
    it('should return images owned by user', async () => {
      const images = [testImage];
      const mockResult = TestUtils.createSuccessResult(images);
      repository.findByOwnerId = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByOwnerId('user_123');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual(images);
      }
      expect(repository.findByOwnerId).toHaveBeenCalledWith('user_123');
    });

    it('should return empty array when no images found for owner', async () => {
      const mockResult = TestUtils.createSuccessResult([]);
      repository.findByOwnerId = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByOwnerId('user_456');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual([]);
      }
    });
  });

  describe('findByType', () => {
    it('should return images of specific type', async () => {
      const userProfileImages = [testImage];
      const mockResult = TestUtils.createSuccessResult(userProfileImages);
      repository.findByType = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByType(ImageType.USER_PROFILE);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual(userProfileImages);
      }
      expect(repository.findByType).toHaveBeenCalledWith(ImageType.USER_PROFILE);
    });

    it('should return empty array when no images of type found', async () => {
      const mockResult = TestUtils.createSuccessResult([]);
      repository.findByType = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByType(ImageType.PROJECT_IMAGE);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual([]);
      }
    });
  });

  describe('findByOwnerIdAndType', () => {
    it('should return images owned by user of specific type', async () => {
      const userProfileImages = [testImage];
      const mockResult = TestUtils.createSuccessResult(userProfileImages);
      repository.findByOwnerIdAndType = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByOwnerIdAndType('user_123', ImageType.USER_PROFILE);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual(userProfileImages);
      }
      expect(repository.findByOwnerIdAndType).toHaveBeenCalledWith('user_123', ImageType.USER_PROFILE);
    });

    it('should return empty array when no matching images found', async () => {
      const mockResult = TestUtils.createSuccessResult([]);
      repository.findByOwnerIdAndType = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByOwnerIdAndType('user_456', ImageType.PROJECT_IMAGE);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual([]);
      }
    });
  });

  describe('save', () => {
    it('should save image and return saved entity', async () => {
      const mockResult = TestUtils.createSuccessResult(testImage);
      repository.save = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.save(testImage);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(testImage);
      }
      expect(repository.save).toHaveBeenCalledWith(testImage);
    });

    it('should return error when save fails', async () => {
      const error = new Error('Save failed');
      const mockResult = TestUtils.createFailureResult(error);
      repository.save = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.save(testImage);
      
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(error);
      }
    });
  });

  describe('update', () => {
    it('should update image and return updated entity', async () => {
      const updatedImage = testImage.updateDimensions(1920, 1080);
      const mockResult = TestUtils.createSuccessResult(updatedImage);
      repository.update = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.update(updatedImage);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(updatedImage);
      }
      expect(repository.update).toHaveBeenCalledWith(updatedImage);
    });

    it('should return error when update fails', async () => {
      const error = new Error('Update failed');
      const mockResult = TestUtils.createFailureResult(error);
      repository.update = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.update(testImage);
      
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(error);
      }
    });
  });

  describe('delete', () => {
    it('should delete image by ID', async () => {
      const mockResult = TestUtils.createSuccessResult(undefined);
      repository.delete = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.delete('image_123');
      
      expect(result.success).toBe(true);
      expect(repository.delete).toHaveBeenCalledWith('image_123');
    });

    it('should return error when delete fails', async () => {
      const error = new Error('Delete failed');
      const mockResult = TestUtils.createFailureResult(error);
      repository.delete = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.delete('image_123');
      
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(error);
      }
    });
  });

  describe('exists', () => {
    it('should return true when image exists', async () => {
      const mockResult = TestUtils.createSuccessResult(true);
      repository.exists = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.exists('image_123');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(true);
      }
      expect(repository.exists).toHaveBeenCalledWith('image_123');
    });

    it('should return false when image does not exist', async () => {
      const mockResult = TestUtils.createSuccessResult(false);
      repository.exists = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.exists('nonexistent');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(false);
      }
    });
  });

  describe('deleteByUrl', () => {
    it('should delete image by URL', async () => {
      const imageUrl = new ImageUrl('https://example.com/image.jpg');
      const mockResult = TestUtils.createSuccessResult(undefined);
      repository.deleteByUrl = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.deleteByUrl(imageUrl);
      
      expect(result.success).toBe(true);
      expect(repository.deleteByUrl).toHaveBeenCalledWith(imageUrl);
    });

    it('should return error when delete by URL fails', async () => {
      const imageUrl = new ImageUrl('https://example.com/image.jpg');
      const error = new Error('Delete by URL failed');
      const mockResult = TestUtils.createFailureResult(error);
      repository.deleteByUrl = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.deleteByUrl(imageUrl);
      
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(error);
      }
    });
  });
});

describe('ImageStorageService Contract Tests', () => {
  let storageService: ImageStorageService;

  beforeEach(() => {
    // Mock storage service will be provided by concrete implementation tests
    storageService = {} as ImageStorageService;
  });

  describe('uploadImage', () => {
    it('should upload image and return URL', async () => {
      const imageBuffer = Buffer.from('fake-image-data');
      const expectedUrl = new ImageUrl('https://example.com/uploaded.jpg');
      const mockResult = TestUtils.createSuccessResult(expectedUrl);
      storageService.uploadImage = jest.fn().mockResolvedValue(mockResult);

      const result = await storageService.uploadImage(imageBuffer, 'path/to/image.jpg', 'image/jpeg');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(expectedUrl);
      }
      expect(storageService.uploadImage).toHaveBeenCalledWith(imageBuffer, 'path/to/image.jpg', 'image/jpeg');
    });

    it('should return error when upload fails', async () => {
      const imageBuffer = Buffer.from('fake-image-data');
      const error = new Error('Upload failed');
      const mockResult = TestUtils.createFailureResult(error);
      storageService.uploadImage = jest.fn().mockResolvedValue(mockResult);

      const result = await storageService.uploadImage(imageBuffer, 'path/to/image.jpg', 'image/jpeg');
      
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(error);
      }
    });
  });

  describe('deleteImage', () => {
    it('should delete image by URL', async () => {
      const imageUrl = new ImageUrl('https://example.com/image.jpg');
      const mockResult = TestUtils.createSuccessResult(undefined);
      storageService.deleteImage = jest.fn().mockResolvedValue(mockResult);

      const result = await storageService.deleteImage(imageUrl);
      
      expect(result.success).toBe(true);
      expect(storageService.deleteImage).toHaveBeenCalledWith(imageUrl);
    });

    it('should return error when delete fails', async () => {
      const imageUrl = new ImageUrl('https://example.com/image.jpg');
      const error = new Error('Delete failed');
      const mockResult = TestUtils.createFailureResult(error);
      storageService.deleteImage = jest.fn().mockResolvedValue(mockResult);

      const result = await storageService.deleteImage(imageUrl);
      
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(error);
      }
    });
  });

  describe('generateThumbnail', () => {
    it('should generate thumbnail from image buffer', async () => {
      const imageBuffer = Buffer.from('fake-image-data');
      const thumbnailBuffer = Buffer.from('fake-thumbnail-data');
      const mockResult = TestUtils.createSuccessResult(thumbnailBuffer);
      storageService.generateThumbnail = jest.fn().mockResolvedValue(mockResult);

      const result = await storageService.generateThumbnail(imageBuffer, 300, 300);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(thumbnailBuffer);
      }
      expect(storageService.generateThumbnail).toHaveBeenCalledWith(imageBuffer, 300, 300);
    });

    it('should return error when thumbnail generation fails', async () => {
      const imageBuffer = Buffer.from('fake-image-data');
      const error = new Error('Thumbnail generation failed');
      const mockResult = TestUtils.createFailureResult(error);
      storageService.generateThumbnail = jest.fn().mockResolvedValue(mockResult);

      const result = await storageService.generateThumbnail(imageBuffer, 300, 300);
      
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(error);
      }
    });
  });

  describe('validateImageFormat', () => {
    it('should return true for valid image format', async () => {
      const mockResult = TestUtils.createSuccessResult(true);
      storageService.validateImageFormat = jest.fn().mockResolvedValue(mockResult);

      const result = await storageService.validateImageFormat('image/jpeg');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(true);
      }
      expect(storageService.validateImageFormat).toHaveBeenCalledWith('image/jpeg');
    });

    it('should return false for invalid image format', async () => {
      const mockResult = TestUtils.createSuccessResult(false);
      storageService.validateImageFormat = jest.fn().mockResolvedValue(mockResult);

      const result = await storageService.validateImageFormat('text/plain');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(false);
      }
    });
  });

  describe('getImageMetadata', () => {
    it('should return image metadata', async () => {
      const imageBuffer = Buffer.from('fake-image-data');
      const metadata = { width: 800, height: 600, format: 'jpeg' };
      const mockResult = TestUtils.createSuccessResult(metadata);
      storageService.getImageMetadata = jest.fn().mockResolvedValue(mockResult);

      const result = await storageService.getImageMetadata(imageBuffer);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual(metadata);
      }
      expect(storageService.getImageMetadata).toHaveBeenCalledWith(imageBuffer);
    });

    it('should return error when metadata extraction fails', async () => {
      const imageBuffer = Buffer.from('invalid-image-data');
      const error = new Error('Metadata extraction failed');
      const mockResult = TestUtils.createFailureResult(error);
      storageService.getImageMetadata = jest.fn().mockResolvedValue(mockResult);

      const result = await storageService.getImageMetadata(imageBuffer);
      
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(error);
      }
    });
  });
});