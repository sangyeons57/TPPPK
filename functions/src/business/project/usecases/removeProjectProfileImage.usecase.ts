import { ProjectRepository } from '../../../domain/project/repositories/project.repository';
import { CustomResult, Result } from '../../../core/types';
import { NotFoundError, ValidationError } from '../../../core/errors';
import { FirebaseStorageService } from '../../../infrastructure/storage/firebase-storage.service';

export interface RemoveProjectProfileImageRequest {
  projectId: string;
  userId: string; // 요청하는 사용자 ID (권한 검증용)
}

export interface RemoveProjectProfileImageResponse {
  success: boolean;
  message: string;
}

export class RemoveProjectProfileImageUseCase {
  constructor(
    private readonly projectRepository: ProjectRepository,
    private readonly storageService: FirebaseStorageService
  ) {}

  async execute(request: RemoveProjectProfileImageRequest): Promise<CustomResult<RemoveProjectProfileImageResponse>> {
    try {
      // 1. 프로젝트 존재 확인
      const projectResult = await this.projectRepository.findById(request.projectId);
      if (!projectResult.success) {
        return Result.failure(projectResult.error);
      }

      const project = projectResult.data;
      if (!project) {
        return Result.failure(new NotFoundError('Project', request.projectId));
      }

      // 2. 사용자 권한 확인 (프로젝트 멤버인지 확인)
      const hasPermission = await this.checkUserPermission(request.projectId, request.userId);
      if (!hasPermission.success) {
        return Result.failure(hasPermission.error);
      }

      // 3. 프로젝트 이미지 경로 정의
      const profileImagePath = `project_profiles/${request.projectId}/profile.webp`;

      try {
        // 4. Firebase Storage에서 프로젝트 이미지 삭제
        await this.storageService.deleteFile(profileImagePath);
        
        // 5. 성공 응답
        return Result.success({
          success: true,
          message: 'Project profile image removed successfully'
        });
      } catch (storageError) {
        // 파일이 존재하지 않는 경우도 성공으로 간주
        if (this.isFileNotFoundError(storageError)) {
          return Result.success({
            success: true,
            message: 'Project profile image already removed or does not exist'
          });
        }
        
        // 다른 스토리지 에러는 실패로 처리
        return Result.failure(new Error(`Failed to delete project profile image: ${storageError instanceof Error ? storageError.message : 'Unknown storage error'}`));
      }
    } catch (error) {
      return Result.failure(new Error(`Failed to remove project profile image: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  private async checkUserPermission(projectId: string, userId: string): Promise<CustomResult<void>> {
    try {
      // 프로젝트 멤버인지 확인
      const memberResult = await this.projectRepository.findProjectMember(projectId, userId);
      if (!memberResult.success || !memberResult.data) {
        return Result.failure(new ValidationError('permission', 'User is not a member of this project'));
      }

      return Result.success(undefined);
    } catch (error) {
      return Result.failure(new Error(`Permission check failed: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  private isFileNotFoundError(error: any): boolean {
    return error?.code === 'storage/object-not-found' || 
           error?.message?.includes('No such object') ||
           error?.message?.includes('Object does not exist');
  }
}