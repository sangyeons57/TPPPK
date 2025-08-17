import { IdempotencyManager } from "../idempotency";
import { mockFirestore, MockFirestoreHelper } from "../../__tests__/helpers";

// Mock admin.firestore
jest.mock("firebase-admin", () => ({
  firestore: () => mockFirestore,
}));

describe("IdempotencyManager", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    MockFirestoreHelper.reset();
  });

  describe("checkAndMark", () => {
    it("should allow new operation", async () => {
      // Mock non-existent document
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        exists: false,
      });

      const result = await IdempotencyManager.checkAndMark(
        "test-key",
        "test-operation",
        60
      );

      expect(result).toBe(true);
      expect(mockFirestore.collection).toHaveBeenCalledWith("idempotency_keys");
      expect(mockFirestore.collection().doc().set).toHaveBeenCalledWith({
        operation: "test-operation",
        status: "processing",
        createdAt: expect.any(Object),
        expiresAt: expect.any(Date),
      });
    });

    it("should reject completed operation", async () => {
      // Mock existing completed document
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        exists: true,
        data: () => ({
          operation: "test-operation",
          status: "completed",
        }),
      });

      const result = await IdempotencyManager.checkAndMark(
        "test-key",
        "test-operation"
      );

      expect(result).toBe(false);
      expect(mockFirestore.collection().doc().set).not.toHaveBeenCalled();
    });

    it("should reject processing operation within TTL", async () => {
      const recentDate = new Date(Date.now() - 30 * 60 * 1000); // 30 minutes ago
      
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        exists: true,
        data: () => ({
          operation: "test-operation",
          status: "processing",
          createdAt: { toDate: () => recentDate },
        }),
      });

      const result = await IdempotencyManager.checkAndMark(
        "test-key",
        "test-operation",
        60 // 60 minutes TTL
      );

      expect(result).toBe(false);
    });

    it("should allow processing operation beyond TTL", async () => {
      const oldDate = new Date(Date.now() - 120 * 60 * 1000); // 2 hours ago
      
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        exists: true,
        data: () => ({
          operation: "test-operation",
          status: "processing",
          createdAt: { toDate: () => oldDate },
        }),
      });

      const result = await IdempotencyManager.checkAndMark(
        "test-key",
        "test-operation",
        60 // 60 minutes TTL
      );

      expect(result).toBe(true);
      expect(mockFirestore.collection().doc().set).toHaveBeenCalled();
    });

    it("should handle different operations with same key", async () => {
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        exists: true,
        data: () => ({
          operation: "different-operation",
          status: "completed",
        }),
      });

      const result = await IdempotencyManager.checkAndMark(
        "test-key",
        "test-operation"
      );

      expect(result).toBe(true);
      expect(mockFirestore.collection().doc().set).toHaveBeenCalled();
    });
  });

  describe("markCompleted", () => {
    it("should mark operation as completed", async () => {
      await IdempotencyManager.markCompleted("test-key");

      expect(mockFirestore.collection().doc().update).toHaveBeenCalledWith({
        status: "completed",
        completedAt: expect.any(Object),
      });
    });

    it("should handle update errors gracefully", async () => {
      mockFirestore.collection().doc().update.mockRejectedValueOnce(
        new Error("Update failed")
      );

      await expect(
        IdempotencyManager.markCompleted("test-key")
      ).rejects.toThrow("Update failed");
    });
  });

  describe("markFailed", () => {
    it("should mark operation as failed with error message", async () => {
      const error = new Error("Operation failed");
      
      await IdempotencyManager.markFailed("test-key", error);

      expect(mockFirestore.collection().doc().update).toHaveBeenCalledWith({
        status: "failed",
        failedAt: expect.any(Object),
        error: "Operation failed",
      });
    });

    it("should handle non-Error objects", async () => {
      const error = "String error";
      
      await IdempotencyManager.markFailed("test-key", error);

      expect(mockFirestore.collection().doc().update).toHaveBeenCalledWith({
        status: "failed",
        failedAt: expect.any(Object),
        error: "String error",
      });
    });

    it("should not throw if update fails", async () => {
      mockFirestore.collection().doc().update.mockRejectedValueOnce(
        new Error("Update failed")
      );

      // Should not throw
      await expect(
        IdempotencyManager.markFailed("test-key", new Error("Test"))
      ).resolves.toBeUndefined();
    });
  });

  describe("generateKey", () => {
    it("should generate consistent keys", () => {
      const key1 = IdempotencyManager.generateKey("user1", "operation", "param1");
      const key2 = IdempotencyManager.generateKey("user1", "operation", "param1");
      
      expect(key1).toBe(key2);
      expect(key1).toBe("user1:operation:param1");
    });

    it("should generate different keys for different inputs", () => {
      const key1 = IdempotencyManager.generateKey("user1", "operation", "param1");
      const key2 = IdempotencyManager.generateKey("user2", "operation", "param1");
      const key3 = IdempotencyManager.generateKey("user1", "different", "param1");
      
      expect(key1).not.toBe(key2);
      expect(key1).not.toBe(key3);
    });

    it("should handle multiple parameters", () => {
      const key = IdempotencyManager.generateKey(
        "user1", 
        "operation", 
        "param1", 
        "param2", 
        "param3"
      );
      
      expect(key).toBe("user1:operation:param1:param2:param3");
    });
  });

  describe("cleanupExpired", () => {
    it("should clean up expired documents", async () => {
      const expiredDocs = [
        { ref: { delete: jest.fn() } },
        { ref: { delete: jest.fn() } },
      ];

      mockFirestore.collection().where().limit().get.mockResolvedValueOnce({
        empty: false,
        docs: expiredDocs,
      });

      await IdempotencyManager.cleanupExpired();

      expect(mockFirestore.batch().commit).toHaveBeenCalled();
    });

    it("should handle no expired documents", async () => {
      mockFirestore.collection().where().limit().get.mockResolvedValueOnce({
        empty: true,
        docs: [],
      });

      await IdempotencyManager.cleanupExpired();

      expect(mockFirestore.batch().commit).not.toHaveBeenCalled();
    });

    it("should handle cleanup errors", async () => {
      mockFirestore.collection().where().limit().get.mockRejectedValueOnce(
        new Error("Query failed")
      );

      await expect(IdempotencyManager.cleanupExpired()).rejects.toThrow("Query failed");
    });
  });
});