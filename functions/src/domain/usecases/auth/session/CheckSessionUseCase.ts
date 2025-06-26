/**
 * Check session use case
 * Validates current user session
 */

import { Result } from '../../../../shared/types/Result';
import { UseCaseContext } from '../../../../shared/types/common';
import { AuthRepository } from '../../../repositories';
import { UserSession } from '../../../models/data/UserSession';

export class CheckSessionUseCase {
  constructor(
    private readonly authRepository: AuthRepository
  ) {}

  async execute(context?: UseCaseContext): Promise<Result<UserSession, Error>> {
    try {
      const sessionResult = await this.authRepository.getCurrentUserSession();
      
      if (!sessionResult.isSuccess) {
        return sessionResult;
      }

      const session = sessionResult.getOrThrow();

      // Check if session is expired
      if (session.isExpired()) {
        await this.authRepository.logout();
        return Result.failure(new Error('Session has expired'));
      }

      // Check if token is expired
      if (session.isTokenExpired()) {
        await this.authRepository.logout();
        return Result.failure(new Error('Token has expired'));
      }

      return Result.success(session);

    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }
}