/**
 * Functions repository interface
 * Defines contracts for Firebase Functions operations
 */

import { Result } from '../../shared/types/Result';
import { Repository } from './Repository';

export interface FunctionsRepository extends Repository {
  /**
   * Call a generic Firebase function
   * @param functionName Name of the function to call
   * @param data Optional data to pass to the function
   * @returns Result containing function response
   */
  callFunction(
    functionName: string,
    data?: Record<string, any>
  ): Promise<Result<Record<string, any>, Error>>;

  /**
   * Get Hello World message from Firebase function
   * @returns Result containing Hello World message
   */
  getHelloWorld(): Promise<Result<string, Error>>;

  /**
   * Call a function with user context
   * @param functionName Name of the function to call
   * @param userId User ID for context
   * @param customData Optional custom data
   * @returns Result containing function response
   */
  callFunctionWithUserData(
    functionName: string,
    userId: string,
    customData?: Record<string, any>
  ): Promise<Result<Record<string, any>, Error>>;
}