import { RepositoryFactory } from '../../../shared/RepositoryFactory';
import { FriendRepositoryFactoryContext } from './FriendRepositoryFactoryContext';
import { FriendRepository } from '../friend.repository';
import { FirestoreFriendDataSource } from '../../../../infrastructure/datasources/firestore/friend.datasource';

/**
 * Factory for creating friend repositories
 */
export class FriendRepositoryFactory implements RepositoryFactory<FriendRepository, FriendRepositoryFactoryContext> {
  /**
   * Creates a friend repository instance
   * @param context - Optional context for friend repository creation
   * @returns FriendRepository instance
   */
  create(context?: FriendRepositoryFactoryContext): FriendRepository {
    return new FirestoreFriendDataSource();
  }
}