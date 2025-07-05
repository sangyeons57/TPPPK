import { UserProfileEntity, Email, Username } from '../entities/user.entity';
import { CustomResult } from '../../../core/types';

export interface UserProfileRepository {
  findById(id: string): Promise<CustomResult<UserProfileEntity | null>>;
  findByUserId(userId: string): Promise<CustomResult<UserProfileEntity | null>>;
  findByEmail(email: Email): Promise<CustomResult<UserProfileEntity | null>>;
  findByUsername(username: Username): Promise<CustomResult<UserProfileEntity | null>>;
  save(userProfile: UserProfileEntity): Promise<CustomResult<UserProfileEntity>>;
  update(userProfile: UserProfileEntity): Promise<CustomResult<UserProfileEntity>>;
  delete(id: string): Promise<CustomResult<void>>;
  exists(userId: string): Promise<CustomResult<boolean>>;
  findActiveProfiles(limit?: number): Promise<CustomResult<UserProfileEntity[]>>;
}