import { RepositoryFactory } from "../../domain/shared/RepositoryFactory";
import { ProjectRepositoryFactoryContext } from "../../domain/project/repositories/factory/ProjectRepositoryFactoryContext";
import { ProjectRepository } from "../../domain/project/repositories/project.repository";
import { MemberRepositoryFactoryContext } from "../../domain/member/repositories/factory/MemberRepositoryFactoryContext";
import { MemberRepository } from "../../domain/member/repositories/member.repository";
import { UpdateProjectImageUseCase } from "./usecases/updateProjectImage.usecase";
import { RemoveProjectProfileImageUseCase } from "./usecases/removeProjectProfileImage.usecase";

/**
 * Interface for project management use cases
 */
export interface ProjectUseCases {
  updateProjectImageUseCase: UpdateProjectImageUseCase;
  removeProjectProfileImageUseCase: RemoveProjectProfileImageUseCase;

  // Common repositories for advanced use cases
  projectRepository: ProjectRepository;
}

/**
 * Provider for project management use cases
 * Handles project updates, image processing, and project-related operations
 */
export class ProjectUseCaseProvider {
  constructor(
    private readonly projectRepositoryFactory: RepositoryFactory<ProjectRepository, ProjectRepositoryFactoryContext>,
    private readonly memberRepositoryFactory: RepositoryFactory<MemberRepository, MemberRepositoryFactoryContext>
  ) { }

  /**
   * Creates project management use cases
   * @param context - Optional context for repository creation
   * @return ProjectUseCases instance
   */
  create(context?: ProjectRepositoryFactoryContext): ProjectUseCases {
    const projectRepository = this.projectRepositoryFactory.create(context);

    return {
      updateProjectImageUseCase: new UpdateProjectImageUseCase(
        projectRepository
      ),
      removeProjectProfileImageUseCase: new RemoveProjectProfileImageUseCase(
        projectRepository,
        this.memberRepositoryFactory.create({ projectId: "" }) // Will be set properly when called
      ),

      // Common repositories
      projectRepository
    };
  }

  /**
   * Creates project management use cases for a specific project
   * @param projectId - The project ID for member repository context
   * @param context - Optional context for repository creation
   * @return ProjectUseCases instance
   */
  createForProject(projectId: string, context?: ProjectRepositoryFactoryContext): ProjectUseCases {
    const projectRepository = this.projectRepositoryFactory.create(context);
    const memberRepository = this.memberRepositoryFactory.create({ projectId });

    return {
      updateProjectImageUseCase: new UpdateProjectImageUseCase(
        projectRepository
      ),
      removeProjectProfileImageUseCase: new RemoveProjectProfileImageUseCase(
        projectRepository,
        memberRepository
      ),
      // Common repositories
      projectRepository
    };
  }
}
