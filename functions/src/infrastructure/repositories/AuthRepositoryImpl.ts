/**
 * Authentication repository implementation using Firebase Auth
 */

import { auth } from '../config/firebase';
import { Result } from '../../shared/types/Result';
import { FunctionError, FunctionErrorCode } from '../../shared/types/common';
import { AuthRepository } from '../../domain/repositories';
import { UserSession } from '../../domain/models/data/UserSession';
import { UserEmail } from '../../domain/models/vo';
import { AUTH } from '../../shared/constants';

export class AuthRepositoryImpl implements AuthRepository {
  async login(email: UserEmail, password: string): Promise<Result<UserSession, Error>> {
    try {
      // Note: Firebase Admin SDK doesn't support password authentication directly
      // This would typically be handled by the client SDK
      // For server-side, we would validate a token instead
      throw new FunctionError(
        FunctionErrorCode.INTERNAL_ERROR,
        'Direct password authentication not supported in Firebase Functions',
        { method: 'login', email: email.value }
      );
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  async isLoggedIn(): Promise<boolean> {
    try {
      // In Firebase Functions, we check if there's a valid auth context
      // This would be determined by the function call context
      return false; // Placeholder implementation
    } catch (error) {
      return false;
    }
  }

  async logout(): Promise<Result<void, Error>> {
    try {
      // In Firebase Functions, logout is typically handled client-side
      // Server-side can revoke tokens if needed
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  async signup(email: string, password: string): Promise<Result<string, Error>> {
    try {
      const userRecord = await auth.createUser({
        email,
        password,
        emailVerified: false,
      });

      return Result.success(userRecord.uid);
    } catch (error: any) {
      const errorMessage = this.mapAuthError(error);
      return Result.failure(new FunctionError(
        FunctionErrorCode.BAD_REQUEST,
        errorMessage,
        { email, originalError: error.code }
      ));
    }
  }

  async requestPasswordResetCode(email: string): Promise<Result<void, Error>> {
    try {
      // Generate password reset link
      const resetLink = await auth.generatePasswordResetLink(email);
      
      // In a real implementation, you would send this link via email service
      console.log('Password reset link generated:', resetLink);
      
      return Result.success(undefined);
    } catch (error: any) {
      return Result.failure(new FunctionError(
        FunctionErrorCode.BAD_REQUEST,
        'Failed to generate password reset link',
        { email, originalError: error.code }
      ));
    }
  }

  async sendEmailVerification(): Promise<Result<void, Error>> {
    try {
      // This would typically be handled by generating a verification link
      // and sending it via email service
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  async checkEmailVerification(): Promise<Result<boolean, Error>> {
    try {
      // In Firebase Functions, this would check the user record
      return Result.success(false); // Placeholder
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  async updatePassword(newPassword: string): Promise<Result<void, Error>> {
    try {
      // This would require the user UID from context
      throw new FunctionError(
        FunctionErrorCode.INTERNAL_ERROR,
        'Password update requires user context',
        { method: 'updatePassword' }
      );
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  async withdrawCurrentUser(): Promise<Result<void, Error>> {
    try {
      // This would require the user UID from context
      throw new FunctionError(
        FunctionErrorCode.INTERNAL_ERROR,
        'User deletion requires user context',
        { method: 'withdrawCurrentUser' }
      );
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  async getCurrentUserSession(): Promise<Result<UserSession, Error>> {
    try {
      // In Firebase Functions, this would extract session from the request context
      throw new FunctionError(
        FunctionErrorCode.INTERNAL_ERROR,
        'Session retrieval requires request context',
        { method: 'getCurrentUserSession' }
      );
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error(String(error)));
    }
  }

  async getLoginErrorMessage(exception: Error): Promise<string> {
    return this.mapAuthError(exception);
  }

  async getSignUpErrorMessage(exception: Error): Promise<string> {
    return this.mapAuthError(exception);
  }

  async getPasswordResetErrorMessage(exception: Error): Promise<string> {
    return this.mapAuthError(exception);
  }

  private mapAuthError(error: any): string {
    const errorCode = error?.code || 'unknown';
    
    switch (errorCode) {
      case 'auth/email-already-exists':
        return '이미 존재하는 이메일 주소입니다.';
      case 'auth/invalid-email':
        return '유효하지 않은 이메일 주소입니다.';
      case 'auth/weak-password':
        return '비밀번호가 너무 약합니다.';
      case 'auth/user-not-found':
        return '존재하지 않는 사용자입니다.';
      case 'auth/wrong-password':
        return '잘못된 비밀번호입니다.';
      case 'auth/too-many-requests':
        return '너무 많은 요청이 발생했습니다. 잠시 후 다시 시도해주세요.';
      case 'auth/network-request-failed':
        return '네트워크 연결을 확인해주세요.';
      default:
        return error?.message || '인증 오류가 발생했습니다.';
    }
  }

  // Helper methods for Firebase Functions context
  async verifyIdToken(idToken: string): Promise<Result<UserSession, Error>> {
    try {
      const decodedToken = await auth.verifyIdToken(idToken);
      
      const session = UserSession.create(
        decodedToken.uid,
        decodedToken.email || '',
        idToken,
        {
          isEmailVerified: decodedToken.email_verified || false,
          customClaims: decodedToken,
        }
      );

      return Result.success(session);
    } catch (error: any) {
      return Result.failure(new FunctionError(
        FunctionErrorCode.UNAUTHORIZED,
        'Invalid authentication token',
        { originalError: error.code }
      ));
    }
  }

  async getUserById(uid: string): Promise<Result<any, Error>> {
    try {
      const userRecord = await auth.getUser(uid);
      return Result.success(userRecord);
    } catch (error: any) {
      return Result.failure(new FunctionError(
        FunctionErrorCode.NOT_FOUND,
        'User not found',
        { uid, originalError: error.code }
      ));
    }
  }

  async updateUserClaims(uid: string, customClaims: Record<string, any>): Promise<Result<void, Error>> {
    try {
      await auth.setCustomUserClaims(uid, customClaims);
      return Result.success(undefined);
    } catch (error: any) {
      return Result.failure(new FunctionError(
        FunctionErrorCode.INTERNAL_ERROR,
        'Failed to update user claims',
        { uid, originalError: error.code }
      ));
    }
  }
}