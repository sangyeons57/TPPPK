import { blockDMChannel } from "../http/blockChannel";
import { mockFirestore, MockFirestoreHelper, createMockRequest, TEST_USERS, testCallable } from "../../../__tests__/helpers";
import { HttpsError } from "firebase-functions/v2/https";

// Mock admin.firestore
jest.mock("firebase-admin", () => ({
  firestore: () => mockFirestore,
  app: () => ({}),
}));

describe("blockDMChannel", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    MockFirestoreHelper.reset();
  });

  describe("Authentication", () => {
    it("should throw error when user is not authenticated", async () => {
      const request = createMockRequest({ channelId: "test-channel-id" });

      await expect(testCallable(blockDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(blockDMChannel, request)).rejects.toThrow("User not authenticated");
    });
  });

  describe("Input Validation", () => {
    it("should validate channelId is provided", async () => {
      const request = createMockRequest({}, TEST_USERS.ALICE);

      await expect(testCallable(blockDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(blockDMChannel, request)).rejects.toThrow("Channel ID is required");
    });

    it("should validate channelId format", async () => {
      const request = createMockRequest(
        { channelId: "invalid@channel" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(blockDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(blockDMChannel, request)).rejects.toThrow("Invalid channel ID format");
    });
  });

  describe("Channel Existence", () => {
    it("should throw error when channel does not exist", async () => {
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: false,
      });

      const request = createMockRequest(
        { channelId: "non-existent-channel" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(blockDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(blockDMChannel, request)).rejects.toThrow("Channel not found");
    });
  });

  describe("Channel Type Validation", () => {
    it("should throw error for non-DM channels", async () => {
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "group",
          participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid, TEST_USERS.CHARLIE.uid],
        }),
      });

      const request = createMockRequest(
        { channelId: "group-channel-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(blockDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(blockDMChannel, request)).rejects.toThrow("Can only block DM channels");
    });
  });

  describe("Participation Check", () => {
    it("should throw error when user is not a participant", async () => {
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.BOB.uid, TEST_USERS.CHARLIE.uid],
          isActive: true,
        }),
      });

      const request = createMockRequest(
        { channelId: "test-channel-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(blockDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(blockDMChannel, request)).rejects.toThrow("You are not a participant in this channel");
    });
  });

  describe("Block Status Check", () => {
    beforeEach(() => {
      // Mock valid DM channel
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
          isActive: true,
          blockedBy: [],
        }),
      });
    });

    it("should throw error when channel is already blocked by user", async () => {
      // Mock channel already blocked by Alice
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
          isActive: true,
          blockedBy: [TEST_USERS.ALICE.uid],
        }),
      });

      const request = createMockRequest(
        { channelId: "test-channel-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(blockDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(blockDMChannel, request)).rejects.toThrow("Channel is already blocked by you");
    });

    it("should allow blocking when not already blocked", async () => {
      const request = createMockRequest(
        { channelId: "test-channel-id" },
        TEST_USERS.ALICE
      );

      const result = await testCallable(blockDMChannel, request);

      expect(result).toEqual({
        success: true,
        message: "Channel blocked successfully",
        channelId: "test-channel-id",
        blockedAt: expect.any(String),
      });
    });
  });

  describe("Successful Blocking", () => {
    beforeEach(() => {
      // Mock valid DM channel
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
          isActive: true,
          blockedBy: [],
        }),
      });
    });

    it("should block channel successfully", async () => {
      const request = createMockRequest(
        { channelId: "test-channel-id" },
        TEST_USERS.ALICE
      );

      const result = await testCallable(blockDMChannel, request);

      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith({
        blockedBy: expect.arrayContaining([TEST_USERS.ALICE.uid]),
        isActive: false,
        blockedAt: expect.any(Object),
        updatedAt: expect.any(Object),
      });

      expect(result).toEqual({
        success: true,
        message: "Channel blocked successfully",
        channelId: "test-channel-id",
        blockedAt: expect.any(String),
      });
    });

    it("should add to existing blocked users list", async () => {
      // Mock channel already blocked by Bob
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
          isActive: true,
          blockedBy: [TEST_USERS.BOB.uid],
        }),
      });

      const request = createMockRequest(
        { channelId: "test-channel-id" },
        TEST_USERS.ALICE
      );

      await testCallable(blockDMChannel, request);

      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith({
        blockedBy: expect.arrayContaining([TEST_USERS.BOB.uid, TEST_USERS.ALICE.uid]),
        isActive: false,
        blockedAt: expect.any(Object),
        updatedAt: expect.any(Object),
      });
    });

    it("should handle soft blocking when specified", async () => {
      const request = createMockRequest(
        { 
          channelId: "test-channel-id",
          blockType: "soft",
          reason: "Spam messages"
        },
        TEST_USERS.ALICE
      );

      await testCallable(blockDMChannel, request);

      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith({
        blockedBy: expect.arrayContaining([TEST_USERS.ALICE.uid]),
        isActive: true, // Still active for soft block
        blockType: "soft",
        blockReason: "Spam messages",
        blockedAt: expect.any(Object),
        updatedAt: expect.any(Object),
      });
    });

    it("should handle hard blocking by default", async () => {
      const request = createMockRequest(
        { channelId: "test-channel-id" },
        TEST_USERS.ALICE
      );

      await testCallable(blockDMChannel, request);

      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith({
        blockedBy: expect.arrayContaining([TEST_USERS.ALICE.uid]),
        isActive: false, // Inactive for hard block
        blockType: "hard",
        blockedAt: expect.any(Object),
        updatedAt: expect.any(Object),
      });
    });
  });

  describe("Participant Blocking", () => {
    beforeEach(() => {
      // Mock valid DM channel
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
          isActive: true,
          blockedBy: [],
        }),
      });
    });

    it("should also block the other participant", async () => {
      const request = createMockRequest(
        { 
          channelId: "test-channel-id",
          blockParticipant: true
        },
        TEST_USERS.ALICE
      );

      await testCallable(blockDMChannel, request);

      // Should update both channel and create user block
      expect(mockFirestore.collection().add as any).toHaveBeenCalledWith({
        blockerId: TEST_USERS.ALICE.uid,
        blockedId: TEST_USERS.BOB.uid,
        status: "active",
        reason: "Blocked from DM channel",
        channelId: "test-channel-id",
        createdAt: expect.any(Object),
      });
    });

    it("should not block participant when flag is false", async () => {
      const request = createMockRequest(
        { 
          channelId: "test-channel-id",
          blockParticipant: false
        },
        TEST_USERS.ALICE
      );

      await testCallable(blockDMChannel, request);

      // Should only update channel, not create user block
      expect(mockFirestore.collection().add).not.toHaveBeenCalled();
    });
  });

  describe("Batch Operations", () => {
    beforeEach(() => {
      // Mock valid DM channel
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
          isActive: true,
          blockedBy: [],
        }),
      });
    });

    it("should use batch operations for multiple updates", async () => {
      const request = createMockRequest(
        { 
          channelId: "test-channel-id",
          blockParticipant: true,
          muteNotifications: true
        },
        TEST_USERS.ALICE
      );

      await testCallable(blockDMChannel, request);

      // Verify batch operations were used
      expect(mockFirestore.batch().commit as any).toHaveBeenCalled();
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
        { channelId: "test-channel-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(blockDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(blockDMChannel, request)).rejects.toThrow("Request already processed");
    });
  });

  describe("Error Handling", () => {
    it("should handle Firestore read errors", async () => {
      (mockFirestore.collection().doc().get as any).mockRejectedValueOnce(
        new Error("Firestore read error")
      );

      const request = createMockRequest(
        { channelId: "test-channel-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(blockDMChannel, request)).rejects.toThrow("Firestore read error");
    });

    it("should handle Firestore update errors", async () => {
      // Mock valid channel
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
          isActive: true,
          blockedBy: [],
        }),
      });

      // Mock update failure
      mockFirestore.collection().doc().update.mockRejectedValueOnce(
        new Error("Update failed")
      );

      const request = createMockRequest(
        { channelId: "test-channel-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(blockDMChannel, request)).rejects.toThrow("Update failed");
    });

    it("should handle batch commit errors", async () => {
      // Mock valid channel
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
          isActive: true,
          blockedBy: [],
        }),
      });

      // Mock batch commit failure
      mockFirestore.batch().commit.mockRejectedValueOnce(
        new Error("Batch commit failed")
      );

      const request = createMockRequest(
        { 
          channelId: "test-channel-id",
          blockParticipant: true
        },
        TEST_USERS.ALICE
      );

      await expect(testCallable(blockDMChannel, request)).rejects.toThrow("Batch commit failed");
    });
  });

  describe("Notifications", () => {
    beforeEach(() => {
      // Mock valid DM channel
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
          isActive: true,
          blockedBy: [],
        }),
      });
    });

    it("should handle notification preferences", async () => {
      const request = createMockRequest(
        { 
          channelId: "test-channel-id",
          muteNotifications: true,
          notifyOtherParticipant: false
        },
        TEST_USERS.ALICE
      );

      const result = await testCallable(blockDMChannel, request);

      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith(
        expect.objectContaining({
          notificationSettings: {
            [TEST_USERS.ALICE.uid]: {
              muted: true,
              blockedAt: expect.any(Object),
            },
          },
        })
      );

      expect(result.success).toBe(true);
    });
  });

  describe("Admin Override", () => {
    beforeEach(() => {
      // Mock DM channel that Alice is not part of
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.BOB.uid, TEST_USERS.CHARLIE.uid],
          isActive: true,
          blockedBy: [],
        }),
      });
    });

    it("should allow admin to block any channel", async () => {
      const request = createMockRequest(
        { channelId: "test-channel-id" },
        TEST_USERS.ADMIN
      );

      const result = await testCallable(blockDMChannel, request);

      expect(result.success).toBe(true);
      expect(result.message).toBe("Channel blocked successfully");
    });
  });
});