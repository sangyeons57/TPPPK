import { onUserProfileImageUpload } from '../../../triggers/user/userImage.trigger';
import { 
  setupFirebaseTest, 
  cleanupFirebaseTest 
} from '../../helpers/firebaseTestSetup';
import { Providers } from '../../../config/dependencies';
import { ProcessUserImageUseCase } from '../../../business/user/usecases/processUserImage.usecase';

// Mock the dependencies
jest.mock('../../../config/dependencies');
jest.mock('../../../business/user/usecases/processUserImage.usecase');
jest.mock('firebase-admin/storage');

describe('User Image Trigger Functions', () => {
  let mockUserUseCases: any;
  let mockProcessUseCase: any;
  let mockStorage: any;
  let mockFile: any;

  beforeAll(() => {
    setupFirebaseTest();
  });

  afterAll(() => {
    cleanupFirebaseTest();
  });

  beforeEach(() => {
    jest.clearAllMocks();
    
    // Mock file download
    mockFile = {
      download: jest.fn().mockResolvedValue([Buffer.from('mock-image-data')]),
    };

    // Mock storage
    mockStorage = {
      bucket: jest.fn().mockReturnValue({
        file: jest.fn().mockReturnValue(mockFile),
      }),
    };

    // Mock firebase-admin/storage
    const mockFirebaseStorage = {
      getStorage: jest.fn().mockReturnValue(mockStorage),
    };
    require('firebase-admin/storage').getStorage = mockFirebaseStorage.getStorage;

    // Mock user use cases
    mockUserUseCases = {
      imageProcessingService: 'mock-image-service',
      userProfileRepository: 'mock-user-repository',
    };

    // Mock provider
    const mockProvider = {
      create: jest.fn().mockReturnValue(mockUserUseCases),
    };

    (Providers.getUserProvider as jest.Mock).mockReturnValue(mockProvider);

    // Mock ProcessUserImageUseCase
    mockProcessUseCase = {
      execute: jest.fn(),
    };
    (ProcessUserImageUseCase as jest.Mock).mockImplementation(() => mockProcessUseCase);
  });

  describe('onUserProfileImageUpload', () => {
    const createMockEvent = (overrides: any = {}) => ({
      specversion: '1.0',
      id: 'test-event-id',
      source: 'test-source',
      type: 'google.cloud.storage.object.v1.finalized',
      time: new Date().toISOString(),
      data: {
        bucket: 'user-profiles-bucket',
        name: 'user-123/profile.jpg',
        contentType: 'image/jpeg',
        ...overrides,
      },
    });

    describe('Success Cases', () => {
      it('should successfully process user profile image', async () => {
        const mockResult = {
          success: true,
          data: {
            userId: 'user-123',
            processedImageUrl: 'https://storage.googleapis.com/bucket/user-123/profile-processed.jpg',
            thumbnailUrl: 'https://storage.googleapis.com/bucket/user-123/profile-thumb.jpg',
          },
        };

        mockProcessUseCase.execute.mockResolvedValue(mockResult);

        const event = createMockEvent();
        
        // Execute the trigger
        await onUserProfileImageUpload(event);

        // Verify file download was called
        expect(mockStorage.bucket).toHaveBeenCalledWith('user-profiles-bucket');
        expect(mockFile.download).toHaveBeenCalled();

        // Verify ProcessUserImageUseCase was created and executed
        expect(ProcessUserImageUseCase).toHaveBeenCalledWith(
          'mock-image-service',
          'mock-user-repository'
        );

        expect(mockProcessUseCase.execute).toHaveBeenCalledWith({
          userId: 'user-123',
          imageBuffer: Buffer.from('mock-image-data'),
          contentType: 'image/jpeg',
        });
      });

      it('should handle different image types', async () => {
        const imageTypes = ['image/png', 'image/gif', 'image/webp'];
        
        for (const contentType of imageTypes) {
          jest.clearAllMocks();
          
          const mockResult = {
            success: true,
            data: { userId: 'user-123' },
          };

          mockProcessUseCase.execute.mockResolvedValue(mockResult);

          const event = createMockEvent({ contentType });
          
          await onUserProfileImageUpload(event);

          expect(mockProcessUseCase.execute).toHaveBeenCalledWith(
            expect.objectContaining({
              contentType,
            })
          );
        }
      });

      it('should extract userId from different path formats', async () => {
        const pathTests = [
          { path: 'user-123/profile.jpg', expectedUserId: 'user-123' },
          { path: 'user-456/images/avatar.png', expectedUserId: 'user-456' },
          { path: 'test-user-789/profile-pic.jpeg', expectedUserId: 'test-user-789' },
        ];

        for (const { path, expectedUserId } of pathTests) {
          jest.clearAllMocks();
          
          const mockResult = {
            success: true,
            data: { userId: expectedUserId },
          };

          mockProcessUseCase.execute.mockResolvedValue(mockResult);

          const event = createMockEvent({ name: path });
          
          await onUserProfileImageUpload(event);

          expect(mockProcessUseCase.execute).toHaveBeenCalledWith(
            expect.objectContaining({
              userId: expectedUserId,
            })
          );
        }
      });
    });

    describe('Validation Cases', () => {
      it('should skip processing when name is missing', async () => {
        const event = createMockEvent({ name: null });
        
        await onUserProfileImageUpload(event);

        expect(mockProcessUseCase.execute).not.toHaveBeenCalled();
        expect(mockFile.download).not.toHaveBeenCalled();
      });

      it('should skip processing when contentType is missing', async () => {
        const event = createMockEvent({ contentType: null });
        
        await onUserProfileImageUpload(event);

        expect(mockProcessUseCase.execute).not.toHaveBeenCalled();
        expect(mockFile.download).not.toHaveBeenCalled();
      });

      it('should skip processing for non-image files', async () => {
        const nonImageTypes = [
          'application/pdf',
          'text/plain',
          'video/mp4',
          'audio/mp3',
        ];

        for (const contentType of nonImageTypes) {
          jest.clearAllMocks();
          
          const event = createMockEvent({ contentType });
          
          await onUserProfileImageUpload(event);

          expect(mockProcessUseCase.execute).not.toHaveBeenCalled();
          expect(mockFile.download).not.toHaveBeenCalled();
        }
      });

      it('should skip processing when userId cannot be extracted', async () => {
        const invalidPaths = [
          'profile.jpg', // No user ID prefix
          '', // Empty path
          '/', // Just separator
          'invalid-path', // No separator
        ];

        for (const name of invalidPaths) {
          jest.clearAllMocks();
          
          const event = createMockEvent({ name });
          
          await onUserProfileImageUpload(event);

          expect(mockProcessUseCase.execute).not.toHaveBeenCalled();
          expect(mockFile.download).not.toHaveBeenCalled();
        }
      });
    });

    describe('Error Handling', () => {
      it('should handle file download errors gracefully', async () => {
        mockFile.download.mockRejectedValue(new Error('File not found'));

        const event = createMockEvent();
        
        // Should not throw error
        await expect(onUserProfileImageUpload(event)).resolves.toBeUndefined();

        expect(mockProcessUseCase.execute).not.toHaveBeenCalled();
      });

      it('should handle storage service errors gracefully', async () => {
        mockStorage.bucket.mockImplementation(() => {
          throw new Error('Storage service unavailable');
        });

        const event = createMockEvent();
        
        // Should not throw error
        await expect(onUserProfileImageUpload(event)).resolves.toBeUndefined();

        expect(mockProcessUseCase.execute).not.toHaveBeenCalled();
      });

      it('should handle use case execution errors gracefully', async () => {
        mockProcessUseCase.execute.mockRejectedValue(new Error('Image processing failed'));

        const event = createMockEvent();
        
        // Should not throw error
        await expect(onUserProfileImageUpload(event)).resolves.toBeUndefined();

        expect(mockFile.download).toHaveBeenCalled();
        expect(mockProcessUseCase.execute).toHaveBeenCalled();
      });

      it('should handle use case failure result gracefully', async () => {
        const failureResult = {
          success: false,
          error: { message: 'Invalid image format' },
        };

        mockProcessUseCase.execute.mockResolvedValue(failureResult);

        const event = createMockEvent();
        
        // Should not throw error
        await expect(onUserProfileImageUpload(event)).resolves.toBeUndefined();

        expect(mockProcessUseCase.execute).toHaveBeenCalled();
      });

      it('should handle provider initialization errors gracefully', async () => {
        (Providers.getUserProvider as jest.Mock).mockImplementation(() => {
          throw new Error('Provider initialization failed');
        });

        const event = createMockEvent();
        
        // Should not throw error
        await expect(onUserProfileImageUpload(event)).resolves.toBeUndefined();
      });
    });

    describe('Edge Cases', () => {
      it('should handle empty file buffer', async () => {
        mockFile.download.mockResolvedValue([Buffer.alloc(0)]);

        const mockResult = {
          success: true,
          data: { userId: 'user-123' },
        };

        mockProcessUseCase.execute.mockResolvedValue(mockResult);

        const event = createMockEvent();
        
        await onUserProfileImageUpload(event);

        expect(mockProcessUseCase.execute).toHaveBeenCalledWith({
          userId: 'user-123',
          imageBuffer: Buffer.alloc(0),
          contentType: 'image/jpeg',
        });
      });

      it('should handle large file buffers', async () => {
        const largeBuffer = Buffer.alloc(10 * 1024 * 1024); // 10MB
        mockFile.download.mockResolvedValue([largeBuffer]);

        const mockResult = {
          success: true,
          data: { userId: 'user-123' },
        };

        mockProcessUseCase.execute.mockResolvedValue(mockResult);

        const event = createMockEvent();
        
        await onUserProfileImageUpload(event);

        expect(mockProcessUseCase.execute).toHaveBeenCalledWith({
          userId: 'user-123',
          imageBuffer: largeBuffer,
          contentType: 'image/jpeg',
        });
      });

      it('should handle special characters in user ID', async () => {
        const specialUserIds = [
          'user_123',
          'user-with-dashes',
          'user.with.dots',
          'user@email.com',
        ];

        for (const userId of specialUserIds) {
          jest.clearAllMocks();
          
          const mockResult = {
            success: true,
            data: { userId },
          };

          mockProcessUseCase.execute.mockResolvedValue(mockResult);

          const event = createMockEvent({ name: `${userId}/profile.jpg` });
          
          await onUserProfileImageUpload(event);

          expect(mockProcessUseCase.execute).toHaveBeenCalledWith(
            expect.objectContaining({
              userId,
            })
          );
        }
      });
    });

    describe('Performance', () => {
      it('should process multiple images concurrently', async () => {
        const mockResult = {
          success: true,
          data: { userId: 'user-123' },
        };

        mockProcessUseCase.execute.mockResolvedValue(mockResult);

        const events = Array.from({ length: 5 }, (_, i) => 
          createMockEvent({ name: `user-${i}/profile.jpg` })
        );

        const promises = events.map(event => onUserProfileImageUpload(event));
        
        await Promise.all(promises);

        expect(mockProcessUseCase.execute).toHaveBeenCalledTimes(5);
      });
    });
  });
});