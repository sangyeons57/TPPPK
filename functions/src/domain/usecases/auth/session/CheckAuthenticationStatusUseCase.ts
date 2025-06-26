/**
 * Check authentication status use case
 * Determines if user is currently authenticated
 */

import { Result } from '../../../../shared/types/Result';
import { UseCaseContext } from '../../../../shared/types/common';
import { AuthRepository } from '../../../repositories';

export class CheckAuthenticationStatusUseCase {
  constructor(
    private readonly authRepository: AuthRepository
  ) {}

  async execute(context?: UseCaseContext): Promise<Result<boolean, Error>> {
    try {
      const isLoggedIn = await this.authRepository.isLoggedIn();
      return Result.success(isLoggedIn);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }
}