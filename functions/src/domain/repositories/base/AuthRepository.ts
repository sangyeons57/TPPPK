/**
 * Authentication repository interface
 * Defines contracts for authentication-related data operations
 */

import { Result } from '../../../shared/types/Result';
import { Repository } from '../Repository';
import { UserSession } from '../../models/data/UserSession';
import { UserEmail } from '../../models/vo';

export interface AuthRepository extends Repository {
  /**
   * Authenticate a user with email and password
   * @param email User email
   * @param password User password
   * @returns Result containing UserSession on success
   */
  login(email: UserEmail, password: string): Promise<Result<UserSession, Error>>;

  /**
   * Check if a user is currently logged in
   * @returns True if user is logged in
   */
  isLoggedIn(): Promise<boolean>;

  /**
   * Log out the current user
   * @returns Result indicating success or failure
   */
  logout(): Promise<Result<void, Error>>;

  /**
   * Register a new user account
   * @param email User email
   * @param password User password
   * @returns Result containing user ID on success
   */
  signup(email: string, password: string): Promise<Result<string, Error>>;

  /**
   * Request a password reset code
   * @param email User email
   * @returns Result indicating success or failure
   */
  requestPasswordResetCode(email: string): Promise<Result<void, Error>>;

  /**
   * Send email verification to current user
   * @returns Result indicating success or failure
   */
  sendEmailVerification(): Promise<Result<void, Error>>;

  /**
   * Check if current user's email is verified
   * @returns Result containing verification status
   */
  checkEmailVerification(): Promise<Result<boolean, Error>>;

  /**
   * Update user password
   * @param newPassword New password
   * @returns Result indicating success or failure
   */
  updatePassword(newPassword: string): Promise<Result<void, Error>>;

  /**
   * Delete current user account
   * @returns Result indicating success or failure
   */
  withdrawCurrentUser(): Promise<Result<void, Error>>;

  /**
   * Get current user session
   * @returns Result containing current user session
   */
  getCurrentUserSession(): Promise<Result<UserSession, Error>>;

  /**
   * Get error message for login failures
   * @param exception The thrown exception
   * @returns Human-readable error message
   */
  getLoginErrorMessage(exception: Error): Promise<string>;

  /**
   * Get error message for signup failures
   * @param exception The thrown exception
   * @returns Human-readable error message
   */
  getSignUpErrorMessage(exception: Error): Promise<string>;

  /**
   * Get error message for password reset failures
   * @param exception The thrown exception
   * @returns Human-readable error message
   */
  getPasswordResetErrorMessage(exception: Error): Promise<string>;
}