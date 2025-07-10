import { firestore } from 'firebase-admin';
import { InviteDataSource } from '../interfaces/invite.datasource';
import { InviteData, InviteEntity, InviteStatus } from '../../../domain/invite/entities/invite.entity';

export class FirestoreInviteDataSource implements InviteDataSource {
  private db: firestore.Firestore;
  private collection: firestore.CollectionReference<firestore.DocumentData>;

  constructor() {
    this.db = firestore();
    this.collection = this.db.collection(InviteEntity.COLLECTION_NAME);
  }

  async create(invite: InviteData): Promise<InviteData> {
    // Use the invite ID as the document ID directly
    const docRef = this.collection.doc(invite.id);
    
    await docRef.set({
      ...invite,
      createdAt: firestore.FieldValue.serverTimestamp(),
      updatedAt: firestore.FieldValue.serverTimestamp()
    });
    
    const doc = await docRef.get();
    return this.mapDocumentToData(doc);
  }

  async findByCode(inviteCode: string): Promise<InviteData | null> {
    // ✅ O(1) direct document lookup instead of O(n) collection scan
    // Since invite code IS the document ID, we can fetch directly
    const doc = await this.collection.doc(inviteCode).get();
    
    if (!doc.exists) {
      return null;
    }

    return this.mapDocumentToData(doc);
  }

  async findById(id: string): Promise<InviteData | null> {
    const doc = await this.collection.doc(id).get();
    
    if (!doc.exists) {
      return null;
    }

    return this.mapDocumentToData(doc);
  }

  async findActiveByProjectId(projectId: string): Promise<InviteData[]> {
    const querySnapshot = await this.collection
      .where(InviteEntity.KEY_PROJECT_ID, '==', projectId)
      .where(InviteEntity.KEY_STATUS, '==', InviteStatus.ACTIVE)
      .get();

    return querySnapshot.docs.map(doc => this.mapDocumentToData(doc));
  }

  async update(invite: InviteData): Promise<InviteData> {
    const docRef = this.collection.doc(invite.id);
    
    await docRef.update({
      ...invite,
      updatedAt: firestore.FieldValue.serverTimestamp()
    });
    
    const doc = await docRef.get();
    return this.mapDocumentToData(doc);
  }

  async delete(id: string): Promise<void> {
    await this.collection.doc(id).delete();
  }

  async existsByCode(inviteCode: string): Promise<boolean> {
    // ✅ O(1) direct document existence check instead of O(n) collection scan
    // Since invite code IS the document ID, we can check existence directly
    const doc = await this.collection.doc(inviteCode).get();
    return doc.exists;
  }

  private mapDocumentToData(doc: firestore.QueryDocumentSnapshot | firestore.DocumentSnapshot): InviteData {
    const data = doc.data();
    if (!data) {
      throw new Error('Document data is undefined');
    }

    return {
      id: doc.id, // This IS the invite code
      projectId: data.projectId,
      inviterId: data.inviterId,
      expiresAt: data.expiresAt?.toDate() || new Date(),
      status: data.status,
      createdAt: data.createdAt?.toDate() || new Date(),
      updatedAt: data.updatedAt?.toDate() || new Date()
    };
  }
}