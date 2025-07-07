import * as admin from "firebase-admin";
import {CustomResult, Result} from "../../../core/types";
import {InternalError} from "../../../core/errors";
import {
  ProjectWrapperDatasource,
  ProjectWrapperSearchCriteria,
} from "../interfaces/projectwrapper.datasource";
import {
  ProjectWrapperEntity,
  ProjectWrapperStatus,
  ProjectWrapperData,
} from "../../../domain/projectwrapper/entities/projectwrapper.entity";
import {UserEntity} from "../../../domain/user/entities/user.entity";
import {FieldPath} from "firebase-admin/firestore";

export class FirestoreProjectWrapperDataSource implements ProjectWrapperDatasource {
  private readonly db = admin.firestore();

  /**
   * Helper method to convert Firestore document data to ProjectWrapperEntity
   * @param {FirebaseFirestore.QueryDocumentSnapshot | FirebaseFirestore.DocumentSnapshot} doc - Firestore document snapshot
   * @return {CustomResult<ProjectWrapperEntity>} ProjectWrapperEntity 또는 에러 결과
   */
  private createProjectWrapperEntity(doc: FirebaseFirestore.QueryDocumentSnapshot | FirebaseFirestore.DocumentSnapshot): CustomResult<ProjectWrapperEntity> {
    try {
      const data = doc.data() as ProjectWrapperData;

      const entityData: ProjectWrapperData = {
        ...data,
        id: doc.id,
        joinedAt: data.joinedAt instanceof admin.firestore.Timestamp ? data.joinedAt.toDate() : new Date(data.joinedAt),
        lastUpdatedAt: data.lastUpdatedAt instanceof admin.firestore.Timestamp ? data.lastUpdatedAt.toDate() : new Date(data.lastUpdatedAt),
        createdAt: data.createdAt instanceof admin.firestore.Timestamp ? data.createdAt.toDate() : new Date(data.createdAt),
        updatedAt: data.updatedAt instanceof admin.firestore.Timestamp ? data.updatedAt.toDate() : new Date(data.updatedAt),
      };

      return Result.success(ProjectWrapperEntity.fromData(entityData));
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to create ProjectWrapper entity: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  /**
   * 특정 사용자의 project_wrappers subcollection 참조를 반환합니다
   * @param {string} userId - 사용자 ID
   * @return {FirebaseFirestore.CollectionReference} ProjectWrapper subcollection 참조
   */
  private getUserProjectWrappersCollection(userId: string) {
    return this.db.collection(UserEntity.COLLECTION_NAME)
      .doc(userId)
      .collection(ProjectWrapperEntity.COLLECTION_NAME);
  }

  async findById(userId: string, wrapperId: string): Promise<CustomResult<ProjectWrapperEntity | null>> {
    try {
      const doc = await this.getUserProjectWrappersCollection(userId).doc(wrapperId).get();

      if (!doc.exists) {
        return Result.success(null);
      }

      return this.createProjectWrapperEntity(doc);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to find project wrapper by ID: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async findByUserIdAndProjectId(userId: string, projectId: string): Promise<CustomResult<ProjectWrapperEntity | null>> {
    try {
      const query = this.getUserProjectWrappersCollection(userId)
        .where(FieldPath.documentId(), "==", projectId)
        .limit(1);

      const snapshot = await query.get();

      if (snapshot.empty) {
        return Result.success(null);
      }

      return this.createProjectWrapperEntity(snapshot.docs[0]);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to find project wrapper by user and project ID: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async findByUserId(userId: string, status?: ProjectWrapperStatus): Promise<CustomResult<ProjectWrapperEntity[]>> {
    try {
      let query: FirebaseFirestore.Query = this.getUserProjectWrappersCollection(userId);

      if (status) {
        query = query.where(ProjectWrapperEntity.KEY_STATUS, "==", status);
      }

      const snapshot = await query.orderBy(ProjectWrapperEntity.KEY_LAST_UPDATED_AT, "desc").get();
      const wrappers: ProjectWrapperEntity[] = [];

      for (const doc of snapshot.docs) {
        const entityResult = this.createProjectWrapperEntity(doc);
        if (entityResult.success) {
          wrappers.push(entityResult.data);
        }
      }

      return Result.success(wrappers);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to find project wrappers by user ID: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async findByProjectId(projectId: string): Promise<CustomResult<ProjectWrapperEntity[]>> {
    try {
      // 모든 사용자의 project_wrappers를 검색하기 위해 collection group query 사용
      const query = this.db.collectionGroup(ProjectWrapperEntity.COLLECTION_NAME);
      const q = query.where(FieldPath.documentId(), "==", projectId);

      const snapshot = await q.get();
      const wrappers: ProjectWrapperEntity[] = [];

      for (const doc of snapshot.docs) {
        const entityResult = this.createProjectWrapperEntity(doc);
        if (entityResult.success) {
          wrappers.push(entityResult.data);
        }
      }

      return Result.success(wrappers);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to find project wrappers by project ID: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async findActiveByUserId(userId: string): Promise<CustomResult<ProjectWrapperEntity[]>> {
    return this.findByUserId(userId, ProjectWrapperStatus.ACTIVE);
  }

  async save(userId: string, wrapper: ProjectWrapperEntity): Promise<CustomResult<ProjectWrapperEntity>> {
    try {
      const data = wrapper.toData();
      const docRef = this.getUserProjectWrappersCollection(userId).doc(wrapper.id);

      // Remove the id field before saving
      const {id, ...saveData} = data;

      await docRef.set(saveData);
      return Result.success(wrapper);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to save project wrapper: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async update(userId: string, wrapper: ProjectWrapperEntity): Promise<CustomResult<ProjectWrapperEntity>> {
    try {
      const data = wrapper.toData();
      const docRef = this.getUserProjectWrappersCollection(userId).doc(wrapper.id);

      // Remove the id field before updating
      const {id, ...updateData} = data;

      await docRef.update(updateData);
      return Result.success(wrapper);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to update project wrapper: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async delete(userId: string, wrapperId: string): Promise<CustomResult<void>> {
    try {
      await this.getUserProjectWrappersCollection(userId).doc(wrapperId).delete();
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to delete project wrapper: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async deleteByUserIdAndProjectId(userId: string, projectId: string): Promise<CustomResult<void>> {
    try {
      const wrapperResult = await this.findByUserIdAndProjectId(userId, projectId);

      if (!wrapperResult.success) {
        return Result.failure(wrapperResult.error);
      }

      if (!wrapperResult.data) {
        return Result.success(undefined); // Nothing to delete
      }

      return this.delete(userId, wrapperResult.data.id);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to delete project wrapper by user and project ID: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async deleteAllByUserId(userId: string): Promise<CustomResult<void>> {
    try {
      const collection = this.getUserProjectWrappersCollection(userId);
      const snapshot = await collection.get();

      const batch = this.db.batch();
      snapshot.docs.forEach((doc) => {
        batch.delete(doc.ref);
      });

      await batch.commit();
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to delete all project wrappers by user ID: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async deleteAllByProjectId(projectId: string): Promise<CustomResult<void>> {
    try {
      // Collection group query를 사용해 모든 사용자의 해당 프로젝트 wrapper 찾기
      const query = this.db.collectionGroup(ProjectWrapperEntity.COLLECTION_NAME)
        .where(FieldPath.documentId(), "==", projectId);

      const snapshot = await query.get();
      const batch = this.db.batch();

      snapshot.docs.forEach((doc) => {
        batch.delete(doc.ref);
      });

      await batch.commit();
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to delete all project wrappers by project ID: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async findByCriteria(criteria: ProjectWrapperSearchCriteria): Promise<CustomResult<ProjectWrapperEntity[]>> {
    try {
      let query: FirebaseFirestore.Query;

      if (criteria.userId) {
        // 특정 사용자의 subcollection에서 검색
        query = this.getUserProjectWrappersCollection(criteria.userId);
      } else {
        // 모든 사용자의 wrapper에서 검색
        query = this.db.collectionGroup(ProjectWrapperEntity.COLLECTION_NAME);
      }

      if (criteria.projectId) {
        query = query.where(FieldPath.documentId(), "==", criteria.projectId);
      }

      if (criteria.status) {
        query = query.where(ProjectWrapperEntity.KEY_STATUS, "==", criteria.status);
      }

      if (criteria.limit) {
        query = query.limit(criteria.limit);
      }

      if (criteria.offset) {
        query = query.offset(criteria.offset);
      }

      const snapshot = await query.get();
      const wrappers: ProjectWrapperEntity[] = [];

      for (const doc of snapshot.docs) {
        const entityResult = this.createProjectWrapperEntity(doc);
        if (entityResult.success) {
          wrappers.push(entityResult.data);
        }
      }

      return Result.success(wrappers);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to find project wrappers by criteria: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async countByUserId(userId: string, status?: ProjectWrapperStatus): Promise<CustomResult<number>> {
    try {
      let query: FirebaseFirestore.Query = this.getUserProjectWrappersCollection(userId);

      if (status) {
        query = query.where(ProjectWrapperEntity.KEY_STATUS, "==", status);
      }

      const snapshot = await query.get();
      return Result.success(snapshot.size);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to count project wrappers by user ID: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async countByProjectId(projectId: string): Promise<CustomResult<number>> {
    try {
      const query = this.db.collectionGroup(ProjectWrapperEntity.COLLECTION_NAME)
        .where(FieldPath.documentId(), "==", projectId);

      const snapshot = await query.get();
      return Result.success(snapshot.size);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to count project wrappers by project ID: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async exists(userId: string, projectId: string): Promise<CustomResult<boolean>> {
    try {
      const wrapperResult = await this.findByUserIdAndProjectId(userId, projectId);

      if (!wrapperResult.success) {
        return Result.failure(wrapperResult.error);
      }

      return Result.success(wrapperResult.data !== null);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to check if project wrapper exists: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async findMemberIdsByProjectId(projectId: string): Promise<CustomResult<string[]>> {
    try {
      const query = this.db.collectionGroup(ProjectWrapperEntity.COLLECTION_NAME)
        .where(FieldPath.documentId(), "==", projectId)
        .where(ProjectWrapperEntity.KEY_STATUS, "==", ProjectWrapperStatus.ACTIVE);

      const snapshot = await query.get();
      const memberIds: string[] = [];

      snapshot.docs.forEach((doc) => {
        // 부모 문서(user) ID 추출
        const userRef = doc.ref.parent.parent;
        if (userRef) {
          memberIds.push(userRef.id);
        }
      });

      return Result.success(memberIds);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to find member IDs by project ID: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async batchUpdateByProjectId(
    projectId: string,
    updates: {
      name?: string;
      imageUrl?: string;
    }
  ): Promise<CustomResult<number>> {
    try {
      // 해당 프로젝트의 모든 wrapper 찾기
      const query = this.db.collectionGroup(ProjectWrapperEntity.COLLECTION_NAME)
        .where(FieldPath.documentId(), "==", projectId);

      const snapshot = await query.get();

      if (snapshot.empty) {
        return Result.success(0);
      }

      const batch = this.db.batch();
      const now = new Date();
      let updateCount = 0;

      // 배치 업데이트 준비
      const updateData: any = {
        [ProjectWrapperEntity.KEY_LAST_UPDATED_AT]: now,
        [ProjectWrapperEntity.KEY_UPDATED_AT]: now,
      };

      if (updates.name !== undefined) {
        updateData[ProjectWrapperEntity.KEY_PROJECT_NAME] = updates.name;
      }

      if (updates.imageUrl !== undefined) {
        updateData[ProjectWrapperEntity.KEY_PROJECT_IMAGE_URL] = updates.imageUrl;
      }

      snapshot.docs.forEach((doc) => {
        batch.update(doc.ref, updateData);
        updateCount++;
      });

      await batch.commit();
      return Result.success(updateCount);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to batch update project wrappers: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }
}
