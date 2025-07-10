import { RepositoryFactory } from '../../domain/shared/RepositoryFactory';
import { ProjectRepositoryFactoryContext } from '../../domain/project/repositories/factory/ProjectRepositoryFactoryContext';
import { ProjectRepository } from '../../domain/project/repositories/project.repository';
import { UpdateProjectImageUseCase } from './usecases/updateProjectImage.usecase';
import { RemoveProjectProfileImageUseCase } from './usecases/removeProjectProfileImage.usecase';
import { FirebaseStorageService } from '../../infrastructure/storage/firebase-storage.service';

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
    private readonly storageService: FirebaseStorageService
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
        projectRepository
      ),
      removeProjectProfileImageUseCase: new RemoveProjectProfileImageUseCase(
        projectRepository,
        this.storageService
      ),
      
      // Common repositories
      projectRepository
    };
  }
}