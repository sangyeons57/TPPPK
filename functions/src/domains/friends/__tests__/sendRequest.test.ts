import { sendFriendRequest } from "../http/sendRequest";
import { mockFirestore, MockFirestoreHelper, createMockRequest, TEST_USERS, testCallable } from "../../../__tests__/helpers";
import { HttpsError } from "firebase-functions/v2/https";

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
      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow("Receiver user ID is required");
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
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: false,
      });

      const request = createMockRequest(
        { receiverUserId: "non-existent-user" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow("Receiver user not found");
    });
  });

  describe("Friendship Status Checks", () => {
    beforeEach(() => {
      // Mock receiver user exists
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: TEST_USERS.BOB.name,
          email: TEST_USERS.BOB.email,
        }),
      });
    });

    it("should throw error when users are already friends", async () => {
      // Mock existing friendship
      (mockFirestore.collection().where().where().limit().get as any).mockResolvedValueOnce({
        empty: false,
        docs: [{ 
          data: () => ({
            status: "accepted",
            requesterId: TEST_USERS.ALICE.uid,
            receiverId: TEST_USERS.BOB.uid,
          })
        }],
      });

      const request = createMockRequest(
        { receiverUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow("Users are already friends");
    });

    it("should throw error when pending request already exists", async () => {
      // Mock no existing friendship
      (mockFirestore.collection().where().where().limit().get as any).mockResolvedValueOnce({
        empty: true,
      });

      // Mock existing pending request
      (mockFirestore.collection().where().where().where().limit().get as any).mockResolvedValueOnce({
        empty: false,
        docs: [{ 
          data: () => ({
            status: "pending",
            requesterId: TEST_USERS.ALICE.uid,
            receiverId: TEST_USERS.BOB.uid,
          })
        }],
      });

      const request = createMockRequest(
        { receiverUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow("Friend request already exists");
    });
  });

  describe("Successful Request", () => {
    beforeEach(() => {
      // Mock receiver user exists
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: TEST_USERS.BOB.name,
          email: TEST_USERS.BOB.email,
        }),
      });

      // Mock no existing friendship
      (mockFirestore.collection().where().where().limit().get as any).mockResolvedValueOnce({
        empty: true,
      });

      // Mock no existing pending request
      (mockFirestore.collection().where().where().where().limit().get as any).mockResolvedValueOnce({
        empty: true,
      });
    });

    it("should create friend request successfully", async () => {
      const request = createMockRequest(
        { receiverUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      const result = await testCallable(sendFriendRequest, request);

      expect(mockFirestore.collection().doc().set as any).toHaveBeenCalledWith({
        id: expect.any(String),
        userId: TEST_USERS.ALICE.uid,
        friendId: TEST_USERS.BOB.uid,
        status: "pending",
        createdAt: expect.any(Object),
        updatedAt: expect.any(Object),
      });

      expect(result).toEqual({
        success: true,
        friendRequestId: expect.any(String),
      });
    });

    it("should handle idempotency correctly", async () => {
      // Mock idempotency key already processed
      mockFirestore.collection().doc().get
        .mockResolvedValueOnce({
          exists: true,
          data: () => ({ status: "completed" }),
        });

      const request = createMockRequest(
        { receiverUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(sendFriendRequest, request)).rejects.toThrow("Request already processed");
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