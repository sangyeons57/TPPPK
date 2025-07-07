import { firestore } from 'firebase-admin';
import { MemberDataSource } from '../interfaces/member.datasource';
import { MemberData, MemberEntity, MemberStatus } from '../../../domain/member/entities/member.entity';
import { CustomResult, Result } from '../../../core/types';

export class FirestoreMemberDataSource implements MemberDataSource {
  private readonly db: firestore.Firestore;

  constructor(db: firestore.Firestore) {
    this.db = db;
  }

  private getCollectionPath(projectId: string): string {
    return `projects/${projectId}/${MemberEntity.COLLECTION_NAME}`;
  }

  private getMemberDocPath(projectId: string, memberId: string): string {
    return `${this.getCollectionPath(projectId)}/${memberId}`;
  }

  private convertToMemberData(doc: firestore.DocumentSnapshot): MemberData | null {
    if (!doc.exists) {
      return null;
    }

    const data = doc.data();
    if (!data) {
      return null;
    }

    return {
      id: doc.id,
      projectId: data[MemberEntity.KEY_PROJECT_ID] || '',
      userId: data[MemberEntity.KEY_USER_ID] || '',
      roleIds: data[MemberEntity.KEY_ROLE_IDS] || [],
      status: data[MemberEntity.KEY_STATUS] || MemberStatus.ACTIVE,
      createdAt: data[MemberEntity.KEY_CREATED_AT]?.toDate() || new Date(),
      updatedAt: data[MemberEntity.KEY_UPDATED_AT]?.toDate() || new Date()
    };
  }

  private convertToFirestoreData(member: MemberData): Record<string, any> {
    return {
      [MemberEntity.KEY_PROJECT_ID]: member.projectId,
      [MemberEntity.KEY_USER_ID]: member.userId,
      [MemberEntity.KEY_ROLE_IDS]: member.roleIds,
      [MemberEntity.KEY_STATUS]: member.status,
      [MemberEntity.KEY_CREATED_AT]: firestore.Timestamp.fromDate(member.createdAt),
      [MemberEntity.KEY_UPDATED_AT]: firestore.Timestamp.fromDate(member.updatedAt)
    };
  }

  async findById(projectId: string, memberId: string): Promise<CustomResult<MemberData>> {
    try {
      const docPath = this.getMemberDocPath(projectId, memberId);
      const doc = await this.db.doc(docPath).get();
      
      const memberData = this.convertToMemberData(doc);
      if (!memberData) {
        return Result.failure(new Error(`Member not found: ${memberId}`));
      }

      return Result.success(memberData);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to find member by ID'));
    }
  }

  async findByUserId(projectId: string, userId: string): Promise<CustomResult<MemberData>> {
    try {
      const collectionPath = this.getCollectionPath(projectId);
      const query = this.db.collection(collectionPath)
        .where(MemberEntity.KEY_USER_ID, '==', userId)
        .limit(1);
      
      const snapshot = await query.get();
      
      if (snapshot.empty) {
        return Result.failure(new Error(`Member not found for user: ${userId}`));
      }

      const memberData = this.convertToMemberData(snapshot.docs[0]);
      if (!memberData) {
        return Result.failure(new Error(`Invalid member data for user: ${userId}`));
      }

      return Result.success(memberData);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to find member by user ID'));
    }
  }

  async findAll(projectId: string): Promise<CustomResult<MemberData[]>> {
    try {
      const collectionPath = this.getCollectionPath(projectId);
      const snapshot = await this.db.collection(collectionPath).get();
      
      const members: MemberData[] = [];
      snapshot.forEach(doc => {
        const memberData = this.convertToMemberData(doc);
        if (memberData) {
          members.push(memberData);
        }
      });

      return Result.success(members);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to find all members'));
    }
  }

  async findAllActive(projectId: string): Promise<CustomResult<MemberData[]>> {
    try {
      const collectionPath = this.getCollectionPath(projectId);
      const query = this.db.collection(collectionPath)
        .where(MemberEntity.KEY_STATUS, '==', MemberStatus.ACTIVE);
      
      const snapshot = await query.get();
      
      const members: MemberData[] = [];
      snapshot.forEach(doc => {
        const memberData = this.convertToMemberData(doc);
        if (memberData) {
          members.push(memberData);
        }
      });

      return Result.success(members);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to find active members'));
    }
  }

  async save(projectId: string, member: MemberData): Promise<CustomResult<MemberData>> {
    try {
      const docPath = this.getMemberDocPath(projectId, member.id);
      const firestoreData = this.convertToFirestoreData(member);
      
      await this.db.doc(docPath).set(firestoreData, { merge: true });
      
      return Result.success(member);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to save member'));
    }
  }

  async delete(projectId: string, memberId: string): Promise<CustomResult<void>> {
    try {
      const docPath = this.getMemberDocPath(projectId, memberId);
      await this.db.doc(docPath).delete();
      
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to delete member'));
    }
  }

  async deleteByUserId(projectId: string, userId: string): Promise<CustomResult<void>> {
    try {
      const collectionPath = this.getCollectionPath(projectId);
      const query = this.db.collection(collectionPath)
        .where(MemberEntity.KEY_USER_ID, '==', userId);
      
      const snapshot = await query.get();
      
      if (snapshot.empty) {
        return Result.failure(new Error(`Member not found for user: ${userId}`));
      }

      const batch = this.db.batch();
      snapshot.docs.forEach(doc => {
        batch.delete(doc.ref);
      });
      
      await batch.commit();
      
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to delete member by user ID'));
    }
  }

  async deleteAll(projectId: string): Promise<CustomResult<void>> {
    try {
      const collectionPath = this.getCollectionPath(projectId);
      const snapshot = await this.db.collection(collectionPath).get();
      
      if (snapshot.empty) {
        return Result.success(undefined);
      }

      const batch = this.db.batch();
      snapshot.docs.forEach(doc => {
        batch.delete(doc.ref);
      });
      
      await batch.commit();
      
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to delete all members'));
    }
  }

  async exists(projectId: string, userId: string): Promise<CustomResult<boolean>> {
    try {
      const collectionPath = this.getCollectionPath(projectId);
      const query = this.db.collection(collectionPath)
        .where(MemberEntity.KEY_USER_ID, '==', userId)
        .limit(1);
      
      const snapshot = await query.get();
      
      return Result.success(!snapshot.empty);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to check member existence'));
    }
  }

  async count(projectId: string): Promise<CustomResult<number>> {
    try {
      const collectionPath = this.getCollectionPath(projectId);
      const snapshot = await this.db.collection(collectionPath).get();
      
      return Result.success(snapshot.size);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to count members'));
    }
  }

  async countActive(projectId: string): Promise<CustomResult<number>> {
    try {
      const collectionPath = this.getCollectionPath(projectId);
      const query = this.db.collection(collectionPath)
        .where(MemberEntity.KEY_STATUS, '==', MemberStatus.ACTIVE);
      
      const snapshot = await query.get();
      
      return Result.success(snapshot.size);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to count active members'));
    }
  }
}