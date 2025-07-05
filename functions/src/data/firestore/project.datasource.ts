import { ProjectRepository } from '../../domain/project/project.repository';
import { ProjectEntity, ProjectName, ProjectDescription, ProjectImage, ProjectStatus } from '../../domain/project/project.entity';
import { CustomResult, Result } from '../../core/types';
import { NotFoundError, InternalError } from '../../core/errors';
import { FIRESTORE_COLLECTIONS } from '../../core/constants';
import { getFirestore, FieldValue } from 'firebase-admin/firestore';

interface ProjectData {
  id: string;
  name: string;
  description?: string;
  ownerId: string;
  image?: string;
  status: ProjectStatus;
  memberCount: number;
  createdAt: FirebaseFirestore.Timestamp;
  updatedAt: FirebaseFirestore.Timestamp;
}

export class FirestoreProjectDataSource implements ProjectRepository {
  private readonly db = getFirestore();
  private readonly collection = this.db.collection(FIRESTORE_COLLECTIONS.PROJECTS);

  async findById(id: string): Promise<CustomResult<ProjectEntity | null>> {
    try {
      const doc = await this.collection.doc(id).get();
      if (!doc.exists) {
        return Result.success(null);
      }
      
      const data = doc.data() as ProjectData;
      return Result.success(this.mapToEntity(data));
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find project by id: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async findByOwnerId(ownerId: string): Promise<CustomResult<ProjectEntity[]>> {
    try {
      const query = await this.collection
        .where('ownerId', '==', ownerId)
        .orderBy('createdAt', 'desc')
        .get();
      
      const projects = query.docs.map(doc => this.mapToEntity(doc.data() as ProjectData));
      return Result.success(projects);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find projects by owner: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async findByName(name: ProjectName): Promise<CustomResult<ProjectEntity | null>> {
    try {
      const query = await this.collection.where('name', '==', name.value).limit(1).get();
      if (query.empty) {
        return Result.success(null);
      }
      
      const data = query.docs[0].data() as ProjectData;
      return Result.success(this.mapToEntity(data));
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find project by name: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async findByStatus(status: ProjectStatus): Promise<CustomResult<ProjectEntity[]>> {
    try {
      const query = await this.collection
        .where('status', '==', status)
        .orderBy('createdAt', 'desc')
        .get();
      
      const projects = query.docs.map(doc => this.mapToEntity(doc.data() as ProjectData));
      return Result.success(projects);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find projects by status: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async save(project: ProjectEntity): Promise<CustomResult<ProjectEntity>> {
    try {
      const data = this.mapToData(project);
      await this.collection.doc(project.id).set(data);
      return Result.success(project);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to save project: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async update(project: ProjectEntity): Promise<CustomResult<ProjectEntity>> {
    try {
      const data = this.mapToData(project);
      data.updatedAt = FieldValue.serverTimestamp() as any;
      await this.collection.doc(project.id).update(data);
      return Result.success(project);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to update project: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async delete(id: string): Promise<CustomResult<void>> {
    try {
      await this.collection.doc(id).delete();
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to delete project: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async exists(id: string): Promise<CustomResult<boolean>> {
    try {
      const doc = await this.collection.doc(id).get();
      return Result.success(doc.exists);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to check project existence: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async findActiveProjects(limit: number = 50): Promise<CustomResult<ProjectEntity[]>> {
    try {
      const query = await this.collection
        .where('status', '==', ProjectStatus.ACTIVE)
        .orderBy('createdAt', 'desc')
        .limit(limit)
        .get();
      
      const projects = query.docs.map(doc => this.mapToEntity(doc.data() as ProjectData));
      return Result.success(projects);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find active projects: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async findProjectsByMemberId(memberId: string): Promise<CustomResult<ProjectEntity[]>> {
    try {
      const memberQuery = await this.db
        .collection(FIRESTORE_COLLECTIONS.PROJECT_MEMBERS)
        .where('userId', '==', memberId)
        .get();
      
      if (memberQuery.empty) {
        return Result.success([]);
      }
      
      const projectIds = memberQuery.docs.map(doc => doc.data().projectId);
      const projects: ProjectEntity[] = [];
      
      for (const projectId of projectIds) {
        const projectResult = await this.findById(projectId);
        if (projectResult.success && projectResult.data) {
          projects.push(projectResult.data);
        }
      }
      
      return Result.success(projects);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find projects by member: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async updateMemberCount(projectId: string, count: number): Promise<CustomResult<void>> {
    try {
      await this.collection.doc(projectId).update({
        memberCount: count,
        updatedAt: FieldValue.serverTimestamp()
      });
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to update member count: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  private mapToEntity(data: ProjectData): ProjectEntity {
    return new ProjectEntity(
      data.id,
      new ProjectName(data.name),
      data.ownerId,
      data.createdAt.toDate(),
      data.updatedAt.toDate(),
      data.status,
      data.memberCount,
      data.description ? new ProjectDescription(data.description) : undefined,
      data.image ? new ProjectImage(data.image) : undefined
    );
  }

  private mapToData(entity: ProjectEntity): ProjectData {
    return {
      id: entity.id,
      name: entity.name.value,
      description: entity.description?.value,
      ownerId: entity.ownerId,
      image: entity.image?.value,
      status: entity.status,
      memberCount: entity.memberCount,
      createdAt: entity.createdAt as any,
      updatedAt: entity.updatedAt as any,
    };
  }
}