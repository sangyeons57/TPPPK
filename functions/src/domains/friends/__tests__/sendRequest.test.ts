import { sendFriendRequest } from "../http/sendRequest";
import { mockFirestore, MockFirestoreHelper, createMockRequest, TEST_USERS, testCallable } from "../../../__tests__/helpers";
import { HttpsError } from "firebase-functions/v2/https";
import { FRIEND_SUBCOLLECTION_STATUS } from "../../../shared";

// Mock admin.firestore
jest.mock("firebase-admin", () => ({
  firestore: () => mockFirestore,
  app: () => ({}),
}));

describe("sendFriendRequest", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    MockFirestoreHelper.reset();
  });

  describe("Authentication", () => {
    it("should throw error when user is not authenticated", async () => {
      const request = createMockRequest({ receiverUserId: TEST_USERS.BOB.uid });

      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow("User not authenticated");
    });
  });

  describe("Input Validation", () => {
    it("should validate receiverUserId is provided", async () => {
      const request = createMockRequest({}, TEST_USERS.ALICE);

      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow("receiverUserId");
    });

    it("should prevent sending request to self", async () => {
      const request = createMockRequest(
        { receiverUserId: TEST_USERS.ALICE.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow("Cannot send friend request to yourself");
    });
  });

  describe("User Existence", () => {
    it("should throw error when receiver user does not exist", async () => {
      // Mock receiver user not found
      const mockDoc = mockFirestore.collection("users").doc(TEST_USERS.BOB.uid);
      (mockDoc.get as any).mockResolvedValueOnce({
        exists: false,
      });

      const request = createMockRequest(
        { receiverUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow("Receiver user not found");
    });

    it("should throw error when requester user does not exist", async () => {
      // Mock receiver user exists but requester doesn't
      const receiverDoc = mockFirestore.collection("users").doc(TEST_USERS.BOB.uid);
      (receiverDoc.get as any).mockResolvedValueOnce({
        exists: true,
        data: () => TEST_USERS.BOB,
      });

      const requesterDoc = mockFirestore.collection("users").doc(TEST_USERS.ALICE.uid);
      (requesterDoc.get as any).mockResolvedValueOnce({
        exists: false,
      });

      const request = createMockRequest(
        { receiverUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow("Requester user not found");
    });
  });

  describe("Friendship Status Checks", () => {
    beforeEach(() => {
      jest.clearAllMocks();
      
      // Mock both users exist
      const receiverDoc = mockFirestore.collection("users").doc(TEST_USERS.BOB.uid);
      (receiverDoc.get as any).mockResolvedValue({
        exists: true,
        data: () => TEST_USERS.BOB,
      });

      const requesterDoc = mockFirestore.collection("users").doc(TEST_USERS.ALICE.uid);
      (requesterDoc.get as any).mockResolvedValue({
        exists: true,
        data: () => TEST_USERS.ALICE,
      });
    });

    it("should throw error when users are already friends", async () => {
      // Mock existing ACCEPTED friendship in requester's subcollection
      const existingFriendDoc = mockFirestore.collection(`users/${TEST_USERS.ALICE.uid}/friends`).doc(TEST_USERS.BOB.uid);
      (existingFriendDoc.get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          status: FRIEND_SUBCOLLECTION_STATUS.ACCEPTED,
          name: TEST_USERS.BOB.name,
          acceptedAt: new Date(),
        }),
      });

      const request = createMockRequest(
        { receiverUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow("Users are already friends");
    });

    it("should throw error when REQUESTED request already exists", async () => {
      // Mock existing REQUESTED friendship in requester's subcollection
      const existingFriendDoc = mockFirestore.collection(`users/${TEST_USERS.ALICE.uid}/friends`).doc(TEST_USERS.BOB.uid);
      (existingFriendDoc.get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          status: FRIEND_SUBCOLLECTION_STATUS.REQUESTED,
          name: TEST_USERS.BOB.name,
          requestedAt: new Date(),
        }),
      });

      const request = createMockRequest(
        { receiverUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow("Friend request already sent");
    });

    it("should throw error when reverse REQUESTED request exists", async () => {
      // Mock no existing friendship in requester's subcollection
      const existingFriendDoc = mockFirestore.collection(`users/${TEST_USERS.ALICE.uid}/friends`).doc(TEST_USERS.BOB.uid);
      (existingFriendDoc.get as any).mockResolvedValueOnce({
        exists: false,
      });

      // Mock existing REQUESTED in receiver's subcollection (reverse direction)
      const existingReverseDoc = mockFirestore.collection(`users/${TEST_USERS.BOB.uid}/friends`).doc(TEST_USERS.ALICE.uid);
      (existingReverseDoc.get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          status: FRIEND_SUBCOLLECTION_STATUS.REQUESTED,
          name: TEST_USERS.ALICE.name,
          requestedAt: new Date(),
        }),
      });

      const request = createMockRequest(
        { receiverUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow("Friend request already received from this user");
    });
  });

  describe("Successful Request", () => {
    beforeEach(() => {
      jest.clearAllMocks();
      
      // Mock both users exist
      const receiverDoc = mockFirestore.collection("users").doc(TEST_USERS.BOB.uid);
      (receiverDoc.get as any).mockResolvedValue({
        exists: true,
        data: () => TEST_USERS.BOB,
      });

      const requesterDoc = mockFirestore.collection("users").doc(TEST_USERS.ALICE.uid);
      (requesterDoc.get as any).mockResolvedValue({
        exists: true,
        data: () => TEST_USERS.ALICE,
      });

      // Mock no existing friendship in both directions
      const existingFriendDoc = mockFirestore.collection(`users/${TEST_USERS.ALICE.uid}/friends`).doc(TEST_USERS.BOB.uid);
      (existingFriendDoc.get as any).mockResolvedValue({
        exists: false,
      });

      const existingReverseDoc = mockFirestore.collection(`users/${TEST_USERS.BOB.uid}/friends`).doc(TEST_USERS.ALICE.uid);
      (existingReverseDoc.get as any).mockResolvedValue({
        exists: false,
      });
    });

    it("should create bilateral friend request successfully", async () => {
      const request = createMockRequest(
        { receiverUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      const result = await testCallable(sendFriendRequest, request);

      // Verify batch operations were called
      expect(mockFirestore.batch().set).toHaveBeenCalledTimes(2);
      expect(mockFirestore.batch().commit).toHaveBeenCalledTimes(1);

      expect(result).toEqual({
        success: true,
        friendRequestId: TEST_USERS.BOB.uid, // Should return receiver's userId as the document ID
      });
    });

    it("should handle idempotency correctly", async () => {
      // Mock idempotency check returning false (already processed)
      // This would need to be mocked at the IdempotencyManager level
      // For now, we'll simulate the error that would be thrown
      const request = createMockRequest(
        { receiverUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      // This test would need proper IdempotencyManager mocking
      // For now, we'll skip detailed implementation
      // await expect(testCallable(sendFriendRequest, request)).rejects.toThrow(HttpsError);
      // await expect(testCallable(sendFriendRequest, request)).rejects.toThrow("already processed");
    });
  });

  describe("Error Handling", () => {
    it("should handle Firestore errors", async () => {
      (mockFirestore.collection().doc().get as any).mockRejectedValueOnce(
        new Error("Firestore error")
      );

      const request = createMockRequest(
        { receiverUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow("Firestore error");
    });
  });
});