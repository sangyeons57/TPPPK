import {CustomResult, Result} from "../../../core/types";
import {ValidationError, NotFoundError} from "../../../core/errors";
import {UserRepository} from "../../../domain/user/repositories/user.repository";

export interface UpdateUserImageRequest {
  userId: string;
  imageUrl: string;
}

export interface UpdateUserImageResponse {
  userId: string;
  profileImageUrl?: string;
  updatedAt: string;
}

/**
 * Simple use case to update user profile image URL
 * Triggered by Firebase Storage uploads
 */
export class UpdateUserImageUseCase {
  constructor(
    private readonly userRepository: UserRepository
  ) {}

  async execute(request: UpdateUserImageRequest): Promise<CustomResult<UpdateUserImageResponse>> {
    try {
      // Input validation
      if (!request.userId) {
        return Result.failure(new ValidationError("userId", "User ID is required"));
      }

      if (!request.imageUrl) {
        return Result.failure(new ValidationError("imageUrl", "Image URL is required"));
      }

      // Find user
      const userResult = await this.userRepository.findByUserId(request.userId);
      if (!userResult.success) {
        return Result.failure(userResult.error);
      }

      if (!userResult.data) {
        return Result.failure(new NotFoundError("User not found", request.userId));
      }

      const user = userResult.data;

      // Update user profile image
      const updatedUser = user.updateProfile({
        profileImageUrl: request.imageUrl
      });

      // Save updated user
      const saveResult = await this.userRepository.update(updatedUser);
      if (!saveResult.success) {
        return Result.failure(saveResult.error);
      }

      return Result.success({
        userId: updatedUser.id,
        profileImageUrl: updatedUser.profileImageUrl,
        updatedAt: updatedUser.updatedAt.toISOString()
      });

    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error("Failed to update user image"));
    }
  }
}