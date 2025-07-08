import { validateId } from '../../../core/validation';
import { BaseEntity } from '../../../core/types';

export enum InviteStatus {
  ACTIVE = 'active',
  EXPIRED = 'expired',
  REVOKED = 'revoked'
}

export interface InviteData {
  id: string; // This IS the invite code (document ID)
  projectId: string;
  inviterId: string;
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
  public static readonly KEY_EXPIRES_AT = "expiresAt";
  public static readonly KEY_MAX_USES = "maxUses";
  public static readonly KEY_CURRENT_USES = "currentUses";
  public static readonly KEY_STATUS = "status";
  public static readonly KEY_CREATED_AT = "createdAt";
  public static readonly KEY_UPDATED_AT = "updatedAt";

  constructor(
    public readonly id: string, // This IS the invite code (document ID)
    public readonly projectId: string,
    public readonly inviterId: string,
    public readonly expiresAt: Date,
    public readonly currentUses: number,
    public readonly status: InviteStatus,
    public readonly createdAt: Date,
    public readonly updatedAt: Date,
    public readonly maxUses?: number
  ) {
    // Validation
    validateId(projectId, 'project id');
    validateId(inviterId, 'inviter id');
    
    if (!id || id.length < 6) {
      throw new Error('Invite code (id) must be at least 6 characters long');
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
   * Gets the invite code (which is the same as the document ID)
   */
  get inviteCode(): string {
    return this.id;
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
      id: this.id, // This is the invite code
      projectId: this.projectId,
      inviterId: this.inviterId,
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
   * @param inviteCode The custom invite code that will be used as document ID
   * @param projectId The project ID
   * @param inviterId The user who created the invite
   * @param expiresAt When the invite expires
   * @param maxUses Maximum number of times the invite can be used
   */
  static create(
    inviteCode: string,
    projectId: string,
    inviterId: string,
    expiresAt: Date,
    maxUses?: number
  ): InviteEntity {
    const now = new Date();
    
    return new InviteEntity(
      inviteCode, // id = inviteCode
      projectId,
      inviterId,
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
      data.id, // This is the invite code
      data.projectId,
      data.inviterId,
      data.expiresAt,
      data.currentUses,
      data.status,
      data.createdAt,
      data.updatedAt,
      data.maxUses
    );
  }

  /**
   * Generates a custom invite code optimized for user-friendliness.
   * Uses a mix of uppercase letters and numbers, avoiding confusing characters.
   */
  static generateInviteCode(): string {
    // Exclude confusing characters: 0, O, 1, I, l
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    let result = '';
    for (let i = 0; i < 8; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
  }

  /**
   * Generates a shorter invite code for specific use cases.
   */
  static generateShortInviteCode(): string {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    let result = '';
    for (let i = 0; i < 6; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
  }
}