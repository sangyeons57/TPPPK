import { RepositoryFactory } from '../../domain/shared/RepositoryFactory';
import { SessionRepositoryFactoryContext } from '../../domain/auth/repositories/factory/SessionRepositoryFactoryContext';
import { UserRepositoryFactoryContext } from '../../domain/user/repositories/factory/UserRepositoryFactoryContext';
import { SessionRepository } from '../../domain/auth/repositories/session.repository';
import { UserProfileRepository } from '../../domain/user/repositories/userProfile.repository';
import { LoginUserUseCase } from './usecases/loginUser.usecase';
import { RegisterUserUseCase } from './usecases/registerUser.usecase';

/**
 * Interface for authentication session use cases
 */
export interface AuthSessionUseCases {
  loginUserUseCase: LoginUserUseCase;
  registerUserUseCase: RegisterUserUseCase;
  
  // Common repositories for advanced use cases
  sessionRepository: SessionRepository;
  userProfileRepository: UserProfileRepository;
}

/**
 * Provider for authentication session use cases
 * Handles login, registration, and session management functionality
 */
export class AuthSessionUseCaseProvider {
  constructor(
    private readonly sessionRepositoryFactory: RepositoryFactory<SessionRepository, SessionRepositoryFactoryContext>,
    private readonly userRepositoryFactory: RepositoryFactory<UserProfileRepository, UserRepositoryFactoryContext>
  ) {}

  /**
   * Creates authentication session use cases
   * @param context - Optional context for repository creation
   * @returns AuthSessionUseCases instance
   */
  create(context?: SessionRepositoryFactoryContext): AuthSessionUseCases {
    const sessionRepository = this.sessionRepositoryFactory.create(context);
    const userProfileRepository = this.userRepositoryFactory.create();

    return {
      loginUserUseCase: new LoginUserUseCase(
        sessionRepository,
        userProfileRepository
      ),
      
      registerUserUseCase: new RegisterUserUseCase(
        userProfileRepository
      ),

      // Common repositories
      sessionRepository,
      userProfileRepository
    };
  }
}