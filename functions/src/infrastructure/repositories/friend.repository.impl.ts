import {FriendRepository} from "../../domain/friend/repositories/friend.repository";
import {FriendDatasource, FriendSearchCriteria} from "../datasources/interfaces/friend.datasource";
import {FriendEntity, UserId, FriendId, FriendStatus} from "../../domain/friend/entities/friend.entity";
import {CustomResult} from "../../core/types";

/**
 * Friend Repository 구현체
 * FriendDatasource를 사용하여 Repository 인터페이스를 구현
 */
export class FriendRepositoryImpl implements FriendRepository {
  constructor(private readonly datasource: FriendDatasource) {}

  async findById(id: FriendId): Promise<CustomResult<FriendEntity | null>> {
    return this.datasource.findById(id);
  }

  async findByUserIds(userId: UserId, friendUserId: UserId): Promise<CustomResult<FriendEntity | null>> {
    return this.datasource.findByUserIds(userId, friendUserId);
  }

  async findFriendsByUserId(userId: UserId, status?: FriendStatus): Promise<CustomResult<FriendEntity[]>> {
    return this.datasource.findFriendsByUserId(userId, status);
  }

  async findReceivedFriendRequests(userId: UserId): Promise<CustomResult<FriendEntity[]>> {
    return this.datasource.findReceivedFriendRequests(userId);
  }

  async findSentFriendRequests(userId: UserId): Promise<CustomResult<FriendEntity[]>> {
    return this.datasource.findSentFriendRequests(userId);
  }

  async areUsersFriends(userId: UserId, friendUserId: UserId): Promise<CustomResult<boolean>> {
    return this.datasource.areUsersFriends(userId, friendUserId);
  }

  async friendRequestExists(requesterId: UserId, receiverId: UserId): Promise<CustomResult<boolean>> {
    return this.datasource.friendRequestExists(requesterId, receiverId);
  }

  async save(friend: FriendEntity): Promise<CustomResult<FriendEntity>> {
    return this.datasource.save(friend);
  }

  async update(friend: FriendEntity): Promise<CustomResult<FriendEntity>> {
    return this.datasource.update(friend);
  }

  async delete(id: FriendId): Promise<CustomResult<void>> {
    return this.datasource.delete(id);
  }

  async deleteByUserIds(userId: UserId, friendUserId: UserId): Promise<CustomResult<void>> {
    return this.datasource.deleteByUserIds(userId, friendUserId);
  }

  async deleteAllByUserId(userId: UserId): Promise<CustomResult<void>> {
    return this.datasource.deleteAllByUserId(userId);
  }

  async findByCriteria(criteria: FriendSearchCriteria): Promise<CustomResult<FriendEntity[]>> {
    return this.datasource.findByCriteria(criteria);
  }

  async countFriendsByUserId(userId: UserId): Promise<CustomResult<number>> {
    return this.datasource.countFriendsByUserId(userId);
  }

  async countPendingRequestsByUserId(userId: UserId): Promise<CustomResult<number>> {
    return this.datasource.countPendingRequestsByUserId(userId);
  }
}
