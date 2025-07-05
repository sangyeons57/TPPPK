import { RepositoryFactoryContext } from '../../../shared/RepositoryFactory';

/**
 * Context for session-related repository creation
 */
export interface SessionRepositoryFactoryContext extends RepositoryFactoryContext {
  userId?: string;
  sessionId?: string;
}