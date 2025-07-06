import { UserRepository } from '../../../domain/user/repositories/user.repository';
import { UserEntity, UserName, ImageUrl, UserMemo } from '../../../domain/user/entities/user.entity';
import { CustomResult, Result } from '../../../core/types';
import { NotFoundError, ValidationError } from '../../../core/errors';

export interface UpdateUserProfileRequest {
  userId: string;
  name?: string;
  profileImageUrl?: string;
  memo?: string;
}

export interface UpdateUserProfileResponse {
  userProfile: UserEntity;
}

export class UpdateUserProfileUseCase {
  constructor(
    private readonly userRepository: UserRepository
  ) {}

  async execute(request: UpdateUserProfileRequest): Promise<CustomResult<UpdateUserProfileResponse>> {
    try {
      const existingUserResult = await this.userRepository.findByUserId(request.userId);
      if (!existingUserResult.success) {
        return Result.failure(existingUserResult.error);
      }

      const existingUser = existingUserResult.data;
      if (!existingUser) {
        return Result.failure(new NotFoundError('User', request.userId));
      }

      const updates: {
        name?: UserName;
        profileImageUrl?: ImageUrl;
        memo?: UserMemo;
      } = {};

      if (request.name) {
        const nameValidation = await this.validateUserName(request.name, existingUser.id);
        if (!nameValidation.success) {
          return Result.failure(nameValidation.error);
        }
        updates.name = new UserName(request.name);
      }

      if (request.profileImageUrl) {
        updates.profileImageUrl = new ImageUrl(request.profileImageUrl);
      }

      if (request.memo !== undefined) {
        updates.memo = request.memo ? new UserMemo(request.memo) : undefined;
      }

      const updatedUser = existingUser.updateProfile(updates);
      const saveResult = await this.userRepository.update(updatedUser);

      if (!saveResult.success) {
        return Result.failure(saveResult.error);
      }

      return Result.success({
        userProfile: saveResult.data
      });
    } catch (error) {
      return Result.failure(new Error(`Failed to update user profile: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  private async validateUserName(name: string, currentUserId: string): Promise<CustomResult<void>> {
    try {
      const existingUser = await this.userRepository.findByName(new UserName(name));
      if (!existingUser.success) {
        return Result.failure(existingUser.error);
      }

      if (existingUser.data && existingUser.data.id !== currentUserId) {
        return Result.failure(new ValidationError('name', 'Username already exists'));
      }

      return Result.success(undefined);
    } catch (error) {
      return Result.failure(new Error(`Username validation failed: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }
}