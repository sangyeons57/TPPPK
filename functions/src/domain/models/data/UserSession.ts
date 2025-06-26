/**
 * User session data model
 * Represents an authenticated user session
 */

import { UserId, UserEmail, Token } from '../vo';

export interface UserSessionData {
  readonly userId: string;
  readonly email: string;
  readonly token: string;
  readonly refreshToken?: string;
  readonly isEmailVerified: boolean;
  readonly loginAt: Date;
  readonly expiresAt: Date;
  readonly customClaims?: Record<string, any>;
}

export class UserSession {
  public readonly userId: UserId;
  public readonly email: UserEmail;
  public readonly token: Token;
  public readonly refreshToken?: Token;
  public readonly isEmailVerified: boolean;
  public readonly loginAt: Date;
  public readonly expiresAt: Date;
  public readonly customClaims: Record<string, any>;

  constructor(data: UserSessionData) {
    this.userId = UserId.from(data.userId);
    this.email = UserEmail.from(data.email);
    this.token = Token.from(data.token);
    this.refreshToken = data.refreshToken ? Token.from(data.refreshToken) : undefined;
    this.isEmailVerified = data.isEmailVerified;
    this.loginAt = data.loginAt;
    this.expiresAt = data.expiresAt;
    this.customClaims = data.customClaims || {};
  }

  public static create(
    userId: string,
    email: string,
    token: string,
    options: {
      refreshToken?: string;
      isEmailVerified?: boolean;
      expiresAt?: Date;
      customClaims?: Record<string, any>;
    } = {}
  ): UserSession {
    const now = new Date();
    const defaultExpiresAt = new Date(now.getTime() + 3600000); // 1 hour

    return new UserSession({
      userId,
      email,
      token,
      refreshToken: options.refreshToken,
      isEmailVerified: options.isEmailVerified ?? false,
      loginAt: now,
      expiresAt: options.expiresAt ?? defaultExpiresAt,
      customClaims: options.customClaims,
    });
  }

  public isExpired(currentTime: Date = new Date()): boolean {
    return currentTime >= this.expiresAt;
  }

  public isTokenExpired(currentTime: Date = new Date()): boolean {
    return this.token.isExpired(currentTime);
  }

  public hasRole(role: string): boolean {
    return this.customClaims?.roles?.includes(role) ?? false;
  }

  public hasPermission(permission: string): boolean {
    return this.customClaims?.permissions?.includes(permission) ?? false;
  }

  public hasAnyRole(roles: string[]): boolean {
    return roles.some(role => this.hasRole(role));
  }

  public hasAllRoles(roles: string[]): boolean {
    return roles.every(role => this.hasRole(role));
  }

  public withCustomClaims(claims: Record<string, any>): UserSession {
    return new UserSession({
      userId: this.userId.value,
      email: this.email.value,
      token: this.token.value,
      refreshToken: this.refreshToken?.value,
      isEmailVerified: this.isEmailVerified,
      loginAt: this.loginAt,
      expiresAt: this.expiresAt,
      customClaims: { ...this.customClaims, ...claims },
    });
  }

  public toJSON(): UserSessionData {
    return {
      userId: this.userId.value,
      email: this.email.value,
      token: this.token.value,
      refreshToken: this.refreshToken?.value,
      isEmailVerified: this.isEmailVerified,
      loginAt: this.loginAt,
      expiresAt: this.expiresAt,
      customClaims: this.customClaims,
    };
  }

  public toString(): string {
    return `UserSession(userId=${this.userId.value}, email=${this.email.value})`;
  }
}