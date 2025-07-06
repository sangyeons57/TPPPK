import * as admin from "firebase-admin";
import {ProjectDatasource} from "../interfaces/project.datasource";
import {ProjectEntity, ProjectName, ProjectDescription, ProjectImage, ProjectStatus} from "../../../domain/project/entities/project.entity";
import {CustomResult, Result} from "../../../core/types";
import {InternalError} from "../../../core/errors";

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

export class FirestoreProjectDataSource implements ProjectDatasource {
  private readonly db = admin.firestore();
  private readonly collection = this.db.collection(ProjectEntity.COLLECTION_NAME);

  async findById(id: string): Promise<CustomResult<ProjectEntity | null>> {
    try {
      const doc = await this.collection.doc(id).get();

      if (!doc.exists) {
        return Result.success(null);
      }

      const data = doc.data();
      if (!data) {
        return Result.success(null);
      }

      // Create ProjectEntity from Firestore data
      const project = ProjectEntity.fromDataSource(
        doc.id,
        new ProjectName(data.name),
        data.ownerId,
        data.createdAt?.toDate(),
        data.updatedAt?.toDate(),
        data.image ? new ProjectImage(data.image) : undefined
      );

      return Result.success(project);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to find project by ID: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async findByOwnerId(ownerId: string): Promise<CustomResult<ProjectEntity[]>> {
    try {
      const query = await this.collection
        .where("ownerId", "==", ownerId)
        .orderBy("createdAt", "desc")
        .get();

      const projects = query.docs.map((doc) => this.mapToEntity(doc.id, doc.data() as ProjectData));
      return Result.success(projects);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find projects by owner: ${error instanceof Error ? error.message : "Unknown error"}`));
    }
  }

  async findByName(name: ProjectName): Promise<CustomResult<ProjectEntity | null>> {
    try {
      const query = await this.collection.where("name", "==", name.value).limit(1).get();
      if (query.empty) {
        return Result.success(null);
      }

      const data = query.docs[0].data() as ProjectData;
      return Result.success(this.mapToEntity(data.id, data));
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find project by name: ${error instanceof Error ? error.message : "Unknown error"}`));
    }
  }

  async findByStatus(status: ProjectStatus): Promise<CustomResult<ProjectEntity[]>> {
    try {
      const query = await this.collection
        .where("status", "==", status)
        .orderBy("createdAt", "desc")
        .get();

      const projects = query.docs.map((doc) => this.mapToEntity(doc.id, doc.data() as ProjectData));
      return Result.success(projects);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find projects by status: ${error instanceof Error ? error.message : "Unknown error"}`));
    }
  }

  async save(project: ProjectEntity): Promise<CustomResult<ProjectEntity>> {
    try {
      const projectData = project.toData();
      const docData = {
        name: projectData.name,
        imageUrl: projectData.imageUrl || null,
        ownerId: projectData.ownerId,
        createdAt: admin.firestore.Timestamp.fromDate(projectData.createdAt),
        updatedAt: admin.firestore.Timestamp.fromDate(projectData.updatedAt),
      };

      if (project.id) {
        // Update existing project
        await this.collection.doc(project.id).set(docData);
        return Result.success(project);
      } else {
        // Create new project
        const docRef = await this.collection.add(docData);
        const newProject = ProjectEntity.fromDataSource(
          docRef.id,
          project.name,
          project.ownerId,
          projectData.createdAt,
          projectData.updatedAt,
          project.image
        );
        return Result.success(newProject);
      }
    } catch (error) {
      return Result.failure(new InternalError(`Failed to save project: ${error instanceof Error ? error.message : "Unknown error"}`));
    }
  }

  async update(project: ProjectEntity): Promise<CustomResult<ProjectEntity>> {
    try {
      const projectData = project.toData();
      const docData = {
        name: projectData.name,
        imageUrl: projectData.imageUrl || null,
        ownerId: projectData.ownerId,
        createdAt: admin.firestore.Timestamp.fromDate(projectData.createdAt),
        updatedAt: admin.firestore.Timestamp.fromDate(projectData.updatedAt),
      };

      await this.collection.doc(project.id).update(docData);
      return Result.success(project);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to update project: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async delete(id: string): Promise<CustomResult<void>> {
    try {
      await this.collection.doc(id).delete();
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to delete project: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async exists(id: string): Promise<CustomResult<boolean>> {
    try {
      const doc = await this.collection.doc(id).get();
      return Result.success(doc.exists);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to check project existence: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async findActiveProjects(limit = 50): Promise<CustomResult<ProjectEntity[]>> {
    try {
      const query = await this.collection
        .where("status", "==", ProjectStatus.ACTIVE)
        .orderBy("createdAt", "desc")
        .limit(limit)
        .get();

      const projects = query.docs.map((doc) => this.mapToEntity(doc.id, doc.data() as ProjectData));
      return Result.success(projects);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to find active projects: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async updateMemberCount(projectId: string, count: number): Promise<CustomResult<void>> {
    try {
      await this.collection.doc(projectId).update({
        memberCount: count,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to update member count: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  private mapToEntity(id: string, data: ProjectData): ProjectEntity {
    return new ProjectEntity(
      id,
      new ProjectName(data.name),
      data.ownerId,
      data.createdAt.toDate(),
      data.updatedAt.toDate(),
      data.status || ProjectStatus.ACTIVE,
      data.memberCount || 1,
      data.description ? new ProjectDescription(data.description) : undefined,
      data.image ? new ProjectImage(data.image) : undefined
    );
  }
}
