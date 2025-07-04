import { UserProfileRepository } from '../../domain/user/userProfile.repository';
import { UserProfileEntity, Email, Username, UserProfileImage } from '../../domain/user/user.entity';
import { CustomResult, Result } from '../../core/types';
import { NotFoundError, InternalError } from '../../core/errors';
import { FIRESTORE_COLLECTIONS } from '../../core/constants';
import { getFirestore, FieldValue } from 'firebase-admin/firestore';

interface UserProfileData {
  id: string;
  userId: string;
  username: string;
  email: string;
  profileImage?: string;
  bio?: string;
  displayName?: string;
  isActive: boolean;
  createdAt: FirebaseFirestore.Timestamp;
  updatedAt: FirebaseFirestore.Timestamp;
}

export class FirestoreUserProfileDataSource implements UserProfileRepository {
  private readonly db = getFirestore();
  private readonly collection = this.db.collection(FIRESTORE_COLLECTIONS.USER_PROFILES);

  async findById(id: string): Promise<CustomResult<UserProfileEntity | null>> {
    try {
      const doc = await this.collection.doc(id).get();
      if (!doc.exists) {
        return Result.success(null);
      }
      
      const data = doc.data() as UserProfileData;
      return Result.success(this.mapToEntity(data));
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find user profile by id: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async findByUserId(userId: string): Promise<CustomResult<UserProfileEntity | null>> {
    try {
      const query = await this.collection.where('userId', '==', userId).limit(1).get();
      if (query.empty) {
        return Result.success(null);
      }
      
      const data = query.docs[0].data() as UserProfileData;
      return Result.success(this.mapToEntity(data));
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find user profile by userId: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async findByEmail(email: Email): Promise<CustomResult<UserProfileEntity | null>> {
    try {
      const query = await this.collection.where('email', '==', email.value).limit(1).get();
      if (query.empty) {
        return Result.success(null);
      }
      
      const data = query.docs[0].data() as UserProfileData;
      return Result.success(this.mapToEntity(data));
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find user profile by email: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async findByUsername(username: Username): Promise<CustomResult<UserProfileEntity | null>> {
    try {
      const query = await this.collection.where('username', '==', username.value).limit(1).get();
      if (query.empty) {
        return Result.success(null);
      }
      
      const data = query.docs[0].data() as UserProfileData;
      return Result.success(this.mapToEntity(data));
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find user profile by username: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async save(userProfile: UserProfileEntity): Promise<CustomResult<UserProfileEntity>> {
    try {
      const data = this.mapToData(userProfile);
      await this.collection.doc(userProfile.id).set(data);
      return Result.success(userProfile);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to save user profile: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async update(userProfile: UserProfileEntity): Promise<CustomResult<UserProfileEntity>> {
    try {
      const data = this.mapToData(userProfile);
      data.updatedAt = FieldValue.serverTimestamp() as any;
      await this.collection.doc(userProfile.id).update(data);
      return Result.success(userProfile);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to update user profile: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async delete(id: string): Promise<CustomResult<void>> {
    try {
      await this.collection.doc(id).delete();
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to delete user profile: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async exists(userId: string): Promise<CustomResult<boolean>> {
    try {
      const query = await this.collection.where('userId', '==', userId).limit(1).get();
      return Result.success(!query.empty);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to check user profile existence: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async findActiveProfiles(limit: number = 50): Promise<CustomResult<UserProfileEntity[]>> {
    try {
      const query = await this.collection
        .where('isActive', '==', true)
        .orderBy('createdAt', 'desc')
        .limit(limit)
        .get();
      
      const profiles = query.docs.map(doc => this.mapToEntity(doc.data() as UserProfileData));
      return Result.success(profiles);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find active profiles: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  private mapToEntity(data: UserProfileData): UserProfileEntity {
    return new UserProfileEntity(
      data.id,
      data.userId,
      new Username(data.username),
      new Email(data.email),
      data.createdAt.toDate(),
      data.updatedAt.toDate(),
      data.isActive,
      data.profileImage ? new UserProfileImage(data.profileImage) : undefined,
      data.bio,
      data.displayName
    );
  }

  private mapToData(entity: UserProfileEntity): UserProfileData {
    return {
      id: entity.id,
      userId: entity.userId,
      username: entity.username.value,
      email: entity.email.value,
      profileImage: entity.profileImage?.value,
      bio: entity.bio,
      displayName: entity.displayName,
      isActive: entity.isActive,
      createdAt: entity.createdAt as any,
      updatedAt: entity.updatedAt as any,
    };
  }
}