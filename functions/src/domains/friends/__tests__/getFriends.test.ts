import { getFriends } from "../http/getFriends";
import { mockFirestore, MockFirestoreHelper, createMockRequest, TEST_USERS, testCallable } from "../../../__tests__/helpers";
import { HttpsError } from "firebase-functions/v2/https";

// Mock admin.firestore
jest.mock("firebase-admin", () => ({
  firestore: () => mockFirestore,
  app: () => ({}),
}));

describe("getFriends", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    MockFirestoreHelper.reset();
  });

  describe("Authentication", () => {
    it("should throw error when user is not authenticated", async () => {
      const request = createMockRequest({});

      await expect(testCallable(getFriends, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(getFriends, request)).rejects.toThrow("User not authenticated");
    });
  });

  describe("Successful Retrieval", () => {
    it("should return friends list successfully", async () => {
      const mockFriendships = [
        {
          id: "friendship1",
          data: () => ({
            requesterId: TEST_USERS.ALICE.uid,
            receiverId: TEST_USERS.BOB.uid,
            status: "accepted",
            acceptedAt: { toDate: () => new Date("2024-01-01") },
            createdAt: { toDate: () => new Date("2024-01-01") },
          }),
        },
        {
          id: "friendship2",
          data: () => ({
            requesterId: TEST_USERS.CHARLIE.uid,
            receiverId: TEST_USERS.ALICE.uid,
            status: "accepted",
            acceptedAt: { toDate: () => new Date("2024-01-02") },
            createdAt: { toDate: () => new Date("2024-01-02") },
          }),
        },
      ];

      // Mock friendships query
      mockFirestore.collection().where().where().get.mockResolvedValueOnce({
        docs: mockFriendships,
      });

      // Mock user data fetches
      mockFirestore.collection().doc().get
        .mockResolvedValueOnce({
          exists: true,
          data: () => ({
            name: TEST_USERS.BOB.name,
            email: TEST_USERS.BOB.email,
            profileImageUrl: TEST_USERS.BOB.profileImageUrl,
          }),
        })
        .mockResolvedValueOnce({
          exists: true,
          data: () => ({
            name: TEST_USERS.CHARLIE.name,
            email: TEST_USERS.CHARLIE.email,
            profileImageUrl: TEST_USERS.CHARLIE.profileImageUrl,
          }),
        });

      const request = createMockRequest({}, TEST_USERS.ALICE);

      const result = await testCallable(getFriends, request);

      expect(result).toEqual({
        friends: [
          {
            id: TEST_USERS.BOB.uid,
            name: TEST_USERS.BOB.name,
            email: TEST_USERS.BOB.email,
            profileImageUrl: TEST_USERS.BOB.profileImageUrl,
            friendshipId: "friendship1",
            friendsSince: new Date("2024-01-01").toISOString(),
          },
          {
            id: TEST_USERS.CHARLIE.uid,
            name: TEST_USERS.CHARLIE.name,
            email: TEST_USERS.CHARLIE.email,
            profileImageUrl: TEST_USERS.CHARLIE.profileImageUrl,
            friendshipId: "friendship2",
            friendsSince: new Date("2024-01-02").toISOString(),
          },
        ],
        total: 2,
      });
    });

    it("should return empty list when no friends", async () => {
      // Mock empty friendships query
      mockFirestore.collection().where().where().get.mockResolvedValueOnce({
        docs: [],
      });

      const request = createMockRequest({}, TEST_USERS.ALICE);

      const result = await testCallable(getFriends, request);

      expect(result).toEqual({
        friends: [],
        total: 0,
      });
    });

    it("should handle pagination correctly", async () => {
      const mockFriendships = Array.from({ length: 5 }, (_, i) => ({
        id: `friendship${i + 1}`,
        data: () => ({
          requesterId: i % 2 === 0 ? TEST_USERS.ALICE.uid : `friend${i}`,
          receiverId: i % 2 === 0 ? `friend${i}` : TEST_USERS.ALICE.uid,
          status: "accepted",
          acceptedAt: { toDate: () => new Date(`2024-01-0${i + 1}`) },
          createdAt: { toDate: () => new Date(`2024-01-0${i + 1}`) },
        }),
      }));

      // Mock friendships query with limit
      mockFirestore.collection().where().where().limit().get.mockResolvedValueOnce({
        docs: mockFriendships.slice(0, 3), // First 3 friends
      });

      // Mock user data fetches
      for (let i = 0; i < 3; i++) {
        mockFirestore.collection().doc().get.mockResolvedValueOnce({
          exists: true,
          data: () => ({
            name: `Friend ${i}`,
            email: `friend${i}@test.com`,
          }),
        });
      }

      const request = createMockRequest(
        { limit: 3, offset: 0 },
        TEST_USERS.ALICE
      );

      const result = await testCallable(getFriends, request);

      expect(result.friends).toHaveLength(3);
      expect(mockFirestore.collection().where().where().limit).toHaveBeenCalledWith(3);
    });
  });

  describe("Friend Data Handling", () => {
    it("should handle missing friend user data", async () => {
      const mockFriendships = [
        {
          id: "friendship1",
          data: () => ({
            requesterId: TEST_USERS.ALICE.uid,
            receiverId: "deleted-user",
            status: "accepted",
            acceptedAt: { toDate: () => new Date("2024-01-01") },
            createdAt: { toDate: () => new Date("2024-01-01") },
          }),
        },
      ];

      // Mock friendships query
      mockFirestore.collection().where().where().get.mockResolvedValueOnce({
        docs: mockFriendships,
      });

      // Mock user not found
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        exists: false,
      });

      const request = createMockRequest({}, TEST_USERS.ALICE);

      const result = await testCallable(getFriends, request);

      // Should filter out friends with missing user data
      expect(result).toEqual({
        friends: [],
        total: 0,
      });
    });

    it("should determine correct friend ID when user is receiver", async () => {
      const mockFriendships = [
        {
          id: "friendship1",
          data: () => ({
            requesterId: TEST_USERS.BOB.uid,
            receiverId: TEST_USERS.ALICE.uid, // Alice is receiver
            status: "accepted",
            acceptedAt: { toDate: () => new Date("2024-01-01") },
            createdAt: { toDate: () => new Date("2024-01-01") },
          }),
        },
      ];

      // Mock friendships query
      mockFirestore.collection().where().where().get.mockResolvedValueOnce({
        docs: mockFriendships,
      });

      // Mock user data fetch for Bob
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: TEST_USERS.BOB.name,
          email: TEST_USERS.BOB.email,
        }),
      });

      const request = createMockRequest({}, TEST_USERS.ALICE);

      const result = await testCallable(getFriends, request);

      expect(result.friends[0].id).toBe(TEST_USERS.BOB.uid);
    });
  });

  describe("Error Handling", () => {
    it("should handle Firestore query errors", async () => {
      mockFirestore.collection().where().where().get.mockRejectedValueOnce(
        new Error("Query failed")
      );

      const request = createMockRequest({}, TEST_USERS.ALICE);

      await expect(testCallable(getFriends, request)).rejects.toThrow("Query failed");
    });

    it("should handle user data fetch errors", async () => {
      const mockFriendships = [
        {
          id: "friendship1",
          data: () => ({
            requesterId: TEST_USERS.ALICE.uid,
            receiverId: TEST_USERS.BOB.uid,
            status: "accepted",
            acceptedAt: { toDate: () => new Date("2024-01-01") },
            createdAt: { toDate: () => new Date("2024-01-01") },
          }),
        },
      ];

      // Mock friendships query
      mockFirestore.collection().where().where().get.mockResolvedValueOnce({
        docs: mockFriendships,
      });

      // Mock user data fetch error
      mockFirestore.collection().doc().get.mockRejectedValueOnce(
        new Error("User fetch failed")
      );

      const request = createMockRequest({}, TEST_USERS.ALICE);

      await expect(testCallable(getFriends, request)).rejects.toThrow("User fetch failed");
    });
  });

  describe("Input Parameters", () => {
    it("should apply default limit when not provided", async () => {
      mockFirestore.collection().where().where().get.mockResolvedValueOnce({
        docs: [],
      });

      const request = createMockRequest({}, TEST_USERS.ALICE);

      await testCallable(getFriends, request);

      // Should not call limit if no limit provided (uses default)
      expect(mockFirestore.collection().where().where().get).toHaveBeenCalled();
    });

    it("should apply custom limit when provided", async () => {
      mockFirestore.collection().where().where().limit().get.mockResolvedValueOnce({
        docs: [],
      });

      const request = createMockRequest(
        { limit: 10 },
        TEST_USERS.ALICE
      );

      await testCallable(getFriends, request);

      expect(mockFirestore.collection().where().where().limit).toHaveBeenCalledWith(10);
    });
  });
});