/**
 * User repository implementation using Firestore
 */

import { firestore } from '../config/firebase';
import { Result } from '../../shared/types/Result';
import { FunctionError, FunctionErrorCode } from '../../shared/types/common';
import { UserRepository } from '../../domain/repositories';
import { User, UserData } from '../../domain/models/base/User';
import { UserId, UserEmail } from '../../domain/models/vo';
import { UserAccountStatus } from '../../domain/models/enums/UserAccountStatus';
import { COLLECTIONS } from '../../shared/constants';

export class UserRepositoryImpl implements UserRepository {
  private readonly collection: FirebaseFirestore.CollectionReference;

  constructor(collectionPath: string = COLLECTIONS.USERS) {
    this.collection = firestore.collection(collectionPath);
  }

  async findById(userId: UserId): Promise<Result<User, Error>> {
    try {
      const doc = await this.collection.doc(userId.value).get();
      
      if (!doc.exists) {
        return Result.failure(new FunctionError(
          FunctionErrorCode.NOT_FOUND,
          'User not found',
          { userId: userId.value }
        ));
      }

      const userData = this.mapDocumentToUser(doc);
      const user = new User(userData);
      
      return Result.success(user);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  async findByEmail(email: UserEmail): Promise<Result<User, Error>> {
    try {
      const query = await this.collection.where('email', '==', email.value).limit(1).get();
      
      if (query.empty) {
        return Result.failure(new FunctionError(
          FunctionErrorCode.NOT_FOUND,
          'User not found',
          { email: email.value }
        ));
      }

      const doc = query.docs[0];
      const userData = this.mapDocumentToUser(doc);
      const user = new User(userData);
      
      return Result.success(user);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  async existsByEmail(email: UserEmail): Promise<Result<boolean, Error>> {
    try {
      const query = await this.collection.where('email', '==', email.value).limit(1).get();
      return Result.success(!query.empty);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  async existsById(userId: UserId): Promise<Result<boolean, Error>> {
    try {
      const doc = await this.collection.doc(userId.value).get();
      return Result.success(doc.exists);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  async save(user: User): Promise<Result<User, Error>> {
    try {
      const userData = this.mapUserToDocument(user);
      await this.collection.doc(user.id).set(userData);
      
      return Result.success(user);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  async update(user: User): Promise<Result<User, Error>> {
    try {
      const userData = this.mapUserToDocument(user);
      await this.collection.doc(user.id).update(userData);
      
      return Result.success(user);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  async deleteById(userId: UserId): Promise<Result<void, Error>> {
    try {
      await this.collection.doc(userId.value).delete();
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  async findByNamePattern(namePattern: string, limit: number = 50): Promise<Result<User[], Error>> {
    try {
      // Firestore doesn't support full-text search, so we use array-contains for keywords
      // This is a simplified implementation
      const query = await this.collection
        .where('name', '>=', namePattern)
        .where('name', '<=', namePattern + '\uf8ff')
        .limit(limit)
        .get();

      const users = query.docs.map(doc => {
        const userData = this.mapDocumentToUser(doc);
        return new User(userData);
      });

      return Result.success(users);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  async findByAccountStatus(status: UserAccountStatus, limit: number = 100): Promise<Result<User[], Error>> {
    try {
      const query = await this.collection
        .where('accountStatus', '==', status)
        .limit(limit)
        .get();

      const users = query.docs.map(doc => {
        const userData = this.mapDocumentToUser(doc);
        return new User(userData);
      });

      return Result.success(users);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  async updateLastLogin(userId: UserId, loginTime: Date): Promise<Result<void, Error>> {
    try {
      await this.collection.doc(userId.value).update({
        lastLoginAt: loginTime,
        updatedAt: new Date(),
      });
      
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  async updateFcmToken(userId: UserId, fcmToken?: string): Promise<Result<void, Error>> {
    try {
      await this.collection.doc(userId.value).update({
        fcmToken: fcmToken || null,
        updatedAt: new Date(),
      });
      
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  async countByStatus(status: UserAccountStatus): Promise<Result<number, Error>> {
    try {
      const query = await this.collection.where('accountStatus', '==', status).count().get();
      return Result.success(query.data().count);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  async findRecentlyRegistered(days: number, limit: number = 100): Promise<Result<User[], Error>> {
    try {
      const cutoffDate = new Date();
      cutoffDate.setDate(cutoffDate.getDate() - days);

      const query = await this.collection
        .where('createdAt', '>=', cutoffDate)
        .orderBy('createdAt', 'desc')
        .limit(limit)
        .get();

      const users = query.docs.map(doc => {
        const userData = this.mapDocumentToUser(doc);
        return new User(userData);
      });

      return Result.success(users);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  async findInactiveUsers(days: number, limit: number = 100): Promise<Result<User[], Error>> {
    try {
      const cutoffDate = new Date();
      cutoffDate.setDate(cutoffDate.getDate() - days);

      const query = await this.collection
        .where('lastLoginAt', '<', cutoffDate)
        .orderBy('lastLoginAt', 'asc')
        .limit(limit)
        .get();

      const users = query.docs.map(doc => {
        const userData = this.mapDocumentToUser(doc);
        return new User(userData);
      });

      return Result.success(users);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  private mapDocumentToUser(doc: FirebaseFirestore.QueryDocumentSnapshot): UserData {
    const data = doc.data();
    
    return {
      id: doc.id,
      email: data.email,
      name: data.name,
      profileImageUrl: data.profileImageUrl,
      memo: data.memo,
      accountStatus: data.accountStatus,
      emailVerified: data.emailVerified,
      fcmToken: data.fcmToken,
      lastLoginAt: data.lastLoginAt?.toDate(),
      consentTimestamp: data.consentTimestamp?.toDate(),
      createdAt: data.createdAt?.toDate(),
      updatedAt: data.updatedAt?.toDate(),
    };
  }

  private mapUserToDocument(user: User): Record<string, any> {
    return {
      email: user.email.value,
      name: user.name.value,
      profileImageUrl: user.profileImageUrl || null,
      memo: user.memo || null,
      accountStatus: user.accountStatus,
      emailVerified: user.emailVerified,
      fcmToken: user.fcmToken || null,
      lastLoginAt: user.lastLoginAt || null,
      consentTimestamp: user.consentTimestamp,
      createdAt: user.createdAt,
      updatedAt: user.updatedAt,
    };
  }
}