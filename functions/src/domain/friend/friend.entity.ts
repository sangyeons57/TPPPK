import { CustomResult, Result } from '../../core/types';
import { ValidationError, ConflictError } from '../../core/errors';

export enum FriendStatus {
  PENDING = 'PENDING',       // 요청자 입장에서 대기중
  REQUESTED = 'REQUESTED',   // 수신자 입장에서 응답 대기
  ACCEPTED = 'ACCEPTED',     // 친구 관계 성립
  REJECTED = 'REJECTED',     // 거절됨
  BLOCKED = 'BLOCKED',       // 차단됨
  REMOVED = 'REMOVED'        // 친구 관계 해제
}

export class FriendId {
  constructor(public readonly value: string) {
    if (!value || value.trim().length === 0) {
      throw new ValidationError('friendId', 'Friend ID cannot be empty');
    }
  }

  equals(other: FriendId): boolean {
    return this.value === other.value;
  }

  toString(): string {
    return this.value;
  }
}

export class UserId {
  constructor(public readonly value: string) {
    if (!value || value.trim().length === 0) {
      throw new ValidationError('userId', 'User ID cannot be empty');
    }
  }

  equals(other: UserId): boolean {
    return this.value === other.value;
  }

  toString(): string {
    return this.value;
  }
}

export interface FriendData {
  id: string;
  userId: string;
  friendUserId: string;
  status: FriendStatus;
  requestedAt: Date;
  respondedAt?: Date;
  createdAt: Date;
  updatedAt: Date;
}

// Domain Events
export abstract class DomainEvent {
  public readonly occurredAt: Date;

  constructor(public readonly aggregateId: string) {
    this.occurredAt = new Date();
  }
}

export class FriendRequestSentEvent extends DomainEvent {
  constructor(
    friendId: string,
    public readonly requesterId: string,
    public readonly receiverId: string
  ) {
    super(friendId);
  }
}

export class FriendRequestAcceptedEvent extends DomainEvent {
  constructor(
    friendId: string,
    public readonly requesterId: string,
    public readonly receiverId: string
  ) {
    super(friendId);
  }
}

export class FriendRequestRejectedEvent extends DomainEvent {
  constructor(
    friendId: string,
    public readonly requesterId: string,
    public readonly receiverId: string
  ) {
    super(friendId);
  }
}

export class FriendRemovedEvent extends DomainEvent {
  constructor(
    friendId: string,
    public readonly userId: string,
    public readonly friendUserId: string
  ) {
    super(friendId);
  }
}

export class FriendEntity {
  private domainEvents: DomainEvent[] = [];

  constructor(
    private readonly _id: FriendId,
    private readonly _userId: UserId,
    private readonly _friendUserId: UserId,
    private _status: FriendStatus,
    private readonly _requestedAt: Date,
    private _respondedAt?: Date,
    private readonly _createdAt: Date = new Date(),
    private _updatedAt: Date = new Date()
  ) {
    this.validateInvariant();
  }

  // Getters
  get id(): FriendId {
    return this._id;
  }

  get userId(): UserId {
    return this._userId;
  }

  get friendUserId(): UserId {
    return this._friendUserId;
  }

  get status(): FriendStatus {
    return this._status;
  }

  get requestedAt(): Date {
    return this._requestedAt;
  }

  get respondedAt(): Date | undefined {
    return this._respondedAt;
  }

  get createdAt(): Date {
    return this._createdAt;
  }

  get updatedAt(): Date {
    return this._updatedAt;
  }

  get domainEventsSnapshot(): DomainEvent[] {
    return [...this.domainEvents];
  }

  // Business Logic Methods

  /**
   * 친구 요청을 수락합니다.
   * REQUESTED 상태에서만 호출 가능합니다.
   */
  accept(): CustomResult<FriendEntity> {
    if (this._status !== FriendStatus.REQUESTED) {
      return Result.failure(
        new ConflictError(`Cannot accept friend request. Current status: ${this._status}. Expected: ${FriendStatus.REQUESTED}`)
      );
    }

    const now = new Date();
    const acceptedFriend = new FriendEntity(
      this._id,
      this._userId,
      this._friendUserId,
      FriendStatus.ACCEPTED,
      this._requestedAt,
      now,
      this._createdAt,
      now
    );

    acceptedFriend.addDomainEvent(
      new FriendRequestAcceptedEvent(
        this._id.value,
        this._userId.value,
        this._friendUserId.value
      )
    );

    return Result.success(acceptedFriend);
  }

  /**
   * 친구 요청을 거절합니다.
   * REQUESTED 상태에서만 호출 가능합니다.
   */
  reject(): CustomResult<FriendEntity> {
    if (this._status !== FriendStatus.REQUESTED) {
      return Result.failure(
        new ConflictError(`Cannot reject friend request. Current status: ${this._status}. Expected: ${FriendStatus.REQUESTED}`)
      );
    }

    const now = new Date();
    const rejectedFriend = new FriendEntity(
      this._id,
      this._userId,
      this._friendUserId,
      FriendStatus.REJECTED,
      this._requestedAt,
      now,
      this._createdAt,
      now
    );

    rejectedFriend.addDomainEvent(
      new FriendRequestRejectedEvent(
        this._id.value,
        this._userId.value,
        this._friendUserId.value
      )
    );

    return Result.success(rejectedFriend);
  }

  /**
   * 친구 관계를 해제합니다.
   * ACCEPTED 상태에서만 호출 가능합니다.
   */
  remove(): CustomResult<FriendEntity> {
    if (this._status !== FriendStatus.ACCEPTED) {
      return Result.failure(
        new ConflictError(`Cannot remove friend. Current status: ${this._status}. Expected: ${FriendStatus.ACCEPTED}`)
      );
    }

    const now = new Date();
    const removedFriend = new FriendEntity(
      this._id,
      this._userId,
      this._friendUserId,
      FriendStatus.REMOVED,
      this._requestedAt,
      this._respondedAt,
      this._createdAt,
      now
    );

    removedFriend.addDomainEvent(
      new FriendRemovedEvent(
        this._id.value,
        this._userId.value,
        this._friendUserId.value
      )
    );

    return Result.success(removedFriend);
  }

  /**
   * 사용자가 이 친구 관계에서 요청자인지 확인합니다.
   */
  isRequester(userId: UserId): boolean {
    return this._userId.equals(userId);
  }

  /**
   * 사용자가 이 친구 관계에서 수신자인지 확인합니다.
   */
  isReceiver(userId: UserId): boolean {
    return this._friendUserId.equals(userId);
  }

  /**
   * 친구 관계가 활성 상태인지 확인합니다.
   */
  isActive(): boolean {
    return this._status === FriendStatus.ACCEPTED;
  }

  /**
   * 친구 관계가 대기 중인지 확인합니다.
   */
  isPending(): boolean {
    return this._status === FriendStatus.PENDING || this._status === FriendStatus.REQUESTED;
  }

  /**
   * 도메인 이벤트를 추가합니다.
   */
  private addDomainEvent(event: DomainEvent): void {
    this.domainEvents.push(event);
  }

  /**
   * 도메인 이벤트를 클리어합니다.
   */
  clearDomainEvents(): void {
    this.domainEvents = [];
  }

  /**
   * 불변 조건을 검증합니다.
   */
  private validateInvariant(): void {
    if (this._userId.equals(this._friendUserId)) {
      throw new ValidationError('friendUserId', 'Cannot be friends with yourself');
    }

    if (!Object.values(FriendStatus).includes(this._status)) {
      throw new ValidationError('status', `Invalid friend status: ${this._status}`);
    }

    if (this._requestedAt > new Date()) {
      throw new ValidationError('requestedAt', 'Requested date cannot be in the future');
    }

    if (this._respondedAt && this._respondedAt < this._requestedAt) {
      throw new ValidationError('respondedAt', 'Response date cannot be before request date');
    }
  }

  /**
   * 데이터 객체로 변환합니다.
   */
  toData(): FriendData {
    return {
      id: this._id.value,
      userId: this._userId.value,
      friendUserId: this._friendUserId.value,
      status: this._status,
      requestedAt: this._requestedAt,
      respondedAt: this._respondedAt,
      createdAt: this._createdAt,
      updatedAt: this._updatedAt
    };
  }

  /**
   * 데이터 객체로부터 엔티티를 생성합니다.
   */
  static fromData(data: FriendData): CustomResult<FriendEntity> {
    try {
      const friend = new FriendEntity(
        new FriendId(data.id),
        new UserId(data.userId),
        new UserId(data.friendUserId),
        data.status,
        data.requestedAt,
        data.respondedAt,
        data.createdAt,
        data.updatedAt
      );

      return Result.success(friend);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to create friend entity'));
    }
  }

  /**
   * 새로운 친구 요청을 생성합니다.
   */
  static createFriendRequest(
    requesterId: UserId,
    receiverId: UserId
  ): CustomResult<FriendEntity> {
    try {
      const now = new Date();
      const friendId = new FriendId(`friend_${requesterId.value}_${receiverId.value}_${now.getTime()}`);
      
      const friend = new FriendEntity(
        friendId,
        requesterId,
        receiverId,
        FriendStatus.REQUESTED,
        now,
        undefined,
        now,
        now
      );

      friend.addDomainEvent(
        new FriendRequestSentEvent(
          friendId.value,
          requesterId.value,
          receiverId.value
        )
      );

      return Result.success(friend);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to create friend request'));
    }
  }
}