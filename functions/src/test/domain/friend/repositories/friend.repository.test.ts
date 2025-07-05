import { FriendRepository, FriendSearchCriteria } from '../../../domain/friend/friend.repository';
import { FriendEntity, FriendStatus, UserId, FriendId } from '../../../domain/friend/friend.entity';
import { TestFactories, TestUtils } from '../../helpers';

describe('FriendRepository Contract Tests', () => {
  let repository: FriendRepository;
  let testFriend: FriendEntity;
  let userId: UserId;
  let friendUserId: UserId;
  let friendId: FriendId;

  beforeEach(() => {
    userId = new UserId('user_123');
    friendUserId = new UserId('user_456');
    friendId = new FriendId('friend_123');
    
    testFriend = TestFactories.createFriend({
      id: friendId.value,
      userId: userId.value,
      friendUserId: friendUserId.value,
      status: FriendStatus.ACCEPTED,
    });

    // Mock repository will be provided by concrete implementation tests
    repository = {} as FriendRepository;
  });

  describe('findById', () => {
    it('should return friend when found', async () => {
      const mockResult = TestUtils.createSuccessResult(testFriend);
      repository.findById = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findById(friendId);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(testFriend);
      }
      expect(repository.findById).toHaveBeenCalledWith(friendId);
    });

    it('should return null when friend not found', async () => {
      const mockResult = TestUtils.createSuccessResult(null);
      repository.findById = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findById(friendId);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBeNull();
      }
    });

    it('should return error when repository fails', async () => {
      const error = new Error('Database connection failed');
      const mockResult = TestUtils.createFailureResult(error);
      repository.findById = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findById(friendId);
      
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(error);
      }
    });
  });

  describe('findByUserIds', () => {
    it('should return friendship between two users', async () => {
      const mockResult = TestUtils.createSuccessResult(testFriend);
      repository.findByUserIds = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByUserIds(userId, friendUserId);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(testFriend);
      }
      expect(repository.findByUserIds).toHaveBeenCalledWith(userId, friendUserId);
    });

    it('should return null when no friendship exists', async () => {
      const mockResult = TestUtils.createSuccessResult(null);
      repository.findByUserIds = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByUserIds(userId, friendUserId);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBeNull();
      }
    });
  });

  describe('findFriendsByUserId', () => {
    it('should return all friends when no status filter', async () => {
      const friends = [testFriend];
      const mockResult = TestUtils.createSuccessResult(friends);
      repository.findFriendsByUserId = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findFriendsByUserId(userId);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual(friends);
      }
      expect(repository.findFriendsByUserId).toHaveBeenCalledWith(userId, undefined);
    });

    it('should return friends with specific status', async () => {
      const friends = [testFriend];
      const mockResult = TestUtils.createSuccessResult(friends);
      repository.findFriendsByUserId = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findFriendsByUserId(userId, FriendStatus.ACCEPTED);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual(friends);
      }
      expect(repository.findFriendsByUserId).toHaveBeenCalledWith(userId, FriendStatus.ACCEPTED);
    });

    it('should return empty array when no friends found', async () => {
      const mockResult = TestUtils.createSuccessResult([]);
      repository.findFriendsByUserId = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findFriendsByUserId(userId);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual([]);
      }
    });
  });

  describe('findReceivedFriendRequests', () => {
    it('should return received friend requests', async () => {
      const requests = [TestFactories.createFriend({
        friendUserId: userId.value,
        status: FriendStatus.REQUESTED,
      })];
      const mockResult = TestUtils.createSuccessResult(requests);
      repository.findReceivedFriendRequests = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findReceivedFriendRequests(userId);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual(requests);
      }
    });
  });

  describe('findSentFriendRequests', () => {
    it('should return sent friend requests', async () => {
      const requests = [TestFactories.createFriend({
        userId: userId.value,
        status: FriendStatus.REQUESTED,
      })];
      const mockResult = TestUtils.createSuccessResult(requests);
      repository.findSentFriendRequests = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findSentFriendRequests(userId);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual(requests);
      }
    });
  });

  describe('areUsersFriends', () => {
    it('should return true when users are friends', async () => {
      const mockResult = TestUtils.createSuccessResult(true);
      repository.areUsersFriends = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.areUsersFriends(userId, friendUserId);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(true);
      }
    });

    it('should return false when users are not friends', async () => {
      const mockResult = TestUtils.createSuccessResult(false);
      repository.areUsersFriends = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.areUsersFriends(userId, friendUserId);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(false);
      }
    });
  });

  describe('friendRequestExists', () => {
    it('should return true when friend request exists', async () => {
      const mockResult = TestUtils.createSuccessResult(true);
      repository.friendRequestExists = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.friendRequestExists(userId, friendUserId);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(true);
      }
    });

    it('should return false when friend request does not exist', async () => {
      const mockResult = TestUtils.createSuccessResult(false);
      repository.friendRequestExists = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.friendRequestExists(userId, friendUserId);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(false);
      }
    });
  });

  describe('save', () => {
    it('should save friend and return saved entity', async () => {
      const mockResult = TestUtils.createSuccessResult(testFriend);
      repository.save = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.save(testFriend);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(testFriend);
      }
      expect(repository.save).toHaveBeenCalledWith(testFriend);
    });

    it('should return error when save fails', async () => {
      const error = new Error('Save failed');
      const mockResult = TestUtils.createFailureResult(error);
      repository.save = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.save(testFriend);
      
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(error);
      }
    });
  });

  describe('update', () => {
    it('should update friend and return updated entity', async () => {
      const mockResult = TestUtils.createSuccessResult(testFriend);
      repository.update = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.update(testFriend);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(testFriend);
      }
      expect(repository.update).toHaveBeenCalledWith(testFriend);
    });
  });

  describe('delete', () => {
    it('should delete friend by ID', async () => {
      const mockResult = TestUtils.createSuccessResult(undefined);
      repository.delete = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.delete(friendId);
      
      expect(result.success).toBe(true);
      expect(repository.delete).toHaveBeenCalledWith(friendId);
    });
  });

  describe('deleteByUserIds', () => {
    it('should delete friendship between two users', async () => {
      const mockResult = TestUtils.createSuccessResult(undefined);
      repository.deleteByUserIds = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.deleteByUserIds(userId, friendUserId);
      
      expect(result.success).toBe(true);
      expect(repository.deleteByUserIds).toHaveBeenCalledWith(userId, friendUserId);
    });
  });

  describe('deleteAllByUserId', () => {
    it('should delete all friendships for a user', async () => {
      const mockResult = TestUtils.createSuccessResult(undefined);
      repository.deleteAllByUserId = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.deleteAllByUserId(userId);
      
      expect(result.success).toBe(true);
      expect(repository.deleteAllByUserId).toHaveBeenCalledWith(userId);
    });
  });

  describe('findByCriteria', () => {
    it('should find friends by search criteria', async () => {
      const criteria: FriendSearchCriteria = {
        userId: userId.value,
        status: FriendStatus.ACCEPTED,
        limit: 10,
      };
      const friends = [testFriend];
      const mockResult = TestUtils.createSuccessResult(friends);
      repository.findByCriteria = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByCriteria(criteria);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual(friends);
      }
      expect(repository.findByCriteria).toHaveBeenCalledWith(criteria);
    });
  });

  describe('countFriendsByUserId', () => {
    it('should return friend count for user', async () => {
      const mockResult = TestUtils.createSuccessResult(5);
      repository.countFriendsByUserId = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.countFriendsByUserId(userId);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(5);
      }
    });
  });

  describe('countPendingRequestsByUserId', () => {
    it('should return pending request count for user', async () => {
      const mockResult = TestUtils.createSuccessResult(3);
      repository.countPendingRequestsByUserId = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.countPendingRequestsByUserId(userId);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(3);
      }
    });
  });
});