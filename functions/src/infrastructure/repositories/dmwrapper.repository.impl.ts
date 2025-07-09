import {CustomResult} from "../../core/types";
import {DMWrapperRepository} from "../../domain/dmwrapper/repositories/dmwrapper.repository";
import {DMWrapperEntity} from "../../domain/dmwrapper/entities/dmwrapper.entity";
import {DMWrapperDatasource} from "../datasources/firestore/dmwrapper.datasource";

export class DMWrapperRepositoryImpl implements DMWrapperRepository {
  constructor(private readonly datasource: DMWrapperDatasource) {}

  async save(userId: string, dmWrapper: DMWrapperEntity): Promise<CustomResult<DMWrapperEntity>> {
    return this.datasource.save(userId, dmWrapper);
  }

  async findByUserAndOtherUser(userId: string, otherUserId: string): Promise<CustomResult<DMWrapperEntity | null>> {
    return this.datasource.findByUserAndOtherUser(userId, otherUserId);
  }

  async findByUserId(userId: string): Promise<CustomResult<DMWrapperEntity[]>> {
    return this.datasource.findByUserId(userId);
  }

  async update(userId: string, dmWrapper: DMWrapperEntity): Promise<CustomResult<DMWrapperEntity>> {
    return this.datasource.update(userId, dmWrapper);
  }

  async delete(userId: string, channelId: string): Promise<CustomResult<void>> {
    return this.datasource.delete(userId, channelId);
  }
}
