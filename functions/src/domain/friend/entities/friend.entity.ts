import {CustomResult, Result} from "../../../core/types";
import {ValidationError} from "../../../core/errors";
import {FriendId, validateId, validateUsername, validateImageUrl} from "../../../core/validation";

export enum FriendStatus {
  PENDING = "PENDING", // 수신자 입장에서 응답 대기
  REQUESTED = "REQUESTED", // 요청자 입장에서 대기중
  ACCEPTED = "ACCEPTED", // 친구 관계 성립
  REJECTED = "REJECTED", // 거절됨
  BLOCKED = "BLOCKED", // 차단됨
  REMOVED = "REMOVED", // 친구 관계 해제
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



export class FriendEntity {
  public static readonly COLLECTION_NAME = "friends";

  // Keys matching Android Friend.kt
  public static readonly KEY_STATUS = "status";
  public static readonly KEY_REQUESTED_AT = "requestedAt";
  public static readonly KEY_ACCEPTED_AT = "acceptedAt";
  public static readonly KEY_NAME = "name";
  public static readonly KEY_PROFILE_IMAGE_URL = "profileImageUrl";


  constructor(
    private readonly _id: FriendId,
    private _name: string,
    private _profileImageUrl: string | undefined,
    private _status: FriendStatus,
    private readonly _requestedAt: Date | undefined,
    private _acceptedAt: Date | undefined,
    private readonly _createdAt: Date = new Date(),
    private _updatedAt: Date = new Date()
  ) {
    // Validate inputs
    validateId(_id, "friendId");
    validateUsername(_name);
    if (_profileImageUrl) {
      validateImageUrl(_profileImageUrl);
    }
    this.validateInvariant();
  }

  // Getters
  get id(): FriendId {
    return this._id;
  }

  get name(): string {
    return this._name;
  }

  get profileImageUrl(): string | undefined {
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


  // Business Logic Methods (matching Android Friend.kt)

  changeName(newName: string): void {
    validateUsername(newName);
    if (this._name === newName) return;
    this._name = newName;
    this._updatedAt = new Date();
  }

  changeProfileImage(newProfileImageUrl?: string): void {
    if (newProfileImageUrl) {
      validateImageUrl(newProfileImageUrl);
    }
    if (this._profileImageUrl === newProfileImageUrl) return;
    this._profileImageUrl = newProfileImageUrl;
    this._updatedAt = new Date();
  }

  acceptRequest(): void {
    if (this._status === FriendStatus.PENDING || this._status === FriendStatus.REQUESTED) {
      this._status = FriendStatus.ACCEPTED;
      this._acceptedAt = new Date();
      this._updatedAt = new Date();
      }
  }

  blockUser(): void {
    if (this._status === FriendStatus.BLOCKED) return;
    this._status = FriendStatus.BLOCKED;
    this._updatedAt = new Date();
  }

  removeFriend(): void {
    if (this._status === FriendStatus.REMOVED) return;
    this._status = FriendStatus.REMOVED;
    this._updatedAt = new Date();
  }

  markAsPending(): void {
    if (this._status === FriendStatus.PENDING) return;
    this._status = FriendStatus.PENDING;
    this._updatedAt = new Date();
  }

  markAsRequested(): void {
    if (this._status === FriendStatus.REQUESTED) return;
    this._status = FriendStatus.REQUESTED;
    this._updatedAt = new Date();
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
            return Result.success(this);
      }
      return Result.failure(new ValidationError("status", "Cannot remove friend with current status"));
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error("Failed to remove friend"));
    }
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
      id: this._id,
      name: this._name,
      profileImageUrl: this._profileImageUrl,
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
        data.id,
        data.name,
        data.profileImageUrl,
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
    name: string,
    profileImageUrl?: string,
    requestedAt?: Date
  ): FriendEntity {
    return new FriendEntity(
      id,
      name,
      profileImageUrl,
      FriendStatus.REQUESTED,
      requestedAt || new Date(),
      undefined,
      new Date(),
      new Date()
    );
  }

  static receivedRequest(
    id: FriendId,
    name: string,
    profileImageUrl?: string,
    requestedAt?: Date
  ): FriendEntity {
    return new FriendEntity(
      id,
      name,
      profileImageUrl,
      FriendStatus.PENDING,
      requestedAt || new Date(),
      undefined,
      new Date(),
      new Date()
    );
  }

  static fromDataSource(
    id: FriendId,
    name: string,
    profileImageUrl: string | undefined,
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
