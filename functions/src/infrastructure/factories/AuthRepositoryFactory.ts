/**
 * Auth repository factory implementation
 */

import { RepositoryFactory, AuthRepository, AuthRepositoryFactoryContext } from '../../domain/repositories';
import { AuthRepositoryImpl } from '../repositories/AuthRepositoryImpl';

export class AuthRepositoryFactory implements RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository> {
  create(input: AuthRepositoryFactoryContext): AuthRepository {
    return new AuthRepositoryImpl();
  }
}