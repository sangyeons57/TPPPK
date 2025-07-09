import { ProjectInvitationEntity } from '../entities/projectInvitation.entity';
import { ProjectInvitationStatus } from '../entities/projectInvitation.entity';

/**
 * 프로젝트 초대 관련 데이터 처리를 위한 Repository 인터페이스
 */
export interface ProjectInvitationRepository {
  /**
   * 프로젝트 초대를 생성합니다.
   * 
   * @param invitation 생성할 초대 엔티티
   * @returns 생성된 초대 엔티티
   */
  create(invitation: ProjectInvitationEntity): Promise<ProjectInvitationEntity>;

  /**
   * 프로젝트 초대를 업데이트합니다.
   * 
   * @param invitation 업데이트할 초대 엔티티
   * @returns 업데이트된 초대 엔티티
   */
  update(invitation: ProjectInvitationEntity): Promise<ProjectInvitationEntity>;

  /**
   * 초대 ID로 프로젝트 초대를 조회합니다.
   * 
   * @param invitationId 초대 ID
   * @returns 조회된 초대 엔티티 또는 null
   */
  findById(invitationId: string): Promise<ProjectInvitationEntity | null>;

  /**
   * 사용자가 받은 초대 목록을 조회합니다.
   * 
   * @param inviteeId 초대받은 사용자 ID
   * @param status 조회할 상태 (선택사항)
   * @returns 초대 목록
   */
  findByInviteeId(inviteeId: string, status?: ProjectInvitationStatus): Promise<ProjectInvitationEntity[]>;

  /**
   * 사용자가 보낸 초대 목록을 조회합니다.
   * 
   * @param inviterId 초대한 사용자 ID
   * @param projectId 프로젝트 ID (선택사항)
   * @param status 조회할 상태 (선택사항)
   * @returns 초대 목록
   */
  findByInviterId(inviterId: string, projectId?: string, status?: ProjectInvitationStatus): Promise<ProjectInvitationEntity[]>;

  /**
   * 특정 프로젝트의 초대 목록을 조회합니다.
   * 
   * @param projectId 프로젝트 ID
   * @param status 조회할 상태 (선택사항)
   * @returns 초대 목록
   */
  findByProjectId(projectId: string, status?: ProjectInvitationStatus): Promise<ProjectInvitationEntity[]>;

  /**
   * 중복 초대 확인 (같은 프로젝트에 같은 사용자가 이미 초대받았는지)
   * 
   * @param projectId 프로젝트 ID
   * @param inviteeId 초대받을 사용자 ID
   * @returns 중복 초대 여부
   */
  hasPendingInvitation(projectId: string, inviteeId: string): Promise<boolean>;

  /**
   * 프로젝트 초대를 삭제합니다.
   * 
   * @param invitationId 삭제할 초대 ID
   */
  delete(invitationId: string): Promise<void>;
}