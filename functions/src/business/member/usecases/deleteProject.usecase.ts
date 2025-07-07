import { MemberRepository } from '../../../domain/member/repositories/member.repository';
import { ProjectRepository } from '../../../domain/project/repositories/project.repository';
import { UserRepository } from '../../../domain/user/repositories/user.repository';
import { CustomResult, Result } from '../../../core/types';
import { validateId } from '../../../core/validation';
import { ValidationError, NotFoundError } from '../../../core/errors';

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
      // Input validation
      if (!request.projectId || !request.deletedBy) {
        return Result.failure(
          new ValidationError("request", "Project ID and deleted by are required")
        );
      }

      validateId(request.projectId, "project ID");
      validateId(request.deletedBy, "deleted by");

      // Check if the project exists
      const projectResult = await this.projectRepository.findById(request.projectId);
      if (!projectResult.success) {
        return Result.failure(
          new NotFoundError("project", `Project not found: ${request.projectId}`)
        );
      }

      // Check if the user performing the deletion has permission (is a member)
      const deleterMemberResult = await this.memberRepository.findByUserId(request.deletedBy);
      if (!deleterMemberResult.success) {
        return Result.failure(
          new ValidationError("deletedBy", "Only project members can delete the project")
        );
      }

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
        memberUserIds = allMembersResult.data.map(member => member.userId);
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
            console.warn(`Failed to remove project wrapper for user ${userId}:`, removeWrapperResult.error);
          }
        } catch (error) {
          console.warn(`Error removing project wrapper for user ${userId}:`, error);
        }
      }

      // Step 4: Mark the project as deleted (rather than actually deleting)
      // This preserves data integrity and allows for potential recovery
      const deletedProject = project.delete();
      const saveProjectResult = await this.projectRepository.save(deletedProject);
      
      if (!saveProjectResult.success) {
        console.warn(`Failed to mark project as deleted ${request.projectId}:`, saveProjectResult.error);
      }

      const deletedAt = new Date().toISOString();

      return Result.success({
        success: true,
        deletedProjectId: request.projectId,
        deletedAt,
        membersRemoved: memberCount,
        projectWrappersCleaned,
        projectDeactivated: saveProjectResult.success
      });

    } catch (error) {
      return Result.failure(
        error instanceof Error ? error : new Error("Failed to delete project")
      );
    }
  }
}