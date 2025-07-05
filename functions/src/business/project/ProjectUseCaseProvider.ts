import { RepositoryFactory } from '../../domain/shared/RepositoryFactory';
import { ProjectRepositoryFactoryContext } from '../../domain/project/repositories/factory/ProjectRepositoryFactoryContext';
import { ProjectRepository } from '../../domain/project/repositories/project.repository';
import { ImageProcessingService } from '../../core/services/imageProcessing.service';
import { UpdateProjectImageUseCase } from './usecases/updateProjectImage.usecase';

/**
 * Interface for project management use cases
 */
export interface ProjectUseCases {
  updateProjectImageUseCase: UpdateProjectImageUseCase;
  
  // Common repositories for advanced use cases
  projectRepository: ProjectRepository;
  imageProcessingService: ImageProcessingService;
}

/**
 * Provider for project management use cases
 * Handles project updates, image processing, and project-related operations
 */
export class ProjectUseCaseProvider {
  constructor(
    private readonly projectRepositoryFactory: RepositoryFactory<ProjectRepository, ProjectRepositoryFactoryContext>,
    private readonly imageProcessingService: ImageProcessingService
  ) {}

  /**
   * Creates project management use cases
   * @param context - Optional context for repository creation
   * @returns ProjectUseCases instance
   */
  create(context?: ProjectRepositoryFactoryContext): ProjectUseCases {
    const projectRepository = this.projectRepositoryFactory.create(context);

    return {
      updateProjectImageUseCase: new UpdateProjectImageUseCase(
        this.imageProcessingService,
        projectRepository
      ),

      // Common repositories
      projectRepository,
      imageProcessingService: this.imageProcessingService
    };
  }
}