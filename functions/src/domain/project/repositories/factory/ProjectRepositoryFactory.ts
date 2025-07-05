import { RepositoryFactory } from '../../../shared/RepositoryFactory';
import { ProjectRepositoryFactoryContext } from './ProjectRepositoryFactoryContext';
import { ProjectRepository } from '../project.repository';
import { FirestoreProjectDataSource } from '../../../../infrastructure/datasources/firestore/project.datasource';

/**
 * Factory for creating project repositories
 */
export class ProjectRepositoryFactory implements RepositoryFactory<ProjectRepository, ProjectRepositoryFactoryContext> {
  /**
   * Creates a project repository instance
   * @param context - Optional context for project repository creation
   * @returns ProjectRepository instance
   */
  create(context?: ProjectRepositoryFactoryContext): ProjectRepository {
    return new FirestoreProjectDataSource();
  }
}