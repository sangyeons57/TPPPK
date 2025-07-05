import { updateUserProfileFunction } from '../../../triggers/user/userProfile.trigger';
import { 
  setupFirebaseTest, 
  cleanupFirebaseTest 
} from '../../helpers/firebaseTestSetup';
import { 
  testUnauthenticatedCallable,
  testAuthenticatedCallable,
  assertCallableSuccess,
  assertCallableError,
  generateTestData 
} from '../../helpers/triggerTestUtils';
import { Providers } from '../../../config/dependencies';

// Mock the dependencies
jest.mock('../../../config/dependencies');

describe('User Profile Trigger Functions', () => {
  let testEnv: any;
  let mockUserUseCases: any;

  beforeAll(() => {
    testEnv = setupFirebaseTest();
  });

  afterAll(() => {
    cleanupFirebaseTest();
  });

  beforeEach(() => {
    jest.clearAllMocks();
    
    // Mock the user use cases
    mockUserUseCases = {
      updateUserProfileUseCase: {
        execute: jest.fn(),
      },
    };

    // Mock the provider
    const mockProvider = {
      create: jest.fn().mockReturnValue(mockUserUseCases),
    };

    (Providers.getUserProvider as jest.Mock).mockReturnValue(mockProvider);
  });

  describe('updateUserProfileFunction', () => {
    describe('Success Cases', () => {
      it('should successfully update all profile fields', async () => {
        const mockUpdateResponse = {
          success: true,
          data: {
            userProfile: {
              uid: 'user-123',
              username: 'updated_user',
              profileImage: 'https://example.com/profile.jpg',
              bio: 'Updated bio',
              displayName: 'Updated Name',
              email: 'user@example.com',
            },
          },
        };

        mockUserUseCases.updateUserProfileUseCase.execute.mockResolvedValue(mockUpdateResponse);

        const updateRequest = {
          userId: 'user-123',
          username: 'updated_user',
          profileImage: 'https://example.com/profile.jpg',
          bio: 'Updated bio',
          displayName: 'Updated Name',
        };

        const response = await testUnauthenticatedCallable(
          updateUserProfileFunction,
          updateRequest
        );

        assertCallableSuccess(response);
        expect(response.data.userProfile).toEqual(mockUpdateResponse.data.userProfile);

        // Verify use case was called with correct parameters
        expect(mockUserUseCases.updateUserProfileUseCase.execute).toHaveBeenCalledWith({
          userId: updateRequest.userId,
          username: updateRequest.username,
          profileImage: updateRequest.profileImage,
          bio: updateRequest.bio,
          displayName: updateRequest.displayName,
        });
      });

      it('should update only specified fields', async () => {
        const mockUpdateResponse = {
          success: true,
          data: {
            userProfile: {
              uid: 'user-123',
              username: 'new_username',
              email: 'user@example.com',
              // Other fields unchanged
            },
          },
        };

        mockUserUseCases.updateUserProfileUseCase.execute.mockResolvedValue(mockUpdateResponse);

        const updateRequest = {
          userId: 'user-123',
          username: 'new_username',
          // Only updating username
        };

        const response = await testUnauthenticatedCallable(
          updateUserProfileFunction,
          updateRequest
        );

        assertCallableSuccess(response);
        expect(mockUserUseCases.updateUserProfileUseCase.execute).toHaveBeenCalledWith({
          userId: 'user-123',
          username: 'new_username',
          profileImage: undefined,
          bio: undefined,
          displayName: undefined,
        });
      });

      it('should handle empty optional fields', async () => {
        const mockUpdateResponse = {
          success: true,
          data: {
            userProfile: {
              uid: 'user-123',
              email: 'user@example.com',
            },
          },
        };

        mockUserUseCases.updateUserProfileUseCase.execute.mockResolvedValue(mockUpdateResponse);

        const updateRequest = {
          userId: 'user-123',
          username: '',
          bio: '',
          displayName: '',
        };

        const response = await testUnauthenticatedCallable(
          updateUserProfileFunction,
          updateRequest
        );

        assertCallableSuccess(response);
        expect(mockUserUseCases.updateUserProfileUseCase.execute).toHaveBeenCalledWith({
          userId: 'user-123',
          username: '',
          profileImage: undefined,
          bio: '',
          displayName: '',
        });
      });
    });

    describe('Validation Errors', () => {
      it('should require userId', async () => {
        const response = await testUnauthenticatedCallable(
          updateUserProfileFunction,
          {
            username: 'testuser',
            // userId missing
          }
        );

        assertCallableError(response, 'invalid-argument');
        expect(response.error!.message).toContain('User ID is required');
      });

      it('should handle empty userId', async () => {
        const response = await testUnauthenticatedCallable(
          updateUserProfileFunction,
          {
            userId: '',
            username: 'testuser',
          }
        );

        assertCallableError(response, 'invalid-argument');
        expect(response.error!.message).toContain('User ID is required');
      });

      it('should handle null userId', async () => {
        const response = await testUnauthenticatedCallable(
          updateUserProfileFunction,
          {
            userId: null,
            username: 'testuser',
          }
        );

        assertCallableError(response, 'invalid-argument');
        expect(response.error!.message).toContain('User ID is required');
      });
    });

    describe('Business Logic Errors', () => {
      it('should handle user not found error', async () => {
        const notFoundError = {
          success: false,
          error: {
            name: 'NOT_FOUND',
            message: 'User not found',
          },
        };

        mockUserUseCases.updateUserProfileUseCase.execute.mockResolvedValue(notFoundError);

        const response = await testUnauthenticatedCallable(
          updateUserProfileFunction,
          {
            userId: 'non-existent-user',
            username: 'newusername',
          }
        );

        assertCallableError(response, 'not-found');
        expect(response.error!.message).toBe('User not found');
      });

      it('should handle validation errors', async () => {
        const validationError = {
          success: false,
          error: {
            name: 'VALIDATION_ERROR',
            message: 'Username already taken',
          },
        };

        mockUserUseCases.updateUserProfileUseCase.execute.mockResolvedValue(validationError);

        const response = await testUnauthenticatedCallable(
          updateUserProfileFunction,
          {
            userId: 'user-123',
            username: 'taken_username',
          }
        );

        assertCallableError(response, 'invalid-argument');
        expect(response.error!.message).toBe('Username already taken');
      });

      it('should handle other business logic errors', async () => {
        const businessError = {
          success: false,
          error: {
            name: 'PERMISSION_DENIED',
            message: 'Insufficient permissions to update profile',
          },
        };

        mockUserUseCases.updateUserProfileUseCase.execute.mockResolvedValue(businessError);

        const response = await testUnauthenticatedCallable(
          updateUserProfileFunction,
          {
            userId: 'user-123',
            username: 'newusername',
          }
        );

        assertCallableError(response, 'internal');
        expect(response.error!.message).toBe('Insufficient permissions to update profile');
      });
    });

    describe('System Errors', () => {
      it('should handle unexpected errors', async () => {
        mockUserUseCases.updateUserProfileUseCase.execute.mockRejectedValue(
          new Error('Database connection failed')
        );

        const response = await testUnauthenticatedCallable(
          updateUserProfileFunction,
          {
            userId: 'user-123',
            username: 'newusername',
          }
        );

        assertCallableError(response, 'internal');
        expect(response.error!.message).toContain('Update profile failed');
        expect(response.error!.message).toContain('Database connection failed');
      });

      it('should handle unknown error types', async () => {
        mockUserUseCases.updateUserProfileUseCase.execute.mockRejectedValue('Unknown error');

        const response = await testUnauthenticatedCallable(
          updateUserProfileFunction,
          {
            userId: 'user-123',
            username: 'newusername',
          }
        );

        assertCallableError(response, 'internal');
        expect(response.error!.message).toContain('Unknown error');
      });
    });

    describe('Data Validation Edge Cases', () => {
      it('should handle special characters in username', async () => {
        const mockUpdateResponse = {
          success: true,
          data: {
            userProfile: {
              uid: 'user-123',
              username: 'user_name-123',
            },
          },
        };

        mockUserUseCases.updateUserProfileUseCase.execute.mockResolvedValue(mockUpdateResponse);

        const response = await testUnauthenticatedCallable(
          updateUserProfileFunction,
          {
            userId: 'user-123',
            username: 'user_name-123',
          }
        );

        assertCallableSuccess(response);
        expect(mockUserUseCases.updateUserProfileUseCase.execute).toHaveBeenCalledWith(
          expect.objectContaining({
            username: 'user_name-123',
          })
        );
      });

      it('should handle long bio text', async () => {
        const longBio = 'A'.repeat(1000);
        const mockUpdateResponse = {
          success: true,
          data: {
            userProfile: {
              uid: 'user-123',
              bio: longBio,
            },
          },
        };

        mockUserUseCases.updateUserProfileUseCase.execute.mockResolvedValue(mockUpdateResponse);

        const response = await testUnauthenticatedCallable(
          updateUserProfileFunction,
          {
            userId: 'user-123',
            bio: longBio,
          }
        );

        assertCallableSuccess(response);
        expect(mockUserUseCases.updateUserProfileUseCase.execute).toHaveBeenCalledWith(
          expect.objectContaining({
            bio: longBio,
          })
        );
      });

      it('should handle unicode characters in display name', async () => {
        const unicodeName = '🔥 Firebase ユーザー 特殊文字 123';
        const mockUpdateResponse = {
          success: true,
          data: {
            userProfile: {
              uid: 'user-123',
              displayName: unicodeName,
            },
          },
        };

        mockUserUseCases.updateUserProfileUseCase.execute.mockResolvedValue(mockUpdateResponse);

        const response = await testUnauthenticatedCallable(
          updateUserProfileFunction,
          {
            userId: 'user-123',
            displayName: unicodeName,
          }
        );

        assertCallableSuccess(response);
        expect(mockUserUseCases.updateUserProfileUseCase.execute).toHaveBeenCalledWith(
          expect.objectContaining({
            displayName: unicodeName,
          })
        );
      });

      it('should handle valid profile image URLs', async () => {
        const profileImageUrl = 'https://storage.googleapis.com/bucket/profile-images/user-123.jpg';
        const mockUpdateResponse = {
          success: true,
          data: {
            userProfile: {
              uid: 'user-123',
              profileImage: profileImageUrl,
            },
          },
        };

        mockUserUseCases.updateUserProfileUseCase.execute.mockResolvedValue(mockUpdateResponse);

        const response = await testUnauthenticatedCallable(
          updateUserProfileFunction,
          {
            userId: 'user-123',
            profileImage: profileImageUrl,
          }
        );

        assertCallableSuccess(response);
        expect(mockUserUseCases.updateUserProfileUseCase.execute).toHaveBeenCalledWith(
          expect.objectContaining({
            profileImage: profileImageUrl,
          })
        );
      });
    });

    describe('Authentication Context', () => {
      it('should work with authenticated requests', async () => {
        const mockUpdateResponse = {
          success: true,
          data: {
            userProfile: {
              uid: 'user-123',
              username: 'updated_user',
            },
          },
        };

        mockUserUseCases.updateUserProfileUseCase.execute.mockResolvedValue(mockUpdateResponse);

        const response = await testAuthenticatedCallable(
          updateUserProfileFunction,
          {
            userId: 'user-123',
            username: 'updated_user',
          },
          { uid: 'user-123' }
        );

        assertCallableSuccess(response);
        expect(response.data.userProfile.username).toBe('updated_user');
      });

      it('should work with unauthenticated requests', async () => {
        const mockUpdateResponse = {
          success: true,
          data: {
            userProfile: {
              uid: 'user-123',
              username: 'updated_user',
            },
          },
        };

        mockUserUseCases.updateUserProfileUseCase.execute.mockResolvedValue(mockUpdateResponse);

        const response = await testUnauthenticatedCallable(
          updateUserProfileFunction,
          {
            userId: 'user-123',
            username: 'updated_user',
          }
        );

        assertCallableSuccess(response);
        expect(response.data.userProfile.username).toBe('updated_user');
      });
    });
  });
});