import {BaseEntity} from "../../../core/types";
import {validateId} from "../../../core/validation";

// DMChannel Data Interface (for Firestore mapping)
export interface DMChannelData {
  id: string;
  participants: string[];
  createdAt: Date;
  updatedAt: Date;
}

// DMChannel Entity (matching Android DMChannel.kt structure)
export class DMChannelEntity implements BaseEntity {
  public static readonly COLLECTION_NAME = "dm_channels";

  // Keys matching Android DMChannel.kt
  public static readonly KEY_PARTICIPANTS = "participants";
  public static readonly KEY_CREATED_AT = "createdAt";
  public static readonly KEY_UPDATED_AT = "updatedAt";

  constructor(
    public readonly id: string,
    public readonly participants: string[],
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
      this.createdAt,
      new Date()
    );
  }

  hasParticipant(userId: string): boolean {
    return this.participants.includes(userId);
  }

  getOtherParticipant(userId: string): string | undefined {
    return this.participants.find(participantId => participantId !== userId);
  }

  toData(): DMChannelData {
    return {
      id: this.id,
      participants: this.participants,
      createdAt: this.createdAt,
      updatedAt: this.updatedAt,
    };
  }

  static fromData(data: DMChannelData): DMChannelEntity {
    return new DMChannelEntity(
      data.id,
      data.participants,
      data.createdAt,
      data.updatedAt
    );
  }

  static create(
    id: string,
    participants: string[]
  ): DMChannelEntity {
    // Ensure participants are distinct
    const distinctParticipants = [...new Set(participants)];
    
    return new DMChannelEntity(
      id,
      distinctParticipants,
      new Date(),
      new Date()
    );
  }

  static createForUsers(
    channelId: string,
    userId1: string,
    userId2: string
  ): DMChannelEntity {
    validateId(userId1, "userId1");
    validateId(userId2, "userId2");
    
    if (userId1 === userId2) {
      throw new Error("Cannot create DM channel with the same user");
    }
    
    return DMChannelEntity.create(channelId, [userId1, userId2]);
  }
}