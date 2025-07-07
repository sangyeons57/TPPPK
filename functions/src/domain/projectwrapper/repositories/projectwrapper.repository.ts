import {CustomResult} from "../../../core/types";
import {ProjectWrapperEntity, ProjectWrapperStatus} from "../entities/projectwrapper.entity";

export interface ProjectWrapperSearchCriteria {
  userId?: string;
  projectId?: string;
  status?: ProjectWrapperStatus;
  limit?: number;
  offset?: number;
}

export interface ProjectWrapperRepository {
  /**
   * ID로 프로젝트 래퍼를 찾습니다.
   * @param userId 사용자 ID (collection owner)
   * @param wrapperId 래퍼 ID
   */
  findById(userId: string, wrapperId: string): Promise<CustomResult<ProjectWrapperEntity | null>>;

  /**
   * 특정 사용자의 특정 프로젝트 래퍼를 찾습니다.
   * @param userId 사용자 ID
   * @param projectId 프로젝트 ID
   */
  findByUserIdAndProjectId(userId: string, projectId: string): Promise<CustomResult<ProjectWrapperEntity | null>>;

  /**
   * 사용자의 모든 프로젝트 래퍼를 조회합니다.
   * @param userId 사용자 ID
   * @param status 선택적 상태 필터
   */
  findByUserId(userId: string, status?: ProjectWrapperStatus): Promise<CustomResult<ProjectWrapperEntity[]>>;

  /**
   * 특정 프로젝트의 모든 래퍼를 조회합니다 (모든 멤버의 래퍼).
   * @param projectId 프로젝트 ID
   */
  findByProjectId(projectId: string): Promise<CustomResult<ProjectWrapperEntity[]>>;

  /**
   * 사용자의 활성 프로젝트 래퍼들을 조회합니다.
   * @param userId 사용자 ID
   */
  findActiveByUserId(userId: string): Promise<CustomResult<ProjectWrapperEntity[]>>;

  /**
   * 프로젝트 래퍼를 저장합니다.
   * @param userId 프로젝트 래퍼를 저장할 사용자의 ID (collection owner)
   * @param wrapper 저장할 프로젝트 래퍼 엔티티
   */
  save(userId: string, wrapper: ProjectWrapperEntity): Promise<CustomResult<ProjectWrapperEntity>>;

  /**
   * 프로젝트 래퍼를 업데이트합니다.
   * @param userId 프로젝트 래퍼를 업데이트할 사용자의 ID (collection owner)
   * @param wrapper 업데이트할 프로젝트 래퍼 엔티티
   */
  update(userId: string, wrapper: ProjectWrapperEntity): Promise<CustomResult<ProjectWrapperEntity>>;

  /**
   * 프로젝트 래퍼를 삭제합니다.
   * @param userId 사용자 ID
   * @param wrapperId 래퍼 ID
   */
  delete(userId: string, wrapperId: string): Promise<CustomResult<void>>;

  /**
   * 사용자의 특정 프로젝트 래퍼를 삭제합니다.
   * @param userId 사용자 ID
   * @param projectId 프로젝트 ID
   */
  deleteByUserIdAndProjectId(userId: string, projectId: string): Promise<CustomResult<void>>;

  /**
   * 사용자의 모든 프로젝트 래퍼를 삭제합니다.
   * @param userId 사용자 ID
   */
  deleteAllByUserId(userId: string): Promise<CustomResult<void>>;

  /**
   * 특정 프로젝트의 모든 래퍼를 삭제합니다 (프로젝트 삭제 시 사용).
   * @param projectId 프로젝트 ID
   */
  deleteAllByProjectId(projectId: string): Promise<CustomResult<void>>;

  /**
   * 검색 조건에 따라 프로젝트 래퍼를 조회합니다.
   */
  findByCriteria(criteria: ProjectWrapperSearchCriteria): Promise<CustomResult<ProjectWrapperEntity[]>>;

  /**
   * 사용자의 프로젝트 래퍼 수를 조회합니다.
   * @param userId 사용자 ID
   * @param status 선택적 상태 필터
   */
  countByUserId(userId: string, status?: ProjectWrapperStatus): Promise<CustomResult<number>>;

  /**
   * 특정 프로젝트의 래퍼 수를 조회합니다 (멤버 수와 동일).
   * @param projectId 프로젝트 ID
   */
  countByProjectId(projectId: string): Promise<CustomResult<number>>;

  /**
   * 사용자가 특정 프로젝트의 래퍼를 가지고 있는지 확인합니다.
   * @param userId 사용자 ID
   * @param projectId 프로젝트 ID
   */
  exists(userId: string, projectId: string): Promise<CustomResult<boolean>>;

  /**
   * 특정 프로젝트의 모든 멤버 ID를 조회합니다.
   * @param projectId 프로젝트 ID
   */
  findMemberIdsByProjectId(projectId: string): Promise<CustomResult<string[]>>;

  /**
   * 특정 프로젝트의 모든 래퍼를 배치로 업데이트합니다.
   * @param projectId 프로젝트 ID
   * @param updates 업데이트할 정보
   */
  batchUpdateByProjectId(
    projectId: string, 
    updates: {
      name?: string;
      imageUrl?: string;
    }
  ): Promise<CustomResult<number>>; // 업데이트된 래퍼 수 반환
} 