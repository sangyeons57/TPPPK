import { RepositoryFactory } from '../../../shared/RepositoryFactory';
import { UserRepositoryFactoryContext } from './UserRepositoryFactoryContext';
import { UserRepository } from '../user.repository';
import { FirestoreUserDataSource } from '../../../../infrastructure/datasources/firestore/user.datasource';

/**
 * Factory for creating user repositories
 */
export class UserRepositoryFactory implements RepositoryFactory<UserRepository, UserRepositoryFactoryContext> {
  /**
   * Creates a user repository instance
   * @param context - Optional context for user repository creation
   * @returns UserRepository instance
   */
  create(context?: UserRepositoryFactoryContext): UserRepository {
    return new FirestoreUserDataSource();
  }
}