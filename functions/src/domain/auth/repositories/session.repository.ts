import { SessionEntity, SessionToken, RefreshToken, SessionStatus } from '../entities/session.entity';
import { CustomResult } from '../../../core/types';

export interface SessionRepository {
  findById(id: string): Promise<CustomResult<SessionEntity | null>>;
  findByToken(token: SessionToken): Promise<CustomResult<SessionEntity | null>>;
  findByRefreshToken(refreshToken: RefreshToken): Promise<CustomResult<SessionEntity | null>>;
  findByUserId(userId: string): Promise<CustomResult<SessionEntity[]>>;
  findActiveSessionsByUserId(userId: string): Promise<CustomResult<SessionEntity[]>>;
  findByStatus(status: SessionStatus): Promise<CustomResult<SessionEntity[]>>;
  save(session: SessionEntity): Promise<CustomResult<SessionEntity>>;
  update(session: SessionEntity): Promise<CustomResult<SessionEntity>>;
  delete(id: string): Promise<CustomResult<void>>;
  deleteByUserId(userId: string): Promise<CustomResult<void>>;
  exists(id: string): Promise<CustomResult<boolean>>;
  revokeAllUserSessions(userId: string): Promise<CustomResult<void>>;
  cleanupExpiredSessions(): Promise<CustomResult<number>>;
}