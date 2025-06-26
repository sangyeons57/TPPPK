/**
 * Functions repository factory context
 * Provides specific context for creating functions repositories
 */

import { RepositoryFactoryContext } from '../RepositoryFactoryContext';

export interface FunctionsRepositoryFactoryContext extends RepositoryFactoryContext {
  readonly functionsConfig?: {
    readonly timeout?: number;
    readonly region?: string;
    readonly maxRetries?: number;
    readonly enableLogging?: boolean;
  };
}