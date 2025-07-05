import { RepositoryFactoryContext } from '../../../shared/RepositoryFactory';

/**
 * Context for project-related repository creation
 */
export interface ProjectRepositoryFactoryContext extends RepositoryFactoryContext {
  projectId?: string;
  userId?: string;
}