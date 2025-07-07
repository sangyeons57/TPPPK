import { MemberRepository } from '../../../domain/member/repositories/member.repository';
import { ProjectRepository } from '../../../domain/project/repositories/project.repository';
import { UserRepository } from '../../../domain/user/repositories/user.repository';
import { CustomResult, Result } from '../../../core/types';
import { validateId } from '../../../core/validation';
import { ValidationError, NotFoundError } from '../../../core/errors';

export interface RemoveMemberRequest {
  projectId: string;
  userId: string;
  removedBy: string; // User ID of who is removing the member
}

export interface RemoveMemberResponse {
  success: boolean;
  removedUserId: string;
  removedAt: string;
  memberRemoved: boolean;
  projectWrapperRemoved: boolean;
}

export class RemoveMemberUseCase {
  constructor(
    private readonly memberRepository: MemberRepository,
    private readonly projectRepository: ProjectRepository,
    private readonly userRepository: UserRepository
  ) {}

  async execute(request: RemoveMemberRequest): Promise<CustomResult<RemoveMemberResponse>> {
    try {
      // Input validation
      if (!request.projectId || !request.userId || !request.removedBy) {
        return Result.failure(
          new ValidationError("request", "Project ID, user ID, and removed by are required")
        );
      }

      validateId(request.projectId, "project ID");
      validateId(request.userId, "user ID");
      validateId(request.removedBy, "removed by");

      // Cannot remove yourself (use leaveMember for self-removal)
      if (request.userId === request.removedBy) {
        return Result.failure(
          new ValidationError("userId", "Cannot remove yourself from project. Use leave member instead.")
        );
      }

      // Check if the project exists
      const projectResult = await this.projectRepository.findById(request.projectId);
      if (!projectResult.success) {
        return Result.failure(
          new NotFoundError("project", `Project not found: ${request.projectId}`)
        );
      }

      // Check if the user being removed is actually a member
      const memberResult = await this.memberRepository.findByUserId(request.userId);
      if (!memberResult.success) {
        return Result.failure(
          new NotFoundError("member", `User is not a member of this project: ${request.userId}`)
        );
      }

      // Check if the user performing the removal has permission (is also a member)
      const removerMemberResult = await this.memberRepository.findByUserId(request.removedBy);
      if (!removerMemberResult.success) {
        return Result.failure(
          new ValidationError("removedBy", "Only project members can remove other members")
        );
      }

      // TODO: Add role-based permission check here if needed
      // For now, any member can remove any other member

      // Remove the member from the project
      const deleteMemberResult = await this.memberRepository.deleteByUserId(request.userId);
      if (!deleteMemberResult.success) {
        return Result.failure(deleteMemberResult.error);
      }

      // Remove the project wrapper from the user's collection
      // This is handled by the UserRepository which manages the user's project wrappers
      let projectWrapperRemoved = false;
      try {
        const removeWrapperResult = await this.userRepository.removeProjectWrapper(
          request.userId, 
          request.projectId
        );
        projectWrapperRemoved = removeWrapperResult.success;
        
        // Log but don't fail the operation if wrapper removal fails
        if (!removeWrapperResult.success) {
          console.warn(`Failed to remove project wrapper for user ${request.userId}:`, removeWrapperResult.error);
        }
      } catch (error) {
        console.warn(`Error removing project wrapper for user ${request.userId}:`, error);
      }

      const removedAt = new Date().toISOString();

      return Result.success({
        success: true,
        removedUserId: request.userId,
        removedAt,
        memberRemoved: true,
        projectWrapperRemoved
      });

    } catch (error) {
      return Result.failure(
        error instanceof Error ? error : new Error("Failed to remove member")
      );
    }
  }
}