import { RepositoryFactory } from '../../../shared/RepositoryFactory';
import { SessionRepositoryFactoryContext } from './SessionRepositoryFactoryContext';
import { SessionRepository } from '../session.repository';
import { FirestoreSessionDataSource } from '../../../../infrastructure/datasources/firestore/session.datasource';

/**
 * Factory for creating session repositories
 */
export class SessionRepositoryFactory implements RepositoryFactory<SessionRepository, SessionRepositoryFactoryContext> {
  /**
   * Creates a session repository instance
   * @param context - Optional context for session repository creation
   * @returns SessionRepository instance
   */
  create(context?: SessionRepositoryFactoryContext): SessionRepository {
    return new FirestoreSessionDataSource();
  }
}