/**
 * 프로젝트 초대 상태 열거형
 */
export enum ProjectInvitationStatus {
  PENDING = 'PENDING',
  ACCEPTED = 'ACCEPTED',
  REJECTED = 'REJECTED',
  EXPIRED = 'EXPIRED',
  CANCELLED = 'CANCELLED'
}

/**
 * 프로젝트 초대 엔티티
 * 
 * 사용자가 다른 사용자를 프로젝트에 초대할 때 생성되는 엔티티입니다.
 */
export class ProjectInvitationEntity {
  constructor(
    public readonly id: string,
    public readonly projectId: string,
    public readonly inviterId: string,
    public readonly inviteeId: string,
    public readonly status: ProjectInvitationStatus,
    public readonly expiresAt: Date,
    public readonly message?: string,
    public readonly respondedAt?: Date,
    public readonly createdAt: Date = new Date(),
    public readonly updatedAt: Date = new Date()
  ) {}

  /**
   * 초대가 만료되었는지 확인
   */
  isExpired(): boolean {
    return new Date() > this.expiresAt;
  }

  /**
   * 초대에 응답할 수 있는지 확인
   */
  canRespond(): boolean {
    return this.status === ProjectInvitationStatus.PENDING && !this.isExpired();
  }

  /**
   * 초대 수락
   */
  accept(): ProjectInvitationEntity {
    if (!this.canRespond()) {
      throw new Error(`초대에 응답할 수 없습니다. 현재 상태: ${this.status}`);
    }

    return new ProjectInvitationEntity(
      this.id,
      this.projectId,
      this.inviterId,
      this.inviteeId,
      ProjectInvitationStatus.ACCEPTED,
      this.expiresAt,
      this.message,
      new Date(),
      this.createdAt,
      new Date()
    );
  }

  /**
   * 초대 거절
   */
  reject(): ProjectInvitationEntity {
    if (!this.canRespond()) {
      throw new Error(`초대에 응답할 수 없습니다. 현재 상태: ${this.status}`);
    }

    return new ProjectInvitationEntity(
      this.id,
      this.projectId,
      this.inviterId,
      this.inviteeId,
      ProjectInvitationStatus.REJECTED,
      this.expiresAt,
      this.message,
      new Date(),
      this.createdAt,
      new Date()
    );
  }

  /**
   * 초대 취소 (초대한 사람이 취소)
   */
  cancel(): ProjectInvitationEntity {
    if (this.status !== ProjectInvitationStatus.PENDING) {
      throw new Error(`대기 중인 초대만 취소할 수 있습니다. 현재 상태: ${this.status}`);
    }

    return new ProjectInvitationEntity(
      this.id,
      this.projectId,
      this.inviterId,
      this.inviteeId,
      ProjectInvitationStatus.CANCELLED,
      this.expiresAt,
      this.message,
      this.respondedAt,
      this.createdAt,
      new Date()
    );
  }

  /**
   * 초대 만료 처리
   */
  expire(): ProjectInvitationEntity {
    if (this.status !== ProjectInvitationStatus.PENDING) {
      throw new Error(`대기 중인 초대만 만료시킬 수 있습니다. 현재 상태: ${this.status}`);
    }

    return new ProjectInvitationEntity(
      this.id,
      this.projectId,
      this.inviterId,
      this.inviteeId,
      ProjectInvitationStatus.EXPIRED,
      this.expiresAt,
      this.message,
      this.respondedAt,
      this.createdAt,
      new Date()
    );
  }

  /**
   * Firestore 데이터로 변환
   */
  toData(): ProjectInvitationData {
    return {
      id: this.id,
      projectId: this.projectId,
      inviterId: this.inviterId,
      inviteeId: this.inviteeId,
      status: this.status,
      expiresAt: this.expiresAt,
      message: this.message || null,
      respondedAt: this.respondedAt || null,
      createdAt: this.createdAt,
      updatedAt: this.updatedAt
    };
  }

  /**
   * 새 초대 생성
   */
  static create(
    projectId: string,
    inviterId: string,
    inviteeId: string,
    message?: string,
    expiresInHours: number = 72
  ): ProjectInvitationEntity {
    const now = new Date();
    const expiresAt = new Date(now.getTime() + expiresInHours * 60 * 60 * 1000);
    
    // 고유 ID 생성 (timestamp + random)
    const id = `inv_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

    return new ProjectInvitationEntity(
      id,
      projectId,
      inviterId,
      inviteeId,
      ProjectInvitationStatus.PENDING,
      expiresAt,
      message,
      undefined,
      now,
      now
    );
  }

  /**
   * Firestore 데이터에서 엔티티 생성
   */
  static fromData(data: ProjectInvitationData): ProjectInvitationEntity {
    return new ProjectInvitationEntity(
      data.id,
      data.projectId,
      data.inviterId,
      data.inviteeId,
      data.status as ProjectInvitationStatus,
      data.expiresAt instanceof Date ? data.expiresAt : new Date(data.expiresAt),
      data.message || undefined,
      data.respondedAt ? (data.respondedAt instanceof Date ? data.respondedAt : new Date(data.respondedAt)) : undefined,
      data.createdAt instanceof Date ? data.createdAt : new Date(data.createdAt),
      data.updatedAt instanceof Date ? data.updatedAt : new Date(data.updatedAt)
    );
  }
}

/**
 * Firestore에 저장될 데이터 인터페이스
 */
export interface ProjectInvitationData {
  id: string;
  projectId: string;
  inviterId: string;
  inviteeId: string;
  status: string;
  expiresAt: Date | any; // Firestore Timestamp
  message: string | null;
  respondedAt: Date | any | null; // Firestore Timestamp
  createdAt: Date | any; // Firestore Timestamp
  updatedAt: Date | any; // Firestore Timestamp
}