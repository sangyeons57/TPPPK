import {BaseEntity} from "../../../core/types";
import {validateEmail, validateUsername, validateImageUrl, validateUserMemo, validateFcmToken} from "../../../core/validation";


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
    public readonly email: string,
    public readonly name: string,
    public readonly consentTimeStamp: Date,
    public readonly profileImageUrl?: string,
    public readonly memo?: string,
    public readonly userStatus: UserStatus = UserStatus.OFFLINE,
    public readonly fcmToken?: string,
    public readonly accountStatus: UserAccountStatus = UserAccountStatus.ACTIVE,
    public readonly createdAt: Date = new Date(),
    public readonly updatedAt: Date = new Date()
  ) {
    // Validate inputs
    validateEmail(email);
    validateUsername(name);
    if (profileImageUrl) {
      validateImageUrl(profileImageUrl);
    }
    if (memo) {
      validateUserMemo(memo);
    }
    if (fcmToken) {
      validateFcmToken(fcmToken);
    }
  }

  // Business logic methods
  updateProfile(updates: {
    name?: string;
    profileImageUrl?: string;
    memo?: string;
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
      email: this.email,
      name: this.name,
      consentTimeStamp: this.consentTimeStamp,
      profileImageUrl: this.profileImageUrl,
      memo: this.memo,
      userStatus: this.userStatus,
      fcmToken: this.fcmToken,
      accountStatus: this.accountStatus,
      createdAt: this.createdAt,
      updatedAt: this.updatedAt,
    };
  }

  static fromData(data: UserData): UserEntity {
    return new UserEntity(
      data.id,
      data.email,
      data.name,
      data.consentTimeStamp,
      data.profileImageUrl,
      data.memo,
      data.userStatus,
      data.fcmToken,
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
    email: string,
    name: string,
    consentTimeStamp: Date,
    profileImageUrl?: string,
    memo?: string,
    initialFcmToken?: string
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
