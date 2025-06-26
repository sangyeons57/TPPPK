/**
 * Repository factory context base interface
 * Provides context for repository creation
 */

import { RepositoryContext } from '../../../shared/types/common';

export interface RepositoryFactoryContext {
  readonly repositoryContext?: RepositoryContext;
  readonly metadata?: Record<string, any>;
}