import { RepositoryFactory } from '../../domain/shared/RepositoryFactory';
import { ProjectRepositoryFactoryContext } from '../../domain/project/repositories/factory/ProjectRepositoryFactoryContext';
import { ImageRepositoryFactoryContext } from '../../domain/image/repositories/factory/ImageRepositoryFactoryContext';
import { ProjectRepository } from '../../domain/project/repositories/project.repository';
import { ImageRepository } from '../../domain/image/repositories/image.repository';
import { UpdateProjectImageUseCase } from './usecases/updateProjectImage.usecase';

/**
 * Interface for project management use cases
 */
export interface ProjectUseCases {
  updateProjectImageUseCase: UpdateProjectImageUseCase;
  
  // Common repositories for advanced use cases
  projectRepository: ProjectRepository;
  imageRepository: ImageRepository;
}

/**
 * Provider for project management use cases
 * Handles project updates, image processing, and project-related operations
 */
export class ProjectUseCaseProvider {
  constructor(
    private readonly projectRepositoryFactory: RepositoryFactory<ProjectRepository, ProjectRepositoryFactoryContext>,
    private readonly imageRepositoryFactory: RepositoryFactory<ImageRepository, ImageRepositoryFactoryContext>
  ) {}

  /**
   * Creates project management use cases
   * @param context - Optional context for repository creation
   * @returns ProjectUseCases instance
   */
  create(context?: ProjectRepositoryFactoryContext): ProjectUseCases {
    const projectRepository = this.projectRepositoryFactory.create(context);
    const imageRepository = this.imageRepositoryFactory.create();

    return {
      updateProjectImageUseCase: new UpdateProjectImageUseCase(
        projectRepository,
        imageRepository
      ),

      // Common repositories
      projectRepository,
      imageRepository
    };
  }
}