import {RepositoryFactory} from "../../domain/shared/RepositoryFactory";
import {UserRepositoryFactoryContext} from "../../domain/user/repositories/factory/UserRepositoryFactoryContext";
import {UserRepository} from "../../domain/user/repositories/user.repository";
import {UpdateUserProfileUseCase} from "./usecases/updateUserProfile.usecase";
import {UpdateUserImageUseCase} from "./usecases/updateUserImage.usecase";

/**
 * Interface for user management use cases
 */
export interface UserUseCases {
  updateUserProfileUseCase: UpdateUserProfileUseCase;
  updateUserImageUseCase: UpdateUserImageUseCase;

  // Common repositories for advanced use cases
  userRepository: UserRepository;
}

/**
 * Provider for user management use cases
 * Handles user profile updates, image processing, and user-related operations
 */
export class UserUseCaseProvider {
  constructor(
    private readonly userRepositoryFactory: RepositoryFactory<UserRepository, UserRepositoryFactoryContext>
  ) {}

  create(context?: UserRepositoryFactoryContext): UserUseCases {
    const userRepository = this.userRepositoryFactory.create(context);

    return {
      updateUserProfileUseCase: new UpdateUserProfileUseCase(
        userRepository
      ),
      
      updateUserImageUseCase: new UpdateUserImageUseCase(
        userRepository
      ),

      // Common repositories
      userRepository,
    };
  }
}
