/**
 * Authentication registration use case provider
 * Groups user registration and verification use cases
 */

import { AuthRepository, UserRepository, RepositoryFactory } from '../../repositories';
import { AuthRepositoryFactoryContext, UserRepositoryFactoryContext } from '../../repositories';
import { SignUpUseCase } from '../../usecases/auth/registration/SignUpUseCase';

export interface AuthRegistrationUseCases {
  readonly signUpUseCase: SignUpUseCase;
  readonly authRepository: AuthRepository;
  readonly userRepository: UserRepository;
}

export class AuthRegistrationUseCaseProvider {
  constructor(
    private readonly authRepositoryFactory: RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>,
    private readonly userRepositoryFactory: RepositoryFactory<UserRepositoryFactoryContext, UserRepository>
  ) {}

  create(options: {
    authContext?: AuthRepositoryFactoryContext;
    userContext?: UserRepositoryFactoryContext;
  } = {}): AuthRegistrationUseCases {
    const authRepository = this.authRepositoryFactory.create(
      options.authContext || {}
    );

    const userRepository = this.userRepositoryFactory.create(
      options.userContext || { collectionPath: 'users' }
    );

    return {
      signUpUseCase: new SignUpUseCase(authRepository, userRepository),
      authRepository,
      userRepository,
    };
  }
}