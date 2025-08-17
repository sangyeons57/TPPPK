import { unblockDMChannel } from "../http/unblockChannel";
import { mockFirestore, MockFirestoreHelper, createMockRequest, TEST_USERS, testCallable } from "../../../__tests__/helpers";
import { HttpsError } from "firebase-functions/v2/https";

// Mock admin.firestore
jest.mock("firebase-admin", () => ({
  firestore: () => mockFirestore,
  app: () => ({}),
}));

describe("unblockDMChannel", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    MockFirestoreHelper.reset();
  });

  describe("Authentication", () => {
    it("should throw error when user is not authenticated", async () => {
      const request = createMockRequest({ channelId: "test-channel-id" });

      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow("User not authenticated");
    });
  });

  describe("Input Validation", () => {
    it("should validate channelId is provided", async () => {
      const request = createMockRequest({}, TEST_USERS.ALICE);

      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow("Channel ID is required");
    });

    it("should validate channelId format", async () => {
      const request = createMockRequest(
        { channelId: "invalid@channel" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow("Invalid channel ID format");
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

      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow("Channel not found");
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

      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow("Can only unblock DM channels");
    });
  });

  describe("Participation Check", () => {
    it("should throw error when user is not a participant", async () => {
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.BOB.uid, TEST_USERS.CHARLIE.uid],
          blockedBy: [TEST_USERS.BOB.uid],
        }),
      });

      const request = createMockRequest(
        { channelId: "test-channel-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow("You are not a participant in this channel");
    });
  });

  describe("Block Status Check", () => {
    it("should throw error when channel is not blocked by user", async () => {
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
          isActive: true,
          blockedBy: [TEST_USERS.BOB.uid], // Only blocked by Bob, not Alice
        }),
      });

      const request = createMockRequest(
        { channelId: "test-channel-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow("Channel is not blocked by you");
    });

    it("should throw error when channel is not blocked at all", async () => {
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
          isActive: true,
          blockedBy: [],
        }),
      });

      const request = createMockRequest(
        { channelId: "test-channel-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow("Channel is not blocked by you");
    });
  });

  describe("Successful Unblocking", () => {
    beforeEach(() => {
      // Mock channel blocked by Alice
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
          isActive: false,
          blockedBy: [TEST_USERS.ALICE.uid],
          blockedAt: { toDate: () => new Date("2024-01-01") },
        }),
      });
    });

    it("should unblock channel successfully", async () => {
      const request = createMockRequest(
        { channelId: "test-channel-id" },
        TEST_USERS.ALICE
      );

      const result = await testCallable(unblockDMChannel, request);

      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith({
        blockedBy: [],
        isActive: true,
        unblockedAt: expect.any(Object),
        unblockedBy: TEST_USERS.ALICE.uid,
        updatedAt: expect.any(Object),
      });

      expect(result).toEqual({
        success: true,
        message: "Channel unblocked successfully",
        channelId: "test-channel-id",
        unblockedAt: expect.any(String),
      });
    });

    it("should remove only current user from blocked list when multiple users blocked", async () => {
      // Mock channel blocked by both Alice and Bob
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
          isActive: false,
          blockedBy: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
        }),
      });

      const request = createMockRequest(
        { channelId: "test-channel-id" },
        TEST_USERS.ALICE
      );

      await testCallable(unblockDMChannel, request);

      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith({
        blockedBy: [TEST_USERS.BOB.uid], // Alice removed, Bob remains
        isActive: false, // Still inactive because Bob still blocks
        unblockedAt: expect.any(Object),
        unblockedBy: TEST_USERS.ALICE.uid,
        updatedAt: expect.any(Object),
      });
    });

    it("should activate channel when last blocker unblocks", async () => {
      const request = createMockRequest(
        { channelId: "test-channel-id" },
        TEST_USERS.ALICE
      );

      await testCallable(unblockDMChannel, request);

      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith({
        blockedBy: [],
        isActive: true, // Activated when no blockers remain
        unblockedAt: expect.any(Object),
        unblockedBy: TEST_USERS.ALICE.uid,
        updatedAt: expect.any(Object),
      });
    });
  });

  describe("Participant Unblocking", () => {
    beforeEach(() => {
      // Mock channel blocked by Alice
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
          isActive: false,
          blockedBy: [TEST_USERS.ALICE.uid],
        }),
      });
    });

    it("should also unblock the other participant when requested", async () => {
      // Mock existing user block
      (mockFirestore.collection().where().where().limit().get as any).mockResolvedValueOnce({
        empty: false,
        docs: [{
          id: "block-doc-id",
          data: () => ({
            blockerId: TEST_USERS.ALICE.uid,
            blockedId: TEST_USERS.BOB.uid,
            status: "active",
          }),
        }],
      });

      const request = createMockRequest(
        { 
          channelId: "test-channel-id",
          unblockParticipant: true
        },
        TEST_USERS.ALICE
      );

      await testCallable(unblockDMChannel, request);

      // Should update user block status
      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith({
        status: "inactive",
        unblockedAt: expect.any(Object),
        updatedAt: expect.any(Object),
      });
    });

    it("should handle case when participant was never blocked", async () => {
      // Mock no existing user block
      (mockFirestore.collection().where().where().limit().get as any).mockResolvedValueOnce({
        empty: true,
        docs: [],
      });

      const request = createMockRequest(
        { 
          channelId: "test-channel-id",
          unblockParticipant: true
        },
        TEST_USERS.ALICE
      );

      const result = await testCallable(unblockDMChannel, request);

      // Should still succeed with channel unblock
      expect(result.success).toBe(true);
    });

    it("should not unblock participant when flag is false", async () => {
      const request = createMockRequest(
        { 
          channelId: "test-channel-id",
          unblockParticipant: false
        },
        TEST_USERS.ALICE
      );

      await testCallable(unblockDMChannel, request);

      // Should not query for user blocks
      expect(mockFirestore.collection().where).not.toHaveBeenCalledWith("blockerId", "==", TEST_USERS.ALICE.uid);
    });
  });

  describe("Notification Restoration", () => {
    beforeEach(() => {
      // Mock channel with muted notifications
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
          isActive: false,
          blockedBy: [TEST_USERS.ALICE.uid],
          notificationSettings: {
            [TEST_USERS.ALICE.uid]: {
              muted: true,
              blockedAt: new Date("2024-01-01"),
            },
          },
        }),
      });
    });

    it("should restore notifications when requested", async () => {
      const request = createMockRequest(
        { 
          channelId: "test-channel-id",
          restoreNotifications: true
        },
        TEST_USERS.ALICE
      );

      await testCallable(unblockDMChannel, request);

      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith(
        expect.objectContaining({
          notificationSettings: {
            [TEST_USERS.ALICE.uid]: {
              muted: false,
              restoredAt: expect.any(Object),
            },
          },
        })
      );
    });

    it("should keep notifications muted when not requested", async () => {
      const request = createMockRequest(
        { 
          channelId: "test-channel-id",
          restoreNotifications: false
        },
        TEST_USERS.ALICE
      );

      await testCallable(unblockDMChannel, request);

      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith(
        expect.not.objectContaining({
          notificationSettings: expect.any(Object),
        })
      );
    });
  });

  describe("History Handling", () => {
    beforeEach(() => {
      // Mock channel with block history
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
          isActive: false,
          blockedBy: [TEST_USERS.ALICE.uid],
          blockHistory: [{
            blockedBy: TEST_USERS.ALICE.uid,
            blockedAt: new Date("2024-01-01"),
            reason: "Spam",
          }],
        }),
      });
    });

    it("should preserve block history when unblocking", async () => {
      const request = createMockRequest(
        { channelId: "test-channel-id" },
        TEST_USERS.ALICE
      );

      await testCallable(unblockDMChannel, request);

      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith(
        expect.objectContaining({
          blockHistory: expect.arrayContaining([
            expect.objectContaining({
              blockedBy: TEST_USERS.ALICE.uid,
              unblockedAt: expect.any(Object),
            }),
          ]),
        })
      );
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

      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow("Request already processed");
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

      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow("Firestore read error");
    });

    it("should handle Firestore update errors", async () => {
      // Mock valid blocked channel
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
          isActive: false,
          blockedBy: [TEST_USERS.ALICE.uid],
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

      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow("Update failed");
    });

    it("should handle batch operations errors", async () => {
      // Mock valid blocked channel
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
          blockedBy: [TEST_USERS.ALICE.uid],
        }),
      });

      // Mock existing user block
      (mockFirestore.collection().where().where().limit().get as any).mockResolvedValueOnce({
        empty: false,
        docs: [{ id: "block-doc", data: () => ({}) }],
      });

      // Mock batch commit failure
      mockFirestore.batch().commit.mockRejectedValueOnce(
        new Error("Batch commit failed")
      );

      const request = createMockRequest(
        { 
          channelId: "test-channel-id",
          unblockParticipant: true
        },
        TEST_USERS.ALICE
      );

      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow("Batch commit failed");
    });
  });

  describe("Admin Override", () => {
    beforeEach(() => {
      // Mock DM channel that Alice is not part of but is blocked
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.BOB.uid, TEST_USERS.CHARLIE.uid],
          isActive: false,
          blockedBy: [TEST_USERS.BOB.uid],
        }),
      });
    });

    it("should allow admin to unblock any channel", async () => {
      const request = createMockRequest(
        { channelId: "test-channel-id" },
        TEST_USERS.ADMIN
      );

      const result = await testCallable(unblockDMChannel, request);

      expect(result.success).toBe(true);
      expect(result.message).toBe("Channel unblocked successfully");
    });
  });

  describe("Edge Cases", () => {
    it("should handle channel with undefined blockedBy field", async () => {
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
          isActive: false,
          // blockedBy field is undefined
        }),
      });

      const request = createMockRequest(
        { channelId: "test-channel-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow("Channel is not blocked by you");
    });

    it("should handle channel with empty blockedBy array", async () => {
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          type: "dm",
          participants: [TEST_USERS.ALICE.uid, TEST_USERS.BOB.uid],
          isActive: true,
          blockedBy: [],
        }),
      });

      const request = createMockRequest(
        { channelId: "test-channel-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(unblockDMChannel, request)).rejects.toThrow("Channel is not blocked by you");
    });
  });
});