import {CustomResult} from "../../../core/types";
import {DMWrapperEntity} from "../entities/dmwrapper.entity";

export interface DMWrapperRepository {
  save(userId: string, dmWrapper: DMWrapperEntity): Promise<CustomResult<DMWrapperEntity>>;
  findByUserAndOtherUser(userId: string, otherUserId: string): Promise<CustomResult<DMWrapperEntity | null>>;
  findByUserId(userId: string): Promise<CustomResult<DMWrapperEntity[]>>;
  update(userId: string, dmWrapper: DMWrapperEntity): Promise<CustomResult<DMWrapperEntity>>;
  delete(userId: string, channelId: string): Promise<CustomResult<void>>;
}