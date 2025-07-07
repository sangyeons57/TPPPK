import { MemberRepositoryFactory } from '../../domain/member/repositories/factory/MemberRepositoryFactory';
import { MemberRepositoryFactoryContext } from '../../domain/member/repositories/factory/MemberRepositoryFactoryContext';
import { ProjectRepositoryFactory } from '../../domain/project/repositories/factory/ProjectRepositoryFactory';
import { UserRepositoryFactory } from '../../domain/user/repositories/factory/UserRepositoryFactory';
import { MemberRepository } from '../../domain/member/repositories/member.repository';
import { ProjectRepository } from '../../domain/project/repositories/project.repository';
import { UserRepository } from '../../domain/user/repositories/user.repository';
import {
  RemoveMemberUseCase,
  BlockMemberUseCase,
  LeaveMemberUseCase,
  DeleteProjectUseCase
} from './usecases';

export interface MemberUseCases {
  removeMemberUseCase: RemoveMemberUseCase;
  blockMemberUseCase: BlockMemberUseCase;
  leaveMemberUseCase: LeaveMemberUseCase;
  deleteProjectUseCase: DeleteProjectUseCase;
  
  // Common repositories for advanced use cases
  memberRepository: MemberRepository;
  projectRepository: ProjectRepository;
  userRepository: UserRepository;
}

export class MemberUseCaseProvider {
  constructor(
    private readonly memberRepositoryFactory: MemberRepositoryFactory,
    private readonly projectRepositoryFactory: ProjectRepositoryFactory,
    private readonly userRepositoryFactory: UserRepositoryFactory
  ) {}

  create(context?: MemberRepositoryFactoryContext): MemberUseCases {
    // Create repositories with appropriate contexts
    const memberRepository = this.memberRepositoryFactory.create(context);
    const projectRepository = this.projectRepositoryFactory.create();
    const userRepository = this.userRepositoryFactory.create();

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
      leaveMemberUseCase: new LeaveMemberUseCase(
        memberRepository,
        projectRepository,
        userRepository
      ),
      deleteProjectUseCase: new DeleteProjectUseCase(
        memberRepository,
        projectRepository,
        userRepository
      ),
      
      // Common repositories
      memberRepository,
      projectRepository,
      userRepository
    };
  }
}