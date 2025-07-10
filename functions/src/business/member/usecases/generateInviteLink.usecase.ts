import { InviteRepository } from '../../../domain/invite/repositories/invite.repository';
import { InviteEntity } from '../../../domain/invite/entities/invite.entity';
import { ProjectRepository } from '../../../domain/project/repositories/project.repository';
import { MemberRepository } from '../../../domain/member/repositories/member.repository';
import { CustomResult, Result } from '../../../core/types';
import { validateId } from '../../../core/validation';
import { ValidationError, NotFoundError, UnauthorizedError } from '../../../core/errors';

export interface GenerateInviteLinkRequest {
  projectId: string;
  inviterId: string;
  expiresInHours?: number; // Default 24 hours
}

export interface GenerateInviteLinkResponse {
  inviteCode: string;
  inviteLink: string;
  expiresAt: Date;
  status: string;
}

export class GenerateInviteLinkUseCase {
  constructor(
    private readonly inviteRepository: InviteRepository,
    private readonly projectRepository: ProjectRepository,
    private readonly memberRepository: MemberRepository
  ) {}

  async execute(request: GenerateInviteLinkRequest): Promise<CustomResult<GenerateInviteLinkResponse>> {
    try {
      const {projectId, inviterId, expiresInHours = 24} = request;

      // Validate input
      if (!projectId || !inviterId) {
        return Result.failure(
          new ValidationError('request', 'projectId and inviterId are required')
        );
      }

      validateId(projectId, 'project ID');
      validateId(inviterId, 'inviter ID');

      // Check if project exists
      const projectResult = await this.projectRepository.findById(projectId);
      if (!projectResult.success) {
        return Result.failure(
          new NotFoundError('project', 'Project not found')
        );
      }

      // Check if inviter is a member of the project
      const memberResult = await this.memberRepository.findByUserId(inviterId);
      if (!memberResult.success) {
        return Result.failure(
          new UnauthorizedError('User is not an active member of this project')
        );
      }

      const member = memberResult.data;
      if (!member.isActive()) {
        return Result.failure(
          new UnauthorizedError('User is not an active member of this project')
        );
      }

      // ✅ Generate invite code with collision handling
      // Try up to 3 attempts for collision resolution (extremely rare with 8-char codes)
      let inviteCode: string;
      let attempts = 0;
      const maxAttempts = 3;

      do {
        inviteCode = InviteEntity.generateInviteCode();
        attempts++;
        
        // Check if this exact document ID already exists
        const existsResult = await this.inviteRepository.existsByCode(inviteCode);
        if (!existsResult.success) {
          return Result.failure(existsResult.error);
        }
        
        if (!existsResult.data) {
          break; // Code is unique, we can use it
        }
        
        if (attempts >= maxAttempts) {
          return Result.failure(
            new Error('Failed to generate unique invite code after maximum attempts')
          );
        }
      } while (attempts < maxAttempts);

      // Calculate expiration time
      const expiresAt = new Date();
      expiresAt.setHours(expiresAt.getHours() + expiresInHours);

      // Create invite entity with custom invite code as ID
      const invite = InviteEntity.create(
        inviteCode, // Custom invite code will be used as document ID
        projectId,
        inviterId,
        expiresAt
      );

      // Save invite
      const saveResult = await this.inviteRepository.create(invite);
      if (!saveResult.success) {
        return Result.failure(saveResult.error);
      }
      
      const savedInvite = saveResult.data;

      // Generate invite link
      const baseUrl = process.env.APP_BASE_URL || 'https://tpppk.app';
      const inviteLink = `${baseUrl}/invite/${savedInvite.inviteCode}`;

      return Result.success({
        inviteCode: savedInvite.inviteCode, // Same as savedInvite.id
        inviteLink,
        expiresAt: savedInvite.expiresAt,
        status: savedInvite.status,
      });

    } catch (error) {
      return Result.failure(
        error instanceof Error ? error : new Error('Failed to generate invite link')
      );
    }
  }
}