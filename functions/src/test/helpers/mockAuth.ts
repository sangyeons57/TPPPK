/**
 * Mock authentication utilities for testing Firebase Functions
 */

export interface MockAuthUser {
  uid: string;
  email?: string;
  displayName?: string;
  emailVerified?: boolean;
}

export interface MockAuthContext {
  uid: string;
  token: {
    email?: string;
    email_verified?: boolean;
    firebase?: {
      identities?: Record<string, any>;
      sign_in_provider?: string;
    };
  };
}

/**
 * Creates a mock authenticated user context for testing
 */
export const createMockAuthUser = (overrides: Partial<MockAuthUser> = {}): MockAuthUser => {
  return {
    uid: 'test-user-123',
    email: 'test@example.com',
    displayName: 'Test User',
    emailVerified: true,
    ...overrides,
  };
};

/**
 * Creates a mock authentication context for callable functions
 */
export const createMockAuthContext = (user: Partial<MockAuthUser> = {}): MockAuthContext => {
  const mockUser = createMockAuthUser(user);
  
  return {
    uid: mockUser.uid,
    token: {
      email: mockUser.email,
      email_verified: mockUser.emailVerified,
      firebase: {
        identities: {
          email: [mockUser.email],
        },
        sign_in_provider: 'password',
      },
    },
  };
};

/**
 * Creates an unauthenticated context (no auth)
 */
export const createUnauthenticatedContext = () => {
  return undefined;
};

/**
 * Mock Firebase Auth service for testing
 */
export const mockFirebaseAuth = {
  verifyIdToken: jest.fn().mockResolvedValue({
    uid: 'test-user-123',
    email: 'test@example.com',
    email_verified: true,
  }),
  
  createUser: jest.fn().mockResolvedValue({
    uid: 'new-user-123',
    email: 'newuser@example.com',
  }),
  
  updateUser: jest.fn().mockResolvedValue({
    uid: 'test-user-123',
    email: 'updated@example.com',
  }),
  
  deleteUser: jest.fn().mockResolvedValue(undefined),
  
  getUserByEmail: jest.fn().mockResolvedValue({
    uid: 'test-user-123',
    email: 'test@example.com',
  }),
  
  setCustomUserClaims: jest.fn().mockResolvedValue(undefined),
};

/**
 * Reset all auth mocks to their default state
 */
export const resetAuthMocks = () => {
  Object.values(mockFirebaseAuth).forEach(mock => {
    if (jest.isMockFunction(mock)) {
      mock.mockClear();
    }
  });
};