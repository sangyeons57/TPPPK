import * as functions from 'firebase-functions-test';
import { helloWorld } from '../index';

const testEnv = functions.default();

describe('helloWorld', () => {
  afterAll(() => {
    testEnv.cleanup();
  });

  it('should return Hello from Firebase message', () => {
    // Create a mock request
    const mockRequest = {
      data: null,
      rawRequest: {
        method: 'POST',
        headers: {},
      },
    };

    // Call the function
    const result = helloWorld(mockRequest, {} as any);

    // Verify the result
    expect(result).toEqual({
      message: 'Hello from Firebase!'
    });
  });

  it('should return consistent response format', () => {
    // Create a mock request with some data
    const mockRequest = {
      data: { test: 'data' },
      rawRequest: {
        method: 'POST',
        headers: {},
      },
    };

    // Call the function
    const result = helloWorld(mockRequest, {} as any);

    // Verify the response has the expected structure
    expect(result).toHaveProperty('message');
    expect(typeof result.message).toBe('string');
    expect(result.message).toBe('Hello from Firebase!');
  });

  it('should handle empty request', () => {
    // Create an empty mock request
    const mockRequest = {
      data: {},
      rawRequest: {
        method: 'POST',
        headers: {},
      },
    };

    // Call the function
    const result = helloWorld(mockRequest, {} as any);

    // Verify the result
    expect(result).toEqual({
      message: 'Hello from Firebase!'
    });
  });
});