import {ProjectInvitationEntity, ProjectInvitationStatus} from "../../../domain/projectInvitation/entities/projectInvitation.entity";
import {ProjectInvitationRepository} from "../../../domain/projectInvitation/repositories/projectInvitation.repository";
import {ProjectInvitationRepositoryFactory} from "../../../domain/projectInvitation/repositories/factory/ProjectInvitationRepositoryFactory";
import {ProjectInvitationRepositoryFactoryContext} from "../../../domain/projectInvitation/repositories/factory/ProjectInvitationRepositoryFactoryContext";

/**
 * 프로젝트 초대 보내기 UseCase
 */
export class SendProjectInvitationUseCase {
  constructor(
    private readonly repositoryFactory: ProjectInvitationRepositoryFactory
  ) {}

  /**
   * 프로젝트 초대를 보냅니다.
   *
   * @param inviterId 초대하는 사용자 ID
   * @param projectId 프로젝트 ID
   * @param inviteeId 초대받을 사용자 ID
   * @param message 초대 메시지 (선택사항)
   * @param expiresInHours 만료 시간 (시간 단위, 기본 72시간)
   * @returns 생성된 초대 엔티티
   */
  async execute(
    inviterId: string,
    projectId: string,
    inviteeId: string,
    message?: string,
    expiresInHours: number = 72
  ): Promise<ProjectInvitationEntity> {
    // 자기 자신에게 초대 보내기 방지
    if (inviterId === inviteeId) {
      throw new Error("자기 자신에게 초대를 보낼 수 없습니다.");
    }

    // Repository 생성
    const context: ProjectInvitationRepositoryFactoryContext = {
      userId: inviterId,
      projectId: projectId,
    };
    const repository: ProjectInvitationRepository = this.repositoryFactory.create(context);

    // 만료 시간 계산
    const expiresAt = new Date();
    expiresAt.setHours(expiresAt.getHours() + expiresInHours);

    // 초대 엔티티 생성
    const invitation = ProjectInvitationEntity.create(
      inviterId,
      projectId,
      inviteeId,
      message,
      expiresAt
    );

    // 초대 저장
    return await repository.create(invitation);
  }
}