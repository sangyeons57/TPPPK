import { updateUserProfile } from "../http/updateProfile";
import { mockFirestore, MockFirestoreHelper, createMockRequest, TEST_USERS } from "../../../__tests__/helpers";
import { HttpsError } from "firebase-functions/v2/https";

// Mock admin.firestore
jest.mock("firebase-admin", () => ({
  firestore: () => mockFirestore,
  app: () => ({}),
}));

describe("updateUserProfile", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    MockFirestoreHelper.reset();
  });

  describe("Authentication", () => {
    it("should throw error when user is not authenticated", async () => {
      const request = createMockRequest({ name: "New Name" });

      await expect(updateUserProfile(request)).rejects.toThrow(HttpsError);
      await expect(updateUserProfile(request)).rejects.toThrow("User not authenticated");
    });
  });

  describe("Input Validation", () => {
    it("should validate username format", async () => {
      const request = createMockRequest(
        { name: "invalid@name" },
        TEST_USERS.ALICE
      );

      await expect(updateUserProfile(request)).rejects.toThrow(HttpsError);
      await expect(updateUserProfile(request)).rejects.toThrow("Username can only contain letters, numbers, hyphens, and underscores");
    });

    it("should allow valid usernames", async () => {
      // Mock existing user
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: "OldName",
          memo: "Old memo",
        }),
      });

      // Mock username availability check
      mockFirestore.collection().where().limit().get.mockResolvedValueOnce({
        empty: true,
      });

      // Mock updated document
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        data: () => ({
          name: "valid-username",
          memo: "Test memo",
          updatedAt: { toDate: () => new Date() },
        }),
      });

      const request = createMockRequest(
        { name: "valid-username", memo: "Test memo" },
        TEST_USERS.ALICE
      );

      const result = await updateUserProfile(request);

      expect(result.userProfile.name).toBe("valid-username");
      expect(result.userProfile.memo).toBe("Test memo");
    });
  });

  describe("User Existence", () => {
    it("should throw error when user does not exist", async () => {
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        exists: false,
      });

      const request = createMockRequest(
        { name: "NewName" },
        TEST_USERS.ALICE
      );

      await expect(updateUserProfile(request)).rejects.toThrow(HttpsError);
      await expect(updateUserProfile(request)).rejects.toThrow("User not found");
    });
  });

  describe("Username Uniqueness", () => {
    it("should throw error when username is already taken", async () => {
      // Mock existing user
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: "OldName",
          memo: "Old memo",
        }),
      });

      // Mock username conflict
      mockFirestore.collection().where().limit().get.mockResolvedValueOnce({
        empty: false,
        docs: [{ id: "other-user-id" }],
      });

      const request = createMockRequest(
        { name: "taken-username" },
        TEST_USERS.ALICE
      );

      await expect(updateUserProfile(request)).rejects.toThrow(HttpsError);
      await expect(updateUserProfile(request)).rejects.toThrow("Username already exists");
    });

    it("should allow user to keep their current username", async () => {
      // Mock existing user
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: "CurrentName",
          memo: "Old memo",
        }),
      });

      // Mock same username check (same user)
      mockFirestore.collection().where().limit().get.mockResolvedValueOnce({
        empty: false,
        docs: [{ id: TEST_USERS.ALICE.uid }],
      });

      // Mock updated document
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        data: () => ({
          name: "CurrentName",
          memo: "New memo",
          updatedAt: { toDate: () => new Date() },
        }),
      });

      const request = createMockRequest(
        { name: "CurrentName", memo: "New memo" },
        TEST_USERS.ALICE
      );

      const result = await updateUserProfile(request);

      expect(result.userProfile.name).toBe("CurrentName");
      expect(result.userProfile.memo).toBe("New memo");
    });
  });

  describe("Profile Updates", () => {
    beforeEach(() => {
      // Mock existing user
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: "OldName",
          memo: "Old memo",
        }),
      });
    });

    it("should update only name", async () => {
      // Mock username availability
      mockFirestore.collection().where().limit().get.mockResolvedValueOnce({
        empty: true,
      });

      // Mock updated document
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        data: () => ({
          name: "NewName",
          memo: "Old memo",
          updatedAt: { toDate: () => new Date() },
        }),
      });

      const request = createMockRequest(
        { name: "NewName" },
        TEST_USERS.ALICE
      );

      const result = await updateUserProfile(request);

      expect(mockFirestore.collection().doc().update).toHaveBeenCalledWith({
        name: "NewName",
        updatedAt: expect.any(Object),
      });
      expect(result.userProfile.name).toBe("NewName");
    });

    it("should update only memo", async () => {
      // Mock updated document
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        data: () => ({
          name: "OldName",
          memo: "New memo",
          updatedAt: { toDate: () => new Date() },
        }),
      });

      const request = createMockRequest(
        { memo: "New memo" },
        TEST_USERS.ALICE
      );

      const result = await updateUserProfile(request);

      expect(mockFirestore.collection().doc().update).toHaveBeenCalledWith({
        memo: "New memo",
        updatedAt: expect.any(Object),
      });
      expect(result.userProfile.memo).toBe("New memo");
    });

    it("should update both name and memo", async () => {
      // Mock username availability
      mockFirestore.collection().where().limit().get.mockResolvedValueOnce({
        empty: true,
      });

      // Mock updated document
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        data: () => ({
          name: "NewName",
          memo: "New memo",
          updatedAt: { toDate: () => new Date() },
        }),
      });

      const request = createMockRequest(
        { name: "NewName", memo: "New memo" },
        TEST_USERS.ALICE
      );

      const result = await updateUserProfile(request);

      expect(mockFirestore.collection().doc().update).toHaveBeenCalledWith({
        name: "NewName",
        memo: "New memo",
        updatedAt: expect.any(Object),
      });
      expect(result.userProfile.name).toBe("NewName");
      expect(result.userProfile.memo).toBe("New memo");
    });

    it("should clear memo when set to empty string", async () => {
      // Mock updated document
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        data: () => ({
          name: "OldName",
          memo: "",
          updatedAt: { toDate: () => new Date() },
        }),
      });

      const request = createMockRequest(
        { memo: "" },
        TEST_USERS.ALICE
      );

      const result = await updateUserProfile(request);

      expect(mockFirestore.collection().doc().update).toHaveBeenCalledWith({
        memo: "",
        updatedAt: expect.any(Object),
      });
      expect(result.userProfile.memo).toBe("");
    });
  });

  describe("Response Format", () => {
    it("should return correct response format", async () => {
      // Mock existing user
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: "OldName",
          memo: "Old memo",
        }),
      });

      // Mock updated document
      const mockUpdatedAt = new Date("2024-01-01T12:00:00Z");
      mockFirestore.collection().doc().get.mockResolvedValueOnce({
        data: () => ({
          name: "OldName",
          memo: "New memo",
          updatedAt: { toDate: () => mockUpdatedAt },
        }),
      });

      const request = createMockRequest(
        { memo: "New memo" },
        TEST_USERS.ALICE
      );

      const result = await updateUserProfile(request);

      expect(result).toEqual({
        userProfile: {
          id: TEST_USERS.ALICE.uid,
          name: "OldName",
          memo: "New memo",
          updatedAt: mockUpdatedAt.toISOString(),
        },
      });
    });
  });
});