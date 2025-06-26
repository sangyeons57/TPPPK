/**
 * Integration test for Hello World function with new architecture
 */

import { describe, it, expect, beforeAll } from '@jest/globals';
import { container } from '../../infrastructure/di/Container';

describe('HelloWorld Integration Test', () => {
  let functionsUseCases: any;

  beforeAll(() => {
    functionsUseCases = container.getFunctionsUseCases();
  });

  it('should return Hello World message', async () => {
    const result = await functionsUseCases.helloWorldUseCase.execute();
    
    expect(result.isSuccess).toBe(true);
    expect(result.getOrThrow()).toBe('Hello from Firebase!');
  });

  it('should process custom message', async () => {
    const customMessage = 'Test Message';
    const result = await functionsUseCases.helloWorldUseCase.executeWithCustomMessage(customMessage);
    
    expect(result.isSuccess).toBe(true);
    expect(result.getOrThrow()).toBe(`Processed: ${customMessage}`);
  });

  it('should handle errors gracefully', async () => {
    // Test error handling by calling a non-existent function
    const functionsRepository = functionsUseCases.functionsRepository;
    const result = await functionsRepository.callFunction('nonExistentFunction');
    
    expect(result.isFailure).toBe(true);
  });
});

describe('Container Dependency Injection', () => {
  it('should provide auth session use cases', () => {
    const authSessionUseCases = container.getAuthSessionUseCases();
    
    expect(authSessionUseCases).toBeDefined();
    expect(authSessionUseCases.loginUseCase).toBeDefined();
    expect(authSessionUseCases.logoutUseCase).toBeDefined();
    expect(authSessionUseCases.checkSessionUseCase).toBeDefined();
    expect(authSessionUseCases.checkAuthenticationStatusUseCase).toBeDefined();
  });

  it('should provide auth registration use cases', () => {
    const authRegistrationUseCases = container.getAuthRegistrationUseCases();
    
    expect(authRegistrationUseCases).toBeDefined();
    expect(authRegistrationUseCases.signUpUseCase).toBeDefined();
  });

  it('should provide functions use cases', () => {
    const functionsUseCases = container.getFunctionsUseCases();
    
    expect(functionsUseCases).toBeDefined();
    expect(functionsUseCases.helloWorldUseCase).toBeDefined();
  });
});