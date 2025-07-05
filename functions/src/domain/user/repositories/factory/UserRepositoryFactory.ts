import { RepositoryFactory } from '../../../shared/RepositoryFactory';
import { UserRepositoryFactoryContext } from './UserRepositoryFactoryContext';
import { UserProfileRepository } from '../userProfile.repository';
import { FirestoreUserProfileDataSource } from '../../../../infrastructure/datasources/firestore/userProfile.datasource';

/**
 * Factory for creating user profile repositories
 */
export class UserRepositoryFactory implements RepositoryFactory<UserProfileRepository, UserRepositoryFactoryContext> {
  /**
   * Creates a user profile repository instance
   * @param context - Optional context for user repository creation
   * @returns UserProfileRepository instance
   */
  create(context?: UserRepositoryFactoryContext): UserProfileRepository {
    return new FirestoreUserProfileDataSource();
  }
}