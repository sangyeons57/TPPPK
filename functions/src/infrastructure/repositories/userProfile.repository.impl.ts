import { UserProfileRepository } from '../../domain/user/repositories/userProfile.repository';
import { UserProfileDatasource } from '../datasources/interfaces/userProfile.datasource';
import { UserProfileEntity, Email, Username } from '../../domain/user/entities/user.entity';
import { CustomResult } from '../../core/types';

/**
 * UserProfile Repository 구현체
 * UserProfileDatasource를 사용하여 Repository 인터페이스를 구현
 */
export class UserProfileRepositoryImpl implements UserProfileRepository {
  constructor(private readonly datasource: UserProfileDatasource) {}

  async findById(id: string): Promise<CustomResult<UserProfileEntity | null>> {
    return this.datasource.findById(id);
  }

  async findByUserId(userId: string): Promise<CustomResult<UserProfileEntity | null>> {
    return this.datasource.findByUserId(userId);
  }

  async findByEmail(email: Email): Promise<CustomResult<UserProfileEntity | null>> {
    return this.datasource.findByEmail(email);
  }

  async findByUsername(username: Username): Promise<CustomResult<UserProfileEntity | null>> {
    return this.datasource.findByUsername(username);
  }

  async save(userProfile: UserProfileEntity): Promise<CustomResult<UserProfileEntity>> {
    return this.datasource.save(userProfile);
  }

  async update(userProfile: UserProfileEntity): Promise<CustomResult<UserProfileEntity>> {
    return this.datasource.update(userProfile);
  }

  async delete(id: string): Promise<CustomResult<void>> {
    return this.datasource.delete(id);
  }

  async exists(userId: string): Promise<CustomResult<boolean>> {
    return this.datasource.exists(userId);
  }

  async findActiveProfiles(limit?: number): Promise<CustomResult<UserProfileEntity[]>> {
    return this.datasource.findActiveProfiles(limit);
  }
}