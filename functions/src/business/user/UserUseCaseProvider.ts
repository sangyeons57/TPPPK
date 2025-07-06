import {RepositoryFactory} from "../../domain/shared/RepositoryFactory";
import {UserRepositoryFactoryContext} from "../../domain/user/repositories/factory/UserRepositoryFactoryContext";
import {UserRepository} from "../../domain/user/repositories/user.repository";
import {ImageProcessingService} from "../../core/services/imageProcessing.service";
import {UpdateUserProfileUseCase} from "./usecases/updateUserProfile.usecase";
import {ProcessUserImageUseCase} from "./usecases/processUserImage.usecase";

/**
 * Interface for user management use cases
 */
export interface UserUseCases {
  updateUserProfileUseCase: UpdateUserProfileUseCase;
  processUserImageUseCase: ProcessUserImageUseCase;

  // Common repositories for advanced use cases
  userRepository: UserRepository;
  imageProcessingService: ImageProcessingService;
}

/**
 * Provider for user management use cases
 * Handles user profile updates, image processing, and user-related operations
 */
export class UserUseCaseProvider {
  constructor(
    private readonly userRepositoryFactory: RepositoryFactory<UserRepository, UserRepositoryFactoryContext>,
    private readonly imageProcessingService: ImageProcessingService
  ) {}

  create(context?: UserRepositoryFactoryContext): UserUseCases {
    const userRepository = this.userRepositoryFactory.create(context);

    return {
      updateUserProfileUseCase: new UpdateUserProfileUseCase(
        userRepository
      ),

      processUserImageUseCase: new ProcessUserImageUseCase(
        this.imageProcessingService,
        userRepository
      ),

      // Common repositories
      userRepository,
      imageProcessingService: this.imageProcessingService,
    };
  }
}
