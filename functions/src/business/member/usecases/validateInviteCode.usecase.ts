import { InviteRepository } from '../../../domain/invite/repositories/invite.repository';
import { ProjectRepository } from '../../../domain/project/repositories/project.repository';
import { MemberRepository } from '../../../domain/member/repositories/member.repository';
import { CustomResult, Result } from '../../../core/types';
import { validateId } from '../../../core/validation';
import { ValidationError } from '../../../core/errors';

export interface ValidateInviteCodeRequest {
  inviteCode: string;
  userId?: string; // Optional - to check if user is already a member
}

export interface ValidateInviteCodeResponse {
  valid: boolean;
  projectId?: string;
  projectName?: string;
  projectImage?: string;
  inviterName?: string;
  expiresAt?: Date;
  maxUses?: number;
  currentUses?: number;
  isAlreadyMember?: boolean;
  errorMessage?: string;
}

export class ValidateInviteCodeUseCase {
  constructor(
    private readonly inviteRepository: InviteRepository,
    private readonly projectRepository: ProjectRepository,
    private readonly memberRepository: MemberRepository
  ) {}

  async execute(request: ValidateInviteCodeRequest): Promise<CustomResult<ValidateInviteCodeResponse>> {
    try {
      const { inviteCode, userId } = request;

      // Validate input
      if (!inviteCode) {
        return Result.failure(
          new ValidationError('request', 'inviteCode is required')
        );
      }

      if (userId) {
        validateId(userId, 'user ID');
      }

      // Find invite by code
      const inviteResult = await this.inviteRepository.findByCode(inviteCode);
      if (!inviteResult.success) {
        return Result.success({
          valid: false,
          errorMessage: 'Failed to validate invite code'
        });
      }

      const invite = inviteResult.data;
      if (!invite) {
        return Result.success({
          valid: false,
          errorMessage: 'Invite code not found'
        });
      }

      // Check if invite is usable
      if (!invite.canBeUsed()) {
        let errorMessage = 'Invite is not usable';
        if (invite.isExpired()) {
          errorMessage = 'Invite has expired';
        } else if (invite.isRevoked()) {
          errorMessage = 'Invite has been revoked';
        } else if (invite.maxUses !== undefined && invite.currentUses >= invite.maxUses) {
          errorMessage = 'Invite has reached maximum usage limit';
        }
        
        return Result.success({
          valid: false,
          errorMessage
        });
      }

      // Get project information
      const projectResult = await this.projectRepository.findById(invite.projectId);
      if (!projectResult.success) {
        return Result.success({
          valid: false,
          errorMessage: 'Failed to validate invite code'
        });
      }

      const project = projectResult.data;
      if (!project) {
        return Result.success({
          valid: false,
          errorMessage: 'Project not found'
        });
      }

      // Check if user is already a member (if userId provided)
      let isAlreadyMember = false;
      if (userId) {
        const existingMemberResult = await this.memberRepository.findByUserId(userId);
        if (existingMemberResult.success && existingMemberResult.data) {
          isAlreadyMember = existingMemberResult.data.isActive();
        }
      }

      return Result.success({
        valid: true,
        projectId: invite.projectId,
        projectName: project.name,
        projectImage: project.image,
        expiresAt: invite.expiresAt,
        maxUses: invite.maxUses,
        currentUses: invite.currentUses,
        isAlreadyMember
      });

    } catch (error) {
      return Result.failure(
        error instanceof Error ? error : new Error('Failed to validate invite code')
      );
    }
  }
}