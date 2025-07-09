import {Firestore} from "firebase-admin/firestore";
import {ProjectInvitationEntity, ProjectInvitationStatus} from "../../../domain/projectInvitation/entities/projectInvitation.entity";
import {ProjectInvitationDataSource} from "../interfaces/projectInvitation.datasource";

/**
 * 프로젝트 초대 Firestore 데이터소스 구현체
 */
export class ProjectInvitationFirestoreDataSource implements ProjectInvitationDataSource {
  private readonly collectionName = "project_invitations";

  constructor(private readonly firestore: Firestore) {}

  /**
   * 프로젝트 초대를 생성합니다.
   */
  async create(invitation: ProjectInvitationEntity): Promise<ProjectInvitationEntity> {
    const docRef = this.firestore.collection(this.collectionName).doc();
    const now = new Date();
    
    const data = {
      id: docRef.id,
      inviterId: invitation.inviterId,
      projectId: invitation.projectId,
      inviteeId: invitation.inviteeId,
      message: invitation.message,
      status: invitation.status,
      createdAt: now,
      updatedAt: now,
      expiresAt: invitation.expiresAt,
    };

    await docRef.set(data);
    return ProjectInvitationEntity.fromData(data);
  }

  /**
   * 프로젝트 초대를 업데이트합니다.
   */
  async update(invitation: ProjectInvitationEntity): Promise<ProjectInvitationEntity> {
    const docRef = this.firestore.collection(this.collectionName).doc(invitation.id);
    const now = new Date();
    
    const data = {
      id: invitation.id,
      inviterId: invitation.inviterId,
      projectId: invitation.projectId,
      inviteeId: invitation.inviteeId,
      message: invitation.message,
      status: invitation.status,
      createdAt: invitation.createdAt,
      updatedAt: now,
      expiresAt: invitation.expiresAt,
    };

    await docRef.update(data);
    return ProjectInvitationEntity.fromData(data);
  }

  /**
   * 초대 ID로 프로젝트 초대를 조회합니다.
   */
  async findById(invitationId: string): Promise<ProjectInvitationEntity | null> {
    const docRef = this.firestore.collection(this.collectionName).doc(invitationId);
    const doc = await docRef.get();
    
    if (!doc.exists) {
      return null;
    }
    
    return ProjectInvitationEntity.fromData(doc.data()!);
  }

  /**
   * 사용자가 받은 초대 목록을 조회합니다.
   */
  async findByInviteeId(inviteeId: string, status?: ProjectInvitationStatus): Promise<ProjectInvitationEntity[]> {
    let query = this.firestore.collection(this.collectionName).where("inviteeId", "==", inviteeId);
    
    if (status) {
      query = query.where("status", "==", status);
    }
    
    const snapshot = await query.get();
    return snapshot.docs.map(doc => ProjectInvitationEntity.fromData(doc.data()));
  }

  /**
   * 사용자가 보낸 초대 목록을 조회합니다.
   */
  async findByInviterId(inviterId: string, projectId?: string, status?: ProjectInvitationStatus): Promise<ProjectInvitationEntity[]> {
    let query = this.firestore.collection(this.collectionName).where("inviterId", "==", inviterId);
    
    if (projectId) {
      query = query.where("projectId", "==", projectId);
    }
    
    if (status) {
      query = query.where("status", "==", status);
    }
    
    const snapshot = await query.get();
    return snapshot.docs.map(doc => ProjectInvitationEntity.fromData(doc.data()));
  }

  /**
   * 특정 프로젝트의 초대 목록을 조회합니다.
   */
  async findByProjectId(projectId: string, status?: ProjectInvitationStatus): Promise<ProjectInvitationEntity[]> {
    let query = this.firestore.collection(this.collectionName).where("projectId", "==", projectId);
    
    if (status) {
      query = query.where("status", "==", status);
    }
    
    const snapshot = await query.get();
    return snapshot.docs.map(doc => ProjectInvitationEntity.fromData(doc.data()));
  }

  /**
   * 중복 초대 확인 (같은 프로젝트에 같은 사용자가 이미 초대받았는지)
   */
  async hasPendingInvitation(projectId: string, inviteeId: string): Promise<boolean> {
    const snapshot = await this.firestore
      .collection(this.collectionName)
      .where("projectId", "==", projectId)
      .where("inviteeId", "==", inviteeId)
      .where("status", "==", ProjectInvitationStatus.PENDING)
      .limit(1)
      .get();
    
    return !snapshot.empty;
  }

  /**
   * 프로젝트 초대를 삭제합니다.
   */
  async delete(invitationId: string): Promise<void> {
    const docRef = this.firestore.collection(this.collectionName).doc(invitationId);
    await docRef.delete();
  }
}