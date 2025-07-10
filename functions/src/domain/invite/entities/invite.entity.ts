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
  status: InviteStatus;
  createdAt: Date;
  updatedAt: Date;
}

export class InviteEntity implements BaseEntity {
  public static readonly COLLECTION_NAME = "project_invitations";
  
  // Static keys for Firestore field names
  public static readonly KEY_INVITE_CODE = "inviteCode";
  public static readonly KEY_PROJECT_ID = "projectId";
  public static readonly KEY_INVITER_ID = "inviterId";
  public static readonly KEY_EXPIRES_AT = "expiresAt";
  public static readonly KEY_STATUS = "status";
  public static readonly KEY_CREATED_AT = "createdAt";
  public static readonly KEY_UPDATED_AT = "updatedAt";

  constructor(
    public readonly id: string, // This IS the invite code (document ID)
    public readonly projectId: string,
    public readonly inviterId: string,
    public readonly expiresAt: Date,
    public readonly status: InviteStatus,
    public readonly createdAt: Date,
    public readonly updatedAt: Date
  ) {
    // Validation
    validateId(projectId, 'project id');
    validateId(inviterId, 'inviter id');
    
    if (!id || id.length < 6) {
      throw new Error('Invite code (id) must be at least 6 characters long');
    }
  }

  /**
   * Gets the invite code (which is the same as the document ID)
   */
  get inviteCode(): string {
    return this.id;
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
      InviteStatus.REVOKED,
      this.createdAt,
      new Date()
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
    return this.status === InviteStatus.ACTIVE && !this.isExpired();
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
   */
  static create(
    inviteCode: string,
    projectId: string,
    inviterId: string,
    expiresAt: Date
  ): InviteEntity {
    const now = new Date();
    
    return new InviteEntity(
      inviteCode, // id = inviteCode
      projectId,
      inviterId,
      expiresAt,
      InviteStatus.ACTIVE,
      now,
      now
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
      data.status,
      data.createdAt,
      data.updatedAt
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