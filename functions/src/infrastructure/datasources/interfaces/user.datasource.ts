import {UserEntity} from "../../../domain/user/entities/user.entity";
import {CustomResult} from "../../../core/types";

/**
 * User data source interface for data persistence operations
 */
export interface UserDataSource {
  /**
   * Find user by document ID
   */
  findById(id: string): Promise<CustomResult<UserEntity | null>>;

  /**
   * Find user by user ID (may be same as document ID)
   */
  findByUserId(userId: string): Promise<CustomResult<UserEntity | null>>;

  /**
   * Find user by email address
   */
  findByEmail(email: string): Promise<CustomResult<UserEntity | null>>;

  /**
   * Find user by name
   */
  findByName(name: string): Promise<CustomResult<UserEntity | null>>;

  /**
   * Save a new user entity
   */
  save(user: UserEntity): Promise<CustomResult<UserEntity>>;

  /**
   * Update an existing user entity
   */
  update(user: UserEntity): Promise<CustomResult<UserEntity>>;

  /**
   * Delete a user by ID
   */
  delete(id: string): Promise<CustomResult<void>>;

  /**
   * Check if user exists by user ID
   */
  exists(userId: string): Promise<CustomResult<boolean>>;

  /**
   * Find active users with optional limit
   */
  findActiveUsers(limit?: number): Promise<CustomResult<UserEntity[]>>;

  /**
   * Remove a project wrapper from a user's collection
   */
  removeProjectWrapper(userId: string, projectId: string): Promise<CustomResult<void>>;
}