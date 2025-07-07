import { MemberRepository } from '../../../domain/member/repositories/member.repository';
import { ProjectRepository } from '../../../domain/project/repositories/project.repository';
import { UserRepository } from '../../../domain/user/repositories/user.repository';
import { CustomResult, Result } from '../../../core/types';
import { validateId } from '../../../core/validation';
import { ValidationError, NotFoundError } from '../../../core/errors';

export interface LeaveMemberRequest {
  projectId: string;
  userId: string; // User ID of who is leaving
}

export interface LeaveMemberResponse {
  success: boolean;
  leftUserId: string;
  leftAt: string;
  memberRemoved: boolean;
  projectWrapperRemoved: boolean;
}

export class LeaveMemberUseCase {
  constructor(
    private readonly memberRepository: MemberRepository,
    private readonly projectRepository: ProjectRepository,
    private readonly userRepository: UserRepository
  ) {}

  async execute(request: LeaveMemberRequest): Promise<CustomResult<LeaveMemberResponse>> {
    try {
      // Input validation
      if (!request.projectId || !request.userId) {
        return Result.failure(
          new ValidationError("request", "Project ID and user ID are required")
        );
      }

      validateId(request.projectId, "project ID");
      validateId(request.userId, "user ID");

      // Check if the project exists
      const projectResult = await this.projectRepository.findById(request.projectId);
      if (!projectResult.success) {
        return Result.failure(
          new NotFoundError("project", `Project not found: ${request.projectId}`)
        );
      }

      // Check if the user is actually a member
      const memberResult = await this.memberRepository.findByUserId(request.userId);
      if (!memberResult.success) {
        return Result.failure(
          new NotFoundError("member", `User is not a member of this project: ${request.userId}`)
        );
      }

      // Check if this is the last member - might need special handling
      const memberCountResult = await this.memberRepository.countActive();
      if (memberCountResult.success && memberCountResult.data === 1) {
        // This is the last member leaving - you might want to handle this differently
        // For now, we'll allow it, but you could implement project archival logic here
        console.warn(`Last member leaving project ${request.projectId}`);
      }

      // Remove the member from the project
      const deleteMemberResult = await this.memberRepository.deleteByUserId(request.userId);
      if (!deleteMemberResult.success) {
        return Result.failure(deleteMemberResult.error);
      }

      // Remove the project wrapper from the user's collection
      // This ensures the project no longer appears in the user's project list
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

      const leftAt = new Date().toISOString();

      return Result.success({
        success: true,
        leftUserId: request.userId,
        leftAt,
        memberRemoved: true,
        projectWrapperRemoved
      });

    } catch (error) {
      return Result.failure(
        error instanceof Error ? error : new Error("Failed to leave project")
      );
    }
  }
}