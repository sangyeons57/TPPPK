import { RepositoryFactory } from '../../domain/shared/RepositoryFactory';
import { FriendRepositoryFactoryContext } from '../../domain/friend/repositories/factory/FriendRepositoryFactoryContext';
import { UserRepositoryFactoryContext } from '../../domain/user/repositories/factory/UserRepositoryFactoryContext';
import { FriendRepository } from '../../domain/friend/repositories/friend.repository';
import { UserProfileRepository } from '../../domain/user/repositories/userProfile.repository';
import { SendFriendRequestUseCase } from './usecases/sendFriendRequest.usecase';
import { AcceptFriendRequestUseCase } from './usecases/acceptFriendRequest.usecase';
import { RejectFriendRequestUseCase } from './usecases/rejectFriendRequest.usecase';
import { RemoveFriendUseCase } from './usecases/removeFriend.usecase';
import { GetFriendsUseCase } from './usecases/getFriends.usecase';
import { GetFriendRequestsUseCase } from './usecases/getFriendRequests.usecase';

/**
 * Interface for friend management use cases
 */
export interface FriendUseCases {
  sendFriendRequestUseCase: SendFriendRequestUseCase;
  acceptFriendRequestUseCase: AcceptFriendRequestUseCase;
  rejectFriendRequestUseCase: RejectFriendRequestUseCase;
  removeFriendUseCase: RemoveFriendUseCase;
  getFriendsUseCase: GetFriendsUseCase;
  getFriendRequestsUseCase: GetFriendRequestsUseCase;
  
  // Common repositories for advanced use cases
  friendRepository: FriendRepository;
  userProfileRepository: UserProfileRepository;
}

/**
 * Provider for friend management use cases
 * Handles friend requests, acceptances, rejections, and friend list management
 */
export class FriendUseCaseProvider {
  constructor(
    private readonly friendRepositoryFactory: RepositoryFactory<FriendRepository, FriendRepositoryFactoryContext>,
    private readonly userRepositoryFactory: RepositoryFactory<UserProfileRepository, UserRepositoryFactoryContext>
  ) {}

  /**
   * Creates friend management use cases
   * @param context - Optional context for repository creation
   * @returns FriendUseCases instance
   */
  create(context?: FriendRepositoryFactoryContext): FriendUseCases {
    const friendRepository = this.friendRepositoryFactory.create(context);
    const userProfileRepository = this.userRepositoryFactory.create();

    return {
      sendFriendRequestUseCase: new SendFriendRequestUseCase(
        friendRepository,
        userProfileRepository
      ),
      
      acceptFriendRequestUseCase: new AcceptFriendRequestUseCase(
        friendRepository,
        userProfileRepository
      ),
      
      rejectFriendRequestUseCase: new RejectFriendRequestUseCase(
        friendRepository,
        userProfileRepository
      ),
      
      removeFriendUseCase: new RemoveFriendUseCase(
        friendRepository,
        userProfileRepository
      ),
      
      getFriendsUseCase: new GetFriendsUseCase(
        friendRepository,
        userProfileRepository
      ),
      
      getFriendRequestsUseCase: new GetFriendRequestsUseCase(
        friendRepository,
        userProfileRepository
      ),

      // Common repositories
      friendRepository,
      userProfileRepository
    };
  }
}