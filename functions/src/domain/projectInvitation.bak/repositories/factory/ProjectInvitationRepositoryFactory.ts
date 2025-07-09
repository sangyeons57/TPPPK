import { ProjectInvitationRepository } from '../projectInvitation.repository';
import { ProjectInvitationRepositoryFactoryContext } from './ProjectInvitationRepositoryFactoryContext';

/**
 * ProjectInvitationRepository Factory 인터페이스
 */
export interface ProjectInvitationRepositoryFactory {
  /**
   * ProjectInvitationRepository 인스턴스를 생성합니다.
   * 
   * @param context Factory 컨텍스트
   * @returns ProjectInvitationRepository 인스턴스
   */
  create(context: ProjectInvitationRepositoryFactoryContext): ProjectInvitationRepository;
}