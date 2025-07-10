import { MemberRepositoryFactory } from '../../domain/member/repositories/factory/MemberRepositoryFactory';
import { MemberRepositoryFactoryContext } from '../../domain/member/repositories/factory/MemberRepositoryFactoryContext';
import { ProjectRepositoryFactory } from '../../domain/project/repositories/factory/ProjectRepositoryFactory';
import { UserRepositoryFactory } from '../../domain/user/repositories/factory/UserRepositoryFactory';
import { ProjectWrapperRepositoryFactory } from '../../domain/projectwrapper/repositories/factory/ProjectWrapperRepositoryFactory';
import { InviteRepositoryFactory } from '../../domain/invite/repositories/factory/InviteRepositoryFactory';
import { InviteRepositoryFactoryImpl } from '../../domain/invite/repositories/factory/InviteRepositoryFactoryImpl';
import { MemberRepository } from '../../domain/member/repositories/member.repository';
import { ProjectRepository } from '../../domain/project/repositories/project.repository';
import { UserRepository } from '../../domain/user/repositories/user.repository';
import { ProjectWrapperRepository } from '../../domain/projectwrapper/repositories/projectwrapper.repository';
import { InviteRepository } from '../../domain/invite/repositories/invite.repository';
import {
  RemoveMemberUseCase,
  BlockMemberUseCase,
  // LeaveMemberUseCase, // Temporarily disabled
  DeleteProjectUseCase,
  GenerateInviteLinkUseCase,
  JoinProjectWithInviteUseCase,
  ValidateInviteCodeUseCase
} from './usecases';

export interface MemberUseCases {
  removeMemberUseCase: RemoveMemberUseCase;
  blockMemberUseCase: BlockMemberUseCase;
  // leaveMemberUseCase: LeaveMemberUseCase; // Temporarily disabled
  deleteProjectUseCase: DeleteProjectUseCase;
  generateInviteLinkUseCase: GenerateInviteLinkUseCase;
  joinProjectWithInviteUseCase: JoinProjectWithInviteUseCase;
  validateInviteCodeUseCase: ValidateInviteCodeUseCase;
  
  // Common repositories for advanced use cases
  memberRepository: MemberRepository;
  projectRepository: ProjectRepository;
  userRepository: UserRepository;
  projectWrapperRepository: ProjectWrapperRepository;
  inviteRepository: InviteRepository;
}

export class MemberUseCaseProvider {
  constructor(
    private readonly memberRepositoryFactory: MemberRepositoryFactory,
    private readonly projectRepositoryFactory: ProjectRepositoryFactory,
    private readonly userRepositoryFactory: UserRepositoryFactory,
    private readonly projectWrapperRepositoryFactory: ProjectWrapperRepositoryFactory,
    private readonly inviteRepositoryFactory: InviteRepositoryFactory = new InviteRepositoryFactoryImpl()
  ) {}

  /**
   * Creates an invite repository without requiring member context
   * Used for invite code validation before knowing the projectId
   */
  createInviteRepository(): InviteRepository {
    return this.inviteRepositoryFactory.create();
  }

  create(context?: MemberRepositoryFactoryContext): MemberUseCases {
    // Create repositories with appropriate contexts
    const memberRepository = this.memberRepositoryFactory.create(context);
    const projectRepository = this.projectRepositoryFactory.create();
    const userRepository = this.userRepositoryFactory.create();
    const projectWrapperRepository = this.projectWrapperRepositoryFactory.create();
    const inviteRepository = this.inviteRepositoryFactory.create();

    return {
      removeMemberUseCase: new RemoveMemberUseCase(
        memberRepository,
        projectRepository,
        userRepository
      ),
      blockMemberUseCase: new BlockMemberUseCase(
        memberRepository,
        projectRepository
      ),
      // leaveMemberUseCase: new LeaveMemberUseCase( // Temporarily disabled
      //   memberRepository,
      //   projectRepository,
      //   userRepository
      // ),
      deleteProjectUseCase: new DeleteProjectUseCase(
        projectRepository
      ),
      generateInviteLinkUseCase: new GenerateInviteLinkUseCase(
        inviteRepository,
        projectRepository,
        memberRepository
      ),
      joinProjectWithInviteUseCase: new JoinProjectWithInviteUseCase(
        inviteRepository,
        projectRepository,
        memberRepository,
        projectWrapperRepository,
        userRepository
      ),
      validateInviteCodeUseCase: new ValidateInviteCodeUseCase(
        inviteRepository,
        projectRepository,
        memberRepository
      ),
      
      // Common repositories
      memberRepository,
      projectRepository,
      userRepository,
      projectWrapperRepository,
      inviteRepository
    };
  }
}