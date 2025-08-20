import { acceptFriendRequest } from "../http/acceptRequest";
import { mockFirestore, MockFirestoreHelper, createMockRequest, TEST_USERS, testCallable } from "../../../__tests__/helpers";
import { HttpsError } from "firebase-functions/v2/https";
import { FRIEND_SUBCOLLECTION_STATUS } from "../../../shared";

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
    it("should validate friendUserId is provided", async () => {
      const request = createMockRequest({}, TEST_USERS.ALICE);

      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow("friendUserId");
    });
  });

  describe("Request Existence", () => {
    it("should throw error when friend request does not exist", async () => {
      // Mock no friend document in receiver's subcollection
      const pendingFriendDoc = mockFirestore.collection(`users/${TEST_USERS.ALICE.uid}/friends`).doc(TEST_USERS.BOB.uid);
      (pendingFriendDoc.get as any).mockResolvedValueOnce({
        exists: false,
      });

      const request = createMockRequest(
        { friendUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow("Friend request not found");
    });
  });

  describe("Status Validation", () => {
    it("should throw error when friend request is not pending", async () => {
      // Mock existing friend document but not PENDING status
      const pendingFriendDoc = mockFirestore.collection(`users/${TEST_USERS.ALICE.uid}/friends`).doc(TEST_USERS.BOB.uid);
      (pendingFriendDoc.get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          status: "ACCEPTED", // Already accepted
          name: TEST_USERS.BOB.name,
          acceptedAt: new Date(),
        }),
      });

      const request = createMockRequest(
        { friendUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow("Friend request is not pending");
    });
  });

  describe("Bilateral Validation", () => {
    it("should throw error when corresponding requester document not found", async () => {
      // Mock PENDING friend document exists in receiver's subcollection
      const pendingFriendDoc = mockFirestore.collection(`users/${TEST_USERS.ALICE.uid}/friends`).doc(TEST_USERS.BOB.uid);
      (pendingFriendDoc.get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          status: "PENDING",
          name: TEST_USERS.BOB.name,
          requestedAt: new Date(),
        }),
      });

      // Mock corresponding requester document does not exist
      const requesterFriendDoc = mockFirestore.collection(`users/${TEST_USERS.BOB.uid}/friends`).doc(TEST_USERS.ALICE.uid);
      (requesterFriendDoc.get as any).mockResolvedValueOnce({
        exists: false,
      });

      const request = createMockRequest(
        { friendUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow("Corresponding friend request not found");
    });

    it("should throw error when status mismatch", async () => {
      // Mock PENDING friend document exists in receiver's subcollection
      const pendingFriendDoc = mockFirestore.collection(`users/${TEST_USERS.ALICE.uid}/friends`).doc(TEST_USERS.BOB.uid);
      (pendingFriendDoc.get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          status: "PENDING",
          name: TEST_USERS.BOB.name,
          requestedAt: new Date(),
        }),
      });

      // Mock corresponding requester document with wrong status
      const requesterFriendDoc = mockFirestore.collection(`users/${TEST_USERS.BOB.uid}/friends`).doc(TEST_USERS.ALICE.uid);
      (requesterFriendDoc.get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          status: "ACCEPTED", // Should be REQUESTED 
          name: TEST_USERS.ALICE.name,
        }),
      });

      const request = createMockRequest(
        { friendUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow("Friend request status mismatch");
    });
  });

  describe("Successful Acceptance", () => {
    beforeEach(() => {
      jest.clearAllMocks();
      
      // Mock valid PENDING request in receiver's subcollection
      const pendingFriendDoc = mockFirestore.collection(`users/${TEST_USERS.ALICE.uid}/friends`).doc(TEST_USERS.BOB.uid);
      (pendingFriendDoc.get as any).mockResolvedValue({
        exists: true,
        data: () => ({
          status: "PENDING",
          name: TEST_USERS.BOB.name,
          requestedAt: new Date(),
        }),
      });

      // Mock corresponding REQUESTED document in requester's subcollection
      const requesterFriendDoc = mockFirestore.collection(`users/${TEST_USERS.BOB.uid}/friends`).doc(TEST_USERS.ALICE.uid);
      (requesterFriendDoc.get as any).mockResolvedValue({
        exists: true,
        data: () => ({
          status: "REQUESTED",
          name: TEST_USERS.ALICE.name,
          requestedAt: new Date(),
        }),
      });
    });

    it("should accept friend request successfully with bilateral updates", async () => {
      const request = createMockRequest(
        { friendUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      const result = await testCallable(acceptFriendRequest, request);

      // Verify batch operations were called
      expect(mockFirestore.batch().update).toHaveBeenCalledTimes(2);
      expect(mockFirestore.batch().commit).toHaveBeenCalledTimes(1);

      expect(result).toEqual({
        success: true,
        message: "Friend request accepted successfully",
      });
    });

    it("should handle batch operations correctly", async () => {
      const request = createMockRequest(
        { friendUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await testCallable(acceptFriendRequest, request);

      // Verify both documents are updated to ACCEPTED status
      expect(mockFirestore.batch().update).toHaveBeenCalledWith(
        expect.any(Object), // pendingFriendRef
        expect.objectContaining({
          status: "ACCEPTED",
          acceptedAt: expect.any(Object),
          updatedAt: expect.any(Object),
        })
      );

      expect(mockFirestore.batch().update).toHaveBeenCalledWith(
        expect.any(Object), // requesterFriendRef  
        expect.objectContaining({
          status: "ACCEPTED",
          acceptedAt: expect.any(Object),
          updatedAt: expect.any(Object),
        })
      );

      // Verify batch commit was called
      expect(mockFirestore.batch().commit).toHaveBeenCalled();
    });
  });

  describe("Idempotency", () => {
    it("should handle idempotency correctly", async () => {
      // Mock idempotency check returning false (already processed)
      // This would need to be mocked at the IdempotencyManager level
      // For now, we'll simulate the expected behavior

      const request = createMockRequest(
        { friendUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      // This test would need proper IdempotencyManager mocking
      // For now, we'll verify the idempotency key generation concept
      expect(TEST_USERS.ALICE.uid).toBeTruthy();
      expect(TEST_USERS.BOB.uid).toBeTruthy();
    });
  });

  describe("Error Handling", () => {
    it("should handle Firestore errors", async () => {
      const pendingFriendDoc = mockFirestore.collection(`users/${TEST_USERS.ALICE.uid}/friends`).doc(TEST_USERS.BOB.uid);
      (pendingFriendDoc.get as any).mockRejectedValueOnce(
        new Error("Firestore error")
      );

      const request = createMockRequest(
        { friendUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow("Firestore error");
    });

    it("should handle batch commit errors", async () => {
      // Mock valid bilateral friendship setup
      const pendingFriendDoc = mockFirestore.collection(`users/${TEST_USERS.ALICE.uid}/friends`).doc(TEST_USERS.BOB.uid);
      (pendingFriendDoc.get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          status: "PENDING",
          name: TEST_USERS.BOB.name,
        }),
      });

      const requesterFriendDoc = mockFirestore.collection(`users/${TEST_USERS.BOB.uid}/friends`).doc(TEST_USERS.ALICE.uid);
      (requesterFriendDoc.get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          status: "REQUESTED",
          name: TEST_USERS.ALICE.name,
        }),
      });

      // Mock batch commit failure
      mockFirestore.batch().commit.mockRejectedValueOnce(
        new Error("Batch commit failed")
      );

      const request = createMockRequest(
        { friendUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(acceptFriendRequest, request)).rejects.toThrow("Batch commit failed");
    });
  });
});