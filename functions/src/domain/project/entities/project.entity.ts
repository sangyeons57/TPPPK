import {BaseEntity, ValueObject} from "../../../core/types";
import {ValidationError} from "../../../core/errors";

export class ProjectName implements ValueObject<string> {
  constructor(public readonly value: string) {
    if (value.length < 3) {
      throw new ValidationError("projectName", "Project name must be at least 3 characters");
    }
    if (value.length > 100) {
      throw new ValidationError("projectName", "Project name must be at most 100 characters");
    }
  }

  equals(other: ProjectName): boolean {
    return this.value === other.value;
  }
}

export class ProjectDescription implements ValueObject<string> {
  constructor(public readonly value: string) {
    if (value.length > 1000) {
      throw new ValidationError("projectDescription", "Project description must be at most 1000 characters");
    }
  }

  equals(other: ProjectDescription): boolean {
    return this.value === other.value;
  }
}

export class ProjectImage implements ValueObject<string> {
  constructor(public readonly value: string) {
    if (!value.startsWith("https://")) {
      throw new ValidationError("projectImage", "Project image must be a valid HTTPS URL");
    }
  }

  equals(other: ProjectImage): boolean {
    return this.value === other.value;
  }
}

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
  name: ProjectName;
  description?: ProjectDescription;
  ownerId: string;
  image?: ProjectImage;
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
    public readonly name: ProjectName,
    public readonly ownerId: string,
    public readonly createdAt: Date,
    public readonly updatedAt: Date,
    public readonly status: ProjectStatus = ProjectStatus.ACTIVE,
    public readonly memberCount: number = 1,
    public readonly description?: ProjectDescription,
    public readonly image?: ProjectImage
  ) {}

  updateProject(updates: {
    name?: ProjectName;
    description?: ProjectDescription;
    image?: ProjectImage;
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

  changeName(newName: ProjectName): ProjectEntity {
    if (this.name.equals(newName)) return this;

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

  changeImageUrl(newImageUrl?: ProjectImage): ProjectEntity {
    if (this.image?.equals(newImageUrl || new ProjectImage("https://placeholder.com"))) return this;

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
      name: this.name.value,
      imageUrl: this.image?.value,
      ownerId: this.ownerId,
      createdAt: this.createdAt,
      updatedAt: this.updatedAt,
    };
  }

  static fromData(data: ProjectData): ProjectEntity {
    return new ProjectEntity(
      data.id,
      new ProjectName(data.name),
      data.ownerId,
      data.createdAt,
      data.updatedAt,
      ProjectStatus.ACTIVE,
      1,
      undefined,
      data.imageUrl ? new ProjectImage(data.imageUrl) : undefined
    );
  }

  static create(
    name: ProjectName,
    ownerId: string,
    imageUrl?: ProjectImage
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
    name: ProjectName,
    ownerId: string,
    createdAt?: Date,
    updatedAt?: Date,
    imageUrl?: ProjectImage
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
