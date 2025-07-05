import { RepositoryFactory } from '../../domain/shared/RepositoryFactory';
import { UserRepositoryFactoryContext } from '../../domain/user/repositories/factory/UserRepositoryFactoryContext';
import { ImageRepositoryFactoryContext } from '../../domain/image/repositories/factory/ImageRepositoryFactoryContext';
import { UserProfileRepository } from '../../domain/user/repositories/userProfile.repository';
import { ImageRepository } from '../../domain/image/repositories/image.repository';
import { UpdateUserProfileUseCase } from './usecases/updateUserProfile.usecase';
import { ProcessUserImageUseCase } from './usecases/processUserImage.usecase';

/**
 * Interface for user management use cases
 */
export interface UserUseCases {
  updateUserProfileUseCase: UpdateUserProfileUseCase;
  processUserImageUseCase: ProcessUserImageUseCase;
  
  // Common repositories for advanced use cases
  userProfileRepository: UserProfileRepository;
  imageRepository: ImageRepository;
}

/**
 * Provider for user management use cases
 * Handles user profile updates, image processing, and user-related operations
 */
export class UserUseCaseProvider {
  constructor(
    private readonly userRepositoryFactory: RepositoryFactory<UserProfileRepository, UserRepositoryFactoryContext>,
    private readonly imageRepositoryFactory: RepositoryFactory<ImageRepository, ImageRepositoryFactoryContext>
  ) {}

  /**
   * Creates user management use cases
   * @param context - Optional context for repository creation
   * @returns UserUseCases instance
   */
  create(context?: UserRepositoryFactoryContext): UserUseCases {
    const userProfileRepository = this.userRepositoryFactory.create(context);
    const imageRepository = this.imageRepositoryFactory.create();

    return {
      updateUserProfileUseCase: new UpdateUserProfileUseCase(
        userProfileRepository
      ),
      
      processUserImageUseCase: new ProcessUserImageUseCase(
        userProfileRepository,
        imageRepository
      ),

      // Common repositories
      userProfileRepository,
      imageRepository
    };
  }
}