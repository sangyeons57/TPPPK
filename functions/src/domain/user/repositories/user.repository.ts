import { UserEntity, Email, UserName } from '../entities/user.entity';
import { CustomResult } from '../../../core/types';

export interface UserRepository {
  findById(id: string): Promise<CustomResult<UserEntity | null>>;
  findByUserId(userId: string): Promise<CustomResult<UserEntity | null>>;
  findByEmail(email: Email): Promise<CustomResult<UserEntity | null>>;
  findByName(name: UserName): Promise<CustomResult<UserEntity | null>>;
  save(user: UserEntity): Promise<CustomResult<UserEntity>>;
  update(user: UserEntity): Promise<CustomResult<UserEntity>>;
  delete(id: string): Promise<CustomResult<void>>;
  exists(userId: string): Promise<CustomResult<boolean>>;
  findActiveUsers(limit?: number): Promise<CustomResult<UserEntity[]>>;
}