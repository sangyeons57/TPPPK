import {RepositoryFactory} from "../../domain/shared/RepositoryFactory";
import {ProjectWrapperRepositoryFactoryContext} from "../../domain/projectwrapper/repositories/factory/ProjectWrapperRepositoryFactoryContext";
import {ProjectRepositoryFactoryContext} from "../../domain/project/repositories/factory/ProjectRepositoryFactoryContext";
import {MemberRepositoryFactoryContext} from "../../domain/member/repositories/factory/MemberRepositoryFactoryContext";
import {ProjectWrapperRepository} from "../../domain/projectwrapper/repositories/projectwrapper.repository";
import {ProjectRepository} from "../../domain/project/repositories/project.repository";
import {MemberRepository} from "../../domain/member/repositories/member.repository";
import {SyncProjectWrapperUseCase} from "./usecases/syncProjectWrapper.usecase";

/**
 * Interface for project wrapper management use cases
 */
export interface ProjectWrapperUseCases {
  syncProjectWrapperUseCase: SyncProjectWrapperUseCase;

  // Common repositories for advanced use cases
  projectWrapperRepository: ProjectWrapperRepository;
  projectRepository: ProjectRepository;
  memberRepository: MemberRepository;
}

/**
 * Provider for project wrapper management use cases
 * Handles project wrapper synchronization and management
 */
export class ProjectWrapperUseCaseProvider {
  constructor(
    private readonly projectWrapperRepositoryFactory: RepositoryFactory<ProjectWrapperRepository, ProjectWrapperRepositoryFactoryContext>,
    private readonly projectRepositoryFactory: RepositoryFactory<ProjectRepository, ProjectRepositoryFactoryContext>,
    private readonly memberRepositoryFactory: RepositoryFactory<MemberRepository, MemberRepositoryFactoryContext>
  ) {}

  /**
   * Creates project wrapper management use cases
   * @param {ProjectWrapperRepositoryFactoryContext} [context] - Optional context for repository creation
   * @return {ProjectWrapperUseCases} ProjectWrapperUseCases instance
   */
  create(context?: ProjectWrapperRepositoryFactoryContext): ProjectWrapperUseCases {
    const projectWrapperRepository = this.projectWrapperRepositoryFactory.create(context);
    const projectRepository = this.projectRepositoryFactory.create();

    // MemberRepository는 기본 프로젝트 컨텍스트로 생성 (개별 사용을 위함)
    const memberRepository = this.memberRepositoryFactory.create({
      projectId: "default",
    });

    return {
      syncProjectWrapperUseCase: new SyncProjectWrapperUseCase(
        projectWrapperRepository,
        projectRepository,
        this.memberRepositoryFactory
      ),

      // Common repositories
      projectWrapperRepository,
      projectRepository,
      memberRepository,
    };
  }
}
