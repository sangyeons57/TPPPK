import { BaseEntity, ValueObject, CustomResult, Result } from '../../core/types';
import { ValidationError } from '../../core/errors';
import { VALIDATION_RULES } from '../../core/constants';
import { UserId } from '../friend/friend.entity';

export class Email implements ValueObject<string> {
  constructor(public readonly value: string) {
    if (!VALIDATION_RULES.EMAIL_REGEX.test(value)) {
      throw new ValidationError('email', 'Invalid email format');
    }
  }

  equals(other: Email): boolean {
    return this.value === other.value;
  }
}

export class Username implements ValueObject<string> {
  constructor(public readonly value: string) {
    if (value.length < VALIDATION_RULES.USERNAME_MIN_LENGTH) {
      throw new ValidationError('username', `Username must be at least ${VALIDATION_RULES.USERNAME_MIN_LENGTH} characters`);
    }
    if (value.length > VALIDATION_RULES.USERNAME_MAX_LENGTH) {
      throw new ValidationError('username', `Username must be at most ${VALIDATION_RULES.USERNAME_MAX_LENGTH} characters`);
    }
  }

  equals(other: Username): boolean {
    return this.value === other.value;
  }
}

export class UserProfileImage implements ValueObject<string> {
  constructor(public readonly value: string) {
    if (!value.startsWith('https://')) {
      throw new ValidationError('profileImage', 'Profile image must be a valid HTTPS URL');
    }
  }

  equals(other: UserProfileImage): boolean {
    return this.value === other.value;
  }
}

export interface UserProfile extends BaseEntity {
  userId: string;
  username: Username;
  email: Email;
  profileImage?: UserProfileImage;
  bio?: string;
  displayName?: string;
  isActive: boolean;
  friendCount?: number;
  canReceiveFriendRequests?: boolean;
}

export interface UserSearchProfile {
  id: string;
  userId: string;
  username: string;
  displayName?: string;
  profileImage?: string;
  isActive: boolean;
  isFriend?: boolean;
  hasPendingRequest?: boolean;
}

export class UserProfileEntity implements UserProfile {
  constructor(
    public readonly id: string,
    public readonly userId: string,
    public readonly username: Username,
    public readonly email: Email,
    public readonly createdAt: Date,
    public readonly updatedAt: Date,
    public readonly isActive: boolean = true,
    public readonly profileImage?: UserProfileImage,
    public readonly bio?: string,
    public readonly displayName?: string,
    public readonly friendCount?: number,
    public readonly canReceiveFriendRequests: boolean = true
  ) {}

  updateProfile(updates: {
    username?: Username;
    profileImage?: UserProfileImage;
    bio?: string;
    displayName?: string;
    canReceiveFriendRequests?: boolean;
  }): UserProfileEntity {
    return new UserProfileEntity(
      this.id,
      this.userId,
      updates.username || this.username,
      this.email,
      this.createdAt,
      new Date(),
      this.isActive,
      updates.profileImage || this.profileImage,
      updates.bio !== undefined ? updates.bio : this.bio,
      updates.displayName !== undefined ? updates.displayName : this.displayName,
      this.friendCount,
      updates.canReceiveFriendRequests !== undefined ? updates.canReceiveFriendRequests : this.canReceiveFriendRequests
    );
  }

  deactivate(): UserProfileEntity {
    return new UserProfileEntity(
      this.id,
      this.userId,
      this.username,
      this.email,
      this.createdAt,
      new Date(),
      false,
      this.profileImage,
      this.bio,
      this.displayName,
      this.friendCount,
      this.canReceiveFriendRequests
    );
  }

  activate(): UserProfileEntity {
    return new UserProfileEntity(
      this.id,
      this.userId,
      this.username,
      this.email,
      this.createdAt,
      new Date(),
      true,
      this.profileImage,
      this.bio,
      this.displayName,
      this.friendCount,
      this.canReceiveFriendRequests
    );
  }

  /**
   * 친구 수를 업데이트합니다.
   */
  updateFriendCount(count: number): UserProfileEntity {
    return new UserProfileEntity(
      this.id,
      this.userId,
      this.username,
      this.email,
      this.createdAt,
      new Date(),
      this.isActive,
      this.profileImage,
      this.bio,
      this.displayName,
      count,
      this.canReceiveFriendRequests
    );
  }

  /**
   * 친구 요청 수신 설정을 변경합니다.
   */
  toggleFriendRequestsReceiving(): UserProfileEntity {
    return new UserProfileEntity(
      this.id,
      this.userId,
      this.username,
      this.email,
      this.createdAt,
      new Date(),
      this.isActive,
      this.profileImage,
      this.bio,
      this.displayName,
      this.friendCount,
      !this.canReceiveFriendRequests
    );
  }

  /**
   * 사용자가 친구 요청을 받을 수 있는지 확인합니다.
   */
  canReceiveRequests(): boolean {
    return this.isActive && this.canReceiveFriendRequests;
  }

  /**
   * 사용자를 UserId로 변환합니다.
   */
  toUserId(): UserId {
    return new UserId(this.userId);
  }

  /**
   * 검색용 프로필로 변환합니다.
   */
  toSearchProfile(isFriend?: boolean, hasPendingRequest?: boolean): UserSearchProfile {
    return {
      id: this.id,
      userId: this.userId,
      username: this.username.value,
      displayName: this.displayName,
      profileImage: this.profileImage?.value,
      isActive: this.isActive,
      isFriend,
      hasPendingRequest
    };
  }
}