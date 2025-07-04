import { BaseEntity, ValueObject } from '../../core/types';
import { ValidationError } from '../../core/errors';
import { VALIDATION_RULES } from '../../core/constants';

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
    public readonly displayName?: string
  ) {}

  updateProfile(updates: {
    username?: Username;
    profileImage?: UserProfileImage;
    bio?: string;
    displayName?: string;
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
      updates.displayName !== undefined ? updates.displayName : this.displayName
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
      this.displayName
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
      this.displayName
    );
  }
}