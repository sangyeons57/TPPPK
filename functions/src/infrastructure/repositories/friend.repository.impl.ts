import {FriendRepository} from "../../domain/friend/repositories/friend.repository";
import {FriendDatasource, FriendSearchCriteria} from "../datasources/interfaces/friend.datasource";
import {FriendEntity, FriendStatus} from "../../domain/friend/entities/friend.entity";
import {CustomResult} from "../../core/types";

/**
 * Friend Repository 구현체
 * FriendDatasource를 사용하여 Repository 인터페이스를 구현
 */
export class FriendRepositoryImpl implements FriendRepository {
  constructor(private readonly datasource: FriendDatasource) {}

  async findById(id: string): Promise<CustomResult<FriendEntity | null>> {
    return this.datasource.findById(id);
  }

  async findByUserIds(userId: string, friendUserId: string): Promise<CustomResult<FriendEntity | null>> {
    return this.datasource.findByUserIds(userId, friendUserId);
  }

  async findFriendsByUserId(userId: string, status?: FriendStatus): Promise<CustomResult<FriendEntity[]>> {
    return this.datasource.findFriendsByUserId(userId, status);
  }

  async findReceivedFriendRequests(userId: string): Promise<CustomResult<FriendEntity[]>> {
    return this.datasource.findReceivedFriendRequests(userId);
  }

  async findSentFriendRequests(userId: string): Promise<CustomResult<FriendEntity[]>> {
    return this.datasource.findSentFriendRequests(userId);
  }

  async areUsersFriends(userId: string, friendUserId: string): Promise<CustomResult<boolean>> {
    return this.datasource.areUsersFriends(userId, friendUserId);
  }

  async friendRequestExists(requesterId: string, receiverId: string): Promise<CustomResult<boolean>> {
    return this.datasource.friendRequestExists(requesterId, receiverId);
  }

  async save(userId: string, friend: FriendEntity): Promise<CustomResult<FriendEntity>> {
    return this.datasource.save(userId, friend);
  }

  async update(userId: string, friend: FriendEntity): Promise<CustomResult<FriendEntity>> {
    return this.datasource.update(userId, friend);
  }

  async delete(id: string): Promise<CustomResult<void>> {
    return this.datasource.delete(id);
  }

  async deleteByUserIds(userId: string, friendUserId: string): Promise<CustomResult<void>> {
    return this.datasource.deleteByUserIds(userId, friendUserId);
  }

  async deleteAllByUserId(userId: string): Promise<CustomResult<void>> {
    return this.datasource.deleteAllByUserId(userId);
  }

  async findByCriteria(criteria: FriendSearchCriteria): Promise<CustomResult<FriendEntity[]>> {
    return this.datasource.findByCriteria(criteria);
  }

  async countFriendsByUserId(userId: string): Promise<CustomResult<number>> {
    return this.datasource.countFriendsByUserId(userId);
  }

  async countPendingRequestsByUserId(userId: string): Promise<CustomResult<number>> {
    return this.datasource.countPendingRequestsByUserId(userId);
  }
}
