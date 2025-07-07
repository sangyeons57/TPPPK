import {CustomResult} from "../../core/types";
import {DMChannelRepository} from "../../domain/dmchannel/repositories/dmchannel.repository";
import {DMChannelEntity} from "../../domain/dmchannel/entities/dmchannel.entity";
import {DMChannelDatasource} from "../datasources/firestore/dmchannel.datasource";

export class DMChannelRepositoryImpl implements DMChannelRepository {
  constructor(private readonly datasource: DMChannelDatasource) {}

  async save(dmChannel: DMChannelEntity): Promise<CustomResult<DMChannelEntity>> {
    return this.datasource.save(dmChannel);
  }

  async findById(channelId: string): Promise<CustomResult<DMChannelEntity | null>> {
    return this.datasource.findById(channelId);
  }

  async findByParticipants(userId1: string, userId2: string): Promise<CustomResult<DMChannelEntity | null>> {
    return this.datasource.findByParticipants(userId1, userId2);
  }

  async delete(channelId: string): Promise<CustomResult<void>> {
    return this.datasource.delete(channelId);
  }
}