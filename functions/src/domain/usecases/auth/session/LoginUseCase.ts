/**
 * Login use case
 * Handles user authentication with email and password
 */

import { Result } from '../../../../shared/types/Result';
import { FunctionError, FunctionErrorCode, UseCaseContext } from '../../../../shared/types/common';
import { AuthRepository, UserRepository } from '../../../repositories';
import { UserSession } from '../../../models/data/UserSession';
import { UserEmail, UserId } from '../../../models/vo';
import { UserAccountStatus } from '../../../models/enums/UserAccountStatus';
import { User } from '../../../models/base/User';

export class WithdrawnAccountError extends Error {
  constructor(message: string = 'Account has been withdrawn') {
    super(message);
    this.name = 'WithdrawnAccountError';
  }
}

export class LoginUseCase {
  constructor(
    private readonly authRepository: AuthRepository,
    private readonly userRepository: UserRepository
  ) {}

  async execute(
    email: UserEmail,
    password: string,
    context?: UseCaseContext
  ): Promise<Result<UserSession, Error>> {
    try {
      // Step 1: Attempt authentication
      const authResult = await this.authRepository.login(email, password);
      
      if (!authResult.isSuccess) {
        return authResult;
      }

      const userSession = authResult.getOrThrow();

      // Step 2: Verify user account status
      const userResult = await this.userRepository.findById(userSession.userId);
      
      if (!userResult.isSuccess) {
        // If user not found, logout and return error
        await this.authRepository.logout();
        return Result.failure(userResult.getOrNull() || new Error('User not found'));
      }

      const user = userResult.getOrThrow();

      // Step 3: Check account status
      if (user.accountStatus === UserAccountStatus.WITHDRAWN) {
        await this.authRepository.logout();
        return Result.failure(new WithdrawnAccountError('탈퇴한 계정입니다'));
      }

      if (user.accountStatus === UserAccountStatus.SUSPENDED) {
        await this.authRepository.logout();
        return Result.failure(new FunctionError(
          FunctionErrorCode.FORBIDDEN,
          '정지된 계정입니다',
          { userId: user.userId.value, status: user.accountStatus }
        ));
      }

      if (user.accountStatus === UserAccountStatus.LOCKED) {
        await this.authRepository.logout();
        return Result.failure(new FunctionError(
          FunctionErrorCode.FORBIDDEN,
          '잠긴 계정입니다. 관리자에게 문의하세요',
          { userId: user.userId.value, status: user.accountStatus }
        ));
      }

      if (!user.canLogin()) {
        await this.authRepository.logout();
        return Result.failure(new FunctionError(
          FunctionErrorCode.FORBIDDEN,
          '로그인할 수 없는 계정 상태입니다',
          { userId: user.userId.value, status: user.accountStatus, emailVerified: user.emailVerified }
        ));
      }

      // Step 4: Update last login time
      await this.userRepository.updateLastLogin(user.userId, new Date());

      // Step 5: Return successful session
      return Result.success(userSession);

    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }
}