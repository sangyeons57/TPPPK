/**
 * User repository factory implementation
 */

import { RepositoryFactory, UserRepository, UserRepositoryFactoryContext } from '../../domain/repositories';
import { UserRepositoryImpl } from '../repositories/UserRepositoryImpl';

export class UserRepositoryFactory implements RepositoryFactory<UserRepositoryFactoryContext, UserRepository> {
  create(input: UserRepositoryFactoryContext): UserRepository {
    return new UserRepositoryImpl(input.collectionPath);
  }
}