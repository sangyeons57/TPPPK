import { rejectFriendRequest } from "../http/rejectRequest";
import { mockFirestore, MockFirestoreHelper, createMockRequest, TEST_USERS, testCallable } from "../../../__tests__/helpers";
import { HttpsError } from "firebase-functions/v2/https";

// Mock admin.firestore
jest.mock("firebase-admin", () => ({
  firestore: () => mockFirestore,
  app: () => ({}),
}));

describe("rejectFriendRequest", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    MockFirestoreHelper.reset();
  });

  describe("Authentication", () => {
    it("should throw error when user is not authenticated", async () => {
      const request = createMockRequest({ requestId: "test-request-id" });

      await expect(testCallable(rejectFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(rejectFriendRequest, request)).rejects.toThrow("User not authenticated");
    });
  });

  describe("Input Validation", () => {
    it("should validate requestId is provided", async () => {
      const request = createMockRequest({}, TEST_USERS.ALICE);

      await expect(testCallable(rejectFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(rejectFriendRequest, request)).rejects.toThrow("Request ID is required");
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

      await expect(testCallable(rejectFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(rejectFriendRequest, request)).rejects.toThrow("Friend request not found");
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

      await expect(testCallable(rejectFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(rejectFriendRequest, request)).rejects.toThrow("Not authorized to reject this request");
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

      await expect(testCallable(rejectFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(rejectFriendRequest, request)).rejects.toThrow("Request is no longer pending");
    });

    it("should throw error when request is already rejected", async () => {
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        exists: true,
        data: () => ({
          requesterId: TEST_USERS.BOB.uid,
          receiverId: TEST_USERS.ALICE.uid,
          status: "rejected", // Already rejected
        }),
      });

      const request = createMockRequest(
        { requestId: "test-request-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(rejectFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(rejectFriendRequest, request)).rejects.toThrow("Request is no longer pending");
    });
  });

  describe("Successful Rejection", () => {
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

    it("should reject friend request successfully", async () => {
      const request = createMockRequest(
        { requestId: "test-request-id" },
        TEST_USERS.ALICE
      );

      const result = await testCallable(rejectFriendRequest, request);

      // Verify request is updated to rejected
      expect(mockFirestore.collection().doc().update).toHaveBeenCalledWith({
        status: "rejected",
        rejectedAt: expect.any(Object),
      });

      expect(result).toEqual({
        message: "Friend request rejected successfully",
        requestId: "test-request-id",
      });
    });

    it("should handle soft delete option", async () => {
      const request = createMockRequest(
        { requestId: "test-request-id", softDelete: true },
        TEST_USERS.ALICE
      );

      const result = await testCallable(rejectFriendRequest, request);

      // Verify request is marked as rejected, not deleted
      expect(mockFirestore.collection().doc().update).toHaveBeenCalledWith({
        status: "rejected",
        rejectedAt: expect.any(Object),
      });

      expect(mockFirestore.collection().doc().delete).not.toHaveBeenCalled();
      expect(result.message).toBe("Friend request rejected successfully");
    });

    it("should handle hard delete option", async () => {
      const request = createMockRequest(
        { requestId: "test-request-id", softDelete: false },
        TEST_USERS.ALICE
      );

      const result = await testCallable(rejectFriendRequest, request);

      // Verify request is deleted
      expect(mockFirestore.collection().doc().delete).toHaveBeenCalled();
      expect(result.message).toBe("Friend request rejected and removed successfully");
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

      await expect(testCallable(rejectFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(rejectFriendRequest, request)).rejects.toThrow("Request already processed");
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

      await expect(testCallable(rejectFriendRequest, request)).rejects.toThrow("Firestore error");
    });

    it("should handle update errors", async () => {
      // Mock valid request
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        exists: true,
        data: () => ({
          requesterId: TEST_USERS.BOB.uid,
          receiverId: TEST_USERS.ALICE.uid,
          status: "pending",
        }),
      });

      // Mock update failure
      mockFirestore.collection().doc().update.mockRejectedValueOnce(
        new Error("Update failed")
      );

      const request = createMockRequest(
        { requestId: "test-request-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(rejectFriendRequest, request)).rejects.toThrow("Update failed");
    });
  });
});