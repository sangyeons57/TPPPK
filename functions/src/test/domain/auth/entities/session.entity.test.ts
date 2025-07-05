import { SessionEntity, SessionToken, RefreshToken, SessionStatus } from '../../domain/auth/session.entity';
import { ValidationError } from '../../core/errors';
import { TestFactories, TestUtils } from '../helpers';

describe('Session Domain Entities', () => {
  describe('SessionToken Value Object', () => {
    it('should create valid session token', () => {
      const token = new SessionToken('session_token_1234567890');
      expect(token.value).toBe('session_token_1234567890');
    });

    it('should throw ValidationError for short session token', () => {
      expect(() => new SessionToken('short')).toThrow(ValidationError);
      expect(() => new SessionToken('short')).toThrow('Session token must be at least 10 characters');
    });

    it('should check equality correctly', () => {
      const token1 = new SessionToken('session_token_1234567890');
      const token2 = new SessionToken('session_token_1234567890');
      const token3 = new SessionToken('other_token_1234567890');

      expect(token1.equals(token2)).toBe(true);
      expect(token1.equals(token3)).toBe(false);
    });
  });

  describe('RefreshToken Value Object', () => {
    it('should create valid refresh token', () => {
      const token = new RefreshToken('refresh_token_1234567890');
      expect(token.value).toBe('refresh_token_1234567890');
    });

    it('should throw ValidationError for short refresh token', () => {
      expect(() => new RefreshToken('short')).toThrow(ValidationError);
      expect(() => new RefreshToken('short')).toThrow('Refresh token must be at least 10 characters');
    });

    it('should check equality correctly', () => {
      const token1 = new RefreshToken('refresh_token_1234567890');
      const token2 = new RefreshToken('refresh_token_1234567890');
      const token3 = new RefreshToken('other_token_1234567890');

      expect(token1.equals(token2)).toBe(true);
      expect(token1.equals(token3)).toBe(false);
    });
  });

  describe('SessionEntity', () => {
    let mockDateNow: jest.SpyInstance;

    beforeEach(() => {
      mockDateNow = TestUtils.mockDateNow(TestUtils.createMockDate('2023-01-15T12:00:00.000Z'));
    });

    afterEach(() => {
      TestUtils.restoreAllMocks();
    });

    it('should create session entity with valid data', () => {
      const session = TestFactories.createSession({
        id: 'session_123',
        userId: 'user_123',
        token: 'session_token_1234567890',
        refreshToken: 'refresh_token_1234567890',
        status: SessionStatus.ACTIVE,
      });

      expect(session.id).toBe('session_123');
      expect(session.userId).toBe('user_123');
      expect(session.token.value).toBe('session_token_1234567890');
      expect(session.refreshToken.value).toBe('refresh_token_1234567890');
      expect(session.status).toBe(SessionStatus.ACTIVE);
    });

    describe('isExpired', () => {
      it('should return false for non-expired session', () => {
        const session = TestFactories.createSession({
          expiresAt: TestUtils.createFutureDate(24),
        });

        expect(session.isExpired()).toBe(false);
      });

      it('should return true for expired session', () => {
        const session = TestFactories.createSession({
          expiresAt: TestUtils.createPastDate(1),
        });

        expect(session.isExpired()).toBe(true);
      });
    });

    describe('isActive', () => {
      it('should return true for active non-expired session', () => {
        const session = TestFactories.createSession({
          status: SessionStatus.ACTIVE,
          expiresAt: TestUtils.createFutureDate(24),
        });

        expect(session.isActive()).toBe(true);
      });

      it('should return false for revoked session', () => {
        const session = TestFactories.createSession({
          status: SessionStatus.REVOKED,
          expiresAt: TestUtils.createFutureDate(24),
        });

        expect(session.isActive()).toBe(false);
      });

      it('should return false for expired session even if status is active', () => {
        const session = TestFactories.createSession({
          status: SessionStatus.ACTIVE,
          expiresAt: TestUtils.createPastDate(1),
        });

        expect(session.isActive()).toBe(false);
      });
    });

    describe('revoke', () => {
      it('should revoke session and update timestamp', () => {
        const originalSession = TestFactories.createSession({
          status: SessionStatus.ACTIVE,
        });

        const revokedSession = originalSession.revoke();

        expect(revokedSession.status).toBe(SessionStatus.REVOKED);
        expect(revokedSession.updatedAt).toEqual(new Date('2023-01-15T12:00:00.000Z'));
        expect(revokedSession.id).toBe(originalSession.id);
        expect(revokedSession.userId).toBe(originalSession.userId);
      });
    });

    describe('expire', () => {
      it('should expire session and update timestamp', () => {
        const originalSession = TestFactories.createSession({
          status: SessionStatus.ACTIVE,
        });

        const expiredSession = originalSession.expire();

        expect(expiredSession.status).toBe(SessionStatus.EXPIRED);
        expect(expiredSession.updatedAt).toEqual(new Date('2023-01-15T12:00:00.000Z'));
        expect(expiredSession.id).toBe(originalSession.id);
        expect(expiredSession.userId).toBe(originalSession.userId);
      });
    });

    describe('updateAccess', () => {
      it('should update last accessed time and IP address', () => {
        const originalSession = TestFactories.createSession({
          ipAddress: '192.168.1.1',
        });

        const updatedSession = originalSession.updateAccess('192.168.1.2');

        expect(updatedSession.lastAccessedAt).toEqual(new Date('2023-01-15T12:00:00.000Z'));
        expect(updatedSession.updatedAt).toEqual(new Date('2023-01-15T12:00:00.000Z'));
        expect(updatedSession.ipAddress).toBe('192.168.1.2');
        expect(updatedSession.id).toBe(originalSession.id);
      });

      it('should update last accessed time without changing IP if not provided', () => {
        const originalSession = TestFactories.createSession({
          ipAddress: '192.168.1.1',
        });

        const updatedSession = originalSession.updateAccess();

        expect(updatedSession.lastAccessedAt).toEqual(new Date('2023-01-15T12:00:00.000Z'));
        expect(updatedSession.ipAddress).toBe('192.168.1.1');
      });
    });

    describe('refresh', () => {
      it('should refresh session with new tokens and expiration', () => {
        const originalSession = TestFactories.createSession({
          status: SessionStatus.ACTIVE,
        });

        const newToken = new SessionToken('new_session_token_1234567890');
        const newRefreshToken = new RefreshToken('new_refresh_token_1234567890');
        const newExpiresAt = TestUtils.createFutureDate(48);

        const refreshedSession = originalSession.refresh(newToken, newRefreshToken, newExpiresAt);

        expect(refreshedSession.token).toBe(newToken);
        expect(refreshedSession.refreshToken).toBe(newRefreshToken);
        expect(refreshedSession.expiresAt).toBe(newExpiresAt);
        expect(refreshedSession.status).toBe(SessionStatus.ACTIVE);
        expect(refreshedSession.updatedAt).toEqual(new Date('2023-01-15T12:00:00.000Z'));
        expect(refreshedSession.lastAccessedAt).toEqual(new Date('2023-01-15T12:00:00.000Z'));
        expect(refreshedSession.id).toBe(originalSession.id);
        expect(refreshedSession.userId).toBe(originalSession.userId);
      });
    });
  });
});