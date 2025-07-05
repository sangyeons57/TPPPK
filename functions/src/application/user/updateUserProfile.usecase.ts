import { UserProfileRepository } from '../../domain/user/userProfile.repository';
import { UserProfileEntity, Username, UserProfileImage } from '../../domain/user/user.entity';
import { CustomResult, Result } from '../../core/types';
import { NotFoundError, ValidationError } from '../../core/errors';

export interface UpdateUserProfileRequest {
  userId: string;
  username?: string;
  profileImage?: string;
  bio?: string;
  displayName?: string;
}

export interface UpdateUserProfileResponse {
  userProfile: UserProfileEntity;
}

export class UpdateUserProfileUseCase {
  constructor(
    private readonly userProfileRepository: UserProfileRepository
  ) {}

  async execute(request: UpdateUserProfileRequest): Promise<CustomResult<UpdateUserProfileResponse>> {
    try {
      const existingProfileResult = await this.userProfileRepository.findByUserId(request.userId);
      if (!existingProfileResult.success) {
        return Result.failure(existingProfileResult.error);
      }

      const existingProfile = existingProfileResult.data;
      if (!existingProfile) {
        return Result.failure(new NotFoundError('UserProfile', request.userId));
      }

      const updates: {
        username?: Username;
        profileImage?: UserProfileImage;
        bio?: string;
        displayName?: string;
      } = {};

      if (request.username) {
        const usernameValidation = await this.validateUsername(request.username, existingProfile.id);
        if (!usernameValidation.success) {
          return Result.failure(usernameValidation.error);
        }
        updates.username = new Username(request.username);
      }

      if (request.profileImage) {
        updates.profileImage = new UserProfileImage(request.profileImage);
      }

      if (request.bio !== undefined) {
        updates.bio = request.bio;
      }

      if (request.displayName !== undefined) {
        updates.displayName = request.displayName;
      }

      const updatedProfile = existingProfile.updateProfile(updates);
      const saveResult = await this.userProfileRepository.update(updatedProfile);

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

  private async validateUsername(username: string, currentProfileId: string): Promise<CustomResult<void>> {
    try {
      const existingUser = await this.userProfileRepository.findByUsername(new Username(username));
      if (!existingUser.success) {
        return Result.failure(existingUser.error);
      }

      if (existingUser.data && existingUser.data.id !== currentProfileId) {
        return Result.failure(new ValidationError('username', 'Username already exists'));
      }

      return Result.success(undefined);
    } catch (error) {
      return Result.failure(new Error(`Username validation failed: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }
}