/**
 * Sign up use case
 * Handles new user registration
 */

import { Result } from '../../../../shared/types/Result';
import { FunctionError, FunctionErrorCode, UseCaseContext } from '../../../../shared/types/common';
import { AuthRepository, UserRepository } from '../../../repositories';
import { User } from '../../../models/base/User';
import { UserEmail, UserName } from '../../../models/vo';
import { Validator } from '../../../../shared/validation/validator';

export interface SignUpRequest {
  readonly email: string;
  readonly password: string;
  readonly name: string;
  readonly profileImageUrl?: string;
  readonly memo?: string;
}

export class SignUpUseCase {
  constructor(
    private readonly authRepository: AuthRepository,
    private readonly userRepository: UserRepository
  ) {}

  async execute(
    request: SignUpRequest,
    context?: UseCaseContext
  ): Promise<Result<string, Error>> {
    try {
      // Step 1: Validate input
      const validationResult = this.validateInput(request);
      if (!validationResult.isValid) {
        return Result.failure(new FunctionError(
          FunctionErrorCode.VALIDATION_ERROR,
          'Validation failed',
          { errors: validationResult.errors }
        ));
      }

      const email = UserEmail.from(request.email);
      const name = UserName.from(request.name);

      // Step 2: Check if user already exists
      const existsResult = await this.userRepository.existsByEmail(email);
      if (!existsResult.isSuccess) {
        return Result.failure(existsResult.getOrNull() || new Error('Failed to check user existence'));
      }

      if (existsResult.getOrThrow()) {
        return Result.failure(new FunctionError(
          FunctionErrorCode.CONFLICT,
          'User with this email already exists',
          { email: request.email }
        ));
      }

      // Step 3: Create authentication account
      const authResult = await this.authRepository.signup(request.email, request.password);
      if (!authResult.isSuccess) {
        return authResult;
      }

      const userId = authResult.getOrThrow();

      try {
        // Step 4: Create user profile
        const user = User.registerNewUser(
          userId,
          request.email,
          request.name,
          new Date(),
          {
            profileImageUrl: request.profileImageUrl,
            memo: request.memo,
            emailVerified: false,
          }
        );

        const saveResult = await this.userRepository.save(user);
        if (!saveResult.isSuccess) {
          // Rollback: delete the auth account if user profile creation fails
          await this.authRepository.withdrawCurrentUser();
          return Result.failure(saveResult.getOrNull() || new Error('Failed to create user profile'));
        }

        // Step 5: Send email verification
        const verificationResult = await this.authRepository.sendEmailVerification();
        if (!verificationResult.isSuccess) {
          // Log the error but don't fail the registration
          console.warn('Failed to send email verification:', verificationResult.getOrNull());
        }

        return Result.success(userId);

      } catch (error) {
        // Rollback: delete the auth account if anything fails
        await this.authRepository.withdrawCurrentUser();
        throw error;
      }

    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  private validateInput(request: SignUpRequest) {
    return Validator.create()
      .email(request.email)
      .password(request.password)
      .username(request.name, 'name')
      .custom(
        !request.memo || request.memo.length <= 200,
        'memo',
        'TOO_LONG',
        'Memo cannot exceed 200 characters'
      )
      .getResult();
  }
}