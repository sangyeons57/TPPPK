import {CustomResult, Result} from "../../../core/types";
import {ValidationError, NotFoundError, InternalError} from "../../../core/errors";
import {ProjectWrapperRepository} from "../../../domain/projectwrapper/repositories/projectwrapper.repository";
import {ProjectRepository} from "../../../domain/project/repositories/project.repository";
import {MemberRepository} from "../../../domain/member/repositories/member.repository";
import {RepositoryFactory} from "../../../domain/shared/RepositoryFactory";
import {MemberRepositoryFactoryContext} from "../../../domain/member/repositories/factory/MemberRepositoryFactoryContext";

export interface SyncProjectWrapperRequest {
  projectId: string;
  updates: {
    name?: string;
    imageUrl?: string;
  };
}

export interface SyncProjectWrapperResponse {
  projectId: string;
  updatedMemberCount: number;
  success: boolean;
  errors?: string[];
}

/**
 * 프로젝트 정보 변경 시 모든 멤버의 ProjectWrapper를 동기화하는 UseCase
 */
export class SyncProjectWrapperUseCase {
  constructor(
    private readonly projectWrapperRepository: ProjectWrapperRepository,
    private readonly projectRepository: ProjectRepository,
    private readonly memberRepositoryFactory: RepositoryFactory<MemberRepository, MemberRepositoryFactoryContext>
  ) {}

  async execute(request: SyncProjectWrapperRequest): Promise<CustomResult<SyncProjectWrapperResponse>> {
    try {
      // 입력 검증
      if (!request.projectId) {
        return Result.failure(new ValidationError("projectId", "Project ID is required"));
      }

      if (!request.updates || (request.updates.name === undefined && request.updates.imageUrl === undefined)) {
        return Result.failure(new ValidationError("updates", "At least one update field (name or imageUrl) is required"));
      }

      // 프로젝트 존재 확인
      const projectResult = await this.projectRepository.findById(request.projectId);
      if (!projectResult.success) {
        return Result.failure(projectResult.error);
      }

      if (!projectResult.data) {
        return Result.failure(new NotFoundError("Project not found", request.projectId));
      }

      // const project = projectResult.data; // 현재 사용하지 않음

      // 배치 업데이트 실행
      const batchUpdateResult = await this.projectWrapperRepository.batchUpdateByProjectId(
        request.projectId,
        request.updates
      );

      if (!batchUpdateResult.success) {
        return Result.failure(batchUpdateResult.error);
      }

      const updatedCount = batchUpdateResult.data;

      // 결과 반환
      return Result.success({
        projectId: request.projectId,
        updatedMemberCount: updatedCount,
        success: true,
      });
    } catch (error) {
      return Result.failure(
        error instanceof Error ? error : new InternalError("Failed to sync project wrappers")
      );
    }
  }

  /**
   * 개별 멤버의 ProjectWrapper를 동기화하는 메서드 (fallback용)
   * @param {string} userId 사용자 ID
   * @param {string} projectId 프로젝트 ID
   * @param {object} updates 업데이트 정보
   */
  async syncIndividualWrapper(
    userId: string,
    projectId: string,
    updates: { name?: string; imageUrl?: string }
  ): Promise<CustomResult<boolean>> {
    try {
      // 사용자의 해당 프로젝트 wrapper 찾기
      const wrapperResult = await this.projectWrapperRepository.findByUserIdAndProjectId(userId, projectId);

      if (!wrapperResult.success) {
        return Result.failure(wrapperResult.error);
      }

      if (!wrapperResult.data) {
        // wrapper가 없으면 동기화할 필요 없음
        return Result.success(false);
      }

      const wrapper = wrapperResult.data;

      // wrapper 업데이트
      const updatedWrapper = wrapper.updateProjectInfo(updates);

      // 저장
      const saveResult = await this.projectWrapperRepository.update(userId, updatedWrapper);

      if (!saveResult.success) {
        return Result.failure(saveResult.error);
      }

      return Result.success(true);
    } catch (error) {
      return Result.failure(
        error instanceof Error ? error : new InternalError("Failed to sync individual wrapper")
      );
    }
  }

  /**
   * 특정 프로젝트의 모든 멤버 ID를 조회하는 헬퍼 메서드
   * @param {string} projectId 프로젝트 ID
   */
  async getProjectMemberIds(projectId: string): Promise<CustomResult<string[]>> {
    try {
      // 해당 프로젝트의 MemberRepository 생성
      const memberRepository = this.memberRepositoryFactory.create({projectId: projectId});

      // 모든 멤버 조회
      const membersResult = await memberRepository.findAll();
      if (!membersResult.success) {
        return Result.failure(membersResult.error);
      }

      // userId만 추출
      const userIds = membersResult.data.map((member) => member.userId);
      return Result.success(userIds);
    } catch (error) {
      return Result.failure(
        error instanceof Error ? error : new InternalError("Failed to get project member IDs")
      );
    }
  }

  /**
   * 특정 멤버들의 wrapper를 일괄 동기화하는 메서드
   * @param {string[]} memberIds 멤버 ID 배열
   * @param {string} projectId 프로젝트 ID
   * @param {object} updates 업데이트 정보
   */
  async syncMemberWrappers(
    memberIds: string[],
    projectId: string,
    updates: { name?: string; imageUrl?: string }
  ): Promise<CustomResult<SyncProjectWrapperResponse>> {
    try {
      const errors: string[] = [];
      let successCount = 0;

      // 각 멤버의 wrapper를 개별적으로 동기화
      for (const memberId of memberIds) {
        const syncResult = await this.syncIndividualWrapper(memberId, projectId, updates);

        if (syncResult.success && syncResult.data) {
          successCount++;
        } else {
          const errorMessage = !syncResult.success && syncResult.error ? syncResult.error.message : "Unknown error";
          errors.push(`Failed to sync wrapper for member ${memberId}: ${errorMessage}`);
        }
      }

      return Result.success({
        projectId,
        updatedMemberCount: successCount,
        success: errors.length === 0,
        errors: errors.length > 0 ? errors : undefined,
      });
    } catch (error) {
      return Result.failure(
        error instanceof Error ? error : new InternalError("Failed to sync member wrappers")
      );
    }
  }
}
