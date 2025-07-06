import {BaseEntity} from "../../../core/types";
import {ValidationError} from "../../../core/errors";
import {validateProjectName, validateProjectDescription, validateImageUrl, ProjectId} from "../../../core/validation";


export enum ProjectStatus {
  ACTIVE = "active",
  ARCHIVED = "archived",
  DELETED = "deleted"
}

export interface ProjectData {
  id: string;
  name: string;
  imageUrl?: string;
  ownerId: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface Project extends BaseEntity {
  name: string;
  description?: string;
  ownerId: string;
  image?: string;
  status: ProjectStatus;
  memberCount: number;
}

export class ProjectEntity implements Project {
  public static readonly COLLECTION_NAME = "projects";

  // Keys matching Android Project.kt
  public static readonly KEY_NAME = "name";
  public static readonly KEY_IMAGE_URL = "imageUrl";
  public static readonly KEY_OWNER_ID = "ownerId";

  constructor(
    public readonly id: string,
    public readonly name: string,
    public readonly ownerId: string,
    public readonly createdAt: Date,
    public readonly updatedAt: Date,
    public readonly status: ProjectStatus = ProjectStatus.ACTIVE,
    public readonly memberCount: number = 1,
    public readonly description?: string,
    public readonly image?: string
  ) {
    // Validate inputs
    validateProjectName(name);
    if (description) {
      validateProjectDescription(description);
    }
    if (image) {
      validateImageUrl(image);
    }
  }

  updateProject(updates: {
    name?: string;
    description?: string;
    image?: string;
  }): ProjectEntity {
    return new ProjectEntity(
      this.id,
      updates.name || this.name,
      this.ownerId,
      this.createdAt,
      new Date(),
      this.status,
      this.memberCount,
      updates.description !== undefined ? updates.description : this.description,
      updates.image !== undefined ? updates.image : this.image
    );
  }

  updateMemberCount(count: number): ProjectEntity {
    if (count < 0) {
      throw new ValidationError("memberCount", "Member count cannot be negative");
    }

    return new ProjectEntity(
      this.id,
      this.name,
      this.ownerId,
      this.createdAt,
      new Date(),
      this.status,
      count,
      this.description,
      this.image
    );
  }

  archive(): ProjectEntity {
    return new ProjectEntity(
      this.id,
      this.name,
      this.ownerId,
      this.createdAt,
      new Date(),
      ProjectStatus.ARCHIVED,
      this.memberCount,
      this.description,
      this.image
    );
  }

  activate(): ProjectEntity {
    return new ProjectEntity(
      this.id,
      this.name,
      this.ownerId,
      this.createdAt,
      new Date(),
      ProjectStatus.ACTIVE,
      this.memberCount,
      this.description,
      this.image
    );
  }

  delete(): ProjectEntity {
    return new ProjectEntity(
      this.id,
      this.name,
      this.ownerId,
      this.createdAt,
      new Date(),
      ProjectStatus.DELETED,
      this.memberCount,
      this.description,
      this.image
    );
  }

  changeName(newName: string): ProjectEntity {
    validateProjectName(newName);
    if (this.name === newName) return this;

    return new ProjectEntity(
      this.id,
      newName,
      this.ownerId,
      this.createdAt,
      new Date(),
      this.status,
      this.memberCount,
      this.description,
      this.image
    );
  }

  changeImageUrl(newImageUrl?: string): ProjectEntity {
    if (newImageUrl) {
      validateImageUrl(newImageUrl);
    }
    if (this.image === newImageUrl) return this;

    return new ProjectEntity(
      this.id,
      this.name,
      this.ownerId,
      this.createdAt,
      new Date(),
      this.status,
      this.memberCount,
      this.description,
      newImageUrl
    );
  }

  toData(): ProjectData {
    return {
      id: this.id,
      name: this.name,
      imageUrl: this.image,
      ownerId: this.ownerId,
      createdAt: this.createdAt,
      updatedAt: this.updatedAt,
    };
  }

  static fromData(data: ProjectData): ProjectEntity {
    return new ProjectEntity(
      data.id,
      data.name,
      data.ownerId,
      data.createdAt,
      data.updatedAt,
      ProjectStatus.ACTIVE,
      1,
      undefined,
      data.imageUrl
    );
  }

  static create(
    name: string,
    ownerId: string,
    imageUrl?: string
  ): ProjectEntity {
    return new ProjectEntity(
      "", // ID will be set by repository
      name,
      ownerId,
      new Date(),
      new Date(),
      ProjectStatus.ACTIVE,
      1,
      undefined,
      imageUrl
    );
  }

  static fromDataSource(
    id: string,
    name: string,
    ownerId: string,
    createdAt?: Date,
    updatedAt?: Date,
    imageUrl?: string
  ): ProjectEntity {
    return new ProjectEntity(
      id,
      name,
      ownerId,
      createdAt || new Date(),
      updatedAt || new Date(),
      ProjectStatus.ACTIVE,
      1,
      undefined,
      imageUrl
    );
  }
}
