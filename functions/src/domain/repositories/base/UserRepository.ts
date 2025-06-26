/**
 * User repository interface
 * Defines contracts for user-related data operations
 */

import { Result } from '../../../shared/types/Result';
import { Repository } from '../Repository';
import { User } from '../../models/base/User';
import { UserId, UserEmail } from '../../models/vo';
import { UserAccountStatus } from '../../models/enums/UserAccountStatus';

export interface UserRepository extends Repository {
  /**
   * Find a user by ID
   * @param userId User ID
   * @returns Result containing User on success
   */
  findById(userId: UserId): Promise<Result<User, Error>>;

  /**
   * Find a user by email
   * @param email User email
   * @returns Result containing User on success
   */
  findByEmail(email: UserEmail): Promise<Result<User, Error>>;

  /**
   * Check if a user exists by email
   * @param email User email
   * @returns Result containing existence status
   */
  existsByEmail(email: UserEmail): Promise<Result<boolean, Error>>;

  /**
   * Check if a user exists by ID
   * @param userId User ID
   * @returns Result containing existence status
   */
  existsById(userId: UserId): Promise<Result<boolean, Error>>;

  /**
   * Save a new user
   * @param user User to save
   * @returns Result containing saved User
   */
  save(user: User): Promise<Result<User, Error>>;

  /**
   * Update an existing user
   * @param user User to update
   * @returns Result containing updated User
   */
  update(user: User): Promise<Result<User, Error>>;

  /**
   * Delete a user by ID
   * @param userId User ID
   * @returns Result indicating success or failure
   */
  deleteById(userId: UserId): Promise<Result<void, Error>>;

  /**
   * Find users by name pattern
   * @param namePattern Name pattern to search
   * @param limit Maximum number of results
   * @returns Result containing array of Users
   */
  findByNamePattern(namePattern: string, limit?: number): Promise<Result<User[], Error>>;

  /**
   * Find users by account status
   * @param status Account status to filter by
   * @param limit Maximum number of results
   * @returns Result containing array of Users
   */
  findByAccountStatus(status: UserAccountStatus, limit?: number): Promise<Result<User[], Error>>;

  /**
   * Update user's last login timestamp
   * @param userId User ID
   * @param loginTime Login timestamp
   * @returns Result indicating success or failure
   */
  updateLastLogin(userId: UserId, loginTime: Date): Promise<Result<void, Error>>;

  /**
   * Update user's FCM token
   * @param userId User ID
   * @param fcmToken FCM token
   * @returns Result indicating success or failure
   */
  updateFcmToken(userId: UserId, fcmToken?: string): Promise<Result<void, Error>>;

  /**
   * Get user count by status
   * @param status Account status
   * @returns Result containing user count
   */
  countByStatus(status: UserAccountStatus): Promise<Result<number, Error>>;

  /**
   * Find recently registered users
   * @param days Number of days to look back
   * @param limit Maximum number of results
   * @returns Result containing array of Users
   */
  findRecentlyRegistered(days: number, limit?: number): Promise<Result<User[], Error>>;

  /**
   * Find inactive users
   * @param days Number of days since last login
   * @param limit Maximum number of results
   * @returns Result containing array of Users
   */
  findInactiveUsers(days: number, limit?: number): Promise<Result<User[], Error>>;
}