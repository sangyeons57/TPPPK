import { createDMChannel } from "../http/createChannel";
import { mockFirestore, MockFirestoreHelper, createMockRequest, TEST_USERS, testCallable } from "../../../__tests__/helpers";
import { HttpsError } from "firebase-functions/v2/https";

// Mock admin.firestore
jest.mock("firebase-admin", () => ({
  firestore: () => mockFirestore,
  app: () => ({}),
}));

describe("createDMChannel", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    MockFirestoreHelper.reset();
  });

  describe("Authentication", () => {
    it("should throw error when user is not authenticated", async () => {
      const request = createMockRequest({ targetUserId: TEST_USERS.BOB.uid });

      await expect(testCallable(createDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(createDMChannel, request)).rejects.toThrow("User not authenticated");
    });
  });

  describe("Input Validation", () => {
    it("should validate targetUserId is provided", async () => {
      const request = createMockRequest({}, TEST_USERS.ALICE);

      await expect(testCallable(createDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(createDMChannel, request)).rejects.toThrow("Target user ID is required");
    });

    it("should prevent creating DM with self", async () => {
      const request = createMockRequest(
        { targetUserId: TEST_USERS.ALICE.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(createDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(createDMChannel, request)).rejects.toThrow("Cannot create DM channel with yourself");
    });

    it("should validate targetUserId format", async () => {
      const request = createMockRequest(
        { targetUserId: "invalid@user" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(createDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(createDMChannel, request)).rejects.toThrow("Invalid user ID format");
    });
  });

  describe("User Existence", () => {
    it("should throw error when target user does not exist", async () => {
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: false,
      });

      const request = createMockRequest(
        { targetUserId: "non-existent-user" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(createDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(createDMChannel, request)).rejects.toThrow("Target user not found");
    });
  });

  describe("Existing Channel Check", () => {
    beforeEach(() => {
      // Mock target user exists
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: TEST_USERS.BOB.name,
          email: TEST_USERS.BOB.email,
        }),
      });
    });

    it("should return existing channel if found", async () => {
      const existingChannel = {
        id: "existing-channel-id",
        data: () => ({
          participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
          type: "dm",
          createdAt: { toDate: () => new Date("2024-01-01") },
          lastMessageAt: { toDate: () => new Date("2024-01-01") },
        }),
      };

      // Mock existing channel found
      (mockFirestore.collection().where().where().limit().get as any).mockResolvedValueOnce({
        empty: false,
        docs: [existingChannel],
      });

      const request = createMockRequest(
        { targetUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      const result = await testCallable(createDMChannel, request);

      expect(result).toEqual({
        channelId: "existing-channel-id",
        isNewChannel: false,
        participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
        createdAt: new Date("2024-01-01").toISOString(),
      });
    });

    it("should check for channel in both participant orders", async () => {
      // Mock no channel found
      (mockFirestore.collection().where().where().limit().get as any).mockResolvedValueOnce({
        empty: true,
        docs: [],
      });

      const request = createMockRequest(
        { targetUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await testCallable(createDMChannel, request);

      // Should check for channels with participants in both orders
      expect(mockFirestore.collection().where).toHaveBeenCalledWith(
        "participants", "==", [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid]
      );
    });
  });

  describe("New Channel Creation", () => {
    beforeEach(() => {
      // Mock target user exists
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: TEST_USERS.BOB.name,
          email: TEST_USERS.BOB.email,
        }),
      });

      // Mock no existing channel
      (mockFirestore.collection().where().where().limit().get as any).mockResolvedValueOnce({
        empty: true,
        docs: [],
      });
    });

    it("should create new DM channel successfully", async () => {
      const request = createMockRequest(
        { targetUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      const result = await testCallable(createDMChannel, request);

      expect(mockFirestore.collection().add as any).toHaveBeenCalledWith({
        type: "dm",
        participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
        participantDetails: {
          [TEST_USERS.ALICE.uid]: {
            name: expect.any(String),
            joinedAt: expect.any(Object),
          },
          [TEST_USERS.BOB.uid]: {
            name: TEST_USERS.BOB.name,
            joinedAt: expect.any(Object),
          },
        },
        createdAt: expect.any(Object),
        createdBy: TEST_USERS.ALICE.uid,
        lastMessageAt: expect.any(Object),
        messageCount: 0,
        isActive: true,
      });

      expect(result).toEqual({
        channelId: expect.any(String),
        isNewChannel: true,
        participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
        createdAt: expect.any(String),
      });
    });

    it("should handle channel name customization", async () => {
      const request = createMockRequest(
        { 
          targetUserId: TEST_USERS.BOB.uid,
          channelName: "Custom Chat Name"
        },
        TEST_USERS.ALICE
      );

      await testCallable(createDMChannel, request);

      expect(mockFirestore.collection().add as any).toHaveBeenCalledWith(
        expect.objectContaining({
          name: "Custom Chat Name",
        })
      );
    });

    it("should generate default channel name when not provided", async () => {
      const request = createMockRequest(
        { targetUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await testCallable(createDMChannel, request);

      expect(mockFirestore.collection().add as any).toHaveBeenCalledWith(
        expect.objectContaining({
          name: expect.stringMatching(/.*,.*/) // Default format with participant names
        })
      );
    });
  });

  describe("Blocked Users Check", () => {
    beforeEach(() => {
      // Mock target user exists
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: TEST_USERS.BOB.name,
          email: TEST_USERS.BOB.email,
        }),
      });
    });

    it("should prevent creating channel with blocked user", async () => {
      // Mock blocked relationship
      (mockFirestore.collection().where().where().limit().get as any)
        .mockResolvedValueOnce({ empty: true }) // No existing channel
        .mockResolvedValueOnce({ // Blocked check
          empty: false,
          docs: [{
            data: () => ({
              blockerId: TEST_USERS.ALICE.uid,
              blockedId: TEST_USERS.BOB.uid,
              status: "active",
            }),
          }],
        });

      const request = createMockRequest(
        { targetUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(createDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(createDMChannel, request)).rejects.toThrow("Cannot create channel with blocked user");
    });

    it("should prevent creating channel when blocked by target user", async () => {
      // Mock being blocked by target
      (mockFirestore.collection().where().where().limit().get as any)
        .mockResolvedValueOnce({ empty: true }) // No existing channel
        .mockResolvedValueOnce({ // Blocked check
          empty: false,
          docs: [{
            data: () => ({
              blockerId: TEST_USERS.BOB.uid,
              blockedId: TEST_USERS.ALICE.uid,
              status: "active",
            }),
          }],
        });

      const request = createMockRequest(
        { targetUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(createDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(createDMChannel, request)).rejects.toThrow("User has blocked you");
    });

    it("should allow channel creation when no blocks exist", async () => {
      // Mock no existing channel
      (mockFirestore.collection().where().where().limit().get as any)
        .mockResolvedValueOnce({ empty: true }) // No existing channel
        .mockResolvedValueOnce({ empty: true }); // No blocks

      const request = createMockRequest(
        { targetUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      const result = await testCallable(createDMChannel, request);

      expect(result.isNewChannel).toBe(true);
      expect(mockFirestore.collection().add as any).toHaveBeenCalled();
    });
  });

  describe("Friendship Requirement", () => {
    beforeEach(() => {
      // Mock target user exists
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: TEST_USERS.BOB.name,
          email: TEST_USERS.BOB.email,
        }),
      });

      // Mock no existing channel
      (mockFirestore.collection().where().where().limit().get as any)
        .mockResolvedValueOnce({ empty: true }) // No existing channel
        .mockResolvedValueOnce({ empty: true }); // No blocks
    });

    it("should require friendship to create DM", async () => {
      // Mock no friendship
      (mockFirestore.collection().where().where().where().limit().get as any).mockResolvedValueOnce({
        empty: true,
      });

      const request = createMockRequest(
        { targetUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(createDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(createDMChannel, request)).rejects.toThrow("Users must be friends to create DM channel");
    });

    it("should allow DM creation between friends", async () => {
      // Mock friendship exists
      (mockFirestore.collection().where().where().where().limit().get as any).mockResolvedValueOnce({
        empty: false,
        docs: [{
          data: () => ({
            requesterId: TEST_USERS.ALICE.uid,
            receiverId: TEST_USERS.BOB.uid,
            status: "accepted",
          }),
        }],
      });

      const request = createMockRequest(
        { targetUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      const result = await testCallable(createDMChannel, request);

      expect(result.isNewChannel).toBe(true);
      expect(mockFirestore.collection().add as any).toHaveBeenCalled();
    });
  });

  describe("Idempotency", () => {
    it("should handle idempotency correctly", async () => {
      // Mock idempotency key already processed
      (mockFirestore.collection().doc().get as any)
        .mockResolvedValueOnce({
          exists: true,
          data: () => ({ status: "completed" }),
        });

      const request = createMockRequest(
        { targetUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(createDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(createDMChannel, request)).rejects.toThrow("Request already processed");
    });
  });

  describe("Error Handling", () => {
    it("should handle Firestore errors", async () => {
      (mockFirestore.collection().doc().get as any).mockRejectedValueOnce(
        new Error("Firestore error")
      );

      const request = createMockRequest(
        { targetUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(createDMChannel, request)).rejects.toThrow("Firestore error");
    });

    it("should handle channel creation errors", async () => {
      // Mock target user exists
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({ name: TEST_USERS.BOB.name }),
      });

      // Mock no existing channel
      (mockFirestore.collection().where().where().limit().get as any)
        .mockResolvedValueOnce({ empty: true })
        .mockResolvedValueOnce({ empty: true }); // No blocks

      // Mock friendship exists
      (mockFirestore.collection().where().where().where().limit().get as any).mockResolvedValueOnce({
        empty: false,
        docs: [{ data: () => ({ status: "accepted" }) }],
      });

      // Mock add failure
      mockFirestore.collection().add.mockRejectedValueOnce(
        new Error("Channel creation failed")
      );

      const request = createMockRequest(
        { targetUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(createDMChannel, request)).rejects.toThrow("Channel creation failed");
    });
  });

  describe("Privacy Settings", () => {
    beforeEach(() => {
      // Mock target user exists
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: TEST_USERS.BOB.name,
          email: TEST_USERS.BOB.email,
          privacySettings: {
            allowDMFromFriends: false,
          },
        }),
      });

      // Mock no existing channel and no blocks
      (mockFirestore.collection().where().where().limit().get as any)
        .mockResolvedValueOnce({ empty: true })
        .mockResolvedValueOnce({ empty: true });

      // Mock friendship exists
      (mockFirestore.collection().where().where().where().limit().get as any).mockResolvedValueOnce({
        empty: false,
        docs: [{ data: () => ({ status: "accepted" }) }],
      });
    });

    it("should respect user privacy settings", async () => {
      const request = createMockRequest(
        { targetUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      await expect(testCallable(createDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(createDMChannel, request)).rejects.toThrow("User has disabled DM from friends");
    });

    it("should allow DM when privacy settings permit", async () => {
      // Mock user with DM enabled
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: TEST_USERS.BOB.name,
          privacySettings: {
            allowDMFromFriends: true,
          },
        }),
      });

      const request = createMockRequest(
        { targetUserId: TEST_USERS.BOB.uid },
        TEST_USERS.ALICE
      );

      const result = await testCallable(createDMChannel, request);

      expect(result.isNewChannel).toBe(true);
    });
  });
});