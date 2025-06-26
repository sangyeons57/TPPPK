/**
 * Hello World use case
 * Demonstrates Firebase Functions integration
 */

import { Result } from '../../../shared/types/Result';
import { UseCaseContext } from '../../../shared/types/common';
import { FunctionsRepository } from '../../repositories';

export class HelloWorldUseCase {
  constructor(
    private readonly functionsRepository: FunctionsRepository
  ) {}

  async execute(context?: UseCaseContext): Promise<Result<string, Error>> {
    try {
      return await this.functionsRepository.getHelloWorld();
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  async executeWithCustomMessage(
    customMessage: string,
    context?: UseCaseContext
  ): Promise<Result<string, Error>> {
    try {
      const data = { message: customMessage };
      const result = await this.functionsRepository.callFunction('customHelloWorld', data);
      
      if (!result.isSuccess) {
        return Result.failure(result.getOrNull() || new Error('Function call failed'));
      }

      const responseData = result.getOrThrow();
      const message = responseData.message || responseData.result || 'No message received';
      
      return Result.success(String(message));

    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }
}