import {BaseEntity} from "../../../core/types";
import {ValidationError} from "../../../core/errors";
import {validateId, validateProjectName, validateImageUrl} from "../../../core/validation";

// ProjectWrapper Status Enum (프로젝트 참여 상태)
export enum ProjectWrapperStatus {
  ACTIVE = "ACTIVE", // 활성 참여
  ARCHIVED = "ARCHIVED", // 아카이브됨
  LEFT = "LEFT", // 떠났음
  REMOVED = "REMOVED", // 제거됨
  UNKNOWN = "UNKNOWN" // 알 수 없음 (Android 호환)
}

// ProjectWrapper Data Interface (for Firestore mapping)
export interface ProjectWrapperData {
  id: string; // 이것이 곧 projectId
  projectName: string;
  projectImageUrl?: string;
  status: ProjectWrapperStatus;
  joinedAt: Date;
  lastUpdatedAt: Date;
  createdAt: Date;
  updatedAt: Date;
}

// ProjectWrapper Entity (사용자별 프로젝트 요약 정보)
export class ProjectWrapperEntity implements BaseEntity {
  public static readonly COLLECTION_NAME = "project_wrappers";

  // Keys matching Android ProjectWrapper.kt
  public static readonly KEY_PROJECT_NAME = "projectName";
  public static readonly KEY_PROJECT_IMAGE_URL = "projectImageUrl";
  public static readonly KEY_STATUS = "status";
  public static readonly KEY_JOINED_AT = "joinedAt";
  public static readonly KEY_LAST_UPDATED_AT = "lastUpdatedAt";
  public static readonly KEY_CREATED_AT = "createdAt";
  public static readonly KEY_UPDATED_AT = "updatedAt";

  constructor(
    private readonly _id: string, // 이것이 곧 projectId
    private _projectName: string,
    private _projectImageUrl: string | undefined,
    private _status: ProjectWrapperStatus,
    private readonly _joinedAt: Date,
    private _lastUpdatedAt: Date,
    private readonly _createdAt: Date = new Date(),
    private _updatedAt: Date = new Date()
  ) {
    // Validate inputs
    validateId(_id, "projectId");
    validateProjectName(_projectName);
    if (_projectImageUrl) {
      validateImageUrl(_projectImageUrl);
    }
    this.validateInvariant();
  }

  // Getters
  get id(): string {
    return this._id;
  }

  get projectId(): string {
    return this._id; // id가 곧 projectId
  }

  get projectName(): string {
    return this._projectName;
  }

  get projectImageUrl(): string | undefined {
    return this._projectImageUrl;
  }

  get status(): ProjectWrapperStatus {
    return this._status;
  }

  get joinedAt(): Date {
    return this._joinedAt;
  }

  get lastUpdatedAt(): Date {
    return this._lastUpdatedAt;
  }

  get createdAt(): Date {
    return this._createdAt;
  }

  get updatedAt(): Date {
    return this._updatedAt;
  }

  // Business Logic Methods

  /**
   * 프로젝트 이름을 업데이트합니다.
   * @param {string} newName - 새로운 프로젝트 이름
   * @return {ProjectWrapperEntity} 업데이트된 프로젝트 랩퍼 엔티티
   */
  updateProjectName(newName: string): ProjectWrapperEntity {
    validateProjectName(newName);
    if (this._projectName === newName) return this;

    return new ProjectWrapperEntity(
      this._id,
      newName,
      this._projectImageUrl,
      this._status,
      this._joinedAt,
      new Date(), // lastUpdatedAt 갱신
      this._createdAt,
      new Date() // updatedAt 갱신
    );
  }

  /**
   * 프로젝트 이미지를 업데이트합니다.
   * @param {string} newImageUrl - 새로운 프로젝트 이미지 URL
   * @return {ProjectWrapperEntity} 업데이트된 프로젝트 랩퍼 엔티티
   */
  updateProjectImage(newImageUrl?: string): ProjectWrapperEntity {
    if (newImageUrl) {
      validateImageUrl(newImageUrl);
    }
    if (this._projectImageUrl === newImageUrl) return this;

    return new ProjectWrapperEntity(
      this._id,
      this._projectName,
      newImageUrl,
      this._status,
      this._joinedAt,
      new Date(), // lastUpdatedAt 갱신
      this._createdAt,
      new Date() // updatedAt 갱신
    );
  }

  /**
   * 프로젝트 정보를 일괄 업데이트합니다.
   * @param {Object} updates - 업데이트할 프로젝트 정보
   * @param {string} updates.name - 새로운 프로젝트 이름
   * @param {string} updates.imageUrl - 새로운 프로젝트 이미지 URL
   * @return {ProjectWrapperEntity} 업데이트된 프로젝트 랩퍼 엔티티
   */
  updateProjectInfo(updates: {
    name?: string;
    imageUrl?: string;
  }): ProjectWrapperEntity {
    let hasChanges = false;
    const newName = updates.name || this._projectName;
    const newImageUrl = updates.imageUrl !== undefined ? updates.imageUrl : this._projectImageUrl;

    if (updates.name) {
      validateProjectName(updates.name);
      hasChanges = hasChanges || (this._projectName !== updates.name);
    }

    if (updates.imageUrl !== undefined) {
      if (updates.imageUrl) {
        validateImageUrl(updates.imageUrl);
      }
      hasChanges = hasChanges || (this._projectImageUrl !== updates.imageUrl);
    }

    if (!hasChanges) return this;

    return new ProjectWrapperEntity(
      this._id,
      newName,
      newImageUrl,
      this._status,
      this._joinedAt,
      new Date(), // lastUpdatedAt 갱신
      this._createdAt,
      new Date() // updatedAt 갱신
    );
  }

  /**
   * 프로젝트를 아카이브합니다.
   * @return {ProjectWrapperEntity} 아카이브된 프로젝트 랩퍼 엔티티
   */
  archive(): ProjectWrapperEntity {
    if (this._status === ProjectWrapperStatus.ARCHIVED) return this;

    return new ProjectWrapperEntity(
      this._id,
      this._projectName,
      this._projectImageUrl,
      ProjectWrapperStatus.ARCHIVED,
      this._joinedAt,
      new Date(), // lastUpdatedAt 갱신
      this._createdAt,
      new Date() // updatedAt 갱신
    );
  }

  /**
   * 프로젝트를 활성화합니다.
   * @return {ProjectWrapperEntity} 활성화된 프로젝트 랩퍼 엔티티
   */
  activate(): ProjectWrapperEntity {
    if (this._status === ProjectWrapperStatus.ACTIVE) return this;

    return new ProjectWrapperEntity(
      this._id,
      this._projectName,
      this._projectImageUrl,
      ProjectWrapperStatus.ACTIVE,
      this._joinedAt,
      new Date(), // lastUpdatedAt 갱신
      this._createdAt,
      new Date() // updatedAt 갱신
    );
  }

  /**
   * 사용자가 프로젝트를 떠난 것으로 표시합니다.
   * @return {ProjectWrapperEntity} 상태가 업데이트된 프로젝트 랩퍼 엔티티
   */
  markAsLeft(): ProjectWrapperEntity {
    if (this._status === ProjectWrapperStatus.LEFT) return this;

    return new ProjectWrapperEntity(
      this._id,
      this._projectName,
      this._projectImageUrl,
      ProjectWrapperStatus.LEFT,
      this._joinedAt,
      new Date(), // lastUpdatedAt 갱신
      this._createdAt,
      new Date() // updatedAt 갱신
    );
  }

  /**
   * 사용자가 프로젝트에서 제거된 것으로 표시합니다.
   * @return {ProjectWrapperEntity} 상태가 업데이트된 프로젝트 랩퍼 엔티티
   */
  markAsRemoved(): ProjectWrapperEntity {
    if (this._status === ProjectWrapperStatus.REMOVED) return this;

    return new ProjectWrapperEntity(
      this._id,
      this._projectName,
      this._projectImageUrl,
      ProjectWrapperStatus.REMOVED,
      this._joinedAt,
      new Date(), // lastUpdatedAt 갱신
      this._createdAt,
      new Date() // updatedAt 갱신
    );
  }

  // Status Check Methods
  isActive(): boolean {
    return this._status === ProjectWrapperStatus.ACTIVE;
  }

  isArchived(): boolean {
    return this._status === ProjectWrapperStatus.ARCHIVED;
  }

  hasLeft(): boolean {
    return this._status === ProjectWrapperStatus.LEFT;
  }

  isRemoved(): boolean {
    return this._status === ProjectWrapperStatus.REMOVED;
  }

  // Validation
  private validateInvariant(): void {
    if (!this._projectName || this._projectName.trim().length === 0) {
      throw new ValidationError("projectName", "Project name cannot be empty");
    }
    if (this._joinedAt > new Date()) {
      throw new ValidationError("joinedAt", "Joined date cannot be in the future");
    }
    if (this._createdAt > new Date()) {
      throw new ValidationError("createdAt", "Created date cannot be in the future");
    }
  }

  // Firestore Data Conversion
  toData(): ProjectWrapperData {
    return {
      id: this._id, // projectId와 동일
      projectName: this._projectName,
      projectImageUrl: this._projectImageUrl,
      status: this._status,
      joinedAt: this._joinedAt,
      lastUpdatedAt: this._lastUpdatedAt,
      createdAt: this._createdAt,
      updatedAt: this._updatedAt,
    };
  }

  static fromData(data: ProjectWrapperData): ProjectWrapperEntity {
    return new ProjectWrapperEntity(
      data.id, // projectId
      data.projectName,
      data.projectImageUrl,
      data.status,
      data.joinedAt,
      data.lastUpdatedAt,
      data.createdAt,
      data.updatedAt
    );
  }

  // Factory Methods
  static create(
    projectId: string,
    projectName: string,
    projectImageUrl?: string
  ): ProjectWrapperEntity {
    const now = new Date();
    return new ProjectWrapperEntity(
      projectId, // id가 곧 projectId
      projectName,
      projectImageUrl,
      ProjectWrapperStatus.ACTIVE,
      now, // joinedAt
      now, // lastUpdatedAt
      now, // createdAt
      now // updatedAt
    );
  }

  static fromProjectInfo(
    projectId: string,
    projectName: string,
    projectImageUrl?: string,
    joinedAt?: Date
  ): ProjectWrapperEntity {
    const now = new Date();
    return new ProjectWrapperEntity(
      projectId, // id가 곧 projectId
      projectName,
      projectImageUrl,
      ProjectWrapperStatus.ACTIVE,
      joinedAt || now,
      now, // lastUpdatedAt
      now, // createdAt
      now // updatedAt
    );
  }
}
