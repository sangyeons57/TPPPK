import {ProjectWrapperRepository} from "../../domain/projectwrapper/repositories/projectwrapper.repository";
import {ProjectWrapperDatasource, ProjectWrapperSearchCriteria} from "../datasources/interfaces/projectwrapper.datasource";
import {ProjectWrapperEntity, ProjectWrapperStatus} from "../../domain/projectwrapper/entities/projectwrapper.entity";
import {CustomResult} from "../../core/types";

/**
 * ProjectWrapper Repository 구현체
 * ProjectWrapperDatasource를 사용하여 Repository 인터페이스를 구현
 */
export class ProjectWrapperRepositoryImpl implements ProjectWrapperRepository {
  constructor(private readonly datasource: ProjectWrapperDatasource) {}

  async findById(userId: string, wrapperId: string): Promise<CustomResult<ProjectWrapperEntity | null>> {
    return this.datasource.findById(userId, wrapperId);
  }

  async findByUserIdAndProjectId(userId: string, projectId: string): Promise<CustomResult<ProjectWrapperEntity | null>> {
    return this.datasource.findByUserIdAndProjectId(userId, projectId);
  }

  async findByUserId(userId: string, status?: ProjectWrapperStatus): Promise<CustomResult<ProjectWrapperEntity[]>> {
    return this.datasource.findByUserId(userId, status);
  }

  async findByProjectId(projectId: string): Promise<CustomResult<ProjectWrapperEntity[]>> {
    return this.datasource.findByProjectId(projectId);
  }

  async findActiveByUserId(userId: string): Promise<CustomResult<ProjectWrapperEntity[]>> {
    return this.datasource.findActiveByUserId(userId);
  }

  async save(userId: string, wrapper: ProjectWrapperEntity): Promise<CustomResult<ProjectWrapperEntity>> {
    return this.datasource.save(userId, wrapper);
  }

  async update(userId: string, wrapper: ProjectWrapperEntity): Promise<CustomResult<ProjectWrapperEntity>> {
    return this.datasource.update(userId, wrapper);
  }

  async delete(userId: string, wrapperId: string): Promise<CustomResult<void>> {
    return this.datasource.delete(userId, wrapperId);
  }

  async deleteByUserIdAndProjectId(userId: string, projectId: string): Promise<CustomResult<void>> {
    return this.datasource.deleteByUserIdAndProjectId(userId, projectId);
  }

  async deleteAllByUserId(userId: string): Promise<CustomResult<void>> {
    return this.datasource.deleteAllByUserId(userId);
  }

  async deleteAllByProjectId(projectId: string): Promise<CustomResult<void>> {
    return this.datasource.deleteAllByProjectId(projectId);
  }

  async findByCriteria(criteria: ProjectWrapperSearchCriteria): Promise<CustomResult<ProjectWrapperEntity[]>> {
    return this.datasource.findByCriteria(criteria);
  }

  async countByUserId(userId: string, status?: ProjectWrapperStatus): Promise<CustomResult<number>> {
    return this.datasource.countByUserId(userId, status);
  }

  async countByProjectId(projectId: string): Promise<CustomResult<number>> {
    return this.datasource.countByProjectId(projectId);
  }

  async exists(userId: string, projectId: string): Promise<CustomResult<boolean>> {
    return this.datasource.exists(userId, projectId);
  }

  async findMemberIdsByProjectId(projectId: string): Promise<CustomResult<string[]>> {
    return this.datasource.findMemberIdsByProjectId(projectId);
  }

  async batchUpdateByProjectId(
    projectId: string,
    updates: {
      name?: string;
      imageUrl?: string;
    }
  ): Promise<CustomResult<number>> {
    return this.datasource.batchUpdateByProjectId(projectId, updates);
  }
}
