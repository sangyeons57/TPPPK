import {CustomResult} from "../../../core/types";
import {DMChannelEntity} from "../entities/dmchannel.entity";

export interface DMChannelRepository {
  save(dmChannel: DMChannelEntity): Promise<CustomResult<DMChannelEntity>>;
  findById(channelId: string): Promise<CustomResult<DMChannelEntity | null>>;
  findByParticipants(userId1: string, userId2: string): Promise<CustomResult<DMChannelEntity | null>>;
  delete(channelId: string): Promise<CustomResult<void>>;
}