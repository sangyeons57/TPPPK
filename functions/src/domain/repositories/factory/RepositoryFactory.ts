/**
 * Repository factory interface
 * Provides a contract for creating repository instances with context
 */

import { Repository } from '../Repository';
import { RepositoryFactoryContext } from './RepositoryFactoryContext';

export interface RepositoryFactory<TInput extends RepositoryFactoryContext, TOutput extends Repository> {
  /**
   * Create a repository instance with the provided context
   * @param input Factory context containing configuration and dependencies
   * @returns Repository instance
   */
  create(input: TInput): TOutput;
}