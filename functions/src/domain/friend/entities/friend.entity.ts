import {CustomResult, Result} from "../../../core/types";
import {ValidationError} from "../../../core/errors";

export enum FriendStatus {
  PENDING = "PENDING", // 수신자 입장에서 응답 대기
  REQUESTED = "REQUESTED", // 요청자 입장에서 대기중
  ACCEPTED = "ACCEPTED", // 친구 관계 성립
  REJECTED = "REJECTED", // 거절됨
  BLOCKED = "BLOCKED", // 차단됨
  REMOVED = "REMOVED", // 친구 관계 해제
}

export class FriendId {
  constructor(public readonly value: string) {
    if (!value || value.trim().length === 0) {
      throw new ValidationError("friendId", "Friend ID cannot be empty");
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
      throw new ValidationError("userId", "User ID cannot be empty");
    }
  }

  equals(other: UserId): boolean {
    return this.value === other.value;
  }

  toString(): string {
    return this.value;
  }
}

export class UserName {
  constructor(public readonly value: string) {
    if (!value || value.trim().length === 0) {
      throw new ValidationError("name", "Name cannot be empty");
    }
  }

  equals(other: UserName): boolean {
    return this.value === other.value;
  }

  toString(): string {
    return this.value;
  }
}

export class ImageUrl {
  constructor(public readonly value: string) {
    if (!value.startsWith("https://")) {
      throw new ValidationError("profileImageUrl", "Profile image must be a valid HTTPS URL");
    }
  }

  equals(other: ImageUrl): boolean {
    return this.value === other.value;
  }

  toString(): string {
    return this.value;
  }
}

export interface FriendData {
  id: string;
  name: string;
  profileImageUrl?: string;
  status: FriendStatus;
  requestedAt?: Date;
  acceptedAt?: Date;
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

export class FriendStatusChangedEvent extends DomainEvent {
  constructor(
    friendId: string,
    public readonly newStatus: FriendStatus
  ) {
    super(friendId);
  }
}

export class FriendNameChangedEvent extends DomainEvent {
  constructor(
    friendId: string,
    public readonly newName: UserName
  ) {
    super(friendId);
  }
}

export class FriendProfileImageChangedEvent extends DomainEvent {
  constructor(
    friendId: string,
    public readonly newProfileImageUrl?: ImageUrl
  ) {
    super(friendId);
  }
}

export class FriendEntity {
  public static readonly COLLECTION_NAME = "friends";

  // Keys matching Android Friend.kt
  public static readonly KEY_STATUS = "status";
  public static readonly KEY_REQUESTED_AT = "requestedAt";
  public static readonly KEY_ACCEPTED_AT = "acceptedAt";
  public static readonly KEY_NAME = "name";
  public static readonly KEY_PROFILE_IMAGE_URL = "profileImageUrl";

  private domainEvents: DomainEvent[] = [];

  constructor(
    private readonly _id: FriendId,
    private _name: UserName,
    private _profileImageUrl: ImageUrl | undefined,
    private _status: FriendStatus,
    private readonly _requestedAt: Date | undefined,
    private _acceptedAt: Date | undefined,
    private readonly _createdAt: Date = new Date(),
    private _updatedAt: Date = new Date()
  ) {
    this.validateInvariant();
  }

  // Getters
  get id(): FriendId {
    return this._id;
  }

  get name(): UserName {
    return this._name;
  }

  get profileImageUrl(): ImageUrl | undefined {
    return this._profileImageUrl;
  }

  get status(): FriendStatus {
    return this._status;
  }

  get requestedAt(): Date | undefined {
    return this._requestedAt;
  }

  get acceptedAt(): Date | undefined {
    return this._acceptedAt;
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

  // Business Logic Methods (matching Android Friend.kt)

  changeName(newName: UserName): void {
    if (this._name.equals(newName)) return;
    this._name = newName;
    this._updatedAt = new Date();
    this.addDomainEvent(new FriendNameChangedEvent(this._id.value, newName));
  }

  changeProfileImage(newProfileImageUrl?: ImageUrl): void {
    if (this._profileImageUrl?.equals(newProfileImageUrl || new ImageUrl("https://placeholder.com"))) return;
    this._profileImageUrl = newProfileImageUrl;
    this._updatedAt = new Date();
    this.addDomainEvent(new FriendProfileImageChangedEvent(this._id.value, newProfileImageUrl));
  }

  acceptRequest(): void {
    if (this._status === FriendStatus.PENDING || this._status === FriendStatus.REQUESTED) {
      this._status = FriendStatus.ACCEPTED;
      this._acceptedAt = new Date();
      this._updatedAt = new Date();
      this.addDomainEvent(new FriendStatusChangedEvent(this._id.value, this._status));
    }
  }

  blockUser(): void {
    if (this._status === FriendStatus.BLOCKED) return;
    this._status = FriendStatus.BLOCKED;
    this._updatedAt = new Date();
    this.addDomainEvent(new FriendStatusChangedEvent(this._id.value, this._status));
  }

  removeFriend(): void {
    if (this._status === FriendStatus.REMOVED) return;
    this._status = FriendStatus.REMOVED;
    this._updatedAt = new Date();
    this.addDomainEvent(new FriendStatusChangedEvent(this._id.value, this._status));
  }

  markAsPending(): void {
    if (this._status === FriendStatus.PENDING) return;
    this._status = FriendStatus.PENDING;
    this._updatedAt = new Date();
    this.addDomainEvent(new FriendStatusChangedEvent(this._id.value, this._status));
  }

  markAsRequested(): void {
    if (this._status === FriendStatus.REQUESTED) return;
    this._status = FriendStatus.REQUESTED;
    this._updatedAt = new Date();
    this.addDomainEvent(new FriendStatusChangedEvent(this._id.value, this._status));
  }

  isActive(): boolean {
    return this._status === FriendStatus.ACCEPTED;
  }

  isPending(): boolean {
    return this._status === FriendStatus.PENDING || this._status === FriendStatus.REQUESTED;
  }

  // Methods matching Android Friend.kt
  accept(): CustomResult<FriendEntity> {
    try {
      if (this._status === FriendStatus.PENDING || this._status === FriendStatus.REQUESTED) {
        this._status = FriendStatus.ACCEPTED;
        this._acceptedAt = new Date();
        this._updatedAt = new Date();
        this.addDomainEvent(new FriendStatusChangedEvent(this._id.value, this._status));
        return Result.success(this);
      }
      return Result.failure(new ValidationError("status", "Cannot accept friend request with current status"));
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error("Failed to accept friend request"));
    }
  }

  reject(): CustomResult<FriendEntity> {
    try {
      if (this._status === FriendStatus.PENDING || this._status === FriendStatus.REQUESTED) {
        this._status = FriendStatus.REJECTED;
        this._updatedAt = new Date();
        this.addDomainEvent(new FriendStatusChangedEvent(this._id.value, this._status));
        return Result.success(this);
      }
      return Result.failure(new ValidationError("status", "Cannot reject friend request with current status"));
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error("Failed to reject friend request"));
    }
  }

  remove(): CustomResult<FriendEntity> {
    try {
      if (this._status === FriendStatus.ACCEPTED) {
        this._status = FriendStatus.REMOVED;
        this._updatedAt = new Date();
        this.addDomainEvent(new FriendStatusChangedEvent(this._id.value, this._status));
        return Result.success(this);
      }
      return Result.failure(new ValidationError("status", "Cannot remove friend with current status"));
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error("Failed to remove friend"));
    }
  }

  private addDomainEvent(event: DomainEvent): void {
    this.domainEvents.push(event);
  }

  clearDomainEvents(): void {
    this.domainEvents = [];
  }

  private validateInvariant(): void {
    if (!Object.values(FriendStatus).includes(this._status)) {
      throw new ValidationError("status", `Invalid friend status: ${this._status}`);
    }

    if (this._requestedAt && this._requestedAt > new Date()) {
      throw new ValidationError("requestedAt", "Requested date cannot be in the future");
    }

    if (this._acceptedAt && this._requestedAt && this._acceptedAt < this._requestedAt) {
      throw new ValidationError("acceptedAt", "Accepted date cannot be before request date");
    }
  }

  toData(): FriendData {
    return {
      id: this._id.value,
      name: this._name.value,
      profileImageUrl: this._profileImageUrl?.value,
      status: this._status,
      requestedAt: this._requestedAt,
      acceptedAt: this._acceptedAt,
      createdAt: this._createdAt,
      updatedAt: this._updatedAt,
    };
  }

  static fromData(data: FriendData): CustomResult<FriendEntity> {
    try {
      const friend = new FriendEntity(
        new FriendId(data.id),
        new UserName(data.name),
        data.profileImageUrl ? new ImageUrl(data.profileImageUrl) : undefined,
        data.status,
        data.requestedAt,
        data.acceptedAt,
        data.createdAt,
        data.updatedAt
      );

      return Result.success(friend);
    } catch (error) {
      return Result.failure(
        error instanceof Error ? error : new Error("Failed to create friend entity")
      );
    }
  }

  // Factory methods matching Android Friend.kt
  static newRequest(
    id: FriendId,
    name: UserName,
    profileImageUrl?: ImageUrl,
    requestedAt?: Date
  ): FriendEntity {
    const friend = new FriendEntity(
      id,
      name,
      profileImageUrl,
      FriendStatus.REQUESTED,
      requestedAt || new Date(),
      undefined,
      new Date(),
      new Date()
    );

    friend.addDomainEvent(
      new FriendRequestSentEvent(id.value, "currentUserId", "friendUserId")
    );

    return friend;
  }

  static receivedRequest(
    id: FriendId,
    name: UserName,
    profileImageUrl?: ImageUrl,
    requestedAt?: Date
  ): FriendEntity {
    const friend = new FriendEntity(
      id,
      name,
      profileImageUrl,
      FriendStatus.PENDING,
      requestedAt || new Date(),
      undefined,
      new Date(),
      new Date()
    );

    return friend;
  }

  static fromDataSource(
    id: FriendId,
    name: UserName,
    profileImageUrl: ImageUrl | undefined,
    status: FriendStatus,
    requestedAt?: Date,
    acceptedAt?: Date,
    createdAt?: Date,
    updatedAt?: Date
  ): FriendEntity {
    return new FriendEntity(
      id,
      name,
      profileImageUrl,
      status,
      requestedAt,
      acceptedAt,
      createdAt || new Date(),
      updatedAt || new Date()
    );
  }
}
