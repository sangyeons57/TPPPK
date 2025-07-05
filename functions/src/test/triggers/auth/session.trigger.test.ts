import { loginUserFunction, logoutUserFunction } from '../../../triggers/auth/session.trigger';
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

describe('Auth Session Trigger Functions', () => {
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
      loginUserUseCase: {
        execute: jest.fn(),
      },
      logoutUserUseCase: {
        execute: jest.fn(),
      },
    };

    // Mock the provider
    const mockProvider = {
      create: jest.fn().mockReturnValue(mockAuthUseCases),
    };

    (Providers.getAuthSessionProvider as jest.Mock).mockReturnValue(mockProvider);
  });

  describe('loginUserFunction', () => {
    describe('Success Cases', () => {
      it('should successfully login user with valid credentials', async () => {
        const mockLoginResponse = {
          success: true,
          data: {
            sessionToken: 'mock-session-token',
            refreshToken: 'mock-refresh-token',
            expiresAt: new Date('2024-12-31T23:59:59Z'),
            userProfile: {
              uid: 'user-123',
              email: 'test@example.com',
              displayName: 'Test User',
            },
          },
        };

        mockAuthUseCases.loginUserUseCase.execute.mockResolvedValue(mockLoginResponse);

        const loginRequest = generateTestData.loginRequest();
        const response = await testUnauthenticatedCallable(
          loginUserFunction,
          loginRequest
        );

        assertCallableSuccess(response);
        expect(response.data.sessionToken).toBe('mock-session-token');
        expect(response.data.refreshToken).toBe('mock-refresh-token');
        expect(response.data.expiresAt).toBe('2024-12-31T23:59:59.000Z');
        expect(response.data.userProfile).toEqual(mockLoginResponse.data.userProfile);

        // Verify use case was called with correct parameters
        expect(mockAuthUseCases.loginUserUseCase.execute).toHaveBeenCalledWith({
          email: loginRequest.email,
          password: loginRequest.password,
          deviceInfo: loginRequest.deviceInfo,
          ipAddress: '127.0.0.1',
        });
      });

      it('should handle login without device info', async () => {
        const mockLoginResponse = {
          success: true,
          data: {
            sessionToken: 'mock-session-token',
            refreshToken: 'mock-refresh-token',
            expiresAt: new Date('2024-12-31T23:59:59Z'),
            userProfile: { uid: 'user-123' },
          },
        };

        mockAuthUseCases.loginUserUseCase.execute.mockResolvedValue(mockLoginResponse);

        const loginRequest = {
          email: generateTestData.email(),
          password: generateTestData.password(),
          // deviceInfo omitted
        };

        const response = await testUnauthenticatedCallable(
          loginUserFunction,
          loginRequest
        );

        assertCallableSuccess(response);
        expect(mockAuthUseCases.loginUserUseCase.execute).toHaveBeenCalledWith({
          email: loginRequest.email,
          password: loginRequest.password,
          deviceInfo: undefined,
          ipAddress: '127.0.0.1',
        });
      });
    });

    describe('Validation Errors', () => {
      it('should require email and password', async () => {
        const validRequest = generateTestData.loginRequest();
        await testRequiredFields(loginUserFunction, validRequest, ['email', 'password']);
      });

      it('should handle empty email', async () => {
        const response = await testUnauthenticatedCallable(
          loginUserFunction,
          { email: '', password: 'password123' }
        );

        assertCallableError(response, 'invalid-argument');
        expect(response.error!.message).toContain('Email and password are required');
      });

      it('should handle empty password', async () => {
        const response = await testUnauthenticatedCallable(
          loginUserFunction,
          { email: 'test@example.com', password: '' }
        );

        assertCallableError(response, 'invalid-argument');
        expect(response.error!.message).toContain('Email and password are required');
      });
    });

    describe('Authentication Errors', () => {
      it('should handle unauthorized error', async () => {
        const unauthorizedError = {
          success: false,
          error: {
            name: 'Unauthorized',
            message: 'Invalid credentials',
          },
        };

        mockAuthUseCases.loginUserUseCase.execute.mockResolvedValue(unauthorizedError);

        const response = await testUnauthenticatedCallable(
          loginUserFunction,
          generateTestData.loginRequest()
        );

        assertCallableError(response, 'unauthenticated');
        expect(response.error!.message).toBe('Invalid credentials');
      });

      it('should handle other business logic errors', async () => {
        const businessError = {
          success: false,
          error: {
            name: 'ValidationError',
            message: 'User account is suspended',
          },
        };

        mockAuthUseCases.loginUserUseCase.execute.mockResolvedValue(businessError);

        const response = await testUnauthenticatedCallable(
          loginUserFunction,
          generateTestData.loginRequest()
        );

        assertCallableError(response, 'internal');
        expect(response.error!.message).toBe('User account is suspended');
      });
    });

    describe('System Errors', () => {
      it('should handle unexpected errors', async () => {
        mockAuthUseCases.loginUserUseCase.execute.mockRejectedValue(
          new Error('Database connection failed')
        );

        const response = await testUnauthenticatedCallable(
          loginUserFunction,
          generateTestData.loginRequest()
        );

        assertCallableError(response, 'internal');
        expect(response.error!.message).toContain('Login failed');
        expect(response.error!.message).toContain('Database connection failed');
      });

      it('should handle unknown error types', async () => {
        mockAuthUseCases.loginUserUseCase.execute.mockRejectedValue('Unknown error');

        const response = await testUnauthenticatedCallable(
          loginUserFunction,
          generateTestData.loginRequest()
        );

        assertCallableError(response, 'internal');
        expect(response.error!.message).toContain('Unknown error');
      });
    });
  });

  describe('logoutUserFunction', () => {
    describe('Success Cases', () => {
      it('should successfully logout user with valid session token', async () => {
        const response = await testUnauthenticatedCallable(
          logoutUserFunction,
          { sessionToken: generateTestData.sessionToken() }
        );

        assertCallableSuccess(response);
        expect(response.data.success).toBe(true);
      });
    });

    describe('Validation Errors', () => {
      it('should require session token', async () => {
        const response = await testUnauthenticatedCallable(
          logoutUserFunction,
          {}
        );

        assertCallableError(response, 'invalid-argument');
        expect(response.error!.message).toContain('Session token is required');
      });

      it('should handle empty session token', async () => {
        const response = await testUnauthenticatedCallable(
          logoutUserFunction,
          { sessionToken: '' }
        );

        assertCallableError(response, 'invalid-argument');
        expect(response.error!.message).toContain('Session token is required');
      });

      it('should handle null session token', async () => {
        const response = await testUnauthenticatedCallable(
          logoutUserFunction,
          { sessionToken: null }
        );

        assertCallableError(response, 'invalid-argument');
        expect(response.error!.message).toContain('Session token is required');
      });
    });

    describe('System Errors', () => {
      // Note: Current implementation doesn't actually call logout use case
      // This test covers the current behavior and should be updated when 
      // the TODO is implemented
      it('should handle system errors gracefully', async () => {
        const response = await testUnauthenticatedCallable(
          logoutUserFunction,
          { sessionToken: generateTestData.sessionToken() }
        );

        // Current implementation always returns success
        assertCallableSuccess(response);
        expect(response.data.success).toBe(true);
      });
    });
  });

  describe('Integration Scenarios', () => {
    it('should maintain session state between login and logout', async () => {
      // Mock successful login
      const mockLoginResponse = {
        success: true,
        data: {
          sessionToken: 'integration-session-token',
          refreshToken: 'integration-refresh-token',
          expiresAt: new Date('2024-12-31T23:59:59Z'),
          userProfile: { uid: 'integration-user-123' },
        },
      };

      mockAuthUseCases.loginUserUseCase.execute.mockResolvedValue(mockLoginResponse);

      // Perform login
      const loginResponse = await testUnauthenticatedCallable(
        loginUserFunction,
        generateTestData.loginRequest()
      );

      assertCallableSuccess(loginResponse);
      const sessionToken = loginResponse.data.sessionToken;

      // Perform logout with the session token
      const logoutResponse = await testUnauthenticatedCallable(
        logoutUserFunction,
        { sessionToken }
      );

      assertCallableSuccess(logoutResponse);
      expect(logoutResponse.data.success).toBe(true);
    });
  });
});