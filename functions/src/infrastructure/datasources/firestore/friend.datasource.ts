import * as admin from "firebase-admin";
import {CustomResult, Result} from "../../../core/types";
import {InternalError} from "../../../core/errors";
import {
  FriendDatasource,
  FriendSearchCriteria,
} from "../interfaces/friend.datasource";
import {
  FriendEntity,
  FriendStatus,
  FriendData,
} from "../../../domain/friend/entities/friend.entity";
import {UserEntity} from "../../../domain/user/entities/user.entity";

export class FirestoreFriendDataSource implements FriendDatasource {
  private readonly db = admin.firestore();

  /**
   * Helper method to convert Firestore document data to FriendEntity
   */
  private createFriendEntity(doc: FirebaseFirestore.QueryDocumentSnapshot | FirebaseFirestore.DocumentSnapshot): CustomResult<FriendEntity> {
    const data = doc.data() as FriendData;
    return FriendEntity.fromData({
      ...data,
      id: doc.id,
      requestedAt: data.requestedAt instanceof admin.firestore.Timestamp ? data.requestedAt.toDate() : data.requestedAt,
      acceptedAt: data.acceptedAt instanceof admin.firestore.Timestamp ? data.acceptedAt.toDate() : data.acceptedAt,
      createdAt: data.createdAt instanceof admin.firestore.Timestamp ? data.createdAt.toDate() : new Date(data.createdAt),
      updatedAt: data.updatedAt instanceof admin.firestore.Timestamp ? data.updatedAt.toDate() : new Date(data.updatedAt),
    });
  }

  /**
   * 특정 사용자의 friends subcollection 참조를 반환합니다
   * @param {string} userId - 사용자 ID
   * @return {FirebaseFirestore.CollectionReference} Friends subcollection 참조
   */
  private getUserFriendsCollection(userId: string) {
    return this.db.collection(UserEntity.COLLECTION_NAME)
      .doc(userId)
      .collection(FriendEntity.COLLECTION_NAME);
  }

  async findById(id: string): Promise<CustomResult<FriendEntity | null>> {
    try {
      // friendId는 단순 문서 ID이므로 모든 사용자의 subcollection에서 검색해야 함
      // 현재는 첫 번째 사용자에서만 검색 (개선 필요)
      const usersSnapshot = await this.db.collection(UserEntity.COLLECTION_NAME).limit(1).get();

      if (usersSnapshot.empty) {
        return Result.success(null);
      }

      const userId = usersSnapshot.docs[0].id;
      const doc = await this.getUserFriendsCollection(userId).doc(id).get();

      if (!doc.exists) {
        return Result.success(null);
      }

      return this.createFriendEntity(doc);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to find friend by ID: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async findByUserIds(userId: string, friendUserId: string): Promise<CustomResult<FriendEntity | null>> {
    try {
      // Use friendUserId as document ID to find the friend document
      const doc = await this.getUserFriendsCollection(userId).doc(friendUserId).get();

      if (!doc.exists) {
        return Result.success(null);
      }

      return this.createFriendEntity(doc);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to find friend by user IDs: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async findFriendsByUserId(userId: string, status?: FriendStatus): Promise<CustomResult<FriendEntity[]>> {
    try {
      let query: FirebaseFirestore.Query = this.getUserFriendsCollection(userId);

      if (status) {
        query = query.where(FriendEntity.KEY_STATUS, "==", status);
      }

      const snapshot = await query.orderBy("createdAt", "desc").get();
      const friends: FriendEntity[] = [];

      for (const doc of snapshot.docs) {
        const entityResult = this.createFriendEntity(doc);
        if (entityResult.success) {
          friends.push(entityResult.data);
        }
      }

      return Result.success(friends);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to find friends by user ID: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async findReceivedFriendRequests(userId: string): Promise<CustomResult<FriendEntity[]>> {
    try {
      const query = this.getUserFriendsCollection(userId)
        .where(FriendEntity.KEY_STATUS, "==", FriendStatus.PENDING)
        .orderBy(FriendEntity.KEY_REQUESTED_AT, "desc");

      const snapshot = await query.get();
      const requests: FriendEntity[] = [];

      for (const doc of snapshot.docs) {
        const entityResult = this.createFriendEntity(doc);

        if (entityResult.success) {
          requests.push(entityResult.data);
        }
      }

      return Result.success(requests);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to find received friend requests: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async findSentFriendRequests(userId: string): Promise<CustomResult<FriendEntity[]>> {
    try {
      const query = this.getUserFriendsCollection(userId)
        .where(FriendEntity.KEY_STATUS, "==", FriendStatus.REQUESTED)
        .orderBy(FriendEntity.KEY_REQUESTED_AT, "desc");

      const snapshot = await query.get();
      const requests: FriendEntity[] = [];

      for (const doc of snapshot.docs) {
        const entityResult = this.createFriendEntity(doc);

        if (entityResult.success) {
          requests.push(entityResult.data);
        }
      }

      return Result.success(requests);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to find sent friend requests: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async areUsersFriends(userId: string, friendUserId: string): Promise<CustomResult<boolean>> {
    try {
      // Use friendUserId as document ID to check if friendship exists
      const doc = await this.getUserFriendsCollection(userId).doc(friendUserId).get();
      
      if (!doc.exists) {
        return Result.success(false);
      }

      const friendEntityResult = this.createFriendEntity(doc);
      if (!friendEntityResult.success) {
        return Result.success(false);
      }

      return Result.success(friendEntityResult.data.status === FriendStatus.ACCEPTED);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to check if users are friends: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async friendRequestExists(requesterId: string, receiverId: string): Promise<CustomResult<boolean>> {
    try {
      // Check requester's subcollection using receiverId as document ID
      const requesterDoc = await this.getUserFriendsCollection(requesterId).doc(receiverId).get();
      if (requesterDoc.exists) {
        const friendEntityResult = this.createFriendEntity(requesterDoc);
        if (friendEntityResult.success) {
          const status = friendEntityResult.data.status;
          if (status === FriendStatus.PENDING || status === FriendStatus.REQUESTED) {
            return Result.success(true);
          }
        }
      }

      // Check receiver's subcollection using requesterId as document ID
      const receiverDoc = await this.getUserFriendsCollection(receiverId).doc(requesterId).get();
      if (receiverDoc.exists) {
        const friendEntityResult = this.createFriendEntity(receiverDoc);
        if (friendEntityResult.success) {
          const status = friendEntityResult.data.status;
          if (status === FriendStatus.PENDING || status === FriendStatus.REQUESTED) {
            return Result.success(true);
          }
        }
      }

      return Result.success(false);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to check if friend request exists: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async save(userId: string, friend: FriendEntity): Promise<CustomResult<FriendEntity>> {
    try {
      const friendData = friend.toData();
      const docData = {
        [FriendEntity.KEY_NAME]: friendData.name,
        [FriendEntity.KEY_PROFILE_IMAGE_URL]: friendData.profileImageUrl || null,
        [FriendEntity.KEY_STATUS]: friendData.status,
        [FriendEntity.KEY_REQUESTED_AT]: friendData.requestedAt ? admin.firestore.Timestamp.fromDate(friendData.requestedAt) : null,
        [FriendEntity.KEY_ACCEPTED_AT]: friendData.acceptedAt ? admin.firestore.Timestamp.fromDate(friendData.acceptedAt) : null,
        createdAt: admin.firestore.Timestamp.fromDate(friendData.createdAt),
        updatedAt: admin.firestore.Timestamp.fromDate(friendData.updatedAt),
      };

      // Use the userId parameter to determine which user's collection to save to
      const collection = this.getUserFriendsCollection(userId);
      // Use custom ID (friend.id) instead of auto-generated ID
      const docRef = collection.doc(friend.id);
      await docRef.set(docData);

      // Return the original friend entity with the same ID
      return Result.success(friend);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to save friend: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async update(userId: string, friend: FriendEntity): Promise<CustomResult<FriendEntity>> {
    try {
      const friendData = friend.toData();
      const docData = {
        [FriendEntity.KEY_NAME]: friendData.name,
        [FriendEntity.KEY_PROFILE_IMAGE_URL]: friendData.profileImageUrl || null,
        [FriendEntity.KEY_STATUS]: friendData.status,
        [FriendEntity.KEY_REQUESTED_AT]: friendData.requestedAt ? admin.firestore.Timestamp.fromDate(friendData.requestedAt) : null,
        [FriendEntity.KEY_ACCEPTED_AT]: friendData.acceptedAt ? admin.firestore.Timestamp.fromDate(friendData.acceptedAt) : null,
        createdAt: admin.firestore.Timestamp.fromDate(friendData.createdAt),
        updatedAt: admin.firestore.Timestamp.fromDate(friendData.updatedAt),
      };

      // Use the userId parameter to determine which user's collection to update
      const docRef = this.getUserFriendsCollection(userId).doc(friend.id);
      await docRef.update(docData);
      
      return Result.success(friend);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to update friend: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async delete(id: string): Promise<CustomResult<void>> {
    try {
      // 모든 사용자의 subcollection에서 해당 ID를 찾아 삭제해야 함
      // 현재는 간단히 구현 (개선 필요)
      const usersSnapshot = await this.db.collection(UserEntity.COLLECTION_NAME).get();
      const batch = this.db.batch();

      for (const userDoc of usersSnapshot.docs) {
        const friendRef = this.getUserFriendsCollection(userDoc.id).doc(id);
        batch.delete(friendRef);
      }

      await batch.commit();
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to delete friend: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async deleteByUserIds(userId: string, friendUserId: string): Promise<CustomResult<void>> {
    try {
      // Use friendUserId as document ID for direct deletion
      const docRef = this.getUserFriendsCollection(userId).doc(friendUserId);
      await docRef.delete();
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to delete friends by user IDs: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async deleteAllByUserId(userId: string): Promise<CustomResult<void>> {
    try {
      const snapshot = await this.getUserFriendsCollection(userId).get();
      const batch = this.db.batch();

      snapshot.docs.forEach((doc) => {
        batch.delete(doc.ref);
      });

      await batch.commit();
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to delete all friends by user ID: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async findByCriteria(criteria: FriendSearchCriteria): Promise<CustomResult<FriendEntity[]>> {
    try {
      if (!criteria.userId) {
        return Result.failure(new InternalError("userId is required for subcollection query"));
      }

      // If searching for specific friendUserId, use document ID for direct access
      if (criteria.friendUserId) {
        const doc = await this.getUserFriendsCollection(criteria.userId).doc(criteria.friendUserId).get();
        if (!doc.exists) {
          return Result.success([]);
        }
        
        const entityResult = this.createFriendEntity(doc);
        if (!entityResult.success) {
          return Result.success([]);
        }
        
        // Check status filter if provided
        if (criteria.status && entityResult.data.status !== criteria.status) {
          return Result.success([]);
        }
        
        return Result.success([entityResult.data]);
      }

      // General query for all friends with optional status filter
      let query: FirebaseFirestore.Query = this.getUserFriendsCollection(criteria.userId);

      if (criteria.status) {
        query = query.where(FriendEntity.KEY_STATUS, "==", criteria.status);
      }

      query = query.orderBy("createdAt", "desc");

      if (criteria.limit) {
        query = query.limit(criteria.limit);
      }

      if (criteria.offset) {
        query = query.offset(criteria.offset);
      }

      const snapshot = await query.get();
      const friends: FriendEntity[] = [];

      for (const doc of snapshot.docs) {
        const entityResult = this.createFriendEntity(doc);

        if (entityResult.success) {
          friends.push(entityResult.data);
        }
      }

      return Result.success(friends);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to find friends by criteria: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async countFriendsByUserId(userId: string): Promise<CustomResult<number>> {
    try {
      const query = this.getUserFriendsCollection(userId)
        .where(FriendEntity.KEY_STATUS, "==", FriendStatus.ACCEPTED);

      const snapshot = await query.get();
      return Result.success(snapshot.size);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to count friends by user ID: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async countPendingRequestsByUserId(userId: string): Promise<CustomResult<number>> {
    try {
      const query = this.getUserFriendsCollection(userId)
        .where(FriendEntity.KEY_STATUS, "==", FriendStatus.PENDING);

      const snapshot = await query.get();
      return Result.success(snapshot.size);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to count pending requests by user ID: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }
}
