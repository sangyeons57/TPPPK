import {SendProjectInvitationUseCase} from "./usecases/sendProjectInvitation.usecase";
import {AcceptProjectInvitationUseCase} from "./usecases/acceptProjectInvitation.usecase";
import {RejectProjectInvitationUseCase} from "./usecases/rejectProjectInvitation.usecase";
import {CancelProjectInvitationUseCase} from "./usecases/cancelProjectInvitation.usecase";
import {GetProjectInvitationsUseCase} from "./usecases/getProjectInvitations.usecase";
import {GetReceivedInvitationsUseCase} from "./usecases/getReceivedInvitations.usecase";
import {ProjectInvitationRepositoryFactory} from "../../domain/projectInvitation/repositories/factory/ProjectInvitationRepositoryFactory";
import {ProjectInvitationRepositoryFactoryImpl} from "../../domain/projectInvitation/repositories/factory/ProjectInvitationRepositoryFactoryImpl";

/**
 * 프로젝트 초대 관련 UseCase들을 제공하는 Provider
 */
export class ProjectInvitationUseCaseProvider {
  private readonly repositoryFactory: ProjectInvitationRepositoryFactory;

  constructor() {
    this.repositoryFactory = new ProjectInvitationRepositoryFactoryImpl();
  }

  /**
   * 프로젝트 초대 보내기 UseCase
   */
  getSendProjectInvitationUseCase(): SendProjectInvitationUseCase {
    return new SendProjectInvitationUseCase(this.repositoryFactory);
  }

  /**
   * 프로젝트 초대 수락 UseCase
   */
  getAcceptProjectInvitationUseCase(): AcceptProjectInvitationUseCase {
    return new AcceptProjectInvitationUseCase(this.repositoryFactory);
  }

  /**
   * 프로젝트 초대 거절 UseCase
   */
  getRejectProjectInvitationUseCase(): RejectProjectInvitationUseCase {
    return new RejectProjectInvitationUseCase(this.repositoryFactory);
  }

  /**
   * 프로젝트 초대 취소 UseCase
   */
  getCancelProjectInvitationUseCase(): CancelProjectInvitationUseCase {
    return new CancelProjectInvitationUseCase(this.repositoryFactory);
  }

  /**
   * 프로젝트 초대 목록 조회 UseCase
   */
  getGetProjectInvitationsUseCase(): GetProjectInvitationsUseCase {
    return new GetProjectInvitationsUseCase(this.repositoryFactory);
  }

  /**
   * 받은 초대 목록 조회 UseCase
   */
  getGetReceivedInvitationsUseCase(): GetReceivedInvitationsUseCase {
    return new GetReceivedInvitationsUseCase(this.repositoryFactory);
  }
}