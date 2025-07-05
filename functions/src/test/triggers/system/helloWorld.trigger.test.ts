import { helloWorldFunction } from '../../../triggers/system/helloWorld.trigger';
import { 
  setupFirebaseTest, 
  cleanupFirebaseTest 
} from '../../helpers/firebaseTestSetup';
import { 
  testCallableFunction,
  testUnauthenticatedCallable,
  assertCallableSuccess,
  assertCallableError,
  generateTestData 
} from '../../helpers/triggerTestUtils';

describe('helloWorld Trigger Function', () => {
  let testEnv: any;

  beforeAll(() => {
    testEnv = setupFirebaseTest();
  });

  afterAll(() => {
    cleanupFirebaseTest();
  });

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('Success Cases', () => {
    it('should return hello world message without name', async () => {
      const response = await testUnauthenticatedCallable(
        helloWorldFunction,
        {}
      );

      assertCallableSuccess(response);
      expect(response.data.message).toBe('Hello, World!');
      expect(response.data.timestamp).toBeDefined();
      expect(new Date(response.data.timestamp)).toBeInstanceOf(Date);
    });

    it('should return personalized hello message with name', async () => {
      const testName = 'Firebase Functions';
      const response = await testUnauthenticatedCallable(
        helloWorldFunction,
        { name: testName }
      );

      assertCallableSuccess(response);
      expect(response.data.message).toBe(`Hello, ${testName}!`);
      expect(response.data.timestamp).toBeDefined();
    });

    it('should handle empty name', async () => {
      const response = await testUnauthenticatedCallable(
        helloWorldFunction,
        { name: '' }
      );

      assertCallableSuccess(response);
      expect(response.data.message).toBe('Hello, World!');
    });

    it('should handle null name', async () => {
      const response = await testUnauthenticatedCallable(
        helloWorldFunction,
        { name: null }
      );

      assertCallableSuccess(response);
      expect(response.data.message).toBe('Hello, World!');
    });

    it('should handle undefined name', async () => {
      const response = await testUnauthenticatedCallable(
        helloWorldFunction,
        { name: undefined }
      );

      assertCallableSuccess(response);
      expect(response.data.message).toBe('Hello, World!');
    });
  });

  describe('Response Format Validation', () => {
    it('should return consistent response structure', async () => {
      const response = await testUnauthenticatedCallable(
        helloWorldFunction,
        { name: 'Test' }
      );

      assertCallableSuccess(response);
      expect(response.data).toHaveProperty('message');
      expect(response.data).toHaveProperty('timestamp');
      expect(typeof response.data.message).toBe('string');
      expect(typeof response.data.timestamp).toBe('string');
    });

    it('should return valid ISO timestamp', async () => {
      const response = await testUnauthenticatedCallable(
        helloWorldFunction,
        {}
      );

      assertCallableSuccess(response);
      const timestamp = new Date(response.data.timestamp);
      expect(timestamp.toISOString()).toBe(response.data.timestamp);
      expect(timestamp.getTime()).toBeLessThanOrEqual(Date.now());
    });
  });

  describe('Edge Cases', () => {
    it('should handle special characters in name', async () => {
      const specialName = '🔥 Firebase 特殊文字 Functions! 123';
      const response = await testUnauthenticatedCallable(
        helloWorldFunction,
        { name: specialName }
      );

      assertCallableSuccess(response);
      expect(response.data.message).toBe(`Hello, ${specialName}!`);
    });

    it('should handle very long name', async () => {
      const longName = 'A'.repeat(1000);
      const response = await testUnauthenticatedCallable(
        helloWorldFunction,
        { name: longName }
      );

      assertCallableSuccess(response);
      expect(response.data.message).toBe(`Hello, ${longName}!`);
    });

    it('should handle numeric name', async () => {
      const response = await testUnauthenticatedCallable(
        helloWorldFunction,
        { name: 123 }
      );

      assertCallableSuccess(response);
      expect(response.data.message).toBe('Hello, 123!');
    });

    it('should handle object name', async () => {
      const objectName = { test: 'value' };
      const response = await testUnauthenticatedCallable(
        helloWorldFunction,
        { name: objectName }
      );

      assertCallableSuccess(response);
      expect(response.data.message).toBe(`Hello, ${objectName}!`);
    });
  });

  describe('Error Handling', () => {
    it('should handle function execution errors gracefully', async () => {
      // Mock an error scenario by providing invalid data structure
      // that might cause JSON parsing issues
      const circularObj: any = {};
      circularObj.self = circularObj;

      const response = await testUnauthenticatedCallable(
        helloWorldFunction,
        { name: circularObj }
      );

      // The function should still succeed because it handles the data as-is
      assertCallableSuccess(response);
      expect(response.data.message).toContain('Hello,');
    });
  });

  describe('Performance', () => {
    it('should respond quickly', async () => {
      const startTime = Date.now();
      
      const response = await testUnauthenticatedCallable(
        helloWorldFunction,
        { name: 'Performance Test' }
      );

      const endTime = Date.now();
      const executionTime = endTime - startTime;

      assertCallableSuccess(response);
      expect(executionTime).toBeLessThan(1000); // Should complete within 1 second
    });

    it('should handle multiple concurrent requests', async () => {
      const promises = Array.from({ length: 10 }, (_, i) =>
        testUnauthenticatedCallable(
          helloWorldFunction,
          { name: `User${i}` }
        )
      );

      const responses = await Promise.all(promises);

      responses.forEach((response, index) => {
        assertCallableSuccess(response);
        expect(response.data.message).toBe(`Hello, User${index}!`);
      });
    });
  });
});