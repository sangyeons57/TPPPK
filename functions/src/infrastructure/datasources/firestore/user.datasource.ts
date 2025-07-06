import { UserRepository } from '../../../domain/user/repositories/user.repository';
import { UserEntity, UserData, Email, UserName, UserStatus, UserAccountStatus, ImageUrl, UserMemo, UserFcmToken } from '../../../domain/user/entities/user.entity';
import { CustomResult, Result } from '../../../core/types';
import { InternalError } from '../../../core/errors';
import { FIRESTORE_COLLECTIONS } from '../../../core/constants';
import { getFirestore, FieldValue } from 'firebase-admin/firestore';

export class FirestoreUserDataSource implements UserRepository {
  private readonly db = getFirestore();
  private readonly collection = this.db.collection(FIRESTORE_COLLECTIONS.USERS);

  async findById(id: string): Promise<CustomResult<UserEntity | null>> {
    try {
      const doc = await this.collection.doc(id).get();
      if (!doc.exists) {
        return Result.success(null);
      }

      const data = doc.data() as UserData;
      return Result.success(this.mapToEntity(data));
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find user by id: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async findByUserId(userId: string): Promise<CustomResult<UserEntity | null>> {
    try {
      // In our case, userId is the same as the document ID (Firebase Auth UID)
      return this.findById(userId);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find user by userId: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async findByEmail(email: Email): Promise<CustomResult<UserEntity | null>> {
    try {
      const query = await this.collection.where('email', '==', email.value).limit(1).get();
      if (query.empty) {
        return Result.success(null);
      }

      const data = query.docs[0].data() as UserData;
      return Result.success(this.mapToEntity(data));
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find user by email: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async findByName(name: UserName): Promise<CustomResult<UserEntity | null>> {
    try {
      const query = await this.collection.where('name', '==', name.value).limit(1).get();
      if (query.empty) {
        return Result.success(null);
      }

      const data = query.docs[0].data() as UserData;
      return Result.success(this.mapToEntity(data));
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find user by name: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async save(user: UserEntity): Promise<CustomResult<UserEntity>> {
    try {
      const data = this.mapToData(user);
      await this.collection.doc(user.id).set(data);
      return Result.success(user);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to save user: ${error instanceof Error ? error.message : 'Unknown error'}`)
      );
    }
  }

  async update(user: UserEntity): Promise<CustomResult<UserEntity>> {
    try {
      const data = this.mapToData(user);
      data.updatedAt = FieldValue.serverTimestamp() as any;
      await this.collection.doc(user.id).update(data as any);
      return Result.success(user);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to update user: ${error instanceof Error ? error.message : 'Unknown error'}`)
      );
    }
  }

  async delete(id: string): Promise<CustomResult<void>> {
    try {
      await this.collection.doc(id).delete();
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to delete user: ${error instanceof Error ? error.message : 'Unknown error'}`)
      );
    }
  }

  async exists(userId: string): Promise<CustomResult<boolean>> {
    try {
      const doc = await this.collection.doc(userId).get();
      return Result.success(doc.exists);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to check user existence: ${error instanceof Error ? error.message : 'Unknown error'}`)
      );
    }
  }

  async findActiveUsers(limit = 50): Promise<CustomResult<UserEntity[]>> {
    try {
      const query = await this.collection
        .where('accountStatus', '==', UserAccountStatus.ACTIVE)
        .orderBy('createdAt', 'desc')
        .limit(limit)
        .get();

      const users = query.docs.map((doc) => this.mapToEntity(doc.data() as UserData));
      return Result.success(users);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to find active users: ${error instanceof Error ? error.message : 'Unknown error'}`)
      );
    }
  }

  private mapToEntity(data: UserData): UserEntity {
    return new UserEntity(
      data.id,
      new Email(data.email),
      new UserName(data.name),
      data.consentTimeStamp,
      data.profileImageUrl ? new ImageUrl(data.profileImageUrl) : undefined,
      data.memo ? new UserMemo(data.memo) : undefined,
      data.userStatus || UserStatus.OFFLINE,
      data.fcmToken ? new UserFcmToken(data.fcmToken) : undefined,
      data.accountStatus || UserAccountStatus.ACTIVE,
      data.createdAt,
      data.updatedAt
    );
  }

  private mapToData(entity: UserEntity): UserData {
    return {
      id: entity.id,
      email: entity.email.value,
      name: entity.name.value,
      consentTimeStamp: entity.consentTimeStamp,
      profileImageUrl: entity.profileImageUrl?.value,
      memo: entity.memo?.value,
      userStatus: entity.userStatus,
      fcmToken: entity.fcmToken?.value,
      accountStatus: entity.accountStatus,
      createdAt: entity.createdAt,
      updatedAt: entity.updatedAt,
    };
  }
}