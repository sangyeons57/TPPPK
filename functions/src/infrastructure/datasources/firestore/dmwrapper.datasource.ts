import * as admin from "firebase-admin";
import {CustomResult, Result} from "../../../core/types";
import {InternalError} from "../../../core/errors";
import {DMWrapperEntity} from "../../../domain/dmwrapper/entities/dmwrapper.entity";
import {UserEntity} from "../../../domain/user/entities/user.entity";

export interface DMWrapperDatasource {
  save(userId: string, dmWrapper: DMWrapperEntity): Promise<CustomResult<DMWrapperEntity>>;
  findByUserAndOtherUser(userId: string, otherUserId: string): Promise<CustomResult<DMWrapperEntity | null>>;
  findByUserId(userId: string): Promise<CustomResult<DMWrapperEntity[]>>;
  update(userId: string, dmWrapper: DMWrapperEntity): Promise<CustomResult<DMWrapperEntity>>;
  delete(userId: string, otherUserId: string): Promise<CustomResult<void>>;
}

export class FirestoreDMWrapperDataSource implements DMWrapperDatasource {
  private readonly db = admin.firestore();
  private readonly collectionName = "dm_wrapper";

  /**
   * Helper method to convert Firestore document data to DMWrapperEntity
   */
  private createDMWrapperEntity(doc: FirebaseFirestore.QueryDocumentSnapshot | FirebaseFirestore.DocumentSnapshot): CustomResult<DMWrapperEntity> {
    const data = doc.data();
    if (!data) {
      return Result.failure(new InternalError("Document data is null"));
    }

    try {
      const entity = DMWrapperEntity.fromData({
        id: doc.id,
        otherUserId: data.otherUserId,
        otherUserName: data.otherUserName,
        otherUserImageUrl: data.otherUserImageUrl || null,
        lastMessagePreview: data.lastMessagePreview || null,
        createdAt: data.createdAt instanceof admin.firestore.Timestamp ? data.createdAt.toDate() : new Date(data.createdAt),
        updatedAt: data.updatedAt instanceof admin.firestore.Timestamp ? data.updatedAt.toDate() : new Date(data.updatedAt),
      });
      return Result.success(entity);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to create DMWrapper entity: ${error instanceof Error ? error.message : "Unknown error"}`));
    }
  }

  /**
   * 특정 사용자의 dmwrappers subcollection 참조를 반환합니다
   * @param {string} userId - 사용자 ID
   * @return {FirebaseFirestore.CollectionReference} DMWrappers subcollection 참조
   */
  private getUserDMWrappersCollection(userId: string) {
    return this.db.collection(UserEntity.COLLECTION_NAME)
      .doc(userId)
      .collection(this.collectionName);
  }

  async save(userId: string, dmWrapper: DMWrapperEntity): Promise<CustomResult<DMWrapperEntity>> {
    try {
      const dmWrapperData = dmWrapper.toData();
      const docData = {
        otherUserId: dmWrapperData.otherUserId,
        otherUserName: dmWrapperData.otherUserName,
        otherUserImageUrl: dmWrapperData.otherUserImageUrl || null,
        lastMessagePreview: dmWrapperData.lastMessagePreview || null,
        createdAt: admin.firestore.Timestamp.fromDate(dmWrapperData.createdAt),
        updatedAt: admin.firestore.Timestamp.fromDate(dmWrapperData.updatedAt),
      };

      const docRef = this.getUserDMWrappersCollection(userId).doc(dmWrapper.id);
      await docRef.set(docData);

      return Result.success(dmWrapper);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to save DM wrapper: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async findByUserAndOtherUser(userId: string, otherUserId: string): Promise<CustomResult<DMWrapperEntity | null>> {
    try {
      // Use otherUserId as document ID for direct access
      const doc = await this.getUserDMWrappersCollection(userId).doc(otherUserId).get();

      if (!doc.exists) {
        return Result.success(null);
      }

      return this.createDMWrapperEntity(doc);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to find DM wrapper by user and other user: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async findByUserId(userId: string): Promise<CustomResult<DMWrapperEntity[]>> {
    try {
      const query = this.getUserDMWrappersCollection(userId)
        .orderBy("lastMessageAt", "desc")
        .orderBy("createdAt", "desc");

      const snapshot = await query.get();
      const dmWrappers: DMWrapperEntity[] = [];

      for (const doc of snapshot.docs) {
        const entityResult = this.createDMWrapperEntity(doc);
        if (entityResult.success) {
          dmWrappers.push(entityResult.data);
        }
      }

      return Result.success(dmWrappers);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to find DM wrappers by user ID: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async update(userId: string, dmWrapper: DMWrapperEntity): Promise<CustomResult<DMWrapperEntity>> {
    try {
      const dmWrapperData = dmWrapper.toData();
      const docData = {
        otherUserId: dmWrapperData.otherUserId,
        otherUserName: dmWrapperData.otherUserName,
        otherUserImageUrl: dmWrapperData.otherUserImageUrl || null,
        lastMessagePreview: dmWrapperData.lastMessagePreview || null,
        createdAt: admin.firestore.Timestamp.fromDate(dmWrapperData.createdAt),
        updatedAt: admin.firestore.Timestamp.fromDate(dmWrapperData.updatedAt),
      };

      const docRef = this.getUserDMWrappersCollection(userId).doc(dmWrapper.id);
      await docRef.update(docData);

      return Result.success(dmWrapper);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to update DM wrapper: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async delete(userId: string, otherUserId: string): Promise<CustomResult<void>> {
    try {
      // Use otherUserId as document ID for direct deletion
      const docRef = this.getUserDMWrappersCollection(userId).doc(otherUserId);
      await docRef.delete();
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to delete DM wrapper: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }
}