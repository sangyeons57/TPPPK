import { removeFriend } from "../http/removeFriend";
import { mockFirestore, MockFirestoreHelper, createMockRequest, TEST_USERS, testCallable } from "../../../__tests__/helpers";
import { HttpsError } from "firebase-functions/v2/https";

// Mock admin.firestore
jest.mock("firebase-admin", () => ({
  firestore: () => mockFirestore,
  app: () => ({}),
}));

describe("removeFriend", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    MockFirestoreHelper.reset();
  });

  describe("Authentication", () => {
    it("should throw error when user is not authenticated", async () => {
      const request = createMockRequest({ friendId: TEST_USERS.BOB.uid });

      await expect(testCallable(removeFriend, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(removeFriend, request)).rejects.toThrow("User not authenticated");
    });
  });

  describe("Input Validation", () => {
    it("should validate friendId is provided", async () => {
      const request = createMockRequest({}, TEST_USERS.ALICE);

      await expect(testCallable(removeFriend, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(removeFriend, request)).rejects.toThrow("Friend ID is required");
    });

    it("should prevent removing self as friend", async () => {
      const request = createMockRequest(
        { friendId: TEST_USERS.ALICE.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(removeFriend, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(removeFriend, request)).rejects.toThrow("Cannot remove yourself as friend");
    });
  });

  describe("Friendship Existence", () => {
    it("should throw error when friendship does not exist", async () => {
      // Mock no friendship found
      mockFirestore.collection().where().where().where().limit().get.mockResolvedValueOnce({
        empty: true,
        docs: [],
      });

      const request = createMockRequest(
        { friendId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(removeFriend, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(removeFriend, request)).rejects.toThrow("Friendship not found");
    });

    it("should throw error when friendship is not accepted", async () => {
      // Mock pending friendship
      mockFirestore.collection().where().where().where().limit().get.mockResolvedValueOnce({
        empty: false,
        docs: [{
          id: "friendship1",
          data: () => ({
            requesterId: TEST_USERS.ALICE.uid,
            receiverId: TEST_USERS.BOB.uid,
            status: "pending",
          }),
        }],
      });

      const request = createMockRequest(
        { friendId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(removeFriend, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(removeFriend, request)).rejects.toThrow("Users are not friends");
    });
  });

  describe("Successful Removal", () => {
    beforeEach(() => {
      // Mock existing accepted friendship
      mockFirestore.collection().where().where().where().limit().get.mockResolvedValueOnce({
        empty: false,
        docs: [{
          id: "friendship1",
          data: () => ({
            requesterId: TEST_USERS.ALICE.uid,
            receiverId: TEST_USERS.BOB.uid,
            status: "accepted",
            acceptedAt: { toDate: () => new Date("2024-01-01") },
          }),
        }],
      });
    });

    it("should remove friendship successfully", async () => {
      const request = createMockRequest(
        { friendId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      const result = await testCallable(removeFriend, request);

      // Verify friendship document is deleted
      expect(mockFirestore.collection().doc().delete).toHaveBeenCalledWith();

      expect(result).toEqual({
        message: "Friend removed successfully",
        removedFriendId: TEST_USERS.BOB.uid,
        friendshipId: "friendship1",
      });
    });

    it("should handle friendship where user is receiver", async () => {
      // Mock friendship where user is receiver
      mockFirestore.collection().where().where().where().limit().get.mockResolvedValueOnce({
        empty: false,
        docs: [{
          id: "friendship2",
          data: () => ({
            requesterId: TEST_USERS.BOB.uid,
            receiverId: TEST_USERS.ALICE.uid, // Alice is receiver
            status: "accepted",
            acceptedAt: { toDate: () => new Date("2024-01-01") },
          }),
        }],
      });

      const request = createMockRequest(
        { friendId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      const result = await testCallable(removeFriend, request);

      expect(mockFirestore.collection().doc().delete).toHaveBeenCalled();
      expect(result.removedFriendId).toBe(TEST_USERS.BOB.uid);
      expect(result.friendshipId).toBe("friendship2");
    });

    it("should handle soft removal when specified", async () => {
      const request = createMockRequest(
        { friendId: TEST_USERS.BOB.uid, softDelete: true },
        TEST_USERS.ALICE
      );

      const result = await testCallable(removeFriend, request);

      // Verify friendship is updated, not deleted
      expect(mockFirestore.collection().doc().update).toHaveBeenCalledWith({
        status: "removed",
        removedAt: expect.any(Object),
        removedBy: TEST_USERS.ALICE.uid,
      });

      expect(mockFirestore.collection().doc().delete).not.toHaveBeenCalled();
      expect(result.message).toBe("Friend removed successfully");
    });
  });

  describe("Batch Operations", () => {
    beforeEach(() => {
      // Mock existing friendship
      mockFirestore.collection().where().where().where().limit().get.mockResolvedValueOnce({
        empty: false,
        docs: [{
          id: "friendship1",
          data: () => ({
            requesterId: TEST_USERS.ALICE.uid,
            receiverId: TEST_USERS.BOB.uid,
            status: "accepted",
          }),
        }],
      });
    });

    it("should clean up related data in batch", async () => {
      const request = createMockRequest(
        { friendId: TEST_USERS.BOB.uid, cleanupRelatedData: true },
        TEST_USERS.ALICE
      );

      await testCallable(removeFriend, request);

      // Verify batch operations
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
        { friendId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(removeFriend, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(removeFriend, request)).rejects.toThrow("Request already processed");
    });
  });

  describe("Error Handling", () => {
    it("should handle Firestore query errors", async () => {
      mockFirestore.collection().where().where().where().limit().get.mockRejectedValueOnce(
        new Error("Query failed")
      );

      const request = createMockRequest(
        { friendId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(removeFriend, request)).rejects.toThrow("Query failed");
    });

    it("should handle delete errors", async () => {
      // Mock existing friendship
      mockFirestore.collection().where().where().where().limit().get.mockResolvedValueOnce({
        empty: false,
        docs: [{
          id: "friendship1",
          data: () => ({
            requesterId: TEST_USERS.ALICE.uid,
            receiverId: TEST_USERS.BOB.uid,
            status: "accepted",
          }),
        }],
      });

      // Mock delete failure
      mockFirestore.collection().doc().delete.mockRejectedValueOnce(
        new Error("Delete failed")
      );

      const request = createMockRequest(
        { friendId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(removeFriend, request)).rejects.toThrow("Delete failed");
    });

    it("should handle batch commit errors", async () => {
      // Mock existing friendship
      mockFirestore.collection().where().where().where().limit().get.mockResolvedValueOnce({
        empty: false,
        docs: [{
          id: "friendship1",
          data: () => ({
            requesterId: TEST_USERS.ALICE.uid,
            receiverId: TEST_USERS.BOB.uid,
            status: "accepted",
          }),
        }],
      });

      // Mock batch commit failure
      mockFirestore.batch().commit.mockRejectedValueOnce(
        new Error("Batch commit failed")
      );

      const request = createMockRequest(
        { friendId: TEST_USERS.BOB.uid, cleanupRelatedData: true },
        TEST_USERS.ALICE
      );

      await expect(testCallable(removeFriend, request)).rejects.toThrow("Batch commit failed");
    });
  });

  describe("Query Construction", () => {
    it("should query for friendship in both directions", async () => {
      mockFirestore.collection().where().where().where().limit().get.mockResolvedValueOnce({
        empty: true,
        docs: [],
      });

      const request = createMockRequest(
        { friendId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(removeFriend, request)).rejects.toThrow("Friendship not found");

      // Verify the query construction for bidirectional friendship search
      expect(mockFirestore.collection).toHaveBeenCalledWith("friends");
      expect(mockFirestore.collection().where).toHaveBeenCalledWith("status", "==", "accepted");
    });
  });
});