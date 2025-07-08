import {ProjectRepository} from "../../../domain/project/repositories/project.repository";
import {CustomResult, Result} from "../../../core/types";
import {validateId} from "../../../core/validation";
import {ValidationError, NotFoundError} from "../../../core/errors";
import {logger} from "firebase-functions/v2";

export interface DeleteProjectRequest {
  projectId: string;
  deletedBy: string; // User ID of who is deleting the project
}

export interface DeleteProjectResponse {
  success: boolean;
  deletedProjectId: string;
  deletedAt: string;
  projectDeactivated: boolean;
}

export class DeleteProjectUseCase {
  constructor(
    private readonly projectRepository: ProjectRepository
  ) {}

  async execute(request: DeleteProjectRequest): Promise<CustomResult<DeleteProjectResponse>> {
    try {
      logger.info(`🎯 DeleteProjectUseCase starting: projectId=${request.projectId}, deletedBy=${request.deletedBy}`);

      // Input validation
      if (!request.projectId || !request.deletedBy) {
        logger.error("❌ Missing required fields in request");
        return Result.failure(
          new ValidationError("request", "Project ID and deleted by are required")
        );
      }

      validateId(request.projectId, "project ID");
      validateId(request.deletedBy, "deleted by");
      logger.info("✅ Input validation passed");

      // Check if the project exists
      logger.info(`🔍 Checking if project exists: ${request.projectId}`);
      const projectResult = await this.projectRepository.findById(request.projectId);
      if (!projectResult.success) {
        logger.error(`❌ Project not found: ${request.projectId}, error: ${projectResult.error?.message}`);
        return Result.failure(
          new NotFoundError("project", `Project not found: ${request.projectId}`)
        );
      }
      logger.info(`✅ Project found: ${request.projectId}`);

      const project = projectResult.data;
      if (!project) {
        return Result.failure(
          new NotFoundError("project", `Project not found: ${request.projectId}`)
        );
      }

      // Check if the user performing the deletion is the project owner
      logger.info(`🔍 Checking if user is project owner: userId=${request.deletedBy}, projectOwnerId=${project.ownerId}`);
      if (project.ownerId !== request.deletedBy) {
        logger.error(`❌ Permission denied: Only project owner can delete the project. userId=${request.deletedBy}, ownerId=${project.ownerId}`);
        return Result.failure(
          new ValidationError("deletedBy", "Only project owner can delete the project")
        );
      }
      logger.info(`✅ User is project owner: ${request.deletedBy}`);

      // Mark the project as deleted
      // ProjectWrapper cleanup will be handled when users try to access the project
      logger.info(`🗑️ Marking project as deleted: ${request.projectId}`);
      const deletedProject = project.delete();
      const saveProjectResult = await this.projectRepository.save(deletedProject);

      if (!saveProjectResult.success) {
        logger.error(`❌ Failed to mark project as deleted ${request.projectId}: ${saveProjectResult.error?.message}`);
        return Result.failure(saveProjectResult.error);
      }

      const deletedAt = new Date().toISOString();

      logger.info(`✅ Project deletion completed successfully: projectId=${request.projectId}`);

      return Result.success({
        success: true,
        deletedProjectId: request.projectId,
        deletedAt,
        projectDeactivated: saveProjectResult.success,
      });
    } catch (error) {
      logger.error(`❌ Error in DeleteProjectUseCase: ${error instanceof Error ? error.message : "Unknown error"}`);
      return Result.failure(
        error instanceof Error ? error : new Error("Failed to delete project")
      );
    }
  }
}
