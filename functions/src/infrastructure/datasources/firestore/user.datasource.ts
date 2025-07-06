import * as admin from "firebase-admin";
import {
  UserEntity,
  UserData,
  UserAccountStatus,
} from "../../../domain/user/entities/user.entity";
import {UserDataSource} from "../interfaces/user.datasource";
import {CustomResult, Result} from "../../../core/types";
import {DatabaseError} from "../../../core/errors";

export class FirestoreUserDataSource implements UserDataSource {
  private readonly db: admin.firestore.Firestore;
  private readonly collectionName = "users";

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
      const query = await this.db
        .collection(this.collectionName)
        .where("id", "==", userId)
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
      return Result.failure(new DatabaseError("Failed to find user by userId", error instanceof Error ? error.message : String(error)));
    }
  }

  async findByEmail(email: string): Promise<CustomResult<UserEntity | null>> {
    try {
      const query = await this.db
        .collection(this.collectionName)
        .where("email", "==", email)
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
      return Result.failure(new DatabaseError("Failed to find user by email", error instanceof Error ? error.message : String(error)));
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
      const docRef = this.db.collection(this.collectionName).doc(user.id);
      
      await docRef.set({
        ...userData,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      // Return the saved user with updated timestamps
      const savedDoc = await docRef.get();
      const savedData = savedDoc.data() as UserData;
      const savedEntity = this.mapToEntity(user.id, savedData);
      
      return Result.success(savedEntity);
    } catch (error) {
      return Result.failure(new DatabaseError("Failed to save user", error instanceof Error ? error.message : String(error)));
    }
  }

  async update(user: UserEntity): Promise<CustomResult<UserEntity>> {
    try {
      const userData = user.toData();
      const docRef = this.db.collection(this.collectionName).doc(user.id);
      
      await docRef.update({
        ...userData,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      // Return the updated user with new timestamp
      const updatedDoc = await docRef.get();
      const updatedData = updatedDoc.data() as UserData;
      const updatedEntity = this.mapToEntity(user.id, updatedData);
      
      return Result.success(updatedEntity);
    } catch (error) {
      return Result.failure(new DatabaseError("Failed to update user", error instanceof Error ? error.message : String(error)));
    }
  }

  async delete(id: string): Promise<CustomResult<void>> {
    try {
      await this.db.collection(this.collectionName).doc(id).delete();
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(new DatabaseError("Failed to delete user", error instanceof Error ? error.message : String(error)));
    }
  }

  async exists(userId: string): Promise<CustomResult<boolean>> {
    try {
      const query = await this.db
        .collection(this.collectionName)
        .where("id", "==", userId)
        .limit(1)
        .get();

      return Result.success(!query.empty);
    } catch (error) {
      return Result.failure(new DatabaseError("Failed to check user existence", error instanceof Error ? error.message : String(error)));
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

      snapshot.docs.forEach(doc => {
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
   * Maps Firestore document data to UserEntity
   */
  private mapToEntity(docId: string, data: UserData): UserEntity {
    return UserEntity.fromData({
      ...data,
      id: data.id || docId, // Use data.id if available, fallback to docId
      createdAt: data.createdAt instanceof admin.firestore.Timestamp 
        ? data.createdAt.toDate() 
        : data.createdAt,
      updatedAt: data.updatedAt instanceof admin.firestore.Timestamp 
        ? data.updatedAt.toDate() 
        : data.updatedAt,
    });
  }
}