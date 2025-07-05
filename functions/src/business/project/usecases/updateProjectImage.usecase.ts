import { ImageProcessingService, ProcessedImageEntity } from '../../../core/services/imageProcessing.service';
import { ProjectRepository } from '../../../domain/project/repositories/project.repository';
import { ImageType } from '../../../core/types/image.types';
import { CustomResult, Result } from '../../../core/types';
import { NotFoundError, UnauthorizedError } from '../../../core/errors';

export interface UpdateProjectImageRequest {
  projectId: string;
  userId: string;
  imageBuffer: Buffer;
  contentType: string;
}

export interface UpdateProjectImageResponse {
  imageUrl: string;
  thumbnailUrl?: string;
  project: any;
}

export class UpdateProjectImageUseCase {
  constructor(
    private readonly imageProcessingService: ImageProcessingService,
    private readonly projectRepository: ProjectRepository
  ) {}

  async execute(request: UpdateProjectImageRequest): Promise<CustomResult<UpdateProjectImageResponse>> {
    try {
      const projectResult = await this.projectRepository.findById(request.projectId);
      if (!projectResult.success) {
        return Result.failure(projectResult.error);
      }

      const project = projectResult.data;
      if (!project) {
        return Result.failure(new NotFoundError('Project', request.projectId));
      }

      if (project.ownerId !== request.userId) {
        return Result.failure(new UnauthorizedError('Only project owner can update project image'));
      }

      const processResult = await this.imageProcessingService.processAndUploadImage(
        request.imageBuffer,
        request.contentType,
        request.userId,
        ImageType.PROJECT_IMAGE
      );

      if (!processResult.success) {
        return Result.failure(processResult.error);
      }

      const processedImage = processResult.data;

      const updatedProject = project.updateProject({
        image: processedImage.originalUrl
      });

      const updateResult = await this.projectRepository.update(updatedProject);
      if (!updateResult.success) {
        await this.imageProcessingService.deleteImage(processedImage.id);
        return Result.failure(updateResult.error);
      }

      return Result.success({
        imageUrl: processedImage.originalUrl.value,
        thumbnailUrl: processedImage.thumbnailUrl?.value,
        project: updateResult.data
      });
    } catch (error) {
      return Result.failure(new Error(`Failed to update project image: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }
}