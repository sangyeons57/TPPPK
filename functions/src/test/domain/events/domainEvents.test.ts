import { 
  DomainEvent,
  FriendRequestSentEvent,
  FriendRequestAcceptedEvent,
  FriendRequestRejectedEvent,
  FriendRemovedEvent,
  FriendEntity,
  FriendStatus,
  UserId,
  FriendId
} from '../../../domain/friend/friend.entity';
import { TestFactories, TestUtils } from '../../helpers';

describe('Domain Events', () => {
  let mockDateNow: jest.SpyInstance;
  const fixedDate = TestUtils.createMockDate('2023-01-15T12:00:00.000Z');

  beforeEach(() => {
    mockDateNow = TestUtils.mockDateNow(fixedDate);
  });

  afterEach(() => {
    TestUtils.restoreAllMocks();
  });

  describe('DomainEvent Base Class', () => {
    class TestDomainEvent extends DomainEvent {
      constructor(aggregateId: string, public readonly testData: string) {
        super(aggregateId);
      }
    }

    it('should set occurredAt to current date', () => {
      const event = new TestDomainEvent('test_aggregate_123', 'test data');

      expect(event.aggregateId).toBe('test_aggregate_123');
      expect(event.testData).toBe('test data');
      expect(event.occurredAt).toEqual(fixedDate);
    });

    it('should have unique occurredAt for each event', () => {
      TestUtils.restoreAllMocks();

      const event1 = new TestDomainEvent('test_1', 'data 1');
      
      // Advance time slightly
      jest.spyOn(Date, 'now').mockReturnValue(Date.now() + 100);
      const event2 = new TestDomainEvent('test_2', 'data 2');

      expect(event1.occurredAt.getTime()).toBeLessThan(event2.occurredAt.getTime());

      TestUtils.restoreAllMocks();
    });
  });

  describe('FriendRequestSentEvent', () => {
    it('should create event with correct properties', () => {
      const event = new FriendRequestSentEvent('friend_123', 'user_456', 'user_789');

      expect(event.aggregateId).toBe('friend_123');
      expect(event.requesterId).toBe('user_456');
      expect(event.receiverId).toBe('user_789');
      expect(event.occurredAt).toEqual(fixedDate);
    });
  });

  describe('FriendRequestAcceptedEvent', () => {
    it('should create event with correct properties', () => {
      const event = new FriendRequestAcceptedEvent('friend_123', 'user_456', 'user_789');

      expect(event.aggregateId).toBe('friend_123');
      expect(event.requesterId).toBe('user_456');
      expect(event.receiverId).toBe('user_789');
      expect(event.occurredAt).toEqual(fixedDate);
    });
  });

  describe('FriendRequestRejectedEvent', () => {
    it('should create event with correct properties', () => {
      const event = new FriendRequestRejectedEvent('friend_123', 'user_456', 'user_789');

      expect(event.aggregateId).toBe('friend_123');
      expect(event.requesterId).toBe('user_456');
      expect(event.receiverId).toBe('user_789');
      expect(event.occurredAt).toEqual(fixedDate);
    });
  });

  describe('FriendRemovedEvent', () => {
    it('should create event with correct properties', () => {
      const event = new FriendRemovedEvent('friend_123', 'user_456', 'user_789');

      expect(event.aggregateId).toBe('friend_123');
      expect(event.userId).toBe('user_456');
      expect(event.friendUserId).toBe('user_789');
      expect(event.occurredAt).toEqual(fixedDate);
    });
  });

  describe('Domain Events Integration with FriendEntity', () => {
    let userId: UserId;
    let friendUserId: UserId;

    beforeEach(() => {
      userId = new UserId('user_123');
      friendUserId = new UserId('user_456');
    });

    describe('createFriendRequest', () => {
      it('should generate FriendRequestSentEvent', () => {
        const result = FriendEntity.createFriendRequest(userId, friendUserId);

        expect(result.success).toBe(true);
        if (result.success) {
          const friend = result.data;
          const events = friend.domainEventsSnapshot;

          expect(events).toHaveLength(1);
          expect(events[0]).toBeInstanceOf(FriendRequestSentEvent);

          const event = events[0] as FriendRequestSentEvent;
          expect(event.requesterId).toBe(userId.value);
          expect(event.receiverId).toBe(friendUserId.value);
          expect(event.aggregateId).toBe(friend.id.value);
        }
      });

      it('should clear domain events', () => {
        const result = FriendEntity.createFriendRequest(userId, friendUserId);

        expect(result.success).toBe(true);
        if (result.success) {
          const friend = result.data;
          expect(friend.domainEventsSnapshot).toHaveLength(1);

          friend.clearDomainEvents();
          expect(friend.domainEventsSnapshot).toHaveLength(0);
        }
      });
    });

    describe('accept', () => {
      it('should generate FriendRequestAcceptedEvent', () => {
        const pendingFriend = TestFactories.createFriend({
          userId: userId.value,
          friendUserId: friendUserId.value,
          status: FriendStatus.REQUESTED,
        });

        const result = pendingFriend.accept();

        expect(result.success).toBe(true);
        if (result.success) {
          const acceptedFriend = result.data;
          const events = acceptedFriend.domainEventsSnapshot;

          expect(events).toHaveLength(1);
          expect(events[0]).toBeInstanceOf(FriendRequestAcceptedEvent);

          const event = events[0] as FriendRequestAcceptedEvent;
          expect(event.requesterId).toBe(userId.value);
          expect(event.receiverId).toBe(friendUserId.value);
          expect(event.aggregateId).toBe(acceptedFriend.id.value);
        }
      });
    });

    describe('reject', () => {
      it('should generate FriendRequestRejectedEvent', () => {
        const pendingFriend = TestFactories.createFriend({
          userId: userId.value,
          friendUserId: friendUserId.value,
          status: FriendStatus.REQUESTED,
        });

        const result = pendingFriend.reject();

        expect(result.success).toBe(true);
        if (result.success) {
          const rejectedFriend = result.data;
          const events = rejectedFriend.domainEventsSnapshot;

          expect(events).toHaveLength(1);
          expect(events[0]).toBeInstanceOf(FriendRequestRejectedEvent);

          const event = events[0] as FriendRequestRejectedEvent;
          expect(event.requesterId).toBe(userId.value);
          expect(event.receiverId).toBe(friendUserId.value);
          expect(event.aggregateId).toBe(rejectedFriend.id.value);
        }
      });
    });

    describe('remove', () => {
      it('should generate FriendRemovedEvent', () => {
        const acceptedFriend = TestFactories.createFriend({
          userId: userId.value,
          friendUserId: friendUserId.value,
          status: FriendStatus.ACCEPTED,
        });

        const result = acceptedFriend.remove();

        expect(result.success).toBe(true);
        if (result.success) {
          const removedFriend = result.data;
          const events = removedFriend.domainEventsSnapshot;

          expect(events).toHaveLength(1);
          expect(events[0]).toBeInstanceOf(FriendRemovedEvent);

          const event = events[0] as FriendRemovedEvent;
          expect(event.userId).toBe(userId.value);
          expect(event.friendUserId).toBe(friendUserId.value);
          expect(event.aggregateId).toBe(removedFriend.id.value);
        }
      });
    });

    describe('domainEventsSnapshot', () => {
      it('should return immutable copy of events', () => {
        const result = FriendEntity.createFriendRequest(userId, friendUserId);

        expect(result.success).toBe(true);
        if (result.success) {
          const friend = result.data;
          const events1 = friend.domainEventsSnapshot;
          const events2 = friend.domainEventsSnapshot;

          expect(events1).toEqual(events2);
          expect(events1).not.toBe(events2); // Different instances

          // Modifying the returned array should not affect the original
          events1.push(new FriendRequestAcceptedEvent('test', 'test1', 'test2'));
          expect(friend.domainEventsSnapshot).toHaveLength(1);
        }
      });
    });

    describe('multiple operations with events', () => {
      it('should accumulate events from multiple operations', () => {
        // Create a friend request
        const createResult = FriendEntity.createFriendRequest(userId, friendUserId);
        expect(createResult.success).toBe(true);

        if (createResult.success) {
          let friend = createResult.data;
          expect(friend.domainEventsSnapshot).toHaveLength(1);
          expect(friend.domainEventsSnapshot[0]).toBeInstanceOf(FriendRequestSentEvent);

          // Accept the request (this creates a new entity)
          const acceptResult = friend.accept();
          expect(acceptResult.success).toBe(true);

          if (acceptResult.success) {
            friend = acceptResult.data;
            expect(friend.domainEventsSnapshot).toHaveLength(1);
            expect(friend.domainEventsSnapshot[0]).toBeInstanceOf(FriendRequestAcceptedEvent);

            // Remove the friendship (this creates another new entity)
            const removeResult = friend.remove();
            expect(removeResult.success).toBe(true);

            if (removeResult.success) {
              friend = removeResult.data;
              expect(friend.domainEventsSnapshot).toHaveLength(1);
              expect(friend.domainEventsSnapshot[0]).toBeInstanceOf(FriendRemovedEvent);
            }
          }
        }
      });
    });

    describe('event timing', () => {
      it('should have recent occurredAt timestamp', () => {
        TestUtils.restoreAllMocks();
        const beforeTime = Date.now();

        const result = FriendEntity.createFriendRequest(userId, friendUserId);

        const afterTime = Date.now();

        expect(result.success).toBe(true);
        if (result.success) {
          const friend = result.data;
          const events = friend.domainEventsSnapshot;
          const eventTime = events[0].occurredAt.getTime();

          expect(eventTime).toBeGreaterThanOrEqual(beforeTime);
          expect(eventTime).toBeLessThanOrEqual(afterTime);
        }
      });
    });
  });
});