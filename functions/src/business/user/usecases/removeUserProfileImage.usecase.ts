import { UserRepository } from '../../../domain/user/repositories/user.repository';
import { CustomResult, Result } from '../../../core/types';
import { NotFoundError, ValidationError } from '../../../core/errors';
import { FirebaseStorageService } from '../../../infrastructure/storage/firebase-storage.service';

export interface RemoveUserProfileImageRequest {
  userId: string;
}

export interface RemoveUserProfileImageResponse {
  success: boolean;
  message: string;
}

export class RemoveUserProfileImageUseCase {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly storageService: FirebaseStorageService
  ) {}

  async execute(request: RemoveUserProfileImageRequest): Promise<CustomResult<RemoveUserProfileImageResponse>> {
    try {
      // 1. 사용자 존재 확인
      const userResult = await this.userRepository.findByUserId(request.userId);
      if (!userResult.success) {
        return Result.failure(userResult.error);
      }

      const user = userResult.data;
      if (!user) {
        return Result.failure(new NotFoundError('User', request.userId));
      }

      // 2. 프로필 이미지 경로 정의
      const profileImagePath = `user_profiles/${request.userId}/profile.webp`;

      try {
        // 3. Firebase Storage에서 프로필 이미지 삭제
        await this.storageService.deleteFile(profileImagePath);
        
        // 4. 성공 응답
        return Result.success({
          success: true,
          message: 'Profile image removed successfully'
        });
      } catch (storageError) {
        // 파일이 존재하지 않는 경우도 성공으로 간주
        if (this.isFileNotFoundError(storageError)) {
          return Result.success({
            success: true,
            message: 'Profile image already removed or does not exist'
          });
        }
        
        // 다른 스토리지 에러는 실패로 처리
        return Result.failure(new Error(`Failed to delete profile image: ${storageError instanceof Error ? storageError.message : 'Unknown storage error'}`));
      }
    } catch (error) {
      return Result.failure(new Error(`Failed to remove user profile image: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  private isFileNotFoundError(error: any): boolean {
    return error?.code === 'storage/object-not-found' || 
           error?.message?.includes('No such object') ||
           error?.message?.includes('Object does not exist');
  }
}