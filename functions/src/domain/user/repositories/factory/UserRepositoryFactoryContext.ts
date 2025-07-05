import { RepositoryFactoryContext } from '../../../shared/RepositoryFactory';

/**
 * Context for user-related repository creation
 */
export interface UserRepositoryFactoryContext extends RepositoryFactoryContext {
  userId?: string;
}