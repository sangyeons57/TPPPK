import { acceptFriendRequest } from "../http/acceptRequest";
import { mockFirestore, MockFirestoreHelper, createMockRequest, TEST_USERS, testCallable } from "../../../__tests__/helpers";
import { HttpsError } from "firebase-functions/v2/https";

// Mock admin.firestore
jest.mock("firebase-admin", () => ({
  firestore: () => mockFirestore,
  app: () => ({}),
}));

describe("acceptFriendRequest", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    MockFirestoreHelper.reset();
  });

  describe("Authentication", () => {
    it("should throw error when user is not authenticated", async () => {
      const request = createMockRequest({ requestId: "test-request-id" });

      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow("User not authenticated");
    });
  });

  describe("Input Validation", () => {
    it("should validate requestId is provided", async () => {
      const request = createMockRequest({}, TEST_USERS.ALICE);

      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow("Request ID is required");
    });
  });

  describe("Request Existence", () => {
    it("should throw error when request does not exist", async () => {
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        exists: false,
      });

      const request = createMockRequest(
        { requestId: "non-existent-request" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow("Friend request not found");
    });
  });

  describe("Authorization", () => {
    it("should throw error when user is not the receiver", async () => {
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        exists: true,
        data: () => ({
          requesterId: TEST_USERS.BOB.uid,
          receiverId: TEST_USERS.CHARLIE.uid, // Different from authenticated user
          status: "pending",
        }),
      });

      const request = createMockRequest(
        { requestId: "test-request-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow("Not authorized to accept this request");
    });
  });

  describe("Request Status", () => {
    it("should throw error when request is not pending", async () => {
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        exists: true,
        data: () => ({
          requesterId: TEST_USERS.BOB.uid,
          receiverId: TEST_USERS.ALICE.uid,
          status: "accepted", // Already accepted
        }),
      });

      const request = createMockRequest(
        { requestId: "test-request-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow("Request is no longer pending");
    });

    it("should throw error when request is rejected", async () => {
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        exists: true,
        data: () => ({
          requesterId: TEST_USERS.BOB.uid,
          receiverId: TEST_USERS.ALICE.uid,
          status: "rejected",
        }),
      });

      const request = createMockRequest(
        { requestId: "test-request-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow("Request is no longer pending");
    });
  });

  describe("Successful Acceptance", () => {
    beforeEach(() => {
      // Mock valid pending request
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        exists: true,
        data: () => ({
          requesterId: TEST_USERS.BOB.uid,
          receiverId: TEST_USERS.ALICE.uid,
          status: "pending",
          createdAt: { toDate: () => new Date() },
        }),
      });
    });

    it("should accept friend request successfully", async () => {
      const request = createMockRequest(
        { requestId: "test-request-id" },
        TEST_USERS.ALICE
      );

      const result = await testCallable(acceptFriendRequest, request);

      // Verify request is updated to accepted
      expect(mockFirestore.collection().doc().update).toHaveBeenCalledWith({
        status: "accepted",
        acceptedAt: expect.any(Object),
      });

      expect(result).toEqual({
        message: "Friend request accepted successfully",
        friendship: {
          friendId: TEST_USERS.BOB.uid,
          status: "accepted",
          acceptedAt: expect.any(String),
        },
      });
    });

    it("should handle batch operations correctly", async () => {
      const request = createMockRequest(
        { requestId: "test-request-id" },
        TEST_USERS.ALICE
      );

      await acceptFriendRequest(request);

      // Verify batch commit was called
      expect(mockFirestore.batch().commit).toHaveBeenCalled();
    });
  });

  describe("Idempotency", () => {
    it("should handle idempotency correctly", async () => {
      // Mock idempotency key already processed
      mockFirestore.collection().doc().get
        .mockResolvedValueOnce({
          exists: true,
          data: () => ({ status: "completed" }),
        });

      const request = createMockRequest(
        { requestId: "test-request-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow("Request already processed");
    });
  });

  describe("Error Handling", () => {
    it("should handle Firestore errors", async () => {
      mockFirestore.collection().doc().get.mockRejectedValueOnce(
        new Error("Firestore error")
      );

      const request = createMockRequest(
        { requestId: "test-request-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow("Firestore error");
    });

    it("should handle batch commit errors", async () => {
      // Mock valid request
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        exists: true,
        data: () => ({
          requesterId: TEST_USERS.BOB.uid,
          receiverId: TEST_USERS.ALICE.uid,
          status: "pending",
        }),
      });

      // Mock batch commit failure
      mockFirestore.batch().commit.mockRejectedValueOnce(
        new Error("Batch commit failed")
      );

      const request = createMockRequest(
        { requestId: "test-request-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow("Batch commit failed");
    });
  });
});