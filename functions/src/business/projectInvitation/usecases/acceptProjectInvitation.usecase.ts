import {ProjectInvitationEntity, ProjectInvitationStatus} from "../../../domain/projectInvitation/entities/projectInvitation.entity";
import {ProjectInvitationRepository} from "../../../domain/projectInvitation/repositories/projectInvitation.repository";
import {ProjectInvitationRepositoryFactory} from "../../../domain/projectInvitation/repositories/factory/ProjectInvitationRepositoryFactory";
import {ProjectInvitationRepositoryFactoryContext} from "../../../domain/projectInvitation/repositories/factory/ProjectInvitationRepositoryFactoryContext";

/**
 * 프로젝트 초대 수락 UseCase
 */
export class AcceptProjectInvitationUseCase {
  constructor(
    private readonly repositoryFactory: ProjectInvitationRepositoryFactory
  ) {}

  /**
   * 프로젝트 초대를 수락합니다.
   *
   * @param userId 사용자 ID
   * @param invitationId 초대 ID
   * @returns 수락된 초대 엔티티
   */
  async execute(
    userId: string,
    invitationId: string
  ): Promise<ProjectInvitationEntity> {
    // Repository 생성
    const context: ProjectInvitationRepositoryFactoryContext = {
      userId: userId,
    };
    const repository: ProjectInvitationRepository = this.repositoryFactory.create(context);

    // 초대 조회
    const invitation = await repository.findById(invitationId);
    if (!invitation) {
      throw new Error("초대를 찾을 수 없습니다.");
    }

    // 초대받은 사용자가 맞는지 확인
    if (invitation.inviteeId !== userId) {
      throw new Error("이 초대를 수락할 권한이 없습니다.");
    }

    // 초대 상태 확인
    if (invitation.status !== ProjectInvitationStatus.PENDING) {
      throw new Error("이미 처리된 초대입니다.");
    }

    // 만료 확인
    if (invitation.expiresAt && invitation.expiresAt < new Date()) {
      throw new Error("만료된 초대입니다.");
    }

    // 초대 수락
    invitation.accept();
    
    // 초대 업데이트
    return await repository.update(invitation);
  }
}