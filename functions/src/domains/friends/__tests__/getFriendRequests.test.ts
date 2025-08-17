import { getFriendRequests } from "../http/getFriendRequests";
import { mockFirestore, MockFirestoreHelper, createMockRequest, TEST_USERS, testCallable } from "../../../__tests__/helpers";
import { HttpsError } from "firebase-functions/v2/https";

// Mock admin.firestore
jest.mock("firebase-admin", () => ({
  firestore: () => mockFirestore,
  app: () => ({}),
}));

describe("getFriendRequests", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    MockFirestoreHelper.reset();
  });

  describe("Authentication", () => {
    it("should throw error when user is not authenticated", async () => {
      const request = createMockRequest({});

      await expect(testCallable(getFriendRequests, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(getFriendRequests, request)).rejects.toThrow("User not authenticated");
    });
  });

  describe("Successful Retrieval", () => {
    it("should return incoming friend requests by default", async () => {
      const mockRequests = [
        {
          id: "request1",
          data: () => ({
            requesterId: TEST_USERS.BOB.uid,
            receiverId: TEST_USERS.ALICE.uid,
            status: "pending",
            createdAt: { toDate: () => new Date("2024-01-01") },
          }),
        },
        {
          id: "request2",
          data: () => ({
            requesterId: TEST_USERS.CHARLIE.uid,
            receiverId: TEST_USERS.ALICE.uid,
            status: "pending",
            createdAt: { toDate: () => new Date("2024-01-02") },
          }),
        },
      ];

      // Mock incoming requests query
      mockFirestore.collection().where().where().orderBy().get.mockResolvedValueOnce({
        docs: mockRequests,
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

      const result = await testCallable(getFriendRequests, request);

      expect(result).toEqual({
        friendRequests: [
          {
            id: "request1",
            requesterId: TEST_USERS.BOB.uid,
            requesterName: TEST_USERS.BOB.name,
            requesterEmail: TEST_USERS.BOB.email,
            requesterProfileImageUrl: TEST_USERS.BOB.profileImageUrl,
            status: "pending",
            type: "incoming",
            createdAt: new Date("2024-01-01").toISOString(),
          },
          {
            id: "request2",
            requesterId: TEST_USERS.CHARLIE.uid,
            requesterName: TEST_USERS.CHARLIE.name,
            requesterEmail: TEST_USERS.CHARLIE.email,
            requesterProfileImageUrl: TEST_USERS.CHARLIE.profileImageUrl,
            status: "pending",
            type: "incoming",
            createdAt: new Date("2024-01-02").toISOString(),
          },
        ],
        total: 2,
        type: "incoming",
      });
    });

    it("should return outgoing friend requests when specified", async () => {
      const mockRequests = [
        {
          id: "request1",
          data: () => ({
            requesterId: TEST_USERS.ALICE.uid,
            receiverId: TEST_USERS.BOB.uid,
            status: "pending",
            createdAt: { toDate: () => new Date("2024-01-01") },
          }),
        },
      ];

      // Mock outgoing requests query
      mockFirestore.collection().where().where().orderBy().get.mockResolvedValueOnce({
        docs: mockRequests,
      });

      // Mock user data fetch
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: TEST_USERS.BOB.name,
          email: TEST_USERS.BOB.email,
          profileImageUrl: TEST_USERS.BOB.profileImageUrl,
        }),
      });

      const request = createMockRequest(
        { type: "outgoing" },
        TEST_USERS.ALICE
      );

      const result = await testCallable(getFriendRequests, request);

      expect(result).toEqual({
        friendRequests: [
          {
            id: "request1",
            receiverId: TEST_USERS.BOB.uid,
            receiverName: TEST_USERS.BOB.name,
            receiverEmail: TEST_USERS.BOB.email,
            receiverProfileImageUrl: TEST_USERS.BOB.profileImageUrl,
            status: "pending",
            type: "outgoing",
            createdAt: new Date("2024-01-01").toISOString(),
          },
        ],
        total: 1,
        type: "outgoing",
      });
    });

    it("should return both incoming and outgoing requests", async () => {
      const mockIncoming = [
        {
          id: "incoming1",
          data: () => ({
            requesterId: TEST_USERS.BOB.uid,
            receiverId: TEST_USERS.ALICE.uid,
            status: "pending",
            createdAt: { toDate: () => new Date("2024-01-01") },
          }),
        },
      ];

      const mockOutgoing = [
        {
          id: "outgoing1",
          data: () => ({
            requesterId: TEST_USERS.ALICE.uid,
            receiverId: TEST_USERS.CHARLIE.uid,
            status: "pending",
            createdAt: { toDate: () => new Date("2024-01-02") },
          }),
        },
      ];

      // Mock both queries
      mockFirestore.collection().where().where().orderBy().get
        .mockResolvedValueOnce({ docs: mockIncoming })  // incoming
        .mockResolvedValueOnce({ docs: mockOutgoing }); // outgoing

      // Mock user data fetches
      mockFirestore.collection().doc().get
        .mockResolvedValueOnce({
          exists: true,
          data: () => ({ name: TEST_USERS.BOB.name, email: TEST_USERS.BOB.email }),
        })
        .mockResolvedValueOnce({
          exists: true,
          data: () => ({ name: TEST_USERS.CHARLIE.name, email: TEST_USERS.CHARLIE.email }),
        });

      const request = createMockRequest(
        { type: "both" },
        TEST_USERS.ALICE
      );

      const result = await testCallable(getFriendRequests, request);

      expect(result.friendRequests).toHaveLength(2);
      expect(result.total).toBe(2);
      expect(result.type).toBe("both");
    });

    it("should return empty list when no requests", async () => {
      mockFirestore.collection().where().where().orderBy().get.mockResolvedValueOnce({
        docs: [],
      });

      const request = createMockRequest({}, TEST_USERS.ALICE);

      const result = await testCallable(getFriendRequests, request);

      expect(result).toEqual({
        friendRequests: [],
        total: 0,
        type: "incoming",
      });
    });
  });

  describe("Status Filtering", () => {
    it("should filter by pending status", async () => {
      const request = createMockRequest(
        { status: "pending" },
        TEST_USERS.ALICE
      );

      mockFirestore.collection().where().where().orderBy().get.mockResolvedValueOnce({
        docs: [],
      });

      await testCallable(getFriendRequests, request);

      expect(mockFirestore.collection().where).toHaveBeenCalledWith("receiverId", "==", TEST_USERS.ALICE.uid);
      expect(mockFirestore.collection().where).toHaveBeenCalledWith("status", "==", "pending");
    });

    it("should filter by accepted status", async () => {
      const request = createMockRequest(
        { status: "accepted" },
        TEST_USERS.ALICE
      );

      mockFirestore.collection().where().where().orderBy().get.mockResolvedValueOnce({
        docs: [],
      });

      await testCallable(getFriendRequests, request);

      expect(mockFirestore.collection().where).toHaveBeenCalledWith("status", "==", "accepted");
    });

    it("should filter by rejected status", async () => {
      const request = createMockRequest(
        { status: "rejected" },
        TEST_USERS.ALICE
      );

      mockFirestore.collection().where().where().orderBy().get.mockResolvedValueOnce({
        docs: [],
      });

      await testCallable(getFriendRequests, request);

      expect(mockFirestore.collection().where).toHaveBeenCalledWith("status", "==", "rejected");
    });
  });

  describe("Pagination", () => {
    it("should apply limit when provided", async () => {
      mockFirestore.collection().where().where().orderBy().limit().get.mockResolvedValueOnce({
        docs: [],
      });

      const request = createMockRequest(
        { limit: 5 },
        TEST_USERS.ALICE
      );

      await testCallable(getFriendRequests, request);

      expect(mockFirestore.collection().where().where().orderBy().limit).toHaveBeenCalledWith(5);
    });

    it("should apply offset when provided", async () => {
      mockFirestore.collection().where().where().orderBy().offset().get.mockResolvedValueOnce({
        docs: [],
      });

      const request = createMockRequest(
        { offset: 10 },
        TEST_USERS.ALICE
      );

      await testCallable(getFriendRequests, request);

      expect(mockFirestore.collection().where().where().orderBy().offset).toHaveBeenCalledWith(10);
    });
  });

  describe("User Data Handling", () => {
    it("should handle missing requester user data", async () => {
      const mockRequests = [
        {
          id: "request1",
          data: () => ({
            requesterId: "deleted-user",
            receiverId: TEST_USERS.ALICE.uid,
            status: "pending",
            createdAt: { toDate: () => new Date("2024-01-01") },
          }),
        },
      ];

      mockFirestore.collection().where().where().orderBy().get.mockResolvedValueOnce({
        docs: mockRequests,
      });

      // Mock user not found
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        exists: false,
      });

      const request = createMockRequest({}, TEST_USERS.ALICE);

      const result = await testCallable(getFriendRequests, request);

      // Should filter out requests with missing user data
      expect(result.friendRequests).toHaveLength(0);
    });
  });

  describe("Error Handling", () => {
    it("should handle Firestore query errors", async () => {
      mockFirestore.collection().where().where().orderBy().get.mockRejectedValueOnce(
        new Error("Query failed")
      );

      const request = createMockRequest({}, TEST_USERS.ALICE);

      await expect(testCallable(getFriendRequests, request)).rejects.toThrow("Query failed");
    });

    it("should handle user data fetch errors", async () => {
      const mockRequests = [
        {
          id: "request1",
          data: () => ({
            requesterId: TEST_USERS.BOB.uid,
            receiverId: TEST_USERS.ALICE.uid,
            status: "pending",
            createdAt: { toDate: () => new Date("2024-01-01") },
          }),
        },
      ];

      mockFirestore.collection().where().where().orderBy().get.mockResolvedValueOnce({
        docs: mockRequests,
      });

      // Mock user data fetch error
      mockFirestore.collection().doc().get.mockRejectedValueOnce(
        new Error("User fetch failed")
      );

      const request = createMockRequest({}, TEST_USERS.ALICE);

      await expect(testCallable(getFriendRequests, request)).rejects.toThrow("User fetch failed");
    });
  });

  describe("Input Validation", () => {
    it("should handle invalid type parameter", async () => {
      const request = createMockRequest(
        { type: "invalid" as any },
        TEST_USERS.ALICE
      );

      mockFirestore.collection().where().where().orderBy().get.mockResolvedValueOnce({
        docs: [],
      });

      // Should default to incoming
      const result = await testCallable(getFriendRequests, request);
      expect(result.type).toBe("incoming");
    });

    it("should handle invalid status parameter", async () => {
      const request = createMockRequest(
        { status: "invalid" as any },
        TEST_USERS.ALICE
      );

      mockFirestore.collection().where().where().orderBy().get.mockResolvedValueOnce({
        docs: [],
      });

      // Should not add status filter for invalid status
      await testCallable(getFriendRequests, request);
      
      // Verify only receiverId filter is applied, not status
      expect(mockFirestore.collection().where).toHaveBeenCalledWith("receiverId", "==", TEST_USERS.ALICE.uid);
    });
  });
});