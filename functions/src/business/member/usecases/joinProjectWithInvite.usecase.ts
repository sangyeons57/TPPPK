import { InviteRepository } from '../../../domain/invite/repositories/invite.repository';
import { ProjectRepository } from '../../../domain/project/repositories/project.repository';
import { MemberRepository } from '../../../domain/member/repositories/member.repository';
import { ProjectWrapperRepository } from '../../../domain/projectwrapper/repositories/projectwrapper.repository';
import { UserRepository } from '../../../domain/user/repositories/user.repository';
import { MemberEntity } from '../../../domain/member/entities/member.entity';
import { ProjectWrapperEntity, ProjectWrapperStatus } from '../../../domain/projectwrapper/entities/projectwrapper.entity';
import { CustomResult, Result } from '../../../core/types';
import { validateId } from '../../../core/validation';
import { ValidationError, NotFoundError, ConflictError, UnauthorizedError } from '../../../core/errors';

export interface JoinProjectWithInviteRequest {
  inviteCode: string;
  userId: string;
}

export interface JoinProjectWithInviteResponse {
  projectId: string;
  projectName: string;
  membershipId: string;
  success: boolean;
  message: string;
}

export class JoinProjectWithInviteUseCase {
  constructor(
    private readonly inviteRepository: InviteRepository,
    private readonly projectRepository: ProjectRepository,
    private readonly memberRepository: MemberRepository,
    private readonly projectWrapperRepository: ProjectWrapperRepository,
    private readonly userRepository: UserRepository
  ) {}

  async execute(request: JoinProjectWithInviteRequest): Promise<CustomResult<JoinProjectWithInviteResponse>> {
    try {
      const { inviteCode, userId } = request;

      // Validate input
      if (!inviteCode || !userId) {
        return Result.failure(
          new ValidationError('request', 'inviteCode and userId are required')
        );
      }

      validateId(userId, 'user ID');

      // Find invite by code
      const inviteResult = await this.inviteRepository.findByCode(inviteCode);
      if (!inviteResult.success) {
        return Result.failure(inviteResult.error);
      }

      const invite = inviteResult.data;
      if (!invite) {
        return Result.failure(
          new NotFoundError('invite', 'Invite not found')
        );
      }

      // Check if invite is usable
      if (!invite.canBeUsed()) {
        return Result.failure(
          new UnauthorizedError('Invite is expired, revoked, or has reached maximum uses')
        );
      }

      // Check if user exists
      const userResult = await this.userRepository.findById(userId);
      if (!userResult.success) {
        return Result.failure(userResult.error);
      }

      if (!userResult.data) {
        return Result.failure(
          new NotFoundError('user', 'User not found')
        );
      }

      // Check if project exists
      const projectResult = await this.projectRepository.findById(invite.projectId);
      if (!projectResult.success) {
        return Result.failure(projectResult.error);
      }

      const project = projectResult.data;
      if (!project) {
        return Result.failure(
          new NotFoundError('project', 'Project not found')
        );
      }

      // Check if user is already a member of the project
      const existingMemberResult = await this.memberRepository.findByUserId(userId);
      if (existingMemberResult.success && existingMemberResult.data && existingMemberResult.data.isActive()) {
        return Result.failure(
          new ConflictError('user', 'membership', userId)
        );
      }

      // Check if user already has a project wrapper for this project
      const existingWrapperResult = await this.projectWrapperRepository.findByUserIdAndProjectId(userId, invite.projectId);
      if (existingWrapperResult.success && existingWrapperResult.data && existingWrapperResult.data.status === ProjectWrapperStatus.ACTIVE) {
        return Result.failure(
          new ConflictError('user', 'projectWrapper', userId)
        );
      }

      // Create member entity with default role (assuming 'member' role exists)
      const defaultRoleId = 'member'; // This should be configurable or retrieved from somewhere
      const member = MemberEntity.create(
        '', // ID will be set by repository
        invite.projectId,
        userId,
        [defaultRoleId]
      );

      // Save member
      const savedMemberResult = await this.memberRepository.save(member);
      if (!savedMemberResult.success) {
        return Result.failure(savedMemberResult.error);
      }

      const savedMember = savedMemberResult.data;

      // Create or update project wrapper
      let projectWrapper: ProjectWrapperEntity;
      if (existingWrapperResult.success && existingWrapperResult.data) {
        // Reactivate existing wrapper
        projectWrapper = existingWrapperResult.data.activate();
        const updateWrapperResult = await this.projectWrapperRepository.update(userId, projectWrapper);
        if (!updateWrapperResult.success) {
          return Result.failure(updateWrapperResult.error);
        }
      } else {
        // Create new project wrapper
        projectWrapper = ProjectWrapperEntity.create(
          invite.projectId,
          project.name,
          project.image
        );
        const createWrapperResult = await this.projectWrapperRepository.save(userId, projectWrapper);
        if (!createWrapperResult.success) {
          return Result.failure(createWrapperResult.error);
        }
      }

      // Note: Global invite links don't track usage count
      // No need to update invite usage for this approach

      // Update project member count
      const updatedProject = project.updateMemberCount(project.memberCount + 1);
      const updateProjectResult = await this.projectRepository.update(updatedProject);
      if (!updateProjectResult.success) {
        // Log warning but don't fail the operation
        console.warn('Failed to update project member count:', updateProjectResult.error);
      }

      return Result.success({
        projectId: invite.projectId,
        projectName: project.name,
        membershipId: savedMember.id,
        success: true,
        message: 'Successfully joined the project'
      });

    } catch (error) {
      return Result.failure(
        error instanceof Error ? error : new Error('Failed to join project with invite')
      );
    }
  }
}