import { SendFriendRequestUseCase, SendFriendRequestRequest } from '../../../application/friend/sendFriendRequest.usecase';
import { FriendRepository } from '../../../domain/friend/friend.repository';
import { UserProfileRepository } from '../../../domain/user/userProfile.repository';
import { FriendEntity, FriendStatus, UserId } from '../../../domain/friend/friend.entity';
import { ValidationError, ConflictError, NotFoundError } from '../../../core/errors';
import { TestFactories, TestUtils, createMockFriendRepository, createMockUserProfileRepository } from '../../helpers';

describe('SendFriendRequestUseCase', () => {
  let useCase: SendFriendRequestUseCase;
  let mockFriendRepository: jest.Mocked<FriendRepository>;
  let mockUserRepository: jest.Mocked<UserProfileRepository>;

  beforeEach(() => {
    mockFriendRepository = createMockFriendRepository();
    mockUserRepository = createMockUserProfileRepository();
    useCase = new SendFriendRequestUseCase(mockFriendRepository, mockUserRepository);
  });

  describe('execute', () => {
    const validRequest: SendFriendRequestRequest = {
      requesterId: 'user_123',
      receiverUserId: 'user_456'
    };

    it('should send friend request successfully', async () => {
      const requester = TestFactories.createUserProfile({ userId: 'user_123' });
      const receiver = TestFactories.createUserProfile({ 
        userId: 'user_456',
        canReceiveFriendRequests: true 
      });
      const createdFriend = TestFactories.createFriend({
        userId: 'user_123',
        friendUserId: 'user_456',
        status: FriendStatus.REQUESTED,
      });

      mockUserRepository.findByUserId
        .mockResolvedValueOnce(TestUtils.createSuccessResult(requester))
        .mockResolvedValueOnce(TestUtils.createSuccessResult(receiver));
      mockFriendRepository.areUsersFriends.mockResolvedValue(TestUtils.createSuccessResult(false));
      mockFriendRepository.friendRequestExists
        .mockResolvedValueOnce(TestUtils.createSuccessResult(false))
        .mockResolvedValueOnce(TestUtils.createSuccessResult(false));
      mockFriendRepository.save.mockResolvedValue(TestUtils.createSuccessResult(createdFriend));

      const result = await useCase.execute(validRequest);

      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.friendRequestId).toBe(createdFriend.id.value);
        expect(result.data.status).toBe(FriendStatus.REQUESTED);
        expect(result.data.requestedAt).toBe(createdFriend.requestedAt.toISOString());
      }

      expect(mockUserRepository.findByUserId).toHaveBeenCalledTimes(2);
      expect(mockFriendRepository.areUsersFriends).toHaveBeenCalledWith(
        new UserId('user_123'),
        new UserId('user_456')
      );
      expect(mockFriendRepository.save).toHaveBeenCalled();
    });

    it('should fail when requester ID is missing', async () => {
      const invalidRequest: SendFriendRequestRequest = {
        requesterId: '',
        receiverUserId: 'user_456'
      };

      const result = await useCase.execute(invalidRequest);

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBeInstanceOf(ValidationError);
        expect(result.error.message).toContain('Both requester and receiver IDs are required');
      }
    });

    it('should fail when receiver ID is missing', async () => {
      const invalidRequest: SendFriendRequestRequest = {
        requesterId: 'user_123',
        receiverUserId: ''
      };

      const result = await useCase.execute(invalidRequest);

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBeInstanceOf(ValidationError);
        expect(result.error.message).toContain('Both requester and receiver IDs are required');
      }
    });

    it('should fail when trying to send request to self', async () => {
      const selfRequest: SendFriendRequestRequest = {
        requesterId: 'user_123',
        receiverUserId: 'user_123'
      };

      const result = await useCase.execute(selfRequest);

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBeInstanceOf(ValidationError);
        expect(result.error.message).toContain('Cannot send friend request to yourself');
      }
    });

    it('should fail when requester not found', async () => {
      mockUserRepository.findByUserId.mockResolvedValueOnce(TestUtils.createSuccessResult(null));

      const result = await useCase.execute(validRequest);

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBeInstanceOf(NotFoundError);
        expect(result.error.message).toContain('Requester not found');
      }
    });

    it('should fail when receiver not found', async () => {
      const requester = TestFactories.createUserProfile({ userId: 'user_123' });

      mockUserRepository.findByUserId
        .mockResolvedValueOnce(TestUtils.createSuccessResult(requester))
        .mockResolvedValueOnce(TestUtils.createSuccessResult(null));

      const result = await useCase.execute(validRequest);

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBeInstanceOf(NotFoundError);
        expect(result.error.message).toContain('Receiver not found');
      }
    });

    it('should fail when receiver cannot receive requests', async () => {
      const requester = TestFactories.createUserProfile({ userId: 'user_123' });
      const receiver = TestFactories.createUserProfile({ 
        userId: 'user_456',
        canReceiveFriendRequests: false 
      });

      mockUserRepository.findByUserId
        .mockResolvedValueOnce(TestUtils.createSuccessResult(requester))
        .mockResolvedValueOnce(TestUtils.createSuccessResult(receiver));

      const result = await useCase.execute(validRequest);

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBeInstanceOf(ConflictError);
        expect(result.error.message).toContain('User is not accepting friend requests');
      }
    });

    it('should fail when users are already friends', async () => {
      const requester = TestFactories.createUserProfile({ userId: 'user_123' });
      const receiver = TestFactories.createUserProfile({ userId: 'user_456' });

      mockUserRepository.findByUserId
        .mockResolvedValueOnce(TestUtils.createSuccessResult(requester))
        .mockResolvedValueOnce(TestUtils.createSuccessResult(receiver));
      mockFriendRepository.areUsersFriends.mockResolvedValue(TestUtils.createSuccessResult(true));

      const result = await useCase.execute(validRequest);

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBeInstanceOf(ConflictError);
        expect(result.error.message).toContain('Users are already friends');
      }
    });

    it('should fail when friend request already exists', async () => {
      const requester = TestFactories.createUserProfile({ userId: 'user_123' });
      const receiver = TestFactories.createUserProfile({ userId: 'user_456' });

      mockUserRepository.findByUserId
        .mockResolvedValueOnce(TestUtils.createSuccessResult(requester))
        .mockResolvedValueOnce(TestUtils.createSuccessResult(receiver));
      mockFriendRepository.areUsersFriends.mockResolvedValue(TestUtils.createSuccessResult(false));
      mockFriendRepository.friendRequestExists.mockResolvedValueOnce(TestUtils.createSuccessResult(true));

      const result = await useCase.execute(validRequest);

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBeInstanceOf(ConflictError);
        expect(result.error.message).toContain('Friend request already exists');
      }
    });

    it('should fail when reverse friend request already exists', async () => {
      const requester = TestFactories.createUserProfile({ userId: 'user_123' });
      const receiver = TestFactories.createUserProfile({ userId: 'user_456' });

      mockUserRepository.findByUserId
        .mockResolvedValueOnce(TestUtils.createSuccessResult(requester))
        .mockResolvedValueOnce(TestUtils.createSuccessResult(receiver));
      mockFriendRepository.areUsersFriends.mockResolvedValue(TestUtils.createSuccessResult(false));
      mockFriendRepository.friendRequestExists
        .mockResolvedValueOnce(TestUtils.createSuccessResult(false))
        .mockResolvedValueOnce(TestUtils.createSuccessResult(true));

      const result = await useCase.execute(validRequest);

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBeInstanceOf(ConflictError);
        expect(result.error.message).toContain('Friend request already exists from the other user');
      }
    });

    it('should handle repository errors gracefully', async () => {
      const requester = TestFactories.createUserProfile({ userId: 'user_123' });
      const receiver = TestFactories.createUserProfile({ userId: 'user_456' });
      const repositoryError = new Error('Database connection failed');

      mockUserRepository.findByUserId
        .mockResolvedValueOnce(TestUtils.createSuccessResult(requester))
        .mockResolvedValueOnce(TestUtils.createSuccessResult(receiver));
      mockFriendRepository.areUsersFriends.mockResolvedValue(TestUtils.createFailureResult(repositoryError));

      const result = await useCase.execute(validRequest);

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(repositoryError);
      }
    });

    it('should handle save failure', async () => {
      const requester = TestFactories.createUserProfile({ userId: 'user_123' });
      const receiver = TestFactories.createUserProfile({ userId: 'user_456' });
      const saveError = new Error('Save failed');

      mockUserRepository.findByUserId
        .mockResolvedValueOnce(TestUtils.createSuccessResult(requester))
        .mockResolvedValueOnce(TestUtils.createSuccessResult(receiver));
      mockFriendRepository.areUsersFriends.mockResolvedValue(TestUtils.createSuccessResult(false));
      mockFriendRepository.friendRequestExists
        .mockResolvedValueOnce(TestUtils.createSuccessResult(false))
        .mockResolvedValueOnce(TestUtils.createSuccessResult(false));
      mockFriendRepository.save.mockResolvedValue(TestUtils.createFailureResult(saveError));

      const result = await useCase.execute(validRequest);

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(saveError);
      }
    });

    it('should handle unexpected errors', async () => {
      mockUserRepository.findByUserId.mockRejectedValue(new Error('Unexpected error'));

      const result = await useCase.execute(validRequest);

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.message).toContain('Failed to send friend request');
      }
    });
  });
});