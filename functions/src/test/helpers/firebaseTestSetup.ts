import * as functions from 'firebase-functions-test';
import * as admin from 'firebase-admin';

// Initialize the Firebase Functions test environment
const testEnv = functions({
  projectId: 'test-project-id',
});

// Mock Firebase Admin SDK for testing
export const mockFirebaseAdmin = {
  initializeApp: jest.fn(),
  firestore: jest.fn(() => ({
    settings: jest.fn(),
    collection: jest.fn(),
    doc: jest.fn(),
    runTransaction: jest.fn(),
  })),
  auth: jest.fn(() => ({
    verifyIdToken: jest.fn(),
    createUser: jest.fn(),
    updateUser: jest.fn(),
    deleteUser: jest.fn(),
  })),
  storage: jest.fn(() => ({
    bucket: jest.fn(),
  })),
};

// Setup function to initialize test environment
export const setupFirebaseTest = () => {
  // Mock Firebase Admin SDK
  jest.mock('firebase-admin', () => mockFirebaseAdmin);
  
  return testEnv;
};

// Cleanup function to clean up test environment
export const cleanupFirebaseTest = () => {
  testEnv.cleanup();
  jest.clearAllMocks();
};

// Helper to create mock CallableRequest
export const createMockCallableRequest = (data: any, auth?: any) => {
  return {
    data,
    auth: auth || {
      uid: 'test-user-id',
      token: {},
    },
    rawRequest: {
      method: 'POST',
      headers: {
        'content-type': 'application/json',
      },
      ip: '127.0.0.1',
    },
  };
};

// Helper to create mock CallableContext
export const createMockCallableContext = () => {
  return {
    eventId: 'test-event-id',
    timestamp: new Date().toISOString(),
    eventType: 'providers/cloud.firestore/eventTypes/document.write',
    resource: 'projects/test-project/databases/(default)/documents/test/doc',
  };
};

export { testEnv };