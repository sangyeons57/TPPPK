/**
 * User domain model
 * Represents a user in the system with complete domain logic
 */

import { Entity } from '../../../shared/types/common';
import { UserId, UserEmail, UserName } from '../vo';
import { UserAccountStatus } from '../enums/UserAccountStatus';
import { FunctionError, FunctionErrorCode } from '../../../shared/types/common';

export interface UserData {
  readonly id: string;
  readonly email: string;
  readonly name: string;
  readonly profileImageUrl?: string;
  readonly memo?: string;
  readonly accountStatus: UserAccountStatus;
  readonly emailVerified: boolean;
  readonly fcmToken?: string;
  readonly lastLoginAt?: Date;
  readonly consentTimestamp: Date;
  readonly createdAt: Date;
  readonly updatedAt: Date;
}

export class User implements Entity {
  public readonly id: string;
  public readonly userId: UserId;
  public readonly email: UserEmail;
  public readonly name: UserName;
  public readonly profileImageUrl?: string;
  public readonly memo?: string;
  public readonly accountStatus: UserAccountStatus;
  public readonly emailVerified: boolean;
  public readonly fcmToken?: string;
  public readonly lastLoginAt?: Date;
  public readonly consentTimestamp: Date;
  public readonly createdAt: Date;
  public readonly updatedAt: Date;

  constructor(data: UserData) {
    this.id = data.id;
    this.userId = UserId.from(data.id);
    this.email = UserEmail.from(data.email);
    this.name = UserName.from(data.name);
    this.profileImageUrl = data.profileImageUrl;
    this.memo = data.memo;
    this.accountStatus = data.accountStatus;
    this.emailVerified = data.emailVerified;
    this.fcmToken = data.fcmToken;
    this.lastLoginAt = data.lastLoginAt;
    this.consentTimestamp = data.consentTimestamp;
    this.createdAt = data.createdAt;
    this.updatedAt = data.updatedAt;

    this.validate();
  }

  public static registerNewUser(
    id: string,
    email: string,
    name: string,
    consentTimestamp: Date,
    options: {
      profileImageUrl?: string;
      memo?: string;
      emailVerified?: boolean;
      fcmToken?: string;
    } = {}
  ): User {
    const now = new Date();

    return new User({
      id,
      email,
      name,
      profileImageUrl: options.profileImageUrl,
      memo: options.memo,
      accountStatus: options.emailVerified 
        ? UserAccountStatus.ACTIVE 
        : UserAccountStatus.PENDING_VERIFICATION,
      emailVerified: options.emailVerified ?? false,
      fcmToken: options.fcmToken,
      consentTimestamp,
      createdAt: now,
      updatedAt: now,
    });
  }

  // Business logic methods
  public canLogin(): boolean {
    return this.accountStatus === UserAccountStatus.ACTIVE && this.emailVerified;
  }

  public isActive(): boolean {
    return this.accountStatus === UserAccountStatus.ACTIVE;
  }

  public isSuspended(): boolean {
    return this.accountStatus === UserAccountStatus.SUSPENDED;
  }

  public isWithdrawn(): boolean {
    return this.accountStatus === UserAccountStatus.WITHDRAWN;
  }

  public isPendingVerification(): boolean {
    return this.accountStatus === UserAccountStatus.PENDING_VERIFICATION;
  }

  public isLocked(): boolean {
    return this.accountStatus === UserAccountStatus.LOCKED;
  }

  // State transition methods
  public activate(): User {
    if (this.isWithdrawn()) {
      throw new FunctionError(
        FunctionErrorCode.BAD_REQUEST,
        'Cannot activate a withdrawn account',
        { userId: this.userId.value, currentStatus: this.accountStatus }
      );
    }

    return this.updateStatus(UserAccountStatus.ACTIVE);
  }

  public suspend(): User {
    if (this.isWithdrawn()) {
      throw new FunctionError(
        FunctionErrorCode.BAD_REQUEST,
        'Cannot suspend a withdrawn account',
        { userId: this.userId.value, currentStatus: this.accountStatus }
      );
    }

    return this.updateStatus(UserAccountStatus.SUSPENDED);
  }

  public withdraw(): User {
    return this.updateStatus(UserAccountStatus.WITHDRAWN);
  }

  public lock(): User {
    if (this.isWithdrawn()) {
      throw new FunctionError(
        FunctionErrorCode.BAD_REQUEST,
        'Cannot lock a withdrawn account',
        { userId: this.userId.value, currentStatus: this.accountStatus }
      );
    }

    return this.updateStatus(UserAccountStatus.LOCKED);
  }

  public verifyEmail(): User {
    return new User({
      ...this.toData(),
      emailVerified: true,
      accountStatus: this.isPendingVerification() ? UserAccountStatus.ACTIVE : this.accountStatus,
      updatedAt: new Date(),
    });
  }

  public updateName(newName: string): User {
    return new User({
      ...this.toData(),
      name: newName,
      updatedAt: new Date(),
    });
  }

  public updateProfileImage(profileImageUrl?: string): User {
    return new User({
      ...this.toData(),
      profileImageUrl,
      updatedAt: new Date(),
    });
  }

  public updateMemo(memo?: string): User {
    return new User({
      ...this.toData(),
      memo,
      updatedAt: new Date(),
    });
  }

  public updateFcmToken(fcmToken?: string): User {
    return new User({
      ...this.toData(),
      fcmToken,
      updatedAt: new Date(),
    });
  }

  public recordLogin(): User {
    return new User({
      ...this.toData(),
      lastLoginAt: new Date(),
      updatedAt: new Date(),
    });
  }

  private updateStatus(newStatus: UserAccountStatus): User {
    return new User({
      ...this.toData(),
      accountStatus: newStatus,
      updatedAt: new Date(),
    });
  }

  public toData(): UserData {
    return {
      id: this.id,
      email: this.email.value,
      name: this.name.value,
      profileImageUrl: this.profileImageUrl,
      memo: this.memo,
      accountStatus: this.accountStatus,
      emailVerified: this.emailVerified,
      fcmToken: this.fcmToken,
      lastLoginAt: this.lastLoginAt,
      consentTimestamp: this.consentTimestamp,
      createdAt: this.createdAt,
      updatedAt: this.updatedAt,
    };
  }

  public toJSON(): UserData {
    return this.toData();
  }

  public toString(): string {
    return `User(id=${this.id}, email=${this.email.value}, name=${this.name.value}, status=${this.accountStatus})`;
  }

  private validate(): void {
    if (!this.consentTimestamp) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        'Consent timestamp is required',
        { userId: this.userId.value }
      );
    }

    if (this.consentTimestamp > new Date()) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        'Consent timestamp cannot be in the future',
        { userId: this.userId.value, consentTimestamp: this.consentTimestamp }
      );
    }

    if (this.memo && this.memo.length > 200) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        'User memo cannot exceed 200 characters',
        { userId: this.userId.value, memoLength: this.memo.length }
      );
    }
  }
}