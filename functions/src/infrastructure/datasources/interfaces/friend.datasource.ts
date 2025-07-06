import { CustomResult } from '../../../core/types';
import { FriendEntity, FriendStatus } from '../../../domain/friend/entities/friend.entity';
import { UserId, FriendId } from '../../../core/validation';

export interface FriendSearchCriteria {
  userId?: string;
  friendUserId?: string;
  status?: FriendStatus;
  limit?: number;
  offset?: number;
}

/**
 * Friend 데이터 소스 인터페이스
 * 순수한 데이터 접근 계층의 역할만 담당
 */
export interface FriendDatasource {
  /**
   * ID로 친구 관계를 찾습니다.
   */
  findById(id: string): Promise<CustomResult<FriendEntity | null>>;

  /**
   * 두 사용자 간의 친구 관계를 찾습니다.
   */
  findByUserIds(userId: string, friendUserId: string): Promise<CustomResult<FriendEntity | null>>;

  /**
   * 사용자의 모든 친구를 조회합니다.
   */
  findFriendsByUserId(userId: string, status?: FriendStatus): Promise<CustomResult<FriendEntity[]>>;

  /**
   * 사용자가 받은 친구 요청을 조회합니다.
   */
  findReceivedFriendRequests(userId: string): Promise<CustomResult<FriendEntity[]>>;

  /**
   * 사용자가 보낸 친구 요청을 조회합니다.
   */
  findSentFriendRequests(userId: string): Promise<CustomResult<FriendEntity[]>>;

  /**
   * 두 사용자가 이미 친구인지 확인합니다.
   */
  areUsersFriends(userId: string, friendUserId: string): Promise<CustomResult<boolean>>;

  /**
   * 친구 요청이 이미 존재하는지 확인합니다.
   */
  friendRequestExists(requesterId: string, receiverId: string): Promise<CustomResult<boolean>>;

  /**
   * 친구 관계를 저장합니다.
   */
  save(friend: FriendEntity): Promise<CustomResult<FriendEntity>>;

  /**
   * 친구 관계를 업데이트합니다.
   */
  update(friend: FriendEntity): Promise<CustomResult<FriendEntity>>;

  /**
   * 친구 관계를 삭제합니다.
   */
  delete(id: string): Promise<CustomResult<void>>;

  /**
   * 두 사용자 간의 모든 친구 관계를 삭제합니다.
   */
  deleteByUserIds(userId: string, friendUserId: string): Promise<CustomResult<void>>;

  /**
   * 사용자와 관련된 모든 친구 관계를 삭제합니다.
   */
  deleteAllByUserId(userId: string): Promise<CustomResult<void>>;

  /**
   * 검색 조건에 따라 친구 관계를 조회합니다.
   */
  findByCriteria(criteria: FriendSearchCriteria): Promise<CustomResult<FriendEntity[]>>;

  /**
   * 사용자의 친구 수를 조회합니다.
   */
  countFriendsByUserId(userId: string): Promise<CustomResult<number>>;

  /**
   * 사용자의 대기 중인 친구 요청 수를 조회합니다.
   */
  countPendingRequestsByUserId(userId: string): Promise<CustomResult<number>>;
}