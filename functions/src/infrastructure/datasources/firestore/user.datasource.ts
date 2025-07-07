import * as admin from "firebase-admin";
import {
  UserEntity,
  UserData,
  UserAccountStatus,
  UserStatus,
} from "../../../domain/user/entities/user.entity";
import {CustomResult, Result} from "../../../core/types";
import {DatabaseError} from "../../../core/errors";
import {UserDataSource} from "../interfaces/user.datasource";
import {InternalError, ValidationError} from "../../../core/errors";

export class FirestoreUserDataSource implements UserDataSource {
  private readonly db: admin.firestore.Firestore;
  private readonly collectionName = UserEntity.COLLECTION_NAME;

  constructor() {
    this.db = admin.firestore();
  }

  async findById(id: string): Promise<CustomResult<UserEntity | null>> {
    try {
      const doc = await this.db.collection(this.collectionName).doc(id).get();

      if (!doc.exists) {
        return Result.success(null);
      }

      const userData = doc.data() as UserData;
      const userEntity = this.mapToEntity(id, userData);
      return Result.success(userEntity);
    } catch (error) {
      return Result.failure(new DatabaseError("Failed to find user by id", error instanceof Error ? error.message : String(error)));
    }
  }

  async findByUserId(userId: string): Promise<CustomResult<UserEntity | null>> {
    try {
      const doc = await this.db.collection(this.collectionName).doc(userId).get();

      if (!doc.exists) {
        return Result.success(null);
      }

      const data = doc.data() as UserData;
      const entity = UserEntity.fromData({
        ...data,
        id: doc.id,
        consentTimeStamp: data.consentTimeStamp instanceof admin.firestore.Timestamp ? data.consentTimeStamp.toDate() : new Date(data.consentTimeStamp),
        createdAt: data.createdAt instanceof admin.firestore.Timestamp ? data.createdAt.toDate() : new Date(data.createdAt),
        updatedAt: data.updatedAt instanceof admin.firestore.Timestamp ? data.updatedAt.toDate() : new Date(data.updatedAt),
      });

      return Result.success(entity);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to find user by ID: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async findByEmail(email: string): Promise<CustomResult<UserEntity | null>> {
    try {
      const query = await this.db.collection(this.collectionName).where("email", "==", email).limit(1).get();

      if (query.empty) {
        return Result.success(null);
      }

      const doc = query.docs[0];
      const data = doc.data() as UserData;
      const entity = UserEntity.fromData({
        ...data,
        id: doc.id,
        consentTimeStamp: data.consentTimeStamp instanceof admin.firestore.Timestamp ? data.consentTimeStamp.toDate() : new Date(data.consentTimeStamp),
        createdAt: data.createdAt instanceof admin.firestore.Timestamp ? data.createdAt.toDate() : new Date(data.createdAt),
        updatedAt: data.updatedAt instanceof admin.firestore.Timestamp ? data.updatedAt.toDate() : new Date(data.updatedAt),
      });

      return Result.success(entity);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to find user by email: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async findByName(name: string): Promise<CustomResult<UserEntity | null>> {
    try {
      const query = await this.db
        .collection(this.collectionName)
        .where("name", "==", name)
        .limit(1)
        .get();

      if (query.empty) {
        return Result.success(null);
      }

      const doc = query.docs[0];
      const userData = doc.data() as UserData;
      const userEntity = this.mapToEntity(doc.id, userData);
      return Result.success(userEntity);
    } catch (error) {
      return Result.failure(new DatabaseError("Failed to find user by name", error instanceof Error ? error.message : String(error)));
    }
  }

  async save(user: UserEntity): Promise<CustomResult<UserEntity>> {
    try {
      const userData = user.toData();
      const docData = {
        email: userData.email,
        name: userData.name,
        consentTimeStamp: admin.firestore.Timestamp.fromDate(userData.consentTimeStamp),
        profileImageUrl: userData.profileImageUrl || null,
        memo: userData.memo || null,
        userStatus: userData.userStatus,
        fcmToken: userData.fcmToken || null,
        accountStatus: userData.accountStatus,
        createdAt: admin.firestore.Timestamp.fromDate(userData.createdAt),
        updatedAt: admin.firestore.Timestamp.fromDate(userData.updatedAt),
      };

      const docRef = this.db.collection(this.collectionName).doc(user.id);
      await docRef.set(docData);

      return Result.success(user);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to save user: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async update(user: UserEntity): Promise<CustomResult<UserEntity>> {
    try {
      const userData = user.toData();
      const docData = {
        email: userData.email,
        name: userData.name,
        consentTimeStamp: admin.firestore.Timestamp.fromDate(userData.consentTimeStamp),
        profileImageUrl: userData.profileImageUrl || null,
        memo: userData.memo || null,
        userStatus: userData.userStatus,
        fcmToken: userData.fcmToken || null,
        accountStatus: userData.accountStatus,
        createdAt: admin.firestore.Timestamp.fromDate(userData.createdAt),
        updatedAt: admin.firestore.Timestamp.fromDate(userData.updatedAt),
      };

      const docRef = this.db.collection(this.collectionName).doc(user.id);
      await docRef.update(docData);

      return Result.success(user);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to update user: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async delete(userId: string): Promise<CustomResult<void>> {
    try {
      await this.db.collection(this.collectionName).doc(userId).delete();
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to delete user: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async exists(userId: string): Promise<CustomResult<boolean>> {
    try {
      const doc = await this.db.collection(this.collectionName).doc(userId).get();
      return Result.success(doc.exists);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to check if user exists: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async emailExists(email: string): Promise<CustomResult<boolean>> {
    try {
      const query = await this.db.collection(this.collectionName).where("email", "==", email).limit(1).get();
      return Result.success(!query.empty);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to check if email exists: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async updateUserStatus(userId: string, status: UserStatus): Promise<CustomResult<UserEntity>> {
    try {
      const userResult = await this.findByUserId(userId);
      if (!userResult.success) {
        return Result.failure(userResult.error);
      }
      if (!userResult.data) {
        return Result.failure(new ValidationError("userId", "User not found"));
      }

      const updatedUser = userResult.data.updateUserStatus(status);
      return await this.update(updatedUser);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to update user status: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async updateFcmToken(userId: string, fcmToken?: string): Promise<CustomResult<UserEntity>> {
    try {
      const userResult = await this.findByUserId(userId);
      if (!userResult.success) {
        return Result.failure(userResult.error);
      }
      if (!userResult.data) {
        return Result.failure(new ValidationError("userId", "User not found"));
      }

      const updatedUser = userResult.data.updateFcmToken(fcmToken);
      return await this.update(updatedUser);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to update FCM token: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async findActiveUsers(limit = 100): Promise<CustomResult<UserEntity[]>> {
    try {
      const query = this.db
        .collection(this.collectionName)
        .where("accountStatus", "==", UserAccountStatus.ACTIVE)
        .limit(limit);

      const snapshot = await query.get();
      const users: UserEntity[] = [];

      snapshot.docs.forEach((doc) => {
        const userData = doc.data() as UserData;
        const userEntity = this.mapToEntity(doc.id, userData);
        users.push(userEntity);
      });

      return Result.success(users);
    } catch (error) {
      return Result.failure(new DatabaseError("Failed to find active users", error instanceof Error ? error.message : String(error)));
    }
  }

  /**
   * Maps Firestore document data to a UserEntity.
   * @param {string} docId Firestore document ID
   * @param {UserData} data Firestore document data
   * @return {UserEntity} Corresponding UserEntity instance
   */
  private mapToEntity(docId: string, data: UserData): UserEntity {
    return UserEntity.fromData({
      ...data,
      id: data.id || docId, // Use data.id if available, fallback to docId
      createdAt: data.createdAt instanceof admin.firestore.Timestamp ? data.createdAt.toDate() : data.createdAt,
      updatedAt: data.updatedAt instanceof admin.firestore.Timestamp ? data.updatedAt.toDate() : data.updatedAt,
    });
  }
}
