/**
 * Auth repository factory context
 * Provides specific context for creating auth repositories
 */

import { RepositoryFactoryContext } from '../RepositoryFactoryContext';

export interface AuthRepositoryFactoryContext extends RepositoryFactoryContext {
  readonly authConfig?: {
    readonly tokenExpiration?: number;
    readonly refreshTokenExpiration?: number;
    readonly maxLoginAttempts?: number;
    readonly lockoutDuration?: number;
  };
}