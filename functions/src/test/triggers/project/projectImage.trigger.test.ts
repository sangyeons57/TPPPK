import { onProjectImageUpload } from '../../../triggers/project/projectImage.trigger';
import { 
  setupFirebaseTest, 
  cleanupFirebaseTest 
} from '../../helpers/firebaseTestSetup';
import { Providers } from '../../../config/dependencies';
import { UpdateProjectImageUseCase } from '../../../business/project/usecases/updateProjectImage.usecase';

// Mock the dependencies
jest.mock('../../../config/dependencies');
jest.mock('../../../business/project/usecases/updateProjectImage.usecase');
jest.mock('firebase-admin/storage');

describe('Project Image Trigger Functions', () => {
  let mockProjectUseCases: any;
  let mockUpdateUseCase: any;
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
      download: jest.fn().mockResolvedValue([Buffer.from('mock-project-image-data')]),
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

    // Mock project use cases
    mockProjectUseCases = {
      imageProcessingService: 'mock-image-service',
      projectRepository: 'mock-project-repository',
    };

    // Mock provider
    const mockProvider = {
      create: jest.fn().mockReturnValue(mockProjectUseCases),
    };

    (Providers.getProjectProvider as jest.Mock).mockReturnValue(mockProvider);

    // Mock UpdateProjectImageUseCase
    mockUpdateUseCase = {
      execute: jest.fn(),
    };
    (UpdateProjectImageUseCase as jest.Mock).mockImplementation(() => mockUpdateUseCase);
  });

  describe('onProjectImageUpload', () => {
    const createMockEvent = (overrides: any = {}) => ({
      specversion: '1.0',
      id: 'test-event-id',
      source: 'test-source',
      type: 'google.cloud.storage.object.v1.finalized',
      time: new Date().toISOString(),
      data: {
        bucket: 'project-images-bucket',
        name: 'project-123/user-456/project-image.jpg',
        contentType: 'image/jpeg',
        ...overrides,
      },
    });

    describe('Success Cases', () => {
      it('should successfully process project image', async () => {
        const mockResult = {
          success: true,
          data: {
            projectId: 'project-123',
            userId: 'user-456',
            processedImageUrl: 'https://storage.googleapis.com/bucket/project-123/image-processed.jpg',
            thumbnailUrl: 'https://storage.googleapis.com/bucket/project-123/image-thumb.jpg',
          },
        };

        mockUpdateUseCase.execute.mockResolvedValue(mockResult);

        const event = createMockEvent();
        
        await onProjectImageUpload(event);

        // Verify file download was called
        expect(mockStorage.bucket).toHaveBeenCalledWith('project-images-bucket');
        expect(mockFile.download).toHaveBeenCalled();

        // Verify UpdateProjectImageUseCase was created and executed
        expect(UpdateProjectImageUseCase).toHaveBeenCalledWith(
          'mock-image-service',
          'mock-project-repository'
        );

        expect(mockUpdateUseCase.execute).toHaveBeenCalledWith({
          projectId: 'project-123',
          userId: 'user-456',
          imageBuffer: Buffer.from('mock-project-image-data'),
          contentType: 'image/jpeg',
        });
      });

      it('should handle different image types', async () => {
        const imageTypes = ['image/png', 'image/gif', 'image/webp', 'image/svg+xml'];
        
        for (const contentType of imageTypes) {
          jest.clearAllMocks();
          
          const mockResult = {
            success: true,
            data: { projectId: 'project-123', userId: 'user-456' },
          };

          mockUpdateUseCase.execute.mockResolvedValue(mockResult);

          const event = createMockEvent({ contentType });
          
          await onProjectImageUpload(event);

          expect(mockUpdateUseCase.execute).toHaveBeenCalledWith(
            expect.objectContaining({
              contentType,
            })
          );
        }
      });

      it('should extract projectId and userId from different path formats', async () => {
        const pathTests = [
          { 
            path: 'project-123/user-456/image.jpg', 
            expectedProjectId: 'project-123', 
            expectedUserId: 'user-456' 
          },
          { 
            path: 'proj_789/user_123/assets/banner.png', 
            expectedProjectId: 'proj_789', 
            expectedUserId: 'user_123' 
          },
          { 
            path: 'project-with-dashes/user-with-dots.email/logo.jpeg', 
            expectedProjectId: 'project-with-dashes', 
            expectedUserId: 'user-with-dots.email' 
          },
        ];

        for (const { path, expectedProjectId, expectedUserId } of pathTests) {
          jest.clearAllMocks();
          
          const mockResult = {
            success: true,
            data: { projectId: expectedProjectId, userId: expectedUserId },
          };

          mockUpdateUseCase.execute.mockResolvedValue(mockResult);

          const event = createMockEvent({ name: path });
          
          await onProjectImageUpload(event);

          expect(mockUpdateUseCase.execute).toHaveBeenCalledWith(
            expect.objectContaining({
              projectId: expectedProjectId,
              userId: expectedUserId,
            })
          );
        }
      });
    });

    describe('Validation Cases', () => {
      it('should skip processing when name is missing', async () => {
        const event = createMockEvent({ name: null });
        
        await onProjectImageUpload(event);

        expect(mockUpdateUseCase.execute).not.toHaveBeenCalled();
        expect(mockFile.download).not.toHaveBeenCalled();
      });

      it('should skip processing when contentType is missing', async () => {
        const event = createMockEvent({ contentType: null });
        
        await onProjectImageUpload(event);

        expect(mockUpdateUseCase.execute).not.toHaveBeenCalled();
        expect(mockFile.download).not.toHaveBeenCalled();
      });

      it('should skip processing for non-image files', async () => {
        const nonImageTypes = [
          'application/pdf',
          'text/plain',
          'video/mp4',
          'audio/mp3',
          'application/json',
        ];

        for (const contentType of nonImageTypes) {
          jest.clearAllMocks();
          
          const event = createMockEvent({ contentType });
          
          await onProjectImageUpload(event);

          expect(mockUpdateUseCase.execute).not.toHaveBeenCalled();
          expect(mockFile.download).not.toHaveBeenCalled();
        }
      });

      it('should skip processing when projectId or userId cannot be extracted', async () => {
        const invalidPaths = [
          'project-123', // Missing user ID
          'project-123/', // Empty user ID
          '', // Empty path
          '/', // Just separator
          'invalid-path', // No separator
          '/project-123/user-456', // Leading separator
        ];

        for (const name of invalidPaths) {
          jest.clearAllMocks();
          
          const event = createMockEvent({ name });
          
          await onProjectImageUpload(event);

          expect(mockUpdateUseCase.execute).not.toHaveBeenCalled();
          expect(mockFile.download).not.toHaveBeenCalled();
        }
      });
    });

    describe('Error Handling', () => {
      it('should handle file download errors gracefully', async () => {
        mockFile.download.mockRejectedValue(new Error('File not found'));

        const event = createMockEvent();
        
        // Should not throw error
        await expect(onProjectImageUpload(event)).resolves.toBeUndefined();

        expect(mockUpdateUseCase.execute).not.toHaveBeenCalled();
      });

      it('should handle storage service errors gracefully', async () => {
        mockStorage.bucket.mockImplementation(() => {
          throw new Error('Storage service unavailable');
        });

        const event = createMockEvent();
        
        // Should not throw error
        await expect(onProjectImageUpload(event)).resolves.toBeUndefined();

        expect(mockUpdateUseCase.execute).not.toHaveBeenCalled();
      });

      it('should handle use case execution errors gracefully', async () => {
        mockUpdateUseCase.execute.mockRejectedValue(new Error('Image processing failed'));

        const event = createMockEvent();
        
        // Should not throw error
        await expect(onProjectImageUpload(event)).resolves.toBeUndefined();

        expect(mockFile.download).toHaveBeenCalled();
        expect(mockUpdateUseCase.execute).toHaveBeenCalled();
      });

      it('should handle use case failure result gracefully', async () => {
        const failureResult = {
          success: false,
          error: { message: 'Project not found' },
        };

        mockUpdateUseCase.execute.mockResolvedValue(failureResult);

        const event = createMockEvent();
        
        // Should not throw error
        await expect(onProjectImageUpload(event)).resolves.toBeUndefined();

        expect(mockUpdateUseCase.execute).toHaveBeenCalled();
      });

      it('should handle provider initialization errors gracefully', async () => {
        (Providers.getProjectProvider as jest.Mock).mockImplementation(() => {
          throw new Error('Provider initialization failed');
        });

        const event = createMockEvent();
        
        // Should not throw error
        await expect(onProjectImageUpload(event)).resolves.toBeUndefined();
      });

      it('should handle UseCase constructor errors gracefully', async () => {
        (UpdateProjectImageUseCase as jest.Mock).mockImplementation(() => {
          throw new Error('UseCase construction failed');
        });

        const event = createMockEvent();
        
        // Should not throw error
        await expect(onProjectImageUpload(event)).resolves.toBeUndefined();
      });
    });

    describe('Edge Cases', () => {
      it('should handle empty file buffer', async () => {
        mockFile.download.mockResolvedValue([Buffer.alloc(0)]);

        const mockResult = {
          success: true,
          data: { projectId: 'project-123', userId: 'user-456' },
        };

        mockUpdateUseCase.execute.mockResolvedValue(mockResult);

        const event = createMockEvent();
        
        await onProjectImageUpload(event);

        expect(mockUpdateUseCase.execute).toHaveBeenCalledWith({
          projectId: 'project-123',
          userId: 'user-456',
          imageBuffer: Buffer.alloc(0),
          contentType: 'image/jpeg',
        });
      });

      it('should handle large file buffers', async () => {
        const largeBuffer = Buffer.alloc(50 * 1024 * 1024); // 50MB
        mockFile.download.mockResolvedValue([largeBuffer]);

        const mockResult = {
          success: true,
          data: { projectId: 'project-123', userId: 'user-456' },
        };

        mockUpdateUseCase.execute.mockResolvedValue(mockResult);

        const event = createMockEvent();
        
        await onProjectImageUpload(event);

        expect(mockUpdateUseCase.execute).toHaveBeenCalledWith({
          projectId: 'project-123',
          userId: 'user-456',
          imageBuffer: largeBuffer,
          contentType: 'image/jpeg',
        });
      });

      it('should handle special characters in IDs', async () => {
        const specialTests = [
          { projectId: 'project_123', userId: 'user_456' },
          { projectId: 'project-with-dashes', userId: 'user-with-dashes' },
          { projectId: 'project.with.dots', userId: 'user.with.dots' },
          { projectId: 'project@special', userId: 'user@email.com' },
        ];

        for (const { projectId, userId } of specialTests) {
          jest.clearAllMocks();
          
          const mockResult = {
            success: true,
            data: { projectId, userId },
          };

          mockUpdateUseCase.execute.mockResolvedValue(mockResult);

          const event = createMockEvent({ name: `${projectId}/${userId}/image.jpg` });
          
          await onProjectImageUpload(event);

          expect(mockUpdateUseCase.execute).toHaveBeenCalledWith(
            expect.objectContaining({
              projectId,
              userId,
            })
          );
        }
      });

      it('should handle nested file paths', async () => {
        const nestedPaths = [
          'project-123/user-456/subfolder/image.jpg',
          'project-123/user-456/assets/images/banner.png',
          'project-123/user-456/deep/nested/path/logo.gif',
        ];

        for (const path of nestedPaths) {
          jest.clearAllMocks();
          
          const mockResult = {
            success: true,
            data: { projectId: 'project-123', userId: 'user-456' },
          };

          mockUpdateUseCase.execute.mockResolvedValue(mockResult);

          const event = createMockEvent({ name: path });
          
          await onProjectImageUpload(event);

          expect(mockUpdateUseCase.execute).toHaveBeenCalledWith(
            expect.objectContaining({
              projectId: 'project-123',
              userId: 'user-456',
            })
          );
        }
      });
    });

    describe('Performance', () => {
      it('should process multiple project images concurrently', async () => {
        const mockResult = {
          success: true,
          data: { projectId: 'project-123', userId: 'user-456' },
        };

        mockUpdateUseCase.execute.mockResolvedValue(mockResult);

        const events = Array.from({ length: 5 }, (_, i) => 
          createMockEvent({ name: `project-${i}/user-${i}/image.jpg` })
        );

        const promises = events.map(event => onProjectImageUpload(event));
        
        await Promise.all(promises);

        expect(mockUpdateUseCase.execute).toHaveBeenCalledTimes(5);
      });

      it('should handle high frequency uploads', async () => {
        const mockResult = {
          success: true,
          data: { projectId: 'project-123', userId: 'user-456' },
        };

        mockUpdateUseCase.execute.mockResolvedValue(mockResult);

        const batchSize = 10;
        const events = Array.from({ length: batchSize }, (_, i) => 
          createMockEvent({ name: `project-123/user-456/batch-${i}.jpg` })
        );

        const startTime = Date.now();
        
        await Promise.all(events.map(event => onProjectImageUpload(event)));
        
        const endTime = Date.now();
        const totalTime = endTime - startTime;

        expect(mockUpdateUseCase.execute).toHaveBeenCalledTimes(batchSize);
        expect(totalTime).toBeLessThan(5000); // Should complete within 5 seconds
      });
    });
  });
});