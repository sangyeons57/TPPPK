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
      const query = this.getUserFriendsCollection(userId)
        .where("name", "==", friendUserId) // name 필드에 친구의 userId를 저장한다고 가정
        .limit(1);

      const snapshot = await query.get();

      if (snapshot.empty) {
        return Result.success(null);
      }

      const doc = snapshot.docs[0];
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
      const query = this.getUserFriendsCollection(userId)
        .where(FriendEntity.KEY_NAME, "==", friendUserId) // name 필드에 친구의 userId 저장
        .where(FriendEntity.KEY_STATUS, "==", FriendStatus.ACCEPTED)
        .limit(1);

      const snapshot = await query.get();
      return Result.success(!snapshot.empty);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to check if users are friends: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async friendRequestExists(requesterId: string, receiverId: string): Promise<CustomResult<boolean>> {
    try {
      // 요청자의 subcollection에서 확인
      const requesterQuery = this.getUserFriendsCollection(requesterId)
        .where(FriendEntity.KEY_NAME, "==", receiverId)
        .where(FriendEntity.KEY_STATUS, "in", [FriendStatus.PENDING, FriendStatus.REQUESTED])
        .limit(1);

      const requesterSnapshot = await requesterQuery.get();
 
      if (!requesterSnapshot.empty) {
        return Result.success(true);
      }

      // 수신자의 subcollection에서도 확인
      const receiverQuery = this.getUserFriendsCollection(receiverId)
        .where(FriendEntity.KEY_NAME, "==", requesterId)
        .where(FriendEntity.KEY_STATUS, "in", [FriendStatus.PENDING, FriendStatus.REQUESTED])
        .limit(1);

      const receiverSnapshot = await receiverQuery.get();
      return Result.success(!receiverSnapshot.empty);
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
      const docRef = await collection.add(docData);

      // 새로 생성된 ID로 엔티티 재생성
      const savedEntityResult = FriendEntity.fromData({
        ...friendData,
        id: docRef.id,
      });

      return savedEntityResult;
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
      const query = this.getUserFriendsCollection(userId)
        .where(FriendEntity.KEY_NAME, "==", friendUserId);

      const snapshot = await query.get();
      const batch = this.db.batch();

      snapshot.docs.forEach((doc) => {
        batch.delete(doc.ref);
      });

      await batch.commit();
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

      let query: FirebaseFirestore.Query = this.getUserFriendsCollection(criteria.userId);

      if (criteria.friendUserId) {
        query = query.where(FriendEntity.KEY_NAME, "==", criteria.friendUserId);
      }

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
