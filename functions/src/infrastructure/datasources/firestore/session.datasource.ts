import { SessionRepository } from '../../domain/auth/session.repository';
import { SessionEntity, SessionToken, RefreshToken, SessionStatus } from '../../domain/auth/session.entity';
import { CustomResult, Result } from '../../core/types';
import { NotFoundError, InternalError } from '../../core/errors';
import { FIRESTORE_COLLECTIONS } from '../../core/constants';
import { getFirestore, FieldValue } from 'firebase-admin/firestore';

interface SessionData {
  id: string;
  userId: string;
  token: string;
  refreshToken: string;
  expiresAt: FirebaseFirestore.Timestamp;
  status: SessionStatus;
  deviceInfo?: string;
  ipAddress?: string;
  lastAccessedAt: FirebaseFirestore.Timestamp;
  createdAt: FirebaseFirestore.Timestamp;
  updatedAt: FirebaseFirestore.Timestamp;
}

export class FirestoreSessionDataSource implements SessionRepository {
  private readonly db = getFirestore();
  private readonly collection = this.db.collection(FIRESTORE_COLLECTIONS.SESSIONS);

  async findById(id: string): Promise<CustomResult<SessionEntity | null>> {
    try {
      const doc = await this.collection.doc(id).get();
      if (!doc.exists) {
        return Result.success(null);
      }
      
      const data = doc.data() as SessionData;
      return Result.success(this.mapToEntity(data));
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find session by id: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async findByToken(token: SessionToken): Promise<CustomResult<SessionEntity | null>> {
    try {
      const query = await this.collection.where('token', '==', token.value).limit(1).get();
      if (query.empty) {
        return Result.success(null);
      }
      
      const data = query.docs[0].data() as SessionData;
      return Result.success(this.mapToEntity(data));
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find session by token: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async findByRefreshToken(refreshToken: RefreshToken): Promise<CustomResult<SessionEntity | null>> {
    try {
      const query = await this.collection.where('refreshToken', '==', refreshToken.value).limit(1).get();
      if (query.empty) {
        return Result.success(null);
      }
      
      const data = query.docs[0].data() as SessionData;
      return Result.success(this.mapToEntity(data));
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find session by refresh token: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async findByUserId(userId: string): Promise<CustomResult<SessionEntity[]>> {
    try {
      const query = await this.collection
        .where('userId', '==', userId)
        .orderBy('createdAt', 'desc')
        .get();
      
      const sessions = query.docs.map(doc => this.mapToEntity(doc.data() as SessionData));
      return Result.success(sessions);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find sessions by user: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async findActiveSessionsByUserId(userId: string): Promise<CustomResult<SessionEntity[]>> {
    try {
      const query = await this.collection
        .where('userId', '==', userId)
        .where('status', '==', SessionStatus.ACTIVE)
        .where('expiresAt', '>', new Date())
        .orderBy('expiresAt', 'desc')
        .get();
      
      const sessions = query.docs.map(doc => this.mapToEntity(doc.data() as SessionData));
      return Result.success(sessions);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find active sessions by user: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async findByStatus(status: SessionStatus): Promise<CustomResult<SessionEntity[]>> {
    try {
      const query = await this.collection
        .where('status', '==', status)
        .orderBy('createdAt', 'desc')
        .get();
      
      const sessions = query.docs.map(doc => this.mapToEntity(doc.data() as SessionData));
      return Result.success(sessions);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find sessions by status: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async save(session: SessionEntity): Promise<CustomResult<SessionEntity>> {
    try {
      const data = this.mapToData(session);
      await this.collection.doc(session.id).set(data);
      return Result.success(session);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to save session: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async update(session: SessionEntity): Promise<CustomResult<SessionEntity>> {
    try {
      const data = this.mapToData(session);
      data.updatedAt = FieldValue.serverTimestamp() as any;
      await this.collection.doc(session.id).update(data);
      return Result.success(session);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to update session: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async delete(id: string): Promise<CustomResult<void>> {
    try {
      await this.collection.doc(id).delete();
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to delete session: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async deleteByUserId(userId: string): Promise<CustomResult<void>> {
    try {
      const query = await this.collection.where('userId', '==', userId).get();
      const batch = this.db.batch();
      
      query.docs.forEach(doc => {
        batch.delete(doc.ref);
      });
      
      await batch.commit();
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to delete sessions by user: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async exists(id: string): Promise<CustomResult<boolean>> {
    try {
      const doc = await this.collection.doc(id).get();
      return Result.success(doc.exists);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to check session existence: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async revokeAllUserSessions(userId: string): Promise<CustomResult<void>> {
    try {
      const query = await this.collection.where('userId', '==', userId).get();
      const batch = this.db.batch();
      
      query.docs.forEach(doc => {
        batch.update(doc.ref, {
          status: SessionStatus.REVOKED,
          updatedAt: FieldValue.serverTimestamp()
        });
      });
      
      await batch.commit();
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to revoke all user sessions: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  async cleanupExpiredSessions(): Promise<CustomResult<number>> {
    try {
      const query = await this.collection
        .where('expiresAt', '<=', new Date())
        .limit(100)
        .get();
      
      if (query.empty) {
        return Result.success(0);
      }
      
      const batch = this.db.batch();
      query.docs.forEach(doc => {
        batch.update(doc.ref, {
          status: SessionStatus.EXPIRED,
          updatedAt: FieldValue.serverTimestamp()
        });
      });
      
      await batch.commit();
      return Result.success(query.size);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to cleanup expired sessions: ${error instanceof Error ? error.message : 'Unknown error'}`));
    }
  }

  private mapToEntity(data: SessionData): SessionEntity {
    return new SessionEntity(
      data.id,
      data.userId,
      new SessionToken(data.token),
      new RefreshToken(data.refreshToken),
      data.expiresAt.toDate(),
      data.createdAt.toDate(),
      data.updatedAt.toDate(),
      data.status,
      data.lastAccessedAt.toDate(),
      data.deviceInfo,
      data.ipAddress
    );
  }

  private mapToData(entity: SessionEntity): SessionData {
    return {
      id: entity.id,
      userId: entity.userId,
      token: entity.token.value,
      refreshToken: entity.refreshToken.value,
      expiresAt: entity.expiresAt as any,
      status: entity.status,
      deviceInfo: entity.deviceInfo,
      ipAddress: entity.ipAddress,
      lastAccessedAt: entity.lastAccessedAt as any,
      createdAt: entity.createdAt as any,
      updatedAt: entity.updatedAt as any,
    };
  }
}