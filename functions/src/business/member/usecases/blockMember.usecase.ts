import { MemberRepository } from '../../../domain/member/repositories/member.repository';
import { ProjectRepository } from '../../../domain/project/repositories/project.repository';
import { CustomResult, Result } from '../../../core/types';
import { validateId } from '../../../core/validation';
import { ValidationError, NotFoundError } from '../../../core/errors';

export interface BlockMemberRequest {
  projectId: string;
  userId: string;
  blockedBy: string; // User ID of who is blocking the member
}

export interface BlockMemberResponse {
  success: boolean;
  blockedUserId: string;
  blockedAt: string;
  memberBlocked: boolean;
}

export class BlockMemberUseCase {
  constructor(
    private readonly memberRepository: MemberRepository,
    private readonly projectRepository: ProjectRepository
  ) {}

  async execute(request: BlockMemberRequest): Promise<CustomResult<BlockMemberResponse>> {
    try {
      // Input validation
      if (!request.projectId || !request.userId || !request.blockedBy) {
        return Result.failure(
          new ValidationError("request", "Project ID, user ID, and blocked by are required")
        );
      }

      validateId(request.projectId, "project ID");
      validateId(request.userId, "user ID");
      validateId(request.blockedBy, "blocked by");

      // Cannot block yourself
      if (request.userId === request.blockedBy) {
        return Result.failure(
          new ValidationError("userId", "Cannot block yourself")
        );
      }

      // Check if the project exists
      const projectResult = await this.projectRepository.findById(request.projectId);
      if (!projectResult.success) {
        return Result.failure(
          new NotFoundError("project", `Project not found: ${request.projectId}`)
        );
      }

      // Check if the user being blocked is actually a member
      const memberResult = await this.memberRepository.findByUserId(request.userId);
      if (!memberResult.success) {
        return Result.failure(
          new NotFoundError("member", `User is not a member of this project: ${request.userId}`)
        );
      }

      // Check if the user performing the blocking has permission (is also a member)
      const blockerMemberResult = await this.memberRepository.findByUserId(request.blockedBy);
      if (!blockerMemberResult.success) {
        return Result.failure(
          new ValidationError("blockedBy", "Only project members can block other members")
        );
      }

      // TODO: Add role-based permission check here if needed
      // For now, any member can block any other member

      const member = memberResult.data;
      
      // Check if member is already blocked
      if (member.isBlocked()) {
        return Result.failure(
          new ValidationError("member", "Member is already blocked")
        );
      }

      // Block the member
      const blockedMember = member.block();
      
      // Save the updated member
      const saveResult = await this.memberRepository.save(blockedMember);
      if (!saveResult.success) {
        return Result.failure(saveResult.error);
      }

      const blockedAt = new Date().toISOString();

      return Result.success({
        success: true,
        blockedUserId: request.userId,
        blockedAt,
        memberBlocked: true
      });

    } catch (error) {
      return Result.failure(
        error instanceof Error ? error : new Error("Failed to block member")
      );
    }
  }
}