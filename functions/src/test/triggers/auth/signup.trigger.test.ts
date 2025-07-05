import { registerUserFunction } from '../../../triggers/auth/signup.trigger';
import { 
  setupFirebaseTest, 
  cleanupFirebaseTest 
} from '../../helpers/firebaseTestSetup';
import { 
  testUnauthenticatedCallable,
  assertCallableSuccess,
  assertCallableError,
  generateTestData,
  testRequiredFields 
} from '../../helpers/triggerTestUtils';
import { Providers } from '../../../config/dependencies';

// Mock the dependencies
jest.mock('../../../config/dependencies');

describe('Auth Signup Trigger Functions', () => {
  let mockAuthUseCases: any;

  beforeAll(() => {
    setupFirebaseTest();
  });

  afterAll(() => {
    cleanupFirebaseTest();
  });

  beforeEach(() => {
    jest.clearAllMocks();
    
    // Mock the auth use cases
    mockAuthUseCases = {
      registerUserUseCase: {
        execute: jest.fn(),
      },
    };

    // Mock the provider
    const mockProvider = {
      create: jest.fn().mockReturnValue(mockAuthUseCases),
    };

    (Providers.getAuthSessionProvider as jest.Mock).mockReturnValue(mockProvider);
  });

  describe('registerUserFunction', () => {
    describe('Success Cases', () => {
      it('should successfully register user with all required fields', async () => {
        const mockRegisterResponse = {
          success: true,
          data: {
            userProfile: {
              uid: 'new-user-123',
              email: 'newuser@example.com',
              username: 'newuser',
              displayName: 'New User',
            },
            message: 'User registered successfully',
          },
        };

        mockAuthUseCases.registerUserUseCase.execute.mockResolvedValue(mockRegisterResponse);

        const registerRequest = {
          email: generateTestData.email('new'),
          username: 'newuser',
          password: generateTestData.password(),
          displayName: 'New User',
          deviceInfo: generateTestData.deviceInfo(),
        };

        const response = await testUnauthenticatedCallable(
          registerUserFunction,
          registerRequest
        );

        assertCallableSuccess(response);
        expect(response.data.userProfile).toEqual(mockRegisterResponse.data.userProfile);
        expect(response.data.message).toBe('User registered successfully');

        // Verify use case was called with correct parameters
        expect(mockAuthUseCases.registerUserUseCase.execute).toHaveBeenCalledWith({
          email: registerRequest.email,
          username: registerRequest.username,
          password: registerRequest.password,
          displayName: registerRequest.displayName,
          deviceInfo: registerRequest.deviceInfo,
          ipAddress: '127.0.0.1',
        });
      });

      it('should register user without optional fields', async () => {
        const mockRegisterResponse = {
          success: true,
          data: {
            userProfile: {
              uid: 'new-user-123',
              email: 'newuser@example.com',
              username: 'newuser',
            },
            message: 'User registered successfully',
          },
        };

        mockAuthUseCases.registerUserUseCase.execute.mockResolvedValue(mockRegisterResponse);

        const registerRequest = {
          email: generateTestData.email('minimal'),
          username: 'minimaluser',
          password: generateTestData.password(),
          // displayName and deviceInfo omitted
        };

        const response = await testUnauthenticatedCallable(
          registerUserFunction,
          registerRequest
        );

        assertCallableSuccess(response);
        expect(mockAuthUseCases.registerUserUseCase.execute).toHaveBeenCalledWith({
          email: registerRequest.email,
          username: registerRequest.username,
          password: registerRequest.password,
          displayName: undefined,
          deviceInfo: undefined,
          ipAddress: '127.0.0.1',
        });
      });
    });

    describe('Validation Errors', () => {
      it('should require email, username, and password', async () => {
        const validRequest = {
          email: generateTestData.email(),
          username: 'testuser',
          password: generateTestData.password(),
        };

        await testRequiredFields(registerUserFunction, validRequest, ['email', 'username', 'password']);
      });

      it('should handle empty email', async () => {
        const response = await testUnauthenticatedCallable(
          registerUserFunction,
          {
            email: '',
            username: 'testuser',
            password: generateTestData.password(),
          }
        );

        assertCallableError(response, 'invalid-argument');
        expect(response.error!.message).toContain('Email, username, and password are required');
      });

      it('should handle empty username', async () => {
        const response = await testUnauthenticatedCallable(
          registerUserFunction,
          {
            email: generateTestData.email(),
            username: '',
            password: generateTestData.password(),
          }
        );

        assertCallableError(response, 'invalid-argument');
        expect(response.error!.message).toContain('Email, username, and password are required');
      });

      it('should handle empty password', async () => {
        const response = await testUnauthenticatedCallable(
          registerUserFunction,
          {
            email: generateTestData.email(),
            username: 'testuser',
            password: '',
          }
        );

        assertCallableError(response, 'invalid-argument');
        expect(response.error!.message).toContain('Email, username, and password are required');
      });
    });

    describe('Business Logic Errors', () => {
      it('should handle conflict error (user already exists)', async () => {
        const conflictError = {
          success: false,
          error: {
            name: 'Conflict',
            message: 'User with this email already exists',
          },
        };

        mockAuthUseCases.registerUserUseCase.execute.mockResolvedValue(conflictError);

        const response = await testUnauthenticatedCallable(
          registerUserFunction,
          {
            email: 'existing@example.com',
            username: 'existinguser',
            password: generateTestData.password(),
          }
        );

        assertCallableError(response, 'already-exists');
        expect(response.error!.message).toBe('User with this email already exists');
      });

      it('should handle validation error', async () => {
        const validationError = {
          success: false,
          error: {
            name: 'ValidationError',
            message: 'Password does not meet requirements',
          },
        };

        mockAuthUseCases.registerUserUseCase.execute.mockResolvedValue(validationError);

        const response = await testUnauthenticatedCallable(
          registerUserFunction,
          {
            email: generateTestData.email(),
            username: 'testuser',
            password: 'weak',
          }
        );

        assertCallableError(response, 'invalid-argument');
        expect(response.error!.message).toBe('Password does not meet requirements');
      });

      it('should handle other business logic errors', async () => {
        const businessError = {
          success: false,
          error: {
            name: 'RateLimitError',
            message: 'Too many registration attempts',
          },
        };

        mockAuthUseCases.registerUserUseCase.execute.mockResolvedValue(businessError);

        const response = await testUnauthenticatedCallable(
          registerUserFunction,
          {
            email: generateTestData.email(),
            username: 'testuser',
            password: generateTestData.password(),
          }
        );

        assertCallableError(response, 'internal');
        expect(response.error!.message).toBe('Too many registration attempts');
      });
    });

    describe('System Errors', () => {
      it('should handle unexpected errors', async () => {
        mockAuthUseCases.registerUserUseCase.execute.mockRejectedValue(
          new Error('Database connection failed')
        );

        const response = await testUnauthenticatedCallable(
          registerUserFunction,
          {
            email: generateTestData.email(),
            username: 'testuser',
            password: generateTestData.password(),
          }
        );

        assertCallableError(response, 'internal');
        expect(response.error!.message).toContain('Registration failed');
        expect(response.error!.message).toContain('Database connection failed');
      });

      it('should handle unknown error types', async () => {
        mockAuthUseCases.registerUserUseCase.execute.mockRejectedValue('Unknown error');

        const response = await testUnauthenticatedCallable(
          registerUserFunction,
          {
            email: generateTestData.email(),
            username: 'testuser',
            password: generateTestData.password(),
          }
        );

        assertCallableError(response, 'internal');
        expect(response.error!.message).toContain('Unknown error');
      });
    });

    describe('Data Validation Edge Cases', () => {
      it('should handle special characters in username', async () => {
        const mockRegisterResponse = {
          success: true,
          data: {
            userProfile: { uid: 'user-123', username: 'test_user-123' },
            message: 'User registered successfully',
          },
        };

        mockAuthUseCases.registerUserUseCase.execute.mockResolvedValue(mockRegisterResponse);

        const response = await testUnauthenticatedCallable(
          registerUserFunction,
          {
            email: generateTestData.email(),
            username: 'test_user-123',
            password: generateTestData.password(),
          }
        );

        assertCallableSuccess(response);
        expect(mockAuthUseCases.registerUserUseCase.execute).toHaveBeenCalledWith(
          expect.objectContaining({
            username: 'test_user-123',
          })
        );
      });

      it('should handle long display name', async () => {
        const longDisplayName = 'A'.repeat(255);
        const mockRegisterResponse = {
          success: true,
          data: {
            userProfile: { uid: 'user-123', displayName: longDisplayName },
            message: 'User registered successfully',
          },
        };

        mockAuthUseCases.registerUserUseCase.execute.mockResolvedValue(mockRegisterResponse);

        const response = await testUnauthenticatedCallable(
          registerUserFunction,
          {
            email: generateTestData.email(),
            username: 'testuser',
            password: generateTestData.password(),
            displayName: longDisplayName,
          }
        );

        assertCallableSuccess(response);
        expect(mockAuthUseCases.registerUserUseCase.execute).toHaveBeenCalledWith(
          expect.objectContaining({
            displayName: longDisplayName,
          })
        );
      });

      it('should handle null optional fields gracefully', async () => {
        const mockRegisterResponse = {
          success: true,
          data: {
            userProfile: { uid: 'user-123' },
            message: 'User registered successfully',
          },
        };

        mockAuthUseCases.registerUserUseCase.execute.mockResolvedValue(mockRegisterResponse);

        const response = await testUnauthenticatedCallable(
          registerUserFunction,
          {
            email: generateTestData.email(),
            username: 'testuser',
            password: generateTestData.password(),
            displayName: null,
            deviceInfo: null,
          }
        );

        assertCallableSuccess(response);
        expect(mockAuthUseCases.registerUserUseCase.execute).toHaveBeenCalledWith(
          expect.objectContaining({
            displayName: null,
            deviceInfo: null,
          })
        );
      });
    });
  });
});