/**
 * User repository factory context
 * Provides specific context for creating user repositories
 */

import { RepositoryFactoryContext } from '../RepositoryFactoryContext';

export interface UserRepositoryFactoryContext extends RepositoryFactoryContext {
  readonly collectionPath: string;
  readonly userConfig?: {
    readonly enableCaching?: boolean;
    readonly cacheExpiration?: number;
    readonly maxQueryLimit?: number;
  };
}