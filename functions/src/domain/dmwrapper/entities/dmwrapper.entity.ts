import {BaseEntity} from "../../../core/types";
import {validateId, validateUsername, validateImageUrl} from "../../../core/validation";

// DMWrapper Data Interface (for Firestore mapping)
export interface DMWrapperData {
  id: string;
  otherUserId: string;
  otherUserName: string;
  otherUserImageUrl?: string;
  lastMessagePreview?: string;
  createdAt: Date;
  updatedAt: Date;
}

// DMWrapper Entity (matching Android DMWrapper.kt structure)
export class DMWrapperEntity implements BaseEntity {
  public static readonly COLLECTION_NAME = "dm_wrapper";

  // Keys matching Android DMWrapper.kt
  public static readonly KEY_OTHER_USER_ID = "otherUserId";
  public static readonly KEY_OTHER_USER_NAME = "otherUserName";
  public static readonly KEY_OTHER_USER_IMAGE_URL = "otherUserImageUrl";
  public static readonly KEY_LAST_MESSAGE_PREVIEW = "lastMessagePreview";
  public static readonly KEY_CREATED_AT = "createdAt";
  public static readonly KEY_UPDATED_AT = "updatedAt";

  constructor(
    public readonly id: string,
    public readonly otherUserId: string,
    public readonly otherUserName: string,
    public readonly otherUserImageUrl?: string,
    public readonly lastMessagePreview?: string,
    public readonly createdAt: Date = new Date(),
    public readonly updatedAt: Date = new Date()
  ) {
    // Validate inputs
    validateId(id, "dmWrapperId");
    validateId(otherUserId, "otherUserId");
    validateUsername(otherUserName);
    
    if (otherUserImageUrl) {
      validateImageUrl(otherUserImageUrl);
    }
  }

  // Business logic methods
  changeOtherUser(newOtherUserId: string, newOtherUserName: string, newOtherUserImageUrl?: string): DMWrapperEntity {
    validateId(newOtherUserId, "newOtherUserId");
    validateUsername(newOtherUserName);
    
    if (newOtherUserImageUrl) {
      validateImageUrl(newOtherUserImageUrl);
    }
    
    if (this.otherUserId === newOtherUserId &&
        this.otherUserName === newOtherUserName &&
        this.otherUserImageUrl === newOtherUserImageUrl) {
      return this;
    }

    return new DMWrapperEntity(
      this.id,
      newOtherUserId,
      newOtherUserName,
      newOtherUserImageUrl,
      this.lastMessagePreview,
      this.createdAt,
      new Date()
    );
  }

  updateLastMessagePreview(newPreview?: string): DMWrapperEntity {
    if (this.lastMessagePreview === newPreview) {
      return this;
    }

    return new DMWrapperEntity(
      this.id,
      this.otherUserId,
      this.otherUserName,
      this.otherUserImageUrl,
      newPreview,
      this.createdAt,
      new Date()
    );
  }

  updateOtherUserProfile(newName: string, newImageUrl?: string): DMWrapperEntity {
    validateUsername(newName);
    
    if (newImageUrl) {
      validateImageUrl(newImageUrl);
    }
    
    if (this.otherUserName === newName && this.otherUserImageUrl === newImageUrl) {
      return this;
    }

    return new DMWrapperEntity(
      this.id,
      this.otherUserId,
      newName,
      newImageUrl,
      this.lastMessagePreview,
      this.createdAt,
      new Date()
    );
  }

  toData(): DMWrapperData {
    return {
      id: this.id,
      otherUserId: this.otherUserId,
      otherUserName: this.otherUserName,
      otherUserImageUrl: this.otherUserImageUrl,
      lastMessagePreview: this.lastMessagePreview,
      createdAt: this.createdAt,
      updatedAt: this.updatedAt,
    };
  }

  static fromData(data: DMWrapperData): DMWrapperEntity {
    return new DMWrapperEntity(
      data.id,
      data.otherUserId,
      data.otherUserName,
      data.otherUserImageUrl,
      data.lastMessagePreview,
      data.createdAt,
      data.updatedAt
    );
  }

  static create(
    id: string,
    otherUserId: string,
    otherUserName: string,
    otherUserImageUrl?: string
  ): DMWrapperEntity {
    return new DMWrapperEntity(
      id,
      otherUserId,
      otherUserName,
      otherUserImageUrl,
      undefined, // no initial last message preview
      new Date(),
      new Date()
    );
  }

  static createForUsers(
    wrapperId: string,
    otherUserId: string,
    otherUserName: string,
    otherUserImageUrl?: string
  ): DMWrapperEntity {
    validateId(wrapperId, "wrapperId");
    validateId(otherUserId, "otherUserId");
    validateUsername(otherUserName);
    
    if (otherUserImageUrl) {
      validateImageUrl(otherUserImageUrl);
    }
    
    return DMWrapperEntity.create(wrapperId, otherUserId, otherUserName, otherUserImageUrl);
  }
}