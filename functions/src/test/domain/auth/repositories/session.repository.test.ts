import { SessionRepository } from '../../../domain/auth/session.repository';
import { SessionEntity, SessionToken, RefreshToken, SessionStatus } from '../../../domain/auth/session.entity';
import { TestFactories, TestUtils } from '../../helpers';

describe('SessionRepository Contract Tests', () => {
  let repository: SessionRepository;
  let testSession: SessionEntity;
  let testToken: SessionToken;
  let testRefreshToken: RefreshToken;

  beforeEach(() => {
    testToken = new SessionToken('session_token_1234567890');
    testRefreshToken = new RefreshToken('refresh_token_1234567890');
    
    testSession = TestFactories.createSession({
      id: 'session_123',
      userId: 'user_123',
      token: testToken.value,
      refreshToken: testRefreshToken.value,
      status: SessionStatus.ACTIVE,
    });

    // Mock repository will be provided by concrete implementation tests
    repository = {} as SessionRepository;
  });

  describe('findById', () => {
    it('should return session when found', async () => {
      const mockResult = TestUtils.createSuccessResult(testSession);
      repository.findById = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findById('session_123');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(testSession);
      }
      expect(repository.findById).toHaveBeenCalledWith('session_123');
    });

    it('should return null when session not found', async () => {
      const mockResult = TestUtils.createSuccessResult(null);
      repository.findById = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findById('nonexistent');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBeNull();
      }
    });

    it('should return error when repository fails', async () => {
      const error = new Error('Database connection failed');
      const mockResult = TestUtils.createFailureResult(error);
      repository.findById = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findById('session_123');
      
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(error);
      }
    });
  });

  describe('findByToken', () => {
    it('should return session when found by token', async () => {
      const mockResult = TestUtils.createSuccessResult(testSession);
      repository.findByToken = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByToken(testToken);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(testSession);
      }
      expect(repository.findByToken).toHaveBeenCalledWith(testToken);
    });

    it('should return null when session not found by token', async () => {
      const differentToken = new SessionToken('different_token_1234567890');
      const mockResult = TestUtils.createSuccessResult(null);
      repository.findByToken = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByToken(differentToken);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBeNull();
      }
    });
  });

  describe('findByRefreshToken', () => {
    it('should return session when found by refresh token', async () => {
      const mockResult = TestUtils.createSuccessResult(testSession);
      repository.findByRefreshToken = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByRefreshToken(testRefreshToken);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(testSession);
      }
      expect(repository.findByRefreshToken).toHaveBeenCalledWith(testRefreshToken);
    });

    it('should return null when session not found by refresh token', async () => {
      const differentRefreshToken = new RefreshToken('different_refresh_token_1234567890');
      const mockResult = TestUtils.createSuccessResult(null);
      repository.findByRefreshToken = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByRefreshToken(differentRefreshToken);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBeNull();
      }
    });
  });

  describe('findByUserId', () => {
    it('should return all sessions for user', async () => {
      const sessions = [testSession];
      const mockResult = TestUtils.createSuccessResult(sessions);
      repository.findByUserId = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByUserId('user_123');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual(sessions);
      }
      expect(repository.findByUserId).toHaveBeenCalledWith('user_123');
    });

    it('should return empty array when no sessions found for user', async () => {
      const mockResult = TestUtils.createSuccessResult([]);
      repository.findByUserId = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByUserId('user_456');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual([]);
      }
    });
  });

  describe('findActiveSessionsByUserId', () => {
    it('should return active sessions for user', async () => {
      const activeSessions = [testSession];
      const mockResult = TestUtils.createSuccessResult(activeSessions);
      repository.findActiveSessionsByUserId = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findActiveSessionsByUserId('user_123');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual(activeSessions);
      }
      expect(repository.findActiveSessionsByUserId).toHaveBeenCalledWith('user_123');
    });

    it('should return empty array when no active sessions found', async () => {
      const mockResult = TestUtils.createSuccessResult([]);
      repository.findActiveSessionsByUserId = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findActiveSessionsByUserId('user_456');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual([]);
      }
    });
  });

  describe('findByStatus', () => {
    it('should return sessions with specific status', async () => {
      const activeSessions = [testSession];
      const mockResult = TestUtils.createSuccessResult(activeSessions);
      repository.findByStatus = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByStatus(SessionStatus.ACTIVE);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual(activeSessions);
      }
      expect(repository.findByStatus).toHaveBeenCalledWith(SessionStatus.ACTIVE);
    });

    it('should return empty array when no sessions with status found', async () => {
      const mockResult = TestUtils.createSuccessResult([]);
      repository.findByStatus = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByStatus(SessionStatus.EXPIRED);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual([]);
      }
    });
  });

  describe('save', () => {
    it('should save session and return saved entity', async () => {
      const mockResult = TestUtils.createSuccessResult(testSession);
      repository.save = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.save(testSession);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(testSession);
      }
      expect(repository.save).toHaveBeenCalledWith(testSession);
    });

    it('should return error when save fails', async () => {
      const error = new Error('Save failed');
      const mockResult = TestUtils.createFailureResult(error);
      repository.save = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.save(testSession);
      
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(error);
      }
    });
  });

  describe('update', () => {
    it('should update session and return updated entity', async () => {
      const revokedSession = testSession.revoke();
      const mockResult = TestUtils.createSuccessResult(revokedSession);
      repository.update = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.update(revokedSession);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(revokedSession);
      }
      expect(repository.update).toHaveBeenCalledWith(revokedSession);
    });
  });

  describe('delete', () => {
    it('should delete session by ID', async () => {
      const mockResult = TestUtils.createSuccessResult(undefined);
      repository.delete = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.delete('session_123');
      
      expect(result.success).toBe(true);
      expect(repository.delete).toHaveBeenCalledWith('session_123');
    });

    it('should return error when delete fails', async () => {
      const error = new Error('Delete failed');
      const mockResult = TestUtils.createFailureResult(error);
      repository.delete = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.delete('session_123');
      
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(error);
      }
    });
  });

  describe('deleteByUserId', () => {
    it('should delete all sessions for user', async () => {
      const mockResult = TestUtils.createSuccessResult(undefined);
      repository.deleteByUserId = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.deleteByUserId('user_123');
      
      expect(result.success).toBe(true);
      expect(repository.deleteByUserId).toHaveBeenCalledWith('user_123');
    });
  });

  describe('exists', () => {
    it('should return true when session exists', async () => {
      const mockResult = TestUtils.createSuccessResult(true);
      repository.exists = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.exists('session_123');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(true);
      }
      expect(repository.exists).toHaveBeenCalledWith('session_123');
    });

    it('should return false when session does not exist', async () => {
      const mockResult = TestUtils.createSuccessResult(false);
      repository.exists = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.exists('nonexistent');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(false);
      }
    });
  });

  describe('revokeAllUserSessions', () => {
    it('should revoke all sessions for user', async () => {
      const mockResult = TestUtils.createSuccessResult(undefined);
      repository.revokeAllUserSessions = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.revokeAllUserSessions('user_123');
      
      expect(result.success).toBe(true);
      expect(repository.revokeAllUserSessions).toHaveBeenCalledWith('user_123');
    });
  });

  describe('cleanupExpiredSessions', () => {
    it('should cleanup expired sessions and return count', async () => {
      const mockResult = TestUtils.createSuccessResult(5);
      repository.cleanupExpiredSessions = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.cleanupExpiredSessions();
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(5);
      }
      expect(repository.cleanupExpiredSessions).toHaveBeenCalled();
    });

    it('should return zero when no expired sessions found', async () => {
      const mockResult = TestUtils.createSuccessResult(0);
      repository.cleanupExpiredSessions = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.cleanupExpiredSessions();
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(0);
      }
    });
  });
});