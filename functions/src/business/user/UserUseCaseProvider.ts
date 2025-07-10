import {RepositoryFactory} from "../../domain/shared/RepositoryFactory";
import {UserRepositoryFactoryContext} from "../../domain/user/repositories/factory/UserRepositoryFactoryContext";
import {UserRepository} from "../../domain/user/repositories/user.repository";
import {UpdateUserProfileUseCase} from "./usecases/updateUserProfile.usecase";
import {RemoveUserProfileImageUseCase} from "./usecases/removeUserProfileImage.usecase";
import {FirebaseStorageService} from "../../infrastructure/storage/firebase-storage.service";

/**
 * Interface for user management use cases
 */
export interface UserUseCases {
  updateUserProfileUseCase: UpdateUserProfileUseCase;
  removeUserProfileImageUseCase: RemoveUserProfileImageUseCase;
  // updateUserImageUseCase removed - now using fixed path system

  // Common repositories for advanced use cases
  userRepository: UserRepository;
}

/**
 * Provider for user management use cases
 * Handles user profile updates, image processing, and user-related operations
 */
export class UserUseCaseProvider {
  constructor(
    private readonly userRepositoryFactory: RepositoryFactory<UserRepository, UserRepositoryFactoryContext>,
    private readonly storageService: FirebaseStorageService
  ) {}

  create(context?: UserRepositoryFactoryContext): UserUseCases {
    const userRepository = this.userRepositoryFactory.create(context);

    return {
      updateUserProfileUseCase: new UpdateUserProfileUseCase(
        userRepository
      ),
      removeUserProfileImageUseCase: new RemoveUserProfileImageUseCase(
        userRepository,
        this.storageService
      ),

      // Common repositories
      userRepository,
    };
  }
}
