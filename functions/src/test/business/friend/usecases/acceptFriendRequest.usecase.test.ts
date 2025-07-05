import { AcceptFriendRequestUseCase, AcceptFriendRequestRequest } from '../../../business/friend/usecases/acceptFriendRequest.usecase';
import { FriendRepository } from '../../../domain/friend/friend.repository';
import { UserProfileRepository } from '../../../domain/user/userProfile.repository';
import { FriendEntity, FriendStatus, UserId, FriendId } from '../../../domain/friend/friend.entity';
import { ValidationError, ConflictError, NotFoundError } from '../../../core/errors';
import { TestFactories, TestUtils, createMockFriendRepository, createMockUserProfileRepository } from '../../helpers';

describe('AcceptFriendRequestUseCase', () => {
  let useCase: AcceptFriendRequestUseCase;
  let mockFriendRepository: jest.Mocked<FriendRepository>;
  let mockUserRepository: jest.Mocked<UserProfileRepository>;

  beforeEach(() => {
    mockFriendRepository = createMockFriendRepository();
    mockUserRepository = createMockUserProfileRepository();
    useCase = new AcceptFriendRequestUseCase(mockFriendRepository, mockUserRepository);
  });

  describe('execute', () => {
    const validRequest: AcceptFriendRequestRequest = {
      friendRequestId: 'friend_123',
      userId: 'user_456' // receiver
    };

    it('should accept friend request successfully', async () => {
      const friendRequest = TestFactories.createFriend({
        id: 'friend_123',
        userId: 'user_123', // requester
        friendUserId: 'user_456', // receiver
        status: FriendStatus.REQUESTED,
      });
      const receiver = TestFactories.createUserProfile({ userId: 'user_456' });
      const acceptedFriend = TestFactories.createFriend({
        id: 'friend_123',
        userId: 'user_123',
        friendUserId: 'user_456',
        status: FriendStatus.ACCEPTED,
        respondedAt: new Date('2023-01-15T12:00:00Z'),
      });
      const reciprocalFriend = TestFactories.createFriend({
        id: 'friend_456',
        userId: 'user_456', // receiver becomes requester in reciprocal
        friendUserId: 'user_123', // requester becomes receiver in reciprocal
        status: FriendStatus.ACCEPTED,
      });

      mockFriendRepository.findById.mockResolvedValue(TestUtils.createSuccessResult(friendRequest));
      mockUserRepository.findByUserId.mockResolvedValue(TestUtils.createSuccessResult(receiver));
      mockFriendRepository.update.mockResolvedValue(TestUtils.createSuccessResult(acceptedFriend));
      mockFriendRepository.save.mockResolvedValue(TestUtils.createSuccessResult(reciprocalFriend));
      mockFriendRepository.countFriendsByUserId.mockResolvedValue(TestUtils.createSuccessResult(1));

      const result = await useCase.execute(validRequest);

      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.friendRequestId).toBe('friend_123');
        expect(result.data.status).toBe(FriendStatus.ACCEPTED);
        expect(result.data.acceptedAt).toBe(acceptedFriend.respondedAt!.toISOString());
        expect(result.data.reciprocalFriendId).toBe('friend_456');
      }

      expect(mockFriendRepository.findById).toHaveBeenCalledWith(new FriendId('friend_123'));
      expect(mockFriendRepository.update).toHaveBeenCalledWith(acceptedFriend);
      expect(mockFriendRepository.save).toHaveBeenCalled();
    });

    it('should fail when friend request ID is missing', async () => {
      const invalidRequest: AcceptFriendRequestRequest = {
        friendRequestId: '',
        userId: 'user_456'
      };

      const result = await useCase.execute(invalidRequest);

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBeInstanceOf(ValidationError);
        expect(result.error.message).toContain('Friend request ID and user ID are required');
      }
    });

    it('should fail when user ID is missing', async () => {
      const invalidRequest: AcceptFriendRequestRequest = {
        friendRequestId: 'friend_123',
        userId: ''
      };

      const result = await useCase.execute(invalidRequest);

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBeInstanceOf(ValidationError);
        expect(result.error.message).toContain('Friend request ID and user ID are required');
      }
    });

    it('should fail when friend request not found', async () => {
      mockFriendRepository.findById.mockResolvedValue(TestUtils.createSuccessResult(null));

      const result = await useCase.execute(validRequest);

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBeInstanceOf(NotFoundError);
        expect(result.error.message).toContain('Friend request not found');
      }
    });

    it('should fail when user is not the receiver', async () => {
      const friendRequest = TestFactories.createFriend({
        id: 'friend_123',
        userId: 'user_123', // requester
        friendUserId: 'user_789', // receiver (different from request.userId)
        status: FriendStatus.REQUESTED,
      });

      mockFriendRepository.findById.mockResolvedValue(TestUtils.createSuccessResult(friendRequest));

      const result = await useCase.execute(validRequest); // user_456 trying to accept

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBeInstanceOf(ValidationError);
        expect(result.error.message).toContain('Only the request receiver can accept this friend request');
      }
    });

    it('should fail when friend request is not in REQUESTED status', async () => {
      const friendRequest = TestFactories.createFriend({
        id: 'friend_123',
        userId: 'user_123',
        friendUserId: 'user_456',
        status: FriendStatus.ACCEPTED, // already accepted
      });

      mockFriendRepository.findById.mockResolvedValue(TestUtils.createSuccessResult(friendRequest));

      const result = await useCase.execute(validRequest);

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBeInstanceOf(ConflictError);
        expect(result.error.message).toContain('Cannot accept friend request');
      }
    });

    it('should fail when user not found', async () => {
      const friendRequest = TestFactories.createFriend({
        id: 'friend_123',
        userId: 'user_123',
        friendUserId: 'user_456',
        status: FriendStatus.REQUESTED,
      });

      mockFriendRepository.findById.mockResolvedValue(TestUtils.createSuccessResult(friendRequest));
      mockUserRepository.findByUserId.mockResolvedValue(TestUtils.createSuccessResult(null));

      const result = await useCase.execute(validRequest);

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBeInstanceOf(NotFoundError);
        expect(result.error.message).toContain('User not found');
      }
    });

    it('should handle friend request save failure', async () => {
      const friendRequest = TestFactories.createFriend({
        id: 'friend_123',
        userId: 'user_123',
        friendUserId: 'user_456',
        status: FriendStatus.REQUESTED,
      });
      const receiver = TestFactories.createUserProfile({ userId: 'user_456' });
      const saveError = new Error('Database save failed');

      mockFriendRepository.findById.mockResolvedValue(TestUtils.createSuccessResult(friendRequest));
      mockUserRepository.findByUserId.mockResolvedValue(TestUtils.createSuccessResult(receiver));
      mockFriendRepository.update.mockResolvedValue(TestUtils.createFailureResult(saveError));

      const result = await useCase.execute(validRequest);

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(saveError);
      }
    });

    it('should handle reciprocal friend save failure', async () => {
      const friendRequest = TestFactories.createFriend({
        id: 'friend_123',
        userId: 'user_123',
        friendUserId: 'user_456',
        status: FriendStatus.REQUESTED,
      });
      const receiver = TestFactories.createUserProfile({ userId: 'user_456' });
      const acceptedFriend = TestFactories.createFriend({
        id: 'friend_123',
        userId: 'user_123',
        friendUserId: 'user_456',
        status: FriendStatus.ACCEPTED,
      });
      const saveError = new Error('Reciprocal save failed');

      mockFriendRepository.findById.mockResolvedValue(TestUtils.createSuccessResult(friendRequest));
      mockUserRepository.findByUserId.mockResolvedValue(TestUtils.createSuccessResult(receiver));
      mockFriendRepository.update.mockResolvedValue(TestUtils.createSuccessResult(acceptedFriend));
      mockFriendRepository.save.mockResolvedValue(TestUtils.createFailureResult(saveError));

      const result = await useCase.execute(validRequest);

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.message).toContain('Failed to save reciprocal friend relationship');
      }
    });

    it('should handle repository errors gracefully', async () => {
      const repositoryError = new Error('Database connection failed');

      mockFriendRepository.findById.mockResolvedValue(TestUtils.createFailureResult(repositoryError));

      const result = await useCase.execute(validRequest);

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(repositoryError);
      }
    });

    it('should handle unexpected errors', async () => {
      mockFriendRepository.findById.mockRejectedValue(new Error('Unexpected error'));

      const result = await useCase.execute(validRequest);

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.message).toContain('Failed to accept friend request');
      }
    });

    it('should update friend counts asynchronously', async () => {
      const friendRequest = TestFactories.createFriend({
        id: 'friend_123',
        userId: 'user_123',
        friendUserId: 'user_456',
        status: FriendStatus.REQUESTED,
      });
      const receiver = TestFactories.createUserProfile({ userId: 'user_456' });
      const requester = TestFactories.createUserProfile({ userId: 'user_123' });
      const acceptedFriend = TestFactories.createFriend({
        id: 'friend_123',
        status: FriendStatus.ACCEPTED,
        respondedAt: new Date(),
      });
      const reciprocalFriend = TestFactories.createFriend({ id: 'friend_456' });

      mockFriendRepository.findById.mockResolvedValue(TestUtils.createSuccessResult(friendRequest));
      mockUserRepository.findByUserId
        .mockResolvedValueOnce(TestUtils.createSuccessResult(receiver))
        .mockResolvedValueOnce(TestUtils.createSuccessResult(requester))
        .mockResolvedValueOnce(TestUtils.createSuccessResult(receiver));
      mockFriendRepository.update
        .mockResolvedValueOnce(TestUtils.createSuccessResult(acceptedFriend))
        .mockResolvedValueOnce(TestUtils.createSuccessResult(requester.updateFriendCount(1)))
        .mockResolvedValueOnce(TestUtils.createSuccessResult(receiver.updateFriendCount(1)));
      mockFriendRepository.save.mockResolvedValue(TestUtils.createSuccessResult(reciprocalFriend));
      mockFriendRepository.countFriendsByUserId.mockResolvedValue(TestUtils.createSuccessResult(1));

      const result = await useCase.execute(validRequest);

      expect(result.success).toBe(true);

      // Give some time for the async friend count update
      await new Promise(resolve => setTimeout(resolve, 50));

      expect(mockFriendRepository.countFriendsByUserId).toHaveBeenCalledTimes(2);
    });
  });
});