import { BaseEntity, ValueObject } from '../../../core/types';
import { ValidationError } from '../../../core/errors';

export class SessionToken implements ValueObject<string> {
  constructor(public readonly value: string) {
    if (value.length < 10) {
      throw new ValidationError('sessionToken', 'Session token must be at least 10 characters');
    }
  }

  equals(other: SessionToken): boolean {
    return this.value === other.value;
  }
}

export class RefreshToken implements ValueObject<string> {
  constructor(public readonly value: string) {
    if (value.length < 10) {
      throw new ValidationError('refreshToken', 'Refresh token must be at least 10 characters');
    }
  }

  equals(other: RefreshToken): boolean {
    return this.value === other.value;
  }
}

export enum SessionStatus {
  ACTIVE = 'active',
  EXPIRED = 'expired',
  REVOKED = 'revoked'
}

export interface Session extends BaseEntity {
  userId: string;
  token: SessionToken;
  refreshToken: RefreshToken;
  expiresAt: Date;
  status: SessionStatus;
  deviceInfo?: string;
  ipAddress?: string;
  lastAccessedAt: Date;
}

export class SessionEntity implements Session {
  constructor(
    public readonly id: string,
    public readonly userId: string,
    public readonly token: SessionToken,
    public readonly refreshToken: RefreshToken,
    public readonly expiresAt: Date,
    public readonly createdAt: Date,
    public readonly updatedAt: Date,
    public readonly status: SessionStatus = SessionStatus.ACTIVE,
    public readonly lastAccessedAt: Date = new Date(),
    public readonly deviceInfo?: string,
    public readonly ipAddress?: string
  ) {}

  isExpired(): boolean {
    return this.expiresAt < new Date();
  }

  isActive(): boolean {
    return this.status === SessionStatus.ACTIVE && !this.isExpired();
  }

  revoke(): SessionEntity {
    return new SessionEntity(
      this.id,
      this.userId,
      this.token,
      this.refreshToken,
      this.expiresAt,
      this.createdAt,
      new Date(),
      SessionStatus.REVOKED,
      this.lastAccessedAt,
      this.deviceInfo,
      this.ipAddress
    );
  }

  expire(): SessionEntity {
    return new SessionEntity(
      this.id,
      this.userId,
      this.token,
      this.refreshToken,
      this.expiresAt,
      this.createdAt,
      new Date(),
      SessionStatus.EXPIRED,
      this.lastAccessedAt,
      this.deviceInfo,
      this.ipAddress
    );
  }

  updateAccess(ipAddress?: string): SessionEntity {
    return new SessionEntity(
      this.id,
      this.userId,
      this.token,
      this.refreshToken,
      this.expiresAt,
      this.createdAt,
      new Date(),
      this.status,
      new Date(),
      this.deviceInfo,
      ipAddress || this.ipAddress
    );
  }

  refresh(newToken: SessionToken, newRefreshToken: RefreshToken, newExpiresAt: Date): SessionEntity {
    return new SessionEntity(
      this.id,
      this.userId,
      newToken,
      newRefreshToken,
      newExpiresAt,
      this.createdAt,
      new Date(),
      SessionStatus.ACTIVE,
      new Date(),
      this.deviceInfo,
      this.ipAddress
    );
  }
}