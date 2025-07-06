import {ProjectEntity, ProjectStatus, ProjectName} from "../entities/project.entity";
import {CustomResult} from "../../../core/types";

export interface ProjectRepository {
  findById(id: string): Promise<CustomResult<ProjectEntity | null>>;
  findByOwnerId(ownerId: string): Promise<CustomResult<ProjectEntity[]>>;
  findByName(name: ProjectName): Promise<CustomResult<ProjectEntity | null>>;
  findByStatus(status: ProjectStatus): Promise<CustomResult<ProjectEntity[]>>;
  save(project: ProjectEntity): Promise<CustomResult<ProjectEntity>>;
  update(project: ProjectEntity): Promise<CustomResult<ProjectEntity>>;
  delete(id: string): Promise<CustomResult<void>>;
  exists(id: string): Promise<CustomResult<boolean>>;
  findActiveProjects(limit?: number): Promise<CustomResult<ProjectEntity[]>>;
  updateMemberCount(projectId: string, count: number): Promise<CustomResult<void>>;
}
