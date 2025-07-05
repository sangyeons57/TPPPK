import { UserProfileRepository } from '../../../domain/user/userProfile.repository';
import { UserProfileEntity, Email, Username } from '../../../domain/user/user.entity';
import { TestFactories, TestUtils } from '../../helpers';

describe('UserProfileRepository Contract Tests', () => {
  let repository: UserProfileRepository;
  let testProfile: UserProfileEntity;
  let testEmail: Email;
  let testUsername: Username;

  beforeEach(() => {
    testEmail = new Email('test@example.com');
    testUsername = new Username('testuser');
    
    testProfile = TestFactories.createUserProfile({
      id: 'profile_123',
      userId: 'user_123',
      username: testUsername.value,
      email: testEmail.value,
    });

    // Mock repository will be provided by concrete implementation tests
    repository = {} as UserProfileRepository;
  });

  describe('findById', () => {
    it('should return user profile when found', async () => {
      const mockResult = TestUtils.createSuccessResult(testProfile);
      repository.findById = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findById('profile_123');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(testProfile);
      }
      expect(repository.findById).toHaveBeenCalledWith('profile_123');
    });

    it('should return null when profile not found', async () => {
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

      const result = await repository.findById('profile_123');
      
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(error);
      }
    });
  });

  describe('findByUserId', () => {
    it('should return user profile when found by user ID', async () => {
      const mockResult = TestUtils.createSuccessResult(testProfile);
      repository.findByUserId = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByUserId('user_123');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(testProfile);
      }
      expect(repository.findByUserId).toHaveBeenCalledWith('user_123');
    });

    it('should return null when no profile found for user ID', async () => {
      const mockResult = TestUtils.createSuccessResult(null);
      repository.findByUserId = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByUserId('nonexistent_user');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBeNull();
      }
    });
  });

  describe('findByEmail', () => {
    it('should return user profile when found by email', async () => {
      const mockResult = TestUtils.createSuccessResult(testProfile);
      repository.findByEmail = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByEmail(testEmail);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(testProfile);
      }
      expect(repository.findByEmail).toHaveBeenCalledWith(testEmail);
    });

    it('should return null when no profile found for email', async () => {
      const differentEmail = new Email('other@example.com');
      const mockResult = TestUtils.createSuccessResult(null);
      repository.findByEmail = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByEmail(differentEmail);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBeNull();
      }
    });
  });

  describe('findByUsername', () => {
    it('should return user profile when found by username', async () => {
      const mockResult = TestUtils.createSuccessResult(testProfile);
      repository.findByUsername = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByUsername(testUsername);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(testProfile);
      }
      expect(repository.findByUsername).toHaveBeenCalledWith(testUsername);
    });

    it('should return null when no profile found for username', async () => {
      const differentUsername = new Username('otheruser');
      const mockResult = TestUtils.createSuccessResult(null);
      repository.findByUsername = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByUsername(differentUsername);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBeNull();
      }
    });
  });

  describe('save', () => {
    it('should save user profile and return saved entity', async () => {
      const mockResult = TestUtils.createSuccessResult(testProfile);
      repository.save = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.save(testProfile);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(testProfile);
      }
      expect(repository.save).toHaveBeenCalledWith(testProfile);
    });

    it('should return error when save fails', async () => {
      const error = new Error('Save failed');
      const mockResult = TestUtils.createFailureResult(error);
      repository.save = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.save(testProfile);
      
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(error);
      }
    });
  });

  describe('update', () => {
    it('should update user profile and return updated entity', async () => {
      const updatedProfile = testProfile.updateProfile({
        username: new Username('updateduser'),
        bio: 'Updated bio',
      });
      const mockResult = TestUtils.createSuccessResult(updatedProfile);
      repository.update = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.update(updatedProfile);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(updatedProfile);
      }
      expect(repository.update).toHaveBeenCalledWith(updatedProfile);
    });

    it('should return error when update fails', async () => {
      const error = new Error('Update failed');
      const mockResult = TestUtils.createFailureResult(error);
      repository.update = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.update(testProfile);
      
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(error);
      }
    });
  });

  describe('delete', () => {
    it('should delete user profile by ID', async () => {
      const mockResult = TestUtils.createSuccessResult(undefined);
      repository.delete = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.delete('profile_123');
      
      expect(result.success).toBe(true);
      expect(repository.delete).toHaveBeenCalledWith('profile_123');
    });

    it('should return error when delete fails', async () => {
      const error = new Error('Delete failed');
      const mockResult = TestUtils.createFailureResult(error);
      repository.delete = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.delete('profile_123');
      
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(error);
      }
    });
  });

  describe('exists', () => {
    it('should return true when user profile exists', async () => {
      const mockResult = TestUtils.createSuccessResult(true);
      repository.exists = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.exists('user_123');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(true);
      }
      expect(repository.exists).toHaveBeenCalledWith('user_123');
    });

    it('should return false when user profile does not exist', async () => {
      const mockResult = TestUtils.createSuccessResult(false);
      repository.exists = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.exists('nonexistent_user');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(false);
      }
    });
  });

  describe('findActiveProfiles', () => {
    it('should return active profiles without limit', async () => {
      const activeProfiles = [testProfile];
      const mockResult = TestUtils.createSuccessResult(activeProfiles);
      repository.findActiveProfiles = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findActiveProfiles();
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual(activeProfiles);
      }
      expect(repository.findActiveProfiles).toHaveBeenCalledWith(undefined);
    });

    it('should return active profiles with limit', async () => {
      const activeProfiles = [testProfile];
      const mockResult = TestUtils.createSuccessResult(activeProfiles);
      repository.findActiveProfiles = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findActiveProfiles(10);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual(activeProfiles);
      }
      expect(repository.findActiveProfiles).toHaveBeenCalledWith(10);
    });

    it('should return empty array when no active profiles found', async () => {
      const mockResult = TestUtils.createSuccessResult([]);
      repository.findActiveProfiles = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findActiveProfiles();
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual([]);
      }
    });
  });
});