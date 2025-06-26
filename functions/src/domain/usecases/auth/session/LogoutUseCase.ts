/**
 * Logout use case
 * Handles user session termination
 */

import { Result } from '../../../../shared/types/Result';
import { UseCaseContext } from '../../../../shared/types/common';
import { AuthRepository } from '../../../repositories';

export class LogoutUseCase {
  constructor(
    private readonly authRepository: AuthRepository
  ) {}

  async execute(context?: UseCaseContext): Promise<Result<void, Error>> {
    try {
      return await this.authRepository.logout();
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }
}