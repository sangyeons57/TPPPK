import { 
  sendFriendRequestFunction,
  acceptFriendRequestFunction,
  rejectFriendRequestFunction,
  removeFriendFunction,
  getFriendsFunction,
  getFriendRequestsFunction
} from '../../../triggers/friend/friendManagement.trigger';
import { 
  setupFirebaseTest, 
  cleanupFirebaseTest 
} from '../../helpers/firebaseTestSetup';
import { 
  testUnauthenticatedCallable,
  assertCallableSuccess,
  assertCallableError,
  testRequiredFields 
} from '../../helpers/triggerTestUtils';
import { Providers } from '../../../config/dependencies';

// Mock the dependencies
jest.mock('../../../config/dependencies');

describe('Friend Management Trigger Functions', () => {
  let mockFriendUseCases: any;

  beforeAll(() => {
    setupFirebaseTest();
  });

  afterAll(() => {
    cleanupFirebaseTest();
  });

  beforeEach(() => {
    jest.clearAllMocks();
    
    // Mock the friend use cases
    mockFriendUseCases = {
      sendFriendRequestUseCase: {
        execute: jest.fn(),
      },
      acceptFriendRequestUseCase: {
        execute: jest.fn(),
      },
      rejectFriendRequestUseCase: {
        execute: jest.fn(),
      },
      removeFriendUseCase: {
        execute: jest.fn(),
      },
      getFriendsUseCase: {
        execute: jest.fn(),
      },
      getFriendRequestsUseCase: {
        execute: jest.fn(),
      },
    };

    // Mock the provider
    const mockProvider = {
      create: jest.fn().mockReturnValue(mockFriendUseCases),
    };

    (Providers.getFriendProvider as jest.Mock).mockReturnValue(mockProvider);
  });

  describe('sendFriendRequestFunction', () => {
    describe('Success Cases', () => {
      it('should successfully send friend request', async () => {
        const mockResponse = {
          success: true,
          data: {
            friendRequestId: 'request-123',
            status: 'pending',
            createdAt: new Date().toISOString(),
          },
        };

        mockFriendUseCases.sendFriendRequestUseCase.execute.mockResolvedValue(mockResponse);

        const request = {
          requesterId: 'user-123',
          receiverUserId: 'user-456',
        };

        const response = await testUnauthenticatedCallable(
          sendFriendRequestFunction,
          request
        );

        assertCallableSuccess(response);
        expect(response.data.friendRequestId).toBe('request-123');
        expect(mockFriendUseCases.sendFriendRequestUseCase.execute).toHaveBeenCalledWith(request);
      });
    });

    describe('Validation Errors', () => {
      it('should require requesterId and receiverUserId', async () => {
        const validRequest = {
          requesterId: 'user-123',
          receiverUserId: 'user-456',
        };
        await testRequiredFields(sendFriendRequestFunction, validRequest, ['requesterId', 'receiverUserId']);
      });
    });

    describe('Business Logic Errors', () => {
      it('should handle user not found error', async () => {
        const errorResponse = {
          success: false,
          error: { message: 'User not found' },
        };

        mockFriendUseCases.sendFriendRequestUseCase.execute.mockResolvedValue(errorResponse);

        const response = await testUnauthenticatedCallable(
          sendFriendRequestFunction,
          { requesterId: 'user-123', receiverUserId: 'non-existent' }
        );

        assertCallableError(response, 'not-found');
      });

      it('should handle already friends error', async () => {
        const errorResponse = {
          success: false,
          error: { message: 'Users are already friends' },
        };

        mockFriendUseCases.sendFriendRequestUseCase.execute.mockResolvedValue(errorResponse);

        const response = await testUnauthenticatedCallable(
          sendFriendRequestFunction,
          { requesterId: 'user-123', receiverUserId: 'user-456' }
        );

        assertCallableError(response, 'already-exists');
      });

      it('should handle precondition failed error', async () => {
        const errorResponse = {
          success: false,
          error: { message: 'User cannot send friend request' },
        };

        mockFriendUseCases.sendFriendRequestUseCase.execute.mockResolvedValue(errorResponse);

        const response = await testUnauthenticatedCallable(
          sendFriendRequestFunction,
          { requesterId: 'user-123', receiverUserId: 'user-456' }
        );

        assertCallableError(response, 'failed-precondition');
      });
    });
  });

  describe('acceptFriendRequestFunction', () => {
    describe('Success Cases', () => {
      it('should successfully accept friend request', async () => {
        const mockResponse = {
          success: true,
          data: {
            friendship: {
              user1Id: 'user-123',
              user2Id: 'user-456',
              status: 'accepted',
              acceptedAt: new Date().toISOString(),
            },
          },
        };

        mockFriendUseCases.acceptFriendRequestUseCase.execute.mockResolvedValue(mockResponse);

        const request = {
          friendRequestId: 'request-123',
          userId: 'user-456',
        };

        const response = await testUnauthenticatedCallable(
          acceptFriendRequestFunction,
          request
        );

        assertCallableSuccess(response);
        expect(response.data.friendship.status).toBe('accepted');
        expect(mockFriendUseCases.acceptFriendRequestUseCase.execute).toHaveBeenCalledWith(request);
      });
    });

    describe('Validation Errors', () => {
      it('should require friendRequestId and userId', async () => {
        const validRequest = {
          friendRequestId: 'request-123',
          userId: 'user-456',
        };
        await testRequiredFields(acceptFriendRequestFunction, validRequest, ['friendRequestId', 'userId']);
      });
    });

    describe('Business Logic Errors', () => {
      it('should handle permission denied error', async () => {
        const errorResponse = {
          success: false,
          error: { message: 'Only the receiver can accept this request' },
        };

        mockFriendUseCases.acceptFriendRequestUseCase.execute.mockResolvedValue(errorResponse);

        const response = await testUnauthenticatedCallable(
          acceptFriendRequestFunction,
          { friendRequestId: 'request-123', userId: 'user-123' }
        );

        assertCallableError(response, 'permission-denied');
      });

      it('should handle invalid status error', async () => {
        const errorResponse = {
          success: false,
          error: { message: 'Current status does not allow acceptance' },
        };

        mockFriendUseCases.acceptFriendRequestUseCase.execute.mockResolvedValue(errorResponse);

        const response = await testUnauthenticatedCallable(
          acceptFriendRequestFunction,
          { friendRequestId: 'request-123', userId: 'user-456' }
        );

        assertCallableError(response, 'failed-precondition');
      });
    });
  });

  describe('rejectFriendRequestFunction', () => {
    describe('Success Cases', () => {
      it('should successfully reject friend request', async () => {
        const mockResponse = {
          success: true,
          data: {
            friendRequestId: 'request-123',
            status: 'rejected',
            rejectedAt: new Date().toISOString(),
          },
        };

        mockFriendUseCases.rejectFriendRequestUseCase.execute.mockResolvedValue(mockResponse);

        const request = {
          friendRequestId: 'request-123',
          userId: 'user-456',
        };

        const response = await testUnauthenticatedCallable(
          rejectFriendRequestFunction,
          request
        );

        assertCallableSuccess(response);
        expect(response.data.status).toBe('rejected');
        expect(mockFriendUseCases.rejectFriendRequestUseCase.execute).toHaveBeenCalledWith(request);
      });
    });

    describe('Validation Errors', () => {
      it('should require friendRequestId and userId', async () => {
        const validRequest = {
          friendRequestId: 'request-123',
          userId: 'user-456',
        };
        await testRequiredFields(rejectFriendRequestFunction, validRequest, ['friendRequestId', 'userId']);
      });
    });
  });

  describe('removeFriendFunction', () => {
    describe('Success Cases', () => {
      it('should successfully remove friend', async () => {
        const mockResponse = {
          success: true,
          data: {
            message: 'Friend removed successfully',
            removedAt: new Date().toISOString(),
          },
        };

        mockFriendUseCases.removeFriendUseCase.execute.mockResolvedValue(mockResponse);

        const request = {
          userId: 'user-123',
          friendUserId: 'user-456',
        };

        const response = await testUnauthenticatedCallable(
          removeFriendFunction,
          request
        );

        assertCallableSuccess(response);
        expect(response.data.message).toBe('Friend removed successfully');
        expect(mockFriendUseCases.removeFriendUseCase.execute).toHaveBeenCalledWith(request);
      });
    });

    describe('Validation Errors', () => {
      it('should require userId and friendUserId', async () => {
        const validRequest = {
          userId: 'user-123',
          friendUserId: 'user-456',
        };
        await testRequiredFields(removeFriendFunction, validRequest, ['userId', 'friendUserId']);
      });
    });

    describe('Business Logic Errors', () => {
      it('should handle not friends error', async () => {
        const errorResponse = {
          success: false,
          error: { message: 'Users are not friends' },
        };

        mockFriendUseCases.removeFriendUseCase.execute.mockResolvedValue(errorResponse);

        const response = await testUnauthenticatedCallable(
          removeFriendFunction,
          { userId: 'user-123', friendUserId: 'user-456' }
        );

        assertCallableError(response, 'failed-precondition');
      });
    });
  });

  describe('getFriendsFunction', () => {
    describe('Success Cases', () => {
      it('should successfully get friends list', async () => {
        const mockResponse = {
          success: true,
          data: {
            friends: [
              { userId: 'user-456', displayName: 'Friend 1', status: 'accepted' },
              { userId: 'user-789', displayName: 'Friend 2', status: 'accepted' },
            ],
            total: 2,
            hasMore: false,
          },
        };

        mockFriendUseCases.getFriendsUseCase.execute.mockResolvedValue(mockResponse);

        const request = {
          userId: 'user-123',
          status: 'accepted',
          limit: 10,
          offset: 0,
        };

        const response = await testUnauthenticatedCallable(
          getFriendsFunction,
          request
        );

        assertCallableSuccess(response);
        expect(response.data.friends).toHaveLength(2);
        expect(response.data.total).toBe(2);
        expect(mockFriendUseCases.getFriendsUseCase.execute).toHaveBeenCalledWith(request);
      });

      it('should work with minimal parameters', async () => {
        const mockResponse = {
          success: true,
          data: { friends: [], total: 0, hasMore: false },
        };

        mockFriendUseCases.getFriendsUseCase.execute.mockResolvedValue(mockResponse);

        const response = await testUnauthenticatedCallable(
          getFriendsFunction,
          { userId: 'user-123' }
        );

        assertCallableSuccess(response);
        expect(mockFriendUseCases.getFriendsUseCase.execute).toHaveBeenCalledWith({
          userId: 'user-123',
          status: undefined,
          limit: undefined,
          offset: undefined,
        });
      });
    });

    describe('Validation Errors', () => {
      it('should require userId', async () => {
        const response = await testUnauthenticatedCallable(
          getFriendsFunction,
          {}
        );

        assertCallableError(response, 'invalid-argument');
        expect(response.error!.message).toContain('User ID is required');
      });
    });
  });

  describe('getFriendRequestsFunction', () => {
    describe('Success Cases', () => {
      it('should successfully get received friend requests', async () => {
        const mockResponse = {
          success: true,
          data: {
            friendRequests: [
              {
                id: 'request-123',
                requesterId: 'user-456',
                receiverId: 'user-123',
                status: 'pending',
                createdAt: new Date().toISOString(),
              },
            ],
            total: 1,
            hasMore: false,
          },
        };

        mockFriendUseCases.getFriendRequestsUseCase.execute.mockResolvedValue(mockResponse);

        const request = {
          userId: 'user-123',
          type: 'received' as const,
          limit: 10,
          offset: 0,
        };

        const response = await testUnauthenticatedCallable(
          getFriendRequestsFunction,
          request
        );

        assertCallableSuccess(response);
        expect(response.data.friendRequests).toHaveLength(1);
        expect(response.data.total).toBe(1);
        expect(mockFriendUseCases.getFriendRequestsUseCase.execute).toHaveBeenCalledWith(request);
      });

      it('should successfully get sent friend requests', async () => {
        const mockResponse = {
          success: true,
          data: {
            friendRequests: [
              {
                id: 'request-456',
                requesterId: 'user-123',
                receiverId: 'user-789',
                status: 'pending',
                createdAt: new Date().toISOString(),
              },
            ],
            total: 1,
            hasMore: false,
          },
        };

        mockFriendUseCases.getFriendRequestsUseCase.execute.mockResolvedValue(mockResponse);

        const request = {
          userId: 'user-123',
          type: 'sent' as const,
        };

        const response = await testUnauthenticatedCallable(
          getFriendRequestsFunction,
          request
        );

        assertCallableSuccess(response);
        expect(response.data.friendRequests).toHaveLength(1);
        expect(mockFriendUseCases.getFriendRequestsUseCase.execute).toHaveBeenCalledWith({
          userId: 'user-123',
          type: 'sent',
          limit: undefined,
          offset: undefined,
        });
      });
    });

    describe('Validation Errors', () => {
      it('should require userId and type', async () => {
        const validRequest = {
          userId: 'user-123',
          type: 'received' as const,
        };
        await testRequiredFields(getFriendRequestsFunction, validRequest, ['userId', 'type']);
      });

      it('should validate type values', async () => {
        const response = await testUnauthenticatedCallable(
          getFriendRequestsFunction,
          {
            userId: 'user-123',
            type: 'invalid',
          }
        );

        assertCallableError(response, 'invalid-argument');
        expect(response.error!.message).toContain("Type must be either 'received' or 'sent'");
      });

      it('should reject empty type', async () => {
        const response = await testUnauthenticatedCallable(
          getFriendRequestsFunction,
          {
            userId: 'user-123',
            type: '',
          }
        );

        assertCallableError(response, 'invalid-argument');
        expect(response.error!.message).toContain('User ID and type are required');
      });
    });
  });

  describe('System Error Handling', () => {
    it('should handle unexpected errors in all functions', async () => {
      const functions = [
        { fn: sendFriendRequestFunction, data: { requesterId: 'user-123', receiverUserId: 'user-456' } },
        { fn: acceptFriendRequestFunction, data: { friendRequestId: 'request-123', userId: 'user-456' } },
        { fn: rejectFriendRequestFunction, data: { friendRequestId: 'request-123', userId: 'user-456' } },
        { fn: removeFriendFunction, data: { userId: 'user-123', friendUserId: 'user-456' } },
        { fn: getFriendsFunction, data: { userId: 'user-123' } },
        { fn: getFriendRequestsFunction, data: { userId: 'user-123', type: 'received' } },
      ];

      // Mock all use cases to throw errors
      Object.values(mockFriendUseCases).forEach((useCase: any) => {
        useCase.execute.mockRejectedValue(new Error('Database connection failed'));
      });

      for (const { fn, data } of functions) {
        const response = await testUnauthenticatedCallable(fn, data);
        assertCallableError(response, 'internal');
        expect(response.error!.message).toContain('Database connection failed');
      }
    });
  });

  describe('Integration Scenarios', () => {
    it('should handle complete friend request flow', async () => {
      // Step 1: Send friend request
      const sendResponse = {
        success: true,
        data: { friendRequestId: 'request-123', status: 'pending' },
      };
      mockFriendUseCases.sendFriendRequestUseCase.execute.mockResolvedValue(sendResponse);

      const sendResult = await testUnauthenticatedCallable(
        sendFriendRequestFunction,
        { requesterId: 'user-123', receiverUserId: 'user-456' }
      );

      assertCallableSuccess(sendResult);
      const requestId = sendResult.data.friendRequestId;

      // Step 2: Accept friend request
      const acceptResponse = {
        success: true,
        data: { friendship: { status: 'accepted' } },
      };
      mockFriendUseCases.acceptFriendRequestUseCase.execute.mockResolvedValue(acceptResponse);

      const acceptResult = await testUnauthenticatedCallable(
        acceptFriendRequestFunction,
        { friendRequestId: requestId, userId: 'user-456' }
      );

      assertCallableSuccess(acceptResult);
      expect(acceptResult.data.friendship.status).toBe('accepted');

      // Step 3: Verify friendship exists
      const friendsResponse = {
        success: true,
        data: { friends: [{ userId: 'user-456', status: 'accepted' }], total: 1 },
      };
      mockFriendUseCases.getFriendsUseCase.execute.mockResolvedValue(friendsResponse);

      const friendsResult = await testUnauthenticatedCallable(
        getFriendsFunction,
        { userId: 'user-123' }
      );

      assertCallableSuccess(friendsResult);
      expect(friendsResult.data.friends).toHaveLength(1);
    });
  });
});