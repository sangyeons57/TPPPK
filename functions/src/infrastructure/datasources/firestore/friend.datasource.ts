import * as admin from 'firebase-admin';
import { CustomResult, Result } from '../../../core/types';
import { NotFoundError, InternalError } from '../../../core/errors';
import { COLLECTIONS } from '../../../core/constants';
import { 
  FriendDatasource, 
  FriendSearchCriteria 
} from '../../../domain/friend/datasources/friend.datasource';
import { 
  FriendEntity, 
  FriendId, 
  UserId, 
  FriendStatus, 
  FriendData 
} from '../../../domain/friend/entities/friend.entity';

export class FirestoreFriendDataSource implements FriendDatasource {
  private readonly db = admin.firestore();
  private readonly collection = this.db.collection(COLLECTIONS.FRIENDS);

  async findById(id: FriendId): Promise<CustomResult<FriendEntity | null>> {
    try {
      const doc = await this.collection.doc(id.value).get();
      
      if (!doc.exists) {
        return Result.success(null);
      }

      const data = doc.data() as FriendData;
      const entityResult = FriendEntity.fromData({
        ...data,
        id: doc.id,
        requestedAt: data.requestedAt instanceof admin.firestore.Timestamp 
          ? data.requestedAt.toDate() 
          : new Date(data.requestedAt),
        respondedAt: data.respondedAt instanceof admin.firestore.Timestamp 
          ? data.respondedAt.toDate() 
          : data.respondedAt ? new Date(data.respondedAt) : undefined,
        createdAt: data.createdAt instanceof admin.firestore.Timestamp 
          ? data.createdAt.toDate() 
          : new Date(data.createdAt),
        updatedAt: data.updatedAt instanceof admin.firestore.Timestamp 
          ? data.updatedAt.toDate() 
          : new Date(data.updatedAt)
      });

      return entityResult;
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find friend by ID: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async findByUserIds(userId: UserId, friendUserId: UserId): Promise<CustomResult<FriendEntity | null>> {
    try {
      const query = this.collection
        .where('userId', '==', userId.value)
        .where('friendUserId', '==', friendUserId.value)
        .limit(1);

      const snapshot = await query.get();
      
      if (snapshot.empty) {
        return Result.success(null);
      }

      const doc = snapshot.docs[0];
      const data = doc.data() as FriendData;
      
      const entityResult = FriendEntity.fromData({
        ...data,
        id: doc.id,
        requestedAt: data.requestedAt instanceof admin.firestore.Timestamp 
          ? data.requestedAt.toDate() 
          : new Date(data.requestedAt),
        respondedAt: data.respondedAt instanceof admin.firestore.Timestamp 
          ? data.respondedAt.toDate() 
          : data.respondedAt ? new Date(data.respondedAt) : undefined,
        createdAt: data.createdAt instanceof admin.firestore.Timestamp 
          ? data.createdAt.toDate() 
          : new Date(data.createdAt),
        updatedAt: data.updatedAt instanceof admin.firestore.Timestamp 
          ? data.updatedAt.toDate() 
          : new Date(data.updatedAt)
      });

      return entityResult;
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find friend by user IDs: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async findFriendsByUserId(userId: UserId, status?: FriendStatus): Promise<CustomResult<FriendEntity[]>> {
    try {
      let query = this.collection
        .where('userId', '==', userId.value);

      if (status) {
        query = query.where('status', '==', status);
      }

      const snapshot = await query.orderBy('createdAt', 'desc').get();
      const friends: FriendEntity[] = [];

      for (const doc of snapshot.docs) {
        const data = doc.data() as FriendData;
        const entityResult = FriendEntity.fromData({
          ...data,
          id: doc.id,
          requestedAt: data.requestedAt instanceof admin.firestore.Timestamp 
            ? data.requestedAt.toDate() 
            : new Date(data.requestedAt),
          respondedAt: data.respondedAt instanceof admin.firestore.Timestamp 
            ? data.respondedAt.toDate() 
            : data.respondedAt ? new Date(data.respondedAt) : undefined,
          createdAt: data.createdAt instanceof admin.firestore.Timestamp 
            ? data.createdAt.toDate() 
            : new Date(data.createdAt),
          updatedAt: data.updatedAt instanceof admin.firestore.Timestamp 
            ? data.updatedAt.toDate() 
            : new Date(data.updatedAt)
        });

        if (entityResult.success) {
          friends.push(entityResult.data);
        }
      }

      return Result.success(friends);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find friends by user ID: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async findReceivedFriendRequests(userId: UserId): Promise<CustomResult<FriendEntity[]>> {
    try {
      const query = this.collection
        .where('friendUserId', '==', userId.value)
        .where('status', '==', FriendStatus.REQUESTED)
        .orderBy('requestedAt', 'desc');

      const snapshot = await query.get();
      const requests: FriendEntity[] = [];

      for (const doc of snapshot.docs) {
        const data = doc.data() as FriendData;
        const entityResult = FriendEntity.fromData({
          ...data,
          id: doc.id,
          requestedAt: data.requestedAt instanceof admin.firestore.Timestamp 
            ? data.requestedAt.toDate() 
            : new Date(data.requestedAt),
          respondedAt: data.respondedAt instanceof admin.firestore.Timestamp 
            ? data.respondedAt.toDate() 
            : data.respondedAt ? new Date(data.respondedAt) : undefined,
          createdAt: data.createdAt instanceof admin.firestore.Timestamp 
            ? data.createdAt.toDate() 
            : new Date(data.createdAt),
          updatedAt: data.updatedAt instanceof admin.firestore.Timestamp 
            ? data.updatedAt.toDate() 
            : new Date(data.updatedAt)
        });

        if (entityResult.success) {
          requests.push(entityResult.data);
        }
      }

      return Result.success(requests);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find received friend requests: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async findSentFriendRequests(userId: UserId): Promise<CustomResult<FriendEntity[]>> {
    try {
      const query = this.collection
        .where('userId', '==', userId.value)
        .where('status', 'in', [FriendStatus.PENDING, FriendStatus.REQUESTED])
        .orderBy('requestedAt', 'desc');

      const snapshot = await query.get();
      const requests: FriendEntity[] = [];

      for (const doc of snapshot.docs) {
        const data = doc.data() as FriendData;
        const entityResult = FriendEntity.fromData({
          ...data,
          id: doc.id,
          requestedAt: data.requestedAt instanceof admin.firestore.Timestamp 
            ? data.requestedAt.toDate() 
            : new Date(data.requestedAt),
          respondedAt: data.respondedAt instanceof admin.firestore.Timestamp 
            ? data.respondedAt.toDate() 
            : data.respondedAt ? new Date(data.respondedAt) : undefined,
          createdAt: data.createdAt instanceof admin.firestore.Timestamp 
            ? data.createdAt.toDate() 
            : new Date(data.createdAt),
          updatedAt: data.updatedAt instanceof admin.firestore.Timestamp 
            ? data.updatedAt.toDate() 
            : new Date(data.updatedAt)
        });

        if (entityResult.success) {
          requests.push(entityResult.data);
        }
      }

      return Result.success(requests);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find sent friend requests: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async areUsersFriends(userId: UserId, friendUserId: UserId): Promise<CustomResult<boolean>> {
    try {
      const query = this.collection
        .where('userId', '==', userId.value)
        .where('friendUserId', '==', friendUserId.value)
        .where('status', '==', FriendStatus.ACCEPTED)
        .limit(1);

      const snapshot = await query.get();
      return Result.success(!snapshot.empty);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to check if users are friends: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async friendRequestExists(requesterId: UserId, receiverId: UserId): Promise<CustomResult<boolean>> {
    try {
      const query = this.collection
        .where('userId', '==', requesterId.value)
        .where('friendUserId', '==', receiverId.value)
        .where('status', 'in', [FriendStatus.PENDING, FriendStatus.REQUESTED])
        .limit(1);

      const snapshot = await query.get();
      return Result.success(!snapshot.empty);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to check if friend request exists: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async save(friend: FriendEntity): Promise<CustomResult<FriendEntity>> {
    try {
      const friendData = friend.toData();
      const docData = {
        userId: friendData.userId,
        friendUserId: friendData.friendUserId,
        status: friendData.status,
        requestedAt: admin.firestore.Timestamp.fromDate(friendData.requestedAt),
        respondedAt: friendData.respondedAt ? admin.firestore.Timestamp.fromDate(friendData.respondedAt) : null,
        createdAt: admin.firestore.Timestamp.fromDate(friendData.createdAt),
        updatedAt: admin.firestore.Timestamp.fromDate(friendData.updatedAt)
      };

      const docRef = await this.collection.add(docData);
      
      // 새로 생성된 ID로 엔티티 재생성
      const savedEntityResult = FriendEntity.fromData({
        ...friendData,
        id: docRef.id
      });

      return savedEntityResult;
    } catch (error) {
      return Result.failure(new InternalError(`Failed to save friend: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async update(friend: FriendEntity): Promise<CustomResult<FriendEntity>> {
    try {
      const friendData = friend.toData();
      const docData = {
        userId: friendData.userId,
        friendUserId: friendData.friendUserId,
        status: friendData.status,
        requestedAt: admin.firestore.Timestamp.fromDate(friendData.requestedAt),
        respondedAt: friendData.respondedAt ? admin.firestore.Timestamp.fromDate(friendData.respondedAt) : null,
        createdAt: admin.firestore.Timestamp.fromDate(friendData.createdAt),
        updatedAt: admin.firestore.Timestamp.fromDate(friendData.updatedAt)
      };

      await this.collection.doc(friend.id.value).update(docData);
      return Result.success(friend);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to update friend: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async delete(id: FriendId): Promise<CustomResult<void>> {
    try {
      await this.collection.doc(id.value).delete();
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to delete friend: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async deleteByUserIds(userId: UserId, friendUserId: UserId): Promise<CustomResult<void>> {
    try {
      const query = this.collection
        .where('userId', '==', userId.value)
        .where('friendUserId', '==', friendUserId.value);

      const snapshot = await query.get();
      const batch = this.db.batch();

      snapshot.docs.forEach(doc => {
        batch.delete(doc.ref);
      });

      await batch.commit();
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to delete friends by user IDs: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async deleteAllByUserId(userId: UserId): Promise<CustomResult<void>> {
    try {
      // 사용자가 요청자인 경우
      const requesterQuery = this.collection.where('userId', '==', userId.value);
      const requesterSnapshot = await requesterQuery.get();

      // 사용자가 수신자인 경우
      const receiverQuery = this.collection.where('friendUserId', '==', userId.value);
      const receiverSnapshot = await receiverQuery.get();

      const batch = this.db.batch();

      requesterSnapshot.docs.forEach(doc => {
        batch.delete(doc.ref);
      });

      receiverSnapshot.docs.forEach(doc => {
        batch.delete(doc.ref);
      });

      await batch.commit();
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to delete all friends by user ID: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async findByCriteria(criteria: FriendSearchCriteria): Promise<CustomResult<FriendEntity[]>> {
    try {
      let query: FirebaseFirestore.Query = this.collection;

      if (criteria.userId) {
        query = query.where('userId', '==', criteria.userId);
      }

      if (criteria.friendUserId) {
        query = query.where('friendUserId', '==', criteria.friendUserId);
      }

      if (criteria.status) {
        query = query.where('status', '==', criteria.status);
      }

      query = query.orderBy('createdAt', 'desc');

      if (criteria.limit) {
        query = query.limit(criteria.limit);
      }

      if (criteria.offset) {
        query = query.offset(criteria.offset);
      }

      const snapshot = await query.get();
      const friends: FriendEntity[] = [];

      for (const doc of snapshot.docs) {
        const data = doc.data() as FriendData;
        const entityResult = FriendEntity.fromData({
          ...data,
          id: doc.id,
          requestedAt: data.requestedAt instanceof admin.firestore.Timestamp 
            ? data.requestedAt.toDate() 
            : new Date(data.requestedAt),
          respondedAt: data.respondedAt instanceof admin.firestore.Timestamp 
            ? data.respondedAt.toDate() 
            : data.respondedAt ? new Date(data.respondedAt) : undefined,
          createdAt: data.createdAt instanceof admin.firestore.Timestamp 
            ? data.createdAt.toDate() 
            : new Date(data.createdAt),
          updatedAt: data.updatedAt instanceof admin.firestore.Timestamp 
            ? data.updatedAt.toDate() 
            : new Date(data.updatedAt)
        });

        if (entityResult.success) {
          friends.push(entityResult.data);
        }
      }

      return Result.success(friends);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find friends by criteria: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async countFriendsByUserId(userId: UserId): Promise<CustomResult<number>> {
    try {
      const query = this.collection
        .where('userId', '==', userId.value)
        .where('status', '==', FriendStatus.ACCEPTED);

      const snapshot = await query.get();
      return Result.success(snapshot.size);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to count friends by user ID: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async countPendingRequestsByUserId(userId: UserId): Promise<CustomResult<number>> {
    try {
      const query = this.collection
        .where('friendUserId', '==', userId.value)
        .where('status', '==', FriendStatus.REQUESTED);

      const snapshot = await query.get();
      return Result.success(snapshot.size);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to count pending requests by user ID: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }
}