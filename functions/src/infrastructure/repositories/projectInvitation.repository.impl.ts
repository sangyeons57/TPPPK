import {ProjectInvitationEntity, ProjectInvitationStatus} from "../../domain/projectInvitation/entities/projectInvitation.entity";
import {ProjectInvitationRepository} from "../../domain/projectInvitation/repositories/projectInvitation.repository";
import {ProjectInvitationDataSource} from "../datasources/interfaces/projectInvitation.datasource";

/**
 * 프로젝트 초대 Repository 구현체
 */
export class ProjectInvitationRepositoryImpl implements ProjectInvitationRepository {
  constructor(private readonly dataSource: ProjectInvitationDataSource) {}

  /**
   * 프로젝트 초대를 생성합니다.
   */
  async create(invitation: ProjectInvitationEntity): Promise<ProjectInvitationEntity> {
    // 중복 초대 확인
    const hasPending = await this.dataSource.hasPendingInvitation(
      invitation.projectId,
      invitation.inviteeId
    );
    
    if (hasPending) {
      throw new Error("이미 해당 프로젝트에 대한 대기 중인 초대가 있습니다.");
    }
    
    return await this.dataSource.create(invitation);
  }

  /**
   * 프로젝트 초대를 업데이트합니다.
   */
  async update(invitation: ProjectInvitationEntity): Promise<ProjectInvitationEntity> {
    return await this.dataSource.update(invitation);
  }

  /**
   * 초대 ID로 프로젝트 초대를 조회합니다.
   */
  async findById(invitationId: string): Promise<ProjectInvitationEntity | null> {
    return await this.dataSource.findById(invitationId);
  }

  /**
   * 사용자가 받은 초대 목록을 조회합니다.
   */
  async findByInviteeId(inviteeId: string, status?: ProjectInvitationStatus): Promise<ProjectInvitationEntity[]> {
    return await this.dataSource.findByInviteeId(inviteeId, status);
  }

  /**
   * 사용자가 보낸 초대 목록을 조회합니다.
   */
  async findByInviterId(inviterId: string, projectId?: string, status?: ProjectInvitationStatus): Promise<ProjectInvitationEntity[]> {
    return await this.dataSource.findByInviterId(inviterId, projectId, status);
  }

  /**
   * 특정 프로젝트의 초대 목록을 조회합니다.
   */
  async findByProjectId(projectId: string, status?: ProjectInvitationStatus): Promise<ProjectInvitationEntity[]> {
    return await this.dataSource.findByProjectId(projectId, status);
  }

  /**
   * 중복 초대 확인 (같은 프로젝트에 같은 사용자가 이미 초대받았는지)
   */
  async hasPendingInvitation(projectId: string, inviteeId: string): Promise<boolean> {
    return await this.dataSource.hasPendingInvitation(projectId, inviteeId);
  }

  /**
   * 프로젝트 초대를 삭제합니다.
   */
  async delete(invitationId: string): Promise<void> {
    await this.dataSource.delete(invitationId);
  }
}