import {BaseEntity} from "../../../core/types";
import {validateId} from "../../../core/validation";

// DMChannel Status Enum (matching Android DMChannelStatus.kt)
export enum DMChannelStatus {
  ACTIVE = "ACTIVE",
  ARCHIVED = "ARCHIVED",
  BLOCKED = "BLOCKED",
  DELETED = "DELETED",
  UNKNOWN = "UNKNOWN",
}

// DMChannel Data Interface (for Firestore mapping)
export interface DMChannelData {
  id: string;
  participants: string[];
  status: DMChannelStatus;
  blockedByMap: Record<string, string>; // key: userId who blocked, value: userId who was blocked
  createdAt: Date;
  updatedAt: Date;
}

// DMChannel Entity (matching Android DMChannel.kt structure)
export class DMChannelEntity implements BaseEntity {
  public static readonly COLLECTION_NAME = "dm_channels";

  // Keys matching Android DMChannel.kt
  public static readonly KEY_PARTICIPANTS = "participants";
  public static readonly KEY_STATUS = "status";
  public static readonly KEY_BLOCKED_BY_MAP = "blockedByMap";
  public static readonly KEY_CREATED_AT = "createdAt";
  public static readonly KEY_UPDATED_AT = "updatedAt";

  constructor(
    public readonly id: string,
    public readonly participants: string[],
    public readonly status: DMChannelStatus = DMChannelStatus.ACTIVE,
    public readonly blockedByMap: Record<string, string> = {},
    public readonly createdAt: Date = new Date(),
    public readonly updatedAt: Date = new Date()
  ) {
    // Validate inputs
    validateId(id, "dmChannelId");

    if (!participants || participants.length < 2) {
      throw new Error("DMChannel must have at least two participants");
    }

    // Validate each participant ID
    participants.forEach((participantId, index) => {
      validateId(participantId, `participant[${index}]`);
    });

    // Ensure participants are unique
    const uniqueParticipants = [...new Set(participants)];
    if (uniqueParticipants.length !== participants.length) {
      throw new Error("Participant IDs must be unique");
    }

    if (uniqueParticipants.length < 2) {
      throw new Error("DMChannel must have at least two distinct participants");
    }
  }

  // Business logic methods
  updateLastMessage(): DMChannelEntity {
    return new DMChannelEntity(
      this.id,
      this.participants,
      this.status,
      this.blockedByMap,
      this.createdAt,
      new Date()
    );
  }

  archive(): DMChannelEntity {
    if (this.status === DMChannelStatus.ARCHIVED) return this;

    return new DMChannelEntity(
      this.id,
      this.participants,
      DMChannelStatus.ARCHIVED,
      this.blockedByMap,
      this.createdAt,
      new Date()
    );
  }

  activate(): DMChannelEntity {
    if (this.status === DMChannelStatus.ACTIVE) return this;

    return new DMChannelEntity(
      this.id,
      this.participants,
      DMChannelStatus.ACTIVE,
      this.blockedByMap,
      this.createdAt,
      new Date()
    );
  }

  blockByUser(blockerUserId: string): DMChannelEntity {
    const newBlockedByMap = { ...this.blockedByMap };
    const otherUserId = this.getOtherParticipant(blockerUserId);

    if (otherUserId) {
      newBlockedByMap[blockerUserId] = otherUserId;
    }

    return new DMChannelEntity(
      this.id,
      this.participants,
      DMChannelStatus.BLOCKED,
      newBlockedByMap,
      this.createdAt,
      new Date()
    );
  }

  unblockByUser(unblockerUserId: string): DMChannelEntity {
    const newBlockedByMap = { ...this.blockedByMap };
    delete newBlockedByMap[unblockerUserId];

    // If no one is blocking anyone, activate the channel
    const newStatus = Object.keys(newBlockedByMap).length === 0 ? DMChannelStatus.ACTIVE : DMChannelStatus.BLOCKED;

    return new DMChannelEntity(
      this.id,
      this.participants,
      newStatus,
      newBlockedByMap,
      this.createdAt,
      new Date()
    );
  }

  block(): DMChannelEntity {
    if (this.status === DMChannelStatus.BLOCKED) return this;

    return new DMChannelEntity(
      this.id,
      this.participants,
      DMChannelStatus.BLOCKED,
      this.blockedByMap,
      this.createdAt,
      new Date()
    );
  }

  markDeleted(): DMChannelEntity {
    if (this.status === DMChannelStatus.DELETED) return this;

    return new DMChannelEntity(
      this.id,
      this.participants,
      DMChannelStatus.DELETED,
      this.blockedByMap,
      this.createdAt,
      new Date()
    );
  }

  isActive(): boolean {
    return this.status === DMChannelStatus.ACTIVE;
  }

  isArchived(): boolean {
    return this.status === DMChannelStatus.ARCHIVED;
  }

  isBlocked(): boolean {
    return this.status === DMChannelStatus.BLOCKED;
  }

  isDeleted(): boolean {
    return this.status === DMChannelStatus.DELETED;
  }

  hasParticipant(userId: string): boolean {
    return this.participants.includes(userId);
  }

  getOtherParticipant(userId: string): string | undefined {
    return this.participants.find((participantId) => participantId !== userId);
  }

  isBlockedByUser(userId: string): boolean {
    return this.blockedByMap[userId] !== undefined;
  }

  isUserBlocked(userId: string): boolean {
    return Object.values(this.blockedByMap).includes(userId);
  }

  getBlockedUsers(): string[] {
    return Object.values(this.blockedByMap);
  }

  getBlockerUsers(): string[] {
    return Object.keys(this.blockedByMap);
  }

  toData(): DMChannelData {
    return {
      id: this.id,
      participants: this.participants,
      status: this.status,
      blockedByMap: this.blockedByMap,
      createdAt: this.createdAt,
      updatedAt: this.updatedAt,
    };
  }

  static fromData(data: DMChannelData): DMChannelEntity {
    return new DMChannelEntity(
      data.id,
      data.participants,
      data.status || DMChannelStatus.ACTIVE,
      data.blockedByMap || {},
      data.createdAt,
      data.updatedAt
    );
  }

  static create(
    id: string,
    participants: string[],
    status: DMChannelStatus = DMChannelStatus.ACTIVE
  ): DMChannelEntity {
    // Ensure participants are distinct
    const distinctParticipants = [...new Set(participants)];

    return new DMChannelEntity(
      id,
      distinctParticipants,
      status,
      {},
      new Date(),
      new Date()
    );
  }

  static createForUsers(
    channelId: string,
    userId1: string,
    userId2: string,
    status: DMChannelStatus = DMChannelStatus.ACTIVE
  ): DMChannelEntity {
    validateId(userId1, "userId1");
    validateId(userId2, "userId2");

    if (userId1 === userId2) {
      throw new Error("Cannot create DM channel with the same user");
    }

    return DMChannelEntity.create(channelId, [userId1, userId2], status);
  }
}
