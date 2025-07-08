import { validateId } from '../../../core/validation';
import { BaseEntity } from '../../../core/types';

export enum InviteStatus {
  ACTIVE = 'active',
  EXPIRED = 'expired',
  REVOKED = 'revoked'
}

export interface InviteData {
  id: string;
  projectId: string;
  inviterId: string;
  inviteCode: string;
  expiresAt: Date;
  maxUses?: number;
  currentUses: number;
  status: InviteStatus;
  createdAt: Date;
  updatedAt: Date;
}

export class InviteEntity implements BaseEntity {
  public static readonly COLLECTION_NAME = "invites";
  
  // Static keys for Firestore field names
  public static readonly KEY_PROJECT_ID = "projectId";
  public static readonly KEY_INVITER_ID = "inviterId";
  public static readonly KEY_INVITE_CODE = "inviteCode";
  public static readonly KEY_EXPIRES_AT = "expiresAt";
  public static readonly KEY_MAX_USES = "maxUses";
  public static readonly KEY_CURRENT_USES = "currentUses";
  public static readonly KEY_STATUS = "status";
  public static readonly KEY_CREATED_AT = "createdAt";
  public static readonly KEY_UPDATED_AT = "updatedAt";

  constructor(
    public readonly id: string,
    public readonly projectId: string,
    public readonly inviterId: string,
    public readonly inviteCode: string,
    public readonly expiresAt: Date,
    public readonly currentUses: number,
    public readonly status: InviteStatus,
    public readonly createdAt: Date,
    public readonly updatedAt: Date,
    public readonly maxUses?: number
  ) {
    // Validation
    validateId(id, 'invite id');
    validateId(projectId, 'project id');
    validateId(inviterId, 'inviter id');
    
    if (!inviteCode || inviteCode.length < 6) {
      throw new Error('Invite code must be at least 6 characters long');
    }
    
    if (maxUses !== undefined && maxUses < 1) {
      throw new Error('Max uses must be at least 1');
    }
    
    if (currentUses < 0) {
      throw new Error('Current uses cannot be negative');
    }
    
    if (maxUses !== undefined && currentUses > maxUses) {
      throw new Error('Current uses cannot exceed max uses');
    }
  }

  /**
   * Increments the current uses count.
   */
  incrementUses(): InviteEntity {
    if (this.isExpired()) {
      throw new Error('Cannot use expired invite');
    }
    
    if (this.isRevoked()) {
      throw new Error('Cannot use revoked invite');
    }
    
    if (this.maxUses !== undefined && this.currentUses >= this.maxUses) {
      throw new Error('Invite has reached maximum uses');
    }
    
    const newUses = this.currentUses + 1;
    const newStatus = this.maxUses !== undefined && newUses >= this.maxUses 
      ? InviteStatus.EXPIRED 
      : this.status;

    return new InviteEntity(
      this.id,
      this.projectId,
      this.inviterId,
      this.inviteCode,
      this.expiresAt,
      newUses,
      newStatus,
      this.createdAt,
      new Date(),
      this.maxUses
    );
  }

  /**
   * Revokes the invite.
   */
  revoke(): InviteEntity {
    if (this.status === InviteStatus.REVOKED) {
      return this;
    }

    return new InviteEntity(
      this.id,
      this.projectId,
      this.inviterId,
      this.inviteCode,
      this.expiresAt,
      this.currentUses,
      InviteStatus.REVOKED,
      this.createdAt,
      new Date(),
      this.maxUses
    );
  }

  /**
   * Checks if the invite is expired.
   */
  isExpired(): boolean {
    return this.status === InviteStatus.EXPIRED || this.expiresAt < new Date();
  }

  /**
   * Checks if the invite is revoked.
   */
  isRevoked(): boolean {
    return this.status === InviteStatus.REVOKED;
  }

  /**
   * Checks if the invite is active and usable.
   */
  isActive(): boolean {
    return this.status === InviteStatus.ACTIVE && !this.isExpired() && 
           (this.maxUses === undefined || this.currentUses < this.maxUses);
  }

  /**
   * Checks if the invite can be used.
   */
  canBeUsed(): boolean {
    return this.isActive();
  }

  /**
   * Converts the entity to a data object for persistence.
   */
  toData(): InviteData {
    return {
      id: this.id,
      projectId: this.projectId,
      inviterId: this.inviterId,
      inviteCode: this.inviteCode,
      expiresAt: this.expiresAt,
      maxUses: this.maxUses,
      currentUses: this.currentUses,
      status: this.status,
      createdAt: this.createdAt,
      updatedAt: this.updatedAt
    };
  }

  /**
   * Factory method for creating a new invite.
   */
  static create(
    id: string,
    projectId: string,
    inviterId: string,
    inviteCode: string,
    expiresAt: Date,
    maxUses?: number
  ): InviteEntity {
    const now = new Date();
    
    return new InviteEntity(
      id,
      projectId,
      inviterId,
      inviteCode,
      expiresAt,
      0, // currentUses starts at 0
      InviteStatus.ACTIVE,
      now,
      now,
      maxUses
    );
  }

  /**
   * Factory method to reconstitute an Invite from data source.
   */
  static fromData(data: InviteData): InviteEntity {
    return new InviteEntity(
      data.id,
      data.projectId,
      data.inviterId,
      data.inviteCode,
      data.expiresAt,
      data.currentUses,
      data.status,
      data.createdAt,
      data.updatedAt,
      data.maxUses
    );
  }

  /**
   * Generates a random invite code.
   */
  static generateInviteCode(): string {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';
    for (let i = 0; i < 8; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
  }
}