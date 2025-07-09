import * as admin from "firebase-admin";
import {CustomResult, Result} from "../../../core/types";
import {InternalError} from "../../../core/errors";
import {DMChannelEntity} from "../../../domain/dmchannel/entities/dmchannel.entity";

export interface DMChannelDatasource {
  save(dmChannel: DMChannelEntity): Promise<CustomResult<DMChannelEntity>>;
  findById(channelId: string): Promise<CustomResult<DMChannelEntity | null>>;
  findByParticipants(userId1: string, userId2: string): Promise<CustomResult<DMChannelEntity | null>>;
  delete(channelId: string): Promise<CustomResult<void>>;
}

export class FirestoreDMChannelDataSource implements DMChannelDatasource {
  private readonly db = admin.firestore();
  private readonly collectionName = "dm_channels";

  private createDMChannelEntity(doc: FirebaseFirestore.QueryDocumentSnapshot | FirebaseFirestore.DocumentSnapshot): CustomResult<DMChannelEntity> {
    const data = doc.data();
    if (!data) {
      return Result.failure(new InternalError("Document data is null"));
    }

    try {
      const entity = DMChannelEntity.fromData({
        id: doc.id,
        participants: data.participants || [],
        status: data.status,
        blockedByMap: data.blockedByMap || {},
        createdAt: data.createdAt instanceof admin.firestore.Timestamp ? data.createdAt.toDate() : new Date(data.createdAt),
        updatedAt: data.updatedAt instanceof admin.firestore.Timestamp ? data.updatedAt.toDate() : new Date(data.updatedAt),
      });
      return Result.success(entity);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to create DMChannel entity: ${error instanceof Error ? error.message : "Unknown error"}`));
    }
  }

  async save(dmChannel: DMChannelEntity): Promise<CustomResult<DMChannelEntity>> {
    try {
      const dmChannelData = dmChannel.toData();
      const docData = {
        participants: dmChannelData.participants,
        status: dmChannelData.status,
        createdAt: admin.firestore.Timestamp.fromDate(dmChannelData.createdAt),
        updatedAt: admin.firestore.Timestamp.fromDate(dmChannelData.updatedAt),
      };

      const docRef = this.db.collection(this.collectionName).doc(dmChannel.id);
      await docRef.set(docData);

      return Result.success(dmChannel);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to save DM channel: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async findById(channelId: string): Promise<CustomResult<DMChannelEntity | null>> {
    try {
      const doc = await this.db.collection(this.collectionName).doc(channelId).get();

      if (!doc.exists) {
        return Result.success(null);
      }

      return this.createDMChannelEntity(doc);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to find DM channel by ID: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async findByParticipants(userId1: string, userId2: string): Promise<CustomResult<DMChannelEntity | null>> {
    try {
      // DM Channel ID is deterministic based on sorted user IDs
      const sortedUserIds = [userId1, userId2].sort();
      const channelId = `dm_${sortedUserIds[0]}_${sortedUserIds[1]}`;
      
      const doc = await this.db.collection(this.collectionName).doc(channelId).get();

      if (!doc.exists) {
        return Result.success(null);
      }

      return this.createDMChannelEntity(doc);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to find DM channel by participants: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async delete(channelId: string): Promise<CustomResult<void>> {
    try {
      await this.db.collection(this.collectionName).doc(channelId).delete();
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to delete DM channel: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }
}