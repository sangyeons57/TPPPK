import {BaseEntity} from "../../../core/types";
import {validateId, validateUsername} from "../../../core/validation";

// ProjectChannel Type Enum (matching Android ProjectChannelType.kt)
export enum ProjectChannelType {
  MESSAGES = "messages",
  TASKS = "tasks",
  UNKNOWN = "unknown",
}

// ProjectChannel Status Enum (matching Android ProjectChannelStatus.kt)
export enum ProjectChannelStatus {
  ACTIVE = "ACTIVE",
  ARCHIVED = "ARCHIVED",
  DISABLED = "DISABLED",
  DELETED = "DELETED",
  UNKNOWN = "UNKNOWN",
}

// ProjectChannel Data Interface (for Firestore mapping)
export interface ProjectChannelData {
  id: string;
  channelName: string;
  channelType: ProjectChannelType;
  order: number;
  status: ProjectChannelStatus;
  categoryId?: string;
  createdAt: Date;
  updatedAt: Date;
}

// ProjectChannel Entity (matching Android ProjectChannel.kt structure)
export class ProjectChannelEntity implements BaseEntity {
  public static readonly COLLECTION_NAME = "project_channels";

  // Keys matching Android ProjectChannel.kt
  public static readonly KEY_CHANNEL_NAME = "channelName";
  public static readonly KEY_CHANNEL_TYPE = "channelType";
  public static readonly KEY_ORDER = "order";
  public static readonly KEY_STATUS = "status";
  public static readonly KEY_CATEGORY_ID = "categoryId";
  public static readonly KEY_CREATED_AT = "createdAt";
  public static readonly KEY_UPDATED_AT = "updatedAt";

  constructor(
    public readonly id: string,
    public readonly channelName: string,
    public readonly channelType: ProjectChannelType,
    public readonly order: number,
    public readonly status: ProjectChannelStatus = ProjectChannelStatus.ACTIVE,
    public readonly categoryId?: string,
    public readonly createdAt: Date = new Date(),
    public readonly updatedAt: Date = new Date()
  ) {
    // Validate inputs
    validateId(id, "projectChannelId");
    validateUsername(channelName);
    
    if (order <= 0) {
      throw new Error("Channel order must be a positive number");
    }
    
    if (categoryId) {
      validateId(categoryId, "categoryId");
    }
  }

  // Business logic methods
  updateName(newName: string): ProjectChannelEntity {
    if (!newName || newName.trim().length === 0) {
      throw new Error("Channel name cannot be empty");
    }
    
    if (this.channelName === newName) return this;
    
    return new ProjectChannelEntity(
      this.id,
      newName.trim(),
      this.channelType,
      this.order,
      this.status,
      this.categoryId,
      this.createdAt,
      new Date()
    );
  }

  changeOrder(newOrder: number): ProjectChannelEntity {
    if (newOrder <= 0) {
      throw new Error("Channel order must be a positive number");
    }
    
    if (this.order === newOrder) return this;
    
    return new ProjectChannelEntity(
      this.id,
      this.channelName,
      this.channelType,
      newOrder,
      this.status,
      this.categoryId,
      this.createdAt,
      new Date()
    );
  }

  moveToCategory(newCategoryId?: string): ProjectChannelEntity {
    if (newCategoryId) {
      validateId(newCategoryId, "categoryId");
    }
    
    if (this.categoryId === newCategoryId) return this;
    
    return new ProjectChannelEntity(
      this.id,
      this.channelName,
      this.channelType,
      this.order,
      this.status,
      newCategoryId,
      this.createdAt,
      new Date()
    );
  }

  archive(): ProjectChannelEntity {
    if (this.status === ProjectChannelStatus.ARCHIVED) return this;
    
    return new ProjectChannelEntity(
      this.id,
      this.channelName,
      this.channelType,
      this.order,
      ProjectChannelStatus.ARCHIVED,
      this.categoryId,
      this.createdAt,
      new Date()
    );
  }

  activate(): ProjectChannelEntity {
    if (this.status === ProjectChannelStatus.ACTIVE) return this;
    
    return new ProjectChannelEntity(
      this.id,
      this.channelName,
      this.channelType,
      this.order,
      ProjectChannelStatus.ACTIVE,
      this.categoryId,
      this.createdAt,
      new Date()
    );
  }

  disable(): ProjectChannelEntity {
    if (this.status === ProjectChannelStatus.DISABLED) return this;
    
    return new ProjectChannelEntity(
      this.id,
      this.channelName,
      this.channelType,
      this.order,
      ProjectChannelStatus.DISABLED,
      this.categoryId,
      this.createdAt,
      new Date()
    );
  }

  markDeleted(): ProjectChannelEntity {
    if (this.status === ProjectChannelStatus.DELETED) return this;
    
    return new ProjectChannelEntity(
      this.id,
      this.channelName,
      this.channelType,
      this.order,
      ProjectChannelStatus.DELETED,
      this.categoryId,
      this.createdAt,
      new Date()
    );
  }

  isActive(): boolean {
    return this.status === ProjectChannelStatus.ACTIVE;
  }

  isArchived(): boolean {
    return this.status === ProjectChannelStatus.ARCHIVED;
  }

  isDisabled(): boolean {
    return this.status === ProjectChannelStatus.DISABLED;
  }

  isDeleted(): boolean {
    return this.status === ProjectChannelStatus.DELETED;
  }

  toData(): ProjectChannelData {
    return {
      id: this.id,
      channelName: this.channelName,
      channelType: this.channelType,
      order: this.order,
      status: this.status,
      categoryId: this.categoryId,
      createdAt: this.createdAt,
      updatedAt: this.updatedAt,
    };
  }

  static fromData(data: ProjectChannelData): ProjectChannelEntity {
    return new ProjectChannelEntity(
      data.id,
      data.channelName,
      data.channelType || ProjectChannelType.UNKNOWN,
      data.order || 1,
      data.status || ProjectChannelStatus.ACTIVE,
      data.categoryId,
      data.createdAt,
      data.updatedAt
    );
  }

  static create(
    id: string,
    channelName: string,
    channelType: ProjectChannelType,
    order: number,
    status: ProjectChannelStatus = ProjectChannelStatus.ACTIVE,
    categoryId?: string
  ): ProjectChannelEntity {
    return new ProjectChannelEntity(
      id,
      channelName,
      channelType,
      order,
      status,
      categoryId,
      new Date(),
      new Date()
    );
  }

  static createForProject(
    channelId: string,
    channelName: string,
    channelType: ProjectChannelType,
    order: number,
    categoryId?: string
  ): ProjectChannelEntity {
    validateId(channelId, "channelId");
    validateUsername(channelName);
    
    if (order <= 0) {
      throw new Error("Channel order must be a positive number");
    }
    
    if (categoryId) {
      validateId(categoryId, "categoryId");
    }
    
    return ProjectChannelEntity.create(
      channelId,
      channelName,
      channelType,
      order,
      ProjectChannelStatus.ACTIVE,
      categoryId
    );
  }
}