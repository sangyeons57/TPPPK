import {MemberRepository} from "../../../domain/member/repositories/member.repository";
import {ProjectRepository} from "../../../domain/project/repositories/project.repository";
import {UserRepository} from "../../../domain/user/repositories/user.repository";
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
  membersRemoved: number;
  projectWrappersCleaned: number;
  projectDeactivated: boolean;
}

export class DeleteProjectUseCase {
  constructor(
    private readonly memberRepository: MemberRepository,
    private readonly projectRepository: ProjectRepository,
    private readonly userRepository: UserRepository
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

      // Check if the user performing the deletion has permission (is a member)
      logger.info(`🔍 Checking if user is a member: userId=${request.deletedBy}, projectId=${request.projectId}`);
      const deleterMemberResult = await this.memberRepository.findByUserId(request.deletedBy);
      logger.info(`👤 Member lookup result: success=${deleterMemberResult.success}, error=${!deleterMemberResult.success ? deleterMemberResult.error?.message : "none"}`);

      if (!deleterMemberResult.success) {
        logger.error(`❌ User is not a member or member lookup failed: userId=${request.deletedBy}, error=${deleterMemberResult.error?.message}`);
        return Result.failure(
          new ValidationError("deletedBy", "Only project members can delete the project")
        );
      }
      logger.info(`✅ User is a valid member: ${request.deletedBy}`);

      // TODO: Add role-based permission check here
      // For now, any member can delete the project, but you might want to restrict this to owners/admins

      const project = projectResult.data;
      if (!project) {
        return Result.failure(
          new NotFoundError("project", `Project not found: ${request.projectId}`)
        );
      }

      // Step 1: Get all members before deletion
      const allMembersResult = await this.memberRepository.findAll();
      let memberCount = 0;
      let memberUserIds: string[] = [];

      if (allMembersResult.success) {
        memberCount = allMembersResult.data.length;
        memberUserIds = allMembersResult.data.map((member) => member.userId);
      }

      // Step 2: Delete all members from the project
      const deleteMembersResult = await this.memberRepository.deleteAll();
      if (!deleteMembersResult.success) {
        return Result.failure(deleteMembersResult.error);
      }

      // Step 3: Remove project wrappers from all users
      let projectWrappersCleaned = 0;
      for (const userId of memberUserIds) {
        try {
          const removeWrapperResult = await this.userRepository.removeProjectWrapper(
            userId,
            request.projectId
          );

          if (removeWrapperResult.success) {
            projectWrappersCleaned++;
          } else {
            logger.warn(`Failed to remove project wrapper for user ${userId}: ${removeWrapperResult.error?.message}`);
          }
        } catch (error) {
          logger.warn(`Error removing project wrapper for user ${userId}: ${error}`);
        }
      }

      // Step 4: Mark the project as deleted (rather than actually deleting)
      // This preserves data integrity and allows for potential recovery
      const deletedProject = project.delete();
      const saveProjectResult = await this.projectRepository.save(deletedProject);

      if (!saveProjectResult.success) {
        logger.warn(`Failed to mark project as deleted ${request.projectId}: ${saveProjectResult.error?.message}`);
      }

      const deletedAt = new Date().toISOString();

      return Result.success({
        success: true,
        deletedProjectId: request.projectId,
        deletedAt,
        membersRemoved: memberCount,
        projectWrappersCleaned,
        projectDeactivated: saveProjectResult.success,
      });
    } catch (error) {
      return Result.failure(
        error instanceof Error ? error : new Error("Failed to delete project")
      );
    }
  }
}
