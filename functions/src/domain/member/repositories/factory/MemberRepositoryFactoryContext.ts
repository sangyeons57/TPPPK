import { RepositoryFactoryContext } from '../../../shared/RepositoryFactory';

/**
 * Context for member-related repository creation
 */
export interface MemberRepositoryFactoryContext extends RepositoryFactoryContext {
  projectId: string;
}