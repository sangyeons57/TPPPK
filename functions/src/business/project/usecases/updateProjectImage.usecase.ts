import {CustomResult, Result} from "../../../core/types";
import {ValidationError, NotFoundError} from "../../../core/errors";
import {ProjectRepository} from "../../../domain/project/repositories/project.repository";
import {ProjectImage} from "../../../domain/project/entities/project.entity";

export interface UpdateProjectImageRequest {
  projectId: string;
  imageUrl: string;
}

export interface UpdateProjectImageResponse {
  projectId: string;
  imageUrl?: string;
  updatedAt: string;
}

/**
 * Simple use case to update project image URL
 * Triggered by Firebase Storage uploads
 */
export class UpdateProjectImageUseCase {
  constructor(
    private readonly projectRepository: ProjectRepository
  ) {}

  async execute(request: UpdateProjectImageRequest): Promise<CustomResult<UpdateProjectImageResponse>> {
    try {
      // Input validation
      if (!request.projectId) {
        return Result.failure(new ValidationError("projectId", "Project ID is required"));
      }

      if (!request.imageUrl) {
        return Result.failure(new ValidationError("imageUrl", "Image URL is required"));
      }

      // Find project
      const projectResult = await this.projectRepository.findById(request.projectId);
      if (!projectResult.success) {
        return Result.failure(projectResult.error);
      }

      if (!projectResult.data) {
        return Result.failure(new NotFoundError("Project not found", request.projectId));
      }

      const project = projectResult.data;

      // Create new ProjectImage and update project
      const newImage = new ProjectImage(request.imageUrl);
      const updatedProject = project.updateProject({
        image: newImage
      });

      // Save updated project
      const saveResult = await this.projectRepository.update(updatedProject);
      if (!saveResult.success) {
        return Result.failure(saveResult.error);
      }

      return Result.success({
        projectId: updatedProject.id,
        imageUrl: updatedProject.image?.value,
        updatedAt: updatedProject.updatedAt.toISOString()
      });

    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error("Failed to update project image"));
    }
  }
}