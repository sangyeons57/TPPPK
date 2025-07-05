import { UserProfileRepository } from '../../domain/user/userProfile.repository';
import { UserProfileEntity, Email, Username } from '../../domain/user/user.entity';
import { CustomResult, Result } from '../../core/types';
import { ConflictError, ValidationError } from '../../core/errors';

export interface RegisterUserRequest {
  email: string;
  username: string;
  password: string;
  displayName?: string;
  deviceInfo?: string;
  ipAddress?: string;
}

export interface RegisterUserResponse {
  userProfile: UserProfileEntity;
  message: string;
}

export class RegisterUserUseCase {
  constructor(
    private readonly userProfileRepository: UserProfileRepository
  ) {}

  async execute(request: RegisterUserRequest): Promise<CustomResult<RegisterUserResponse>> {
    try {
      const email = new Email(request.email);
      const username = new Username(request.username);

      const emailValidation = await this.validateEmail(email);
      if (!emailValidation.success) {
        return Result.failure(emailValidation.error);
      }

      const usernameValidation = await this.validateUsername(username);
      if (!usernameValidation.success) {
        return Result.failure(usernameValidation.error);
      }

      const passwordValidation = this.validatePassword(request.password);
      if (!passwordValidation.success) {
        return Result.failure(passwordValidation.error);
      }

      const userId = await this.createFirebaseUser(request.email, request.password);
      if (!userId) {
        return Result.failure(new Error('Failed to create user account'));
      }

      const userProfile = new UserProfileEntity(
        this.generateId(),
        userId,
        username,
        email,
        new Date(),
        new Date(),
        true,
        undefined,
        undefined,
        request.displayName
      );

      const saveResult = await this.userProfileRepository.save(userProfile);
      if (!saveResult.success) {
        await this.deleteFirebaseUser(userId);
        return Result.failure(saveResult.error);
      }

      return Result.success({
        userProfile: saveResult.data,
        message: 'User registered successfully'
      });
    } catch (error) {
      return Result.failure(new Error(`Registration failed: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  private async validateEmail(email: Email): Promise<CustomResult<void>> {
    const existingUser = await this.userProfileRepository.findByEmail(email);
    if (!existingUser.success) {
      return Result.failure(existingUser.error);
    }

    if (existingUser.data) {
      return Result.failure(new ConflictError('User', 'email', email.value));
    }

    return Result.success(undefined);
  }

  private async validateUsername(username: Username): Promise<CustomResult<void>> {
    const existingUser = await this.userProfileRepository.findByUsername(username);
    if (!existingUser.success) {
      return Result.failure(existingUser.error);
    }

    if (existingUser.data) {
      return Result.failure(new ConflictError('User', 'username', username.value));
    }

    return Result.success(undefined);
  }

  private validatePassword(password: string): CustomResult<void> {
    if (password.length < 8) {
      return Result.failure(new ValidationError('password', 'Password must be at least 8 characters long'));
    }

    if (!/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/.test(password)) {
      return Result.failure(new ValidationError('password', 'Password must contain at least one uppercase letter, one lowercase letter, and one number'));
    }

    return Result.success(undefined);
  }

  private async createFirebaseUser(email: string, password: string): Promise<string | null> {
    try {
      const { getAuth } = await import('firebase-admin/auth');
      const auth = getAuth();
      
      const userRecord = await auth.createUser({
        email,
        password,
        emailVerified: false
      });

      return userRecord.uid;
    } catch (error) {
      console.error('Failed to create Firebase user:', error);
      return null;
    }
  }

  private async deleteFirebaseUser(userId: string): Promise<void> {
    try {
      const { getAuth } = await import('firebase-admin/auth');
      const auth = getAuth();
      await auth.deleteUser(userId);
    } catch (error) {
      console.error('Failed to delete Firebase user:', error);
    }
  }

  private generateId(): string {
    return `profile_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }
}