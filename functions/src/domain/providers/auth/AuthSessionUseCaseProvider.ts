/**
 * Authentication session use case provider
 * Groups authentication and session management use cases
 */

import { AuthRepository, UserRepository, RepositoryFactory } from '../../repositories';
import { AuthRepositoryFactoryContext, UserRepositoryFactoryContext } from '../../repositories';
import { LoginUseCase } from '../../usecases/auth/session/LoginUseCase';
import { LogoutUseCase } from '../../usecases/auth/session/LogoutUseCase';
import { CheckSessionUseCase } from '../../usecases/auth/session/CheckSessionUseCase';
import { CheckAuthenticationStatusUseCase } from '../../usecases/auth/session/CheckAuthenticationStatusUseCase';

export interface AuthSessionUseCases {
  readonly loginUseCase: LoginUseCase;
  readonly logoutUseCase: LogoutUseCase;
  readonly checkSessionUseCase: CheckSessionUseCase;
  readonly checkAuthenticationStatusUseCase: CheckAuthenticationStatusUseCase;
  readonly authRepository: AuthRepository;
  readonly userRepository: UserRepository;
}

export class AuthSessionUseCaseProvider {
  constructor(
    private readonly authRepositoryFactory: RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>,
    private readonly userRepositoryFactory: RepositoryFactory<UserRepositoryFactoryContext, UserRepository>
  ) {}

  create(options: {
    authContext?: AuthRepositoryFactoryContext;
    userContext?: UserRepositoryFactoryContext;
  } = {}): AuthSessionUseCases {
    const authRepository = this.authRepositoryFactory.create(
      options.authContext || {}
    );

    const userRepository = this.userRepositoryFactory.create(
      options.userContext || { collectionPath: 'users' }
    );

    return {
      loginUseCase: new LoginUseCase(authRepository, userRepository),
      logoutUseCase: new LogoutUseCase(authRepository),
      checkSessionUseCase: new CheckSessionUseCase(authRepository),
      checkAuthenticationStatusUseCase: new CheckAuthenticationStatusUseCase(authRepository),
      authRepository,
      userRepository,
    };
  }
}