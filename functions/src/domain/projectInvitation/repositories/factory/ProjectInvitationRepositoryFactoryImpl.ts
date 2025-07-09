import { ProjectInvitationRepository } from '../projectInvitation.repository';
import { ProjectInvitationRepositoryFactory } from './ProjectInvitationRepositoryFactory';
import { ProjectInvitationRepositoryFactoryContext } from './ProjectInvitationRepositoryFactoryContext';
import { ProjectInvitationRepositoryImpl } from '../../../infrastructure/repositories/projectInvitation.repository.impl';
import { ProjectInvitationDataSource } from '../../../infrastructure/datasources/interfaces/projectInvitation.datasource';
import { ProjectInvitationFirestoreDataSource } from '../../../infrastructure/datasources/firestore/projectInvitation.datasource';
import { getFirestore } from 'firebase-admin/firestore';

/**
 * ProjectInvitationRepository Factory 구현체
 */
export class ProjectInvitationRepositoryFactoryImpl implements ProjectInvitationRepositoryFactory {
  /**
   * ProjectInvitationRepository 인스턴스를 생성합니다.
   * 
   * @param context Factory 컨텍스트
   * @returns ProjectInvitationRepository 인스턴스
   */
  create(context: ProjectInvitationRepositoryFactoryContext): ProjectInvitationRepository {
    // Firestore 데이터소스 생성
    const firestoreDataSource: ProjectInvitationDataSource = new ProjectInvitationFirestoreDataSource(
      getFirestore()
    );

    // Repository 구현체 생성
    return new ProjectInvitationRepositoryImpl(firestoreDataSource);
  }
}