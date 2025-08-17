import { onProjectChange } from "../events/onProjectChange";
import { mockFirestore, MockFirestoreHelper } from "../../../__tests__/helpers";
import { CloudEvent } from "firebase-functions/v2";

// Mock admin.firestore
jest.mock("firebase-admin", () => ({
  firestore: () => mockFirestore,
  app: () => ({}),
}));

describe("onProjectChange", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    MockFirestoreHelper.reset();
  });

  const createMockFirestoreEvent = (
    eventType: string,
    projectId: string,
    oldData?: any,
    newData?: any
  ): CloudEvent<any> => ({
    id: "test-event-id",
    source: "firestore.googleapis.com",
    specversion: "1.0",
    type: `google.cloud.firestore.document.v1.${eventType}`,
    time: "2024-01-01T12:00:00Z",
    data: {
      oldValue: oldData ? { fields: oldData } : undefined,
      value: newData ? { fields: newData } : undefined,
      updateMask: { fieldPaths: ["name", "updatedAt"] },
    },
    subject: `projects/test-project/databases/(default)/documents/projects/${projectId}`,
  });

  describe("Project Creation", () => {
    it("should handle new project creation", async () => {
      const newProjectData = {
        name: { stringValue: "New Project" },
        ownerId: { stringValue: "owner-123" },
        createdAt: { timestampValue: "2024-01-01T12:00:00Z" },
        members: { arrayValue: { values: [{ stringValue: "owner-123" }] } },
      };

      const event = createMockFirestoreEvent("created", "test-project-id", undefined, newProjectData);

      await onProjectChange(event);

      // Should update member statistics
      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith({
        projectCount: expect.any(Object), // FieldValue.increment(1)
        lastProjectCreated: expect.any(Object),
        updatedAt: expect.any(Object),
      });
    });

    it("should handle project creation with multiple members", async () => {
      const newProjectData = {
        name: { stringValue: "Team Project" },
        ownerId: { stringValue: "owner-123" },
        members: { 
          arrayValue: { 
            values: [
              { stringValue: "owner-123" },
              { stringValue: "member-456" },
              { stringValue: "member-789" }
            ] 
          } 
        },
      };

      const event = createMockFirestoreEvent("created", "team-project", undefined, newProjectData);

      await onProjectChange(event);

      // Should update statistics for all members
      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledTimes(3);
    });
  });

  describe("Project Updates", () => {
    it("should handle project name updates", async () => {
      const oldData = {
        name: { stringValue: "Old Name" },
        ownerId: { stringValue: "owner-123" },
        members: { arrayValue: { values: [{ stringValue: "owner-123" }] } },
      };

      const newData = {
        name: { stringValue: "New Name" },
        ownerId: { stringValue: "owner-123" },
        members: { arrayValue: { values: [{ stringValue: "owner-123" }] } },
        updatedAt: { timestampValue: "2024-01-01T12:00:00Z" },
      };

      const event = createMockFirestoreEvent("updated", "test-project", oldData, newData);

      await onProjectChange(event);

      // Should log the change but not update member stats for name changes
      expect(mockFirestore.collection().doc().update).not.toHaveBeenCalled();
    });

    it("should handle member additions", async () => {
      const oldData = {
        name: { stringValue: "Project" },
        ownerId: { stringValue: "owner-123" },
        members: { arrayValue: { values: [{ stringValue: "owner-123" }] } },
      };

      const newData = {
        name: { stringValue: "Project" },
        ownerId: { stringValue: "owner-123" },
        members: { 
          arrayValue: { 
            values: [
              { stringValue: "owner-123" },
              { stringValue: "new-member-456" }
            ] 
          } 
        },
      };

      const event = createMockFirestoreEvent("updated", "test-project", oldData, newData);

      await onProjectChange(event);

      // Should update project count for new member
      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith({
        projectCount: expect.any(Object), // FieldValue.increment(1)
        lastProjectJoined: expect.any(Object),
        updatedAt: expect.any(Object),
      });
    });

    it("should handle member removals", async () => {
      const oldData = {
        name: { stringValue: "Project" },
        ownerId: { stringValue: "owner-123" },
        members: { 
          arrayValue: { 
            values: [
              { stringValue: "owner-123" },
              { stringValue: "leaving-member-456" }
            ] 
          } 
        },
      };

      const newData = {
        name: { stringValue: "Project" },
        ownerId: { stringValue: "owner-123" },
        members: { arrayValue: { values: [{ stringValue: "owner-123" }] } },
      };

      const event = createMockFirestoreEvent("updated", "test-project", oldData, newData);

      await onProjectChange(event);

      // Should decrement project count for removed member
      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith({
        projectCount: expect.any(Object), // FieldValue.increment(-1)
        lastProjectLeft: expect.any(Object),
        updatedAt: expect.any(Object),
      });
    });
  });

  describe("Project Deletion", () => {
    it("should handle project deletion", async () => {
      const deletedData = {
        name: { stringValue: "Deleted Project" },
        ownerId: { stringValue: "owner-123" },
        members: { 
          arrayValue: { 
            values: [
              { stringValue: "owner-123" },
              { stringValue: "member-456" }
            ] 
          } 
        },
      };

      const event = createMockFirestoreEvent("deleted", "deleted-project", deletedData, undefined);

      await onProjectChange(event);

      // Should decrement project count for all former members
      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledTimes(2);
      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledWith({
        projectCount: expect.any(Object), // FieldValue.increment(-1)
        lastProjectDeleted: expect.any(Object),
        updatedAt: expect.any(Object),
      });
    });

    it("should handle deletion of project with no members", async () => {
      const deletedData = {
        name: { stringValue: "Empty Project" },
        ownerId: { stringValue: "owner-123" },
        members: { arrayValue: { values: [] } },
      };

      const event = createMockFirestoreEvent("deleted", "empty-project", deletedData, undefined);

      await onProjectChange(event);

      // Should not update any member stats
      expect(mockFirestore.collection().doc().update).not.toHaveBeenCalled();
    });
  });

  describe("Data Validation", () => {
    it("should handle malformed member arrays", async () => {
      const newProjectData = {
        name: { stringValue: "Project" },
        ownerId: { stringValue: "owner-123" },
        members: { arrayValue: null }, // Malformed
      };

      const event = createMockFirestoreEvent("created", "malformed-project", undefined, newProjectData);

      await expect(onProjectChange(event)).resolves.toBeUndefined();
    });

    it("should handle missing member data", async () => {
      const newProjectData = {
        name: { stringValue: "Project" },
        ownerId: { stringValue: "owner-123" },
        // No members field
      };

      const event = createMockFirestoreEvent("created", "no-members", undefined, newProjectData);

      await expect(onProjectChange(event)).resolves.toBeUndefined();
    });

    it("should handle invalid project ID extraction", async () => {
      const event = createMockFirestoreEvent("created", "", undefined, {});
      event.subject = "invalid/path/structure";

      await expect(onProjectChange(event)).resolves.toBeUndefined();
    });
  });

  describe("Batch Operations", () => {
    it("should handle large member lists efficiently", async () => {
      const members = Array.from({ length: 50 }, (_, i) => ({ stringValue: `member-${i}` }));
      
      const newProjectData = {
        name: { stringValue: "Large Project" },
        ownerId: { stringValue: "owner-123" },
        members: { arrayValue: { values: members } },
      };

      const event = createMockFirestoreEvent("created", "large-project", undefined, newProjectData);

      await onProjectChange(event);

      // Should handle all 50 members
      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalledTimes(50);
    });

    it("should use batch operations when available", async () => {
      const members = [
        { stringValue: "member-1" },
        { stringValue: "member-2" },
        { stringValue: "member-3" },
      ];

      const newProjectData = {
        name: { stringValue: "Batch Project" },
        ownerId: { stringValue: "owner-123" },
        members: { arrayValue: { values: members } },
      };

      const event = createMockFirestoreEvent("created", "batch-project", undefined, newProjectData);

      await onProjectChange(event);

      // Verify batch operations were used
      expect(mockFirestore.batch().commit as any).toHaveBeenCalled();
    });
  });

  describe("Error Handling", () => {
    it("should handle Firestore update errors gracefully", async () => {
      const newProjectData = {
        name: { stringValue: "Error Project" },
        ownerId: { stringValue: "owner-123" },
        members: { arrayValue: { values: [{ stringValue: "owner-123" }] } },
      };

      // Mock update failure
      mockFirestore.collection().doc().update.mockRejectedValueOnce(
        new Error("Firestore update failed")
      );

      const event = createMockFirestoreEvent("created", "error-project", undefined, newProjectData);

      await expect(onProjectChange(event)).rejects.toThrow("Firestore update failed");
    });

    it("should handle partial batch failures", async () => {
      const members = [
        { stringValue: "member-1" },
        { stringValue: "member-2" },
      ];

      const newProjectData = {
        name: { stringValue: "Partial Fail Project" },
        ownerId: { stringValue: "owner-123" },
        members: { arrayValue: { values: members } },
      };

      // Mock batch commit failure
      mockFirestore.batch().commit.mockRejectedValueOnce(
        new Error("Batch commit failed")
      );

      const event = createMockFirestoreEvent("created", "partial-fail", undefined, newProjectData);

      await expect(onProjectChange(event)).rejects.toThrow("Batch commit failed");
    });
  });

  describe("Event Filtering", () => {
    it("should ignore non-project document changes", async () => {
      const event = createMockFirestoreEvent("updated", "test-id", {}, {});
      event.subject = "projects/test-project/databases/(default)/documents/users/user-123";

      await expect(onProjectChange(event)).resolves.toBeUndefined();

      expect(mockFirestore.collection().doc().update).not.toHaveBeenCalled();
    });

    it("should process only projects collection changes", async () => {
      const newProjectData = {
        name: { stringValue: "Valid Project" },
        ownerId: { stringValue: "owner-123" },
        members: { arrayValue: { values: [{ stringValue: "owner-123" }] } },
      };

      const event = createMockFirestoreEvent("created", "valid-project", undefined, newProjectData);
      
      await onProjectChange(event);

      expect(mockFirestore.collection().doc().update as any).toHaveBeenCalled();
    });
  });
});