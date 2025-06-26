/**
 * Functions repository implementation
 * Handles internal function calls and external service integrations
 */

import { Result } from '../../shared/types/Result';
import { FunctionError, FunctionErrorCode } from '../../shared/types/common';
import { FunctionsRepository } from '../../domain/repositories';

export class FunctionsRepositoryImpl implements FunctionsRepository {
  private readonly timeout: number;
  private readonly region: string;
  private readonly enableLogging: boolean;

  constructor(options: {
    timeout?: number;
    region?: string;
    enableLogging?: boolean;
  } = {}) {
    this.timeout = options.timeout || 30000; // 30 seconds
    this.region = options.region || 'asia-northeast3';
    this.enableLogging = options.enableLogging ?? true;
  }

  async callFunction(
    functionName: string,
    data?: Record<string, any>
  ): Promise<Result<Record<string, any>, Error>> {
    try {
      if (this.enableLogging) {
        console.log(`Calling function: ${functionName}`, { data });
      }

      // Since we're inside Firebase Functions, we would typically call other functions
      // via HTTP requests or direct imports, depending on the architecture
      
      // For now, this is a placeholder implementation
      // In a real scenario, this might call other Firebase Functions or external APIs
      
      const response = await this.makeInternalFunctionCall(functionName, data);
      
      if (this.enableLogging) {
        console.log(`Function ${functionName} response:`, response);
      }

      return Result.success(response);

    } catch (error) {
      if (this.enableLogging) {
        console.error(`Function ${functionName} error:`, error);
      }
      
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  async getHelloWorld(): Promise<Result<string, Error>> {
    try {
      // This would typically call the helloWorld function internally
      // For now, we'll return a direct response
      const message = 'Hello from Firebase!';
      
      if (this.enableLogging) {
        console.log('HelloWorld function called:', { message });
      }

      return Result.success(message);

    } catch (error) {
      if (this.enableLogging) {
        console.error('HelloWorld function error:', error);
      }
      
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  async callFunctionWithUserData(
    functionName: string,
    userId: string,
    customData?: Record<string, any>
  ): Promise<Result<Record<string, any>, Error>> {
    try {
      const requestData: Record<string, any> = {
        userId,
        ...customData,
      };

      if (this.enableLogging) {
        console.log(`Calling function with user data: ${functionName}`, { userId, customData });
      }

      const response = await this.makeInternalFunctionCall(functionName, requestData);
      
      if (this.enableLogging) {
        console.log(`Function ${functionName} response:`, response);
      }

      return Result.success(response);

    } catch (error) {
      if (this.enableLogging) {
        console.error(`Function ${functionName} with user data error:`, error);
      }
      
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  private async makeInternalFunctionCall(
    functionName: string,
    data?: Record<string, any>
  ): Promise<Record<string, any>> {
    // This is a placeholder for internal function calls
    // In a real implementation, this might:
    // 1. Make HTTP requests to other Firebase Functions
    // 2. Call functions directly if they're in the same codebase
    // 3. Integrate with external APIs
    
    switch (functionName) {
      case 'helloWorld':
        return { message: 'Hello from Firebase!' };
        
      case 'customHelloWorld':
        const message = data?.message || 'Default message';
        return { message: `Processed: ${message}` };
        
      default:
        throw new FunctionError(
          FunctionErrorCode.NOT_FOUND,
          `Function '${functionName}' not found`,
          { functionName, availableFunctions: ['helloWorld', 'customHelloWorld'] }
        );
    }
  }

  // Helper method for making HTTP requests to external Firebase Functions
  private async makeHttpFunctionCall(
    functionUrl: string,
    data?: Record<string, any>,
    headers: Record<string, string> = {}
  ): Promise<Record<string, any>> {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), this.timeout);

    try {
      const response = await fetch(functionUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...headers,
        },
        body: data ? JSON.stringify(data) : undefined,
        signal: controller.signal,
      });

      clearTimeout(timeoutId);

      if (!response.ok) {
        throw new FunctionError(
          FunctionErrorCode.EXTERNAL_SERVICE_ERROR,
          `HTTP ${response.status}: ${response.statusText}`,
          { url: functionUrl, status: response.status }
        );
      }

      const responseData = await response.json();
      return responseData;

    } catch (error: any) {
      clearTimeout(timeoutId);
      
      if (error.name === 'AbortError') {
        throw new FunctionError(
          FunctionErrorCode.TIMEOUT,
          'Function call timed out',
          { url: functionUrl, timeout: this.timeout }
        );
      }
      
      throw error;
    }
  }
}