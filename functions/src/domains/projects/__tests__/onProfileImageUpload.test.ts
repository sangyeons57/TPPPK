import { onProjectProfileImageUpload } from "../events/onProfileImageUpload";
import { mockFirestore, MockFirestoreHelper } from "../../../__tests__/helpers";
import { CloudEvent } from "firebase-functions/v2";

// Mock admin.firestore
jest.mock("firebase-admin", () => ({
  firestore: () => mockFirestore,
  app: () => ({}),
}));

// Mock storage
const mockStorage = {
  bucket: jest.fn(() => ({
    file: jest.fn(() => ({
      getMetadata: jest.fn(),
      makePublic: jest.fn(),
      getSignedUrl: jest.fn(),
      exists: jest.fn(),
    })),
  })),
};

jest.mock("firebase-admin", () => ({
  firestore: () => mockFirestore,
  storage: () => mockStorage,
  app: () => ({}),
}));

describe("onProjectProfileImageUpload", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    MockFirestoreHelper.reset();
  });

  const createMockStorageEvent = (fileName: string, metadata?: any): CloudEvent<any> => ({
    id: "test-event-id",
    source: "storage.googleapis.com",
    specversion: "1.0",
    type: "google.cloud.storage.object.v1.finalized",
    time: "2024-01-01T12:00:00Z",
    data: {
      bucket: "test-bucket",
      name: fileName,
      contentType: "image/jpeg",
      size: "1024",
      timeCreated: "2024-01-01T12:00:00Z",
      updated: "2024-01-01T12:00:00Z",
      metadata: metadata || {},
    },
  });

  describe("File Path Validation", () => {
    it("should ignore files not in project_profile_images directory", async () => {
      const event = createMockStorageEvent("other/path/image.jpg");

      await expect(onProjectProfileImageUpload(event)).resolves.toBeUndefined();

      expect(mockFirestore.collection().doc().update).not.toHaveBeenCalled();
    });

    it("should process files in project_profile_images directory", async () => {
      // Mock project exists
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: "Test Project",
          ownerId: "test-owner",
        }),
      });

      const event = createMockStorageEvent("project_profile_images/test-project-id/image.jpg");

      await onProjectProfileImageUpload(event);

      expect(mockFirestore.collection().doc().get).toHaveBeenCalledWith();
    });

    it("should extract project ID from file path correctly", async () => {
      // Mock project exists
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: "Test Project",
          ownerId: "test-owner",
        }),
      });

      const event = createMockStorageEvent("project_profile_images/my-project-123/profile.png");

      await onProjectProfileImageUpload(event);

      expect(mockFirestore.collection().doc).toHaveBeenCalledWith("my-project-123");
    });
  });

  describe("Project Validation", () => {
    it("should skip processing if project does not exist", async () => {
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: false,
      });

      const event = createMockStorageEvent("project_profile_images/non-existent-project/image.jpg");

      await onProjectProfileImageUpload(event);

      expect(mockFirestore.collection().doc().update).not.toHaveBeenCalled();
    });

    it("should process if project exists", async () => {
      // Mock project exists
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: "Test Project",
          ownerId: "test-owner",
        }),
      });

      const event = createMockStorageEvent("project_profile_images/test-project/image.jpg");

      await onProjectProfileImageUpload(event);

      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalled();
    });
  });

  describe("Image Processing", () => {
    beforeEach(() => {
      // Mock project exists
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: "Test Project",
          ownerId: "test-owner",
        }),
      });
    });

    it("should update project with image URL", async () => {
      const event = createMockStorageEvent("project_profile_images/test-project/image.jpg");

      await onProjectProfileImageUpload(event);

      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith({
        profileImageUrl: expect.stringContaining("test-project"),
        profileImagePath: "project_profile_images/test-project/image.jpg",
        updatedAt: expect.any(Object),
      });
    });

    it("should handle different image formats", async () => {
      const supportedFormats = ["jpg", "jpeg", "png", "gif", "webp"];

      for (const format of supportedFormats) {
        // Reset mocks
        jest.clearAllMocks();
        
        // Mock project exists
        (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
          exists: true,
          data: () => ({
            name: "Test Project",
            ownerId: "test-owner",
          }),
        });

        const event = createMockStorageEvent(`project_profile_images/test-project/image.${format}`);

        await onProjectProfileImageUpload(event);

        expect(mockFirestore.collection().doc().update as any).toHaveBeenCalled();
      }
    });

    it("should ignore non-image files", async () => {
      const event = createMockStorageEvent("project_profile_images/test-project/document.pdf");

      await onProjectProfileImageUpload(event);

      expect(mockFirestore.collection().doc().update).not.toHaveBeenCalled();
    });
  });

  describe("Metadata Handling", () => {
    beforeEach(() => {
      // Mock project exists
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: "Test Project",
          ownerId: "test-owner",
        }),
      });
    });

    it("should include metadata in update when provided", async () => {
      const metadata = {
        uploadedBy: "test-user-id",
        originalName: "my-image.jpg",
      };

      const event = createMockStorageEvent(
        "project_profile_images/test-project/image.jpg",
        metadata
      );

      await onProjectProfileImageUpload(event);

      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith({
        profileImageUrl: expect.stringContaining("test-project"),
        profileImagePath: "project_profile_images/test-project/image.jpg",
        profileImageMetadata: metadata,
        updatedAt: expect.any(Object),
      });
    });

    it("should handle missing metadata gracefully", async () => {
      const event = createMockStorageEvent("project_profile_images/test-project/image.jpg");

      await onProjectProfileImageUpload(event);

      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith({
        profileImageUrl: expect.stringContaining("test-project"),
        profileImagePath: "project_profile_images/test-project/image.jpg",
        updatedAt: expect.any(Object),
      });
    });
  });

  describe("URL Generation", () => {
    beforeEach(() => {
      // Mock project exists
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: "Test Project",
          ownerId: "test-owner",
        }),
      });
    });

    it("should generate correct public URL", async () => {
      const event = createMockStorageEvent("project_profile_images/test-project-123/profile.jpg");

      await onProjectProfileImageUpload(event);

      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith(
        expect.objectContaining({
          profileImageUrl: expect.stringMatching(/test-project-123.*profile\.jpg/),
        })
      );
    });
  });

  describe("Error Handling", () => {
    it("should handle Firestore read errors", async () => {
      (mockFirestore.collection().doc().get as any).mockRejectedValueOnce(
        new Error("Firestore read error")
      );

      const event = createMockStorageEvent("project_profile_images/test-project/image.jpg");

      await expect(onProjectProfileImageUpload(event)).rejects.toThrow("Firestore read error");
    });

    it("should handle Firestore update errors", async () => {
      // Mock project exists
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: "Test Project",
          ownerId: "test-owner",
        }),
      });

      // Mock update failure
      mockFirestore.collection().doc().update.mockRejectedValueOnce(
        new Error("Firestore update error")
      );

      const event = createMockStorageEvent("project_profile_images/test-project/image.jpg");

      await expect(onProjectProfileImageUpload(event)).rejects.toThrow("Firestore update error");
    });

    it("should handle malformed file paths", async () => {
      const event = createMockStorageEvent("project_profile_images/");

      await expect(onProjectProfileImageUpload(event)).resolves.toBeUndefined();
    });

    it("should handle empty project ID", async () => {
      const event = createMockStorageEvent("project_profile_images//image.jpg");

      await expect(onProjectProfileImageUpload(event)).resolves.toBeUndefined();
    });
  });

  describe("File Size Validation", () => {
    beforeEach(() => {
      // Mock project exists
      (mockFirestore.collection().doc().get as any).mockResolvedValueOnce({
        exists: true,
        data: () => ({
          name: "Test Project",
          ownerId: "test-owner",
        }),
      });
    });

    it("should handle large files", async () => {
      const event = createMockStorageEvent("project_profile_images/test-project/image.jpg");
      event.data.size = "10485760"; // 10MB

      await onProjectProfileImageUpload(event);

      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalled();
    });

    it("should include file size in metadata", async () => {
      const event = createMockStorageEvent("project_profile_images/test-project/image.jpg");
      event.data.size = "2048";

      await onProjectProfileImageUpload(event);

      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith(
        expect.objectContaining({
          profileImageSize: "2048",
        })
      );
    });
  });
});