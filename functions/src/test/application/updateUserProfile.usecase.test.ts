import { UpdateUserProfileUseCase } from '../../application/user/updateUserProfile.usecase';
import { UserProfileRepository } from '../../domain/user/userProfile.repository';
import { UserProfileEntity, Email, Username } from '../../domain/user/user.entity';
import { Result } from '../../core/types';
import { NotFoundError } from '../../core/errors';

describe('UpdateUserProfileUseCase', () => {
  let useCase: UpdateUserProfileUseCase;
  let mockRepository: jest.Mocked<UserProfileRepository>;

  beforeEach(() => {
    mockRepository = {
      findById: jest.fn(),
      findByUserId: jest.fn(),
      findByEmail: jest.fn(),
      findByUsername: jest.fn(),
      save: jest.fn(),
      update: jest.fn(),
      delete: jest.fn(),
      exists: jest.fn(),
      findActiveProfiles: jest.fn(),
    };

    useCase = new UpdateUserProfileUseCase(mockRepository);
  });

  describe('execute', () => {
    it('should update user profile successfully', async () => {
      const existingProfile = new UserProfileEntity(
        'profile1',
        'user1',
        new Username('olduser'),
        new Email('test@example.com'),
        new Date(),
        new Date(),
        true
      );

      mockRepository.findByUserId.mockResolvedValue(Result.success(existingProfile));
      mockRepository.findByUsername.mockResolvedValue(Result.success(null));
      mockRepository.update.mockResolvedValue(Result.success(existingProfile));

      const request = {
        userId: 'user1',
        username: 'newuser',
        bio: 'Updated bio'
      };

      const result = await useCase.execute(request);

      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.userProfile).toBeDefined();
      }
      expect(mockRepository.findByUserId).toHaveBeenCalledWith('user1');
      expect(mockRepository.update).toHaveBeenCalled();
    });

    it('should return NotFoundError when user profile does not exist', async () => {
      mockRepository.findByUserId.mockResolvedValue(Result.success(null));

      const request = {
        userId: 'nonexistent',
        username: 'newuser'
      };

      const result = await useCase.execute(request);

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBeInstanceOf(NotFoundError);
      }
    });

    it('should validate username uniqueness', async () => {
      const existingProfile = new UserProfileEntity(
        'profile1',
        'user1',
        new Username('olduser'),
        new Email('test@example.com'),
        new Date(),
        new Date(),
        true
      );

      const conflictingProfile = new UserProfileEntity(
        'profile2',
        'user2',
        new Username('newuser'),
        new Email('other@example.com'),
        new Date(),
        new Date(),
        true
      );

      mockRepository.findByUserId.mockResolvedValue(Result.success(existingProfile));
      mockRepository.findByUsername.mockResolvedValue(Result.success(conflictingProfile));

      const request = {
        userId: 'user1',
        username: 'newuser'
      };

      const result = await useCase.execute(request);

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.message).toContain('Username already exists');
      }
    });
  });
});