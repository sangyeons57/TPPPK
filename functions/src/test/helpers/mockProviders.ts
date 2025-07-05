import { 
  AuthSessionUseCaseProvider, 
  AuthSessionUseCases,
  FriendUseCaseProvider,
  FriendUseCases,
  UserUseCaseProvider,
  UserUseCases,
  ProjectUseCaseProvider,
  ProjectUseCases
} from '../../business';
import { RepositoryFactory } from '../../domain/shared/RepositoryFactory';
import { SessionRepository } from '../../domain/auth/repositories/session.repository';
import { FriendRepository } from '../../domain/friend/repositories/friend.repository';
import { UserProfileRepository } from '../../domain/user/repositories/userProfile.repository';
import { ProjectRepository } from '../../domain/project/repositories/project.repository';
import { ImageRepository } from '../../domain/image/repositories/image.repository';

/**
 * Mock repository factory for testing
 */
export class MockRepositoryFactory<T> implements RepositoryFactory<T, any> {
  constructor(private mockInstance: T) {}

  create(): T {
    return this.mockInstance;
  }
}

/**
 * Mock provider helper for testing
 */
export class MockProviderHelper {
  
  /**
   * Creates a mock AuthSessionUseCaseProvider
   */
  static createMockAuthSessionProvider(
    mockSessionRepository?: SessionRepository,
    mockUserRepository?: UserProfileRepository
  ): AuthSessionUseCaseProvider {
    const sessionRepoFactory = new MockRepositoryFactory(
      mockSessionRepository || ({} as SessionRepository)
    );
    const userRepoFactory = new MockRepositoryFactory(
      mockUserRepository || ({} as UserProfileRepository)
    );

    return new AuthSessionUseCaseProvider(sessionRepoFactory, userRepoFactory);
  }

  /**
   * Creates a mock FriendUseCaseProvider
   */
  static createMockFriendProvider(
    mockFriendRepository?: FriendRepository,
    mockUserRepository?: UserProfileRepository
  ): FriendUseCaseProvider {
    const friendRepoFactory = new MockRepositoryFactory(
      mockFriendRepository || ({} as FriendRepository)
    );
    const userRepoFactory = new MockRepositoryFactory(
      mockUserRepository || ({} as UserProfileRepository)
    );

    return new FriendUseCaseProvider(friendRepoFactory, userRepoFactory);
  }

  /**
   * Creates a mock UserUseCaseProvider
   */
  static createMockUserProvider(
    mockUserRepository?: UserProfileRepository,
    mockImageRepository?: ImageRepository
  ): UserUseCaseProvider {
    const userRepoFactory = new MockRepositoryFactory(
      mockUserRepository || ({} as UserProfileRepository)
    );
    const imageRepoFactory = new MockRepositoryFactory(
      mockImageRepository || ({} as ImageRepository)
    );

    return new UserUseCaseProvider(userRepoFactory, imageRepoFactory);
  }

  /**
   * Creates a mock ProjectUseCaseProvider
   */
  static createMockProjectProvider(
    mockProjectRepository?: ProjectRepository,
    mockImageRepository?: ImageRepository
  ): ProjectUseCaseProvider {
    const projectRepoFactory = new MockRepositoryFactory(
      mockProjectRepository || ({} as ProjectRepository)
    );
    const imageRepoFactory = new MockRepositoryFactory(
      mockImageRepository || ({} as ImageRepository)
    );

    return new ProjectUseCaseProvider(projectRepoFactory, imageRepoFactory);
  }

  /**
   * Creates mock use cases for auth domain
   */
  static createMockAuthUseCases(
    partial?: Partial<AuthSessionUseCases>
  ): AuthSessionUseCases {
    return {
      loginUserUseCase: {} as any,
      registerUserUseCase: {} as any,
      sessionRepository: {} as any,
      userProfileRepository: {} as any,
      ...partial
    };
  }

  /**
   * Creates mock use cases for friend domain
   */
  static createMockFriendUseCases(
    partial?: Partial<FriendUseCases>
  ): FriendUseCases {
    return {
      sendFriendRequestUseCase: {} as any,
      acceptFriendRequestUseCase: {} as any,
      rejectFriendRequestUseCase: {} as any,
      removeFriendUseCase: {} as any,
      getFriendsUseCase: {} as any,
      getFriendRequestsUseCase: {} as any,
      friendRepository: {} as any,
      userProfileRepository: {} as any,
      ...partial
    };
  }

  /**
   * Creates mock use cases for user domain
   */
  static createMockUserUseCases(
    partial?: Partial<UserUseCases>
  ): UserUseCases {
    return {
      updateUserProfileUseCase: {} as any,
      processUserImageUseCase: {} as any,
      userProfileRepository: {} as any,
      imageRepository: {} as any,
      ...partial
    };
  }

  /**
   * Creates mock use cases for project domain
   */
  static createMockProjectUseCases(
    partial?: Partial<ProjectUseCases>
  ): ProjectUseCases {
    return {
      updateProjectImageUseCase: {} as any,
      projectRepository: {} as any,
      imageRepository: {} as any,
      ...partial
    };
  }
}

/**
 * Mock provider container for testing
 */
export class MockProviderContainer {
  private providers: Map<string, any> = new Map();

  register<T>(key: string, provider: T): void {
    this.providers.set(key, provider);
  }

  get<T>(key: string): T {
    const provider = this.providers.get(key);
    if (!provider) {
      throw new Error(`Mock provider with key '${key}' not found`);
    }
    return provider as T;
  }

  has(key: string): boolean {
    return this.providers.has(key);
  }

  clear(): void {
    this.providers.clear();
  }
}