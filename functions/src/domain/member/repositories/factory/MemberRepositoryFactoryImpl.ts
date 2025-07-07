import { RepositoryFactory } from '../../../shared/RepositoryFactory';
import { MemberRepositoryFactoryContext } from './MemberRepositoryFactoryContext';
import { MemberRepository } from '../member.repository';
import { MemberRepositoryImpl } from '../../../../infrastructure/repositories/member.repository.impl';
import { FirestoreMemberDataSource } from '../../../../infrastructure/datasources/firestore/member.datasource';
import { firestore } from 'firebase-admin';

/**
 * Factory for creating member repositories
 */
export class MemberRepositoryFactoryImpl implements RepositoryFactory<MemberRepository, MemberRepositoryFactoryContext> {
  /**
   * Creates a member repository instance
   * @param context - Context containing projectId for member repository creation
   * @returns MemberRepository instance
   */
  create(context?: MemberRepositoryFactoryContext): MemberRepository {
    if (!context || !context.projectId) {
      throw new Error('MemberRepositoryFactoryContext with projectId is required');
    }

    const db = firestore();
    const dataSource = new FirestoreMemberDataSource(db);
    return new MemberRepositoryImpl(dataSource, context.projectId);
  }
}