import { FriendEntity, FriendStatus, UserId, FriendId } from '../../domain/friend/friend.entity';

describe('FriendEntity', () => {
  const userId = new UserId('user1');
  const friendUserId = new UserId('user2');
  const friendId = new FriendId('friend123');

  describe('createFriendRequest', () => {
    it('should create a friend request successfully', () => {
      const result = FriendEntity.createFriendRequest(userId, friendUserId);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.userId).toEqual(userId);
        expect(result.data.friendUserId).toEqual(friendUserId);
        expect(result.data.status).toBe(FriendStatus.REQUESTED);
        expect(result.data.domainEventsSnapshot).toHaveLength(1);
      }
    });

    it('should fail when trying to create friend request with same user', () => {
      const result = FriendEntity.createFriendRequest(userId, userId);
      
      expect(result.success).toBe(false);
    });
  });

  describe('accept', () => {
    it('should accept a friend request successfully', () => {
      const friendRequest = new FriendEntity(
        friendId,
        userId,
        friendUserId,
        FriendStatus.REQUESTED,
        new Date(),
        undefined,
        new Date(),
        new Date()
      );

      const result = friendRequest.accept();
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.status).toBe(FriendStatus.ACCEPTED);
        expect(result.data.respondedAt).toBeDefined();
        expect(result.data.domainEventsSnapshot).toHaveLength(1);
      }
    });

    it('should fail to accept when status is not REQUESTED', () => {
      const friendRequest = new FriendEntity(
        friendId,
        userId,
        friendUserId,
        FriendStatus.ACCEPTED,
        new Date(),
        new Date(),
        new Date(),
        new Date()
      );

      const result = friendRequest.accept();
      
      expect(result.success).toBe(false);
    });
  });

  describe('reject', () => {
    it('should reject a friend request successfully', () => {
      const friendRequest = new FriendEntity(
        friendId,
        userId,
        friendUserId,
        FriendStatus.REQUESTED,
        new Date(),
        undefined,
        new Date(),
        new Date()
      );

      const result = friendRequest.reject();
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.status).toBe(FriendStatus.REJECTED);
        expect(result.data.respondedAt).toBeDefined();
        expect(result.data.domainEventsSnapshot).toHaveLength(1);
      }
    });

    it('should fail to reject when status is not REQUESTED', () => {
      const friendRequest = new FriendEntity(
        friendId,
        userId,
        friendUserId,
        FriendStatus.ACCEPTED,
        new Date(),
        new Date(),
        new Date(),
        new Date()
      );

      const result = friendRequest.reject();
      
      expect(result.success).toBe(false);
    });
  });

  describe('remove', () => {
    it('should remove a friend successfully', () => {
      const friendship = new FriendEntity(
        friendId,
        userId,
        friendUserId,
        FriendStatus.ACCEPTED,
        new Date(),
        new Date(),
        new Date(),
        new Date()
      );

      const result = friendship.remove();
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.status).toBe(FriendStatus.REMOVED);
        expect(result.data.domainEventsSnapshot).toHaveLength(1);
      }
    });

    it('should fail to remove when status is not ACCEPTED', () => {
      const friendRequest = new FriendEntity(
        friendId,
        userId,
        friendUserId,
        FriendStatus.REQUESTED,
        new Date(),
        undefined,
        new Date(),
        new Date()
      );

      const result = friendRequest.remove();
      
      expect(result.success).toBe(false);
    });
  });

  describe('helper methods', () => {
    const friendship = new FriendEntity(
      friendId,
      userId,
      friendUserId,
      FriendStatus.ACCEPTED,
      new Date(),
      new Date(),
      new Date(),
      new Date()
    );

    it('should correctly identify requester', () => {
      expect(friendship.isRequester(userId)).toBe(true);
      expect(friendship.isRequester(friendUserId)).toBe(false);
    });

    it('should correctly identify receiver', () => {
      expect(friendship.isReceiver(friendUserId)).toBe(true);
      expect(friendship.isReceiver(userId)).toBe(false);
    });

    it('should correctly identify active friendship', () => {
      expect(friendship.isActive()).toBe(true);
      
      const pendingFriendship = new FriendEntity(
        friendId,
        userId,
        friendUserId,
        FriendStatus.REQUESTED,
        new Date(),
        undefined,
        new Date(),
        new Date()
      );
      expect(pendingFriendship.isActive()).toBe(false);
    });

    it('should correctly identify pending friendship', () => {
      const pendingFriendship = new FriendEntity(
        friendId,
        userId,
        friendUserId,
        FriendStatus.REQUESTED,
        new Date(),
        undefined,
        new Date(),
        new Date()
      );
      expect(pendingFriendship.isPending()).toBe(true);
      expect(friendship.isPending()).toBe(false);
    });
  });

  describe('data conversion', () => {
    it('should convert to and from data correctly', () => {
      const originalFriend = new FriendEntity(
        friendId,
        userId,
        friendUserId,
        FriendStatus.ACCEPTED,
        new Date('2023-01-01'),
        new Date('2023-01-02'),
        new Date('2023-01-01'),
        new Date('2023-01-02')
      );

      const data = originalFriend.toData();
      const recreatedResult = FriendEntity.fromData(data);
      
      expect(recreatedResult.success).toBe(true);
      if (recreatedResult.success) {
        const recreatedFriend = recreatedResult.data;
        expect(recreatedFriend.id.value).toBe(originalFriend.id.value);
        expect(recreatedFriend.userId.value).toBe(originalFriend.userId.value);
        expect(recreatedFriend.friendUserId.value).toBe(originalFriend.friendUserId.value);
        expect(recreatedFriend.status).toBe(originalFriend.status);
      }
    });
  });
});