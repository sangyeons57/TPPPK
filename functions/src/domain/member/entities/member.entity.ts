import { validateId } from '../../../core/validation';
import { BaseEntity } from '../../../core/types';

export enum MemberStatus {
  ACTIVE = 'active',
  BLOCKED = 'blocked',
  REMOVED = 'removed'
}

export interface MemberData {
  id: string;
  projectId: string;
  userId: string;
  roleIds: string[];
  status: MemberStatus;
  createdAt: Date;
  updatedAt: Date;
}

export class MemberEntity implements BaseEntity {
  public static readonly COLLECTION_NAME = "members";
  
  // Static keys for Firestore field names
  public static readonly KEY_PROJECT_ID = "projectId";
  public static readonly KEY_USER_ID = "userId";
  public static readonly KEY_ROLE_IDS = "roleIds";
  public static readonly KEY_STATUS = "status";
  public static readonly KEY_CREATED_AT = "createdAt";
  public static readonly KEY_UPDATED_AT = "updatedAt";

  constructor(
    public readonly id: string,
    public readonly projectId: string,
    public readonly userId: string,
    public readonly roleIds: string[],
    public readonly status: MemberStatus,
    public readonly createdAt: Date,
    public readonly updatedAt: Date
  ) {
    // Validation
    validateId(id, 'member id');
    validateId(projectId, 'project id');
    validateId(userId, 'user id');
    
    if (!roleIds || roleIds.length === 0) {
      throw new Error('Member must have at least one role');
    }
    
    // Validate role IDs
    roleIds.forEach(roleId => {
      validateId(roleId, 'role id');
    });
  }

  /**
   * Assigns a new role to the member if they don't already have it.
   */
  assignRole(roleId: string): MemberEntity {
    validateId(roleId, 'role id');
    
    if (this.roleIds.includes(roleId)) {
      return this;
    }

    return new MemberEntity(
      this.id,
      this.projectId,
      this.userId,
      [...this.roleIds, roleId],
      this.status,
      this.createdAt,
      new Date()
    );
  }

  /**
   * Updates all roles for the member.
   */
  updateRoles(roleIds: string[]): MemberEntity {
    if (!roleIds || roleIds.length === 0) {
      throw new Error('Member must have at least one role');
    }

    // Validate all role IDs
    roleIds.forEach(roleId => {
      validateId(roleId, 'role id');
    });

    return new MemberEntity(
      this.id,
      this.projectId,
      this.userId,
      roleIds,
      this.status,
      this.createdAt,
      new Date()
    );
  }

  /**
   * Revokes a role from the member if they have it.
   */
  revokeRole(roleId: string): MemberEntity {
    validateId(roleId, 'role id');
    
    if (!this.roleIds.includes(roleId)) {
      return this;
    }

    const updatedRoles = this.roleIds.filter(id => id !== roleId);
    
    if (updatedRoles.length === 0) {
      throw new Error('Cannot revoke the last role from a member');
    }

    return new MemberEntity(
      this.id,
      this.projectId,
      this.userId,
      updatedRoles,
      this.status,
      this.createdAt,
      new Date()
    );
  }

  /**
   * Blocks the member.
   */
  block(): MemberEntity {
    if (this.status === MemberStatus.BLOCKED) {
      return this;
    }

    return new MemberEntity(
      this.id,
      this.projectId,
      this.userId,
      this.roleIds,
      MemberStatus.BLOCKED,
      this.createdAt,
      new Date()
    );
  }

  /**
   * Removes the member (marks as removed).
   */
  remove(): MemberEntity {
    if (this.status === MemberStatus.REMOVED) {
      return this;
    }

    return new MemberEntity(
      this.id,
      this.projectId,
      this.userId,
      this.roleIds,
      MemberStatus.REMOVED,
      this.createdAt,
      new Date()
    );
  }

  /**
   * Reactivates the member.
   */
  reactivate(): MemberEntity {
    if (this.status === MemberStatus.ACTIVE) {
      return this;
    }

    return new MemberEntity(
      this.id,
      this.projectId,
      this.userId,
      this.roleIds,
      MemberStatus.ACTIVE,
      this.createdAt,
      new Date()
    );
  }

  /**
   * Checks if the member is active.
   */
  isActive(): boolean {
    return this.status === MemberStatus.ACTIVE;
  }

  /**
   * Checks if the member is blocked.
   */
  isBlocked(): boolean {
    return this.status === MemberStatus.BLOCKED;
  }

  /**
   * Checks if the member is removed.
   */
  isRemoved(): boolean {
    return this.status === MemberStatus.REMOVED;
  }

  /**
   * Checks if the member has a specific role.
   */
  hasRole(roleId: string): boolean {
    return this.roleIds.includes(roleId);
  }

  /**
   * Converts the entity to a data object for persistence.
   */
  toData(): MemberData {
    return {
      id: this.id,
      projectId: this.projectId,
      userId: this.userId,
      roleIds: this.roleIds,
      status: this.status,
      createdAt: this.createdAt,
      updatedAt: this.updatedAt
    };
  }

  /**
   * Factory method for creating a new member.
   */
  static create(
    id: string,
    projectId: string,
    userId: string,
    roleIds: string[]
  ): MemberEntity {
    const now = new Date();
    
    return new MemberEntity(
      id,
      projectId,
      userId,
      roleIds,
      MemberStatus.ACTIVE,
      now,
      now
    );
  }

  /**
   * Factory method to reconstitute a Member from data source.
   */
  static fromData(data: MemberData): MemberEntity {
    return new MemberEntity(
      data.id,
      data.projectId,
      data.userId,
      data.roleIds,
      data.status,
      data.createdAt,
      data.updatedAt
    );
  }
}