import { ProjectRepository } from '../../domain/project/repositories/project.repository';
import { ProjectDatasource } from '../datasources/interfaces/project.datasource';
import { ProjectEntity, ProjectStatus, ProjectName } from '../../domain/project/entities/project.entity';
import { CustomResult } from '../../core/types';

/**
 * Project Repository 구현체
 * ProjectDatasource를 사용하여 Repository 인터페이스를 구현
 */
export class ProjectRepositoryImpl implements ProjectRepository {
  constructor(private readonly datasource: ProjectDatasource) {}

  async findById(id: string): Promise<CustomResult<ProjectEntity | null>> {
    return this.datasource.findById(id);
  }

  async findByOwnerId(ownerId: string): Promise<CustomResult<ProjectEntity[]>> {
    return this.datasource.findByOwnerId(ownerId);
  }

  async findByName(name: ProjectName): Promise<CustomResult<ProjectEntity | null>> {
    return this.datasource.findByName(name);
  }

  async findByStatus(status: ProjectStatus): Promise<CustomResult<ProjectEntity[]>> {
    return this.datasource.findByStatus(status);
  }

  async save(project: ProjectEntity): Promise<CustomResult<ProjectEntity>> {
    return this.datasource.save(project);
  }

  async update(project: ProjectEntity): Promise<CustomResult<ProjectEntity>> {
    return this.datasource.update(project);
  }

  async delete(id: string): Promise<CustomResult<void>> {
    return this.datasource.delete(id);
  }

  async exists(id: string): Promise<CustomResult<boolean>> {
    return this.datasource.exists(id);
  }

  async findActiveProjects(limit?: number): Promise<CustomResult<ProjectEntity[]>> {
    return this.datasource.findActiveProjects(limit);
  }

  async findProjectsByMemberId(memberId: string): Promise<CustomResult<ProjectEntity[]>> {
    return this.datasource.findProjectsByMemberId(memberId);
  }

  async updateMemberCount(projectId: string, count: number): Promise<CustomResult<void>> {
    return this.datasource.updateMemberCount(projectId, count);
  }
}