import {BaseEntity, ValueObject} from "../../../core/types";
import {ValidationError} from "../../../core/errors";
import {VALIDATION_RULES} from "../../../core/constants";

// Value Objects
export class Email implements ValueObject<string> {
  constructor(public readonly value: string) {
    if (!VALIDATION_RULES.EMAIL_REGEX.test(value)) {
      throw new ValidationError("email", "Invalid email format");
    }
  }

  equals(other: Email): boolean {
    return this.value === other.value;
  }
}

export class UserName implements ValueObject<string> {
  constructor(public readonly value: string) {
    if (value.length < VALIDATION_RULES.USERNAME_MIN_LENGTH) {
      throw new ValidationError("name", `Username must be at least ${VALIDATION_RULES.USERNAME_MIN_LENGTH} characters`);
    }
    if (value.length > VALIDATION_RULES.USERNAME_MAX_LENGTH) {
      throw new ValidationError("name", `Username must be at most ${VALIDATION_RULES.USERNAME_MAX_LENGTH} characters`);
    }
  }

  equals(other: UserName): boolean {
    return this.value === other.value;
  }
}

export class ImageUrl implements ValueObject<string> {
  constructor(public readonly value: string) {
    if (!value.startsWith("https://")) {
      throw new ValidationError("profileImageUrl", "Profile image must be a valid HTTPS URL");
    }
  }

  equals(other: ImageUrl): boolean {
    return this.value === other.value;
  }
}

export class UserMemo implements ValueObject<string> {
  constructor(public readonly value: string) {
    if (value.length > 500) {
      throw new ValidationError("memo", "User memo must be at most 500 characters");
    }
  }

  equals(other: UserMemo): boolean {
    return this.value === other.value;
  }
}

export class UserFcmToken implements ValueObject<string> {
  constructor(public readonly value: string) {
    if (value.trim().length === 0) {
      throw new ValidationError("fcmToken", "FCM token cannot be empty");
    }
  }

  equals(other: UserFcmToken): boolean {
    return this.value === other.value;
  }
}

// Enums (matching Android exactly)
export enum UserStatus {
  ONLINE = "online",
  OFFLINE = "offline",
  AWAY = "away",
  DO_NOT_DISTURB = "do_not_disturb",
  UNKNOWN = "unknown"
}

export enum UserAccountStatus {
  ACTIVE = "active",
  SUSPENDED = "suspended",
  DELETED = "deleted",
  WITHDRAWN = "withdrawn",
  UNKNOWN = "unknown"
}

// User Data Interface (for Firestore mapping)
export interface UserData {
  id: string;
  email: string;
  name: string;
  consentTimeStamp: Date;
  profileImageUrl?: string;
  memo?: string;
  userStatus: UserStatus;
  fcmToken?: string;
  accountStatus: UserAccountStatus;
  createdAt: Date;
  updatedAt: Date;
}

// User Entity (matching Android User.kt structure exactly)
export class UserEntity implements BaseEntity {
  public static readonly COLLECTION_NAME = "users";

  // Keys matching Android User.kt
  public static readonly KEY_EMAIL = "email";
  public static readonly KEY_NAME = "name";
  public static readonly KEY_CONSENT_TIMESTAMP = "consentTimeStamp";
  public static readonly KEY_PROFILE_IMAGE_URL = "profileImageUrl";
  public static readonly KEY_MEMO = "memo";
  public static readonly KEY_USER_STATUS = "userStatus";
  public static readonly KEY_FCM_TOKEN = "fcmToken";
  public static readonly KEY_ACCOUNT_STATUS = "accountStatus";
  public static readonly KEY_CREATED_AT = "createdAt";
  public static readonly KEY_UPDATED_AT = "updatedAt";

  constructor(
    public readonly id: string,
    public readonly email: Email,
    public readonly name: UserName,
    public readonly consentTimeStamp: Date,
    public readonly profileImageUrl?: ImageUrl,
    public readonly memo?: UserMemo,
    public readonly userStatus: UserStatus = UserStatus.OFFLINE,
    public readonly fcmToken?: UserFcmToken,
    public readonly accountStatus: UserAccountStatus = UserAccountStatus.ACTIVE,
    public readonly createdAt: Date = new Date(),
    public readonly updatedAt: Date = new Date()
  ) {}

  // Business logic methods
  updateProfile(updates: {
    name?: UserName;
    profileImageUrl?: ImageUrl;
    memo?: UserMemo;
  }): UserEntity {
    return new UserEntity(
      this.id,
      this.email,
      updates.name || this.name,
      this.consentTimeStamp,
      updates.profileImageUrl !== undefined ? updates.profileImageUrl : this.profileImageUrl,
      updates.memo !== undefined ? updates.memo : this.memo,
      this.userStatus,
      this.fcmToken,
      this.accountStatus,
      this.createdAt,
      new Date()
    );
  }

  updateUserStatus(newStatus: UserStatus): UserEntity {
    if (this.isWithdrawn()) return this;

    return new UserEntity(
      this.id,
      this.email,
      this.name,
      this.consentTimeStamp,
      this.profileImageUrl,
      this.memo,
      newStatus,
      this.fcmToken,
      this.accountStatus,
      this.createdAt,
      new Date()
    );
  }

  updateFcmToken(newToken?: UserFcmToken): UserEntity {
    if (this.isWithdrawn()) return this;

    return new UserEntity(
      this.id,
      this.email,
      this.name,
      this.consentTimeStamp,
      this.profileImageUrl,
      this.memo,
      this.userStatus,
      newToken,
      this.accountStatus,
      this.createdAt,
      new Date()
    );
  }

  suspendAccount(): UserEntity {
    if (this.isWithdrawn()) return this;

    return new UserEntity(
      this.id,
      this.email,
      this.name,
      this.consentTimeStamp,
      this.profileImageUrl,
      this.memo,
      this.userStatus,
      this.fcmToken,
      UserAccountStatus.SUSPENDED,
      this.createdAt,
      new Date()
    );
  }

  activateAccount(): UserEntity {
    if (this.isWithdrawn()) return this;

    return new UserEntity(
      this.id,
      this.email,
      this.name,
      this.consentTimeStamp,
      this.profileImageUrl,
      this.memo,
      this.userStatus,
      this.fcmToken,
      UserAccountStatus.ACTIVE,
      this.createdAt,
      new Date()
    );
  }

  markAsWithdrawn(): UserEntity {
    if (this.isWithdrawn()) return this;

    return new UserEntity(
      this.id,
      this.email,
      this.name,
      this.consentTimeStamp,
      this.profileImageUrl,
      this.memo,
      UserStatus.OFFLINE, // Ensure offline status on withdrawal
      undefined, // Clear FCM token on withdrawal
      UserAccountStatus.WITHDRAWN,
      this.createdAt,
      new Date()
    );
  }

  removeProfileImage(): UserEntity {
    if (this.isWithdrawn()) return this;

    return new UserEntity(
      this.id,
      this.email,
      this.name,
      this.consentTimeStamp,
      undefined, // Remove profile image
      this.memo,
      this.userStatus,
      this.fcmToken,
      this.accountStatus,
      this.createdAt,
      new Date()
    );
  }

  isWithdrawn(): boolean {
    return this.accountStatus === UserAccountStatus.WITHDRAWN;
  }

  isActive(): boolean {
    return this.accountStatus === UserAccountStatus.ACTIVE;
  }

  canReceiveRequests(): boolean {
    return this.isActive() && this.userStatus !== UserStatus.UNKNOWN;
  }

  toData(): UserData {
    return {
      id: this.id,
      email: this.email.value,
      name: this.name.value,
      consentTimeStamp: this.consentTimeStamp,
      profileImageUrl: this.profileImageUrl?.value,
      memo: this.memo?.value,
      userStatus: this.userStatus,
      fcmToken: this.fcmToken?.value,
      accountStatus: this.accountStatus,
      createdAt: this.createdAt,
      updatedAt: this.updatedAt,
    };
  }

  static fromData(data: UserData): UserEntity {
    return new UserEntity(
      data.id,
      new Email(data.email),
      new UserName(data.name),
      data.consentTimeStamp,
      data.profileImageUrl ? new ImageUrl(data.profileImageUrl) : undefined,
      data.memo ? new UserMemo(data.memo) : undefined,
      data.userStatus,
      data.fcmToken ? new UserFcmToken(data.fcmToken) : undefined,
      data.accountStatus,
      data.createdAt,
      data.updatedAt
    );
  }

  updateFriendCount(count: number): UserEntity {
    // In a real implementation, you might want to store friend count as a field
    // For now, just return the same entity since friend count is calculated dynamically
    return this;
  }

  toSearchProfile(): Partial<UserEntity> {
    return {
      id: this.id,
      name: this.name,
      profileImageUrl: this.profileImageUrl,
      userStatus: this.userStatus,
    };
  }

  static create(
    id: string,
    email: Email,
    name: UserName,
    consentTimeStamp: Date,
    profileImageUrl?: ImageUrl,
    memo?: UserMemo,
    initialFcmToken?: UserFcmToken
  ): UserEntity {
    return new UserEntity(
      id,
      email,
      name,
      consentTimeStamp,
      profileImageUrl,
      memo,
      UserStatus.OFFLINE, // Default to offline
      initialFcmToken,
      UserAccountStatus.ACTIVE, // Default to active
      new Date(),
      new Date()
    );
  }
}
