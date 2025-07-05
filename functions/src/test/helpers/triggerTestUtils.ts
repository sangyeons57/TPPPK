/**
 * Utilities for testing Firebase Function triggers
 */

import { HttpsError } from 'firebase-functions/v2/https';
import { createMockCallableRequest, createMockCallableContext } from './firebaseTestSetup';
import { createMockAuthContext, createUnauthenticatedContext } from './mockAuth';

/**
 * Test helper for callable function responses
 */
export interface TestCallableResponse<T = any> {
  success: boolean;
  data?: T;
  error?: {
    code: string;
    message: string;
  };
}

/**
 * Wrapper to test callable functions and handle errors consistently
 */
export const testCallableFunction = async <TRequest, TResponse>(
  functionToTest: any,
  requestData: TRequest,
  authContext?: any
): Promise<TestCallableResponse<TResponse>> => {
  try {
    const mockRequest = createMockCallableRequest(requestData, authContext);
    const result = await functionToTest(mockRequest);
    
    return {
      success: true,
      data: result,
    };
  } catch (error) {
    if (error instanceof HttpsError) {
      return {
        success: false,
        error: {
          code: error.code,
          message: error.message,
        },
      };
    }
    
    return {
      success: false,
      error: {
        code: 'internal',
        message: error instanceof Error ? error.message : 'Unknown error',
      },
    };
  }
};

/**
 * Test helper for authenticated callable functions
 */
export const testAuthenticatedCallable = async <TRequest, TResponse>(
  functionToTest: any,
  requestData: TRequest,
  userOverrides: any = {}
): Promise<TestCallableResponse<TResponse>> => {
  const authContext = createMockAuthContext(userOverrides);
  return testCallableFunction(functionToTest, requestData, authContext);
};

/**
 * Test helper for unauthenticated callable functions
 */
export const testUnauthenticatedCallable = async <TRequest, TResponse>(
  functionToTest: any,
  requestData: TRequest
): Promise<TestCallableResponse<TResponse>> => {
  const authContext = createUnauthenticatedContext();
  return testCallableFunction(functionToTest, requestData, authContext);
};

/**
 * Common test data generators
 */
export const generateTestData = {
  email: (suffix = ''): string => `test${suffix}@example.com`,
  password: (): string => 'TestPassword123!',
  userId: (suffix = ''): string => `user-${suffix || Date.now()}`,
  sessionToken: (): string => `session-${Date.now()}-${Math.random()}`,
  deviceInfo: (): string => 'Test Device - Jest',
  
  loginRequest: (overrides: any = {}) => ({
    email: generateTestData.email(),
    password: generateTestData.password(),
    deviceInfo: generateTestData.deviceInfo(),
    ...overrides,
  }),
  
  registerRequest: (overrides: any = {}) => ({
    email: generateTestData.email(),
    password: generateTestData.password(),
    displayName: 'Test User',
    ...overrides,
  }),
  
  userProfile: (overrides: any = {}) => ({
    uid: generateTestData.userId(),
    email: generateTestData.email(),
    displayName: 'Test User',
    profileImageUrl: 'https://example.com/profile.jpg',
    ...overrides,
  }),
};

/**
 * Common assertions for Firebase Function responses
 */
export const assertCallableSuccess: <T>(response: TestCallableResponse<T>) => asserts response is TestCallableResponse<T> & { success: true; data: T } = <T>(response: TestCallableResponse<T>): asserts response is TestCallableResponse<T> & { success: true; data: T } => {
  expect(response.success).toBe(true);
  expect(response.data).toBeDefined();
  expect(response.error).toBeUndefined();
};

export const assertCallableError: (response: TestCallableResponse, expectedCode?: string) => asserts response is TestCallableResponse & { success: false; error: { code: string; message: string } } = (response: TestCallableResponse, expectedCode?: string): asserts response is TestCallableResponse & { success: false; error: { code: string; message: string } } => {
  expect(response.success).toBe(false);
  expect(response.error).toBeDefined();
  expect(response.data).toBeUndefined();
  
  if (expectedCode) {
    expect(response.error!.code).toBe(expectedCode);
  }
};

/**
 * Helper to test required field validation
 */
export const testRequiredFields = async <TRequest, TResponse>(
  functionToTest: any,
  validRequest: TRequest,
  requiredFields: (keyof TRequest)[]
): Promise<void> => {
  for (const field of requiredFields) {
    const invalidRequest = { ...validRequest };
    delete invalidRequest[field];
    
    const response = await testUnauthenticatedCallable(functionToTest, invalidRequest);
    assertCallableError(response, 'invalid-argument');
    expect(response.error!.message).toContain(field as string);
  }
};