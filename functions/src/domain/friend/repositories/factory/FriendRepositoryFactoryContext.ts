import {RepositoryFactoryContext} from "../../../shared/RepositoryFactory";

/**
 * Context for friend-related repository creation
 */
export interface FriendRepositoryFactoryContext extends RepositoryFactoryContext {
  userId?: string;
  friendId?: string;
}
