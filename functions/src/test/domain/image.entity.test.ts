import { ProcessedImageEntity, ImageUrl, ImageSize, ImageFormat, ImageType } from '../../domain/image/image.entity';
import { ValidationError } from '../../core/errors';
import { IMAGE_PROCESSING } from '../../core/constants';
import { TestUtils } from '../helpers';

describe('Image Domain Entities', () => {
  describe('ImageUrl Value Object', () => {
    it('should create valid image URL with HTTPS', () => {
      const url = new ImageUrl('https://example.com/image.jpg');
      expect(url.value).toBe('https://example.com/image.jpg');
    });

    it('should throw ValidationError for non-HTTPS URL', () => {
      expect(() => new ImageUrl('http://example.com/image.jpg')).toThrow(ValidationError);
      expect(() => new ImageUrl('http://example.com/image.jpg')).toThrow('Image URL must be a valid HTTPS URL');
    });

    it('should throw ValidationError for invalid URL format', () => {
      expect(() => new ImageUrl('not-a-url')).toThrow(ValidationError);
      expect(() => new ImageUrl('not-a-url')).toThrow('Image URL must be a valid HTTPS URL');
    });

    it('should check equality correctly', () => {
      const url1 = new ImageUrl('https://example.com/image.jpg');
      const url2 = new ImageUrl('https://example.com/image.jpg');
      const url3 = new ImageUrl('https://example.com/other.jpg');

      expect(url1.equals(url2)).toBe(true);
      expect(url1.equals(url3)).toBe(false);
    });
  });

  describe('ImageSize Value Object', () => {
    it('should create valid image size', () => {
      const size = new ImageSize(1024 * 1024); // 1MB
      expect(size.value).toBe(1024 * 1024);
    });

    it('should throw ValidationError for zero size', () => {
      expect(() => new ImageSize(0)).toThrow(ValidationError);
      expect(() => new ImageSize(0)).toThrow('Image size must be positive');
    });

    it('should throw ValidationError for negative size', () => {
      expect(() => new ImageSize(-100)).toThrow(ValidationError);
      expect(() => new ImageSize(-100)).toThrow('Image size must be positive');
    });

    it('should throw ValidationError for size exceeding maximum', () => {
      const oversizeImage = IMAGE_PROCESSING.MAX_SIZE + 1;
      expect(() => new ImageSize(oversizeImage)).toThrow(ValidationError);
      expect(() => new ImageSize(oversizeImage)).toThrow(`Image size must be less than ${IMAGE_PROCESSING.MAX_SIZE} bytes`);
    });

    it('should allow maximum size', () => {
      const maxSize = new ImageSize(IMAGE_PROCESSING.MAX_SIZE);
      expect(maxSize.value).toBe(IMAGE_PROCESSING.MAX_SIZE);
    });

    it('should check equality correctly', () => {
      const size1 = new ImageSize(1024);
      const size2 = new ImageSize(1024);
      const size3 = new ImageSize(2048);

      expect(size1.equals(size2)).toBe(true);
      expect(size1.equals(size3)).toBe(false);
    });
  });

  describe('ImageFormat Value Object', () => {
    it('should create valid image format', () => {
      IMAGE_PROCESSING.SUPPORTED_FORMATS.forEach(format => {
        const imageFormat = new ImageFormat(format);
        expect(imageFormat.value).toBe(format);
      });
    });

    it('should be case insensitive for supported formats', () => {
      const jpegUpper = new ImageFormat('JPEG');
      const jpegLower = new ImageFormat('jpeg');
      expect(jpegUpper.equals(jpegLower)).toBe(true);
    });

    it('should throw ValidationError for unsupported format', () => {
      expect(() => new ImageFormat('gif')).toThrow(ValidationError);
      expect(() => new ImageFormat('gif')).toThrow(`Image format must be one of: ${IMAGE_PROCESSING.SUPPORTED_FORMATS.join(', ')}`);
    });

    it('should throw ValidationError for empty format', () => {
      expect(() => new ImageFormat('')).toThrow(ValidationError);
    });

    it('should check equality correctly (case insensitive)', () => {
      const format1 = new ImageFormat('jpg');
      const format2 = new ImageFormat('JPG');
      const format3 = new ImageFormat('png');

      expect(format1.equals(format2)).toBe(true);
      expect(format1.equals(format3)).toBe(false);
    });
  });

  describe('ProcessedImageEntity', () => {
    let mockDateNow: jest.SpyInstance;

    beforeEach(() => {
      mockDateNow = TestUtils.mockDateNow(TestUtils.createMockDate('2023-01-15T12:00:00.000Z'));
    });

    afterEach(() => {
      TestUtils.restoreAllMocks();
    });

    const createImageEntity = (overrides = {}) => {
      const defaults = {
        id: 'image_123',
        originalUrl: new ImageUrl('https://example.com/original.jpg'),
        size: new ImageSize(1024 * 1024),
        format: new ImageFormat('jpg'),
        type: ImageType.USER_PROFILE,
        ownerId: 'user_123',
        createdAt: new Date('2023-01-01T00:00:00Z'),
        updatedAt: new Date('2023-01-01T00:00:00Z'),
      };
      const data = { ...defaults, ...overrides };
      
      return new ProcessedImageEntity(
        data.id,
        data.originalUrl,
        data.size,
        data.format,
        data.type,
        data.ownerId,
        data.createdAt,
        data.updatedAt,
        data.thumbnailUrl,
        data.width,
        data.height
      );
    };

    it('should create processed image entity with required fields', () => {
      const image = createImageEntity();

      expect(image.id).toBe('image_123');
      expect(image.originalUrl.value).toBe('https://example.com/original.jpg');
      expect(image.size.value).toBe(1024 * 1024);
      expect(image.format.value).toBe('jpg');
      expect(image.type).toBe(ImageType.USER_PROFILE);
      expect(image.ownerId).toBe('user_123');
      expect(image.thumbnailUrl).toBeUndefined();
      expect(image.width).toBeUndefined();
      expect(image.height).toBeUndefined();
    });

    it('should create processed image entity with optional fields', () => {
      const thumbnailUrl = new ImageUrl('https://example.com/thumbnail.jpg');
      const image = createImageEntity({
        thumbnailUrl,
        width: 800,
        height: 600,
      });

      expect(image.thumbnailUrl).toBe(thumbnailUrl);
      expect(image.width).toBe(800);
      expect(image.height).toBe(600);
    });

    describe('addThumbnail', () => {
      it('should add thumbnail URL to image', () => {
        const originalImage = createImageEntity();
        const thumbnailUrl = new ImageUrl('https://example.com/thumbnail.jpg');

        const updatedImage = originalImage.addThumbnail(thumbnailUrl);

        expect(updatedImage.thumbnailUrl).toBe(thumbnailUrl);
        expect(updatedImage.updatedAt).toEqual(new Date('2023-01-15T12:00:00.000Z'));
        expect(updatedImage.id).toBe(originalImage.id);
        expect(updatedImage.originalUrl).toBe(originalImage.originalUrl);
        expect(updatedImage.size).toBe(originalImage.size);
        expect(updatedImage.format).toBe(originalImage.format);
      });

      it('should replace existing thumbnail URL', () => {
        const existingThumbnailUrl = new ImageUrl('https://example.com/old-thumbnail.jpg');
        const originalImage = createImageEntity({ thumbnailUrl: existingThumbnailUrl });
        const newThumbnailUrl = new ImageUrl('https://example.com/new-thumbnail.jpg');

        const updatedImage = originalImage.addThumbnail(newThumbnailUrl);

        expect(updatedImage.thumbnailUrl).toBe(newThumbnailUrl);
        expect(updatedImage.updatedAt).toEqual(new Date('2023-01-15T12:00:00.000Z'));
      });
    });

    describe('updateDimensions', () => {
      it('should update image dimensions', () => {
        const originalImage = createImageEntity();

        const updatedImage = originalImage.updateDimensions(1920, 1080);

        expect(updatedImage.width).toBe(1920);
        expect(updatedImage.height).toBe(1080);
        expect(updatedImage.updatedAt).toEqual(new Date('2023-01-15T12:00:00.000Z'));
        expect(updatedImage.id).toBe(originalImage.id);
        expect(updatedImage.originalUrl).toBe(originalImage.originalUrl);
      });

      it('should replace existing dimensions', () => {
        const originalImage = createImageEntity({ width: 800, height: 600 });

        const updatedImage = originalImage.updateDimensions(1920, 1080);

        expect(updatedImage.width).toBe(1920);
        expect(updatedImage.height).toBe(1080);
      });

      it('should throw ValidationError for zero width', () => {
        const image = createImageEntity();

        expect(() => image.updateDimensions(0, 100)).toThrow(ValidationError);
        expect(() => image.updateDimensions(0, 100)).toThrow('Image dimensions must be positive');
      });

      it('should throw ValidationError for zero height', () => {
        const image = createImageEntity();

        expect(() => image.updateDimensions(100, 0)).toThrow(ValidationError);
        expect(() => image.updateDimensions(100, 0)).toThrow('Image dimensions must be positive');
      });

      it('should throw ValidationError for negative width', () => {
        const image = createImageEntity();

        expect(() => image.updateDimensions(-100, 100)).toThrow(ValidationError);
        expect(() => image.updateDimensions(-100, 100)).toThrow('Image dimensions must be positive');
      });

      it('should throw ValidationError for negative height', () => {
        const image = createImageEntity();

        expect(() => image.updateDimensions(100, -100)).toThrow(ValidationError);
        expect(() => image.updateDimensions(100, -100)).toThrow('Image dimensions must be positive');
      });
    });

    describe('ImageType enum', () => {
      it('should support user profile image type', () => {
        const image = createImageEntity({ type: ImageType.USER_PROFILE });
        expect(image.type).toBe(ImageType.USER_PROFILE);
      });

      it('should support project image type', () => {
        const image = createImageEntity({ type: ImageType.PROJECT_IMAGE });
        expect(image.type).toBe(ImageType.PROJECT_IMAGE);
      });
    });
  });
});