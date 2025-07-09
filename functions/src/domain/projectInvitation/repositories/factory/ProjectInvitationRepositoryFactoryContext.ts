/**
 * ProjectInvitationRepository Factory 컨텍스트
 */
export interface ProjectInvitationRepositoryFactoryContext {
  /**
   * 사용자 ID (인증된 사용자)
   */
  userId: string;

  /**
   * 프로젝트 ID (선택사항)
   */
  projectId?: string;
}