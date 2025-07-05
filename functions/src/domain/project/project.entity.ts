import { BaseEntity, ValueObject } from '../../core/types';
import { ValidationError } from '../../core/errors';

export class ProjectName implements ValueObject<string> {
  constructor(public readonly value: string) {
    if (value.length < 3) {
      throw new ValidationError('projectName', 'Project name must be at least 3 characters');
    }
    if (value.length > 100) {
      throw new ValidationError('projectName', 'Project name must be at most 100 characters');
    }
  }

  equals(other: ProjectName): boolean {
    return this.value === other.value;
  }
}

export class ProjectDescription implements ValueObject<string> {
  constructor(public readonly value: string) {
    if (value.length > 1000) {
      throw new ValidationError('projectDescription', 'Project description must be at most 1000 characters');
    }
  }

  equals(other: ProjectDescription): boolean {
    return this.value === other.value;
  }
}

export class ProjectImage implements ValueObject<string> {
  constructor(public readonly value: string) {
    if (!value.startsWith('https://')) {
      throw new ValidationError('projectImage', 'Project image must be a valid HTTPS URL');
    }
  }

  equals(other: ProjectImage): boolean {
    return this.value === other.value;
  }
}

export enum ProjectStatus {
  ACTIVE = 'active',
  ARCHIVED = 'archived',
  DELETED = 'deleted'
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
      throw new ValidationError('memberCount', 'Member count cannot be negative');
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
}