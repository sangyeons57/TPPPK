import { removeProjectProfileImage } from "../http/removeProfileImage";
import { mockFirestore, MockFirestoreHelper, createMockRequest, TEST_USERS, testCallable } from "../../../__tests__/helpers";
import { HttpsError } from "firebase-functions/v2/https";

// Mock admin.firestore
jest.mock("firebase-admin", () => ({
  firestore: () => mockFirestore,
  app: () => ({}),
}));

describe("removeProjectProfileImage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    MockFirestoreHelper.reset();
  });

  describe("Authentication", () => {
    it("should throw error when user is not authenticated", async () => {
      const request = createMockRequest({ projectId: "test-project-id" });

      await expect(testCallable(removeProjectProfileImage, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(removeProjectProfileImage, request)).rejects.toThrow("User not authenticated");
    });
  });

  describe("Input Validation", () => {
    it("should validate projectId is provided", async () => {
      const request = createMockRequest({}, TEST_USERS.ALICE);

      await expect(testCallable(removeProjectProfileImage, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(removeProjectProfileImage, request)).rejects.toThrow("Project ID is required");
    });

    it("should validate projectId format", async () => {
      const request = createMockRequest(
        { projectId: "invalid@project" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(removeProjectProfileImage, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(removeProjectProfileImage, request)).rejects.toThrow("Invalid project ID format");
    });
  });

  describe("Project Existence", () => {
    it("should throw error when project does not exist", async () => {
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: false,
      });

      const request = createMockRequest(
        { projectId: "non-existent-project" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(removeProjectProfileImage, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(removeProjectProfileImage, request)).rejects.toThrow("Project not found");
    });
  });

  describe("Permission Checks", () => {
    beforeEach(() => {
      // Mock project exists
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: "Test Project",
          ownerId: TEST_USERS.BOB.uid, // Different from authenticated user
          members: [TEST_USERS.BOB.uid],
          profileImageUrl: "https://example.com/image.jpg",
        }),
      });
    });

    it("should throw error when user is not project owner", async () => {
      const request = createMockRequest(
        { projectId: "test-project-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(removeProjectProfileImage, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(removeProjectProfileImage, request)).rejects.toThrow("Only project owner can remove profile image");
    });

    it("should allow project owner to remove image", async () => {
      // Mock project with Alice as owner
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: "Test Project",
          ownerId: TEST_USERS.ALICE.uid,
          members: [TEST_USERS.ALICE.uid],
          profileImageUrl: "https://example.com/image.jpg",
        }),
      });

      const request = createMockRequest(
        { projectId: "test-project-id" },
        TEST_USERS.ALICE
      );

      const result = await testCallable(removeProjectProfileImage, request);

      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith({
        profileImageUrl: null,
        updatedAt: expect.any(Object),
      });

      expect(result).toEqual({
        success: true,
        message: "Project profile image removed successfully",
        projectId: "test-project-id",
      });
    });
  });

  describe("Image Status", () => {
    beforeEach(() => {
      // Mock project with Alice as owner
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: "Test Project",
          ownerId: TEST_USERS.ALICE.uid,
          members: [TEST_USERS.ALICE.uid],
        }),
      });
    });

    it("should handle project with no profile image", async () => {
      const request = createMockRequest(
        { projectId: "test-project-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(removeProjectProfileImage, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(removeProjectProfileImage, request)).rejects.toThrow("Project has no profile image to remove");
    });

    it("should handle project with null profile image", async () => {
      // Mock project with null profileImageUrl
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: "Test Project",
          ownerId: TEST_USERS.ALICE.uid,
          members: [TEST_USERS.ALICE.uid],
          profileImageUrl: null,
        }),
      });

      const request = createMockRequest(
        { projectId: "test-project-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(removeProjectProfileImage, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(removeProjectProfileImage, request)).rejects.toThrow("Project has no profile image to remove");
    });
  });

  describe("Successful Removal", () => {
    beforeEach(() => {
      // Mock project with image
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: "Test Project",
          ownerId: TEST_USERS.ALICE.uid,
          members: [TEST_USERS.ALICE.uid],
          profileImageUrl: "https://example.com/image.jpg",
        }),
      });
    });

    it("should remove profile image successfully", async () => {
      const request = createMockRequest(
        { projectId: "test-project-id" },
        TEST_USERS.ALICE
      );

      const result = await testCallable(removeProjectProfileImage, request);

      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith({
        profileImageUrl: null,
        updatedAt: expect.any(Object),
      });

      expect(result).toEqual({
        success: true,
        message: "Project profile image removed successfully",
        projectId: "test-project-id",
      });
    });

    it("should handle soft deletion when specified", async () => {
      const request = createMockRequest(
        { projectId: "test-project-id", softDelete: true },
        TEST_USERS.ALICE
      );

      const result = await testCallable(removeProjectProfileImage, request);

      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith({
        profileImageUrl: null,
        updatedAt: expect.any(Object),
        removedBy: TEST_USERS.ALICE.uid,
        removedAt: expect.any(Object),
      });

      expect(result.success).toBe(true);
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
        { projectId: "test-project-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(removeProjectProfileImage, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(removeProjectProfileImage, request)).rejects.toThrow("Request already processed");
    });
  });

  describe("Error Handling", () => {
    it("should handle Firestore errors", async () => {
      (mockFirestore.collection().doc().get as any).mockRejectedValueOnce(
        new Error("Firestore error")
      );

      const request = createMockRequest(
        { projectId: "test-project-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(removeProjectProfileImage, request)).rejects.toThrow("Firestore error");
    });

    it("should handle update errors", async () => {
      // Mock project with image
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: "Test Project",
          ownerId: TEST_USERS.ALICE.uid,
          profileImageUrl: "https://example.com/image.jpg",
        }),
      });

      // Mock update failure
      mockFirestore.collection().doc().update.mockRejectedValueOnce(
        new Error("Update failed")
      );

      const request = createMockRequest(
        { projectId: "test-project-id" },
        TEST_USERS.ALICE
      );

      await expect(testCallable(removeProjectProfileImage, request)).rejects.toThrow("Update failed");
    });
  });

  describe("Admin Override", () => {
    beforeEach(() => {
      // Mock project with different owner
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: "Test Project",
          ownerId: TEST_USERS.BOB.uid,
          members: [TEST_USERS.BOB.uid],
          profileImageUrl: "https://example.com/image.jpg",
        }),
      });
    });

    it("should allow admin to remove any project image", async () => {
      const request = createMockRequest(
        { projectId: "test-project-id" },
        TEST_USERS.ADMIN
      );

      const result = await testCallable(removeProjectProfileImage, request);

      expect(result.success).toBe(true);
      expect(result.message).toBe("Project profile image removed successfully");
    });
  });
});